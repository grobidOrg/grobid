
## Header metadata 

Evaluation on 983 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| abstract | 9.44 | 9.16 | 9.3 | 983 |
| authors | 74.28 | 73.52 | 73.9 | 982 |
| first_author | 92.39 | 91.54 | 91.96 | 981 |
| title | 86.81 | 85.05 | 85.92 | 983 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **65.96** | **64.8** | **65.37** | 3929 |
| all fields (macro avg.) | 65.73 | 64.82 | 65.27 | 3929 |



#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| abstract | 22.46 | 21.77 | 22.11 | 983 |
| authors | 74.59 | 73.83 | 74.21 | 982 |
| first_author | 92.39 | 91.54 | 91.96 | 981 |
| title | 94.81 | 92.88 | 93.83 | 983 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **71.24** | **69.99** | **70.61** | 3929 |
| all fields (macro avg.) | 71.06 | 70 | 70.53 | 3929 |



#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| abstract | 47.53 | 46.08 | 46.8 | 983 |
| authors | 88.17 | 87.27 | 87.72 | 982 |
| first_author | 92.7 | 91.85 | 92.27 | 981 |
| title | 96.26 | 94.3 | 95.27 | 983 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **81.3** | **79.87** | **80.58** | 3929 |
| all fields (macro avg.) | 81.16 | 79.88 | 80.51 | 3929 |



#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| abstract | 44.49 | 43.13 | 43.8 | 983 |
| authors | 79.94 | 79.12 | 79.53 | 982 |
| first_author | 92.39 | 91.54 | 91.96 | 981 |
| title | 96.26 | 94.3 | 95.27 | 983 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **78.39** | **77.02** | **77.7** | 3929 |
| all fields (macro avg.) | 78.27 | 77.02 | 77.64 | 3929 |


#### Instance-level results

```
Total expected instances: 	983
Total correct instances: 	73 (strict) 
Total correct instances: 	198 (soft) 
Total correct instances: 	377 (Levenshtein) 
Total correct instances: 	335 (ObservedRatcliffObershelp) 

Instance-level recall:	7.43	(strict) 
Instance-level recall:	20.14	(soft) 
Instance-level recall:	38.35	(Levenshtein) 
Instance-level recall:	34.08	(RatcliffObershelp) 
```


## Citation metadata 

Evaluation on 983 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| authors | 79.4 | 78.19 | 78.79 | 63170 |
| date | 95.86 | 93.99 | 94.91 | 63567 |
| first_author | 94.76 | 93.28 | 94.02 | 63170 |
| inTitle | 95.77 | 94.68 | 95.22 | 63118 |
| issue | 1.99 | 75 | 3.88 | 16 |
| page | 96.26 | 95.2 | 95.72 | 53303 |
| title | 90.25 | 90.68 | 90.47 | 61950 |
| volume | 97.85 | 98.17 | 98.01 | 60955 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **92.66** | **91.93** | **92.29** | 429249 |
| all fields (macro avg.) | 81.52 | 89.9 | 81.38 | 429249 |



#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| authors | 79.54 | 78.33 | 78.93 | 63170 |
| date | 95.86 | 93.99 | 94.91 | 63567 |
| first_author | 94.84 | 93.36 | 94.1 | 63170 |
| inTitle | 96.25 | 95.15 | 95.7 | 63118 |
| issue | 1.99 | 75 | 3.88 | 16 |
| page | 96.26 | 95.2 | 95.72 | 53303 |
| title | 95.92 | 96.38 | 96.15 | 61950 |
| volume | 97.85 | 98.17 | 98.01 | 60955 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **93.59** | **92.85** | **93.22** | 429249 |
| all fields (macro avg.) | 82.31 | 90.7 | 82.17 | 429249 |



#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| authors | 93.29 | 91.87 | 92.58 | 63170 |
| date | 95.86 | 93.99 | 94.91 | 63567 |
| first_author | 95.29 | 93.8 | 94.54 | 63170 |
| inTitle | 96.58 | 95.47 | 96.02 | 63118 |
| issue | 1.99 | 75 | 3.88 | 16 |
| page | 96.26 | 95.2 | 95.72 | 53303 |
| title | 97.66 | 98.12 | 97.89 | 61950 |
| volume | 97.85 | 98.17 | 98.01 | 60955 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **95.97** | **95.21** | **95.59** | 429249 |
| all fields (macro avg.) | 84.35 | 92.7 | 84.19 | 429249 |



#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| authors | 86.71 | 85.39 | 86.05 | 63170 |
| date | 95.86 | 93.99 | 94.91 | 63567 |
| first_author | 94.78 | 93.3 | 94.03 | 63170 |
| inTitle | 96.25 | 95.16 | 95.7 | 63118 |
| issue | 1.99 | 75 | 3.88 | 16 |
| page | 96.26 | 95.2 | 95.72 | 53303 |
| title | 97.5 | 97.97 | 97.74 | 61950 |
| volume | 97.85 | 98.17 | 98.01 | 60955 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **94.87** | **94.11** | **94.49** | 429249 |
| all fields (macro avg.) | 83.4 | 91.77 | 83.26 | 429249 |


#### Instance-level results

```
Total expected instances: 		63569
Total extracted instances: 		66388
Total correct instances: 		42246 (strict) 
Total correct instances: 		45085 (soft) 
Total correct instances: 		52715 (Levenshtein) 
Total correct instances: 		49331 (RatcliffObershelp) 

Instance-level precision:	63.63 (strict) 
Instance-level precision:	67.91 (soft) 
Instance-level precision:	79.4 (Levenshtein) 
Instance-level precision:	74.31 (RatcliffObershelp) 

Instance-level recall:	66.46	(strict) 
Instance-level recall:	70.92	(soft) 
Instance-level recall:	82.93	(Levenshtein) 
Instance-level recall:	77.6	(RatcliffObershelp) 

Instance-level f-score:	65.02 (strict) 
Instance-level f-score:	69.38 (soft) 
Instance-level f-score:	81.13 (Levenshtein) 
Instance-level f-score:	75.92 (RatcliffObershelp) 

Matching 1 :	58505

Matching 2 :	1012

Matching 3 :	1242

Matching 4 :	371

Total matches :	61130
```


#### Citation context resolution
```

Total expected references: 	 63569 - 64.67 references per article
Total predicted references: 	 66388 - 67.54 references per article

Total expected citation contexts: 	 108880 - 110.76 citation contexts per article
Total predicted citation contexts: 	 99284 - 101 citation contexts per article

Total correct predicted citation contexts: 	 95494 - 97.15 citation contexts per article
Total wrong predicted citation contexts: 	 3790 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 96.18
Recall citation contexts: 	 87.71
fscore citation contexts: 	 91.75
```


## Fulltext structures 

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from the actual PDF content and are can be inconsistent from one document to another. The scores of the following metrics are thus not very meaningful in absolute term, in particular for the strict matching (textual content of the srtructure can be very long). As relative values for comparing different models, they seem however useful.


Evaluation on 983 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| availability_stmt | 29.94 | 26.71 | 28.24 | 584 |
| figure_title | 0.02 | 0.01 | 0.01 | 31671 |
| funding_stmt | 4.77 | 23.8 | 7.95 | 920 |
| reference_citation | 55.46 | 55.67 | 55.56 | 108807 |
| reference_figure | 56.78 | 49.91 | 53.12 | 68786 |
| reference_table | 68.24 | 73.46 | 70.75 | 2381 |
| section_title | 85.17 | 74.17 | 79.29 | 21808 |
| table_title | 0.45 | 0.16 | 0.23 | 1924 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **54.74** | **47.79** | **51.03** | 236881 |
| all fields (macro avg.) | 37.6 | 37.99 | 36.89 | 236881 |



#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| availability_stmt | 38.96 | 34.76 | 36.74 | 584 |
| figure_title | 48.86 | 15.12 | 23.09 | 31671 |
| funding_stmt | 4.77 | 23.8 | 7.95 | 920 |
| reference_citation | 91.04 | 91.38 | 91.21 | 108807 |
| reference_figure | 57.06 | 50.16 | 53.39 | 68786 |
| reference_table | 68.32 | 73.54 | 70.83 | 2381 |
| section_title | 86.05 | 74.93 | 80.1 | 21808 |
| table_title | 80.63 | 27.91 | 41.47 | 1924 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **76.29** | **66.6** | **71.12** | 236881 |
| all fields (macro avg.) | 59.46 | 48.95 | 50.6 | 236881 |


**Document-level ratio results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| availability_stmt | 96.3 | 89.21 | 92.62 | 584 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **96.3** | **89.21** | **92.62** | 584 |
| all fields (macro avg.) | 96.3 | 89.21 | 92.62 | 584 |

Evaluation metrics produced in 640.707 seconds
