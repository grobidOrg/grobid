---
title: "Coordinates"
description: "PDF coordinate system and bounding boxes"
sidebar_position: 4
---

# Coordinates

Use this page when you need to map GROBID output back onto the original PDF.

Coordinates are most useful for:

- annotation overlays
- PDF viewers
- linking extracted structure back to page positions
- downstream tools that need geometry, not just text

## What coordinates are available for

GROBID can return coordinates for selected structures, including:

- `ref`
- `biblStruct`
- `persName`
- `figure`
- `formula`
- `head`
- `s`
- `p`
- `note`
- `title`
- `affiliation`

Some are available directly in TEI output, and some are also available in JSON-oriented annotation endpoints.

## How to request coordinates

Use the `teiCoordinates` parameter on fulltext requests.

Example:

```bash
curl -v -H "Accept: application/xml" --form input=@./thefile.pdf --form teiCoordinates=figure --form teiCoordinates=biblStruct localhost:8070/api/processFulltextDocument
```

You can request multiple coordinate-bearing element types in the same call.

## The coordinate system GROBID uses

Important properties of the PDF coordinate system used by GROBID:

- origin is at the upper-left corner
- x grows to the right
- y grows downward
- values are stored in PDF units, not pixels
- page numbering starts at `1`, not `0`

This is one of the most important things to remember when integrating with browser renderers or other PDF tooling.

## Coordinates in TEI output

### `@coords`

In TEI, coordinates appear as a compact `coords` attribute.

Example:

```xml
<persName coords="1,53.80,194.57,58.71,9.29">
```

This means one bounding box with:

- page = `1`
- x = `53.80`
- y = `194.57`
- width = `58.71`
- height = `9.29`

### Multiple boxes

If a structure spans several lines, the `coords` attribute can contain multiple bounding boxes separated by semicolons.

Example:

```xml
<biblStruct coords="10,317.03,183.61,223.16,7.55;10,317.03,192.57,223.21,7.55"/>
```

This is common for references or other multi-line structures.

### `<facsimile>` and page sizes

TEI output may also contain page geometry under `<facsimile>`.

Example:

```xml
<facsimile>
  <surface n="1" ulx="0.0" uly="0.0" lrx="612.0" lry="794.0"/>
</facsimile>
```

This gives you the original page size information needed for scaling coordinates correctly.

## Coordinates in JSON output

Some annotation-oriented services return JSON with:

- page dimensions under `pages`
- bounding boxes under `pos`

Example page metadata:

```json
"pages": [
  {"page_height": 792.0, "page_width": 612.0}
]
```

Example bounding boxes:

```json
"pos": [
  {"p": 1, "x": 20, "y": 20, "h": 10, "w": 30},
  {"p": 1, "x": 30, "y": 20, "h": 10, "w": 30}
]
```

This JSON form is often easier to use directly in browser-based PDF viewers.

## How to align coordinates with other tools

If you compare GROBID coordinates with tools like pdfplumber or PyMuPDF, remember:

- tools may use different origins
- tools may use different scaling assumptions
- browser renderers often use pixels, while GROBID uses PDF units

The safest alignment strategy is:

1. read page dimensions from GROBID output
2. read page dimensions from the target rendering/tool
3. scale coordinates explicitly
4. verify page origin assumptions before trusting the overlay

## Common integration gotchas

### CropBox vs MediaBox mismatch

If coordinates seem offset, your PDF may have different CropBox and MediaBox values.

Practical workaround:

- normalize the PDF so CropBox and MediaBox match before processing

### Coordinates are not pixel positions

Do not treat GROBID coordinates as ready-to-draw browser pixel positions without scaling them to the rendered page size.

### Page number is inside the bounding box tuple

If you need the page number for a structure, read the first value in each TEI `coords` box or JSON `pos` box.

## When coordinates are the right tool

Use coordinates when you want:

- clickable references on top of the original PDF
- visual highlighting of extracted entities
- synchronized PDF + structured metadata interfaces

Do not use them if all you need is logical structure or plain extraction output.

## Related pages

- [Understanding the Output](../getting-started/understanding-the-output)
- [TEI Output Format](./tei-output-format)
- [API Endpoints](./api-endpoints)
