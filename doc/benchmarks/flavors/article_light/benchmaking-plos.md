## Header metadata

Evaluation on 1000 random PDF files out of 998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 99.07     | 98.97     | 99.02     | 969     |
| first_author                | 99.28     | 99.17     | 99.23     | 969     |
| title                       | 95.54     | 94.2      | 94.86     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **97.95** | **97.41** | **97.68** | 2938    |
| all fields (macro avg.)     | 97.96     | 97.45     | 97.7      | 2938    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| authors                     | 99.07     | 98.97    | 99.02     | 969     |
| first_author                | 99.28     | 99.17    | 99.23     | 969     |
| title                       | 99.09     | 97.7     | 98.39     | 1000    |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **99.14** | **98.6** | **98.87** | 2938    |
| all fields (macro avg.)     | 99.14     | 98.61    | 98.88     | 2938    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 99.48     | 99.38     | 99.43     | 969     |
| first_author                | 99.38     | 99.28     | 99.33     | 969     |
| title                       | 99.7      | 98.3      | 98.99     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **99.52** | **98.98** | **99.25** | 2938    |
| all fields (macro avg.)     | 99.52     | 98.99     | 99.25     | 2938    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 99.38     | 99.28     | 99.33     | 969     |
| first_author                | 99.28     | 99.17     | 99.23     | 969     |
| title                       | 99.29     | 97.9      | 98.59     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **99.32** | **98.77** | **99.04** | 2938    |
| all fields (macro avg.)     | 99.32     | 98.78     | 99.05     | 2938    |

#### Instance-level results

```
Total expected instances: 	1000
Total correct instances: 	937 (strict)
Total correct instances: 	971 (soft)
Total correct instances: 	978 (Levenshtein)
Total correct instances: 	974 (ObservedRatcliffObershelp)

Instance-level recall:	93.7	(strict)
Instance-level recall:	97.1	(soft)
Instance-level recall:	97.8	(Levenshtein)
Instance-level recall:	97.4	(RatcliffObershelp)
```

Evaluation metrics produced in 3.302 seconds
