## Header metadata

Evaluation on 984 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| authors                     | 76.11     | 75.18    | 75.64     | 983     |
| first_author                | 90.01     | 89       | 89.5      | 982     |
| title                       | 84.94     | 65.35    | 73.87     | 984     |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **83.59** | **76.5** | **79.89** | 2949    |
| all fields (macro avg.)     | 83.69     | 76.51    | 79.67     | 2949    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 76.42     | 75.48     | 75.95     | 983     |
| first_author                | 90.01     | 89        | 89.5      | 982     |
| title                       | 90.75     | 69.82     | 78.92     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **85.33** | **78.09** | **81.55** | 2949    |
| all fields (macro avg.)     | 85.73     | 78.1      | 81.46     | 2949    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 90.94     | 89.83     | 90.38     | 983     |
| first_author                | 90.22     | 89.21     | 89.71     | 982     |
| title                       | 91.68     | 70.53     | 79.72     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.89** | **83.18** | **86.86** | 2949    |
| all fields (macro avg.)     | 90.94     | 83.19     | 86.6      | 2949    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| authors                     | 82.7      | 81.69    | 82.19     | 983     |
| first_author                | 90.01     | 89       | 89.5      | 982     |
| title                       | 91.28     | 70.22    | 79.38     | 984     |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **87.74** | **80.3** | **83.85** | 2949    |
| all fields (macro avg.)     | 88        | 80.3     | 83.69     | 2949    |

#### Instance-level results

```
Total expected instances: 	984
Total correct instances: 	558 (strict)
Total correct instances: 	599 (soft)
Total correct instances: 	650 (Levenshtein)
Total correct instances: 	622 (ObservedRatcliffObershelp)

Instance-level recall:	56.71	(strict)
Instance-level recall:	60.87	(soft)
Instance-level recall:	66.06	(Levenshtein)
Instance-level recall:	63.21	(RatcliffObershelp)
```

## Citation metadata

Evaluation on 984 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 79.67     | 78.01     | 78.83     | 63265   |
| date                        | 95.89     | 93.36     | 94.61     | 63662   |
| first_author                | 94.79     | 92.78     | 93.77     | 63265   |
| inTitle                     | 95.45     | 93.77     | 94.6      | 63213   |
| issue                       | 1.54      | 81.25     | 3.02      | 16      |
| page                        | 95.75     | 94.37     | 95.05     | 53375   |
| title                       | 90.25     | 90.09     | 90.17     | 62044   |
| volume                      | 97.77     | 97.76     | 97.76     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.54** | **91.35** | **91.94** | 429889  |
| all fields (macro avg.)     | 81.39     | 90.17     | 80.98     | 429889  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 79.8      | 78.15     | 78.97     | 63265   |
| date                        | 95.89     | 93.36     | 94.61     | 63662   |
| first_author                | 94.87     | 92.86     | 93.85     | 63265   |
| inTitle                     | 95.92     | 94.24     | 95.07     | 63213   |
| issue                       | 1.54      | 81.25     | 3.02      | 16      |
| page                        | 95.75     | 94.37     | 95.05     | 53375   |
| title                       | 95.89     | 95.72     | 95.81     | 62044   |
| volume                      | 97.77     | 97.76     | 97.76     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.47** | **92.27** | **92.86** | 429889  |
| all fields (macro avg.)     | 82.18     | 90.96     | 81.77     | 429889  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 93.41     | 91.47     | 92.43     | 63265   |
| date                        | 95.89     | 93.36     | 94.61     | 63662   |
| first_author                | 95.31     | 93.29     | 94.29     | 63265   |
| inTitle                     | 96.53     | 94.84     | 95.68     | 63213   |
| issue                       | 1.54      | 81.25     | 3.02      | 16      |
| page                        | 95.75     | 94.37     | 95.05     | 53375   |
| title                       | 97.67     | 97.5      | 97.58     | 62044   |
| volume                      | 97.77     | 97.76     | 97.76     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **95.86** | **94.64** | **95.25** | 429889  |
| all fields (macro avg.)     | 84.24     | 92.98     | 83.8      | 429889  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 86.96     | 85.15     | 86.04     | 63265   |
| date                        | 95.89     | 93.36     | 94.61     | 63662   |
| first_author                | 94.8      | 92.79     | 93.79     | 63265   |
| inTitle                     | 95.93     | 94.25     | 95.09     | 63213   |
| issue                       | 1.54      | 81.25     | 3.02      | 16      |
| page                        | 95.75     | 94.37     | 95.05     | 53375   |
| title                       | 97.51     | 97.34     | 97.43     | 62044   |
| volume                      | 97.77     | 97.76     | 97.76     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **94.74** | **93.52** | **94.13** | 429889  |
| all fields (macro avg.)     | 83.27     | 92.03     | 82.85     | 429889  |

#### Instance-level results

```
Total expected instances: 		63664
Total extracted instances: 		65174
Total correct instances: 		41611 (strict)
Total correct instances: 		44390 (soft)
Total correct instances: 		52021 (Levenshtein)
Total correct instances: 		48565 (RatcliffObershelp)

Instance-level precision:	63.85 (strict)
Instance-level precision:	68.11 (soft)
Instance-level precision:	79.82 (Levenshtein)
Instance-level precision:	74.52 (RatcliffObershelp)

Instance-level recall:	65.36	(strict)
Instance-level recall:	69.73	(soft)
Instance-level recall:	81.71	(Levenshtein)
Instance-level recall:	76.28	(RatcliffObershelp)

Instance-level f-score:	64.59 (strict)
Instance-level f-score:	68.91 (soft)
Instance-level f-score:	80.75 (Levenshtein)
Instance-level f-score:	75.39 (RatcliffObershelp)

Matching 1 :	58266

Matching 2 :	955

Matching 3 :	1234

Matching 4 :	384

Total matches :	60839
```

#### Citation context resolution

```

Total expected references: 	 63664 - 64.7 references per article
Total predicted references: 	 65174 - 66.23 references per article

Total expected citation contexts: 	 109022 - 110.79 citation contexts per article
Total predicted citation contexts: 	 93018 - 94.53 citation contexts per article

Total correct predicted citation contexts: 	 89774 - 91.23 citation contexts per article
Total wrong predicted citation contexts: 	 3244 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 96.51
Recall citation contexts: 	 82.34
fscore citation contexts: 	 88.87
```

Evaluation metrics produced in 317.237 seconds
