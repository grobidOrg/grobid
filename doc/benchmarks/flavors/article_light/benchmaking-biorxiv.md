## Header metadata

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 84.54     | 83.69     | 84.11     | 1999    |
| first_author                | 96.41     | 95.54     | 95.98     | 1997    |
| title                       | 77.09     | 75.9      | 76.49     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **86.03** | **85.04** | **85.53** | 5996    |
| all fields (macro avg.)     | 86.01     | 85.05     | 85.53     | 5996    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 84.99     | 84.14     | 84.57     | 1999    |
| first_author                | 96.66     | 95.79     | 96.23     | 1997    |
| title                       | 79.18     | 77.95     | 78.56     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **86.96** | **85.96** | **86.45** | 5996    |
| all fields (macro avg.)     | 86.94     | 85.96     | 86.45     | 5996    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 92.17     | 91.25     | 91.7      | 1999    |
| first_author                | 96.87     | 95.99     | 96.43     | 1997    |
| title                       | 91.77     | 90.35     | 91.06     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.61** | **92.53** | **93.06** | 5996    |
| all fields (macro avg.)     | 93.6      | 92.53     | 93.06     | 5996    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 88.13     | 87.24     | 87.68     | 1999    |
| first_author                | 96.41     | 95.54     | 95.98     | 1997    |
| title                       | 87.35     | 86        | 86.67     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.64** | **89.59** | **90.11** | 5996    |
| all fields (macro avg.)     | 90.63     | 89.6      | 90.11     | 5996    |

#### Instance-level results

```
Total expected instances: 	2000
Total correct instances: 	1343 (strict)
Total correct instances: 	1375 (soft)
Total correct instances: 	1698 (Levenshtein)
Total correct instances: 	1560 (ObservedRatcliffObershelp)

Instance-level recall:	67.15	(strict)
Instance-level recall:	68.75	(soft)
Instance-level recall:	84.9	(Levenshtein)
Instance-level recall:	78	(RatcliffObershelp)
```

Evaluation metrics produced in 4.294 seconds
