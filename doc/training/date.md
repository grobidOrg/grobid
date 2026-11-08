# Annotation guidelines for dates

## Introduction

Date segments can be recognized by the `header` model (for example the _date of publication_, or the _online date_) or by the `citation` model. These date segments are further structured by the `date` model which will try to normalize the date into the standard ISO 8601 format. 

For convenience, the structuring of the dates (training files `*.date.tei.xml`) does (exceptionally) not follow the TEI but a basic XML format based on `<day>`, `<month>` and `<year>` elements. 

## Analysis

Each date is enclosed in a `<date>` element. Day, month and year are identified respectively with `<day>`, `<month>` and `<year>` elements. Additional text/characters that do not belong to one of these specific elements (punctuations, etc.) must be left untagged under the `<date>` elements.

For example:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<dates>
	<date>Received <month>August</month> <day>17</day>, <year>2005</year>. </date>
</dates>
```

### Specific cases

- **Ordinal suffixes** stay inside the day element, with or without a space: `<day>12th</day>`, `<day>5 th</day>`.
- **Abbreviated months**: the abbreviation period stays outside the month element: `<month>Dec</month>.`, `<month>Sept</month>.`.
- **Day ranges**: each day is tagged separately and the dash stays untagged: `<day>10</day>–<day>14</day> <month>March</month> <year>2011</year>`.
- **Numeric dates**: when the order cannot be decided from the values (e.g. `07/04/2002`), use the source document's convention when known, otherwise read them day-first (`<day>07</day>/<month>04</month>/<year>2002</year>`). If one value is above 12 it is the day: `<month>09</month>/<day>16</day>/<year>2009</year>`.
- **Separators** are never part of a field: `<month>06</month>/<year>2010</year>`, `(<year>1994</year>)`.
- **Things that are not dates** stay untagged, even when they contain numbers that look like dates: issue numbers (`Issue 1a`), times (`13:26:14`), DOIs (`ajkd.2009.10.047`).
- **OCR artifacts** are tagged as they appear: a spaced year such as `<year>1 994</year>` is still one year.
