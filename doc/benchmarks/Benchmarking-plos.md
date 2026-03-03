# Benchmarking PLOS

## General

This is the end-to-end benchmarking result for GROBID version **0.8.2** against the `PLOS` test set, see
the [End-to-end evaluation](End-to-end-evaluation.md) page for explanations and for reproducing this evaluation.

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

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| abstract                    | 13.02     | 13.44     | 13.22    | 960     |
| authors                     | 98.97     | 98.97     | 98.97    | 969     |
| first_author                | 99.17     | 99.17     | 99.17    | 969     |
| keywords                    | 0         | 0         | 0        | 0       |
| title                       | 95.18     | 94.7      | 94.94    | 1000    |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **76.35** | **76.86** | **76.6** | 3898    |
| all fields (macro avg.)     | 76.58     | 76.57     | 76.58    | 3898    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 49.34     | 50.94     | 50.13     | 960     |
| authors                     | 98.97     | 98.97     | 98.97     | 969     |
| first_author                | 99.17     | 99.17     | 99.17     | 969     |
| keywords                    | 0         | 0         | 0         | 0       |
| title                       | 98.79     | 98.3      | 98.55     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **86.44** | **87.02** | **86.73** | 3898    |
| all fields (macro avg.)     | 86.57     | 86.84     | 86.7      | 3898    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 75.28     | 77.71     | 76.47     | 960     |
| authors                     | 99.38     | 99.38     | 99.38     | 969     |
| first_author                | 99.28     | 99.28     | 99.28     | 969     |
| keywords                    | 0         | 0         | 0         | 0       |
| title                       | 99.3      | 98.8      | 99.05     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.25** | **93.87** | **93.56** | 3898    |
| all fields (macro avg.)     | 93.31     | 93.79     | 93.54     | 3898    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 64.78     | 66.88     | 65.81     | 960     |
| authors                     | 99.28     | 99.28     | 99.28     | 969     |
| first_author                | 99.17     | 99.17     | 99.17     | 969     |
| keywords                    | 0         | 0         | 0         | 0       |
| title                       | 98.99     | 98.5      | 98.75     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.47** | **91.07** | **90.77** | 3898    |
| all fields (macro avg.)     | 90.56     | 90.96     | 90.75     | 3898    |

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
| authors                     | 81.01     | 78.03     | 79.49     | 44770   |
| date                        | 84.38     | 80.65     | 82.47     | 45457   |
| first_author                | 91.27     | 87.89     | 89.55     | 44770   |
| inTitle                     | 81.64     | 83.14     | 82.38     | 42795   |
| issue                       | 93.43     | 92.1      | 92.76     | 18983   |
| page                        | 93.8      | 77.49     | 84.87     | 40844   |
| title                       | 59.87     | 60.23     | 60.05     | 43101   |
| volume                      | 95.72     | 95.6      | 95.66     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **84.11** | **81.04** | **82.55** | 321178  |
| all fields (macro avg.)     | 85.14     | 81.89     | 83.4      | 321178  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 81.33     | 78.33     | 79.8      | 44770   |
| date                        | 84.38     | 80.65     | 82.47     | 45457   |
| first_author                | 91.5      | 88.11     | 89.77     | 44770   |
| inTitle                     | 85.44     | 87.01     | 86.22     | 42795   |
| issue                       | 93.43     | 92.1      | 92.76     | 18983   |
| page                        | 93.8      | 77.49     | 84.87     | 40844   |
| title                       | 91.75     | 92.3      | 92.02     | 43101   |
| volume                      | 95.72     | 95.6      | 95.66     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.18** | **85.93** | **87.53** | 321178  |
| all fields (macro avg.)     | 89.67     | 86.45     | 87.95     | 321178  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 90.45     | 87.12     | 88.76     | 44770   |
| date                        | 84.38     | 80.65     | 82.47     | 45457   |
| first_author                | 92.04     | 88.62     | 90.3      | 44770   |
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
| authors                     | 84.77     | 81.65     | 83.18     | 44770   |
| date                        | 84.38     | 80.65     | 82.47     | 45457   |
| first_author                | 91.27     | 87.89     | 89.55     | 44770   |
| inTitle                     | 85.06     | 86.63     | 85.84     | 42795   |
| issue                       | 93.43     | 92.1      | 92.76     | 18983   |
| page                        | 93.8      | 77.49     | 84.87     | 40844   |
| title                       | 93.65     | 94.21     | 93.93     | 43101   |
| volume                      | 95.72     | 95.6      | 95.66     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.84** | **86.57** | **88.18** | 321178  |
| all fields (macro avg.)     | 90.26     | 87.03     | 88.53     | 321178  |

#### Instance-level results

```
Total expected instances: 		48449
Total extracted instances: 		47775
Total correct instances: 		13503 (strict) 
Total correct instances: 		22251 (soft) 
Total correct instances: 		24863 (Levenshtein) 
Total correct instances: 		23242 (RatcliffObershelp) 

Instance-level precision:	28.26 (strict) 
Instance-level precision:	46.57 (soft) 
Instance-level precision:	52.04 (Levenshtein) 
Instance-level precision:	48.65 (RatcliffObershelp) 

Instance-level recall:	27.87	(strict) 
Instance-level recall:	45.93	(soft) 
Instance-level recall:	51.32	(Levenshtein) 
Instance-level recall:	47.97	(RatcliffObershelp) 

Instance-level f-score:	28.07 (strict) 
Instance-level f-score:	46.25 (soft) 
Instance-level f-score:	51.68 (Levenshtein) 
Instance-level f-score:	48.31 (RatcliffObershelp) 

Matching 1 :	35106

Matching 2 :	1259

Matching 3 :	3277

Matching 4 :	1850

Total matches :	41492
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
the actual PDF content and are can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the srtructure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 1000 random PDF files out of 998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 57.73     | 58.02     | 57.87     | 779     |
| conflict_stmt               | 92.57     | 91.89     | 92.23     | 962     |
| figure_title                | 0.18      | 0.09      | 0.12      | 8943    |
| funding_stmt                | 6.17      | 31.65     | 10.32     | 1507    |
| reference_citation          | 87.98     | 94.53     | 91.13     | 69741   |
| reference_figure            | 74.11     | 85.79     | 79.52     | 11010   |
| reference_table             | 70.21     | 94.32     | 80.5      | 5159    |
| section_title               | 72.98     | 66.28     | 69.47     | 17540   |
| table_title                 | 0         | 0         | 0         | 6092    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **74.78** | **76.96** | **75.86** | 121733  |
| all fields (macro avg.)     | 51.32     | 58.06     | 53.46     | 121733  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| availability_stmt           | 85.57     | 86.01    | 85.79     | 779     |
| conflict_stmt               | 95.6      | 94.91    | 95.25     | 962     |
| figure_title                | 93.18     | 45.82    | 61.43     | 8943    |
| funding_stmt                | 7.8       | 40.01    | 13.05     | 1507    |
| reference_citation          | 87.98     | 94.54    | 91.14     | 69741   |
| reference_figure            | 74.35     | 86.07    | 79.78     | 11010   |
| reference_table             | 70.37     | 94.53    | 80.68     | 5159    |
| section_title               | 78.78     | 71.56    | 75        | 17540   |
| table_title                 | 52.96     | 7.49     | 13.12     | 6092    |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **79.48** | **81.8** | **80.62** | 121733  |
| all fields (macro avg.)     | 71.84     | 68.99    | 66.14     | 121733  |

**Document-level ratio results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 99.49     | 100.51    | 100       | 779     |
| conflict_stmt               | 99.9      | 99.27     | 99.58     | 962     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **99.71** | **99.83** | **99.77** | 1741    |
| all fields (macro avg.)     | 99.69     | 99.89     | 99.79     | 1741    |

Evaluation metrics produced in 779.689 seconds
