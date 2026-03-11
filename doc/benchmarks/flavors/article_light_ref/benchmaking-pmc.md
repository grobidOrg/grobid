## Header metadata

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 92.93     | 92.79     | 92.86     | 1941    |
| first_author                | 96.54     | 96.39     | 96.47     | 1941    |
| title                       | 84.32     | 83.89     | 84.11     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **91.27** | **91.02** | **91.15** | 5825    |
| all fields (macro avg.)     | 91.27     | 91.02     | 91.14     | 5825    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| authors                     | 94.89     | 94.74     | 94.82    | 1941    |
| first_author                | 96.96     | 96.81     | 96.88    | 1941    |
| title                       | 92.03     | 91.56     | 91.8     | 1943    |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **94.63** | **94.37** | **94.5** | 5825    |
| all fields (macro avg.)     | 94.63     | 94.37     | 94.5     | 5825    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 96.75     | 96.6      | 96.67     | 1941    |
| first_author                | 97.21     | 97.06     | 97.14     | 1941    |
| title                       | 98.24     | 97.74     | 97.99     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **97.4**  | **97.13** | **97.27** | 5825    |
| all fields (macro avg.)     | 97.4      | 97.13     | 97.27     | 5825    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 95.82     | 95.67     | 95.75     | 1941    |
| first_author                | 96.54     | 96.39     | 96.47     | 1941    |
| title                       | 96.22     | 95.73     | 95.98     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **96.2**  | **95.93** | **96.06** | 5825    |
| all fields (macro avg.)     | 96.2      | 95.93     | 96.06     | 5825    |

#### Instance-level results

```
Total expected instances: 	1943
Total correct instances: 	1528 (strict) 
Total correct instances: 	1696 (soft) 
Total correct instances: 	1839 (Levenshtein) 
Total correct instances: 	1784 (ObservedRatcliffObershelp) 

Instance-level recall:	78.64	(strict) 
Instance-level recall:	87.29	(soft) 
Instance-level recall:	94.65	(Levenshtein) 
Instance-level recall:	91.82	(RatcliffObershelp) 
```

## Citation metadata

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 82.01     | 75.3      | 78.51     | 85778   |
| date                        | 93.51     | 83.05     | 87.97     | 87067   |
| first_author                | 88.46     | 81.19     | 84.67     | 85778   |
| inTitle                     | 71.85     | 70.71     | 71.27     | 81007   |
| issue                       | 85.83     | 85.46     | 85.64     | 16635   |
| page                        | 93.34     | 83.24     | 88        | 80501   |
| title                       | 78.48     | 74.5      | 76.44     | 80736   |
| volume                      | 94.92     | 88.5      | 91.6      | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **85.9**  | **79.67** | **82.67** | 597569  |
| all fields (macro avg.)     | 86.05     | 80.24     | 83.01     | 597569  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 82.48     | 75.73     | 78.96     | 85778   |
| date                        | 93.51     | 83.05     | 87.97     | 87067   |
| first_author                | 88.63     | 81.34     | 84.83     | 85778   |
| inTitle                     | 83.25     | 81.93     | 82.59     | 81007   |
| issue                       | 85.83     | 85.46     | 85.64     | 16635   |
| page                        | 93.34     | 83.24     | 88        | 80501   |
| title                       | 89.99     | 85.44     | 87.66     | 80736   |
| volume                      | 94.92     | 88.5      | 91.6      | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.23** | **82.75** | **85.87** | 597569  |
| all fields (macro avg.)     | 88.99     | 83.09     | 85.91     | 597569  |

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
| authors                     | 84.94     | 77.99     | 81.32     | 85778   |
| date                        | 93.51     | 83.05     | 87.97     | 87067   |
| first_author                | 88.48     | 81.2      | 84.69     | 85778   |
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
Total correct instances: 		38619 (strict) 
Total correct instances: 		50592 (soft) 
Total correct instances: 		55410 (Levenshtein) 
Total correct instances: 		51988 (RatcliffObershelp) 

Instance-level precision:	44.74 (strict) 
Instance-level precision:	58.61 (soft) 
Instance-level precision:	64.2 (Levenshtein) 
Instance-level precision:	60.23 (RatcliffObershelp) 

Instance-level recall:	42.85	(strict) 
Instance-level recall:	56.14	(soft) 
Instance-level recall:	61.48	(Levenshtein) 
Instance-level recall:	57.68	(RatcliffObershelp) 

Instance-level f-score:	43.78 (strict) 
Instance-level f-score:	57.35 (soft) 
Instance-level f-score:	62.81 (Levenshtein) 
Instance-level f-score:	58.93 (RatcliffObershelp) 

Matching 1 :	67552

Matching 2 :	3953

Matching 3 :	1787

Matching 4 :	660

Total matches :	73952
```

#### Citation context resolution

```

Total expected references: 	 90125 - 46.38 references per article
Total predicted references: 	 86315 - 44.42 references per article

Total expected citation contexts: 	 139835 - 71.97 citation contexts per article
Total predicted citation contexts: 	 111653 - 57.46 citation contexts per article

Total correct predicted citation contexts: 	 94282 - 48.52 citation contexts per article
Total wrong predicted citation contexts: 	 17371 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 84.44
Recall citation contexts: 	 67.42
fscore citation contexts: 	 74.98
```

Evaluation metrics produced in 1145.951 seconds
