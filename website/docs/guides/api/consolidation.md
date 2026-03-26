---
title: "Consolidation"
description: "Enrich results with CrossRef metadata"
sidebar_position: 5
---

# Consolidation

Use consolidation when you want GROBID to enrich extracted metadata using external bibliographic services.

This is one of the biggest sources of user confusion because it changes:

- whether GROBID uses the network during processing
- how much metadata may be added or corrected
- request performance and scalability

## What consolidation does

GROBID first extracts metadata from the PDF itself. Consolidation then sends a reliable subset of that extracted information to an external service and tries to match it against richer bibliographic records.

This can improve:

- header metadata quality
- reference metadata quality
- DOI recovery
- bibliographic completeness

It can also change the result compared with pure PDF-only extraction.

## When to leave it off

Leave consolidation off if you want:

- fully offline behavior
- fewer moving parts during first setup
- easier debugging of pure PDF extraction quality
- simpler high-throughput pipelines

This is why the safest first-use path is usually:

- `consolidateHeader=0`
- `consolidateCitations=0`

## The three practical modes

### 1. No consolidation

Best for:

- first-time setup
- offline processing
- debugging raw extraction quality
- predictable behavior without external metadata changes

Use:

- `consolidateHeader=0`
- `consolidateCitations=0`

### 2. CrossRef

Best for:

- users who want enrichment without self-hosting another service
- lighter operational complexity
- moderate-scale usage

Trade-offs:

- slower at scale than biblio-glutton
- metadata limited to what CrossRef can provide
- network-dependent

CrossRef is the easiest external enrichment option because you do not need to host anything yourself.

### 3. biblio-glutton

Best for:

- higher-throughput processing
- richer metadata needs
- users who can host or access a glutton service endpoint

Trade-offs:

- requires an available service endpoint
- adds operational complexity
- still introduces external dependency, just one you control more directly

Compared with CrossRef, biblio-glutton is typically the better scaling option and can return richer metadata.

## What may change in the output

With consolidation enabled, GROBID may:

- add DOI values
- enrich journal, issue, volume, or other bibliographic fields
- adjust metadata based on matched external records

This means the final TEI may contain metadata that was not directly recoverable from the PDF alone.

In some cases, enriched metadata may also reflect a published record rather than a preprint-style source record.

## CrossRef configuration

Basic example:

```yaml
consolidation:
  crossref:
    mailto: you@example.org
    timeoutSec: 10
```

Notes:

- `mailto` is optional but recommended
- it improves polite-pool behavior and makes requests more reliable
- `timeoutSec` controls how long GROBID waits for CrossRef

If you have a CrossRef Plus token:

```yaml
consolidation:
  crossref:
    token: YOUR_CROSSREF_PLUS_TOKEN
    timeoutSec: 10
```

## biblio-glutton configuration

Example:

```yaml
consolidation:
  service: "glutton"
  glutton:
    url: "http://localhost:8080"
    timeoutSec: 60
```

The key piece is the endpoint URL. In Docker-based setups, this may be:

- a host-local endpoint such as `http://host.docker.internal:8080`
- a remote hosted endpoint
- a self-hosted service reachable from the container

## Request-level control

You do not need to rely only on config. At request time, you can control whether header and citation consolidation are used.

Useful values:

### `consolidateHeader`

- `0`: none
- `1`: enrich metadata
- `2`: DOI only
- `3`: DOI-only path using extracted DOI if present

### `consolidateCitations`

- `0`: none
- `1`: enrich citation metadata
- `2`: DOI only

This makes it possible to keep the service configured for consolidation but disable or narrow it for specific workflows.

## Performance and throughput trade-offs

Consolidation is not free.

It can:

- slow requests
- increase timeout risk
- reduce throughput at scale
- make failures depend on external service availability

That is why it is often a bad idea to enable it immediately in a high-throughput batch pipeline before you have tested the simpler extraction-only path.

## Timeout tuning

Both services support configurable timeouts:

```yaml
consolidation:
  crossref:
    timeoutSec: 100
  glutton:
    timeoutSec: 60
```

Be careful with overly aggressive values. Short timeouts combined with high-volume usage can create confusing intermittent failures.

## Recommended usage strategy

### For first-time users

Start with consolidation off.

### For moderate enrichment needs

Start with CrossRef and add `mailto`.

### For heavier scaling or richer metadata

Use biblio-glutton if you have a reliable endpoint.

### For debugging extraction quality

Turn consolidation off so you can see what came from the PDF itself.

## Related pages

- [REST API Usage](./rest-api-usage)
- [Configuration Guide](../configuration)
- [Docker Setup](../docker/docker-setup)
- [Understanding the Output](../../getting-started/understanding-the-output)
