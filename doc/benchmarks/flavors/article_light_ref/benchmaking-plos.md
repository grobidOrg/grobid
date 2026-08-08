## Header metadata

Evaluation on 1000 random PDF files out of 998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 98.97     | 98.97     | 98.97     | 969     |
| first_author                | 99.17     | 99.17     | 99.17     | 969     |
| title                       | 95.76     | 94.8      | 95.28     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **97.95** | **97.62** | **97.78** | 2938    |
| all fields (macro avg.)     | 97.97     | 97.65     | 97.81     | 2938    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 98.97     | 98.97     | 98.97     | 969     |
| first_author                | 99.17     | 99.17     | 99.17     | 969     |
| title                       | 99.29     | 98.3      | 98.79     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **99.15** | **98.81** | **98.98** | 2938    |
| all fields (macro avg.)     | 99.15     | 98.81     | 98.98     | 2938    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 99.38     | 99.38     | 99.38     | 969     |
| first_author                | 99.28     | 99.28     | 99.28     | 969     |
| title                       | 99.7      | 98.7      | 99.2      | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **99.45** | **99.12** | **99.28** | 2938    |
| all fields (macro avg.)     | 99.45     | 99.12     | 99.28     | 2938    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 99.28     | 99.28     | 99.28     | 969     |
| first_author                | 99.17     | 99.17     | 99.17     | 969     |
| title                       | 99.39     | 98.4      | 98.89     | 1000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **99.28** | **98.94** | **99.11** | 2938    |
| all fields (macro avg.)     | 99.28     | 98.95     | 99.12     | 2938    |

#### Instance-level results

```
Total expected instances: 	1000
Total correct instances: 	943 (strict)
Total correct instances: 	977 (soft)
Total correct instances: 	982 (Levenshtein)
Total correct instances: 	979 (ObservedRatcliffObershelp)

Instance-level recall:	94.3	(strict)
Instance-level recall:	97.7	(soft)
Instance-level recall:	98.2	(Levenshtein)
Instance-level recall:	97.9	(RatcliffObershelp)
```

## Citation metadata

Evaluation on 1000 random PDF files out of 998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 81.03     | 78.05     | 79.51     | 44770   |
| date                        | 84.35     | 80.62     | 82.44     | 45457   |
| first_author                | 91.26     | 87.88     | 89.54     | 44770   |
| inTitle                     | 81.61     | 83.12     | 82.36     | 42795   |
| issue                       | 93.41     | 92.1      | 92.75     | 18983   |
| page                        | 93.75     | 77.48     | 84.84     | 40844   |
| title                       | 59.85     | 60.22     | 60.03     | 43101   |
| volume                      | 95.68     | 95.59     | 95.64     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **84.08** | **81.03** | **82.53** | 321178  |
| all fields (macro avg.)     | 85.12     | 81.88     | 83.39     | 321178  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 81.35     | 78.36     | 79.83     | 44770   |
| date                        | 84.35     | 80.62     | 82.44     | 45457   |
| first_author                | 91.49     | 88.09     | 89.76     | 44770   |
| inTitle                     | 85.39     | 86.98     | 86.18     | 42795   |
| issue                       | 93.41     | 92.1      | 92.75     | 18983   |
| page                        | 93.75     | 77.48     | 84.84     | 40844   |
| title                       | 91.71     | 92.27     | 91.99     | 43101   |
| volume                      | 95.68     | 95.59     | 95.64     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.16** | **85.92** | **87.51** | 321178  |
| all fields (macro avg.)     | 89.64     | 86.44     | 87.93     | 321178  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 90.42     | 87.09     | 88.73     | 44770   |
| date                        | 84.35     | 80.62     | 82.44     | 45457   |
| first_author                | 92.02     | 88.61     | 90.28     | 44770   |
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
| authors                     | 84.77     | 81.65     | 83.18     | 44770   |
| date                        | 84.35     | 80.62     | 82.44     | 45457   |
| first_author                | 91.26     | 87.88     | 89.54     | 44770   |
| inTitle                     | 85.02     | 86.6      | 85.8      | 42795   |
| issue                       | 93.41     | 92.1      | 92.75     | 18983   |
| page                        | 93.75     | 77.48     | 84.84     | 40844   |
| title                       | 93.61     | 94.18     | 93.89     | 43101   |
| volume                      | 95.68     | 95.59     | 95.64     | 40458   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.81** | **86.56** | **88.15** | 321178  |
| all fields (macro avg.)     | 90.23     | 87.01     | 88.51     | 321178  |

#### Instance-level results

```
Total expected instances: 		48449
Total extracted instances: 		47788
Total correct instances: 		13510 (strict)
Total correct instances: 		22256 (soft)
Total correct instances: 		24854 (Levenshtein)
Total correct instances: 		23239 (RatcliffObershelp)

Instance-level precision:	28.27 (strict)
Instance-level precision:	46.57 (soft)
Instance-level precision:	52.01 (Levenshtein)
Instance-level precision:	48.63 (RatcliffObershelp)

Instance-level recall:	27.88	(strict)
Instance-level recall:	45.94	(soft)
Instance-level recall:	51.3	(Levenshtein)
Instance-level recall:	47.97	(RatcliffObershelp)

Instance-level f-score:	28.08 (strict)
Instance-level f-score:	46.25 (soft)
Instance-level f-score:	51.65 (Levenshtein)
Instance-level f-score:	48.3 (RatcliffObershelp)

Matching 1 :	35094

Matching 2 :	1261

Matching 3 :	3277

Matching 4 :	1849

Total matches :	41481
```

#### Citation context resolution

```

Total expected references: 	 48449 - 48.45 references per article
Total predicted references: 	 47788 - 47.79 references per article

Total expected citation contexts: 	 69755 - 69.75 citation contexts per article
Total predicted citation contexts: 	 74454 - 74.45 citation contexts per article

Total correct predicted citation contexts: 	 57418 - 57.42 citation contexts per article
Total wrong predicted citation contexts: 	 17036 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 77.12
Recall citation contexts: 	 82.31
fscore citation contexts: 	 79.63
```

Evaluation metrics produced in 166.763 seconds
