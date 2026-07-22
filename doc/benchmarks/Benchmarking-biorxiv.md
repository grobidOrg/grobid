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
| abstract                    | 2.36      | 2.31      | 2.34      | 1989    |
| affiliation_linked          | 0.86      | 0.86      | 0.86      | 1962    |
| authors                     | 84.9      | 84.43     | 84.67     | 1998    |
| first_author                | 96.78     | 96.34     | 96.56     | 1996    |
| keywords                    | 57.47     | 57.4      | 57.43     | 838     |
| title                       | 77.31     | 76.54     | 76.92     | 1999    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **22.78** | **22.79** | **22.78** | 10782   |
| all fields (macro avg.)     | 53.28     | 52.98     | 53.13     | 10782   |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 59.65     | 58.42     | 59.03     | 1989    |
| affiliation_linked          | 75.16     | 75.55     | 75.35     | 1962    |
| authors                     | 85.41     | 84.93     | 85.17     | 1998    |
| first_author                | 97.03     | 96.59     | 96.81     | 1996    |
| keywords                    | 63.08     | 63.01     | 63.04     | 838     |
| title                       | 79.54     | 78.74     | 79.14     | 1999    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **76.42** | **76.44** | **76.43** | 10782   |
| all fields (macro avg.)     | 76.64     | 76.21     | 76.42     | 10782   |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 80.24     | 78.58     | 79.4      | 1989    |
| affiliation_linked          | 77.33     | 77.74     | 77.53     | 1962    |
| authors                     | 92.6      | 92.09     | 92.35     | 1998    |
| first_author                | 97.28     | 96.84     | 97.06     | 1996    |
| keywords                    | 78.14     | 78.04     | 78.09     | 838     |
| title                       | 91.97     | 91.05     | 91.5      | 1999    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **81.46** | **81.48** | **81.47** | 10782   |
| all fields (macro avg.)     | 86.26     | 85.72     | 85.99     | 10782   |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 77.1      | 75.52     | 76.3      | 1989    |
| affiliation_linked          | 75.81     | 76.21     | 76.01     | 1962    |
| authors                     | 88.53     | 88.04     | 88.28     | 1998    |
| first_author                | 96.78     | 96.34     | 96.56     | 1996    |
| keywords                    | 70.25     | 70.17     | 70.21     | 838     |
| title                       | 87.72     | 86.84     | 87.28     | 1999    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **79.28** | **79.29** | **79.29** | 10782   |
| all fields (macro avg.)     | 82.7      | 82.19     | 82.44     | 10782   |

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
Total correct instances: 	39 (strict)
Total correct instances: 	729 (soft)
Total correct instances: 	1224 (Levenshtein)
Total correct instances: 	1055 (ObservedRatcliffObershelp)

Instance-level recall:	1.95	(strict)
Instance-level recall:	36.47	(soft)
Instance-level recall:	61.23	(Levenshtein)
Instance-level recall:	52.78	(RatcliffObershelp)
```

## Citation metadata

Evaluation on 1999 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| authors                     | 88.36     | 83.26     | 85.73    | 97164   |
| date                        | 91.56     | 86.01     | 88.7     | 97611   |
| doi                         | 71.15     | 83.86     | 76.98    | 16894   |
| first_author                | 95.16     | 89.6      | 92.3     | 97164   |
| inTitle                     | 82.75     | 79.2      | 80.94    | 96411   |
| issue                       | 93.98     | 91.18     | 92.56    | 30298   |
| page                        | 94.83     | 78.06     | 85.63    | 88578   |
| pmcid                       | 65.78     | 82.9      | 73.36    | 807     |
| pmid                        | 69.95     | 80.41     | 74.82    | 2093    |
| title                       | 84.83     | 83.45     | 84.13    | 92444   |
| volume                      | 95.99     | 94.97     | 95.48    | 87691   |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **89.8**  | **85.13** | **87.4** | 707155  |
| all fields (macro avg.)     | 84.94     | 84.81     | 84.6     | 707155  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 89.5      | 84.34     | 86.85     | 97164   |
| date                        | 91.56     | 86.01     | 88.7      | 97611   |
| doi                         | 75.66     | 89.18     | 81.87     | 16894   |
| first_author                | 95.59     | 90        | 92.71     | 97164   |
| inTitle                     | 92.17     | 88.21     | 90.15     | 96411   |
| issue                       | 93.98     | 91.18     | 92.56     | 30298   |
| page                        | 94.83     | 78.06     | 85.63     | 88578   |
| pmcid                       | 74.73     | 94.18     | 83.33     | 807     |
| pmid                        | 73.82     | 84.85     | 78.95     | 2093    |
| title                       | 93.14     | 91.62     | 92.37     | 92444   |
| volume                      | 95.99     | 94.97     | 95.48     | 87691   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.6**  | **87.78** | **90.13** | 707155  |
| all fields (macro avg.)     | 88.27     | 88.42     | 88.05     | 707155  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 94.68     | 89.22     | 91.87     | 97164   |
| date                        | 91.56     | 86.01     | 88.7      | 97611   |
| doi                         | 77.69     | 91.57     | 84.06     | 16894   |
| first_author                | 95.73     | 90.14     | 92.85     | 97164   |
| inTitle                     | 93.21     | 89.2      | 91.16     | 96411   |
| issue                       | 93.98     | 91.18     | 92.56     | 30298   |
| page                        | 94.83     | 78.06     | 85.63     | 88578   |
| pmcid                       | 74.73     | 94.18     | 83.33     | 807     |
| pmid                        | 73.82     | 84.85     | 78.95     | 2093    |
| title                       | 96.02     | 94.45     | 95.23     | 92444   |
| volume                      | 95.99     | 94.97     | 95.48     | 87691   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.92** | **89.03** | **91.41** | 707155  |
| all fields (macro avg.)     | 89.29     | 89.44     | 89.08     | 707155  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 91.68     | 86.39     | 88.96     | 97164   |
| date                        | 91.56     | 86.01     | 88.7      | 97611   |
| doi                         | 76.33     | 89.96     | 82.58     | 16894   |
| first_author                | 95.21     | 89.64     | 92.34     | 97164   |
| inTitle                     | 90.9      | 87        | 88.91     | 96411   |
| issue                       | 93.98     | 91.18     | 92.56     | 30298   |
| page                        | 94.83     | 78.06     | 85.63     | 88578   |
| pmcid                       | 65.78     | 82.9      | 73.36     | 807     |
| pmid                        | 69.95     | 80.41     | 74.82     | 2093    |
| title                       | 95.35     | 93.79     | 94.56     | 92444   |
| volume                      | 95.99     | 94.97     | 95.48     | 87691   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.96** | **88.13** | **90.48** | 707155  |
| all fields (macro avg.)     | 87.41     | 87.3      | 87.08     | 707155  |

#### Instance-level results

```
Total expected instances: 		98780
Total extracted instances: 		98136
Total correct instances: 		43542 (strict)
Total correct instances: 		54450 (soft)
Total correct instances: 		58627 (Levenshtein)
Total correct instances: 		55401 (RatcliffObershelp)

Instance-level precision:	44.37 (strict)
Instance-level precision:	55.48 (soft)
Instance-level precision:	59.74 (Levenshtein)
Instance-level precision:	56.45 (RatcliffObershelp)

Instance-level recall:	44.08	(strict)
Instance-level recall:	55.12	(soft)
Instance-level recall:	59.35	(Levenshtein)
Instance-level recall:	56.09	(RatcliffObershelp)

Instance-level f-score:	44.22 (strict)
Instance-level f-score:	55.3 (soft)
Instance-level f-score:	59.55 (Levenshtein)
Instance-level f-score:	56.27 (RatcliffObershelp)

Matching 1 :	78995

Matching 2 :	4471

Matching 3 :	4350

Matching 4 :	2221

Total matches :	90037
```

#### Citation context resolution

```

Total expected references: 	 98778 - 49.41 references per article
Total predicted references: 	 98136 - 49.09 references per article

Total expected citation contexts: 	 142847 - 71.46 citation contexts per article
Total predicted citation contexts: 	 135039 - 67.55 citation contexts per article

Total correct predicted citation contexts: 	 116481 - 58.27 citation contexts per article
Total wrong predicted citation contexts: 	 18558 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 86.26
Recall citation contexts: 	 81.54
fscore citation contexts: 	 83.83
```

## Fulltext structures

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from
the actual PDF content and can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the structure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 1999 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| availability_stmt           | 28.41     | 27.58    | 27.99     | 446     |
| conflict_stmt               | 66.61     | 59.28    | 62.73     | 609     |
| contribution_stmt           | 42.72     | 43.84    | 43.27     | 609     |
| figure_title                | 4.32      | 2.37     | 3.06      | 22972   |
| funding_stmt                | 3.85      | 23.96    | 6.63      | 747     |
| reference_citation          | 71.95     | 71.07    | 71.51     | 147455  |
| reference_figure            | 70.34     | 77.14    | 73.58     | 47976   |
| reference_table             | 45.21     | 85.02    | 59.03     | 5956    |
| section_title               | 69.7      | 68.88    | 69.29     | 32391   |
| table_title                 | 7.05      | 2.57     | 3.77      | 3924    |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **65.23** | **64.9** | **65.07** | 263085  |
| all fields (macro avg.)     | 41.01     | 46.17    | 42.09     | 263085  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 49.65     | 48.21     | 48.92     | 446     |
| conflict_stmt               | 81.18     | 72.25     | 76.46     | 609     |
| contribution_stmt           | 72.64     | 74.55     | 73.58     | 609     |
| figure_title                | 67.44     | 37.06     | 47.83     | 22972   |
| funding_stmt                | 4.1       | 25.57     | 7.07      | 747     |
| reference_citation          | 84.29     | 83.25     | 83.77     | 147455  |
| reference_figure            | 71        | 77.86     | 74.27     | 47976   |
| reference_table             | 45.61     | 85.78     | 59.55     | 5956    |
| section_title               | 75.17     | 74.28     | 74.72     | 32391   |
| table_title                 | 81.22     | 29.64     | 43.43     | 3924    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **76.5**  | **76.12** | **76.31** | 263085  |
| all fields (macro avg.)     | 63.23     | 60.84     | 58.96     | 263085  |

**Document-level ratio results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 82.48     | 97.09     | 89.19     | 446     |
| conflict_stmt               | 95.42     | 89        | 92.1      | 609     |
| contribution_stmt           | 90.98     | 102.63    | 96.45     | 609     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.89** | **96.15** | **92.92** | 1664    |
| all fields (macro avg.)     | 89.62     | 96.24     | 92.58     | 1664    |

Evaluation metrics produced in 208.92 seconds

