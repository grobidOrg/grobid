## Header metadata

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 84.48     | 83.59     | 84.03     | 1999    |
| first_author                | 96.41     | 95.49     | 95.95     | 1997    |
| title                       | 77.18     | 75.95     | 76.56     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **86.04** | **85.01** | **85.52** | 5996    |
| all fields (macro avg.)     | 86.02     | 85.01     | 85.52     | 5996    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 84.93     | 84.04     | 84.49     | 1999    |
| first_author                | 96.66     | 95.74     | 96.2      | 1997    |
| title                       | 79.37     | 78.1      | 78.73     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **87**    | **85.96** | **86.48** | 5996    |
| all fields (macro avg.)     | 86.99     | 85.96     | 86.47     | 5996    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 92.21     | 91.25     | 91.73     | 1999    |
| first_author                | 96.92     | 95.99     | 96.45     | 1997    |
| title                       | 91.77     | 90.3      | 91.03     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.64** | **92.51** | **93.07** | 5996    |
| all fields (macro avg.)     | 93.63     | 92.51     | 93.07     | 5996    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 88.17     | 87.24     | 87.7      | 1999    |
| first_author                | 96.41     | 95.49     | 95.95     | 1997    |
| title                       | 87.6      | 86.2      | 86.9      | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.73** | **89.64** | **90.18** | 5996    |
| all fields (macro avg.)     | 90.73     | 89.65     | 90.18     | 5996    |

#### Instance-level results

```
Total expected instances: 	2000
Total correct instances: 	1346 (strict) 
Total correct instances: 	1381 (soft) 
Total correct instances: 	1701 (Levenshtein) 
Total correct instances: 	1570 (ObservedRatcliffObershelp) 

Instance-level recall:	67.3	(strict) 
Instance-level recall:	69.05	(soft) 
Instance-level recall:	85.05	(Levenshtein) 
Instance-level recall:	78.5	(RatcliffObershelp) 
```

Evaluation metrics produced in 12.073 seconds
