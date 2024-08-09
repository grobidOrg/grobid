
## Header metadata 

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| abstract | 16.78 | 16.48 | 16.63 | 1911 |
| authors | 92.01 | 91.91 | 91.96 | 1941 |
| first_author | 96.7 | 96.6 | 96.65 | 1941 |
| keywords | 64.99 | 63.62 | 64.3 | 1380 |
| title | 84.67 | 84.41 | 84.54 | 1943 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **71.79** | **71.22** | **71.5** | 9116 |
| all fields (macro avg.) | 71.03 | 70.6 | 70.81 | 9116 |



#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| abstract | 63.83 | 62.69 | 63.25 | 1911 |
| authors | 93.91 | 93.82 | 93.87 | 1941 |
| first_author | 97.06 | 96.96 | 97.01 | 1941 |
| keywords | 73.72 | 72.17 | 72.94 | 1380 |
| title | 92.15 | 91.87 | 92.01 | 1943 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **84.95** | **84.27** | **84.61** | 9116 |
| all fields (macro avg.) | 84.14 | 83.5 | 83.82 | 9116 |



#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| abstract | 91.05 | 89.43 | 90.23 | 1911 |
| authors | 96.08 | 95.98 | 96.03 | 1941 |
| first_author | 97.32 | 97.22 | 97.27 | 1941 |
| keywords | 84.16 | 82.39 | 83.27 | 1380 |
| title | 98.35 | 98.04 | 98.2 | 1943 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **94.01** | **93.25** | **93.63** | 9116 |
| all fields (macro avg.) | 93.39 | 92.61 | 93 | 9116 |



#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| abstract | 87.11 | 85.56 | 86.33 | 1911 |
| authors | 94.95 | 94.85 | 94.9 | 1941 |
| first_author | 96.7 | 96.6 | 96.65 | 1941 |
| keywords | 79.5 | 77.83 | 78.65 | 1380 |
| title | 96.33 | 96.04 | 96.19 | 1943 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **91.68** | **90.95** | **91.32** | 9116 |
| all fields (macro avg.) | 90.92 | 90.17 | 90.54 | 9116 |


#### Instance-level results

```
Total expected instances: 	1943
Total correct instances: 	219 (strict) 
Total correct instances: 	904 (soft) 
Total correct instances: 	1434 (Levenshtein) 
Total correct instances: 	1294 (ObservedRatcliffObershelp) 

Instance-level recall:	11.27	(strict) 
Instance-level recall:	46.53	(soft) 
Instance-level recall:	73.8	(Levenshtein) 
Instance-level recall:	66.6	(RatcliffObershelp) 
```


## Citation metadata 

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| authors | 83.03 | 76.31 | 79.53 | 85778 |
| date | 94.6 | 84.25 | 89.13 | 87067 |
| first_author | 89.78 | 82.49 | 85.98 | 85778 |
| inTitle | 73.23 | 71.88 | 72.55 | 81007 |
| issue | 91.09 | 87.74 | 89.38 | 16635 |
| page | 94.57 | 83.7 | 88.81 | 80501 |
| title | 79.67 | 75.3 | 77.42 | 80736 |
| volume | 96.01 | 89.82 | 92.81 | 80067 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **87.22** | **80.74** | **83.86** | 597569 |
| all fields (macro avg.) | 87.75 | 81.44 | 84.45 | 597569 |



#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| authors | 83.5 | 76.75 | 79.98 | 85778 |
| date | 94.6 | 84.25 | 89.13 | 87067 |
| first_author | 89.95 | 82.65 | 86.14 | 85778 |
| inTitle | 84.92 | 83.36 | 84.13 | 81007 |
| issue | 91.09 | 87.74 | 89.38 | 16635 |
| page | 94.57 | 83.7 | 88.81 | 80501 |
| title | 91.43 | 86.42 | 88.86 | 80736 |
| volume | 96.01 | 89.82 | 92.81 | 80067 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **90.61** | **83.89** | **87.12** | 597569 |
| all fields (macro avg.) | 90.76 | 84.34 | 87.41 | 597569 |



#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| authors | 89.21 | 81.99 | 85.45 | 85778 |
| date | 94.6 | 84.25 | 89.13 | 87067 |
| first_author | 90.15 | 82.84 | 86.34 | 85778 |
| inTitle | 86.18 | 84.59 | 85.38 | 81007 |
| issue | 91.09 | 87.74 | 89.38 | 16635 |
| page | 94.57 | 83.7 | 88.81 | 80501 |
| title | 93.8 | 88.66 | 91.15 | 80736 |
| volume | 96.01 | 89.82 | 92.81 | 80067 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **91.96** | **85.14** | **88.42** | 597569 |
| all fields (macro avg.) | 91.95 | 85.45 | 88.56 | 597569 |



#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| authors | 85.98 | 79.02 | 82.35 | 85778 |
| date | 94.6 | 84.25 | 89.13 | 87067 |
| first_author | 89.8 | 82.51 | 86 | 85778 |
| inTitle | 83.49 | 81.95 | 82.72 | 81007 |
| issue | 91.09 | 87.74 | 89.38 | 16635 |
| page | 94.57 | 83.7 | 88.81 | 80501 |
| title | 93.39 | 88.27 | 90.76 | 80736 |
| volume | 96.01 | 89.82 | 92.81 | 80067 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **91.01** | **84.25** | **87.5** | 597569 |
| all fields (macro avg.) | 91.12 | 84.66 | 87.74 | 597569 |


#### Instance-level results

```
Total expected instances: 		90125
Total extracted instances: 		85902
Total correct instances: 		38762 (strict) 
Total correct instances: 		50900 (soft) 
Total correct instances: 		55783 (Levenshtein) 
Total correct instances: 		52319 (RatcliffObershelp) 

Instance-level precision:	45.12 (strict) 
Instance-level precision:	59.25 (soft) 
Instance-level precision:	64.94 (Levenshtein) 
Instance-level precision:	60.91 (RatcliffObershelp) 

Instance-level recall:	43.01	(strict) 
Instance-level recall:	56.48	(soft) 
Instance-level recall:	61.9	(Levenshtein) 
Instance-level recall:	58.05	(RatcliffObershelp) 

Instance-level f-score:	44.04 (strict) 
Instance-level f-score:	57.83 (soft) 
Instance-level f-score:	63.38 (Levenshtein) 
Instance-level f-score:	59.44 (RatcliffObershelp) 

Matching 1 :	68328

Matching 2 :	4154

Matching 3 :	1863

Matching 4 :	662

Total matches :	75007
```


#### Citation context resolution
```

Total expected references: 	 90125 - 46.38 references per article
Total predicted references: 	 85902 - 44.21 references per article

Total expected citation contexts: 	 139835 - 71.97 citation contexts per article
Total predicted citation contexts: 	 115373 - 59.38 citation contexts per article

Total correct predicted citation contexts: 	 97277 - 50.07 citation contexts per article
Total wrong predicted citation contexts: 	 18096 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 84.32
Recall citation contexts: 	 69.57
fscore citation contexts: 	 76.23
```


## Fulltext structures 

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from the actual PDF content and are can be inconsistent from one document to another. The scores of the following metrics are thus not very meaningful in absolute term, in particular for the strict matching (textual content of the srtructure can be very long). As relative values for comparing different models, they seem however useful.


Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| figure_title | 31.44 | 24.61 | 27.61 | 7281 |
| reference_citation | 57.43 | 58.68 | 58.05 | 134196 |
| reference_figure | 61.21 | 65.9 | 63.47 | 19330 |
| reference_table | 83.01 | 88.39 | 85.62 | 7327 |
| section_title | 76.39 | 67.77 | 71.82 | 27619 |
| table_title | 57.3 | 50.29 | 53.57 | 3971 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **60.41** | **60.32** | **60.36** | 199724 |
| all fields (macro avg.) | 61.13 | 59.27 | 60.02 | 199724 |



#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| figure_title | 78.68 | 61.58 | 69.09 | 7281 |
| reference_citation | 61.68 | 63.03 | 62.35 | 134196 |
| reference_figure | 61.69 | 66.41 | 63.97 | 19330 |
| reference_table | 83.19 | 88.58 | 85.8 | 7327 |
| section_title | 81.25 | 72.08 | 76.39 | 27619 |
| table_title | 81.89 | 71.87 | 76.56 | 3971 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **65.77** | **65.67** | **65.72** | 199724 |
| all fields (macro avg.) | 74.73 | 70.59 | 72.36 | 199724 |


**Document-level ratio results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **0** | **0** | **0** | 0 |
| all fields (macro avg.) | 0 | 0 | 0 | 0 |

Evaluation metrics produced in 625.694 seconds
