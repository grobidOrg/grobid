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

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 16.21     | 15.91     | 16.06     | 1911    |
| authors                     | 61.02     | 60.74     | 60.88     | 1941    |
| first_author                | 88.51     | 88.1      | 88.3      | 1941    |
| keywords                    | 46.36     | 40.14     | 43.03     | 1380    |
| title                       | 70.06     | 69.12     | 69.59     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **57.51** | **55.84** | **56.66** | 9116    |
| all fields (macro avg.)     | 56.43     | 54.8      | 55.57     | 9116    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 59.25     | 58.14     | 58.69     | 1911    |
| authors                     | 61.23     | 60.95     | 61.09     | 1941    |
| first_author                | 88.61     | 88.2      | 88.41     | 1941    |
| keywords                    | 53.72     | 46.52     | 49.86     | 1380    |
| title                       | 77.99     | 76.94     | 77.46     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **69.4**  | **67.39** | **68.38** | 9116    |
| all fields (macro avg.)     | 68.16     | 66.15     | 67.1      | 9116    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 86.29     | 84.67     | 85.47     | 1911    |
| authors                     | 72.15     | 71.82     | 71.99     | 1941    |
| first_author                | 88.98     | 88.56     | 88.77     | 1941    |
| keywords                    | 74.9      | 64.86     | 69.51     | 1380    |
| title                       | 92.12     | 90.89     | 91.5      | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **83.52** | **81.09** | **82.28** | 9116    |
| all fields (macro avg.)     | 82.89     | 80.16     | 81.45     | 9116    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 82.35     | 80.8      | 81.56     | 1911    |
| authors                     | 64.34     | 64.04     | 64.19     | 1941    |
| first_author                | 88.51     | 88.1      | 88.3      | 1941    |
| keywords                    | 63.68     | 55.14     | 59.11     | 1380    |
| title                       | 86.8      | 85.64     | 86.22     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **78.21** | **75.93** | **77.05** | 9116    |
| all fields (macro avg.)     | 77.14     | 74.74     | 75.88     | 9116    |

#### Instance-level results

```
Total expected instances: 	1943
Total correct instances: 	112 (strict) 
Total correct instances: 	483 (soft) 
Total correct instances: 	915 (Levenshtein) 
Total correct instances: 	734 (ObservedRatcliffObershelp) 

Instance-level recall:	5.76	(strict) 
Instance-level recall:	24.86	(soft) 
Instance-level recall:	47.09	(Levenshtein) 
Instance-level recall:	37.78	(RatcliffObershelp) 
```

## Citation metadata

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 82.48     | 74.66     | 78.37     | 85778   |
| date                        | 94.36     | 82.52     | 88.05     | 87067   |
| first_author                | 89.17     | 80.66     | 84.7      | 85778   |
| inTitle                     | 73.04     | 70.81     | 71.9      | 81007   |
| issue                       | 89.71     | 84.04     | 86.78     | 16635   |
| page                        | 94.22     | 83.04     | 88.28     | 80501   |
| title                       | 79.48     | 74.13     | 76.71     | 80736   |
| volume                      | 95.28     | 88.56     | 91.8      | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **86.79** | **79.33** | **82.89** | 597569  |
| all fields (macro avg.)     | 87.22     | 79.8      | 83.32     | 597569  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 82.96     | 75.09     | 78.83     | 85778   |
| date                        | 94.36     | 82.52     | 88.05     | 87067   |
| first_author                | 89.33     | 80.81     | 84.86     | 85778   |
| inTitle                     | 84.75     | 82.16     | 83.43     | 81007   |
| issue                       | 89.71     | 84.04     | 86.78     | 16635   |
| page                        | 94.22     | 83.04     | 88.28     | 80501   |
| title                       | 91.07     | 84.94     | 87.89     | 80736   |
| volume                      | 95.28     | 88.56     | 91.8      | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.16** | **82.41** | **86.11** | 597569  |
| all fields (macro avg.)     | 90.21     | 82.65     | 86.24     | 597569  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 88.75     | 80.33     | 84.33     | 85778   |
| date                        | 94.36     | 82.52     | 88.05     | 87067   |
| first_author                | 89.55     | 81        | 85.06     | 85778   |
| inTitle                     | 86.03     | 83.41     | 84.7      | 81007   |
| issue                       | 89.71     | 84.04     | 86.78     | 16635   |
| page                        | 94.22     | 83.04     | 88.28     | 80501   |
| title                       | 93.59     | 87.29     | 90.33     | 80736   |
| volume                      | 95.28     | 88.56     | 91.8      | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **91.55** | **83.68** | **87.44** | 597569  |
| all fields (macro avg.)     | 91.44     | 83.77     | 87.42     | 597569  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 85.4      | 77.3      | 81.15     | 85778   |
| date                        | 94.36     | 82.52     | 88.05     | 87067   |
| first_author                | 89.18     | 80.67     | 84.71     | 85778   |
| inTitle                     | 83.31     | 80.77     | 82.02     | 81007   |
| issue                       | 89.71     | 84.04     | 86.78     | 16635   |
| page                        | 94.22     | 83.04     | 88.28     | 80501   |
| title                       | 93.09     | 86.82     | 89.85     | 80736   |
| volume                      | 95.28     | 88.56     | 91.8      | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.56** | **82.77** | **86.49** | 597569  |
| all fields (macro avg.)     | 90.57     | 82.97     | 86.58     | 597569  |

#### Instance-level results

```
Total expected instances: 		90125
Total extracted instances: 		86125
Total correct instances: 		37732 (strict) 
Total correct instances: 		49502 (soft) 
Total correct instances: 		54240 (Levenshtein) 
Total correct instances: 		50880 (RatcliffObershelp) 

Instance-level precision:	43.81 (strict) 
Instance-level precision:	57.48 (soft) 
Instance-level precision:	62.98 (Levenshtein) 
Instance-level precision:	59.08 (RatcliffObershelp) 

Instance-level recall:	41.87	(strict) 
Instance-level recall:	54.93	(soft) 
Instance-level recall:	60.18	(Levenshtein) 
Instance-level recall:	56.45	(RatcliffObershelp) 

Instance-level f-score:	42.82 (strict) 
Instance-level f-score:	56.17 (soft) 
Instance-level f-score:	61.55 (Levenshtein) 
Instance-level f-score:	57.74 (RatcliffObershelp) 

Matching 1 :	66746

Matching 2 :	4079

Matching 3 :	2288

Matching 4 :	986

Total matches :	74099
```

#### Citation context resolution

```

Total expected references: 	 90125 - 46.38 references per article
Total predicted references: 	 86125 - 44.33 references per article

Total expected citation contexts: 	 139835 - 71.97 citation contexts per article
Total predicted citation contexts: 	 112840 - 58.08 citation contexts per article

Total correct predicted citation contexts: 	 95258 - 49.03 citation contexts per article
Total wrong predicted citation contexts: 	 17582 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 84.42
Recall citation contexts: 	 68.12
fscore citation contexts: 	 75.4
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
| figure_title                | 31.63     | 26.59     | 28.89     | 7281    |
| reference_citation          | 58.1      | 58.67     | 58.38     | 134196  |
| reference_figure            | 60.63     | 68.29     | 64.23     | 19330   |
| reference_table             | 82.88     | 89.57     | 86.09     | 7327    |
| section_title               | 73.73     | 67.79     | 70.64     | 27619   |
| table_title                 | 67.71     | 49.58     | 57.25     | 3971    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **60.67** | **60.65** | **60.66** | 199724  |
| all fields (macro avg.)     | 62.45     | 60.08     | 60.91     | 199724  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| figure_title                | 79.43     | 66.78     | 72.56     | 7281    |
| reference_citation          | 62.35     | 62.96     | 62.65     | 134196  |
| reference_figure            | 61.12     | 68.85     | 64.76     | 19330   |
| reference_table             | 83.05     | 89.76     | 86.28     | 7327    |
| section_title               | 79.24     | 72.85     | 75.91     | 27619   |
| table_title                 | 94.19     | 68.98     | 79.63     | 3971    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **66.17** | **66.14** | **66.16** | 199724  |
| all fields (macro avg.)     | 76.56     | 71.7      | 73.63     | 199724  |

**Document-level ratio results**

| label                       | precision | recall | f1    | support |
|-----------------------------|-----------|--------|-------|---------|
|                             |           |        |       |         |
| **all fields (micro avg.)** | **0**     | **0**  | **0** | 0       |
| all fields (macro avg.)     | 0         | 0      | 0     | 0       |

Evaluation metrics produced in 1263.288 seconds
