---
title: "Your First Extraction"
description: "Process a PDF and understand the result"
sidebar_position: 4
---

# Your First Extraction

Use this page to turn a running GROBID service into one concrete, successful extraction result.

If the service is not running yet, go back to [Quick Start (Docker)](./quickstart-docker) first.

## Goal

By the end of this page, you will:

- send one PDF to GROBID
- get a TEI XML result back
- recognize the main parts of that result

## 1. Make sure the service is alive

Check these first:

- `http://localhost:8070`
- `http://localhost:8070/api/version`
- `http://localhost:8070/api/isalive`

If these do not work, go to [Docker Troubleshooting](../guides/docker/docker-troubleshooting) or [Troubleshooting](../guides/troubleshooting).

## 2. Pick one known-good PDF

For your first extraction:

- use a normal scholarly article PDF
- avoid scanned/image-only PDFs
- avoid very large or obviously damaged PDFs

You want to prove the workflow first, not stress the parser immediately.

## 3. Send the PDF to the fulltext endpoint

Run:

```bash
curl -v -H "Accept: application/xml" --form input=@./thefile.pdf localhost:8070/api/processFulltextDocument
```

Why this endpoint?

- it gives you the most complete document structure
- it is the best first success path if you want to understand what GROBID produces overall

## 4. Save the result to a file

Instead of printing the XML only to the terminal, save it:

```bash
curl -v -H "Accept: application/xml" --form input=@./thefile.pdf localhost:8070/api/processFulltextDocument -o result.tei.xml
```

Now open `result.tei.xml` in your editor.

## 5. What you should expect in the result

A successful TEI result usually contains these high-level parts:

- `<teiHeader>` for document metadata
- `<text>` for the structured main content
- `<body>` for the article body
- `<back>` for back matter such as references
- `<listBibl>` and `<biblStruct>` for parsed bibliographic references

Do not expect the output to preserve the original page layout exactly. GROBID extracts logical structure, not a visual clone of the PDF.

## 6. Try a simpler endpoint too

If you want a smaller, faster first result, compare with header extraction:

```bash
curl -v -H "Accept: application/xml" --form input=@./thefile.pdf localhost:8070/api/processHeaderDocument -o header.tei.xml
```

This is useful when you care mostly about title, authors, affiliations, abstract, and top-level metadata.

## 7. Optional: include raw citations

If you want to compare parsed references with the original raw reference strings:

```bash
curl -v -H "Accept: application/xml" --form input=@./thefile.pdf --form includeRawCitations=1 localhost:8070/api/processFulltextDocument -o result-with-raw-citations.tei.xml
```

This is useful when validating or debugging reference parsing quality.

## 8. If the result is empty or wrong

Common cases:

- `BAD_INPUT_DATA`: the PDF is unreadable, damaged, or otherwise unusable
- `NO_BLOCKS`: the PDF likely has no extractable text and may need OCR first
- `204`: the request completed, but no structured content was extracted
- `503`: the service is saturated; wait and retry

Go to:

- [REST API Usage](../guides/api/rest-api-usage)
- [Troubleshooting](../guides/troubleshooting)

## 9. What to do next

Now that you have one successful result, the most useful next pages are:

- [Understanding the Output](./understanding-the-output)
- [REST API Usage](../guides/api/rest-api-usage)
- [API Endpoints](../reference/api-endpoints)
- [Configuration Guide](../guides/configuration)

That sequence usually takes you from first success into real usage without a big context jump.
