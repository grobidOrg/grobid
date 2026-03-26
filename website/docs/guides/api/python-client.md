---
title: "Python Client"
description: "Use grobid_client_python for batch processing"
sidebar_position: 2
---

# Python Client

Use the Python client when your GROBID service is already running and you want a practical way to process many PDFs or integrate GROBID into a Python workflow.

This is usually better than writing your own naïve parallel request loop.

## What the Python client is good at

The client is especially useful for:

- processing folders of PDFs
- managing concurrency more safely than ad hoc scripts
- writing TEI outputs automatically
- optionally generating JSON or Markdown derived from TEI
- integrating GROBID into Python data pipelines

## Before you start

Assume:

- GROBID is already running
- the default service URL is `http://localhost:8070`

If the service is not running yet, go back to [Quick Start (Docker)](../../getting-started/quickstart-docker).

## Choose your path

| You want to... | Best path |
| --- | --- |
| process a folder of PDFs from the shell | CLI |
| integrate GROBID into Python code | Python API |
| turn existing TEI files into JSON or Markdown | formatting helpers |

## 1. Install the client

Install from PyPI:

```bash
pip install grobid-client-python
```

## 2. Fastest useful CLI example

Process a folder of PDFs into TEI XML:

```bash
grobid_client --input ./pdfs --output ./out processFulltextDocument
```

This is the strongest first-use path for most users.

## 3. Emit JSON or Markdown too

If you also want derived outputs from the TEI result:

```bash
grobid_client --input ./pdfs --output ./out --json --markdown processFulltextDocument
```

Important:

- JSON and Markdown are derived from TEI, not separate server-side formats
- the client first writes TEI, then performs the conversion locally

## 4. Minimal Python example

```python
from grobid_client.grobid_client import GrobidClient

client = GrobidClient()
client.process(
    "processFulltextDocument",
    input_path="./pdfs",
    output="./out",
)
```

This is the best place to start if you want Python-level integration.

## 5. Most important options first

### `--server`

Point the client at a non-default server:

```bash
grobid_client --server http://my-host:8070 --input ./pdfs --output ./out processFulltextDocument
```

### `--input` and `--output`

- `--input`: directory of files to process
- `--output`: where TEI and optional derived outputs are written

### `--n`

Controls concurrency on the client side.

This is one of the most important knobs for throughput and stability.

If you start seeing many `503` responses, reduce `--n` first.

### `--force`

Overwrite existing outputs if needed.

Useful when rerunning a workflow on the same output directory.

### `--json` and `--markdown`

Generate additional local output formats from TEI:

- `--json`
- `--markdown`

Use these when TEI is not the only downstream format you need.

## 6. Common services you will actually use

### Fulltext

```bash
grobid_client --input ./pdfs --output ./out processFulltextDocument
```

### Header only

```bash
grobid_client --input ./pdfs --output ./out processHeaderDocument
```

### References only

```bash
grobid_client --input ./pdfs --output ./out processReferences
```

### Citation-list parsing from text files

```bash
grobid_client --input ./txt_refs --output ./out processCitationList
```

This is useful if you already have raw citation strings rather than PDFs.

## 7. Coordinates and sentence segmentation

If you need downstream annotation or span-aware processing, use coordinate and sentence options.

These are advanced but practical when needed.

The client exposes options for:

- `teiCoordinates`
- sentence segmentation

This is especially useful when you later want:

- JSON with structured spans
- browser/PDF overlays
- passage-aware post-processing

## 8. Config file mode

The client can load settings from `config.json`.

This is useful when you want stable defaults for:

- server URL
- batch size
- timeout
- sleep time
- output-related settings

A minimal example:

```json
{
  "grobid_server": "http://localhost:8070",
  "batch_size": 1000,
  "timeout": 60,
  "sleep_time": 5
}
```

Practical advice:

- treat this as a convenience config, not a full declarative workflow system
- still keep your first examples simple before relying on config files heavily

## 9. Important pitfall: `n` is not `batch_size`

These are easy to confuse.

- `n` controls client concurrency
- `batch_size` controls how many discovered files are grouped for dispatch logic

If throughput or `503` behavior is weird, make sure you are changing the right one.

## 10. Important pitfall: the client checks the server early

The client checks server availability during initialization by default.

That means a run can fail before any files are processed if:

- the server is not up
- the URL is wrong
- the service is not ready yet

So if the client appears to fail immediately, verify the server first.

## 11. CLI and library defaults are not identical

Do not assume the CLI and Python API behave exactly the same by default.

In particular, some defaults in the library path are more aggressive or more convenient for programmatic use than the CLI flags suggest.

If something feels inconsistent, be explicit rather than relying on implicit defaults.

## 12. Formatting helpers

The repo also provides TEI conversion helpers:

- TEI -> JSON
- TEI -> Markdown

These are useful when:

- you already have TEI files
- you want a lighter downstream representation
- you want a readable Markdown form for inspection or LLM/RAG pipelines

## 13. Recommended first learning sequence

1. CLI: process a folder to TEI
2. CLI: add `--json` or `--markdown`
3. Python: use `GrobidClient()` in a short script
4. Then tune `--n`, timeout, and config file defaults

That sequence gives the fastest path from success to useful automation.

## 14. If something goes wrong

Go to:

- [Batch Processing](./batch-processing)
- [REST API Usage](./rest-api-usage)
- [Troubleshooting](../troubleshooting)

If the service is healthy but the client still struggles, simplify first:

- use `processFulltextDocument`
- use one small PDF
- lower `--n`
- point to a clearly working `http://localhost:8070`
