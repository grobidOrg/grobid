# Benchmarking PLOS

## General

This is the end-to-end benchmarking result for GROBID version **0.9.0** against the `PLOS` test set, see
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

Evaluation on 1000 PDF preprints out of 1000 (no failure).

Runtime for processing 1000 PDF: **999** seconds, (0.99 seconds per PDF) on Ubuntu 22.04, 16 CPU (32 threads), 128GB RAM
and with a GeForce GTX 1080 Ti GPU.

Note: with CRF only models runtime is 304s (0.30 seconds per PDF) with 4 CPU, 8 threads.

## Header metadata

Evaluation on 1000 random PDF files out of 998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 13.02     | 13.44     | 13.22     | 960     |
| affiliation_linked          | 0         | 0         | 0         | 963     |
| authors                     | 98.97     | 98.97     | 98.97     | 969     |
| first_author                | 99.17     | 99.17     | 99.17     | 969     |
| keywords                    | 0         | 0         | 0         | 0       |
| title                       | 95.18     | 94.7      | 94.94     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **24.48** | **25.18** | **24.83** | 4861    |
| all fields (macro avg.)     | 61.27     | 61.26     | 61.26     | 4861    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall   | f1       | support |
|-----------------------------|-----------|----------|----------|---------|
| abstract                    | 49.34     | 50.94    | 50.13    | 960     |
| affiliation_linked          | 71.98     | 74.8     | 73.36    | 963     |
| authors                     | 98.97     | 98.97    | 98.97    | 969     |
| first_author                | 99.17     | 99.17    | 99.17    | 969     |
| keywords                    | 0         | 0        | 0        | 0       |
| title                       | 98.79     | 98.3     | 98.55    | 1000    |
|                             |           |          |          |         |
| **all fields (micro avg.)** | **76.62** | **78.8** | **77.7** | 4861    |
| all fields (macro avg.)     | 83.65     | 84.44    | 84.04    | 4861    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 75.28     | 77.71     | 76.47     | 960     |
| affiliation_linked          | 76.86     | 79.86     | 78.33     | 963     |
| authors                     | 99.38     | 99.38     | 99.38     | 969     |
| first_author                | 99.28     | 99.28     | 99.28     | 969     |
| keywords                    | 0         | 0         | 0         | 0       |
| title                       | 99.3      | 98.8      | 99.05     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **82.11** | **84.45** | **83.26** | 4861    |
| all fields (macro avg.)     | 90.02     | 91.01     | 90.5      | 4861    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 64.78     | 66.88     | 65.81     | 960     |
| affiliation_linked          | 74.49     | 77.4      | 75.91     | 963     |
| authors                     | 99.28     | 99.28     | 99.28     | 969     |
| first_author                | 99.17     | 99.17     | 99.17     | 969     |
| keywords                    | 0         | 0         | 0         | 0       |
| title                       | 98.99     | 98.5      | 98.75     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **79.61** | **81.88** | **80.73** | 4861    |
| all fields (macro avg.)     | 87.34     | 88.25     | 87.79     | 4861    |

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
Total expected instances: 	1000
Total correct instances: 	122 (strict)
Total correct instances: 	482 (soft)
Total correct instances: 	738 (Levenshtein)
Total correct instances: 	635 (ObservedRatcliffObershelp)

Instance-level recall:	12.2	(strict)
Instance-level recall:	48.2	(soft)
Instance-level recall:	73.8	(Levenshtein)
Instance-level recall:	63.5	(RatcliffObershelp)
```

## Citation metadata

Evaluation on 1000 random PDF files out of 998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 81.05     | 78.07     | 79.53     | 44770   |
| date                        | 84.38     | 80.65     | 82.47     | 45457   |
| first_author                | 91.29     | 87.9      | 89.56     | 44770   |
| inTitle                     | 81.64     | 83.14     | 82.38     | 42795   |
| issue                       | 93.43     | 92.1      | 92.76     | 18983   |
| page                        | 93.8      | 77.49     | 84.87     | 40844   |
| title                       | 59.87     | 60.23     | 60.05     | 43101   |
| volume                      | 95.72     | 95.6      | 95.66     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **84.11** | **81.05** | **82.55** | 321178  |
| all fields (macro avg.)     | 85.15     | 81.9      | 83.41     | 321178  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 81.37     | 78.38     | 79.84     | 44770   |
| date                        | 84.38     | 80.65     | 82.47     | 45457   |
| first_author                | 91.51     | 88.12     | 89.78     | 44770   |
| inTitle                     | 85.44     | 87.01     | 86.22     | 42795   |
| issue                       | 93.43     | 92.1      | 92.76     | 18983   |
| page                        | 93.8      | 77.49     | 84.87     | 40844   |
| title                       | 91.75     | 92.3      | 92.02     | 43101   |
| volume                      | 95.72     | 95.6      | 95.66     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.19** | **85.94** | **87.54** | 321178  |
| all fields (macro avg.)     | 89.67     | 86.46     | 87.95     | 321178  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 90.45     | 87.13     | 88.76     | 44770   |
| date                        | 84.38     | 80.65     | 82.47     | 45457   |
| first_author                | 92.05     | 88.64     | 90.31     | 44770   |
| inTitle                     | 86.32     | 87.91     | 87.11     | 42795   |
| issue                       | 93.43     | 92.1      | 92.76     | 18983   |
| page                        | 93.8      | 77.49     | 84.87     | 40844   |
| title                       | 94.27     | 94.84     | 94.55     | 43101   |
| volume                      | 95.72     | 95.6      | 95.66     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **91.01** | **87.69** | **89.32** | 321178  |
| all fields (macro avg.)     | 91.3      | 88.04     | 89.56     | 321178  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 84.79     | 81.67     | 83.2      | 44770   |
| date                        | 84.38     | 80.65     | 82.47     | 45457   |
| first_author                | 91.29     | 87.9      | 89.56     | 44770   |
| inTitle                     | 85.06     | 86.63     | 85.84     | 42795   |
| issue                       | 93.43     | 92.1      | 92.76     | 18983   |
| page                        | 93.8      | 77.49     | 84.87     | 40844   |
| title                       | 93.65     | 94.21     | 93.93     | 43101   |
| volume                      | 95.72     | 95.6      | 95.66     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.85** | **86.58** | **88.18** | 321178  |
| all fields (macro avg.)     | 90.27     | 87.03     | 88.54     | 321178  |

#### Instance-level results

```
Total expected instances: 		48449
Total extracted instances: 		47775
Total correct instances: 		13512 (strict)
Total correct instances: 		22262 (soft)
Total correct instances: 		24867 (Levenshtein)
Total correct instances: 		23248 (RatcliffObershelp)

Instance-level precision:	28.28 (strict)
Instance-level precision:	46.6 (soft)
Instance-level precision:	52.05 (Levenshtein)
Instance-level precision:	48.66 (RatcliffObershelp)

Instance-level recall:	27.89	(strict)
Instance-level recall:	45.95	(soft)
Instance-level recall:	51.33	(Levenshtein)
Instance-level recall:	47.98	(RatcliffObershelp)

Instance-level f-score:	28.08 (strict)
Instance-level f-score:	46.27 (soft)
Instance-level f-score:	51.69 (Levenshtein)
Instance-level f-score:	48.32 (RatcliffObershelp)

Matching 1 :	35106

Matching 2 :	1261

Matching 3 :	3276

Matching 4 :	1850

Total matches :	41493
```

#### Citation context resolution

```

Total expected references: 	 48449 - 48.45 references per article
Total predicted references: 	 47775 - 47.77 references per article

Total expected citation contexts: 	 69755 - 69.75 citation contexts per article
Total predicted citation contexts: 	 73296 - 73.3 citation contexts per article

Total correct predicted citation contexts: 	 57033 - 57.03 citation contexts per article
Total wrong predicted citation contexts: 	 16263 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 77.81
Recall citation contexts: 	 81.76
fscore citation contexts: 	 79.74
```

## Fulltext structures

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from
the actual PDF content and can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the structure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 1000 random PDF files out of 998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 57.73     | 58.02     | 57.87     | 779     |
| conflict_stmt               | 92.57     | 91.89     | 92.23     | 962     |
| figure_title                | 0.18      | 0.09      | 0.12      | 8943    |
| funding_stmt                | 5.79      | 31.12     | 9.77      | 1507    |
| reference_citation          | 87.98     | 94.53     | 91.13     | 69741   |
| reference_figure            | 74.11     | 85.79     | 79.52     | 11010   |
| reference_table             | 70.21     | 94.32     | 80.5      | 5159    |
| section_title               | 72.98     | 66.28     | 69.47     | 17540   |
| table_title                 | 0         | 0         | 0         | 6092    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **74.56** | **76.95** | **75.74** | 121733  |
| all fields (macro avg.)     | 51.28     | 58        | 53.4      | 121733  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| availability_stmt           | 85.57     | 86.01     | 85.79    | 779     |
| conflict_stmt               | 95.6      | 94.91     | 95.25    | 962     |
| figure_title                | 93.18     | 45.82     | 61.43    | 8943    |
| funding_stmt                | 7.33      | 39.35     | 12.35    | 1507    |
| reference_citation          | 87.98     | 94.54     | 91.14    | 69741   |
| reference_figure            | 74.35     | 86.07     | 79.78    | 11010   |
| reference_table             | 70.37     | 94.53     | 80.68    | 5159    |
| section_title               | 78.78     | 71.56     | 75       | 17540   |
| table_title                 | 52.96     | 7.49      | 13.12    | 6092    |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **79.25** | **81.79** | **80.5** | 121733  |
| all fields (macro avg.)     | 71.79     | 68.92     | 66.06    | 121733  |

**Document-level ratio results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 99.49     | 100.51    | 100       | 779     |
| conflict_stmt               | 99.9      | 99.27     | 99.58     | 962     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **99.71** | **99.83** | **99.77** | 1741    |
| all fields (macro avg.)     | 99.69     | 99.89     | 99.79     | 1741    |

Evaluation metrics produced in 102.635 seconds
