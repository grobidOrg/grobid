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

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 12.53     | 12.81     | 12.67     | 960     |
| authors                     | 50.77     | 50.77     | 50.77     | 969     |
| first_author                | 82.77     | 82.77     | 82.77     | 969     |
| keywords                    | 0         | 0         | 0         | 0       |
| title                       | 67.14     | 66.8      | 66.97     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **53.26** | **53.49** | **53.37** | 3898    |
| all fields (macro avg.)     | 53.3      | 53.29     | 53.29     | 3898    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 46.33     | 47.4      | 46.86     | 960     |
| authors                     | 52.32     | 52.32     | 52.32     | 969     |
| first_author                | 82.97     | 82.97     | 82.97     | 969     |
| keywords                    | 0         | 0         | 0         | 0       |
| title                       | 81.51     | 81.1      | 81.3      | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **65.82** | **66.11** | **65.97** | 3898    |
| all fields (macro avg.)     | 65.78     | 65.95     | 65.86     | 3898    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 70.37     | 71.98     | 71.16     | 960     |
| authors                     | 81.01     | 81.01     | 81.01     | 969     |
| first_author                | 84.42     | 84.42     | 84.42     | 969     |
| keywords                    | 0         | 0         | 0         | 0       |
| title                       | 97.39     | 96.9      | 97.14     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **83.35** | **83.71** | **83.53** | 3898    |
| all fields (macro avg.)     | 83.3      | 83.58     | 83.43     | 3898    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 62.22     | 63.65     | 62.92     | 960     |
| authors                     | 62.33     | 62.33     | 62.33     | 969     |
| first_author                | 82.87     | 82.87     | 82.87     | 969     |
| keywords                    | 0         | 0         | 0         | 0       |
| title                       | 92.66     | 92.2      | 92.43     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **75.1**  | **75.42** | **75.26** | 3898    |
| all fields (macro avg.)     | 75.02     | 75.26     | 75.14     | 3898    |

#### Instance-level results

```
Total expected instances: 	1000
Total correct instances: 	58 (strict) 
Total correct instances: 	227 (soft) 
Total correct instances: 	504 (Levenshtein) 
Total correct instances: 	365 (ObservedRatcliffObershelp) 

Instance-level recall:	5.8	(strict) 
Instance-level recall:	22.7	(soft) 
Instance-level recall:	50.4	(Levenshtein) 
Instance-level recall:	36.5	(RatcliffObershelp) 
```

## Citation metadata

Evaluation on 1000 random PDF files out of 998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 81.1      | 77.96     | 79.49     | 44770   |
| date                        | 84.27     | 80.33     | 82.25     | 45457   |
| first_author                | 91.33     | 87.77     | 89.51     | 44770   |
| inTitle                     | 81.71     | 83.51     | 82.6      | 42795   |
| issue                       | 91.98     | 89.6      | 90.77     | 18983   |
| page                        | 93.59     | 77.77     | 84.95     | 40844   |
| title                       | 59.92     | 59.92     | 59.92     | 43101   |
| volume                      | 94.88     | 95.2      | 95.04     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **83.93** | **80.82** | **82.35** | 321178  |
| all fields (macro avg.)     | 84.85     | 81.51     | 83.07     | 321178  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 81.43     | 78.28     | 79.83     | 44770   |
| date                        | 84.27     | 80.33     | 82.25     | 45457   |
| first_author                | 91.56     | 87.99     | 89.74     | 44770   |
| inTitle                     | 85.55     | 87.44     | 86.48     | 42795   |
| issue                       | 91.98     | 89.6      | 90.77     | 18983   |
| page                        | 93.59     | 77.77     | 84.95     | 40844   |
| title                       | 91.97     | 91.96     | 91.96     | 43101   |
| volume                      | 94.88     | 95.2      | 95.04     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.02** | **85.72** | **87.34** | 321178  |
| all fields (macro avg.)     | 89.4      | 86.07     | 87.63     | 321178  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 90.55     | 87.04     | 88.76     | 44770   |
| date                        | 84.27     | 80.33     | 82.25     | 45457   |
| first_author                | 92.09     | 88.5      | 90.26     | 44770   |
| inTitle                     | 86.33     | 88.23     | 87.27     | 42795   |
| issue                       | 91.98     | 89.6      | 90.77     | 18983   |
| page                        | 93.59     | 77.77     | 84.95     | 40844   |
| title                       | 94.58     | 94.58     | 94.58     | 43101   |
| volume                      | 94.88     | 95.2      | 95.04     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.84** | **87.47** | **89.12** | 321178  |
| all fields (macro avg.)     | 91.03     | 87.66     | 89.24     | 321178  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 84.87     | 81.59     | 83.2      | 44770   |
| date                        | 84.27     | 80.33     | 82.25     | 45457   |
| first_author                | 91.33     | 87.77     | 89.51     | 44770   |
| inTitle                     | 85.17     | 87.05     | 86.1      | 42795   |
| issue                       | 91.98     | 89.6      | 90.77     | 18983   |
| page                        | 93.59     | 77.77     | 84.95     | 40844   |
| title                       | 93.9      | 93.9      | 93.9      | 43101   |
| volume                      | 94.88     | 95.2      | 95.04     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.68** | **86.35** | **87.99** | 321178  |
| all fields (macro avg.)     | 90        | 86.65     | 88.22     | 321178  |

#### Instance-level results

```
Total expected instances: 		48449
Total extracted instances: 		48947
Total correct instances: 		13305 (strict) 
Total correct instances: 		21985 (soft) 
Total correct instances: 		24553 (Levenshtein) 
Total correct instances: 		22959 (RatcliffObershelp) 

Instance-level precision:	27.18 (strict) 
Instance-level precision:	44.92 (soft) 
Instance-level precision:	50.16 (Levenshtein) 
Instance-level precision:	46.91 (RatcliffObershelp) 

Instance-level recall:	27.46	(strict) 
Instance-level recall:	45.38	(soft) 
Instance-level recall:	50.68	(Levenshtein) 
Instance-level recall:	47.39	(RatcliffObershelp) 

Instance-level f-score:	27.32 (strict) 
Instance-level f-score:	45.15 (soft) 
Instance-level f-score:	50.42 (Levenshtein) 
Instance-level f-score:	47.15 (RatcliffObershelp) 

Matching 1 :	34852

Matching 2 :	1274

Matching 3 :	3388

Matching 4 :	2026

Total matches :	41540
```

#### Citation context resolution

```

Total expected references: 	 48449 - 48.45 references per article
Total predicted references: 	 48947 - 48.95 references per article

Total expected citation contexts: 	 69755 - 69.75 citation contexts per article
Total predicted citation contexts: 	 71896 - 71.9 citation contexts per article

Total correct predicted citation contexts: 	 55389 - 55.39 citation contexts per article
Total wrong predicted citation contexts: 	 16507 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 77.04
Recall citation contexts: 	 79.41
fscore citation contexts: 	 78.2
```

## Fulltext structures

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from
the actual PDF content and are can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the srtructure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 1000 random PDF files out of 998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall   | f1       | support |
|-----------------------------|-----------|----------|----------|---------|
| availability_stmt           | 46.79     | 44.93    | 45.84    | 779     |
| figure_title                | 0.22      | 0.11     | 0.15     | 8943    |
| funding_stmt                | 5.77      | 26.74    | 9.49     | 1507    |
| reference_citation          | 87.92     | 94.06    | 90.89    | 69741   |
| reference_figure            | 74.2      | 85.74    | 79.55    | 11010   |
| reference_table             | 70.27     | 94.28    | 80.52    | 5159    |
| section_title               | 72.65     | 66.18    | 69.27    | 17540   |
| table_title                 | 0.12      | 0.02     | 0.03     | 6092    |
|                             |           |          |          |         |
| **all fields (micro avg.)** | **74.82** | **76.4** | **75.6** | 120771  |
| all fields (macro avg.)     | 44.74     | 51.51    | 46.97    | 120771  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 68.58     | 65.85     | 67.19     | 779     |
| figure_title                | 90.88     | 45.78     | 60.89     | 8943    |
| funding_stmt                | 7.26      | 33.64     | 11.94     | 1507    |
| reference_citation          | 87.93     | 94.06     | 90.89     | 69741   |
| reference_figure            | 74.44     | 86.02     | 79.81     | 11010   |
| reference_table             | 70.43     | 94.5      | 80.71     | 5159    |
| section_title               | 78.43     | 71.44     | 74.77     | 17540   |
| table_title                 | 53.5      | 7.52      | 13.18     | 6092    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **79.51** | **81.19** | **80.34** | 120771  |
| all fields (macro avg.)     | 66.43     | 62.35     | 59.92     | 120771  |

**Document-level ratio results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 99.6      | 96.02     | 97.78     | 779     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **99.6**  | **96.02** | **97.78** | 779     |
| all fields (macro avg.)     | 99.6      | 96.02     | 97.78     | 779     |

Evaluation metrics produced in 777.894 seconds

