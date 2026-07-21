## Header metadata

Evaluation on 984 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| authors                     | 75.21     | 74.36     | 74.78    | 983     |
| first_author                | 89.92     | 89        | 89.46    | 982     |
| title                       | 84.68     | 65.14     | 73.64    | 984     |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **83.15** | **76.16** | **79.5** | 2949    |
| all fields (macro avg.)     | 83.27     | 76.17     | 79.29    | 2949    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 75.51     | 74.67     | 75.09     | 983     |
| first_author                | 89.92     | 89        | 89.46     | 982     |
| title                       | 90.49     | 69.61     | 78.69     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **84.89** | **77.76** | **81.17** | 2949    |
| all fields (macro avg.)     | 85.31     | 77.76     | 81.08     | 2949    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 89.61     | 88.61     | 89.1      | 983     |
| first_author                | 90.23     | 89.31     | 89.76     | 982     |
| title                       | 91.41     | 70.33     | 79.49     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.34** | **82.74** | **86.37** | 2949    |
| all fields (macro avg.)     | 90.42     | 82.75     | 86.12     | 2949    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 81.38     | 80.47     | 80.92     | 983     |
| first_author                | 89.92     | 89        | 89.46     | 982     |
| title                       | 91.02     | 70.02     | 79.15     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **87.15** | **79.82** | **83.33** | 2949    |
| all fields (macro avg.)     | 87.44     | 79.83     | 83.18     | 2949    |

#### Instance-level results

```
Total expected instances: 	984
Total correct instances: 	554 (strict)
Total correct instances: 	594 (soft)
Total correct instances: 	643 (Levenshtein)
Total correct instances: 	617 (ObservedRatcliffObershelp)

Instance-level recall:	56.3	(strict)
Instance-level recall:	60.37	(soft)
Instance-level recall:	65.35	(Levenshtein)
Instance-level recall:	62.7	(RatcliffObershelp)
```

Evaluation metrics produced in 3.667 seconds
