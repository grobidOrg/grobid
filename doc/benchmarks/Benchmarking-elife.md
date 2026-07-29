# Benchmarking eLife

## General

This is the end-to-end benchmarking result for GROBID version **0.9.0** against the `eLife` test set, see
the [End-to-end evaluation](../End-to-end-evaluation.md) page for explanations and for reproducing this evaluation.

The following end-to-end results are using:

- **BidLSTM_ChainCRF_FEATURES** as sequence labeling for the header model

- **BidLSTM_ChainCRF_FEATURES** as sequence labeling for the reference-segmenter model

- **BidLSTM-CRF-FEATURES** as sequence labeling for the citation model

- **BidLSTM_CRF_FEATURES** as sequence labeling for the affiliation-address model

- **CRF Wapiti** as sequence labelling engine for all other models.

Header extractions are consolidated by default with [biblio-glutton](https://github.com/kermitt2/biblio-glutton)
service (the results with CrossRef REST API as consolidation service should be similar but much slower).

Other versions of these benchmarks with variants and **Deep Learning models** (e.g. newer master snapshots) are
available [here](https://github.com/kermitt2/grobid/tree/master/grobid-trainer/doc). Note that Deep Learning models
might provide higher accuracy, but at the cost of slower runtime and more expensive CPU/GPU resources.

Evaluation on 984 PDF preprints out of 984 (no failure).

Runtime for processing 984 PDF: **1131** seconds (1.15 seconds per PDF file) on Ubuntu 22.04, 16 CPU (32 threads), 128GB
RAM and with a GeForce GTX 1080 Ti GPU.

Note: with CRF only models runtime is 492s (0.50 seconds per PDF) with 4 CPU, 8 threads.

## Header metadata

Evaluation on 984 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 8.65      | 8.33      | 8.49      | 984     |
| affiliation_linked          | 2.47      | 2.86      | 2.65      | 981     |
| authors                     | 78.28     | 77.72     | 78        | 983     |
| first_author                | 93.95     | 93.38     | 93.67     | 982     |
| title                       | 88.8      | 87.8      | 88.3      | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **18.25** | **20.29** | **19.21** | 4914    |
| all fields (macro avg.)     | 54.43     | 54.02     | 54.22     | 4914    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 21.52     | 20.73     | 21.12     | 984     |
| affiliation_linked          | 68.56     | 79.52     | 73.63     | 981     |
| authors                     | 78.69     | 78.13     | 78.41     | 983     |
| first_author                | 93.95     | 93.38     | 93.67     | 982     |
| title                       | 95.79     | 94.72     | 95.25     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **69.59** | **77.39** | **73.29** | 4914    |
| all fields (macro avg.)     | 71.7      | 73.29     | 72.41     | 4914    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 46.73     | 45.02     | 45.86     | 984     |
| affiliation_linked          | 71.67     | 83.12     | 76.97     | 981     |
| authors                     | 89.96     | 89.32     | 89.64     | 983     |
| first_author                | 94.26     | 93.69     | 93.97     | 982     |
| title                       | 97.23     | 96.14     | 96.68     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **74.23** | **82.55** | **78.17** | 4914    |
| all fields (macro avg.)     | 79.97     | 81.46     | 80.62     | 4914    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 43.46     | 41.87     | 42.65     | 984     |
| affiliation_linked          | 70.03     | 81.22     | 75.21     | 981     |
| authors                     | 83.4      | 82.81     | 83.1      | 983     |
| first_author                | 93.95     | 93.38     | 93.67     | 982     |
| title                       | 97.23     | 96.14     | 96.68     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **72.38** | **80.49** | **76.22** | 4914    |
| all fields (macro avg.)     | 77.61     | 79.08     | 78.26     | 4914    |

Note: the "affiliation_linked" field above is a linking-aware metric (each author is paired with its gold counterpart
and their attached affiliations compared). Its support column reports the number of articles the metric is computed
from (those with at least one explicit gold affiliation link), while precision/recall/F1 are measured over the
individual author-affiliation links.
Only authors whose gold affiliation link is explicit are scored; affiliations encoded purely positionally in the gold (
no xref/@rid and no nested aff) are out of scope, not counted as misses.
Ground truth: single-affiliation papers (exactly one <aff>) have been completed by linking every author to that sole
affiliation (~1,649 authors across PMC, bioRxiv and PLOS). Still to be done: multi-affiliation papers that encode the
author-to-affiliation mapping only positionally, which require the PDF superscripts to disambiguate.

#### Instance-level results

```
Total expected instances: 	984
Total correct instances: 	72 (strict)
Total correct instances: 	199 (soft)
Total correct instances: 	382 (Levenshtein)
Total correct instances: 	334 (ObservedRatcliffObershelp)

Instance-level recall:	7.32	(strict)
Instance-level recall:	20.22	(soft)
Instance-level recall:	38.82	(Levenshtein)
Instance-level recall:	33.94	(RatcliffObershelp)
```

## Citation metadata

Evaluation on 984 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 79.71     | 78.25     | 78.97     | 63265   |
| date                        | 96.01     | 93.69     | 94.84     | 63662   |
| first_author                | 94.85     | 93.08     | 93.95     | 63265   |
| inTitle                     | 95.52     | 94.09     | 94.8      | 63213   |
| issue                       | 1.52      | 81.25     | 2.99      | 16      |
| page                        | 95.73     | 94.68     | 95.2      | 53375   |
| title                       | 90.28     | 90.41     | 90.34     | 62044   |
| volume                      | 97.82     | 98.09     | 97.96     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.59** | **91.66** | **92.12** | 429889  |
| all fields (macro avg.)     | 81.43     | 90.44     | 81.13     | 429889  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 79.85     | 78.38     | 79.11     | 63265   |
| date                        | 96.01     | 93.69     | 94.84     | 63662   |
| first_author                | 94.93     | 93.16     | 94.04     | 63265   |
| inTitle                     | 96        | 94.56     | 95.28     | 63213   |
| issue                       | 1.52      | 81.25     | 2.99      | 16      |
| page                        | 95.73     | 94.68     | 95.2      | 53375   |
| title                       | 95.93     | 96.08     | 96.01     | 62044   |
| volume                      | 97.82     | 98.09     | 97.96     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.52** | **92.58** | **93.05** | 429889  |
| all fields (macro avg.)     | 82.22     | 91.24     | 81.93     | 429889  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 93.49     | 91.77     | 92.62     | 63265   |
| date                        | 96.01     | 93.69     | 94.84     | 63662   |
| first_author                | 95.38     | 93.59     | 94.48     | 63265   |
| inTitle                     | 96.62     | 95.16     | 95.88     | 63213   |
| issue                       | 1.52      | 81.25     | 2.99      | 16      |
| page                        | 95.73     | 94.68     | 95.2      | 53375   |
| title                       | 97.69     | 97.84     | 97.77     | 62044   |
| volume                      | 97.82     | 98.09     | 97.96     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **95.92** | **94.96** | **95.44** | 429889  |
| all fields (macro avg.)     | 84.28     | 93.26     | 83.97     | 429889  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 87.01     | 85.42     | 86.2      | 63265   |
| date                        | 96.01     | 93.69     | 94.84     | 63662   |
| first_author                | 94.86     | 93.09     | 93.97     | 63265   |
| inTitle                     | 96.02     | 94.57     | 95.29     | 63213   |
| issue                       | 1.52      | 81.25     | 2.99      | 16      |
| page                        | 95.73     | 94.68     | 95.2      | 53375   |
| title                       | 97.53     | 97.69     | 97.61     | 62044   |
| volume                      | 97.82     | 98.09     | 97.96     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **94.79** | **93.84** | **94.31** | 429889  |
| all fields (macro avg.)     | 83.31     | 92.31     | 83.01     | 429889  |

#### Instance-level results

```
Total expected instances: 		63664
Total extracted instances: 		64961
Total correct instances: 		41722 (strict)
Total correct instances: 		44520 (soft)
Total correct instances: 		52180 (Levenshtein)
Total correct instances: 		48704 (RatcliffObershelp)

Instance-level precision:	64.23 (strict)
Instance-level precision:	68.53 (soft)
Instance-level precision:	80.33 (Levenshtein)
Instance-level precision:	74.97 (RatcliffObershelp)

Instance-level recall:	65.53	(strict)
Instance-level recall:	69.93	(soft)
Instance-level recall:	81.96	(Levenshtein)
Instance-level recall:	76.5	(RatcliffObershelp)

Instance-level f-score:	64.87 (strict)
Instance-level f-score:	69.22 (soft)
Instance-level f-score:	81.14 (Levenshtein)
Instance-level f-score:	75.73 (RatcliffObershelp)

Matching 1 :	58480

Matching 2 :	953

Matching 3 :	1235

Matching 4 :	389

Total matches :	61057
```

#### Citation context resolution

```

Total expected references: 	 63664 - 64.7 references per article
Total predicted references: 	 64961 - 66.02 references per article

Total expected citation contexts: 	 109022 - 110.79 citation contexts per article
Total predicted citation contexts: 	 98096 - 99.69 citation contexts per article

Total correct predicted citation contexts: 	 94362 - 95.9 citation contexts per article
Total wrong predicted citation contexts: 	 3734 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 96.19
Recall citation contexts: 	 86.55
fscore citation contexts: 	 91.12
```

## Fulltext structures

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from
the actual PDF content and can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the structure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 984 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 25.61     | 27.01     | 26.29     | 585     |
| figure_title                | 0.1       | 0.03      | 0.05      | 31718   |
| funding_stmt                | 6.24      | 28.77     | 10.26     | 921     |
| reference_citation          | 57.07     | 54.91     | 55.97     | 108949  |
| reference_figure            | 59.47     | 50.95     | 54.88     | 68926   |
| reference_table             | 72.22     | 72.07     | 72.15     | 2381    |
| section_title               | 82.21     | 77.85     | 79.97     | 21831   |
| table_title                 | 0         | 0         | 0         | 1925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **56.27** | **48.09** | **51.86** | 237236  |
| all fields (macro avg.)     | 37.87     | 38.95     | 37.45     | 237236  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 35.82     | 37.78     | 36.77     | 585     |
| figure_title                | 49.4      | 16.15     | 24.34     | 31718   |
| funding_stmt                | 6.24      | 28.77     | 10.26     | 921     |
| reference_citation          | 93.85     | 90.3      | 92.04     | 108949  |
| reference_figure            | 59.77     | 51.21     | 55.16     | 68926   |
| reference_table             | 72.31     | 72.15     | 72.23     | 2381    |
| section_title               | 83.22     | 78.81     | 80.96     | 21831   |
| table_title                 | 93.08     | 27.95     | 42.99     | 1925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **78.3**  | **66.91** | **72.16** | 237236  |
| all fields (macro avg.)     | 61.71     | 50.39     | 51.84     | 237236  |

**Document-level ratio results**

| label                       | precision | recall  | f1        | support |
|-----------------------------|-----------|---------|-----------|---------|
| availability_stmt           | 93.48     | 105.47  | 99.12     | 585     |
|                             |           |         |           |         |
| **all fields (micro avg.)** | **93.48** | **100** | **96.63** | 585     |
| all fields (macro avg.)     | 93.48     | 100     | 99.12     | 585     |

Evaluation metrics produced in 200.023 seconds


