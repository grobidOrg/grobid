## Header metadata

Evaluation on 1000 random PDF files out of 998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 99.07     | 98.86     | 98.97     | 969     |
| first_author                | 99.28     | 99.07     | 99.17     | 969     |
| title                       | 95.75     | 94.6      | 95.17     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **98.02** | **97.48** | **97.75** | 2938    |
| all fields (macro avg.)     | 98.03     | 97.51     | 97.77     | 2938    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 99.07     | 98.86     | 98.97     | 969     |
| first_author                | 99.28     | 99.07     | 99.17     | 969     |
| title                       | 99.29     | 98.1      | 98.69     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **99.21** | **98.67** | **98.94** | 2938    |
| all fields (macro avg.)     | 99.21     | 98.68     | 98.94     | 2938    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 99.48     | 99.28     | 99.38     | 969     |
| first_author                | 99.38     | 99.17     | 99.28     | 969     |
| title                       | 99.7      | 98.5      | 99.09     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **99.52** | **98.98** | **99.25** | 2938    |
| all fields (macro avg.)     | 99.52     | 98.98     | 99.25     | 2938    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 99.38     | 99.17     | 99.28     | 969     |
| first_author                | 99.28     | 99.07     | 99.17     | 969     |
| title                       | 99.49     | 98.3      | 98.89     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **99.38** | **98.84** | **99.11** | 2938    |
| all fields (macro avg.)     | 99.38     | 98.85     | 99.11     | 2938    |

#### Instance-level results

```
Total expected instances: 	1000
Total correct instances: 	941 (strict) 
Total correct instances: 	975 (soft) 
Total correct instances: 	979 (Levenshtein) 
Total correct instances: 	977 (ObservedRatcliffObershelp) 

Instance-level recall:	94.1	(strict) 
Instance-level recall:	97.5	(soft) 
Instance-level recall:	97.9	(Levenshtein) 
Instance-level recall:	97.7	(RatcliffObershelp) 
```

Evaluation metrics produced in 8.787 seconds
