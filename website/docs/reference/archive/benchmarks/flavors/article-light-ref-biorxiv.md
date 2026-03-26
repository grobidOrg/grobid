## Header metadata

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 84.76     | 83.99     | 84.37     | 1999    |
| first_author                | 96.72     | 95.94     | 96.33     | 1997    |
| title                       | 77.18     | 76.1      | 76.64     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **86.23** | **85.34** | **85.78** | 5996    |
| all fields (macro avg.)     | 86.22     | 85.35     | 85.78     | 5996    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 85.26     | 84.49     | 84.87     | 1999    |
| first_author                | 96.97     | 96.19     | 96.58     | 1997    |
| title                       | 79.46     | 78.35     | 78.9      | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **87.24** | **86.34** | **86.79** | 5996    |
| all fields (macro avg.)     | 87.23     | 86.35     | 86.79     | 5996    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 92.53     | 91.7      | 92.11     | 1999    |
| first_author                | 97.17     | 96.39     | 96.78     | 1997    |
| title                       | 91.94     | 90.65     | 91.29     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.88** | **92.91** | **93.39** | 5996    |
| all fields (macro avg.)     | 93.88     | 92.91     | 93.39     | 5996    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 88.44     | 87.64     | 88.04     | 1999    |
| first_author                | 96.72     | 95.94     | 96.33     | 1997    |
| title                       | 87.68     | 86.45     | 87.06     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.95** | **90.01** | **90.48** | 5996    |
| all fields (macro avg.)     | 90.95     | 90.01     | 90.48     | 5996    |

#### Instance-level results

```
Total expected instances: 	2000
Total correct instances: 	1351 (strict) 
Total correct instances: 	1388 (soft) 
Total correct instances: 	1709 (Levenshtein) 
Total correct instances: 	1576 (ObservedRatcliffObershelp) 

Instance-level recall:	67.55	(strict) 
Instance-level recall:	69.4	(soft) 
Instance-level recall:	85.45	(Levenshtein) 
Instance-level recall:	78.8	(RatcliffObershelp) 
```

## Citation metadata

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 88.24     | 81.99     | 85        | 97183   |
| date                        | 91.49     | 84.76     | 88        | 97630   |
| doi                         | 70.91     | 81.39     | 75.79     | 16894   |
| first_author                | 95.08     | 88.27     | 91.55     | 97183   |
| inTitle                     | 82.59     | 77.93     | 80.19     | 96430   |
| issue                       | 93.91     | 89.84     | 91.83     | 30312   |
| page                        | 94.76     | 76.82     | 84.85     | 88597   |
| pmcid                       | 66.01     | 82.78     | 73.45     | 807     |
| pmid                        | 69.88     | 79.69     | 74.46     | 2093    |
| title                       | 84.71     | 82.16     | 83.42     | 92463   |
| volume                      | 95.89     | 93.57     | 94.71     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.7**  | **83.81** | **86.65** | 707301  |
| all fields (macro avg.)     | 84.86     | 83.56     | 83.93     | 707301  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 89.4      | 83.06     | 86.11     | 97183   |
| date                        | 91.49     | 84.76     | 88        | 97630   |
| doi                         | 75.4      | 86.55     | 80.59     | 16894   |
| first_author                | 95.51     | 88.67     | 91.96     | 97183   |
| inTitle                     | 92.07     | 86.88     | 89.4      | 96430   |
| issue                       | 93.91     | 89.84     | 91.83     | 30312   |
| page                        | 94.76     | 76.82     | 84.85     | 88597   |
| pmcid                       | 74.8      | 93.8      | 83.23     | 807     |
| pmid                        | 73.61     | 83.95     | 78.44     | 2093    |
| title                       | 92.97     | 90.17     | 91.55     | 92463   |
| volume                      | 95.89     | 93.57     | 94.71     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.5**  | **86.42** | **89.36** | 707301  |
| all fields (macro avg.)     | 88.16     | 87.1      | 87.33     | 707301  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 94.6      | 87.9      | 91.13     | 97183   |
| date                        | 91.49     | 84.76     | 88        | 97630   |
| doi                         | 77.42     | 88.87     | 82.75     | 16894   |
| first_author                | 95.66     | 88.8      | 92.1      | 97183   |
| inTitle                     | 93.11     | 87.86     | 90.41     | 96430   |
| issue                       | 93.91     | 89.84     | 91.83     | 30312   |
| page                        | 94.76     | 76.82     | 84.85     | 88597   |
| pmcid                       | 74.8      | 93.8      | 83.23     | 807     |
| pmid                        | 73.61     | 83.95     | 78.44     | 2093    |
| title                       | 95.94     | 93.05     | 94.47     | 92463   |
| volume                      | 95.89     | 93.57     | 94.71     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.84** | **87.67** | **90.65** | 707301  |
| all fields (macro avg.)     | 89.2      | 88.11     | 88.36     | 707301  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 91.59     | 85.1      | 88.22     | 97183   |
| date                        | 91.49     | 84.76     | 88        | 97630   |
| doi                         | 76.07     | 87.32     | 81.3      | 16894   |
| first_author                | 95.13     | 88.31     | 91.59     | 97183   |
| inTitle                     | 90.79     | 85.67     | 88.15     | 96430   |
| issue                       | 93.91     | 89.84     | 91.83     | 30312   |
| page                        | 94.76     | 76.82     | 84.85     | 88597   |
| pmcid                       | 66.01     | 82.78     | 73.45     | 807     |
| pmid                        | 69.88     | 79.69     | 74.46     | 2093    |
| title                       | 95.24     | 92.37     | 93.78     | 92463   |
| volume                      | 95.89     | 93.57     | 94.71     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.87** | **86.77** | **89.72** | 707301  |
| all fields (macro avg.)     | 87.34     | 86.02     | 86.4      | 707301  |

#### Instance-level results

```
Total expected instances: 		98799
Total extracted instances: 		96830
Total correct instances: 		42789 (strict) 
Total correct instances: 		53506 (soft) 
Total correct instances: 		57609 (Levenshtein) 
Total correct instances: 		54441 (RatcliffObershelp) 

Instance-level precision:	44.19 (strict) 
Instance-level precision:	55.26 (soft) 
Instance-level precision:	59.49 (Levenshtein) 
Instance-level precision:	56.22 (RatcliffObershelp) 

Instance-level recall:	43.31	(strict) 
Instance-level recall:	54.16	(soft) 
Instance-level recall:	58.31	(Levenshtein) 
Instance-level recall:	55.1	(RatcliffObershelp) 

Instance-level f-score:	43.75 (strict) 
Instance-level f-score:	54.7 (soft) 
Instance-level f-score:	58.9 (Levenshtein) 
Instance-level f-score:	55.66 (RatcliffObershelp) 

Matching 1 :	77743

Matching 2 :	4503

Matching 3 :	4304

Matching 4 :	2196

Total matches :	88746
```

#### Citation context resolution

```

Total expected references: 	 98797 - 49.4 references per article
Total predicted references: 	 96830 - 48.41 references per article

Total expected citation contexts: 	 142862 - 71.43 citation contexts per article
Total predicted citation contexts: 	 131079 - 65.54 citation contexts per article

Total correct predicted citation contexts: 	 111777 - 55.89 citation contexts per article
Total wrong predicted citation contexts: 	 19302 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 85.27
Recall citation contexts: 	 78.24
fscore citation contexts: 	 81.61
```

Evaluation metrics produced in 1408.979 seconds
