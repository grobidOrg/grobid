# Upgrading GROBID

This page lists the breaking changes users should be aware of when upgrading an existing GROBID installation to a newer version. If you are installing GROBID for the first time, follow the [Quick start](getting_started.md) instead.

!!! tip "Docker users have the easiest path"
    If you run GROBID via the official Docker images ([`grobid/grobid`](https://hub.docker.com/r/grobid/grobid)), upgrading is simply a matter of pulling the new tag — the image already bundles matching model files, DeLFT, TensorFlow and Python versions. The notes below mostly concern users who build GROBID from source, use custom-trained models, or deploy without Docker.

## Upgrading to 0.9.1

Version 0.9.1 is mainly a maintenance and dependency-refresh release. The shipped models, the Deep Learning runtime (DeLFT / TensorFlow / Python) and the JDK 21 / Gradle 9 build requirement are **unchanged from 0.9.0** — no retraining and no environment change are needed. The one thing to check is the **service configuration**, because 0.9.1 upgrades the embedded web framework.

### At a glance

- **Web framework** → Dropwizard **4 → 5** (Jakarta EE 10, **Jetty 11 → 12**).
- **Custom service config** → the `server.maxQueuedRequests` option was removed and now **prevents the service from starting** if present.
- **Downstream services** that embed `grobid-service` → must move off the Jetty‑11 `CrossOriginFilter` / `QoSFilter` (removed in Jetty 12).
- **OpenNLP** → 1.9.4 → 2.5.x. No configuration change.
- The stock `grobid-home/config/grobid.yaml` shipped with GROBID is already compatible — these notes only affect **customised** configs and downstream applications.

### Service configuration: Dropwizard 5 / Jetty 12

Dropwizard 5 validates the `server:` block more strictly than Dropwizard 4 and rejects a few options that Jetty 12 removed. If you kept the default `grobid.yaml` you have nothing to do; if you maintain a customised one, check the following.

#### Remove `server.maxQueuedRequests`

Dropwizard 5's `DefaultServerFactory` no longer accepts `maxQueuedRequests`. If your config still sets it, GROBID aborts at startup:

```
io.dropwizard.configuration.ConfigurationParsingException: grobid.yaml has an error:
  * Unrecognized field at: server.maxQueuedRequests
...
Caused by: com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException:
  Unrecognized field "maxQueuedRequests"
  (class io.dropwizard.core.server.DefaultServerFactory), not marked as ignorable
```

The fix is to delete the line from the `server:` block:

```diff
 server:
   type: custom
   ...
   maxThreads: 2048
-  maxQueuedRequests: 2048
```

#### Keep connector-only options on the connector

`idleTimeout` and `acceptQueueSize` are properties of the HTTP **connector**, not of the server factory. If you had set them at the top `server:` level they must move under the relevant `applicationConnectors` / `adminConnectors` entry (this is also where the stock config already puts them):

```yaml
server:
  type: custom
  applicationConnectors:
    - type: http
      port: 8070
      idleTimeout: 120 seconds   # on the connector, not at the server level
      acceptQueueSize: 2048       # on the connector, not at the server level
```

### Downstream services that embed grobid-service

If you build a service **on top of** `grobid-service` (for example grobid-quantities, datastet, software-mentions, or your own Dropwizard application), note that Jetty 12 **removed** the servlet filters these applications commonly used for CORS and request throttling:

- `org.eclipse.jetty.servlets.CrossOriginFilter`
- `org.eclipse.jetty.servlets.QoSFilter`

Referencing them now throws `NoClassDefFoundError` at startup. Replace them with the equivalent Jetty 12 server **handlers**, inserted above the application context:

```java
import org.eclipse.jetty.server.handler.CrossOriginHandler;
import org.eclipse.jetty.server.handler.QoSHandler;

// CORS
CrossOriginHandler cors = new CrossOriginHandler();
cors.setAllowedOriginPatterns(Set.of("*"));
cors.setAllowedMethods(Set.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
cors.setAllowedHeaders(Set.of("X-Requested-With", "Content-Type", "Accept", "Origin"));
cors.setAllowCredentials(false);
environment.getApplicationContext().insertHandler(cors);

// optional: cap concurrent requests (replaces QoSFilter's maxRequests)
QoSHandler qos = new QoSHandler();
qos.setMaxRequestCount(maxParallelRequests);
environment.getApplicationContext().insertHandler(qos);
```

Bump your own web-stack dependencies to match GROBID 0.9.1: `io.dropwizard:*:5.0.1`, `ru.vyarus:dropwizard-guicey:8.0.2`, and the Jetty 12 BOMs `org.eclipse.jetty:jetty-bom:12.1.9` and `org.eclipse.jetty.ee10:jetty-ee10-bom:12.1.9`. The `grobid-service` `GrobidServiceApplication` is the reference implementation.

### New: metrics export (opt-in)

0.9.1 adds a Prometheus scrape endpoint (`/metrics/prometheus` on the admin connector) and an optional push-based OTLP exporter — both **disabled by default**, so no action is needed to upgrade. To enable them, see [Monitoring with Prometheus](Monitoring.md).

### Other changes

- **OpenNLP 1.9.4 → 2.5.x** (sentence segmentation): no configuration change, but the OpenNLP API differs if you call it directly from custom code.
- **Fail-fast on spaces in `grobid-home`** when Wapiti is the configured engine: if your install path contains spaces, move it to a path without spaces.
- **pdfalto temporary directory**: pdfalto now writes its scratch files to GROBID's configured `grobid.temp` (default `grobid-home/tmp`) via `TMPDIR`, instead of `/tmp`.

For the full list of changes, see the [0.9.1 CHANGELOG entry](https://github.com/grobidOrg/grobid/blob/master/CHANGELOG.md).

## Upgrading to 0.9.0

Version 0.9.0 is a **major release** with changes that affect the build environment, the Deep Learning runtime, and the shipped models. Existing 0.8.x installations will **not** work as-is after pulling 0.9.0 — you must update several components.

### At a glance

- **Java build environment** → OpenJDK **21** and Gradle **9** are now required.
- **Deep Learning runtime** → DeLFT **>= 0.4.1** TensorFlow **2.17**, Python **3.10–3.11**.
- **Shipped models** → all DL models have been retrained. Pull the latest `grobid-home/models/` contents.
- **Custom-trained DL models** → **must be retrained** against the new DeLFT / TensorFlow versions (>= 0.4.1).
- **Obsolete models removed** → ELMo models and architecture that had a better drop-in version.

### Build environment: JDK 21 and Gradle 9

GROBID 0.9.0 requires **OpenJDK 21** to build and run. Previous versions supported JDK 11–17.

If your system still has an older JDK, see the installation instructions in [Build from source](Install-Grobid.md#java-development-kit-jdk). On macOS with Homebrew:

```bash
brew install openjdk@21
export JAVA_HOME=$(brew --prefix)/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

The Gradle wrapper is bundled in the repository, so you do not need to install Gradle yourself — just make sure `./gradlew` uses a JDK 21 runtime.

### Deep Learning runtime: DeLFT, TensorFlow, Python

The Deep Learning stack has been upgraded:

| Component                  | 0.8.2   | 0.9.0                            |
|----------------------------|---------|----------------------------------|
| DeLFT                      | 0.3.4   | **>= 0.4.1** (tested with 0.4.4) |
| TensorFlow                 | 2.9.x   | **2.17**                         |
| Python                     | 3.7–3.8 | **3.10–3.11**                    |
| JEP (Java Embedded Python) | 4.0.1   | **4.3.1**                        |

If you use Deep Learning models locally (not through the Docker image), you need to reinstall DeLFT in a fresh Python environment matching these versions. Follow the updated instructions in [Deep Learning models](Deep-Learning-models.md#getting-started-with-deep-learning).

!!! warning "Old DL model files will not load"
    Model files saved with earlier DeLFT / TensorFlow versions are **not compatible** with DeLFT 0.4.x + TensorFlow 2.17. Attempting to load them will fail at runtime. This applies to both the shipped models (automatically updated when you pull the new code) and to any custom-trained models.

### Shipped models have been retrained

The models shipped under `grobid-home/models/` have all been retrained on expanded training data and against the new DeLFT / TensorFlow versions. When upgrading from source, make sure you pull the latest `grobid-home/models/` contents along with the code — the old model files will not work with the new runtime, and mixing old models with new code will degrade extraction quality even when it does not crash.

See the [0.9.0 CHANGELOG entry](https://github.com/grobidOrg/grobid/blob/master/CHANGELOG.md) for the full list of retrained models and training-data expansions.

### Custom-trained models must be retrained

If you maintain your own custom-trained GROBID models (for a specific publisher, language, or domain), you must retrain them before using them with 0.9.0.

**Deep Learning (DeLFT) models** — required.

  Custom DL models trained against older DeLFT / TensorFlow versions will not load under the 0.9.0 runtime. You must retrain them using DeLFT >= 0.4.1 (0.4.4 recommended) and TensorFlow 2.17. The retraining process itself is unchanged — see [Training and evaluating models](Training-the-models-of-Grobid.md) for the workflow.

!!! tip "Keep your training data version-controlled"
    Retraining is only as painful as restoring your annotated training corpus. If you have not already done so, keep your custom training data under version control separately from the model binaries — this makes future upgrades straightforward.

See [Training and evaluating models](Training-the-models-of-Grobid.md) for the complete list of training tasks and options.

### Obsolete models removed

Several obsolete and unused models were removed in 0.9.0 ([#1367](https://github.com/grobidOrg/grobid/pull/1367)). If your `grobid-home/config/grobid.yaml` still references removed models, GROBID will fail to start. Compare your configuration against the current `grobid-home/config/grobid.yaml` from the 0.9.0 release and remove any stale model entries.

### Other notable changes

- **Consolidation**: the Crossref integration was revised — review the [Consolidation guide](Consolidation.md) if you rely on consolidation-specific behavior.
- **Default TEI output**: consolidated bibliographic references and header fields are now marked explicitly in the TEI output. Downstream consumers that parse TEI may want to take advantage of this.

For the full list of changes, see the [0.9.0 CHANGELOG entry](https://github.com/grobidOrg/grobid/blob/master/CHANGELOG.md).

## Verifying your upgrade

After upgrading, a quick end-to-end smoke test:

1. Start the service: `./gradlew :grobid-service:run` (or `docker run ... grobid/grobid:0.9.0-full`).
2. Open <http://localhost:8070> — the landing page now shows version and revision information, so you can confirm you are on 0.9.0.
3. Check `/api/health` — it now fails early when models are only partially initialized, so a green status is a stronger signal than before.
4. Process a known PDF through the `/api/processFulltextDocument` endpoint and compare the output against a pre-upgrade baseline.
