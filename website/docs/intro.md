---
title: "GROBID Documentation"
description: "Extract structured data from scholarly PDFs with the fastest safe path to a working GROBID setup"
sidebar_position: 1
slug: /
---

# GROBID Documentation

GROBID extracts structured data from scholarly PDFs: titles, authors, affiliations, references, citations, section structure, full text, and TEI XML.

Common capabilities include:

- header extraction for titles, abstracts, authors, affiliations, and keywords
- reference extraction and parsing from PDFs or raw citation strings
- fulltext structuring into sections, paragraphs, figures, tables, notes, and citations
- PDF coordinates for mapping extracted structures back onto the source document
- metadata enrichment through CrossRef or biblio-glutton when consolidation is enabled
- specialized processing flavors for non-standard document types

If you want to get productive quickly, start with Docker. The documentation builder generates a safer `docker run` command, explains the important flags, and helps you avoid the most common setup mistakes reported in GitHub issues.

For most users, the shortest successful path is:

1. open the Docker Builder
2. start the service with the CRF image
3. verify `localhost:8070`
4. make your first REST API request

## Start here

### Recommended: Docker Builder

- Best for most users on Windows, macOS, and Linux
- Guides you through image choice, paths, ports, consolidation, and shell-specific command syntax
- Prevents common mistakes like invalid config mounting or unsafe `grobid-home` overrides

[Open the Docker Builder](./guides/docker/docker-setup)

### Quick path

If you already know you want Docker, go directly here:

[Quick Start with Docker](./getting-started/quickstart-docker)

## Choose your path

### I want GROBID running as fast as possible

- Use [Quick Start (Docker)](./getting-started/quickstart-docker)
- Then continue with [REST API Usage](./guides/api/rest-api-usage)
- If startup fails, go directly to [Docker Troubleshooting](./guides/docker/docker-troubleshooting)

### I need help choosing Docker options

- Use the [Docker Builder](./guides/docker/docker-setup)
- If startup or mounts fail, check [Docker Troubleshooting](./guides/docker/docker-troubleshooting)
- If requests fail after startup, check [Troubleshooting](./guides/troubleshooting)

### I need to build from source

- Go to [Quick Start (Local Build)](./getting-started/quickstart-local)
- Useful for contributors, training workflows, and advanced debugging

### I need to understand the API or outputs

- Start with [REST API Usage](./guides/api/rest-api-usage)
- Then use [API Endpoints](./reference/api-endpoints)

## What users most often get stuck on

The issue triage shows a clear pattern:

- Docker setup and shell-specific command syntax
- Configuration files and consolidation setup
- Error diagnosis and recovery
- API request details and failure modes

The docs are therefore optimized around three early moves:

- get the service running safely
- recover quickly when startup or requests fail
- make the first correct API request without reading a giant reference page

This documentation is organized to get you past those blockers early.

## Documentation map

### Tutorials

- Learn by doing with short, guided outcomes
- Start with [Quick Start (Docker)](./getting-started/quickstart-docker)

### How-to guides

- Solve a task you already know you need
- Start with [Docker Setup](./guides/docker/docker-setup), [Troubleshooting](./guides/troubleshooting), or [Configuration](./guides/configuration)

### Reference

- Look up API endpoints, config keys, Docker options, and response formats
- Start with [API Endpoints](./reference/api-endpoints) and [Configuration Reference](./reference/configuration-reference)

### Explanation

- Understand why GROBID behaves the way it does, and when to choose one setup over another
- Start with [Architecture](./explanation/architecture)
