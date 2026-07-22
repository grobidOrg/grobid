# Benchmarking PubMed Central

## General

This is the end-to-end benchmarking result for GROBID version **0.9.0** against the `PMC_sample_1943` dataset, see
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
| abstract                    | 16.34     | 16.06     | 16.2      | 1911    |
| affiliation_linked          | 0.63      | 0.68      | 0.66      | 1927    |
| authors                     | 92.89     | 92.84     | 92.86     | 1941    |
| first_author                | 96.7      | 96.65     | 96.68     | 1941    |
| keywords                    | 63.19     | 61.09     | 62.12     | 1380    |
| title                       | 84.4      | 84.1      | 84.25     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **30.21** | **31.45** | **30.82** | 11043   |
| all fields (macro avg.)     | 59.03     | 58.57     | 58.79     | 11043   |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 62.96     | 61.9      | 62.43     | 1911    |
| affiliation_linked          | 61.66     | 66.61     | 64.04     | 1927    |
| authors                     | 94.85     | 94.8      | 94.82     | 1941    |
| first_author                | 97.16     | 97.11     | 97.14     | 1941    |
| keywords                    | 71.14     | 68.77     | 69.93     | 1380    |
| title                       | 92.05     | 91.71     | 91.88     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **71.23** | **74.14** | **72.65** | 11043   |
| all fields (macro avg.)     | 79.97     | 80.15     | 80.04     | 11043   |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 89.84     | 88.33     | 89.08     | 1911    |
| affiliation_linked          | 64.17     | 69.32     | 66.64     | 1927    |
| authors                     | 96.7      | 96.65     | 96.68     | 1941    |
| first_author                | 97.37     | 97.32     | 97.35     | 1941    |
| keywords                    | 84.71     | 81.88     | 83.27     | 1380    |
| title                       | 98.24     | 97.89     | 98.07     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **76.6**  | **79.73** | **78.13** | 11043   |
| all fields (macro avg.)     | 88.5      | 88.57     | 88.51     | 11043   |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| abstract                    | 85.74     | 84.3      | 85.01    | 1911    |
| affiliation_linked          | 62.59     | 67.62     | 65.01    | 1927    |
| authors                     | 95.77     | 95.72     | 95.75    | 1941    |
| first_author                | 96.7      | 96.65     | 96.68    | 1941    |
| keywords                    | 78.26     | 75.65     | 76.93    | 1380    |
| title                       | 96.23     | 95.88     | 96.06    | 1943    |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **74.6**  | **77.65** | **76.1** | 11043   |
| all fields (macro avg.)     | 85.88     | 85.97     | 85.91    | 11043   |

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
Total expected instances: 	1943
Total correct instances: 	204 (strict)
Total correct instances: 	877 (soft)
Total correct instances: 	1430 (Levenshtein)
Total correct instances: 	1259 (ObservedRatcliffObershelp)

Instance-level recall:	10.5	(strict)
Instance-level recall:	45.14	(soft)
Instance-level recall:	73.6	(Levenshtein)
Instance-level recall:	64.8	(RatcliffObershelp)
```

## Citation metadata

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 82.49     | 75.36     | 78.77     | 85778   |
| date                        | 94.56     | 83.09     | 88.46     | 87067   |
| first_author                | 88.97     | 81.24     | 84.93     | 85778   |
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
| authors                     | 82.96     | 75.79     | 79.22     | 85778   |
| date                        | 94.56     | 83.09     | 88.46     | 87067   |
| first_author                | 89.14     | 81.4      | 85.09     | 85778   |
| inTitle                     | 84.08     | 82.01     | 83.03     | 81007   |
| issue                       | 90.38     | 85.18     | 87.7      | 16635   |
| page                        | 94.6      | 83.37     | 88.63     | 80501   |
| title                       | 90.88     | 85.52     | 88.12     | 80736   |
| volume                      | 96.06     | 88.54     | 92.15     | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.2**  | **82.81** | **86.35** | 597569  |
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
| authors                     | 85.42     | 78.04     | 81.56     | 85778   |
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
Total extracted instances: 		85510
Total correct instances: 		38667 (strict)
Total correct instances: 		50686 (soft)
Total correct instances: 		55504 (Levenshtein)
Total correct instances: 		52060 (RatcliffObershelp)

Instance-level precision:	45.22 (strict)
Instance-level precision:	59.27 (soft)
Instance-level precision:	64.91 (Levenshtein)
Instance-level precision:	60.88 (RatcliffObershelp)

Instance-level recall:	42.9	(strict)
Instance-level recall:	56.24	(soft)
Instance-level recall:	61.59	(Levenshtein)
Instance-level recall:	57.76	(RatcliffObershelp)

Instance-level f-score:	44.03 (strict)
Instance-level f-score:	57.72 (soft)
Instance-level f-score:	63.2 (Levenshtein)
Instance-level f-score:	59.28 (RatcliffObershelp)

Matching 1 :	67606

Matching 2 :	3945

Matching 3 :	1783

Matching 4 :	661

Total matches :	73995
```

#### Citation context resolution

```

Total expected references: 	 90125 - 46.38 references per article
Total predicted references: 	 85510 - 44.01 references per article

Total expected citation contexts: 	 139835 - 71.97 citation contexts per article
Total predicted citation contexts: 	 115000 - 59.19 citation contexts per article

Total correct predicted citation contexts: 	 97231 - 50.04 citation contexts per article
Total wrong predicted citation contexts: 	 17769 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 84.55
Recall citation contexts: 	 69.53
fscore citation contexts: 	 76.31
```

## Fulltext structures

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from
the actual PDF content and can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the structure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| figure_title                | 31.99     | 26.67     | 29.09     | 7281    |
| reference_citation          | 58.11     | 58.84     | 58.47     | 134196  |
| reference_figure            | 60.6      | 68.29     | 64.22     | 19330   |
| reference_table             | 82.91     | 89.64     | 86.14     | 7327    |
| section_title               | 72.48     | 67.69     | 70        | 27619   |
| table_title                 | 67.72     | 49.66     | 57.3      | 3971    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **60.56** | **60.75** | **60.65** | 199724  |
| all fields (macro avg.)     | 62.3      | 60.13     | 60.87     | 199724  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| figure_title                | 79.92     | 66.63     | 72.67     | 7281    |
| reference_citation          | 62.4      | 63.18     | 62.78     | 134196  |
| reference_figure            | 61.09     | 68.85     | 64.74     | 19330   |
| reference_table             | 83.07     | 89.82     | 86.31     | 7327    |
| section_title               | 77.88     | 72.73     | 75.21     | 27619   |
| table_title                 | 94.3      | 69.15     | 79.79     | 3971    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **66.06** | **66.27** | **66.16** | 199724  |
| all fields (macro avg.)     | 76.44     | 71.72     | 73.59     | 199724  |

**Document-level ratio results**

| label                       | precision | recall | f1    | support |
|-----------------------------|-----------|--------|-------|---------|
|                             |           |        |       |         |
| **all fields (micro avg.)** | **0**     | **0**  | **0** | 0       |
| all fields (macro avg.)     | 0         | 0      | 0     | 0       |

Evaluation metrics produced in 162.688 seconds


