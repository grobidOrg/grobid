## Header metadata

Evaluation on 1000 random PDF files out of 998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 98.97     | 98.97     | 98.97     | 969     |
| first_author                | 99.17     | 99.17     | 99.17     | 969     |
| title                       | 95.67     | 95.1      | 95.39     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **97.92** | **97.72** | **97.82** | 2938    |
| all fields (macro avg.)     | 97.94     | 97.75     | 97.84     | 2938    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 98.97     | 98.97     | 98.97     | 969     |
| first_author                | 99.17     | 99.17     | 99.17     | 969     |
| title                       | 99.2      | 98.6      | 98.9      | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **99.11** | **98.91** | **99.01** | 2938    |
| all fields (macro avg.)     | 99.11     | 98.91     | 99.01     | 2938    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 99.38     | 99.38     | 99.38     | 969     |
| first_author                | 99.28     | 99.28     | 99.28     | 969     |
| title                       | 99.5      | 98.9      | 99.2      | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **99.39** | **99.18** | **99.28** | 2938    |
| all fields (macro avg.)     | 99.39     | 99.19     | 99.29     | 2938    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 99.28     | 99.28     | 99.28     | 969     |
| first_author                | 99.17     | 99.17     | 99.17     | 969     |
| title                       | 99.3      | 98.7      | 99        | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **99.25** | **99.05** | **99.15** | 2938    |
| all fields (macro avg.)     | 99.25     | 99.05     | 99.15     | 2938    |

#### Instance-level results

```
Total expected instances: 	1000
Total correct instances: 	946 (strict) 
Total correct instances: 	980 (soft) 
Total correct instances: 	984 (Levenshtein) 
Total correct instances: 	982 (ObservedRatcliffObershelp) 

Instance-level recall:	94.6	(strict) 
Instance-level recall:	98	(soft) 
Instance-level recall:	98.4	(Levenshtein) 
Instance-level recall:	98.2	(RatcliffObershelp) 
```

## Citation metadata

Evaluation on 1000 random PDF files out of 998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 80.99     | 78.01     | 79.47     | 44770   |
| date                        | 84.35     | 80.62     | 82.44     | 45457   |
| first_author                | 91.25     | 87.86     | 89.53     | 44770   |
| inTitle                     | 81.61     | 83.12     | 82.36     | 42795   |
| issue                       | 93.41     | 92.1      | 92.75     | 18983   |
| page                        | 93.75     | 77.48     | 84.84     | 40844   |
| title                       | 59.85     | 60.22     | 60.03     | 43101   |
| volume                      | 95.68     | 95.59     | 95.64     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **84.08** | **81.03** | **82.52** | 321178  |
| all fields (macro avg.)     | 85.11     | 81.87     | 83.38     | 321178  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| authors                     | 81.31     | 78.32     | 79.79    | 44770   |
| date                        | 84.35     | 80.62     | 82.44    | 45457   |
| first_author                | 91.48     | 88.08     | 89.75    | 44770   |
| inTitle                     | 85.39     | 86.98     | 86.18    | 42795   |
| issue                       | 93.41     | 92.1      | 92.75    | 18983   |
| page                        | 93.75     | 77.48     | 84.84    | 40844   |
| title                       | 91.71     | 92.27     | 91.99    | 43101   |
| volume                      | 95.68     | 95.59     | 95.64    | 40458   |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **89.15** | **85.91** | **87.5** | 321178  |
| all fields (macro avg.)     | 89.63     | 86.43     | 87.92    | 321178  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 90.42     | 87.09     | 88.73     | 44770   |
| date                        | 84.35     | 80.62     | 82.44     | 45457   |
| first_author                | 92.01     | 88.6      | 90.27     | 44770   |
| inTitle                     | 86.28     | 87.88     | 87.07     | 42795   |
| issue                       | 93.41     | 92.1      | 92.75     | 18983   |
| page                        | 93.75     | 77.48     | 84.84     | 40844   |
| title                       | 94.23     | 94.81     | 94.52     | 43101   |
| volume                      | 95.68     | 95.59     | 95.64     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.97** | **87.67** | **89.29** | 321178  |
| all fields (macro avg.)     | 91.27     | 88.02     | 89.53     | 321178  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 84.75     | 81.63     | 83.16     | 44770   |
| date                        | 84.35     | 80.62     | 82.44     | 45457   |
| first_author                | 91.25     | 87.86     | 89.53     | 44770   |
| inTitle                     | 85.02     | 86.6      | 85.8      | 42795   |
| issue                       | 93.41     | 92.1      | 92.75     | 18983   |
| page                        | 93.75     | 77.48     | 84.84     | 40844   |
| title                       | 93.61     | 94.18     | 93.89     | 43101   |
| volume                      | 95.68     | 95.59     | 95.64     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.81** | **86.55** | **88.15** | 321178  |
| all fields (macro avg.)     | 90.23     | 87.01     | 88.51     | 321178  |

#### Instance-level results

```
Total expected instances: 		48449
Total extracted instances: 		47788
Total correct instances: 		13501 (strict) 
Total correct instances: 		22245 (soft) 
Total correct instances: 		24850 (Levenshtein) 
Total correct instances: 		23233 (RatcliffObershelp) 

Instance-level precision:	28.25 (strict) 
Instance-level precision:	46.55 (soft) 
Instance-level precision:	52 (Levenshtein) 
Instance-level precision:	48.62 (RatcliffObershelp) 

Instance-level recall:	27.87	(strict) 
Instance-level recall:	45.91	(soft) 
Instance-level recall:	51.29	(Levenshtein) 
Instance-level recall:	47.95	(RatcliffObershelp) 

Instance-level f-score:	28.06 (strict) 
Instance-level f-score:	46.23 (soft) 
Instance-level f-score:	51.64 (Levenshtein) 
Instance-level f-score:	48.28 (RatcliffObershelp) 

Matching 1 :	35094

Matching 2 :	1260

Matching 3 :	3278

Matching 4 :	1849

Total matches :	41481
```

#### Citation context resolution

```

Total expected references: 	 48449 - 48.45 references per article
Total predicted references: 	 47788 - 47.79 references per article

Total expected citation contexts: 	 69755 - 69.75 citation contexts per article
Total predicted citation contexts: 	 74480 - 74.48 citation contexts per article

Total correct predicted citation contexts: 	 57425 - 57.42 citation contexts per article
Total wrong predicted citation contexts: 	 17055 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 77.1
Recall citation contexts: 	 82.32
fscore citation contexts: 	 79.63
```

Evaluation metrics produced in 678.894 seconds
