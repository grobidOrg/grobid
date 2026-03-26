---
title: "Understanding the Output"
description: "TEI XML structure explained for beginners"
sidebar_position: 5
---

# Understanding the Output

Use this page after your first successful extraction to understand what GROBID is actually giving you back.

GROBID does **not** try to reproduce the PDF visually. It extracts a structured, machine-readable representation of the document.

## The core idea

GROBID outputs TEI XML because the result is not just a flat metadata record. A scholarly article contains:

- nested sections
- references and citations
- author and affiliation structures
- figures, tables, formulas, and notes

That kind of structure fits TEI much better than a simple CSV-like or BibTeX-only representation.

## What GROBID preserves

GROBID aims to preserve:

- logical structure
- document metadata
- bibliographic structure
- section boundaries and titles
- references and their parsed fields

## What GROBID does not preserve exactly

GROBID does **not** try to preserve:

- the exact visual layout of the PDF
- original pagination as a primary output concept
- every typographic detail
- exact whitespace and line breaks from the original document

This is one of the most common sources of confusion. The output is a structured representation of the document, not a visual clone.

## The main TEI sections you will see

### `<teiHeader>`

Contains document-level metadata such as:

- title
- authors
- affiliations
- abstract
- publication metadata

### `<text>`

Contains the main extracted content.

Inside it, you will usually see:

- `<body>` for the main article body
- `<back>` for references and other back matter

### `<listBibl>` and `<biblStruct>`

These represent parsed bibliographic references.

- `<listBibl>` is the collection of references
- `<biblStruct>` is one structured reference record

This is usually more useful than raw strings when you need fields like title, journal, volume, pages, or DOI.

## A tiny mental model of the output

Think of the result like this:

- header metadata in `<teiHeader>`
- readable article structure in `<text>` and `<body>`
- parsed references in `<back>` and `<listBibl>`

If you want only one part of the result, you usually post-process the TEI instead of asking GROBID to emit an entirely different document model.

## Why page numbers or layout may seem missing

GROBID models logical structure, not page design.

So:

- page numbers are not the central organizing concept of the TEI output
- text layout is normalized
- some visual details are intentionally not represented directly

If you need position information, use coordinates.

## Coordinates: when structure needs to point back into the PDF

GROBID can return coordinates for selected structures.

Examples include:

- `persName`
- `figure`
- `biblStruct`
- `formula`
- `head`
- `p`
- `s`
- `title`
- `affiliation`

In TEI output, coordinates appear as a `coords` attribute.

Example:

```xml
<persName coords="1,53.80,194.57,58.71,9.29">
```

This is useful when you want to connect extracted structure back to the source PDF, such as in an annotation viewer.

For more detail, see [Coordinates](../reference/coordinates).

## Common misunderstandings

### “Why doesn’t the TEI look like the PDF?”

Because GROBID extracts logical structure, not visual appearance.

### “Why don’t page numbers appear the way I expect?”

Because pagination is a presentation concept. GROBID focuses on structure first.

### “Why does `processHeaderDocument` look different from `processFulltextDocument`?”

Because they solve different problems. Header extraction is narrower and faster. Fulltext extraction models the complete document structure.

### “Why is only one email linked to an author?”

That is a current design limitation in how author-email matching is resolved.

### “Can I get only the body text?”

Usually you post-process the TEI and extract `<body>` rather than expecting a separate simplified output mode.

## Output quality expectations

GROBID is strongest on scholarly-article-like PDFs.

Quality may degrade when:

- the PDF is image-only or badly OCRed
- the document is very long or non-article-like
- the layout is unusual
- the reference style is far from the training data

This is especially important for:

- footnote-style references
- books or very long documents
- unusual affiliation formats

## When enriched metadata changes the output

If consolidation is enabled, the result may contain metadata that was not explicitly present in the PDF.

This can improve quality, but it also means:

- DOIs may be added
- metadata may be corrected or enriched
- the final result may differ from pure PDF-only extraction

If you want strictly local extraction only, disable consolidation.

## What to do next

Depending on what you need next:

- [REST API Usage](../guides/api/rest-api-usage) if you want to make more requests correctly
- [API Endpoints](../reference/api-endpoints) if you want to look up parameters and endpoint behavior
- [Configuration Guide](../guides/configuration) if you want to change runtime behavior
- [TEI Rationale](../explanation/tei-rationale) if you want the deeper reasoning behind TEI as the document model
