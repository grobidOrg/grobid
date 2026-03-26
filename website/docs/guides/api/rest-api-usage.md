---
title: "REST API Usage"
description: "Process PDFs using the GROBID REST API"
sidebar_position: 1
---

# REST API Usage

Use this page when your GROBID service is already running and you want the fastest path to a correct request.

If you have not started the service yet, use [Quick Start (Docker)](../../getting-started/quickstart-docker) or the [Docker Builder](../docker/docker-setup) first.

## 1. Verify that the service is alive

Before sending PDFs, confirm that the service actually started.

Useful checks:

- `http://localhost:8070`
- `http://localhost:8070/api/version`
- `http://localhost:8070/api/isalive`
- `http://localhost:8070/api/health`

If you exposed the admin port, you can also inspect `http://localhost:8071`.

## 2. Make your first successful request

Start with the simplest fulltext request:

```bash
curl -v -H "Accept: application/xml" --form input=@./thefile.pdf localhost:8070/api/processFulltextDocument
```

Important:

- always send an `Accept` header explicitly
- use `multipart/form-data` for PDF upload endpoints
- begin with one known-good PDF before testing harder files

## 3. Choose the right endpoint

Use the smallest endpoint that matches your goal.

| Goal | Endpoint | Notes |
| --- | --- | --- |
| Extract only header metadata | `/api/processHeaderDocument` | Faster, simpler, usually first ~2 pages |
| Extract full structured article content | `/api/processFulltextDocument` | Header, body, citations, structure |
| Extract references only | `/api/processReferences` | Good when you only need bibliographical references |

### Header only

```bash
curl -v -H "Accept: application/xml" --form input=@./thefile.pdf localhost:8070/api/processHeaderDocument
```

Use this when you want metadata quickly and do not need full text.

### Full text

```bash
curl -v -H "Accept: application/xml" --form input=@./thefile.pdf localhost:8070/api/processFulltextDocument
```

Use this when you need the main document structure, sectioning, references, or TEI output for downstream processing.

### References only

```bash
curl -v -H "Accept: application/xml" --form input=@./thefile.pdf localhost:8070/api/processReferences
```

Use this when you only care about bibliographical references.

## 4. Most useful request parameters

These are the parameters most users need early.

### `consolidateHeader`

- `0`: no consolidation
- `1`: enrich metadata with external service data
- `2`: DOI-only enrichment
- `3`: use extracted DOI only, if present

Use `0` if you want fully local/offline behavior.

### `consolidateCitations`

- `0`: no citation consolidation
- `1`: enrich citation metadata
- `2`: DOI-only enrichment

Use this carefully at scale. Consolidation can slow requests substantially.

### `teiCoordinates`

Adds PDF coordinates for selected structures.

Example:

```bash
curl -v -H "Accept: application/xml" --form input=@./thefile.pdf --form teiCoordinates=figure --form teiCoordinates=biblStruct localhost:8070/api/processFulltextDocument
```

### `includeRawCitations`

Includes the original raw reference strings in the result.

```bash
curl -v -H "Accept: application/xml" --form input=@./thefile.pdf --form includeRawCitations=1 localhost:8070/api/processFulltextDocument
```

### `start` and `end`

Restrict processing to a page range.

```bash
curl -v -H "Accept: application/xml" --form input=@./thefile.pdf --form start=3 --form end=8 localhost:8070/api/processFulltextDocument
```

Useful for long documents or targeted debugging.

## 5. Interpret the response correctly

| Status | Meaning | What to do |
| --- | --- | --- |
| `200` | Success | Continue |
| `204` | Request completed, but no structured content was extracted | Check input quality and endpoint choice |
| `400` | Request is malformed or missing required input | Verify request shape and parameters |
| `500` | Processing failed | Check the detailed error message and logs |
| `503` | Service currently unavailable because capacity is exhausted | Back off and retry |

Common processing error codes you may see:

| Error code | Meaning | What to do |
| --- | --- | --- |
| `BAD_INPUT_DATA` | PDF is unreadable, damaged, or otherwise unusable | Verify the file and retry with a known-good PDF |
| `NO_BLOCKS` | PDF has no usable text blocks | Use OCR first if the PDF is image-only |
| `TOO_MANY_BLOCKS` | Document is too large/complex for safe processing | Split the document or process fewer pages |
| `TOO_MANY_TOKENS` | Document is too long/token-heavy | Reduce scope with `start` / `end` |
| `TIMEOUT` | Processing took too long | Lower load, reduce scope, retry on simpler input |
| `PDFALTO_CONVERSION_FAILURE` | PDF parsing failed before structuring | Check file integrity and logs |

## 6. Understand `503` correctly

In GROBID, `503` often means the service is saturated, not dead.

Recommended retry guidance:

- `processHeaderDocument`: wait around 2 seconds before retry
- `processReferences`: wait around 3 to 6 seconds before retry
- `processFulltextDocument`: wait around 5 to 10 seconds before retry

If you are sending many requests:

- reduce client concurrency
- add retry with backoff
- do not treat `503` as a fatal permanent failure immediately

## 7. Common usage questions

### Why does `processHeaderDocument` differ from `processFulltextDocument`?

`processHeaderDocument` is faster and narrower. It focuses on header metadata and typically uses only the beginning of the PDF. `processFulltextDocument` processes the complete logical structure and is the better choice when you need the full article representation.

### Why does GROBID use the network during processing?

Because consolidation may be enabled. When enabled, GROBID can query CrossRef or biblio-glutton to enrich metadata.

If you want fully offline behavior, use:

- `consolidateHeader=0`
- `consolidateCitations=0`

### Where do DOI and metadata not present in the PDF come from?

From consolidation. GROBID can enrich extracted records with external metadata, which may improve results but can also change them compared with the raw PDF-only output.

### Can I extract only the body text and skip references?

Not through a simple endpoint switch. GROBID extracts the full document structure. If you only want the body text, post-process the TEI output and extract the `<body>` portion.

## 8. When to use a client library

If you are batch-processing many PDFs, use an existing client rather than scripting naïve parallel requests yourself.

Useful clients:

- Python
- Java
- Node.js

Clients are especially useful because they can handle batching, retries, and concurrency more safely than ad hoc scripts.

## 9. If something still fails

Go to:

- [Troubleshooting](../troubleshooting)
- [Docker Troubleshooting](../docker/docker-troubleshooting)
- [API Endpoints](../../reference/api-endpoints)

If you are unsure whether the problem is the service or the PDF, retry with:

- `latest-crf`
- no consolidation
- one known-good PDF
- one request at a time
