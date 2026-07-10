# Benchmarking biorXiv

## General

This is the end-to-end benchmarking result for GROBID version **0.9.0** against the `bioRxiv` test set (
`biorxiv-10k-test-2000`), see the [End-to-end evaluation](../End-to-end-evaluation.md) page for explanations and for
reproducing this evaluation.

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

Evaluation on 2000 PDF preprints out of 2000 (no failure).

Runtime for processing 2000 PDF: **1713** seconds (0.85 seconds per PDF file) on Ubuntu 22.04, 16 CPU (32 threads),
128GB RAM and with a GeForce GTX 1080 Ti GPU.

Note: with CRF only models runtime is 622s (0.31 second per PDF) with 4 CPU, 8 threads.

## Header metadata

Evaluation on 1998 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 2.31      | 2.26      | 2.29      | 1988    |
| affiliation_linked          | 0.83      | 0.81      | 0.82      | 1961    |
| authors                     | 84.84     | 84.38     | 84.61     | 1997    |
| first_author                | 96.73     | 96.29     | 96.51     | 1995    |
| keywords                    | 57.28     | 57.28     | 57.28     | 838     |
| title                       | 77.25     | 76.48     | 76.86     | 1998    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **23.58** | **23.04** | **23.31** | 10777   |
| all fields (macro avg.)     | 53.21     | 52.92     | 53.06     | 10777   |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 59.52     | 58.35     | 58.93     | 1988    |
| affiliation_linked          | 72.88     | 70.63     | 71.74     | 1961    |
| authors                     | 85.35     | 84.88     | 85.11     | 1997    |
| first_author                | 96.98     | 96.54     | 96.76     | 1995    |
| keywords                    | 63.01     | 63.01     | 63.01     | 838     |
| title                       | 79.47     | 78.68     | 79.07     | 1998    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **74.97** | **73.24** | **74.09** | 10777   |
| all fields (macro avg.)     | 76.2      | 75.35     | 75.77     | 10777   |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 80.09     | 78.52     | 79.3      | 1988    |
| affiliation_linked          | 75.69     | 73.36     | 74.51     | 1961    |
| authors                     | 92.55     | 92.04     | 92.29     | 1997    |
| first_author                | 97.23     | 96.79     | 97.01     | 1995    |
| keywords                    | 78.16     | 78.16     | 78.16     | 838     |
| title                       | 91.91     | 90.99     | 91.45     | 1998    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **80.54** | **78.67** | **79.59** | 10777   |
| all fields (macro avg.)     | 85.94     | 84.98     | 85.45     | 10777   |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 77.01     | 75.5      | 76.25     | 1988    |
| affiliation_linked          | 73.93     | 71.65     | 72.77     | 1961    |
| authors                     | 88.47     | 87.98     | 88.22     | 1997    |
| first_author                | 96.73     | 96.29     | 96.51     | 1995    |
| keywords                    | 70.29     | 70.29     | 70.29     | 838     |
| title                       | 87.66     | 86.79     | 87.22     | 1998    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **78.17** | **76.37** | **77.26** | 10777   |
| all fields (macro avg.)     | 82.35     | 81.42     | 81.88     | 10777   |

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
Total expected instances: 	1998
Total correct instances: 	37 (strict)
Total correct instances: 	723 (soft)
Total correct instances: 	1222 (Levenshtein)
Total correct instances: 	1052 (ObservedRatcliffObershelp)

Instance-level recall:	1.85	(strict)
Instance-level recall:	36.19	(soft)
Instance-level recall:	61.16	(Levenshtein)
Instance-level recall:	52.65	(RatcliffObershelp)
```

## Citation metadata

Evaluation on 1998 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 88.32     | 83.09     | 85.63     | 97094   |
| date                        | 91.55     | 85.85     | 88.61     | 97540   |
| doi                         | 71.01     | 83.52     | 76.76     | 16831   |
| first_author                | 95.14     | 89.42     | 92.19     | 97094   |
| inTitle                     | 82.72     | 79.03     | 80.83     | 96340   |
| issue                       | 93.93     | 90.76     | 92.32     | 30298   |
| page                        | 94.83     | 77.89     | 85.53     | 88515   |
| pmcid                       | 65.78     | 82.9      | 73.36     | 807     |
| pmid                        | 69.95     | 80.41     | 74.82     | 2093    |
| title                       | 84.81     | 83.28     | 84.04     | 92375   |
| volume                      | 95.97     | 94.78     | 95.37     | 87626   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.78** | **84.94** | **87.29** | 706613  |
| all fields (macro avg.)     | 84.91     | 84.63     | 84.5      | 706613  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 89.47     | 84.17     | 86.74     | 97094   |
| date                        | 91.55     | 85.85     | 88.61     | 97540   |
| doi                         | 75.49     | 88.79     | 81.6      | 16831   |
| first_author                | 95.57     | 89.82     | 92.61     | 97094   |
| inTitle                     | 92.14     | 88.03     | 90.04     | 96340   |
| issue                       | 93.93     | 90.76     | 92.32     | 30298   |
| page                        | 94.83     | 77.89     | 85.53     | 88515   |
| pmcid                       | 74.73     | 94.18     | 83.33     | 807     |
| pmid                        | 73.82     | 84.85     | 78.95     | 2093    |
| title                       | 93.11     | 91.43     | 92.26     | 92375   |
| volume                      | 95.97     | 94.78     | 95.37     | 87626   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.57** | **87.59** | **90.01** | 706613  |
| all fields (macro avg.)     | 88.24     | 88.23     | 87.94     | 706613  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| authors                     | 94.66     | 89.05     | 91.77    | 97094   |
| date                        | 91.55     | 85.85     | 88.61    | 97540   |
| doi                         | 77.52     | 91.18     | 83.8     | 16831   |
| first_author                | 95.71     | 89.96     | 92.75    | 97094   |
| inTitle                     | 93.18     | 89.03     | 91.06    | 96340   |
| issue                       | 93.93     | 90.76     | 92.32    | 30298   |
| page                        | 94.83     | 77.89     | 85.53    | 88515   |
| pmcid                       | 74.73     | 94.18     | 83.33    | 807     |
| pmid                        | 73.82     | 84.85     | 78.95    | 2093    |
| title                       | 96        | 94.27     | 95.13    | 92375   |
| volume                      | 95.97     | 94.78     | 95.37    | 87626   |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **93.9**  | **88.84** | **91.3** | 706613  |
| all fields (macro avg.)     | 89.26     | 89.25     | 88.96    | 706613  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 91.66     | 86.22     | 88.85     | 97094   |
| date                        | 91.55     | 85.85     | 88.61     | 97540   |
| doi                         | 76.16     | 89.58     | 82.33     | 16831   |
| first_author                | 95.18     | 89.46     | 92.24     | 97094   |
| inTitle                     | 90.88     | 86.82     | 88.8      | 96340   |
| issue                       | 93.93     | 90.76     | 92.32     | 30298   |
| page                        | 94.83     | 77.89     | 85.53     | 88515   |
| pmcid                       | 65.78     | 82.9      | 73.36     | 807     |
| pmid                        | 69.95     | 80.41     | 74.82     | 2093    |
| title                       | 95.32     | 93.6      | 94.45     | 92375   |
| volume                      | 95.97     | 94.78     | 95.37     | 87626   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.94** | **87.93** | **90.37** | 706613  |
| all fields (macro avg.)     | 87.38     | 87.12     | 86.97     | 706613  |

#### Instance-level results

```
Total expected instances: 		98709
Total extracted instances: 		97866
Total correct instances: 		43399 (strict)
Total correct instances: 		54277 (soft)
Total correct instances: 		58438 (Levenshtein)
Total correct instances: 		55219 (RatcliffObershelp)

Instance-level precision:	44.35 (strict)
Instance-level precision:	55.46 (soft)
Instance-level precision:	59.71 (Levenshtein)
Instance-level precision:	56.42 (RatcliffObershelp)

Instance-level recall:	43.97	(strict)
Instance-level recall:	54.99	(soft)
Instance-level recall:	59.2	(Levenshtein)
Instance-level recall:	55.94	(RatcliffObershelp)

Instance-level f-score:	44.16 (strict)
Instance-level f-score:	55.22 (soft)
Instance-level f-score:	59.46 (Levenshtein)
Instance-level f-score:	56.18 (RatcliffObershelp)

Matching 1 :	78769

Matching 2 :	4477

Matching 3 :	4339

Matching 4 :	2217

Total matches :	89802
```

#### Citation context resolution

```

Total expected references: 	 98707 - 49.4 references per article
Total predicted references: 	 97866 - 48.98 references per article

Total expected citation contexts: 	 142723 - 71.43 citation contexts per article
Total predicted citation contexts: 	 134635 - 67.38 citation contexts per article

Total correct predicted citation contexts: 	 116122 - 58.12 citation contexts per article
Total wrong predicted citation contexts: 	 18513 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 86.25
Recall citation contexts: 	 81.36
fscore citation contexts: 	 83.73
```

## Fulltext structures

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from
the actual PDF content and can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the structure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 1998 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 28.18     | 27.42     | 27.79     | 445     |
| conflict_stmt               | 66.36     | 59.05     | 62.49     | 608     |
| contribution_stmt           | 42.77     | 43.75     | 43.25     | 608     |
| figure_title                | 4.25      | 2.36      | 3.03      | 22964   |
| funding_stmt                | 3.84      | 23.96     | 6.62      | 747     |
| reference_citation          | 71.94     | 70.93     | 71.43     | 147331  |
| reference_figure            | 70.34     | 76.98     | 73.51     | 47964   |
| reference_table             | 45.32     | 84.74     | 59.06     | 5956    |
| section_title               | 69.07     | 68.63     | 68.85     | 32350   |
| table_title                 | 6.98      | 2.55      | 3.73      | 3924    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **65.12** | **64.76** | **64.94** | 262897  |
| all fields (macro avg.)     | 40.9      | 46.04     | 41.98     | 262897  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 49.42     | 48.09     | 48.75     | 445     |
| conflict_stmt               | 80.96     | 72.04     | 76.24     | 608     |
| contribution_stmt           | 72.67     | 74.34     | 73.5      | 608     |
| figure_title                | 66.77     | 36.97     | 47.59     | 22964   |
| funding_stmt                | 4.1       | 25.57     | 7.06      | 747     |
| reference_citation          | 84.28     | 83.1      | 83.69     | 147331  |
| reference_figure            | 71        | 77.7      | 74.2      | 47964   |
| reference_table             | 45.72     | 85.48     | 59.57     | 5956    |
| section_title               | 74.48     | 74        | 74.24     | 32350   |
| table_title                 | 81.22     | 29.64     | 43.43     | 3924    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **76.37** | **75.95** | **76.16** | 262897  |
| all fields (macro avg.)     | 63.06     | 60.69     | 58.83     | 262897  |

**Document-level ratio results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 82.32     | 97.3      | 89.19     | 445     |
| conflict_stmt               | 95.41     | 88.98     | 92.09     | 608     |
| contribution_stmt           | 91.07     | 102.3     | 96.36     | 608     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.86** | **96.09** | **92.87** | 1661    |
| all fields (macro avg.)     | 89.6      | 96.2      | 92.54     | 1661    |

Evaluation metrics produced in 204.149 seconds



