---
title: "API Endpoints"
description: "Complete REST API reference"
sidebar_position: 1
---

# API Endpoints

Use this page when you already understand the basic request flow and need a faster lookup of the main endpoints, parameters, and response behavior.

If you are just getting started, begin with [REST API Usage](../guides/api/rest-api-usage).

## Service and health endpoints

| Endpoint | Method | Purpose |
| --- | --- | --- |
| `/api/version` | `GET` | Returns the running version and revision |
| `/api/isalive` | `GET` | Liveness check |
| `/api/health` | `GET` | Readiness check with more detailed status |

Use these first when you are unsure whether the service is actually ready.

## Admin and metrics endpoints

When the admin port is exposed, the service also provides additional operational endpoints on the admin side.

Useful examples include:

- the admin console on `8071`
- health and heartbeat-style checks from the Dropwizard admin stack
- Prometheus-format metrics on `/metrics/prometheus`

These are most useful in production rollouts, load testing, and service monitoring rather than first-time API exploration.

## Main PDF-processing endpoints

| Endpoint | Method | Input | Output | Best for |
| --- | --- | --- | --- | --- |
| `/api/processHeaderDocument` | `POST`, `PUT` | PDF multipart upload | TEI XML or BibTeX | Fast metadata extraction from the beginning of the document |
| `/api/processFulltextDocument` | `POST`, `PUT` | PDF multipart upload | TEI XML | Full document structure, body, references, and rich output |
| `/api/processReferences` | `POST`, `PUT` | PDF multipart upload | TEI XML or BibTeX | Reference extraction only |

## Text-processing endpoints

| Endpoint | Method | Input | Best for |
| --- | --- | --- | --- |
| `/api/processDate` | `POST` | date string | Date normalization |
| `/api/processCitationNames` | `POST` | names string | Citation-name parsing |
| `/api/processAffiliations` | `POST` | affiliation string | Affiliation parsing |
| `/api/processCitation` | `POST` | single citation string | One citation string into structured output |
| `/api/processCitationList` | `POST` | citation list string | Multiple raw citations |

These are useful for integration workflows that already have extracted text instead of PDFs.

## Annotation and PDF-output endpoints

| Endpoint | Method | Input | Output | Notes |
| --- | --- | --- | --- | --- |
| `/api/referenceAnnotations` | `POST`, `PUT` | PDF multipart upload | JSON | Reference annotations with coordinates |

This is useful when you want structured annotations positioned back onto the original PDF rendering.

## Most useful parameters by endpoint

### `/api/processHeaderDocument`

Common parameters:

- `input` (required)
- `consolidateHeader`
- `includeRawAffiliations`
- `includeRawCopyrights`
- `start`
- `end`

Response formats:

- `Accept: application/xml`
- `Accept: application/x-bibtex`

Use BibTeX only when you specifically need it. TEI XML is the richer default.

### `/api/processFulltextDocument`

Common parameters:

- `input` (required)
- `consolidateHeader`
- `consolidateCitations`
- `consolidateFunders`
- `includeRawCitations`
- `includeRawAffiliations`
- `includeRawCopyrights`
- `teiCoordinates`
- `segmentSentences`
- `generateIDs`
- `start`
- `end`
- `flavor`

This is the most important endpoint for document-level TEI output.

### `/api/processReferences`

Common parameters:

- `input` (required)
- `consolidateCitations`
- `includeRawCitations`

Response formats:

- `Accept: application/xml`
- `Accept: application/x-bibtex`

## Response behavior

| Status | Meaning |
| --- | --- |
| `200` | Success |
| `204` | Request completed but no structured content was extracted |
| `400` | Wrong request or missing required data |
| `500` | Processing error |
| `503` | Service is currently unavailable because capacity is exhausted |

Common GROBID error codes that may appear in `500` responses:

- `BAD_INPUT_DATA`
- `NO_BLOCKS`
- `TOO_MANY_BLOCKS`
- `TOO_MANY_TOKENS`
- `TIMEOUT`
- `TAGGING_ERROR`
- `PARSING_ERROR`
- `GENERAL`
- `PDFALTO_CONVERSION_FAILURE`

For interpretation and recovery, use [Troubleshooting](../guides/troubleshooting).

## Accept headers

Always send an explicit `Accept` header.

Recommended default:

```http
Accept: application/xml
```

Some endpoints also support:

```http
Accept: application/x-bibtex
```

Do not rely on implicit response-type defaults.

## `503` retry guidance

`503` usually means backpressure, not a dead service.

Suggested retry windows:

- `/api/processHeaderDocument`: about 2 seconds
- `/api/processReferences`: about 3 to 6 seconds
- `/api/processFulltextDocument`: about 5 to 10 seconds
- lightweight text endpoints such as `/api/processDate`: about 1 second

If you see many `503` responses in a batch workflow, reduce client concurrency before blaming the service.

## Minimal examples

### Header extraction

```bash
curl -v -H "Accept: application/xml" --form input=@./thefile.pdf localhost:8070/api/processHeaderDocument
```

### Fulltext extraction

```bash
curl -v -H "Accept: application/xml" --form input=@./thefile.pdf localhost:8070/api/processFulltextDocument
```

### References only

```bash
curl -v -H "Accept: application/xml" --form input=@./thefile.pdf localhost:8070/api/processReferences
```

### Header as BibTeX

```bash
curl -v -H "Accept: application/x-bibtex" --form input=@./thefile.pdf localhost:8070/api/processHeaderDocument
```

## When to stay in how-to mode instead

If you are still deciding which endpoint to call or how to shape the request, go back to [REST API Usage](../guides/api/rest-api-usage).
