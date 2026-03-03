# Benchmarking eLife

## General

This is the end-to-end benchmarking result for GROBID version **0.8.2** against the `eLife` test set, see
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
| authors                     | 78.18     | 77.62     | 77.9      | 983     |
| first_author                | 93.95     | 93.38     | 93.67     | 982     |
| title                       | 88.8      | 87.8      | 88.3      | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **67.82** | **66.77** | **67.29** | 3933    |
| all fields (macro avg.)     | 67.4      | 66.78     | 67.09     | 3933    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| abstract                    | 21.54     | 20.73    | 21.13     | 984     |
| authors                     | 78.59     | 78.03    | 78.31     | 983     |
| first_author                | 93.95     | 93.38    | 93.67     | 982     |
| title                       | 95.79     | 94.72    | 95.25     | 984     |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **72.83** | **71.7** | **72.26** | 3933    |
| all fields (macro avg.)     | 72.47     | 71.71    | 72.09     | 3933    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 46.67     | 44.92     | 45.78     | 984     |
| authors                     | 89.86     | 89.22     | 89.54     | 983     |
| first_author                | 94.26     | 93.69     | 93.97     | 982     |
| title                       | 97.23     | 96.14     | 96.68     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **82.26** | **80.98** | **81.61** | 3933    |
| all fields (macro avg.)     | 82        | 80.99     | 81.49     | 3933    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| abstract                    | 43.4      | 41.77     | 42.57    | 984     |
| authors                     | 83.3      | 82.71     | 83       | 983     |
| first_author                | 93.95     | 93.38     | 93.67    | 982     |
| title                       | 97.23     | 96.14     | 96.68    | 984     |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **79.73** | **78.49** | **79.1** | 3933    |
| all fields (macro avg.)     | 79.47     | 78.5      | 78.98    | 3933    |

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
| authors                     | 79.7      | 78.4      | 79.04     | 63265   |
| date                        | 96.01     | 93.88     | 94.93     | 63662   |
| first_author                | 94.86     | 93.26     | 94.05     | 63265   |
| inTitle                     | 95.52     | 94.27     | 94.89     | 63213   |
| issue                       | 1.52      | 81.25     | 2.99      | 16      |
| page                        | 95.73     | 94.88     | 95.3      | 53375   |
| title                       | 90.27     | 90.59     | 90.43     | 62044   |
| volume                      | 97.83     | 98.28     | 98.06     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.59** | **91.84** | **92.21** | 429889  |
| all fields (macro avg.)     | 81.43     | 90.6      | 81.21     | 429889  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 79.84     | 78.53     | 79.18     | 63265   |
| date                        | 96.01     | 93.88     | 94.93     | 63662   |
| first_author                | 94.94     | 93.34     | 94.13     | 63265   |
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
| authors                     | 87.01     | 85.58     | 86.29     | 63265   |
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
Total correct instances: 		41799 (strict) 
Total correct instances: 		44601 (soft) 
Total correct instances: 		52283 (Levenshtein) 
Total correct instances: 		48799 (RatcliffObershelp) 

Instance-level precision:	64.22 (strict) 
Instance-level precision:	68.52 (soft) 
Instance-level precision:	80.32 (Levenshtein) 
Instance-level precision:	74.97 (RatcliffObershelp) 

Instance-level recall:	65.66	(strict) 
Instance-level recall:	70.06	(soft) 
Instance-level recall:	82.12	(Levenshtein) 
Instance-level recall:	76.65	(RatcliffObershelp) 

Instance-level f-score:	64.93 (strict) 
Instance-level f-score:	69.28 (soft) 
Instance-level f-score:	81.21 (Levenshtein) 
Instance-level f-score:	75.8 (RatcliffObershelp) 

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
Total predicted citation contexts: 	 99970 - 101.6 citation contexts per article

Total correct predicted citation contexts: 	 96026 - 97.59 citation contexts per article
Total wrong predicted citation contexts: 	 3944 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 96.05
Recall citation contexts: 	 88.08
fscore citation contexts: 	 91.89
```

## Fulltext structures

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from
the actual PDF content and are can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the srtructure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 984 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 25.69     | 27.01     | 26.33     | 585     |
| figure_title                | 0.07      | 0.02      | 0.03      | 31718   |
| funding_stmt                | 7.78      | 26.6      | 12.03     | 921     |
| reference_citation          | 57.1      | 55.99     | 56.54     | 108949  |
| reference_figure            | 58.43     | 51.08     | 54.51     | 68926   |
| reference_table             | 71.35     | 73.41     | 72.37     | 2381    |
| section_title               | 83.33     | 77.3      | 80.2      | 21831   |
| table_title                 | 0         | 0         | 0         | 1925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **56.39** | **48.58** | **52.19** | 237236  |
| all fields (macro avg.)     | 37.97     | 38.93     | 37.75     | 237236  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 36.1      | 37.95     | 37        | 585     |
| figure_title                | 49.82     | 16.06     | 24.29     | 31718   |
| funding_stmt                | 7.78      | 26.6      | 12.03     | 921     |
| reference_citation          | 93.63     | 91.81     | 92.71     | 108949  |
| reference_figure            | 58.71     | 51.33     | 54.77     | 68926   |
| reference_table             | 71.43     | 73.5      | 72.45     | 2381    |
| section_title               | 84.38     | 78.26     | 81.21     | 21831   |
| table_title                 | 95.08     | 28.1      | 43.38     | 1925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **78.46** | **67.59** | **72.62** | 237236  |
| all fields (macro avg.)     | 62.11     | 50.45     | 52.23     | 237236  |

**Document-level ratio results**

| label                       | precision | recall  | f1       | support |
|-----------------------------|-----------|---------|----------|---------|
| availability_stmt           | 93.61     | 105.13  | 99.03    | 585     |
|                             |           |         |          |         |
| **all fields (micro avg.)** | **93.61** | **100** | **96.7** | 585     |
| all fields (macro avg.)     | 93.61     | 100     | 99.03    | 585     |

Evaluation metrics produced in 1244.156 seconds

