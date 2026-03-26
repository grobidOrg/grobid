## Header metadata

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 93.03     | 92.84     | 92.93     | 1941    |
| first_author                | 96.64     | 96.45     | 96.54     | 1941    |
| title                       | 84.44     | 84.05     | 84.24     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **91.37** | **91.11** | **91.24** | 5825    |
| all fields (macro avg.)     | 91.37     | 91.11     | 91.24     | 5825    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 94.99     | 94.8      | 94.89     | 1941    |
| first_author                | 97.06     | 96.86     | 96.96     | 1941    |
| title                       | 92.14     | 91.71     | 91.93     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **94.73** | **94.45** | **94.59** | 5825    |
| all fields (macro avg.)     | 94.73     | 94.46     | 94.59     | 5825    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 96.8      | 96.6      | 96.7      | 1941    |
| first_author                | 97.32     | 97.11     | 97.22     | 1941    |
| title                       | 98.29     | 97.84     | 98.07     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **97.47** | **97.18** | **97.33** | 5825    |
| all fields (macro avg.)     | 97.47     | 97.18     | 97.33     | 5825    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall | f1        | support |
|-----------------------------|-----------|--------|-----------|---------|
| authors                     | 95.92     | 95.72  | 95.82     | 1941    |
| first_author                | 96.64     | 96.45  | 96.54     | 1941    |
| title                       | 96.28     | 95.83  | 96.05     | 1943    |
|                             |           |        |           |         |
| **all fields (micro avg.)** | **96.28** | **96** | **96.14** | 5825    |
| all fields (macro avg.)     | 96.28     | 96     | 96.14     | 5825    |

#### Instance-level results

```
Total expected instances: 	1943
Total correct instances: 	1531 (strict) 
Total correct instances: 	1699 (soft) 
Total correct instances: 	1840 (Levenshtein) 
Total correct instances: 	1786 (ObservedRatcliffObershelp) 

Instance-level recall:	78.8	(strict) 
Instance-level recall:	87.44	(soft) 
Instance-level recall:	94.7	(Levenshtein) 
Instance-level recall:	91.92	(RatcliffObershelp) 
```

Evaluation metrics produced in 12.202 seconds
