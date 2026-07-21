## Header metadata

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 92.62     | 92.43     | 92.52     | 1941    |
| first_author                | 96.49     | 96.29     | 96.39     | 1941    |
| title                       | 84.39     | 84.05     | 84.22     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **91.17** | **90.92** | **91.04** | 5825    |
| all fields (macro avg.)     | 91.17     | 90.92     | 91.04     | 5825    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 94.58     | 94.38     | 94.48     | 1941    |
| first_author                | 96.9      | 96.7      | 96.8      | 1941    |
| title                       | 92.14     | 91.77     | 91.95     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **94.54** | **94.28** | **94.41** | 5825    |
| all fields (macro avg.)     | 94.54     | 94.28     | 94.41     | 5825    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 96.44     | 96.24     | 96.34     | 1941    |
| first_author                | 97.16     | 96.96     | 97.06     | 1941    |
| title                       | 98.29     | 97.89     | 98.09     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **97.3**  | **97.03** | **97.16** | 5825    |
| all fields (macro avg.)     | 97.3      | 97.03     | 97.16     | 5825    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 95.51     | 95.31     | 95.41     | 1941    |
| first_author                | 96.49     | 96.29     | 96.39     | 1941    |
| title                       | 96.28     | 95.88     | 96.08     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **96.09** | **95.83** | **95.96** | 5825    |
| all fields (macro avg.)     | 96.09     | 95.83     | 95.96     | 5825    |

#### Instance-level results

```
Total expected instances: 	1943
Total correct instances: 	1526 (strict)
Total correct instances: 	1694 (soft)
Total correct instances: 	1835 (Levenshtein)
Total correct instances: 	1781 (ObservedRatcliffObershelp)

Instance-level recall:	78.54	(strict)
Instance-level recall:	87.18	(soft)
Instance-level recall:	94.44	(Levenshtein)
Instance-level recall:	91.66	(RatcliffObershelp)
```

Evaluation metrics produced in 3.284 seconds
