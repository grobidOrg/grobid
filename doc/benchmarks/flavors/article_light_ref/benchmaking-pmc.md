## Header metadata

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 92.67     | 92.53     | 92.6      | 1941    |
| first_author                | 96.39     | 96.24     | 96.31     | 1941    |
| title                       | 84.24     | 83.89     | 84.06     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **91.1**  | **90.88** | **90.99** | 5825    |
| all fields (macro avg.)     | 91.1      | 90.89     | 90.99     | 5825    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 94.63     | 94.49     | 94.56     | 1941    |
| first_author                | 96.8      | 96.65     | 96.73     | 1941    |
| title                       | 91.99     | 91.61     | 91.8      | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **94.48** | **94.25** | **94.36** | 5825    |
| all fields (macro avg.)     | 94.47     | 94.25     | 94.36     | 5825    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall | f1        | support |
|-----------------------------|-----------|--------|-----------|---------|
| authors                     | 96.44     | 96.29  | 96.37     | 1941    |
| first_author                | 97.06     | 96.91  | 96.98     | 1941    |
| title                       | 98.19     | 97.79  | 97.99     | 1943    |
|                             |           |        |           |         |
| **all fields (micro avg.)** | **97.23** | **97** | **97.11** | 5825    |
| all fields (macro avg.)     | 97.23     | 97     | 97.11     | 5825    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 95.56     | 95.41     | 95.49     | 1941    |
| first_author                | 96.39     | 96.24     | 96.31     | 1941    |
| title                       | 96.12     | 95.73     | 95.93     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **96.02** | **95.79** | **95.91** | 5825    |
| all fields (macro avg.)     | 96.02     | 95.79     | 95.91     | 5825    |

#### Instance-level results

```
Total expected instances: 	1943
Total correct instances: 	1527 (strict)
Total correct instances: 	1695 (soft)
Total correct instances: 	1836 (Levenshtein)
Total correct instances: 	1782 (ObservedRatcliffObershelp)

Instance-level recall:	78.59	(strict)
Instance-level recall:	87.24	(soft)
Instance-level recall:	94.49	(Levenshtein)
Instance-level recall:	91.71	(RatcliffObershelp)
```

## Citation metadata

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 82.03     | 75.31     | 78.53     | 85778   |
| date                        | 93.51     | 83.05     | 87.97     | 87067   |
| first_author                | 88.46     | 81.19     | 84.67     | 85778   |
| inTitle                     | 71.85     | 70.71     | 71.27     | 81007   |
| issue                       | 85.83     | 85.46     | 85.64     | 16635   |
| page                        | 93.34     | 83.24     | 88        | 80501   |
| title                       | 78.48     | 74.5      | 76.44     | 80736   |
| volume                      | 94.92     | 88.5      | 91.6      | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **85.9**  | **79.67** | **82.67** | 597569  |
| all fields (macro avg.)     | 86.05     | 80.25     | 83.02     | 597569  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 82.5      | 75.75     | 78.98     | 85778   |
| date                        | 93.51     | 83.05     | 87.97     | 87067   |
| first_author                | 88.63     | 81.35     | 84.83     | 85778   |
| inTitle                     | 83.25     | 81.93     | 82.59     | 81007   |
| issue                       | 85.83     | 85.46     | 85.64     | 16635   |
| page                        | 93.34     | 83.24     | 88        | 80501   |
| title                       | 89.99     | 85.44     | 87.66     | 80736   |
| volume                      | 94.92     | 88.5      | 91.6      | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.23** | **82.75** | **85.87** | 597569  |
| all fields (macro avg.)     | 89        | 83.09     | 85.91     | 597569  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 88.13     | 80.92     | 84.37     | 85778   |
| date                        | 93.51     | 83.05     | 87.97     | 87067   |
| first_author                | 88.82     | 81.52     | 85.02     | 85778   |
| inTitle                     | 84.56     | 83.21     | 83.88     | 81007   |
| issue                       | 85.83     | 85.46     | 85.64     | 16635   |
| page                        | 93.34     | 83.24     | 88        | 80501   |
| title                       | 92.21     | 87.54     | 89.82     | 80736   |
| volume                      | 94.92     | 88.5      | 91.6      | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.55** | **83.98** | **87.14** | 597569  |
| all fields (macro avg.)     | 90.17     | 84.18     | 87.04     | 597569  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 84.95     | 77.99     | 81.32     | 85778   |
| date                        | 93.51     | 83.05     | 87.97     | 87067   |
| first_author                | 88.48     | 81.21     | 84.69     | 85778   |
| inTitle                     | 81.86     | 80.56     | 81.2      | 81007   |
| issue                       | 85.83     | 85.46     | 85.64     | 16635   |
| page                        | 93.34     | 83.24     | 88        | 80501   |
| title                       | 91.85     | 87.2      | 89.46     | 80736   |
| volume                      | 94.92     | 88.5      | 91.6      | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.61** | **83.11** | **86.24** | 597569  |
| all fields (macro avg.)     | 89.34     | 83.4      | 86.24     | 597569  |

#### Instance-level results

```
Total expected instances: 		90125
Total extracted instances: 		86315
Total correct instances: 		38626 (strict)
Total correct instances: 		50600 (soft)
Total correct instances: 		55410 (Levenshtein)
Total correct instances: 		51990 (RatcliffObershelp)

Instance-level precision:	44.75 (strict)
Instance-level precision:	58.62 (soft)
Instance-level precision:	64.2 (Levenshtein)
Instance-level precision:	60.23 (RatcliffObershelp)

Instance-level recall:	42.86	(strict)
Instance-level recall:	56.14	(soft)
Instance-level recall:	61.48	(Levenshtein)
Instance-level recall:	57.69	(RatcliffObershelp)

Instance-level f-score:	43.78 (strict)
Instance-level f-score:	57.36 (soft)
Instance-level f-score:	62.81 (Levenshtein)
Instance-level f-score:	58.93 (RatcliffObershelp)

Matching 1 :	67552

Matching 2 :	3955

Matching 3 :	1786

Matching 4 :	659

Total matches :	73952
```

#### Citation context resolution

```

Total expected references: 	 90125 - 46.38 references per article
Total predicted references: 	 86315 - 44.42 references per article

Total expected citation contexts: 	 139835 - 71.97 citation contexts per article
Total predicted citation contexts: 	 111525 - 57.4 citation contexts per article

Total correct predicted citation contexts: 	 94195 - 48.48 citation contexts per article
Total wrong predicted citation contexts: 	 17330 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 84.46
Recall citation contexts: 	 67.36
fscore citation contexts: 	 74.95
```

Evaluation metrics produced in 272.546 seconds
