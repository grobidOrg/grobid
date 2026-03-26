---
title: "Citation Model"
description: "Bibliographic reference labels and tags"
sidebar_position: 4
---

# Citation Model

This model parses one bibliographic reference at a time after the reference-segmenter has split a bibliography into individual entries.

## Main container

Each reference is wrapped in one `<bibl>` element, and one `<bibl>` should correspond to exactly one bibliographic reference.

## Common structures inside a reference

- `<author>` for the author sequence
- `<title level="a">` for article or chapter title
- `<title level="j">` for journal title
- `<title level="s">` for series title
- `<title level="m">` for monograph-like containers such as proceedings, books, theses, or websites
- `<date>` for date strings
- `<biblScope unit="page">`, `unit="volume"`, and `unit="issue"`
- `<publisher>` and `<orgName>` for institutional or publisher-style authorship contexts
- `<editor>`, `<pubPlace>`, `<ptr type="web">`, `<idno>`, and `<note type="report">`

## Important annotation habits

- leave punctuation and syntactic glue outside the tagged fields where possible
- keep identifier prefixes like `doi:` or `PMID:` inside `<idno>` when they are part of the visible reference string
- if two fields touch without spacing in the source, do not insert artificial separators in the annotation
- year-plus-letter forms such as `1996c` stay together inside `<date>`

## Special cases

- collaborations can use `<orgName type="collaboration">`
- thesis titles are treated as monograph-level titles, with thesis/report type captured separately
- publisher names can stand in for author-style positions when the source is an institution, company, or website

## Related pages

- [Date Model](./date)
- [TEI Output Format](../tei-output-format)
