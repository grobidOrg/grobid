---
title: "Date Model"
description: "Date parsing labels and tags"
sidebar_position: 6
---

# Date Model

This model normalizes date strings that were already identified elsewhere, typically in header or citation material.

## What it does

The date model does not decide whether something is a date. It takes a recognized date segment and structures it into normalized components.

## Training format

Unlike the TEI-heavy models, old date-model training uses a simpler XML shape based on:

- `<date>`
- `<day>`
- `<month>`
- `<year>`

## Annotation rule

Keep punctuation and surrounding glue outside the specific day/month/year tags.

Example shape:

```xml
<date>Received <month>August</month> <day>17</day>, <year>2005</year>.</date>
```

## Related pages

- [Header Model](./header)
- [Citation Model](./citation)
