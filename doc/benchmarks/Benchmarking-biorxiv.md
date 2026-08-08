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

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| abstract                    | 2.36      | 2.31     | 2.34      | 1990    |
| affiliation_linked          | 0.86      | 0.86     | 0.86      | 1963    |
| authors                     | 84.91     | 84.44    | 84.68     | 1999    |
| first_author                | 96.78     | 96.34    | 96.56     | 1997    |
| keywords                    | 57.52     | 57.45    | 57.48     | 839     |
| title                       | 77.32     | 76.55    | 76.93     | 2000    |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **22.79** | **22.8** | **22.79** | 10788   |
| all fields (macro avg.)     | 53.29     | 52.99    | 53.14     | 10788   |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 59.67     | 58.44     | 59.05     | 1990    |
| affiliation_linked          | 75.16     | 75.55     | 75.35     | 1963    |
| authors                     | 85.41     | 84.94     | 85.18     | 1999    |
| first_author                | 97.03     | 96.59     | 96.81     | 1997    |
| keywords                    | 63.13     | 63.05     | 63.09     | 839     |
| title                       | 79.55     | 78.75     | 79.15     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **76.42** | **76.44** | **76.43** | 10788   |
| all fields (macro avg.)     | 76.66     | 76.22     | 76.44     | 10788   |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 80.25     | 78.59     | 79.41     | 1990    |
| affiliation_linked          | 77.33     | 77.73     | 77.53     | 1963    |
| authors                     | 92.61     | 92.1      | 92.35     | 1999    |
| first_author                | 97.28     | 96.85     | 97.06     | 1997    |
| keywords                    | 78.16     | 78.07     | 78.12     | 839     |
| title                       | 91.97     | 91.05     | 91.51     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **81.46** | **81.48** | **81.47** | 10788   |
| all fields (macro avg.)     | 86.27     | 85.73     | 86        | 10788   |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| abstract                    | 77.12     | 75.53    | 76.31     | 1990    |
| affiliation_linked          | 75.82     | 76.21    | 76.01     | 1963    |
| authors                     | 88.53     | 88.04    | 88.29     | 1999    |
| first_author                | 96.78     | 96.34    | 96.56     | 1997    |
| keywords                    | 70.29     | 70.2     | 70.24     | 839     |
| title                       | 87.73     | 86.85    | 87.29     | 2000    |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **79.28** | **79.3** | **79.29** | 10788   |
| all fields (macro avg.)     | 82.71     | 82.2     | 82.45     | 10788   |

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
Total expected instances: 	2000
Total correct instances: 	39 (strict)
Total correct instances: 	730 (soft)
Total correct instances: 	1225 (Levenshtein)
Total correct instances: 	1056 (ObservedRatcliffObershelp)

Instance-level recall:	1.95	(strict)
Instance-level recall:	36.5	(soft)
Instance-level recall:	61.25	(Levenshtein)
Instance-level recall:	52.8	(RatcliffObershelp)
```

## Citation metadata

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| authors                     | 88.36     | 83.26     | 85.73    | 97183   |
| date                        | 91.56     | 86.01     | 88.7     | 97630   |
| doi                         | 71.15     | 83.86     | 76.98    | 16894   |
| first_author                | 95.16     | 89.6      | 92.3     | 97183   |
| inTitle                     | 82.75     | 79.19     | 80.93    | 96430   |
| issue                       | 93.98     | 91.19     | 92.56    | 30312   |
| page                        | 94.83     | 78.07     | 85.64    | 88597   |
| pmcid                       | 65.78     | 82.9      | 73.36    | 807     |
| pmid                        | 69.95     | 80.41     | 74.82    | 2093    |
| title                       | 84.83     | 83.45     | 84.13    | 92463   |
| volume                      | 95.99     | 94.97     | 95.48    | 87709   |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **89.8**  | **85.13** | **87.4** | 707301  |
| all fields (macro avg.)     | 84.94     | 84.81     | 84.6     | 707301  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 89.51     | 84.34     | 86.85     | 97183   |
| date                        | 91.56     | 86.01     | 88.7      | 97630   |
| doi                         | 75.66     | 89.18     | 81.87     | 16894   |
| first_author                | 95.59     | 90        | 92.71     | 97183   |
| inTitle                     | 92.17     | 88.21     | 90.14     | 96430   |
| issue                       | 93.98     | 91.19     | 92.56     | 30312   |
| page                        | 94.83     | 78.07     | 85.64     | 88597   |
| pmcid                       | 74.73     | 94.18     | 83.33     | 807     |
| pmid                        | 73.82     | 84.85     | 78.95     | 2093    |
| title                       | 93.14     | 91.62     | 92.37     | 92463   |
| volume                      | 95.99     | 94.97     | 95.48     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.6**  | **87.78** | **90.13** | 707301  |
| all fields (macro avg.)     | 88.27     | 88.42     | 88.05     | 707301  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 94.68     | 89.22     | 91.87     | 97183   |
| date                        | 91.56     | 86.01     | 88.7      | 97630   |
| doi                         | 77.69     | 91.57     | 84.06     | 16894   |
| first_author                | 95.74     | 90.14     | 92.85     | 97183   |
| inTitle                     | 93.21     | 89.2      | 91.16     | 96430   |
| issue                       | 93.98     | 91.19     | 92.56     | 30312   |
| page                        | 94.83     | 78.07     | 85.64     | 88597   |
| pmcid                       | 74.73     | 94.18     | 83.33     | 807     |
| pmid                        | 73.82     | 84.85     | 78.95     | 2093    |
| title                       | 96.02     | 94.45     | 95.23     | 92463   |
| volume                      | 95.99     | 94.97     | 95.48     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.92** | **89.03** | **91.41** | 707301  |
| all fields (macro avg.)     | 89.3      | 89.44     | 89.08     | 707301  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 91.68     | 86.4      | 88.96     | 97183   |
| date                        | 91.56     | 86.01     | 88.7      | 97630   |
| doi                         | 76.33     | 89.96     | 82.58     | 16894   |
| first_author                | 95.21     | 89.64     | 92.34     | 97183   |
| inTitle                     | 90.91     | 87        | 88.91     | 96430   |
| issue                       | 93.98     | 91.19     | 92.56     | 30312   |
| page                        | 94.83     | 78.07     | 85.64     | 88597   |
| pmcid                       | 65.78     | 82.9      | 73.36     | 807     |
| pmid                        | 69.95     | 80.41     | 74.82     | 2093    |
| title                       | 95.35     | 93.79     | 94.56     | 92463   |
| volume                      | 95.99     | 94.97     | 95.48     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.97** | **88.13** | **90.48** | 707301  |
| all fields (macro avg.)     | 87.42     | 87.3      | 87.08     | 707301  |

#### Instance-level results

```
Total expected instances: 		98799
Total extracted instances: 		98155
Total correct instances: 		43550 (strict)
Total correct instances: 		54460 (soft)
Total correct instances: 		58641 (Levenshtein)
Total correct instances: 		55415 (RatcliffObershelp)

Instance-level precision:	44.37 (strict)
Instance-level precision:	55.48 (soft)
Instance-level precision:	59.74 (Levenshtein)
Instance-level precision:	56.46 (RatcliffObershelp)

Instance-level recall:	44.08	(strict)
Instance-level recall:	55.12	(soft)
Instance-level recall:	59.35	(Levenshtein)
Instance-level recall:	56.09	(RatcliffObershelp)

Instance-level f-score:	44.22 (strict)
Instance-level f-score:	55.3 (soft)
Instance-level f-score:	59.55 (Levenshtein)
Instance-level f-score:	56.27 (RatcliffObershelp)

Matching 1 :	79007

Matching 2 :	4475

Matching 3 :	4352

Matching 4 :	2222

Total matches :	90056
```

#### Citation context resolution

```

Total expected references: 	 98797 - 49.4 references per article
Total predicted references: 	 98155 - 49.08 references per article

Total expected citation contexts: 	 142862 - 71.43 citation contexts per article
Total predicted citation contexts: 	 135062 - 67.53 citation contexts per article

Total correct predicted citation contexts: 	 116495 - 58.25 citation contexts per article
Total wrong predicted citation contexts: 	 18567 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 86.25
Recall citation contexts: 	 81.54
fscore citation contexts: 	 83.83
```

## Fulltext structures

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from
the actual PDF content and can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the structure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| availability_stmt           | 28.41     | 27.58    | 27.99     | 446     |
| conflict_stmt               | 66.61     | 59.28    | 62.73     | 609     |
| contribution_stmt           | 42.88     | 44.01    | 43.44     | 609     |
| figure_title                | 4.32      | 2.37     | 3.06      | 22978   |
| funding_stmt                | 3.85      | 23.96    | 6.63      | 747     |
| reference_citation          | 71.95     | 71.07    | 71.51     | 147470  |
| reference_figure            | 70.34     | 77.13    | 73.58     | 47984   |
| reference_table             | 45.21     | 85.03    | 59.03     | 5957    |
| section_title               | 69.69     | 68.88    | 69.28     | 32398   |
| table_title                 | 7.05      | 2.57     | 3.77      | 3925    |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **65.23** | **64.9** | **65.06** | 263123  |
| all fields (macro avg.)     | 41.03     | 46.19    | 42.1      | 263123  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 49.65     | 48.21     | 48.92     | 446     |
| conflict_stmt               | 81.18     | 72.25     | 76.46     | 609     |
| contribution_stmt           | 72.64     | 74.55     | 73.58     | 609     |
| figure_title                | 67.43     | 37.06     | 47.83     | 22978   |
| funding_stmt                | 4.11      | 25.57     | 7.08      | 747     |
| reference_citation          | 84.28     | 83.25     | 83.77     | 147470  |
| reference_figure            | 71        | 77.85     | 74.26     | 47984   |
| reference_table             | 45.61     | 85.78     | 59.56     | 5957    |
| section_title               | 75.16     | 74.29     | 74.72     | 32398   |
| table_title                 | 81.23     | 29.66     | 43.45     | 3925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **76.5**  | **76.12** | **76.31** | 263123  |
| all fields (macro avg.)     | 63.23     | 60.85     | 58.96     | 263123  |

**Document-level ratio results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 82.48     | 97.09     | 89.19     | 446     |
| conflict_stmt               | 95.42     | 89        | 92.1      | 609     |
| contribution_stmt           | 90.98     | 102.63    | 96.45     | 609     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.89** | **96.15** | **92.92** | 1664    |
| all fields (macro avg.)     | 89.62     | 96.24     | 92.58     | 1664    |

Evaluation metrics produced in 204.653 seconds

