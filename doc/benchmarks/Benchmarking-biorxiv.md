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
| abstract                    | 2.31      | 2.26      | 2.28      | 1989    |
| affiliation_linked          | 0.83      | 0.83      | 0.83      | 16325   |
| authors                     | 84.85     | 84.38     | 84.62     | 1998    |
| first_author                | 96.73     | 96.29     | 96.51     | 1996    |
| keywords                    | 57.33     | 57.33     | 57.33     | 839     |
| title                       | 77.26     | 76.49     | 76.87     | 1999    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **23.23** | **23.06** | **23.14** | 25146   |
| all fields (macro avg.)     | 53.22     | 52.93     | 53.07     | 25146   |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 59.54     | 58.37     | 58.95     | 1989    |
| affiliation_linked          | 72.82     | 72.34     | 72.58     | 16325   |
| authors                     | 85.35     | 84.88     | 85.12     | 1998    |
| first_author                | 96.98     | 96.54     | 96.76     | 1996    |
| keywords                    | 63.05     | 63.05     | 63.05     | 839     |
| title                       | 79.48     | 78.69     | 79.08     | 1999    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **74.91** | **74.35** | **74.63** | 25146   |
| all fields (macro avg.)     | 76.21     | 75.65     | 75.92     | 25146   |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 80.1      | 78.53     | 79.31     | 1989    |
| affiliation_linked          | 75.63     | 75.13     | 75.38     | 16325   |
| authors                     | 92.55     | 92.04     | 92.3      | 1998    |
| first_author                | 97.23     | 96.79     | 97.01     | 1996    |
| keywords                    | 78.19     | 78.19     | 78.19     | 839     |
| title                       | 91.92     | 91        | 91.45     | 1999    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **80.42** | **79.83** | **80.12** | 25146   |
| all fields (macro avg.)     | 85.94     | 85.28     | 85.61     | 25146   |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 77.03     | 75.52     | 76.26     | 1989    |
| affiliation_linked          | 73.85     | 73.37     | 73.61     | 16325   |
| authors                     | 88.48     | 87.99     | 88.23     | 1998    |
| first_author                | 96.73     | 96.29     | 96.51     | 1996    |
| keywords                    | 70.32     | 70.32     | 70.32     | 839     |
| title                       | 87.67     | 86.79     | 87.23     | 1999    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **78.06** | **77.48** | **77.77** | 25146   |
| all fields (macro avg.)     | 82.35     | 81.71     | 82.03     | 25146   |

Note: the "affiliation_linked" field above is a linking-aware metric (each author is paired with its gold counterpart
and their attached affiliations compared). Only authors whose gold affiliation link is explicit are scored; affiliations
encoded purely positionally in the gold are out of scope.

#### Instance-level results

```
Total expected instances: 	1999
Total correct instances: 	37 (strict)
Total correct instances: 	724 (soft)
Total correct instances: 	1223 (Levenshtein)
Total correct instances: 	1053 (ObservedRatcliffObershelp)

Instance-level recall:	1.85	(strict)
Instance-level recall:	36.22	(soft)
Instance-level recall:	61.18	(Levenshtein)
Instance-level recall:	52.68	(RatcliffObershelp)
```

## Citation metadata

Evaluation on 1999 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 88.32     | 83.09     | 85.63     | 97113   |
| date                        | 91.55     | 85.85     | 88.61     | 97559   |
| doi                         | 71.01     | 83.52     | 76.76     | 16831   |
| first_author                | 95.14     | 89.42     | 92.19     | 97113   |
| inTitle                     | 82.72     | 79.03     | 80.83     | 96359   |
| issue                       | 93.93     | 90.77     | 92.32     | 30312   |
| page                        | 94.83     | 77.89     | 85.53     | 88534   |
| pmcid                       | 65.78     | 82.9      | 73.36     | 807     |
| pmid                        | 69.95     | 80.41     | 74.82     | 2093    |
| title                       | 84.81     | 83.28     | 84.04     | 92394   |
| volume                      | 95.97     | 94.78     | 95.37     | 87644   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.78** | **84.94** | **87.29** | 706759  |
| all fields (macro avg.)     | 84.91     | 84.63     | 84.5      | 706759  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 89.48     | 84.17     | 86.74     | 97113   |
| date                        | 91.55     | 85.85     | 88.61     | 97559   |
| doi                         | 75.49     | 88.79     | 81.6      | 16831   |
| first_author                | 95.57     | 89.82     | 92.61     | 97113   |
| inTitle                     | 92.14     | 88.03     | 90.04     | 96359   |
| issue                       | 93.93     | 90.77     | 92.32     | 30312   |
| page                        | 94.83     | 77.89     | 85.53     | 88534   |
| pmcid                       | 74.73     | 94.18     | 83.33     | 807     |
| pmid                        | 73.82     | 84.85     | 78.95     | 2093    |
| title                       | 93.1      | 91.42     | 92.26     | 92394   |
| volume                      | 95.97     | 94.78     | 95.37     | 87644   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.57** | **87.59** | **90.01** | 706759  |
| all fields (macro avg.)     | 88.24     | 88.23     | 87.94     | 706759  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| authors                     | 94.66     | 89.05     | 91.77    | 97113   |
| date                        | 91.55     | 85.85     | 88.61    | 97559   |
| doi                         | 77.52     | 91.18     | 83.8     | 16831   |
| first_author                | 95.71     | 89.96     | 92.75    | 97113   |
| inTitle                     | 93.19     | 89.03     | 91.06    | 96359   |
| issue                       | 93.93     | 90.77     | 92.32    | 30312   |
| page                        | 94.83     | 77.89     | 85.53    | 88534   |
| pmcid                       | 74.73     | 94.18     | 83.33    | 807     |
| pmid                        | 73.82     | 84.85     | 78.95    | 2093    |
| title                       | 96        | 94.27     | 95.13    | 92394   |
| volume                      | 95.97     | 94.78     | 95.37    | 87644   |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **93.9**  | **88.84** | **91.3** | 706759  |
| all fields (macro avg.)     | 89.26     | 89.26     | 88.97    | 706759  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 91.66     | 86.22     | 88.86     | 97113   |
| date                        | 91.55     | 85.85     | 88.61     | 97559   |
| doi                         | 76.16     | 89.58     | 82.33     | 16831   |
| first_author                | 95.19     | 89.47     | 92.24     | 97113   |
| inTitle                     | 90.88     | 86.82     | 88.8      | 96359   |
| issue                       | 93.93     | 90.77     | 92.32     | 30312   |
| page                        | 94.83     | 77.89     | 85.53     | 88534   |
| pmcid                       | 65.78     | 82.9      | 73.36     | 807     |
| pmid                        | 69.95     | 80.41     | 74.82     | 2093    |
| title                       | 95.32     | 93.6      | 94.45     | 92394   |
| volume                      | 95.97     | 94.78     | 95.37     | 87644   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.94** | **87.94** | **90.37** | 706759  |
| all fields (macro avg.)     | 87.38     | 87.12     | 86.97     | 706759  |

#### Instance-level results

```
Total expected instances: 		98728
Total extracted instances: 		97885
Total correct instances: 		43407 (strict)
Total correct instances: 		54287 (soft)
Total correct instances: 		58452 (Levenshtein)
Total correct instances: 		55233 (RatcliffObershelp)

Instance-level precision:	44.34 (strict)
Instance-level precision:	55.46 (soft)
Instance-level precision:	59.71 (Levenshtein)
Instance-level precision:	56.43 (RatcliffObershelp)

Instance-level recall:	43.97	(strict)
Instance-level recall:	54.99	(soft)
Instance-level recall:	59.21	(Levenshtein)
Instance-level recall:	55.94	(RatcliffObershelp)

Instance-level f-score:	44.15 (strict)
Instance-level f-score:	55.22 (soft)
Instance-level f-score:	59.46 (Levenshtein)
Instance-level f-score:	56.18 (RatcliffObershelp)

Matching 1 :	78781

Matching 2 :	4481

Matching 3 :	4341

Matching 4 :	2218

Total matches :	89821
```

#### Citation context resolution

```

Total expected references: 	 98726 - 49.39 references per article
Total predicted references: 	 97885 - 48.97 references per article

Total expected citation contexts: 	 142738 - 71.4 citation contexts per article
Total predicted citation contexts: 	 134644 - 67.36 citation contexts per article

Total correct predicted citation contexts: 	 116125 - 58.09 citation contexts per article
Total wrong predicted citation contexts: 	 18519 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 86.25
Recall citation contexts: 	 81.36
fscore citation contexts: 	 83.73
```

## Fulltext structures

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from
the actual PDF content and can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the structure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 1999 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 28.18     | 27.42     | 27.79     | 445     |
| conflict_stmt               | 66.36     | 59.05     | 62.49     | 608     |
| contribution_stmt           | 42.77     | 43.75     | 43.25     | 608     |
| figure_title                | 4.25      | 2.36      | 3.03      | 22970   |
| funding_stmt                | 3.84      | 23.96     | 6.62      | 747     |
| reference_citation          | 71.94     | 70.93     | 71.43     | 147346  |
| reference_figure            | 70.34     | 76.97     | 73.5      | 47972   |
| reference_table             | 45.33     | 84.74     | 59.06     | 5957    |
| section_title               | 69.07     | 68.63     | 68.85     | 32357   |
| table_title                 | 6.98      | 2.55      | 3.73      | 3925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **65.11** | **64.75** | **64.93** | 262935  |
| all fields (macro avg.)     | 40.9      | 46.03     | 41.98     | 262935  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 49.42     | 48.09     | 48.75     | 445     |
| conflict_stmt               | 80.96     | 72.04     | 76.24     | 608     |
| contribution_stmt           | 72.67     | 74.34     | 73.5      | 608     |
| figure_title                | 66.76     | 36.97     | 47.59     | 22970   |
| funding_stmt                | 4.1       | 25.57     | 7.06      | 747     |
| reference_citation          | 84.28     | 83.09     | 83.68     | 147346  |
| reference_figure            | 71        | 77.69     | 74.19     | 47972   |
| reference_table             | 45.72     | 85.48     | 59.58     | 5957    |
| section_title               | 74.47     | 74        | 74.24     | 32357   |
| table_title                 | 81.23     | 29.66     | 43.45     | 3925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **76.37** | **75.95** | **76.16** | 262935  |
| all fields (macro avg.)     | 63.06     | 60.69     | 58.83     | 262935  |

**Document-level ratio results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 82.32     | 97.3      | 89.19     | 445     |
| conflict_stmt               | 95.41     | 88.98     | 92.09     | 608     |
| contribution_stmt           | 91.07     | 102.3     | 96.36     | 608     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.86** | **96.09** | **92.87** | 1661    |
| all fields (macro avg.)     | 89.6      | 96.2      | 92.54     | 1661    |

Evaluation metrics produced in 205.891 seconds
