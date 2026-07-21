## Header metadata

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 84.65     | 83.89     | 84.27     | 1999    |
| first_author                | 96.77     | 95.99     | 96.38     | 1997    |
| title                       | 76.99     | 75.95     | 76.47     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **86.15** | **85.27** | **85.71** | 5996    |
| all fields (macro avg.)     | 86.14     | 85.28     | 85.71     | 5996    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 85.16     | 84.39     | 84.77     | 1999    |
| first_author                | 97.02     | 96.24     | 96.63     | 1997    |
| title                       | 79.17     | 78.1      | 78.63     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **87.13** | **86.24** | **86.68** | 5996    |
| all fields (macro avg.)     | 87.12     | 86.25     | 86.68     | 5996    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 92.33     | 91.5      | 91.91     | 1999    |
| first_author                | 97.17     | 96.39     | 96.78     | 1997    |
| title                       | 91.89     | 90.65     | 91.27     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.8**  | **92.85** | **93.32** | 5996    |
| all fields (macro avg.)     | 93.8      | 92.85     | 93.32     | 5996    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 88.29     | 87.49     | 87.89     | 1999    |
| first_author                | 96.77     | 95.99     | 96.38     | 1997    |
| title                       | 87.33     | 86.15     | 86.74     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.8**  | **89.88** | **90.34** | 5996    |
| all fields (macro avg.)     | 90.8      | 89.88     | 90.34     | 5996    |

#### Instance-level results

```
Total expected instances: 	2000
Total correct instances: 	1344 (strict)
Total correct instances: 	1377 (soft)
Total correct instances: 	1702 (Levenshtein)
Total correct instances: 	1562 (ObservedRatcliffObershelp)

Instance-level recall:	67.2	(strict)
Instance-level recall:	68.85	(soft)
Instance-level recall:	85.1	(Levenshtein)
Instance-level recall:	78.1	(RatcliffObershelp)
```

## Citation metadata

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 88.28     | 82.02     | 85.03     | 97183   |
| date                        | 91.5      | 84.76     | 88        | 97630   |
| doi                         | 70.91     | 81.39     | 75.79     | 16894   |
| first_author                | 95.09     | 88.28     | 91.56     | 97183   |
| inTitle                     | 82.59     | 77.93     | 80.2      | 96430   |
| issue                       | 93.91     | 89.84     | 91.83     | 30312   |
| page                        | 94.76     | 76.82     | 84.85     | 88597   |
| pmcid                       | 66.01     | 82.78     | 73.45     | 807     |
| pmid                        | 69.88     | 79.69     | 74.46     | 2093    |
| title                       | 84.71     | 82.16     | 83.42     | 92463   |
| volume                      | 95.89     | 93.57     | 94.71     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.71** | **83.81** | **86.66** | 707301  |
| all fields (macro avg.)     | 84.86     | 83.57     | 83.94     | 707301  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 89.43     | 83.1      | 86.15     | 97183   |
| date                        | 91.5      | 84.76     | 88        | 97630   |
| doi                         | 75.4      | 86.55     | 80.59     | 16894   |
| first_author                | 95.52     | 88.67     | 91.97     | 97183   |
| inTitle                     | 92.07     | 86.88     | 89.4      | 96430   |
| issue                       | 93.91     | 89.84     | 91.83     | 30312   |
| page                        | 94.76     | 76.82     | 84.85     | 88597   |
| pmcid                       | 74.8      | 93.8      | 83.23     | 807     |
| pmid                        | 73.61     | 83.95     | 78.44     | 2093    |
| title                       | 92.97     | 90.17     | 91.55     | 92463   |
| volume                      | 95.89     | 93.57     | 94.71     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.51** | **86.43** | **89.37** | 707301  |
| all fields (macro avg.)     | 88.17     | 87.1      | 87.34     | 707301  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 94.6      | 87.9      | 91.13     | 97183   |
| date                        | 91.5      | 84.76     | 88        | 97630   |
| doi                         | 77.42     | 88.87     | 82.75     | 16894   |
| first_author                | 95.66     | 88.81     | 92.11     | 97183   |
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
| authors                     | 91.6      | 85.11     | 88.23     | 97183   |
| date                        | 91.5      | 84.76     | 88        | 97630   |
| doi                         | 76.07     | 87.32     | 81.3      | 16894   |
| first_author                | 95.14     | 88.32     | 91.6      | 97183   |
| inTitle                     | 90.79     | 85.67     | 88.15     | 96430   |
| issue                       | 93.91     | 89.84     | 91.83     | 30312   |
| page                        | 94.76     | 76.82     | 84.85     | 88597   |
| pmcid                       | 66.01     | 82.78     | 73.45     | 807     |
| pmid                        | 69.88     | 79.69     | 74.46     | 2093    |
| title                       | 95.24     | 92.37     | 93.78     | 92463   |
| volume                      | 95.89     | 93.57     | 94.71     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.88** | **86.77** | **89.72** | 707301  |
| all fields (macro avg.)     | 87.34     | 86.02     | 86.4      | 707301  |

#### Instance-level results

```
Total expected instances: 		98799
Total extracted instances: 		96830
Total correct instances: 		42806 (strict)
Total correct instances: 		53526 (soft)
Total correct instances: 		57614 (Levenshtein)
Total correct instances: 		54451 (RatcliffObershelp)

Instance-level precision:	44.21 (strict)
Instance-level precision:	55.28 (soft)
Instance-level precision:	59.5 (Levenshtein)
Instance-level precision:	56.23 (RatcliffObershelp)

Instance-level recall:	43.33	(strict)
Instance-level recall:	54.18	(soft)
Instance-level recall:	58.31	(Levenshtein)
Instance-level recall:	55.11	(RatcliffObershelp)

Instance-level f-score:	43.76 (strict)
Instance-level f-score:	54.72 (soft)
Instance-level f-score:	58.9 (Levenshtein)
Instance-level f-score:	55.67 (RatcliffObershelp)

Matching 1 :	77746

Matching 2 :	4504

Matching 3 :	4302

Matching 4 :	2195

Total matches :	88747
```

#### Citation context resolution

```

Total expected references: 	 98797 - 49.4 references per article
Total predicted references: 	 96830 - 48.41 references per article

Total expected citation contexts: 	 142862 - 71.43 citation contexts per article
Total predicted citation contexts: 	 130678 - 65.34 citation contexts per article

Total correct predicted citation contexts: 	 111487 - 55.74 citation contexts per article
Total wrong predicted citation contexts: 	 19191 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 85.31
Recall citation contexts: 	 78.04
fscore citation contexts: 	 81.51
```

Evaluation metrics produced in 323.955 seconds
