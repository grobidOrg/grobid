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

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 2.26      | 2.21      | 2.24      | 1990    |
| authors                     | 84.13     | 83.54     | 83.84     | 1999    |
| first_author                | 96.17     | 95.59     | 95.88     | 1997    |
| keywords                    | 49.6      | 51.61     | 50.58     | 839     |
| title                       | 76.04     | 75.55     | 75.8      | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **63.44** | **63.08** | **63.26** | 8825    |
| all fields (macro avg.)     | 61.64     | 61.7      | 61.67     | 8825    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| abstract                    | 57.63     | 56.33     | 56.98    | 1990    |
| authors                     | 84.53     | 83.94     | 84.24    | 1999    |
| first_author                | 96.32     | 95.74     | 96.03    | 1997    |
| keywords                    | 54.07     | 56.26     | 55.14    | 839     |
| title                       | 78.41     | 77.9      | 78.15    | 2000    |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **76.82** | **76.39** | **76.6** | 8825    |
| all fields (macro avg.)     | 74.19     | 74.03     | 74.11    | 8825    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 79.13     | 77.34     | 78.22     | 1990    |
| authors                     | 92.04     | 91.4      | 91.72     | 1999    |
| first_author                | 96.62     | 96.04     | 96.33     | 1997    |
| keywords                    | 73.88     | 76.88     | 75.35     | 839     |
| title                       | 91.6      | 91        | 91.3      | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **88.31** | **87.81** | **88.06** | 8825    |
| all fields (macro avg.)     | 86.65     | 86.53     | 86.58     | 8825    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1     | support |
|-----------------------------|-----------|-----------|--------|---------|
| abstract                    | 75.48     | 73.77     | 74.61  | 1990    |
| authors                     | 87.76     | 87.14     | 87.45  | 1999    |
| first_author                | 96.17     | 95.59     | 95.88  | 1997    |
| keywords                    | 62.08     | 64.6      | 63.32  | 839     |
| title                       | 87.12     | 86.55     | 86.83  | 2000    |
|                             |           |           |        |         |
| **all fields (micro avg.)** | **84.24** | **83.76** | **84** | 8825    |
| all fields (macro avg.)     | 81.72     | 81.53     | 81.62  | 8825    |

#### Instance-level results

```
Total expected instances: 	2000
Total correct instances: 	37 (strict) 
Total correct instances: 	641 (soft) 
Total correct instances: 	1176 (Levenshtein) 
Total correct instances: 	978 (ObservedRatcliffObershelp) 

Instance-level recall:	1.85	(strict) 
Instance-level recall:	32.05	(soft) 
Instance-level recall:	58.8	(Levenshtein) 
Instance-level recall:	48.9	(RatcliffObershelp) 
```

## Citation metadata

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 87.25     | 81.11     | 84.07     | 97183   |
| date                        | 90.77     | 84.01     | 87.26     | 97630   |
| doi                         | 68.31     | 75.43     | 71.69     | 16894   |
| first_author                | 94.26     | 87.54     | 90.78     | 97183   |
| inTitle                     | 82.48     | 78.11     | 80.23     | 96430   |
| issue                       | 92.34     | 84.86     | 88.44     | 30312   |
| page                        | 94.38     | 77.78     | 85.28     | 88597   |
| pmcid                       | 66.24     | 82.65     | 73.54     | 807     |
| pmid                        | 68.23     | 81.56     | 74.3      | 2093    |
| title                       | 84.61     | 82.04     | 83.3      | 92463   |
| volume                      | 95.36     | 93.56     | 94.45     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.09** | **83.26** | **86.08** | 707301  |
| all fields (macro avg.)     | 84.02     | 82.6      | 83.03     | 707301  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 88.39     | 82.17     | 85.16     | 97183   |
| date                        | 90.77     | 84.01     | 87.26     | 97630   |
| doi                         | 72.82     | 80.41     | 76.43     | 16894   |
| first_author                | 94.67     | 87.92     | 91.17     | 97183   |
| inTitle                     | 91.9      | 87.03     | 89.4      | 96430   |
| issue                       | 92.34     | 84.86     | 88.44     | 30312   |
| page                        | 94.38     | 77.78     | 85.28     | 88597   |
| pmcid                       | 75.17     | 93.8      | 83.46     | 807     |
| pmid                        | 72.7      | 86.91     | 79.17     | 2093    |
| title                       | 92.89     | 90.07     | 91.46     | 92463   |
| volume                      | 95.36     | 93.56     | 94.45     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **91.88** | **85.87** | **88.78** | 707301  |
| all fields (macro avg.)     | 87.4      | 86.23     | 86.52     | 707301  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 93.71     | 87.12     | 90.29     | 97183   |
| date                        | 90.77     | 84.01     | 87.26     | 97630   |
| doi                         | 76.99     | 85.02     | 80.81     | 16894   |
| first_author                | 94.82     | 88.06     | 91.31     | 97183   |
| inTitle                     | 92.84     | 87.92     | 90.31     | 96430   |
| issue                       | 92.34     | 84.86     | 88.44     | 30312   |
| page                        | 94.38     | 77.78     | 85.28     | 88597   |
| pmcid                       | 75.17     | 93.8      | 83.46     | 807     |
| pmid                        | 72.74     | 86.96     | 79.22     | 2093    |
| title                       | 95.79     | 92.88     | 94.31     | 92463   |
| volume                      | 95.36     | 93.56     | 94.45     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.27** | **87.17** | **90.12** | 707301  |
| all fields (macro avg.)     | 88.63     | 87.45     | 87.74     | 707301  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 90.59     | 84.22     | 87.29     | 97183   |
| date                        | 90.77     | 84.01     | 87.26     | 97630   |
| doi                         | 74.72     | 82.51     | 78.42     | 16894   |
| first_author                | 94.3      | 87.58     | 90.82     | 97183   |
| inTitle                     | 90.71     | 85.9      | 88.24     | 96430   |
| issue                       | 92.34     | 84.86     | 88.44     | 30312   |
| page                        | 94.38     | 77.78     | 85.28     | 88597   |
| pmcid                       | 66.24     | 82.65     | 73.54     | 807     |
| pmid                        | 68.23     | 81.56     | 74.3      | 2093    |
| title                       | 95.12     | 92.24     | 93.66     | 92463   |
| volume                      | 95.36     | 93.56     | 94.45     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.3**  | **86.26** | **89.18** | 707301  |
| all fields (macro avg.)     | 86.62     | 85.17     | 85.61     | 707301  |

#### Instance-level results

```
Total expected instances: 		98799
Total extracted instances: 		98373
Total correct instances: 		41161 (strict) 
Total correct instances: 		51566 (soft) 
Total correct instances: 		55887 (Levenshtein) 
Total correct instances: 		52758 (RatcliffObershelp) 

Instance-level precision:	41.84 (strict) 
Instance-level precision:	52.42 (soft) 
Instance-level precision:	56.81 (Levenshtein) 
Instance-level precision:	53.63 (RatcliffObershelp) 

Instance-level recall:	41.66	(strict) 
Instance-level recall:	52.19	(soft) 
Instance-level recall:	56.57	(Levenshtein) 
Instance-level recall:	53.4	(RatcliffObershelp) 

Instance-level f-score:	41.75 (strict) 
Instance-level f-score:	52.31 (soft) 
Instance-level f-score:	56.69 (Levenshtein) 
Instance-level f-score:	53.51 (RatcliffObershelp) 

Matching 1 :	77036

Matching 2 :	4315

Matching 3 :	4784

Matching 4 :	2655

Total matches :	88790
```

#### Citation context resolution

```

Total expected references: 	 98797 - 49.4 references per article
Total predicted references: 	 98373 - 49.19 references per article

Total expected citation contexts: 	 142862 - 71.43 citation contexts per article
Total predicted citation contexts: 	 132881 - 66.44 citation contexts per article

Total correct predicted citation contexts: 	 113221 - 56.61 citation contexts per article
Total wrong predicted citation contexts: 	 19660 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 85.2
Recall citation contexts: 	 79.25
fscore citation contexts: 	 82.12
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
| availability_stmt           | 29.05     | 25.34     | 27.07     | 446     |
| figure_title                | 4.3       | 2.35      | 3.04      | 22978   |
| funding_stmt                | 3.73      | 23.29     | 6.43      | 747     |
| reference_citation          | 71.97     | 70.64     | 71.3      | 147470  |
| reference_figure            | 70.38     | 77.13     | 73.6      | 47984   |
| reference_table             | 45.62     | 86.64     | 59.76     | 5957    |
| section_title               | 71.29     | 69.91     | 70.59     | 32398   |
| table_title                 | 7.41      | 2.7       | 3.96      | 3925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **65.49** | **64.88** | **65.19** | 261905  |
| all fields (macro avg.)     | 37.97     | 44.75     | 39.47     | 261905  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 49.87     | 43.5      | 46.47     | 446     |
| figure_title                | 68.24     | 37.29     | 48.23     | 22978   |
| funding_stmt                | 3.96      | 24.77     | 6.83      | 747     |
| reference_citation          | 84.28     | 82.73     | 83.5      | 147470  |
| reference_figure            | 71.02     | 77.83     | 74.27     | 47984   |
| reference_table             | 46.03     | 87.43     | 60.31     | 5957    |
| section_title               | 76.85     | 75.36     | 76.1      | 32398   |
| table_title                 | 82.73     | 30.14     | 44.18     | 3925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **76.73** | **76.02** | **76.38** | 261905  |
| all fields (macro avg.)     | 60.37     | 57.38     | 54.99     | 261905  |

**Document-level ratio results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 84.2      | 87.22     | 85.68     | 446     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **84.2**  | **87.22** | **85.68** | 446     |
| all fields (macro avg.)     | 84.2      | 87.22     | 85.68     | 446     |

Evaluation metrics produced in 1598.033 seconds

