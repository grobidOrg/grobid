## Header metadata

Evaluation on 1998 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 84.46     | 83.58     | 84.02     | 1997    |
| first_author                | 96.41     | 95.49     | 95.95     | 1995    |
| title                       | 77.16     | 75.93     | 76.54     | 1998    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **86.03** | **84.99** | **85.51** | 5990    |
| all fields (macro avg.)     | 86.01     | 85        | 85.5      | 5990    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 84.92     | 84.03     | 84.47     | 1997    |
| first_author                | 96.66     | 95.74     | 96.2      | 1995    |
| title                       | 79.35     | 78.08     | 78.71     | 1998    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **86.99** | **85.94** | **86.46** | 5990    |
| all fields (macro avg.)     | 86.98     | 85.95     | 86.46     | 5990    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| authors                     | 92.21     | 91.24    | 91.72     | 1997    |
| first_author                | 96.91     | 95.99    | 96.45     | 1995    |
| title                       | 91.76     | 90.29    | 91.02     | 1998    |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **93.63** | **92.5** | **93.06** | 5990    |
| all fields (macro avg.)     | 93.63     | 92.51    | 93.06     | 5990    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 88.16     | 87.23     | 87.69     | 1997    |
| first_author                | 96.41     | 95.49     | 95.95     | 1995    |
| title                       | 87.59     | 86.19     | 86.88     | 1998    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.72** | **89.63** | **90.17** | 5990    |
| all fields (macro avg.)     | 90.72     | 89.64     | 90.17     | 5990    |

#### Instance-level results

```
Total expected instances: 	1998
Total correct instances: 	1344 (strict) 
Total correct instances: 	1379 (soft) 
Total correct instances: 	1699 (Levenshtein) 
Total correct instances: 	1568 (ObservedRatcliffObershelp) 

Instance-level recall:	67.27	(strict) 
Instance-level recall:	69.02	(soft) 
Instance-level recall:	85.04	(Levenshtein) 
Instance-level recall:	78.48	(RatcliffObershelp) 
```

Evaluation metrics produced in 12.216 seconds
