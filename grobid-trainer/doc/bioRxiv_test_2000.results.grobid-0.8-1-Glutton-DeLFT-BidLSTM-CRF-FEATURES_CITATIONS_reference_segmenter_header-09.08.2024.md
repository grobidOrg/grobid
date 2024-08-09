
## Header metadata 

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| abstract | 2.2 | 2.16 | 2.18 | 1990 |
| authors | 83.2 | 82.49 | 82.84 | 1999 |
| first_author | 97.02 | 96.29 | 96.66 | 1997 |
| keywords | 58.71 | 59.83 | 59.27 | 839 |
| title | 77.67 | 76.85 | 77.26 | 2000 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **64.62** | **64.07** | **64.35** | 8825 |
| all fields (macro avg.) | 63.76 | 63.53 | 63.64 | 8825 |



#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| abstract | 59.71 | 58.54 | 59.12 | 1990 |
| authors | 83.7 | 82.99 | 83.35 | 1999 |
| first_author | 97.23 | 96.49 | 96.86 | 1997 |
| keywords | 63.86 | 65.08 | 64.46 | 839 |
| title | 79.89 | 79.05 | 79.47 | 2000 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **78.61** | **77.94** | **78.27** | 8825 |
| all fields (macro avg.) | 76.88 | 76.43 | 76.65 | 8825 |



#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| abstract | 80.22 | 78.64 | 79.42 | 1990 |
| authors | 92.18 | 91.4 | 91.79 | 1999 |
| first_author | 97.48 | 96.75 | 97.11 | 1997 |
| keywords | 79.42 | 80.93 | 80.17 | 839 |
| title | 92.02 | 91.05 | 91.53 | 2000 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **89.43** | **88.66** | **89.04** | 8825 |
| all fields (macro avg.) | 88.26 | 87.75 | 88 | 8825 |



#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| abstract | 76.88 | 75.38 | 76.12 | 1990 |
| authors | 87.79 | 87.04 | 87.42 | 1999 |
| first_author | 97.02 | 96.29 | 96.66 | 1997 |
| keywords | 71.35 | 72.71 | 72.02 | 839 |
| title | 87.87 | 86.95 | 87.41 | 2000 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **85.86** | **85.12** | **85.49** | 8825 |
| all fields (macro avg.) | 84.18 | 83.67 | 83.92 | 8825 |


#### Instance-level results

```
Total expected instances: 	2000
Total correct instances: 	35 (strict) 
Total correct instances: 	708 (soft) 
Total correct instances: 	1222 (Levenshtein) 
Total correct instances: 	1046 (ObservedRatcliffObershelp) 

Instance-level recall:	1.75	(strict) 
Instance-level recall:	35.4	(soft) 
Instance-level recall:	61.1	(Levenshtein) 
Instance-level recall:	52.3	(RatcliffObershelp) 
```


## Citation metadata 

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| authors | 88.16 | 83.24 | 85.63 | 97183 |
| date | 91.69 | 86.31 | 88.92 | 97630 |
| doi | 70.84 | 83.79 | 76.78 | 16894 |
| first_author | 95.06 | 89.68 | 92.29 | 97183 |
| inTitle | 82.83 | 79.4 | 81.08 | 96430 |
| issue | 94.34 | 92.04 | 93.18 | 30312 |
| page | 94.97 | 78.34 | 85.86 | 88597 |
| pmcid | 66.38 | 86.12 | 74.97 | 807 |
| pmid | 70.08 | 84.95 | 76.8 | 2093 |
| title | 84.88 | 83.58 | 84.23 | 92463 |
| volume | 96.23 | 95.23 | 95.73 | 87709 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **89.85** | **85.34** | **87.54** | 707301 |
| all fields (macro avg.) | 85.04 | 85.7 | 85.04 | 707301 |



#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| authors | 89.31 | 84.33 | 86.75 | 97183 |
| date | 91.69 | 86.31 | 88.92 | 97630 |
| doi | 75.34 | 89.11 | 81.65 | 16894 |
| first_author | 95.48 | 90.08 | 92.7 | 97183 |
| inTitle | 92.32 | 88.51 | 90.38 | 96430 |
| issue | 94.34 | 92.04 | 93.18 | 30312 |
| page | 94.97 | 78.34 | 85.86 | 88597 |
| pmcid | 75.64 | 98.14 | 85.44 | 807 |
| pmid | 74.5 | 90.3 | 81.64 | 2093 |
| title | 93.23 | 91.8 | 92.51 | 92463 |
| volume | 96.23 | 95.23 | 95.73 | 87709 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **92.66** | **88.02** | **90.28** | 707301 |
| all fields (macro avg.) | 88.46 | 89.47 | 88.61 | 707301 |



#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| authors | 94.58 | 89.3 | 91.87 | 97183 |
| date | 91.69 | 86.31 | 88.92 | 97630 |
| doi | 77.6 | 91.79 | 84.1 | 16894 |
| first_author | 95.63 | 90.22 | 92.85 | 97183 |
| inTitle | 93.3 | 89.45 | 91.33 | 96430 |
| issue | 94.34 | 92.04 | 93.18 | 30312 |
| page | 94.97 | 78.34 | 85.86 | 88597 |
| pmcid | 75.64 | 98.14 | 85.44 | 807 |
| pmid | 74.5 | 90.3 | 81.64 | 2093 |
| title | 96.05 | 94.58 | 95.31 | 92463 |
| volume | 96.23 | 95.23 | 95.73 | 87709 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **93.99** | **89.28** | **91.57** | 707301 |
| all fields (macro avg.) | 89.51 | 90.52 | 89.66 | 707301 |



#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| authors | 91.54 | 86.43 | 88.91 | 97183 |
| date | 91.69 | 86.31 | 88.92 | 97630 |
| doi | 76.04 | 89.94 | 82.41 | 16894 |
| first_author | 95.1 | 89.72 | 92.33 | 97183 |
| inTitle | 91.06 | 87.29 | 89.13 | 96430 |
| issue | 94.34 | 92.04 | 93.18 | 30312 |
| page | 94.97 | 78.34 | 85.86 | 88597 |
| pmcid | 66.38 | 86.12 | 74.97 | 807 |
| pmid | 70.08 | 84.95 | 76.8 | 2093 |
| title | 95.35 | 93.89 | 94.62 | 92463 |
| volume | 96.23 | 95.23 | 95.73 | 87709 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **93.02** | **88.36** | **90.63** | 707301 |
| all fields (macro avg.) | 87.53 | 88.21 | 87.53 | 707301 |


#### Instance-level results

```
Total expected instances: 		98799
Total extracted instances: 		98068
Total correct instances: 		43771 (strict) 
Total correct instances: 		54778 (soft) 
Total correct instances: 		58972 (Levenshtein) 
Total correct instances: 		55693 (RatcliffObershelp) 

Instance-level precision:	44.63 (strict) 
Instance-level precision:	55.86 (soft) 
Instance-level precision:	60.13 (Levenshtein) 
Instance-level precision:	56.79 (RatcliffObershelp) 

Instance-level recall:	44.3	(strict) 
Instance-level recall:	55.44	(soft) 
Instance-level recall:	59.69	(Levenshtein) 
Instance-level recall:	56.37	(RatcliffObershelp) 

Instance-level f-score:	44.47 (strict) 
Instance-level f-score:	55.65 (soft) 
Instance-level f-score:	59.91 (Levenshtein) 
Instance-level f-score:	56.58 (RatcliffObershelp) 

Matching 1 :	79296

Matching 2 :	4442

Matching 3 :	4371

Matching 4 :	2084

Total matches :	90193
```


#### Citation context resolution
```

Total expected references: 	 98797 - 49.4 references per article
Total predicted references: 	 98068 - 49.03 references per article

Total expected citation contexts: 	 142862 - 71.43 citation contexts per article
Total predicted citation contexts: 	 135692 - 67.85 citation contexts per article

Total correct predicted citation contexts: 	 116736 - 58.37 citation contexts per article
Total wrong predicted citation contexts: 	 18956 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 86.03
Recall citation contexts: 	 81.71
fscore citation contexts: 	 83.82
```


## Fulltext structures 

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from the actual PDF content and are can be inconsistent from one document to another. The scores of the following metrics are thus not very meaningful in absolute term, in particular for the strict matching (textual content of the srtructure can be very long). As relative values for comparing different models, they seem however useful.


Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| availability_stmt | 29.95 | 25.78 | 27.71 | 446 |
| figure_title | 4.23 | 2.01 | 2.72 | 22978 |
| funding_stmt | 4.16 | 24.43 | 7.11 | 745 |
| reference_citation | 71.05 | 71.33 | 71.19 | 147470 |
| reference_figure | 70.59 | 67.74 | 69.14 | 47984 |
| reference_table | 48.11 | 83.03 | 60.92 | 5957 |
| section_title | 72.59 | 69.6 | 71.06 | 32398 |
| table_title | 4.31 | 2.85 | 3.43 | 3925 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **65.46** | **63.41** | **64.42** | 261903 |
| all fields (macro avg.) | 38.12 | 43.35 | 39.16 | 261903 |



#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| availability_stmt | 50.52 | 43.5 | 46.75 | 446 |
| figure_title | 69.47 | 32.91 | 44.67 | 22978 |
| funding_stmt | 4.37 | 25.64 | 7.46 | 745 |
| reference_citation | 83.04 | 83.37 | 83.21 | 147470 |
| reference_figure | 71.22 | 68.34 | 69.75 | 47984 |
| reference_table | 48.56 | 83.8 | 61.49 | 5957 |
| section_title | 76.47 | 73.32 | 74.86 | 32398 |
| table_title | 51.44 | 34.06 | 40.99 | 3925 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **76.38** | **73.99** | **75.17** | 261903 |
| all fields (macro avg.) | 56.89 | 55.62 | 53.65 | 261903 |


**Document-level ratio results**

| label            |  precision |   recall  |     f1     | support |
|---               |---         |---        |---         |---      |
| availability_stmt | 84.77 | 86.1 | 85.43 | 446 |
|                  |            |           |            |         |
| **all fields (micro avg.)** | **84.77** | **86.1** | **85.43** | 446 |
| all fields (macro avg.) | 84.77 | 86.1 | 85.43 | 446 |

Evaluation metrics produced in 773.926 seconds
