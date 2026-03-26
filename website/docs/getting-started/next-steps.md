---
title: "Next Steps"
description: "Where to go after the quickstart"
sidebar_position: 6
---

# Next Steps

If you have already started GROBID and made at least one successful request, this page helps you choose the right next document instead of wandering through the sidebar.

## If you just got your first successful result

Start here:

- [Your First Extraction](./your-first-extraction)
- [Understanding the Output](./understanding-the-output)

This path is best if you are still learning what GROBID returns and how to reason about TEI output.

## If you want to make more API requests correctly

Go here:

- [REST API Usage](../guides/api/rest-api-usage)
- [API Endpoints](../reference/api-endpoints)

Use this path if you want to:

- call other endpoints
- understand parameters
- interpret responses correctly
- build a client integration

## If startup or requests fail

Go here:

- [Docker Troubleshooting](../guides/docker/docker-troubleshooting)
- [Troubleshooting](../guides/troubleshooting)

Use Docker troubleshooting when the problem is:

- container startup
- mounts
- shell syntax
- platform-specific Docker behavior

Use the general troubleshooting page when the problem is:

- request failures
- empty or partial output
- `503`, timeout, or input errors

## If you want to change behavior safely

Go here:

- [Configuration Guide](../guides/configuration)
- [Configuration Reference](../reference/configuration-reference)

Use this path when you want to:

- enable consolidation
- change ports
- tune concurrency
- adjust parser limits
- understand `grobid.yaml`

## If you want metadata enrichment

Go here:

- [Consolidation](../guides/api/consolidation)

Use this path when you want to understand:

- CrossRef vs biblio-glutton
- offline vs online behavior
- why metadata may change after enrichment
- throughput trade-offs caused by consolidation

## If you want to process many PDFs

Go here:

- [Batch Processing](../guides/api/batch-processing)
- [Performance Tuning](../guides/performance-tuning)

Use this path when one request is no longer enough and you need to think about:

- concurrency
- retries
- throughput
- CPU vs full image trade-offs

## If the default processing does not fit your documents

Go here:

- [Processing Flavors](../reference/processing-flavors)

Use this when you are working with:

- article-adjacent documents such as corrections or letters
- standards/specification documents
- specialized processing variants rather than normal scholarly-article defaults

## If you are contributing or debugging locally

Go here:

- [Build from Source](./quickstart-local)

Use this path when Docker is not enough because you need to:

- change code
- debug the local runtime
- work on models or training-related tasks

## A simple decision guide

| Your situation | Best next page |
| --- | --- |
| I got a result but do not understand the XML | [Understanding the Output](./understanding-the-output) |
| I need another endpoint or parameter | [REST API Usage](../guides/api/rest-api-usage) |
| Docker startup failed | [Docker Troubleshooting](../guides/docker/docker-troubleshooting) |
| I want to change config safely | [Configuration Guide](../guides/configuration) |
| I want more metadata enrichment | [Consolidation](../guides/api/consolidation) |
| I want to process many PDFs | [Batch Processing](../guides/api/batch-processing) |
| I need better throughput or stability under load | [Performance Tuning](../guides/performance-tuning) |
| Default processing is the wrong shape for my documents | [Processing Flavors](../reference/processing-flavors) |

## Recommended learning order

If you are not sure what to do next, this is a good sequence:

1. [Your First Extraction](./your-first-extraction)
2. [Understanding the Output](./understanding-the-output)
3. [REST API Usage](../guides/api/rest-api-usage)
4. [Configuration Guide](../guides/configuration)
5. [Batch Processing](../guides/api/batch-processing)

That path usually gets users from first success to real usage with the least confusion.
