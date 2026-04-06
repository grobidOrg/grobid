# Notes for the Grobid developers

This page contains a set of notes for the Grobid developers: 

## Deep learning models on Linux with Conda 

This is a summary of the steps I used to run Grobid using DL natively on Linux:
1. mkdir grobid_workspace
2grobid is in the subdirectory `grobid`
3git clone https://github.com/kermitt2/delft (delft should be in the parent directory, in respect of `grobid`)

Assuming that: 

1. Conda is installed (if not, I installed [this](https://github.com/conda-forge/miniforge/releases/tag/24.9.2-0) - check the version, might be old) 
2. The environment `delft` has been created with either `python=3.10` (e.g. `conda create --name delft python=3.10` or ) or `python=3.11` (e.g. `conda create --name delft python=3.11`) 

Then continue here: 

1. cd grobid
2. `pip install delft==0.4.4`
3. `pip install jep==4.3.1`
4. `export LD_PRELOAD=${CONDA_PREFIX}/lib/libpython3.11.so` (or libpython3.10.so if you use python 3.10)
5. `export XLA_FLAGS=--xla_gpu_cuda_data_dir=$CONDA_PREFIX`

[//]: # (5. `export LD_LIBRARY_PATH=${CONDA_PREFIX}/lib:$LD_LIBRARY_PATH`)
6. Change any model in the `grobid.yaml` configuration file to use delft instead of wapiti (e.g. header model)
7. `./gradlew run`

### Release

This section documents how to cut a new GROBID release end-to-end. In the steps below, replace `<X.Y.Z>` with the version being released (e.g. `0.9.0`) and `<X.Y.(Z+1)>` with the next development version (e.g. `0.9.1`).

#### Background

GROBID uses the [`net.researchgate.release`](https://github.com/researchgate/gradle-release) Gradle plugin (declared at `build.gradle:6` and applied at `build.gradle:85`, configured at `build.gradle:691-698`):

```gradle
release {
    failOnUnversionedFiles = false
    failOnCommitNeeded = false
    tagTemplate = '${version}'
    git {
        requireBranch.set('master')
    }
}
```

The plugin:

- Requires the release to be cut from the `master` branch.
- Creates bare-version tags (e.g. `0.9.0`, not `v0.9.0`).
- **Does NOT push to the remote** — you must `git push` manually.
- **Does NOT fail on uncommitted/unversioned files** — you must verify a clean working tree yourself before running it.

The version flows from a single source of truth (`gradle.properties`) into the running service via `processResources` (`build.gradle:301-309`), which expands `${project_version}` in `grobid-core/src/main/resources/grobid-version.txt`. That file is then read at runtime by `GrobidProperties.getVersion()` and exposed at `GET /api/version`.

The git revision is independently derived from `git describe --tags --always --first-parent` (`build.gradle:59-72`), baked into `grobid-revision.txt`, and surfaced at the same endpoint.

Java artefacts are **not** published to Maven Central. With the end of JCenter, the fact that the repo is too large for JitPack, and that we are not ready to deal with the Maven Central bureaucracy, we publish the GROBID library artefacts ourselves on a "DIY" repository. The expected usage is that consumers run GROBID via the Docker image or the REST service; users of the Java library will typically rebuild from source because they need a local `grobid-home` anyway.

#### Files updated automatically

Do not edit these by hand at release time:

| File | Updated by |
|---|---|
| `gradle.properties` (`version=...`) | The Gradle Release plugin (sets `<X.Y.Z>` then `<X.Y.(Z+1)>-SNAPSHOT`) |
| `grobid-core/build/resources/main/grobid-version.txt` | The `processResources` task at build time (driven by `gradle.properties`) |
| `grobid-core/build/resources/main/grobid-revision.txt` | The `processResources` task using `git describe --tags` |

> **Do not** replace the placeholder `${project_version}` in `grobid-core/src/main/resources/grobid-version.txt` with a literal — it is a Gradle template substituted at build time.

#### Files that MUST be updated manually before running `./gradlew release`

The Gradle Release plugin only touches `gradle.properties`. Everything below has to be edited manually and merged to `master` **before** the release commit, so the changes land in the pre-tag commit:

- `CITATION.cff` — set `version: <X.Y.Z>`.
- `doc/Install-Grobid.md` — update the stable version references and the "current development version" line.
- `doc/Grobid-service.md` — update build/install snippets.
- `doc/Grobid-batch.md` — update CLI examples (about 14 references).
- `doc/Grobid-docker.md` — update `docker pull`/`docker run`/`docker build` examples (about 17 references).
- `doc/Grobid-java-library.md` — update Maven/Gradle dependency snippets and the `-SNAPSHOT` references.
- `doc/Frequently-asked-questions.md` — update version-specific examples.
- `doc/Deep-Learning-models.md` — update the recommended-version line.
- `doc/Notes-grobid-developers.md` — update the example version below in the "Cutting the release" section.
- `doc/getting_started.md` — update any forward-reference wording (e.g. "next release > X.Y.Z").
- `doc/benchmarks/Benchmarking-pmc.md`, `Benchmarking-plos.md`, `Benchmarking-elife.md`, `Benchmarking-biorxiv.md` — update the version in headers when new benchmarks are produced.
- `Readme.md` — most version references use dynamic badges, but verify any inline wording mentioning the previous version.

`README.md` itself uses dynamic GitHub release/Docker Hub badges and does not contain a hardcoded version string.

#### Pre-release checklist

1. CI on `master` (`ci-build-unstable.yml`) is green.
2. `CHANGELOG.md` `[<X.Y.Z>]` section is complete and accurate.
3. All docs above have been updated (see verification grep at the bottom of this section).
4. Open a "Prepare `<X.Y.Z>` release" PR with all the manual edits, get it merged to `master`.
5. Pull `master` locally; ensure the working tree is clean and `gradle.properties` reads the pre-release SNAPSHOT version.
6. Dry build: `./gradlew clean assemble` must succeed.

#### Cutting the release

From a clean `master` checkout:

```
./gradlew release \
    -Prelease.useAutomaticVersion=true \
    -Prelease.releaseVersion=<X.Y.Z> \
    -Prelease.newVersion=<X.Y.(Z+1)>-SNAPSHOT
```

This will:

- Verify the working tree is clean and the branch is `master`.
- Set `version=<X.Y.Z>` in `gradle.properties`.
- Run `build` (which runs the test suite).
- Create commit `[Gradle Release Plugin] - pre tag commit:  '<X.Y.Z>'.`
- Create tag `<X.Y.Z>` pointing at that commit.
- Set `version=<X.Y.(Z+1)>-SNAPSHOT` in `gradle.properties`.
- Create commit `[Gradle Release Plugin] - new version commit:  '<X.Y.(Z+1)>-SNAPSHOT'.`

The plugin **does not push**. Do it yourself:

```
git push origin master
git push origin <X.Y.Z>
```

#### Producing release Docker images

Docker images are built and published manually via `workflow_dispatch` GitHub Actions workflows. There is no tag-triggered release workflow by design — the manual dispatch keeps a human in the loop before promoting images to the `grobid/grobid` org namespace.

The workflows derive `GROBID_VERSION` from `git describe --tags --always --first-parent` of the checked-out ref, so when dispatched from a release tag they produce the bare version (e.g. `0.9.0`), and the resulting image carries the correct `org.label-schema.version` OCI label by construction.

For each release, dispatch the following workflows from the `<X.Y.Z>` tag:

1. **CRF image (multi-arch amd64 + arm64)** — `.github/workflows/ci-build-manual-crf.yml`
   - Run from the GitHub Actions UI on tag `<X.Y.Z>`.
   - `custom_tag` input: `<X.Y.Z>-crf`
   - Pushes `lfoppiano/grobid:<X.Y.Z>-crf` (linux/amd64 + linux/arm64).

2. **Full image (DeLFT, amd64 only)** — `.github/workflows/ci-build-manual-full.yml`
   - Run on tag `<X.Y.Z>`.
   - `custom_tag` input: `<X.Y.Z>-full`
   - Pushes `lfoppiano/grobid:latest-full` and `lfoppiano/grobid:<X.Y.Z>-full`.

3. *(Optional)* **ONNX image** — `.github/workflows/ci-build-manual-onnx.yml` with `custom_tag=<X.Y.Z>-onnx`.

4. *(Optional)* **Evaluation image** — `.github/workflows/ci-build-manual-eval.yml` with `custom_tag=<X.Y.Z>`. Pushes `lfoppiano/grobid-evaluation`.

Once the images are verified under `lfoppiano/grobid`, **promote them to the `grobid/grobid` org namespace** using `.github/workflows/ci-build-tag-custom.yml`:

- Dispatch with `source_image=lfoppiano/grobid`, `source_tag=<X.Y.Z>-crf`, `target_image=grobid/grobid`, `target_tag=<X.Y.Z>-crf`.
- Repeat for `-full` and any other flavors.

#### Java artefact upload to the DIY repository

From the `<X.Y.Z>` tag:

```
git checkout <X.Y.Z>
./gradlew clean install
```

This populates `~/.m2/repository/org/grobid/<artifact>/<X.Y.Z>/` with the JAR, POM, and checksum files for each subproject (`grobid-core`, `grobid-trainer`, `grobid-service`, `grobid-home`). Upload the contents to the DIY repository preserving the Maven layout (`org/grobid/<artifact>/<X.Y.Z>/...`).

Also attach the same JARs (and the `grobid-<X.Y.Z>.zip` source archive from the GitHub release page) to the GitHub release as downloadable assets so users have a fallback.

#### Creating the GitHub release

1. Open the repository's "Releases" page in the GitHub UI → "Draft a new release".
2. Choose tag `<X.Y.Z>`.
3. Title: `GROBID <X.Y.Z>`.
4. Body: paste the `[<X.Y.Z>]` section from `CHANGELOG.md`.
5. Attach the JAR/POM/zip artefacts.

#### Post-release validation

1. Wait for the manual workflows to finish.
2. Pull the released image and verify the OCI label:

   ```
   docker pull grobid/grobid:<X.Y.Z>-crf
   docker inspect grobid/grobid:<X.Y.Z>-crf \
       --format '{{ index .Config.Labels "org.label-schema.version" }}'
   # expected: <X.Y.Z>
   ```

3. Boot the image and check the runtime version:

   ```
   docker run --rm -d --name g-test -p 8070:8070 grobid/grobid:<X.Y.Z>-crf
   sleep 30
   curl -s http://localhost:8070/api/version
   # expected: {"version":"<X.Y.Z>","revision":"<X.Y.Z>"}
   docker stop g-test
   ```

4. Verify the GitHub release page renders the assets and the tag is reachable.
5. Confirm the DIY repository hosts the new artefacts at the expected URLs.
6. Open a follow-up PR to update `CHANGELOG.md`: replace `## [<X.Y.Z>] - unreleased` with `## [<X.Y.Z>] - <YYYY-MM-DD>` (the actual release date), and add a new `## [<X.Y.(Z+1)>] - unreleased` section above it for ongoing development.

#### Verification grep (run before merging the release-prep PR)

This must return zero matches except for known false positives (binary `model.wapiti` files, historical benchmark filenames under `grobid-trainer/doc/`, and the literal `${project_version}` template):

```
grep -rn '<previous-version>' \
  --include='*.md' --include='*.gradle' --include='*.java' \
  --include='*.kt' --include='*.cff' --include='*.yml' \
  --include='*.properties' .
```


### Configuration of GROBID module models

Let's say we want to introduce a new model in a Grobid module called `newModel`. The new model configuration can be expressed as the normal Grobid model in a yaml config file:

```yaml
model:
  name: "newModel"
  #engine: "wapiti"
  engine: "delft"
  wapiti:
    # wapiti training parameters, they will be used at training time only
    epsilon: 0.00001
    window: 30
    nbMaxIterations: 1500
  delft:
    # deep learning parameters
    architecture: "BidLSTM_CRF"
    #architecture: "scibert"
    useELMo: false
    embeddings_name: "glove-840B"
```

In the module configuration class, we refer to the existing Grobid config class, for instance in a class `NewModuleConfiguration`:

```java
package org.grobid.core.utilities;

import org.grobid.core.utilities.GrobidConfig.ModelParameters;

public class NewModuleConfiguration {

   /* other config parameter here */ 

   public ModelParameters getModel() {
        return model;
    }

    public void getModel(ModelParameters model) {
        this.model = model;
    }
}

```

For initializing the new model, we simply do the following:

```java
        NewModuleConfiguration newModuleConfiguration = null;
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            newModuleConfiguration = mapper.readValue(new File("resources/config/config.yml"), NewModuleConfiguration.class);
        } catch(Exception e) {
            LOGGER.error("The config file does not appear valid, see resources/config/config.yml", e);
        }

        if (newModuleConfiguration != null && newModuleConfiguration.getModel() != null)
            GrobidProperties.getInstance().addModel(newModuleConfiguration.getModel());
        LibraryLoader.load();
```

The appropriate libraries will be loaded dynamically based on the configuration of the normal Grobid models and this new model. 


### Unit tests of Grobid Parsers

Sometimes you want to test methods of a grobid parser, without having to instantiate and load the wapiti model.
We recommend separating tests that require wapiti models and call them with a name ending with `IntegrationTest.java` with proper unit tests (using names ending with `Test.java`). 
If you set up a Continuous Integration system, is probably better to exclude integration tests, while they might not work if the grobid-home is properly set up. 

You can exclude Integration tests by default in your gradle.build, by adding: 

```groovy
test {
    exclude '**/**IntegrationTest**'
}
```
   
The DUMMY model (``GrobidModels.DUMMY``) is an artifact to instantiate a GrobidParser wihtout having the model under the grobid-home. 

This is useful for unit test of different part of the parser, for example if you have a method that read the sequence labelling results and assemble into a set of objects. 

**NOTE**: this method unfortunately cannot avoid problems when the Lexicons are used in the parser. A solution for that is that you mock the Lexicon and pass it as method to the parser. Some additional information can be found [here](https://github.com/kermitt2/grobid/issues/410#issuecomment-478888438). 

```java
    public class SuperconductorsParserTest {
    private SuperconductorsParser target;
    private ChemDataExtractorClient mockChemspotClient;

    @Before
    public void setUp() throws Exception {
        //Example of a mocked version of an additional service that is passed to the parser
        mockChemspotClient = EasyMock.createMock(ChemDataExtractorClient.class);
    
        // Passing GrobidModels.DUMMY 
        target = new SuperconductorsParser(GrobidModels.DUMMY, mockChemspotClient);
    }
    
    @Test
    public void test1() throws Exception {
        target.myMethod();
    }
}
```
