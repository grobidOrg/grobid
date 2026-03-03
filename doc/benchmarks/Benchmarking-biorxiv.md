# Benchmarking biorXiv

## General

This is the end-to-end benchmarking result for GROBID version **0.8.2** against the `bioRxiv` test set (
`biorxiv-10k-test-2000`), see the [End-to-end evaluation](End-to-end-evaluation.md) page for explanations and for
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

| label                       | precision | recall   | f1       | support |
|-----------------------------|-----------|----------|----------|---------|
| abstract                    | 2.31      | 2.26     | 2.28     | 1990    |
| authors                     | 84.86     | 84.39    | 84.63    | 1999    |
| first_author                | 96.73     | 96.29    | 96.51    | 1997    |
| keywords                    | 57.33     | 57.33    | 57.33    | 839     |
| title                       | 77.27     | 76.5     | 76.88    | 2000    |
|                             |           |          |          |         |
| **all fields (micro avg.)** | **64.79** | **64.2** | **64.5** | 8825    |
| all fields (macro avg.)     | 63.7      | 63.36    | 63.53    | 8825    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 59.54     | 58.34     | 58.93     | 1990    |
| authors                     | 85.36     | 84.89     | 85.13     | 1999    |
| first_author                | 96.98     | 96.54     | 96.76     | 1997    |
| keywords                    | 63.05     | 63.05     | 63.05     | 839     |
| title                       | 79.49     | 78.7      | 79.1      | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **78.78** | **78.06** | **78.42** | 8825    |
| all fields (macro avg.)     | 76.89     | 76.31     | 76.59     | 8825    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 80.1      | 78.49     | 79.29     | 1990    |
| authors                     | 92.61     | 92.1      | 92.35     | 1999    |
| first_author                | 97.23     | 96.8      | 97.01     | 1997    |
| keywords                    | 78.19     | 78.19     | 78.19     | 839     |
| title                       | 92.02     | 91.1      | 91.56     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.35** | **88.54** | **88.95** | 8825    |
| all fields (macro avg.)     | 88.03     | 87.33     | 87.68     | 8825    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| abstract                    | 77.03     | 75.48     | 76.24    | 1990    |
| authors                     | 88.48     | 87.99     | 88.24    | 1999    |
| first_author                | 96.73     | 96.29     | 96.51    | 1997    |
| keywords                    | 70.32     | 70.32     | 70.32    | 839     |
| title                       | 87.73     | 86.85     | 87.29    | 2000    |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **85.89** | **85.11** | **85.5** | 8825    |
| all fields (macro avg.)     | 84.06     | 83.39     | 83.72    | 8825    |

#### Instance-level results

```
Total expected instances: 	2000
Total correct instances: 	37 (strict) 
Total correct instances: 	723 (soft) 
Total correct instances: 	1224 (Levenshtein) 
Total correct instances: 	1053 (ObservedRatcliffObershelp) 

Instance-level recall:	1.85	(strict) 
Instance-level recall:	36.15	(soft) 
Instance-level recall:	61.2	(Levenshtein) 
Instance-level recall:	52.65	(RatcliffObershelp) 
```

## Citation metadata

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 88.3      | 83.07     | 85.6      | 97183   |
| date                        | 91.55     | 85.85     | 88.61     | 97630   |
| doi                         | 71.1      | 83.57     | 76.83     | 16894   |
| first_author                | 95.13     | 89.42     | 92.19     | 97183   |
| inTitle                     | 82.72     | 79.03     | 80.83     | 96430   |
| issue                       | 93.93     | 90.77     | 92.32     | 30312   |
| page                        | 94.83     | 77.9      | 85.53     | 88597   |
| pmcid                       | 65.78     | 82.9      | 73.36     | 807     |
| pmid                        | 69.95     | 80.41     | 74.82     | 2093    |
| title                       | 84.81     | 83.28     | 84.04     | 92463   |
| volume                      | 95.97     | 94.78     | 95.37     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.77** | **84.94** | **87.29** | 707301  |
| all fields (macro avg.)     | 84.92     | 84.63     | 84.5      | 707301  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 89.45     | 84.15     | 86.72     | 97183   |
| date                        | 91.55     | 85.85     | 88.61     | 97630   |
| doi                         | 75.56     | 88.82     | 81.65     | 16894   |
| first_author                | 95.56     | 89.82     | 92.6      | 97183   |
| inTitle                     | 92.14     | 88.03     | 90.04     | 96430   |
| issue                       | 93.93     | 90.77     | 92.32     | 30312   |
| page                        | 94.83     | 77.9      | 85.53     | 88597   |
| pmcid                       | 74.73     | 94.18     | 83.33     | 807     |
| pmid                        | 73.82     | 84.85     | 78.95     | 2093    |
| title                       | 93.1      | 91.42     | 92.26     | 92463   |
| volume                      | 95.97     | 94.78     | 95.37     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.57** | **87.59** | **90.01** | 707301  |
| all fields (macro avg.)     | 88.24     | 88.23     | 87.94     | 707301  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| authors                     | 94.66     | 89.05     | 91.77    | 97183   |
| date                        | 91.55     | 85.85     | 88.61    | 97630   |
| doi                         | 77.59     | 91.2      | 83.85    | 16894   |
| first_author                | 95.71     | 89.96     | 92.74    | 97183   |
| inTitle                     | 93.19     | 89.03     | 91.06    | 96430   |
| issue                       | 93.93     | 90.77     | 92.32    | 30312   |
| page                        | 94.83     | 77.9      | 85.53    | 88597   |
| pmcid                       | 74.73     | 94.18     | 83.33    | 807     |
| pmid                        | 73.82     | 84.85     | 78.95    | 2093    |
| title                       | 96        | 94.27     | 95.13    | 92463   |
| volume                      | 95.97     | 94.78     | 95.37    | 87709   |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **93.9**  | **88.85** | **91.3** | 707301  |
| all fields (macro avg.)     | 89.27     | 89.26     | 88.97    | 707301  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 91.65     | 86.22     | 88.85     | 97183   |
| date                        | 91.55     | 85.85     | 88.61     | 97630   |
| doi                         | 76.23     | 89.61     | 82.38     | 16894   |
| first_author                | 95.18     | 89.46     | 92.23     | 97183   |
| inTitle                     | 90.88     | 86.82     | 88.8      | 96430   |
| issue                       | 93.93     | 90.77     | 92.32     | 30312   |
| page                        | 94.83     | 77.9      | 85.53     | 88597   |
| pmcid                       | 65.78     | 82.9      | 73.36     | 807     |
| pmid                        | 69.95     | 80.41     | 74.82     | 2093    |
| title                       | 95.32     | 93.61     | 94.46     | 92463   |
| volume                      | 95.97     | 94.78     | 95.37     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.94** | **87.94** | **90.37** | 707301  |
| all fields (macro avg.)     | 87.39     | 87.12     | 86.98     | 707301  |

#### Instance-level results

```
Total expected instances: 		98799
Total extracted instances: 		97956
Total correct instances: 		43436 (strict) 
Total correct instances: 		54320 (soft) 
Total correct instances: 		58502 (Levenshtein) 
Total correct instances: 		55277 (RatcliffObershelp) 

Instance-level precision:	44.34 (strict) 
Instance-level precision:	55.45 (soft) 
Instance-level precision:	59.72 (Levenshtein) 
Instance-level precision:	56.43 (RatcliffObershelp) 

Instance-level recall:	43.96	(strict) 
Instance-level recall:	54.98	(soft) 
Instance-level recall:	59.21	(Levenshtein) 
Instance-level recall:	55.95	(RatcliffObershelp) 

Instance-level f-score:	44.15 (strict) 
Instance-level f-score:	55.22 (soft) 
Instance-level f-score:	59.47 (Levenshtein) 
Instance-level f-score:	56.19 (RatcliffObershelp) 

Matching 1 :	78844

Matching 2 :	4482

Matching 3 :	4342

Matching 4 :	2220

Total matches :	89888
```

#### Citation context resolution

```

Total expected references: 	 98797 - 49.4 references per article
Total predicted references: 	 97956 - 48.98 references per article

Total expected citation contexts: 	 142862 - 71.43 citation contexts per article
Total predicted citation contexts: 	 134780 - 67.39 citation contexts per article

Total correct predicted citation contexts: 	 116246 - 58.12 citation contexts per article
Total wrong predicted citation contexts: 	 18534 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 86.25
Recall citation contexts: 	 81.37
fscore citation contexts: 	 83.74
```

## Fulltext structures

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from
the actual PDF content and are can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the srtructure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 28.34     | 27.58     | 27.95     | 446     |
| conflict_stmt               | 66.42     | 59.11     | 62.55     | 609     |
| contribution_stmt           | 42.86     | 43.84     | 43.34     | 609     |
| figure_title                | 4.25      | 2.35      | 3.03      | 22978   |
| funding_stmt                | 4.13      | 24.1      | 7.05      | 747     |
| reference_citation          | 71.95     | 70.95     | 71.45     | 147470  |
| reference_figure            | 70.31     | 76.97     | 73.49     | 47984   |
| reference_table             | 45.12     | 84.74     | 58.88     | 5957    |
| section_title               | 69.07     | 68.62     | 68.85     | 32398   |
| table_title                 | 6.98      | 2.55      | 3.73      | 3925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **65.19** | **64.77** | **64.98** | 263123  |
| all fields (macro avg.)     | 40.94     | 46.08     | 42.03     | 263123  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| availability_stmt           | 49.54     | 48.21     | 48.86    | 446     |
| conflict_stmt               | 81        | 72.09     | 76.28    | 609     |
| contribution_stmt           | 72.71     | 74.38     | 73.54    | 609     |
| figure_title                | 66.76     | 36.97     | 47.59    | 22978   |
| funding_stmt                | 4.38      | 25.57     | 7.48     | 747     |
| reference_citation          | 84.28     | 83.11     | 83.69    | 147470  |
| reference_figure            | 70.97     | 77.69     | 74.18    | 47984   |
| reference_table             | 45.51     | 85.48     | 59.4     | 5957    |
| section_title               | 74.49     | 74        | 74.24    | 32398   |
| table_title                 | 81.23     | 29.66     | 43.45    | 3925    |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **76.45** | **75.96** | **76.2** | 263123  |
| all fields (macro avg.)     | 63.09     | 60.72     | 58.87    | 263123  |

**Document-level ratio results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 82.35     | 97.31     | 89.21     | 446     |
| conflict_stmt               | 95.42     | 89        | 92.1      | 609     |
| contribution_stmt           | 91.08     | 102.3     | 96.37     | 609     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.88** | **96.09** | **92.88** | 1664    |
| all fields (macro avg.)     | 89.62     | 96.2      | 92.56     | 1664    |

Evaluation metrics produced in 1607.083 seconds

