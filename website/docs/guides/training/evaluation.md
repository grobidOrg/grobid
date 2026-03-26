---
title: "Evaluation"
description: "Run benchmarks and interpret metrics"
sidebar_position: 5
---

# Evaluation

Use this page when you want to evaluate GROBID beyond one model and measure end-to-end extraction quality on real PDF/XML datasets.

## Model-level vs end-to-end evaluation

There are two different evaluation layers:

- model-level evaluation on annotated training data
- end-to-end evaluation over full PDF extraction pipelines

End-to-end evaluation is the more realistic one for users because it includes:

- PDF parsing noise
- layout recovery errors
- cascade error propagation
- metadata normalization differences

## Main datasets

The main public evaluation corpora are available through Zenodo:

- `PMC_sample_1943`
- `biorxiv-10k-test-2000`
- `PLOS_1000`
- `eLife_984`

The old docs also note that some datasets include extra annotations for structures such as funding or availability statements.

## Expected directory layout

The evaluator expects one directory per article containing:

- the PDF
- a gold XML file

Typical pattern:

```text
root/
  article1/
    article1.pdf
    article1.nxml
    article1.pub2tei.tei.xml
  article2/
    article2.pdf
    article2.nxml
    article2.pub2tei.tei.xml
```

## Running JATS-based evaluation

From the repo root:

```bash
./gradlew jatsEval -Pp2t=ABS_PATH_TO_JATS_DATASET/DATASET -Prun=1
```

Set `-Prun=0` if you want to evaluate already-generated outputs without re-running GROBID.

You can also evaluate a fraction of files:

```bash
./gradlew jatsEval -Pp2t=ABS_PATH_TO_JATS_DATASET/DATASET -Prun=0 -PfileRatio=0.1
```

## Running Pub2TEI-based evaluation

```bash
./gradlew teiEval -Pp2t=ABS_PATH_TO_TEI/ -Prun=1
```

Again, `-Prun=0` skips rerunning GROBID when outputs already exist.

## How to interpret the results

GROBID reports:

- precision
- recall
- F1-score
- field-level and instance-level views

The older methodology also compares several matching styles for textual fields:

- strict
- soft
- relative Levenshtein
- Ratcliff/Obershelp

These variants help show whether mismatches are structural or just formatting-level differences.

## Important limits

Do not treat these numbers as perfect absolute truth.

Known issues include:

- JATS/NLM can encode the same information in multiple ways
- some gold XML references are only raw strings, which penalizes structured extraction unfairly
- citation intervals in gold XML may omit intermediate references that GROBID expands correctly
- PDF/XML character encoding differences can produce artificial mismatches
- some fulltext annotations are too inconsistent for strong absolute claims

That is why these evaluations are best used as:

- regression tracking
- relative comparison over time
- task-specific investigation

## Related pages

- [Training Overview](./training-overview)
- [Training Workflow](./training-workflow)
- [Benchmarks Overview](../../reference/benchmarks/overview)
