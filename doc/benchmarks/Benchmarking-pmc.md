# Benchmarking PubMed Central

## General

This is the end-to-end benchmarking result for GROBID version **0.8.2** against the `PMC_sample_1943` dataset, see
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

Evaluation on 1943 random PDF PMC files out of 1943 PDF from 1943 different journals (0 PDF parsing failure).

Runtime for processing 1943 PDF: **1467** seconds, (0.75s per PDF) on Ubuntu 22.04, 16 CPU (32 threads), 128GB RAM and
with a GeForce GTX 1080 Ti GPU.

Note: with CRF only models, runtime is 470s (0.24 seconds per PDF) with 4 CPU, 8 threads.

## Header metadata

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| abstract                    | 16.34     | 16.06    | 16.2      | 1911    |
| authors                     | 92.94     | 92.89    | 92.91     | 1941    |
| first_author                | 96.7      | 96.65    | 96.68     | 1941    |
| keywords                    | 63.19     | 61.09    | 62.12     | 1380    |
| title                       | 84.4      | 84.1     | 84.25     | 1943    |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **71.58** | **70.9** | **71.24** | 9116    |
| all fields (macro avg.)     | 70.71     | 70.16    | 70.43     | 9116    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 62.96     | 61.9      | 62.43     | 1911    |
| authors                     | 94.9      | 94.85     | 94.87     | 1941    |
| first_author                | 97.16     | 97.11     | 97.14     | 1941    |
| keywords                    | 71.14     | 68.77     | 69.93     | 1380    |
| title                       | 92.05     | 91.71     | 91.88     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **84.62** | **83.81** | **84.21** | 9116    |
| all fields (macro avg.)     | 83.64     | 82.87     | 83.25     | 9116    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 89.84     | 88.33     | 89.08     | 1911    |
| authors                     | 96.75     | 96.7      | 96.73     | 1941    |
| first_author                | 97.37     | 97.32     | 97.35     | 1941    |
| keywords                    | 84.71     | 81.88     | 83.27     | 1380    |
| title                       | 98.24     | 97.89     | 98.07     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.99** | **93.09** | **93.54** | 9116    |
| all fields (macro avg.)     | 93.38     | 92.43     | 92.9      | 9116    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 85.74     | 84.3      | 85.01     | 1911    |
| authors                     | 95.82     | 95.78     | 95.8      | 1941    |
| first_author                | 96.7      | 96.65     | 96.68     | 1941    |
| keywords                    | 78.26     | 75.65     | 76.93     | 1380    |
| title                       | 96.23     | 95.88     | 96.06     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **91.41** | **90.53** | **90.97** | 9116    |
| all fields (macro avg.)     | 90.55     | 89.65     | 90.1      | 9116    |

#### Instance-level results

```
Total expected instances: 	1943
Total correct instances: 	205 (strict) 
Total correct instances: 	878 (soft) 
Total correct instances: 	1431 (Levenshtein) 
Total correct instances: 	1260 (ObservedRatcliffObershelp) 

Instance-level recall:	10.55	(strict) 
Instance-level recall:	45.19	(soft) 
Instance-level recall:	73.65	(Levenshtein) 
Instance-level recall:	64.85	(RatcliffObershelp) 
```

## Citation metadata

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 82.48     | 75.35     | 78.75     | 85778   |
| date                        | 94.56     | 83.09     | 88.46     | 87067   |
| first_author                | 88.96     | 81.24     | 84.93     | 85778   |
| inTitle                     | 72.56     | 70.77     | 71.65     | 81007   |
| issue                       | 90.38     | 85.18     | 87.7      | 16635   |
| page                        | 94.6      | 83.37     | 88.63     | 80501   |
| title                       | 79.26     | 74.58     | 76.85     | 80736   |
| volume                      | 96.06     | 88.54     | 92.15     | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **86.84** | **79.72** | **83.13** | 597569  |
| all fields (macro avg.)     | 87.36     | 80.27     | 83.64     | 597569  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 82.95     | 75.78     | 79.2      | 85778   |
| date                        | 94.56     | 83.09     | 88.46     | 87067   |
| first_author                | 89.14     | 81.4      | 85.09     | 85778   |
| inTitle                     | 84.08     | 82.01     | 83.03     | 81007   |
| issue                       | 90.38     | 85.18     | 87.7      | 16635   |
| page                        | 94.6      | 83.37     | 88.63     | 80501   |
| title                       | 90.88     | 85.52     | 88.12     | 80736   |
| volume                      | 96.06     | 88.54     | 92.15     | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.2**  | **82.81** | **86.34** | 597569  |
| all fields (macro avg.)     | 90.33     | 83.11     | 86.55     | 597569  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 88.64     | 80.98     | 84.64     | 85778   |
| date                        | 94.56     | 83.09     | 88.46     | 87067   |
| first_author                | 89.34     | 81.59     | 85.29     | 85778   |
| inTitle                     | 85.39     | 83.29     | 84.33     | 81007   |
| issue                       | 90.38     | 85.18     | 87.7      | 16635   |
| page                        | 94.6      | 83.37     | 88.63     | 80501   |
| title                       | 93.12     | 87.63     | 90.29     | 80736   |
| volume                      | 96.06     | 88.54     | 92.15     | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **91.54** | **84.04** | **87.63** | 597569  |
| all fields (macro avg.)     | 91.51     | 84.21     | 87.69     | 597569  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 85.42     | 78.03     | 81.56     | 85778   |
| date                        | 94.56     | 83.09     | 88.46     | 87067   |
| first_author                | 88.98     | 81.26     | 84.95     | 85778   |
| inTitle                     | 82.67     | 80.63     | 81.63     | 81007   |
| issue                       | 90.38     | 85.18     | 87.7      | 16635   |
| page                        | 94.6      | 83.37     | 88.63     | 80501   |
| title                       | 92.75     | 87.28     | 89.93     | 80736   |
| volume                      | 96.06     | 88.54     | 92.15     | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.58** | **83.16** | **86.71** | 597569  |
| all fields (macro avg.)     | 90.68     | 83.42     | 86.88     | 597569  |

#### Instance-level results

```
Total expected instances: 		90125
Total extracted instances: 		85511
Total correct instances: 		38660 (strict) 
Total correct instances: 		50678 (soft) 
Total correct instances: 		55503 (Levenshtein) 
Total correct instances: 		52057 (RatcliffObershelp) 

Instance-level precision:	45.21 (strict) 
Instance-level precision:	59.26 (soft) 
Instance-level precision:	64.91 (Levenshtein) 
Instance-level precision:	60.88 (RatcliffObershelp) 

Instance-level recall:	42.9	(strict) 
Instance-level recall:	56.23	(soft) 
Instance-level recall:	61.58	(Levenshtein) 
Instance-level recall:	57.76	(RatcliffObershelp) 

Instance-level f-score:	44.02 (strict) 
Instance-level f-score:	57.71 (soft) 
Instance-level f-score:	63.2 (Levenshtein) 
Instance-level f-score:	59.28 (RatcliffObershelp) 

Matching 1 :	67606

Matching 2 :	3942

Matching 3 :	1785

Matching 4 :	662

Total matches :	73995
```

#### Citation context resolution

```

Total expected references: 	 90125 - 46.38 references per article
Total predicted references: 	 85511 - 44.01 references per article

Total expected citation contexts: 	 139835 - 71.97 citation contexts per article
Total predicted citation contexts: 	 114990 - 59.18 citation contexts per article

Total correct predicted citation contexts: 	 97220 - 50.04 citation contexts per article
Total wrong predicted citation contexts: 	 17770 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 84.55
Recall citation contexts: 	 69.52
fscore citation contexts: 	 76.3
```

## Fulltext structures

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from
the actual PDF content and are can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the srtructure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| figure_title                | 32.03     | 26.7      | 29.12     | 7281    |
| reference_citation          | 58.11     | 58.83     | 58.47     | 134196  |
| reference_figure            | 60.58     | 68.28     | 64.2      | 19330   |
| reference_table             | 82.9      | 89.64     | 86.14     | 7327    |
| section_title               | 72.46     | 67.69     | 69.99     | 27619   |
| table_title                 | 67.72     | 49.66     | 57.3      | 3971    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **60.55** | **60.75** | **60.65** | 199724  |
| all fields (macro avg.)     | 62.3      | 60.13     | 60.87     | 199724  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| figure_title                | 79.91     | 66.61     | 72.66     | 7281    |
| reference_citation          | 62.39     | 63.17     | 62.78     | 134196  |
| reference_figure            | 61.08     | 68.84     | 64.72     | 19330   |
| reference_table             | 83.06     | 89.82     | 86.31     | 7327    |
| section_title               | 77.85     | 72.73     | 75.2      | 27619   |
| table_title                 | 94.3      | 69.15     | 79.79     | 3971    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **66.05** | **66.26** | **66.16** | 199724  |
| all fields (macro avg.)     | 76.43     | 71.72     | 73.58     | 199724  |

**Document-level ratio results**

| label                       | precision | recall | f1    | support |
|-----------------------------|-----------|--------|-------|---------|
|                             |           |        |       |         |
| **all fields (micro avg.)** | **0**     | **0**  | **0** | 0       |
| all fields (macro avg.)     | 0         | 0      | 0     | 0       |

Evaluation metrics produced in 1257.994 seconds

