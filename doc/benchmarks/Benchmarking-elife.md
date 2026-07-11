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
| abstract                    | 8.66      | 8.33      | 8.49      | 984     |
| affiliation_linked          | 1.92      | 2.09      | 2         | 981     |
| authors                     | 78.18     | 77.62     | 77.9      | 983     |
| first_author                | 93.95     | 93.38     | 93.67     | 982     |
| title                       | 88.8      | 87.8      | 88.3      | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **18.65** | **19.72** | **19.17** | 4914    |
| all fields (macro avg.)     | 54.3      | 53.85     | 54.07     | 4914    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 21.54     | 20.73     | 21.13     | 984     |
| affiliation_linked          | 68.98     | 74.83     | 71.79     | 981     |
| authors                     | 78.59     | 78.03     | 78.31     | 983     |
| first_author                | 93.95     | 93.38     | 93.67     | 982     |
| title                       | 95.79     | 94.72     | 95.25     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **69.96** | **73.98** | **71.91** | 4914    |
| all fields (macro avg.)     | 71.77     | 72.34     | 72.03     | 4914    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 46.67     | 44.92     | 45.78     | 984     |
| affiliation_linked          | 73.26     | 79.47     | 76.24     | 981     |
| authors                     | 89.86     | 89.22     | 89.54     | 983     |
| first_author                | 94.26     | 93.69     | 93.97     | 982     |
| title                       | 97.23     | 96.14     | 96.68     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **75.55** | **79.88** | **77.65** | 4914    |
| all fields (macro avg.)     | 80.26     | 80.69     | 80.44     | 4914    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 43.4      | 41.77     | 42.57     | 984     |
| affiliation_linked          | 71.08     | 77.11     | 73.97     | 981     |
| authors                     | 83.3      | 82.71     | 83        | 983     |
| first_author                | 93.95     | 93.38     | 93.67     | 982     |
| title                       | 97.23     | 96.14     | 96.68     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **73.28** | **77.48** | **75.32** | 4914    |
| all fields (macro avg.)     | 77.79     | 78.22     | 77.98     | 4914    |

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
| authors                     | 79.72     | 78.41     | 79.06     | 63265   |
| date                        | 96.01     | 93.88     | 94.93     | 63662   |
| first_author                | 94.86     | 93.26     | 94.05     | 63265   |
| inTitle                     | 95.52     | 94.27     | 94.89     | 63213   |
| issue                       | 1.52      | 81.25     | 2.99      | 16      |
| page                        | 95.73     | 94.88     | 95.3      | 53375   |
| title                       | 90.27     | 90.59     | 90.43     | 62044   |
| volume                      | 97.83     | 98.28     | 98.06     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.59** | **91.84** | **92.22** | 429889  |
| all fields (macro avg.)     | 81.43     | 90.6      | 81.21     | 429889  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 79.86     | 78.55     | 79.2      | 63265   |
| date                        | 96.01     | 93.88     | 94.93     | 63662   |
| first_author                | 94.94     | 93.35     | 94.14     | 63265   |
| inTitle                     | 96.01     | 94.74     | 95.37     | 63213   |
| issue                       | 1.52      | 81.25     | 2.99      | 16      |
| page                        | 95.73     | 94.88     | 95.3      | 53375   |
| title                       | 95.93     | 96.26     | 96.09     | 62044   |
| volume                      | 97.83     | 98.28     | 98.06     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.52** | **92.76** | **93.14** | 429889  |
| all fields (macro avg.)     | 82.23     | 91.4      | 82.01     | 429889  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 93.49     | 91.95     | 92.72     | 63265   |
| date                        | 96.01     | 93.88     | 94.93     | 63662   |
| first_author                | 95.38     | 93.78     | 94.57     | 63265   |
| inTitle                     | 96.62     | 95.35     | 95.98     | 63213   |
| issue                       | 1.52      | 81.25     | 2.99      | 16      |
| page                        | 95.73     | 94.88     | 95.3      | 53375   |
| title                       | 97.69     | 98.03     | 97.86     | 62044   |
| volume                      | 97.83     | 98.28     | 98.06     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **95.92** | **95.14** | **95.53** | 429889  |
| all fields (macro avg.)     | 84.28     | 93.42     | 84.05     | 429889  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 87.02     | 85.59     | 86.3      | 63265   |
| date                        | 96.01     | 93.88     | 94.93     | 63662   |
| first_author                | 94.87     | 93.28     | 94.07     | 63265   |
| inTitle                     | 96.02     | 94.76     | 95.38     | 63213   |
| issue                       | 1.52      | 81.25     | 2.99      | 16      |
| page                        | 95.73     | 94.88     | 95.3      | 53375   |
| title                       | 97.53     | 97.87     | 97.7      | 62044   |
| volume                      | 97.83     | 98.28     | 98.06     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **94.79** | **94.02** | **94.41** | 429889  |
| all fields (macro avg.)     | 83.32     | 92.47     | 83.09     | 429889  |

#### Instance-level results

```
Total expected instances: 		63664
Total extracted instances: 		65092
Total correct instances: 		41810 (strict)
Total correct instances: 		44612 (soft)
Total correct instances: 		52284 (Levenshtein)
Total correct instances: 		48804 (RatcliffObershelp)

Instance-level precision:	64.23 (strict)
Instance-level precision:	68.54 (soft)
Instance-level precision:	80.32 (Levenshtein)
Instance-level precision:	74.98 (RatcliffObershelp)

Instance-level recall:	65.67	(strict)
Instance-level recall:	70.07	(soft)
Instance-level recall:	82.12	(Levenshtein)
Instance-level recall:	76.66	(RatcliffObershelp)

Instance-level f-score:	64.94 (strict)
Instance-level f-score:	69.3 (soft)
Instance-level f-score:	81.21 (Levenshtein)
Instance-level f-score:	75.81 (RatcliffObershelp)

Matching 1 :	58590

Matching 2 :	959

Matching 3 :	1235

Matching 4 :	391

Total matches :	61175
```

#### Citation context resolution

```

Total expected references: 	 63664 - 64.7 references per article
Total predicted references: 	 65092 - 66.15 references per article

Total expected citation contexts: 	 109022 - 110.79 citation contexts per article
Total predicted citation contexts: 	 99898 - 101.52 citation contexts per article

Total correct predicted citation contexts: 	 96019 - 97.58 citation contexts per article
Total wrong predicted citation contexts: 	 3879 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 96.12
Recall citation contexts: 	 88.07
fscore citation contexts: 	 91.92
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
| availability_stmt           | 25.37     | 26.67     | 26        | 585     |
| figure_title                | 0.07      | 0.02      | 0.03      | 31718   |
| funding_stmt                | 6.04      | 26.49     | 9.84      | 921     |
| reference_citation          | 57.12     | 55.98     | 56.54     | 108949  |
| reference_figure            | 58.43     | 51.08     | 54.51     | 68926   |
| reference_table             | 71.35     | 73.41     | 72.37     | 2381    |
| section_title               | 83.33     | 77.3      | 80.2      | 21831   |
| table_title                 | 0         | 0         | 0         | 1925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **56.16** | **48.57** | **52.09** | 237236  |
| all fields (macro avg.)     | 37.71     | 38.87     | 37.44     | 237236  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 36.1      | 37.95     | 37        | 585     |
| figure_title                | 49.82     | 16.06     | 24.29     | 31718   |
| funding_stmt                | 6.04      | 26.49     | 9.84      | 921     |
| reference_citation          | 93.68     | 91.81     | 92.74     | 108949  |
| reference_figure            | 58.71     | 51.33     | 54.77     | 68926   |
| reference_table             | 71.43     | 73.5      | 72.45     | 2381    |
| section_title               | 84.38     | 78.26     | 81.21     | 21831   |
| table_title                 | 95.08     | 28.1      | 43.38     | 1925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **78.15** | **67.59** | **72.48** | 237236  |
| all fields (macro avg.)     | 61.9      | 50.44     | 51.96     | 237236  |

**Document-level ratio results**

| label                       | precision | recall  | f1       | support |
|-----------------------------|-----------|---------|----------|---------|
| availability_stmt           | 93.61     | 105.13  | 99.03    | 585     |
|                             |           |         |          |         |
| **all fields (micro avg.)** | **93.61** | **100** | **96.7** | 585     |
| all fields (macro avg.)     | 93.61     | 100     | 99.03    | 585     |

Evaluation metrics produced in 199.998 seconds
