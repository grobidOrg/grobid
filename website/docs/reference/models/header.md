---
title: "Header Model"
description: "Header extraction labels and tags"
sidebar_position: 2
---

# Header Model

This model parses document metadata regions such as title, authors, affiliations, abstract, keywords, and related front-matter signals.

## What belongs to the header model

The header model handles bibliographic and article-front information, even when some of it appears outside the visually obvious title block.

Typical examples:

- title and subtitle
- author list
- affiliations and addresses
- abstracts
- keywords
- correspondence information
- submission and copyright statements
- funding and availability statements when they are part of front matter
- strong identifiers such as DOI or arXiv IDs

## Common TEI elements used in training

- `<titlePart>` for title parts
- `<docAuthor>` for author sequences
- `<affiliation>` and `<address>` for affiliation material
- `<div type="abstract">` for abstracts
- `<keyword>` for keywords and subject classifications
- `<reference>` for "how to cite" style references embedded in the article
- `<email>`, `<editor>`, `<idno>`, `<phone>`, `<page>` where relevant
- several `<note type="...">` variants for document type, submission, copyright, funding, availability, contribution, and conflict statements

## Important annotation habits

- exclude generic field labels like `Abstract`, `Authors`, or `Keywords` from the tagged value
- include author-affiliation markers inside the tagged author or affiliation field when they are part of the linking signal
- label repeated author or affiliation mentions when they are genuine metadata, not just email-parenthesis shorthand
- keep running titles untagged

## Practical edge cases

- English translated titles in non-English articles can be tagged separately as an English-title note
- journal titles, meeting info, and standalone publisher mentions may still belong in the header material depending on placement
- correspondence and copyright blocks at the end of the article can still conceptually belong to front-matter/header processing

## Related pages

- [Segmentation Model](./segmentation)
- [Affiliation Model](./affiliation)
- [Date Model](./date)
