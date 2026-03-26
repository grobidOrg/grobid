## Header metadata

Evaluation on 984 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 77.47     | 76.6      | 77.03     | 983     |
| first_author                | 93.72     | 92.77     | 93.24     | 982     |
| title                       | 88.8      | 86.18     | 87.47     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **86.65** | **85.18** | **85.91** | 2949    |
| all fields (macro avg.)     | 86.66     | 85.18     | 85.92     | 2949    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 77.88     | 77.01     | 77.44     | 983     |
| first_author                | 93.72     | 92.77     | 93.24     | 982     |
| title                       | 95.81     | 92.99     | 94.38     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.1**  | **87.59** | **88.34** | 2949    |
| all fields (macro avg.)     | 89.14     | 87.59     | 88.36     | 2949    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| authors                     | 90.02     | 89.01    | 89.51     | 983     |
| first_author                | 94.03     | 93.08    | 93.55     | 982     |
| title                       | 97.38     | 94.51    | 95.93     | 984     |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **93.79** | **92.2** | **92.99** | 2949    |
| all fields (macro avg.)     | 93.81     | 92.2     | 93        | 2949    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 83.33     | 82.4      | 82.86     | 983     |
| first_author                | 93.72     | 92.77     | 93.24     | 982     |
| title                       | 97.28     | 94.41     | 95.82     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **91.41** | **89.86** | **90.63** | 2949    |
| all fields (macro avg.)     | 91.45     | 89.86     | 90.64     | 2949    |

#### Instance-level results

```
Total expected instances: 	984
Total correct instances: 	677 (strict) 
Total correct instances: 	727 (soft) 
Total correct instances: 	836 (Levenshtein) 
Total correct instances: 	783 (ObservedRatcliffObershelp) 

Instance-level recall:	68.8	(strict) 
Instance-level recall:	73.88	(soft) 
Instance-level recall:	84.96	(Levenshtein) 
Instance-level recall:	79.57	(RatcliffObershelp) 
```

Evaluation metrics produced in 10.247 seconds
