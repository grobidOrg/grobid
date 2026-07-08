<h1>End-to-end evaluation</h1>

Individual models can be evaluated as explained in [Training the different models of Grobid](Training-the-models-of-Grobid.md).

For an end-to-end evaluation, covering the whole extraction process from the parsing of PDF to the end result of the cascading of several sequence labelling models, GROBID includes two possible evaluation progresses:

* against JATS-encoded (NLM) articles, such as [PubMed Central](http://www.ncbi.nlm.nih.gov/pmc), [bioRxiv](https://www.biorxiv.org), [PLOS](https://plos.org/ ) or [eLife](https://elifesciences.org/ ). For example, PubMed Central provides both PDF and fulltext XML files in the [NLM](http://www.ncbi.nlm.nih.gov/pmc/pmcdoc/tagging-guidelines/article/style.html) format. Keeping in mind some limits described below, it is possible to estimate the ability of Grobid to extract and normalize the content of the PDF documents for matching the quality of the NLM file. bioRxiv is used in Grobid to evaluate more precisely performance on preprint articles.

* against TEI documents produced by [Pub2TEI](https://github.com/kermitt2/Pub2TEI). Pub2TEI is a set of XSLT that permit to transform various _native_ XML publishers (including Elsevier, Wiley, Springer, etc. XML formats) into a common TEI format. This TEI format can be used as ground-truth structure information for evaluating GROBID output, keeping in mind some limits described below.

For actual benchmarks, see the [Benchmarking page](benchmarks/Benchmarking.md). We describe below the datasets and how to run the benchmarks.

## Datasets

The corpus used for the end-to-end evaluation of Grobid are all available in a single Hugging Face dataset: [https://huggingface.co/datasets/sciencialab/grobid-evaluation](https://huggingface.co/datasets/sciencialab/grobid-evaluation). Some of these datasets have been further annotated to make the evaluation of certain sub-structures possible (in particular code and data availability sections & funding sections).

*Previously, these datasets were archived on Zenodo at [https://zenodo.org/record/7708580](https://zenodo.org/record/7708580). The Hugging Face repository is now the canonical source.*

These resources are originally published under CC-BY license. Our additional annotations are similarly under CC-BY. We thank NIH, bioRxiv, PLOS and eLife for making these resources Open Access and reusable.

### PubMedCentral gold-standard data

Since ages, we are evaluating GROBID using the `PMC_sample_1943` dataset compiled by Alexandru Constantin. The dataset is available at this [url](https://huggingface.co/datasets/sciencialab/grobid-evaluation) (around 1.5GB in size). The sample dataset contains 1943 articles from 1943 different journals corresponding to the latest publications from a 2011 snapshot.

Any similar PubMed Central set of articles could normally be used, as long they follow the same directory structure: one directory per article containing at least the corresponding PDF file and the reference NLM file.

We suppose in the following that the archive is decompressed under `PATH_TO_PMC/PMC_sample_1943/`.

#### Author–affiliation linking corrections

Manual/curation changes to the author→affiliation links (see [Author–affiliation linking in the gold data](#authoraffiliation-linking-in-the-gold-data)). For `PMC_sample_1943`: **965 authors across 282 files** were auto-linked from a single affiliation, **3 files** completed by forced bijection, and **37** id-less affiliations were given ids for hand-linking; **20** collaboration/consortium contributors were marked and excluded. Two files remain unrecoverable and stay unlinked (out of scope): `Cases_J_2010_Mar_9_3_76` (one author with no affiliation in the JATS *or* the PDF) and `Cryst_Growth_Des_2011_Jun_1_11(6)_2107-2111` (13 authors — the PDF lists the affiliations but prints no author superscripts).

#### Statement-section annotations (author contributions & competing interests)

This dataset is not covered, and kept out because JATS XML too messy. See [Statement sections](#statement-sections-author-contributions-and-competing-interests).

### The bioRxiv gold-standard data

For evaluation on preprint articles, we are using the balanced bioRxiv 10k dataset originally compiled with care and published by Daniel Ecer ([eLife](https://elifesciences.org)), available on [Hugging Face](https://huggingface.co/datasets/sciencialab/grobid-evaluation). More precisely we publish benchmarks using the test subset of 2000 articles. The zip archive is similar in structure to the above PMC sample 1943 dataset and further documented below.

#### Author–affiliation linking corrections

For `biorxiv-10k-test-2000`: **643 authors across 203 files** were auto-linked from a single affiliation, **9** id-less affiliations were given ids for hand-linking, and **39** collaboration contributors were marked and excluded; individual files were repaired by hand (e.g. a dangling `rid` in `050567v1`, and `244319v1` where the affiliation text itself names the author). Eight files remain unlinked (out of scope), mostly source gaps where the JATS dropped the author superscripts (e.g. `338731v1`) or the superscript points to an affiliation not printed in the document.

#### Statement-section annotations (author contributions & competing interests)

To let the evaluation distinguish these statement types, `sec-type` was added to the relevant `<sec>` elements (reference-annotation only — no PDF or document text touched): `sec-type="contribution"` on ~489 author-contribution sections and `sec-type="conflict"` on ~215 competing-interest sections, across 722 files. **Both are scored for bioRxiv** (no fields removed): these edits create the gold matched by `//sec[@sec-type="contribution"]` and `//sec[@sec-type="conflict"]`. See [Statement sections](#statement-sections-author-contributions-and-competing-interests).

### The PLOS 1000 dataset

This is a set of 1000 PLOS articles, called `PLOS_1000` and available on [Hugging Face](https://huggingface.co/datasets/sciencialab/grobid-evaluation), randomly selected from the full [PLOS Open Access collection](https://allof.plos.org/allofplos.zip). Again, for each article, the published PDF is available with the corresponding publisher JATS XML file, around 1.3GB total size.

#### Author–affiliation linking corrections

For `PLOS_1000`: PLOS encodes explicit `<xref>` affiliation links, so almost no completion was needed — **6** authors were linked under the single-real-affiliation pattern (the academic-editor affiliation excluded) plus one single-affiliation case, and **43** collaboration contributors were marked and excluded. No files remain to complete.

#### Statement-section annotations (author contributions & competing interests)

PLOS overloads `fn-type="con"` for both contributions and conflicts, so author-contribution footnotes were given `type="contribution"` (~176 across 183 files; a few `id="contrib…"` / `id="ack…"` footnotes were also typed) to disambiguate. **Only conflicts are scored for PLOS**: `contribution_stmt` is removed (contributions are attributes in the author list), while conflicts already match `//fn[@fn-type="conflict"]`. See [Statement sections](#statement-sections-author-contributions-and-competing-interests).

### eLife 984 dataset

The `eLife_984` dataset is a set of 984 articles from eLife, available on [Hugging Face](https://huggingface.co/datasets/sciencialab/grobid-evaluation), randomly selected from their [open collection available on GitHub](https://github.com/elifesciences/elife-article-xml). Every articles come with the published PDF, the publisher JATS XML file and the eLife public HTML file (as bonus, not used), all in their latest version, around 4.5G total.

#### Author–affiliation linking corrections

For `eLife_984`: eLife already encodes explicit affiliation links, so no single-affiliation completion was required. The apparent gaps were **collaboration consortiums** — e.g. 28721's "Swiss HIV Cohort Study" and 72779's "Barwon Infant Study Investigator team", whose nested member rolls were mis-read as unlinked authors — now flagged `specific-use="collaboration"` and excluded (**12** contributors), together with editorial and peer-review affiliations from `<sub-article>` and editorial-board contrib-groups. **8** id-less affiliations were given ids. No author→affiliation completion remains.

#### Statement-section annotations (author contributions & competing interests)

Competing-interest footnotes are ignored for the time being (authors contributions are not standalone text elements) — pending revisit. See [Statement sections](#statement-sections-author-contributions-and-competing-interests).

## Getting publisher gold-standard data

Some publishers release publications in XML format complementary to PDF in Open Access, allowing text mining (see for instance the dedicated subset of PMC publications). On contractual basis, it is possible to acquire native XML from mainstream publishers. Unfortunately, each publisher uses a different XML schema and covering all these formats would be a very time-consuming work. To ease the processing of these XML documents, the project [Pub2TEI](https://github.com/kermitt2/Pub2TEI) permits to transform the native XML formats of a dozen mainstream publishers into a common TEI format which is the same as the output of GROBID.

See [Pub2TEI](https://github.com/kermitt2/Pub2TEI) for converting native publisher XML into usable TEI.

## Directory structure

For running the evaluation, the tool assumes that the files are organised in a set of directory in the following way:

* a root directory containing one sub-directory per article

* each article sub-directory containing at least the PDF version and a gold XML structured version of the article (in NLM format for PubMedCentral evaluation or in TEI format for the Pub2TEI-based evaluation). See the diagram below - the name of the sub-directory and the files is free.

* extension for files generated with [Pub2TEI](https://github.com/kermitt2/Pub2TEI) is `.pub2tei.tei.xml`. Extension for NLM files is `.nxlm` (PMC) or `xml` (bioRxiv). GROBID will generate additional TEI files with extension `.fulltext.tei.xml`.

```
├── article1
│   ├── article1.pdf
│   └── article1.pub2tei.tei.xml
│   └── article1.nxml
│
└── articles2
│   ├── article2.pdf
│   └── article2.pub2tei.tei.xml
│   └── article2.nxml
...
```

## Warning on JATS/NLM format

JATS/NLM is a very loose XML format, in the sense that there are multiple ways to encode the same information. As a consequence, there are a variety of JATS flavors depending on the publisher and it is not possible to guarantee that any JATS files will be supported as gold standard dataset by the `jatsEval` process. PMC and bioRxiv JATS articles are supported, but for a larger variety of JATS files it is recommended to convert them first into TEI with [Pub2TEI](https://github.com/kermitt2/Pub2TEI) and use the `teiEval` process. [Pub2TEI](https://github.com/kermitt2/Pub2TEI) supports all the JATS/NLM variants we are aware of, and convert them into a constrained and unambiguous single TEI format without information loss.

## Configuration for evaluation

The evaluation tasks (`jatsEval`/`teiEval`) load the default `grobid-home/config/grobid.yaml`. For results comparable to the published [benchmarks](benchmarks/Benchmarking.md), that configuration must be set up as follows:

* **Consolidation via biblio-glutton** — bibliographical reference matching must use a running [biblio-glutton](https://github.com/kermitt2/biblio-glutton) service, not the default CrossRef REST API. In `grobid.yaml`, set `consolidation.service: "glutton"` and point `consolidation.glutton.url` at your glutton instance. See [Consolidation](Consolidation.md).

* **Deep Learning (DeLFT) models** — the models for which Deep Learning significantly outperforms CRF must use `engine: "delft"`: `citation`, `affiliation-address`, `reference-segmenter`, `header` and `funding-acknowledgement`. See [Deep Learning models](Deep-Learning-models.md).

A ready-made preset with both already configured is shipped as `grobid-home/config/grobid-evaluation.yaml` — copy it over `grobid.yaml` (adjusting the glutton URL) as the simplest way to satisfy the requirements.

### Pre-flight configuration check

Because a misconfigured `grobid.yaml` silently produces degraded results over a run that can take hours, a pre-flight check is provided:

```bash
> ./gradlew checkEvalConfig
```

It verifies that consolidation is set to biblio-glutton **and** that the glutton URL actually answers, and that the five models above resolve to the DeLFT engine. It prints an itemized report and exits non-zero if anything is wrong.

If you intentionally want to benchmark a non-standard configuration (for example CRF instead of DeLFT for a given model, or without glutton), run the check in **warn mode** — it still reports what differs from the recommended setup but exits 0:

```bash
> ./gradlew checkEvalConfig -PcheckMode=warn
```

The [multi-dataset runner script](#multi-dataset-runner-script) `grobid-home/scripts/run_evaluation.sh` runs this check automatically before starting and aborts if it fails (unless invoked with `--warn` or `--skip-checks`).

## Running and evaluating

### JATS encoded corpus, e.g. PubMed Central, bioRxiv, PLOS, eLife

Under ```grobid/```, the following command line is used to run and evaluate Grobid on the dataset:
```bash
> ./gradlew jatsEval -Pp2t=ABS_PATH_TO_JATS_DATASET/DATASET -Prun=1
```

Replace the absolute path and directory dataset name by the selected dataset for end-to-end evaluation, for example `PMC_sample_1943`, `biorxiv-10k-test-2000`, `PLOS_1000` or `eLife_984` (see above for downloading these datasets).

The parameters `run` indicates if GROBID has to be executed on all the PDF of the data set. The resulting TEI file will be added in each article subdirectory. If you only want to run the evaluation without re-executing Grobid on the PDF, set the parameter to 0:
```bash
> ./gradlew jatsEval -Pp2t=ABS_PATH_TO_JATS_DATASET/DATASET -Prun=0
```
It is also possible to set a ratio of evaluation data to be used expressed as a number between 0 and 1 introduced by the parameter `fileRatio`. For instance, if you want to evaluate Grobid against only 10% of the PubMedCentral files, use:
```bash
> ./gradlew jatsEval -Pp2t=ABS_PATH_TO_JATS_DATASET/DATASET -Prun=0 -PfileRatio=0.1
```

### Pub2TEI-based

Under ```grobid/```, the following command line is used to run and evaluate Grobid on the dataset:
```bash
> ./gradlew teiEval -Pp2t=ABS_PATH_TO_TEI/ -Prun=1
```
The parameters `run` indicates if GROBID has to be executed on all the PDF of the data set. The resulting GROBID TEI file will be added in each article subdirectory. If you only want to run the evaluation without re-executing Grobid on the PDF, set the parameter to 0:
```bash
> ./gradlew teiEval -Pp2t=ABS_PATH_TO_TEI/ -Prun=0
```
It is also possible to set a ratio of evaluation data to be used expressed as a number between 0 and 1 introduced by the parameter `fileRatio`. For instance, if you want to evaluate Grobid against only 10% of the Pub2TEI-produced files, use:
```bash
> ./gradlew teiEval -Pp2t=ABS_PATH_TO_TEI/ -Prun=0 -PfileRatio=0.1
```

### Multi-dataset runner script

The Gradle tasks above evaluate one dataset at a time. To benchmark GROBID over several gold corpora in one go (e.g. `PMC_sample_1943`, `biorxiv-10k-test-2000`, `PLOS_1000` and `eLife_984` together), use the wrapper script `grobid-home/scripts/run_evaluation.sh`.

It expects a single root folder containing one sub-directory per dataset, iterates (non-recursively) over the matching sub-directories, and runs `jatsEval` on each:

```
grobid-eval-datasets/            <- the root folder passed with -d
├── PMC_sample_1943/
├── biorxiv-10k-test-2000/
├── PLOS_1000/
└── eLife_984/
```

Before running anything, the script performs the [pre-flight configuration check](#pre-flight-configuration-check) and aborts if `grobid.yaml` is not correctly configured for evaluation.

For each dataset, it writes two files into the output directory (`-o`, default: current directory), named after the dataset and the report suffix (`-s`):

* `report-<dataset>-<suffix>.txt` — the captured Gradle console output of the run.
* `report-<dataset>-<suffix>.md` — the Markdown benchmark report produced by `jatsEval` (moved from `grobid-home/tmp/report.md`).

Typical invocations:

```bash
# run every dataset under the root, executing GROBID on the PDFs, with the pre-flight check
> sh grobid-home/scripts/run_evaluation.sh -d ABS_PATH_TO/grobid-eval-datasets -s master -r 1

# only re-evaluate existing TEI (do not re-run GROBID) on 10% of the files, writing reports to ./reports
> sh grobid-home/scripts/run_evaluation.sh -d ABS_PATH_TO/grobid-eval-datasets -r 0 -f 0.1 -o ./reports

# restrict to a subset of datasets with a glob pattern
> sh grobid-home/scripts/run_evaluation.sh -d ABS_PATH_TO/grobid-eval-datasets -p 'PLOS_*'

# report config issues but run anyway (e.g. benchmarking a variant configuration)
> sh grobid-home/scripts/run_evaluation.sh -d ABS_PATH_TO/grobid-eval-datasets --warn

# skip the pre-flight check entirely
> sh grobid-home/scripts/run_evaluation.sh -d ABS_PATH_TO/grobid-eval-datasets --skip-checks

# print the commands without executing them
> sh grobid-home/scripts/run_evaluation.sh -d ABS_PATH_TO/grobid-eval-datasets --dry-run
```

Options (run with `-h` for the full list):

| Option | Meaning | Default |
|---|---|---|
| `-d EVAL_ROOT` | Root folder containing one sub-directory per dataset (**required**) | — |
| `-s REPORT_SUFFIX` | Suffix appended to the report file names | `master` |
| `-r RUN` | Run GROBID on the PDFs (`1`) or only evaluate existing TEI (`0`) | `1` |
| `-f FILERATIO` | Ratio of files to evaluate, `0.0`–`1.0` | `1` |
| `-l FLAVOR` | Optional `flavor` passed to Gradle | empty |
| `-g GRADLEW_PATH` | Path to the `gradlew` executable | `./gradlew` |
| `-j JAVA_NATIVE_LIB` | Path to the LMDB native library (sets `JAVA_TOOL_OPTIONS`) | unset |
| `-o OUT_DIR` | Directory where the per-dataset reports are written | current dir |
| `-p PATTERN` | Glob pattern selecting the dataset sub-directories | `*` |
| `-k` / `--skip-checks` | Skip the pre-flight config check (or `SKIP_CHECKS=1`) | off |
| `-w` / `--warn` | Report config problems but run anyway | off |
| `-n` / `--dry-run` | Print the commands without executing them | off |

The script exits non-zero if any single dataset evaluation fails, but continues with the remaining datasets so a run over several corpora is not aborted by one failure.

## Evaluation results

The evaluation provides precision, recall and F1-score for the different fields in the header and bibliographical references. In addition, the scores are also computed at *instance* level, which means at the level of a complete header or complete citation.

An experimental evaluation for the structures of the full text body is also proposed. This is not reliable in the current state, because most of the annotations of the full texts in PudMed Central are not uniform. For instance, the numbering of the section header is sometime included in the section header annotation, sometime not. The PubMed Central annotations will need to be standardized as a pre-process for a meaningful evaluation, which is a task planned in the next releases.

## Matching techniques


The evaluation covers four different string matching techniques for textual fields, based on the existing evaluation approaches observed in the literature:

* __strict__, i.e. exact match,

* __soft__ corresponding to matching ignoring punctuations, character case and space character mismatches,

* __relative Levenshtein distance__ relative to the max length of two strings

* [__Ratcliff/Obershelp similarity__](http://xlinux.nist.gov/dads/HTML/ratcliffObershelp.html)

These matching variants only apply to textual fields, not numerical and dates fields (such as volume, issue, dates, pages).



## Limits

### Non structured information in XML "gold" data

A relatively important number of citations in the NLM files (and other native publisher XML) are encoded only as raw string, for example in the first file of the set `AAPS_J_2011_Mar_9_13(2)_230-239/12248_2011_Article_9260.nxml`:

```xml
	  ...
      <ref id="CR9">
        <label>9.</label>
        <mixed-citation publication-type="other">Pira&#xF1;a and PCluster: a modeling environment and cluster infrastructure for NONMEM. Keizer RJ, van Benten M, Beijnen JH, Schellens JH, Huitema AD. Comput Methods Programs Biomed. 2011;101(1):72&#x2013;9.</mixed-citation>
      </ref>
      <ref id="CR10">
        <label>10.</label>
        <mixed-citation publication-type="other">Holford, N. VPC, the visual predictive check&#x2014;superiority to standard diagnostic (Rorschach) plots. In: PAGE 2005. 2005.</mixed-citation>
      </ref>
	  ...
```
(this file contains for instance 3 non-encoded citations out of 18)

As a consequence, the fields extracted by GROBID will not match any reference 'expected' values and will all be considered as false positive. The scores for the citation structures are thus lower than the actual performance of the system.

### Non encoded information in XML "gold" data for intervals

In the reference NLM/JATS files from PMC, in the case of range citation callout, e.g. __[1-4]__, usually only the label number visible in the text are annotated. For example, the following text

```
Recent studies have described the use of contrast material with high iodine content (370 mg I/ml or 400 mg I/ml) for coronary CT angiography (CCTA) (1 – 4).
```

which is annotated as follow:

```xml
Recent studies have described the use of contrast material with high iodine content (370 mg I/ml or 400 mg I/ml) for coronary CT angiography (CCTA) (<xref ref-type="bibr" rid="b1">1</xref>&#x2013;<xref ref-type="bibr" rid="b4">4</xref>).
```

The references 2 and 3 are thus missing.

GROBID expends intervals and will likely identify and match these "intermediary" callouts (including 2 and 3 in the above example). However these additional correct extractions and matching from GROBID will be counted as false positive in the evaluation because missing from the "gold" data.


### Author–affiliation linking in the gold data

The end-to-end evaluation includes an `affiliation_linked` metric: each extracted author is paired with its gold counterpart (by normalised surname, with forename initial as tie-break) and the affiliations attached to each are compared. Scoring an author requires a *machine-readable* author→affiliation link in the gold JATS — an `<xref ref-type="aff" rid="..."/>` placed **inside** the `<contrib contrib-type="author">`, or an `<aff>` nested directly in that contrib. The `<aff>` element itself lives as a sibling inside the `<contrib-group>` (or is referenced by id); it is the `xref` *inside the contrib* that establishes the link.

In practice, publisher JATS frequently encodes the association only *positionally* — the author's printed superscript is resolved by the PDF layout — or drops it entirely during conversion. Authors with no resolvable link are treated as **out of scope**: they are skipped, not counted as missed. The raw metric therefore understates performance on these corpora unless the gold links are completed.

To make the metric meaningful, the gold affiliation links were curated with a tiered pipeline (scripts under `doc/affiliation-triage/`), from safe/automatic to manual:

1. **single affiliation** — a contributor group with exactly one `<aff>`: every author is linked to it;
2. **single real affiliation** — the same, once editor affiliations are excluded (the PLOS academic-editor pattern);
3. **forced bijection** — exactly one unlinked author and exactly one unreferenced affiliation;
4. **unlabeled default** — a group whose single *unlabeled* `<aff>` is the shared default for every author that carries no superscript;
5. **id backfill** — an `id` is added to id-less `<aff>` elements so the remainder can be hand-linked;
6. **manual** — the author→affiliation mapping is read from the PDF superscripts and added by hand.

Two classes of contributor are deliberately **excluded**, not missing:

- **Collaboration / consortium group authors** (`<collab>`, e.g. eLife 28721's "Swiss HIV Cohort Study") are flagged `specific-use="collaboration"` on their wrapping `<contrib>` and skipped — they are group authors, not individually affiliated, and their nested member rolls are never scored.
- **Editors and reviewers** — eLife peer-review `<sub-article>` blocks and editorial-board `<contrib-group content-type="section">` — are out of scope for author-affiliation scoring.

Finally, some links are **genuinely unrecoverable** and are left unlinked, because the source itself is incomplete:

- an author printed with **no superscript anywhere** (neither JATS nor PDF), so no affiliation is asserted for them;
- a superscript that **points to an affiliation that was never printed** in the document.

These are source-data gaps that no author→affiliation metric can score; they remain documented known gaps rather than extraction errors. The manual-completion checklist and the remaining gaps are tracked in `doc/affiliation-triage/manual-ground-truth-todo.md`.

### Statement sections: author contributions and competing interests

GROBID extracts several *statement* sections from the back matter — funding, data/code availability, author contributions and competing (conflict of) interests. Two things determine whether each is evaluated: the evaluator removes some statement fields per corpus (`EndToEndEvaluation`), and the gold is annotated (commit `1e43b54`) so that the sections match — or deliberately *don't* match — the XPaths of the scored fields (`FieldSpecification`). These are reference-annotation edits only; no PDF or actual document text is touched. **The raw XML delta is therefore not the same as what gets scored.**

What is actually scored, per dataset:

| Dataset | `contribution_stmt` | `conflict_stmt` |
|---|---|---|
| bioRxiv | scored | scored |
| PLOS | not scored (removed) | scored |
| eLife | not scored (removed) | not scored (removed) |
| PMC | not scored (removed) | not scored (removed) |

The removals and the reasons given in the evaluator:

- **eLife** — both removed: contributions are not standalone text elements (a combination of the author and text), too hard to align "for the time being" — pending revisit.
- **PMC** — both removed (along with availability and funding): not covered, and keeping them would make metrics non-comparable over time.
- **PLOS** — contributions removed (they are attributes in the author list, not text elements); conflicts remain scored.
- **bioRxiv** — nothing removed: both scored.

How each annotation connects to that — the XML edit and the field-removal are two halves of one intent: each edit makes the gold match (or not match) an XPath for a field that is (or isn't) scored in that corpus.

- **bioRxiv** *enables both*: `sec-type="contribution"` (~489) and `sec-type="conflict"` (~215) make the sections match `//sec[@sec-type="contribution"]` and `//sec[@sec-type="conflict"]` — nothing is removed, so this is the gold that gets scored.
- **PLOS** *disambiguates*: PLOS overloads `fn-type="con"`; conflicts already match `//fn[@fn-type="conflict"]` (scored), and `type="contribution"` is added to the contribution footnotes to match `//fn[@type="contribution"]` — but since `contribution_stmt` is removed for PLOS, only conflicts are scored.
- **eLife**: ignored.
- **PMC**: ignored.

### Character encoding and glyphs

XML and PDF content frequently contain many differences at character-level. This is due to PDF which tend to use particular glyphs for enhancing visual rendering. Those special glyths are often loaded in the PDF itself and uses particular unicode not matching unicode of characters in XML. Similarly some special characters are expressed with fonts (for instance using a Greek font to render a λ, using the unicode of the letter _l_).

### Ordering and presentation variants of structures and sub-structures

The order of some structures might also be changed from the logical representation (XML) to the particular PDF presentation base on (unknown) style transformation.

Still related to style rendering, text can be post-processed. For instance, in a bibliographical reference, forenames can be present in full form in the XML, but shorten to initials only in the PDF.

### Evaluation criteria

The tool uses currently rather strict evaluation criteria. For instance, `authors` field is considered correct only if the whole set of authors, including the order of authors, match. More partial and fine-grained matching is not implemented yet.

*Given these limit, this evaluation cannot be considered currently as a reliable absolute evaluation (how good GROBID will extract valid and usable structures from PDF), but rather as a way to keep track of progress from one version of GROBID to another one and avoid regressions.*

