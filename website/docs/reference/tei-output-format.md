---
title: "TEI Output Format"
description: "TEI XML schema and element reference"
sidebar_position: 3
---

# TEI Output Format

Use this page when you already understand the broad idea of GROBID output and need a deeper reference for the TEI structure and its validation model.

If you are still asking "why doesn't this look like the PDF?", start with [Understanding the Output](../getting-started/understanding-the-output) first.

## What GROBID's TEI is

GROBID uses a customized TEI XML model to represent structured document content.

Why TEI:

- the result is richer than a flat bibliographic record
- documents contain nested sections, references, notes, lists, and internal structure
- TEI is a mature text-encoding model for structured documents

This is why GROBID returns TEI XML for full document extraction instead of a simpler flat format.

## A customized TEI, not generic TEI-as-is

GROBID does not use raw unconstrained TEI. It uses a customized schema tuned for GROBID's output model.

Schema resources in the repository include:

- `grobid-home/schemas/odd/Grobid.odd`
- `grobid-home/schemas/rng/Grobid.rng`
- `grobid-home/schemas/rng/Grobid.rnc`
- `grobid-home/schemas/dtd/Grobid.dtd`
- `grobid-home/schemas/xsd`

If you need validation, prefer the RelaxNG schemas.

## Main high-level structure

A typical TEI result contains:

- `<teiHeader>` for metadata
- `<text>` for the document content
- `<body>` for the main text
- `<back>` for references and related back matter
- `<listBibl>` and `<biblStruct>` for structured references

This is the practical structure most downstream users work with.

## Typical elements you will encounter

| Element | Meaning |
| --- | --- |
| `<teiHeader>` | Document-level metadata |
| `<fileDesc>` | Bibliographic and descriptive metadata container |
| `<titleStmt>` | Title and responsibility information |
| `<sourceDesc>` | Description of the source document |
| `<text>` | Main content wrapper |
| `<body>` | Main article body |
| `<div>` | Logical content division |
| `<head>` | Section title |
| `<p>` | Paragraph |
| `<listBibl>` | List of references |
| `<biblStruct>` | Structured bibliographic record |
| `<persName>` | Person name |
| `<affiliation>` | Affiliation/address structure |
| `<note>` | Notes / footnotes |

This is not an exhaustive schema listing, but it covers the structures most users inspect first.

## TEI vs BibTeX vs simpler outputs

GROBID can emit BibTeX for a few narrower use cases, but TEI remains the richer and more structured output.

Use TEI when you care about:

- full document structure
- body extraction
- references with nested structure
- coordinates
- downstream XML/XPath processing

Use BibTeX only when you specifically need a simpler bibliography-oriented format.

## Well-formedness and validation

GROBID aims to produce well-formed XML consistently.

In practice:

- well-formedness should be the norm
- validation should succeed for the large majority of outputs
- occasional validation failure can also be a useful signal that the source PDF or predicted structure is unusually problematic

This matters because GROBID operates on noisy, unconstrained PDFs rather than on clean structured input.

## Coordinates in TEI

GROBID extends TEI output with coordinate support.

Two important places:

### `<facsimile>`

Page size information can be represented under `<facsimile>` with `<surface>` elements.

Example:

```xml
<facsimile>
  <surface n="1" ulx="0.0" uly="0.0" lrx="612.0" lry="794.0"/>
</facsimile>
```

### `@coords`

Coordinates for specific structures appear in a compact `coords` attribute.

Example:

```xml
<persName coords="1,53.80,194.57,58.71,9.29">
```

This means one bounding box with:

- page
- x
- y
- width
- height

Multiple boxes are separated by semicolons.

Example:

```xml
<biblStruct coords="10,317.03,183.61,223.16,7.55;10,317.03,192.57,223.21,7.55"/>
```

For a deeper coordinates explanation, use [Coordinates](./coordinates).

## Important interpretation rule

GROBID's TEI is a logical-document representation, not a page-faithful visual representation.

That means:

- layout is normalized
- whitespace is not preserved literally
- pagination is not the main organizing concept
- coordinates are the bridge back to the original PDF geometry when needed

## If you want to bind TEI into code

If you need generated bindings rather than manual XML parsing, the old docs note that JAXB-based workflows may require custom handling because TEI/XSD bindings can be awkward in practice.

For many users, simpler downstream approaches are more practical:

- XPath/XQuery
- standard XML libraries
- language-native XML parsers

## Recommended downstream strategy

Use this order:

1. inspect one TEI file manually
2. identify the elements you actually need
3. write extraction logic against those elements
4. only then decide whether you need full schema validation or generated bindings

This avoids overengineering early.

## Related pages

- [Understanding the Output](../getting-started/understanding-the-output)
- [API Endpoints](./api-endpoints)
- [Coordinates](./coordinates)
- [TEI Rationale](../explanation/tei-rationale)
