## Header metadata

Evaluation on 984 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 78.27     | 77.31     | 77.79     | 983     |
| first_author                | 94.13     | 93.08     | 93.6      | 982     |
| title                       | 89.29     | 86.38     | 87.81     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **87.21** | **85.59** | **86.39** | 2949    |
| all fields (macro avg.)     | 87.23     | 85.59     | 86.4      | 2949    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall | f1        | support |
|-----------------------------|-----------|--------|-----------|---------|
| authors                     | 78.68     | 77.72  | 78.2      | 983     |
| first_author                | 94.13     | 93.08  | 93.6      | 982     |
| title                       | 96.32     | 93.19  | 94.73     | 984     |
|                             |           |        |           |         |
| **all fields (micro avg.)** | **89.67** | **88** | **88.82** | 2949    |
| all fields (macro avg.)     | 89.71     | 88     | 88.84     | 2949    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 90.53     | 89.42     | 89.97     | 983     |
| first_author                | 94.44     | 93.38     | 93.91     | 982     |
| title                       | 97.79     | 94.61     | 96.18     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **94.23** | **92.47** | **93.34** | 2949    |
| all fields (macro avg.)     | 94.25     | 92.47     | 93.35     | 2949    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| authors                     | 83.63     | 82.6     | 83.11     | 983     |
| first_author                | 94.13     | 93.08    | 93.6      | 982     |
| title                       | 97.79     | 94.61    | 96.18     | 984     |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **91.81** | **90.1** | **90.95** | 2949    |
| all fields (macro avg.)     | 91.85     | 90.1     | 90.96     | 2949    |

#### Instance-level results

```
Total expected instances: 	984
Total correct instances: 	685 (strict) 
Total correct instances: 	735 (soft) 
Total correct instances: 	843 (Levenshtein) 
Total correct instances: 	787 (ObservedRatcliffObershelp) 

Instance-level recall:	69.61	(strict) 
Instance-level recall:	74.7	(soft) 
Instance-level recall:	85.67	(Levenshtein) 
Instance-level recall:	79.98	(RatcliffObershelp) 
```

## Citation metadata

Evaluation on 984 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 79.65     | 78        | 78.81     | 63265   |
| date                        | 95.89     | 93.36     | 94.61     | 63662   |
| first_author                | 94.78     | 92.78     | 93.77     | 63265   |
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
| authors                     | 79.79     | 78.13     | 78.95     | 63265   |
| date                        | 95.89     | 93.36     | 94.61     | 63662   |
| first_author                | 94.87     | 92.86     | 93.85     | 63265   |
| inTitle                     | 95.92     | 94.24     | 95.07     | 63213   |
| issue                       | 1.54      | 81.25     | 3.02      | 16      |
| page                        | 95.75     | 94.37     | 95.05     | 53375   |
| title                       | 95.89     | 95.72     | 95.81     | 62044   |
| volume                      | 97.77     | 97.76     | 97.76     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.46** | **92.26** | **92.86** | 429889  |
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
| authors                     | 86.95     | 85.14     | 86.03     | 63265   |
| date                        | 95.89     | 93.36     | 94.61     | 63662   |
| first_author                | 94.8      | 92.79     | 93.78     | 63265   |
| inTitle                     | 95.93     | 94.25     | 95.09     | 63213   |
| issue                       | 1.54      | 81.25     | 3.02      | 16      |
| page                        | 95.75     | 94.37     | 95.05     | 53375   |
| title                       | 97.51     | 97.34     | 97.43     | 62044   |
| volume                      | 97.77     | 97.76     | 97.76     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **94.74** | **93.52** | **94.12** | 429889  |
| all fields (macro avg.)     | 83.27     | 92.03     | 82.85     | 429889  |

#### Instance-level results

```
Total expected instances: 		63664
Total extracted instances: 		65174
Total correct instances: 		41600 (strict) 
Total correct instances: 		44379 (soft) 
Total correct instances: 		52020 (Levenshtein) 
Total correct instances: 		48560 (RatcliffObershelp) 

Instance-level precision:	63.83 (strict) 
Instance-level precision:	68.09 (soft) 
Instance-level precision:	79.82 (Levenshtein) 
Instance-level precision:	74.51 (RatcliffObershelp) 

Instance-level recall:	65.34	(strict) 
Instance-level recall:	69.71	(soft) 
Instance-level recall:	81.71	(Levenshtein) 
Instance-level recall:	76.28	(RatcliffObershelp) 

Instance-level f-score:	64.58 (strict) 
Instance-level f-score:	68.89 (soft) 
Instance-level f-score:	80.75 (Levenshtein) 
Instance-level f-score:	75.38 (RatcliffObershelp) 

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
Total predicted citation contexts: 	 93048 - 94.56 citation contexts per article

Total correct predicted citation contexts: 	 89788 - 91.25 citation contexts per article
Total wrong predicted citation contexts: 	 3260 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 96.5
Recall citation contexts: 	 82.36
fscore citation contexts: 	 88.87
```

Evaluation metrics produced in 1098.998 seconds
