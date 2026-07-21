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

Evaluation on 1999 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 2.31      | 2.26      | 2.29      | 1989    |
| affiliation_linked          | 0.86      | 0.86      | 0.86      | 1962    |
| authors                     | 84.85     | 84.38     | 84.62     | 1998    |
| first_author                | 96.73     | 96.29     | 96.51     | 1996    |
| keywords                    | 57.28     | 57.28     | 57.28     | 838     |
| title                       | 77.26     | 76.49     | 76.87     | 1999    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **22.78** | **22.77** | **22.78** | 10782   |
| all fields (macro avg.)     | 53.21     | 52.93     | 53.07     | 10782   |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 59.52     | 58.32     | 58.91     | 1989    |
| affiliation_linked          | 75.32     | 75.59     | 75.45     | 1962    |
| authors                     | 85.35     | 84.88     | 85.12     | 1998    |
| first_author                | 96.98     | 96.54     | 96.76     | 1996    |
| keywords                    | 63.01     | 63.01     | 63.01     | 838     |
| title                       | 79.48     | 78.69     | 79.08     | 1999    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **76.5**  | **76.44** | **76.47** | 10782   |
| all fields (macro avg.)     | 76.61     | 76.17     | 76.39     | 10782   |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| abstract                    | 80.09     | 78.48     | 79.28    | 1989    |
| affiliation_linked          | 77.47     | 77.75     | 77.61    | 1962    |
| authors                     | 92.55     | 92.04     | 92.3     | 1998    |
| first_author                | 97.23     | 96.79     | 97.01    | 1996    |
| keywords                    | 78.16     | 78.16     | 78.16    | 838     |
| title                       | 91.92     | 91        | 91.45    | 1999    |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **81.53** | **81.47** | **81.5** | 10782   |
| all fields (macro avg.)     | 86.24     | 85.7      | 85.97    | 10782   |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 77.01     | 75.47     | 76.23     | 1989    |
| affiliation_linked          | 75.97     | 76.25     | 76.11     | 1962    |
| authors                     | 88.48     | 87.99     | 88.23     | 1998    |
| first_author                | 96.73     | 96.29     | 96.51     | 1996    |
| keywords                    | 70.29     | 70.29     | 70.29     | 838     |
| title                       | 87.67     | 86.79     | 87.23     | 1999    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **79.37** | **79.31** | **79.34** | 10782   |
| all fields (macro avg.)     | 82.69     | 82.18     | 82.43     | 10782   |

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
Total expected instances: 	1999
Total correct instances: 	37 (strict)
Total correct instances: 	723 (soft)
Total correct instances: 	1222 (Levenshtein)
Total correct instances: 	1052 (ObservedRatcliffObershelp)

Instance-level recall:	1.85	(strict)
Instance-level recall:	36.17	(soft)
Instance-level recall:	61.13	(Levenshtein)
Instance-level recall:	52.63	(RatcliffObershelp)
```

## Citation metadata

Evaluation on 1999 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| authors                     | 88.33     | 83.1      | 85.64    | 97164   |
| date                        | 91.55     | 85.86     | 88.61    | 97611   |
| doi                         | 71.1      | 83.57     | 76.83    | 16894   |
| first_author                | 95.14     | 89.43     | 92.2     | 97164   |
| inTitle                     | 82.72     | 79.03     | 80.84    | 96411   |
| issue                       | 93.93     | 90.76     | 92.32    | 30298   |
| page                        | 94.83     | 77.89     | 85.53    | 88578   |
| pmcid                       | 65.78     | 82.9      | 73.36    | 807     |
| pmid                        | 69.95     | 80.41     | 74.82    | 2093    |
| title                       | 84.81     | 83.29     | 84.04    | 92444   |
| volume                      | 95.97     | 94.78     | 95.37    | 87691   |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **89.78** | **84.95** | **87.3** | 707155  |
| all fields (macro avg.)     | 84.92     | 84.64     | 84.5     | 707155  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 89.48     | 84.18     | 86.75     | 97164   |
| date                        | 91.55     | 85.86     | 88.61     | 97611   |
| doi                         | 75.56     | 88.82     | 81.65     | 16894   |
| first_author                | 95.57     | 89.83     | 92.61     | 97164   |
| inTitle                     | 92.14     | 88.03     | 90.04     | 96411   |
| issue                       | 93.93     | 90.76     | 92.32     | 30298   |
| page                        | 94.83     | 77.89     | 85.53     | 88578   |
| pmcid                       | 74.73     | 94.18     | 83.33     | 807     |
| pmid                        | 73.82     | 84.85     | 78.95     | 2093    |
| title                       | 93.11     | 91.43     | 92.26     | 92444   |
| volume                      | 95.97     | 94.78     | 95.37     | 87691   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.58** | **87.59** | **90.02** | 707155  |
| all fields (macro avg.)     | 88.24     | 88.24     | 87.95     | 707155  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| authors                     | 94.66     | 89.05     | 91.77    | 97164   |
| date                        | 91.55     | 85.86     | 88.61    | 97611   |
| doi                         | 77.59     | 91.2      | 83.85    | 16894   |
| first_author                | 95.71     | 89.97     | 92.75    | 97164   |
| inTitle                     | 93.18     | 89.03     | 91.06    | 96411   |
| issue                       | 93.93     | 90.76     | 92.32    | 30298   |
| page                        | 94.83     | 77.89     | 85.53    | 88578   |
| pmcid                       | 74.73     | 94.18     | 83.33    | 807     |
| pmid                        | 73.82     | 84.85     | 78.95    | 2093    |
| title                       | 96        | 94.27     | 95.13    | 92444   |
| volume                      | 95.97     | 94.78     | 95.37    | 87691   |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **93.9**  | **88.85** | **91.3** | 707155  |
| all fields (macro avg.)     | 89.27     | 89.26     | 88.97    | 707155  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 91.66     | 86.23     | 88.86     | 97164   |
| date                        | 91.55     | 85.86     | 88.61     | 97611   |
| doi                         | 76.23     | 89.61     | 82.38     | 16894   |
| first_author                | 95.19     | 89.47     | 92.24     | 97164   |
| inTitle                     | 90.88     | 86.82     | 88.8      | 96411   |
| issue                       | 93.93     | 90.76     | 92.32     | 30298   |
| page                        | 94.83     | 77.89     | 85.53     | 88578   |
| pmcid                       | 65.78     | 82.9      | 73.36     | 807     |
| pmid                        | 69.95     | 80.41     | 74.82     | 2093    |
| title                       | 95.32     | 93.6      | 94.45     | 92444   |
| volume                      | 95.97     | 94.78     | 95.37     | 87691   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.94** | **87.94** | **90.37** | 707155  |
| all fields (macro avg.)     | 87.39     | 87.12     | 86.98     | 707155  |

#### Instance-level results

```
Total expected instances: 		98780
Total extracted instances: 		97937
Total correct instances: 		43446 (strict)
Total correct instances: 		54331 (soft)
Total correct instances: 		58493 (Levenshtein)
Total correct instances: 		55273 (RatcliffObershelp)

Instance-level precision:	44.36 (strict)
Instance-level precision:	55.48 (soft)
Instance-level precision:	59.73 (Levenshtein)
Instance-level precision:	56.44 (RatcliffObershelp)

Instance-level recall:	43.98	(strict)
Instance-level recall:	55	(soft)
Instance-level recall:	59.22	(Levenshtein)
Instance-level recall:	55.96	(RatcliffObershelp)

Instance-level f-score:	44.17 (strict)
Instance-level f-score:	55.24 (soft)
Instance-level f-score:	59.47 (Levenshtein)
Instance-level f-score:	56.2 (RatcliffObershelp)

Matching 1 :	78835

Matching 2 :	4478

Matching 3 :	4339

Matching 4 :	2218

Total matches :	89870
```

#### Citation context resolution

```

Total expected references: 	 98778 - 49.41 references per article
Total predicted references: 	 97937 - 48.99 references per article

Total expected citation contexts: 	 142847 - 71.46 citation contexts per article
Total predicted citation contexts: 	 134757 - 67.41 citation contexts per article

Total correct predicted citation contexts: 	 116233 - 58.15 citation contexts per article
Total wrong predicted citation contexts: 	 18524 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 86.25
Recall citation contexts: 	 81.37
fscore citation contexts: 	 83.74
```

## Fulltext structures

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from
the actual PDF content and can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the structure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 1999 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 28.34     | 27.58     | 27.95     | 446     |
| conflict_stmt               | 66.42     | 59.11     | 62.55     | 609     |
| contribution_stmt           | 42.7      | 43.68     | 43.18     | 609     |
| figure_title                | 4.25      | 2.36      | 3.03      | 22972   |
| funding_stmt                | 3.84      | 23.96     | 6.62      | 747     |
| reference_citation          | 71.96     | 70.95     | 71.45     | 147455  |
| reference_figure            | 70.31     | 76.98     | 73.5      | 47976   |
| reference_table             | 45.11     | 84.74     | 58.88     | 5956    |
| section_title               | 69.08     | 68.62     | 68.85     | 32391   |
| table_title                 | 6.98      | 2.55      | 3.73      | 3924    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **65.11** | **64.77** | **64.94** | 263085  |
| all fields (macro avg.)     | 40.9      | 46.05     | 41.97     | 263085  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 49.54     | 48.21     | 48.86     | 446     |
| conflict_stmt               | 81        | 72.09     | 76.28     | 609     |
| contribution_stmt           | 72.71     | 74.38     | 73.54     | 609     |
| figure_title                | 66.77     | 36.97     | 47.59     | 22972   |
| funding_stmt                | 4.1       | 25.57     | 7.06      | 747     |
| reference_citation          | 84.29     | 83.11     | 83.69     | 147455  |
| reference_figure            | 70.97     | 77.7      | 74.18     | 47976   |
| reference_table             | 45.5      | 85.48     | 59.39     | 5956    |
| section_title               | 74.49     | 74        | 74.25     | 32391   |
| table_title                 | 81.22     | 29.64     | 43.43     | 3924    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **76.36** | **75.96** | **76.16** | 263085  |
| all fields (macro avg.)     | 63.06     | 60.71     | 58.83     | 263085  |

**Document-level ratio results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 82.35     | 97.31     | 89.21     | 446     |
| conflict_stmt               | 95.42     | 89        | 92.1      | 609     |
| contribution_stmt           | 91.08     | 102.3     | 96.37     | 609     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.88** | **96.09** | **92.88** | 1664    |
| all fields (macro avg.)     | 89.62     | 96.2      | 92.56     | 1664    |

Evaluation metrics produced in 291.439 seconds

