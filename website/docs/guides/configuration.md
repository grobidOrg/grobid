---
title: "Configuration Guide"
description: "Configure GROBID for your needs"
---

# Configuration Guide

Use this page when you need to change GROBID behavior safely without breaking the bundled runtime.

Most users do **not** need to edit the configuration immediately. If your goal is simply to run GROBID, start with the [Docker Builder](./docker/docker-setup) and only add a config override when you actually need one.

## When you should edit `grobid.yaml`

Typical reasons:

- enable or change consolidation behavior
- change service ports
- tune concurrency for throughput or stability
- adjust PDF parser timeouts or memory limits
- change logging or CORS behavior

Typical reasons **not** to edit it yet:

- you just want the service to start
- you are still validating your first successful request
- you are debugging startup problems and want the fewest moving parts

## Safe changes most users care about first

### Consolidation

Use this when you want DOI and metadata enrichment from external services.

You can configure:

- CrossRef
- biblio-glutton
- timeouts for external lookups

The Docker Builder already generates a full config override for common consolidation cases, which is the safest path for most users.

### Ports

By default, GROBID uses:

- application port: `8070`
- admin port: `8071`

If you need to change them in config, the relevant section is:

```yaml
server:
  type: custom
  applicationConnectors:
    - type: http
      port: 8070
  adminConnectors:
    - type: http
      port: 8071
```

For Docker users, changing the host-side published port is usually easier than editing the config.

### Concurrency

The most important server-side throughput setting is `concurrency`.

```yaml
grobid:
  concurrency: 10
  poolMaxWait: 1
```

What it means:

- `concurrency`: max number of parallel processing threads in the GROBID service
- `poolMaxWait`: how long waiting requests try to obtain a worker before the service gives up

If you see many `503` responses under load, revisit this together with your client-side request rate. Increasing it blindly is not always the right answer.

### PDF parser limits

These are the main safety controls for `pdfalto`:

```yaml
pdf:
  pdfalto:
    path: "pdfalto"
    memoryLimitMb: 6096
    timeoutSec: 60
  blocksMax: 100000
  tokensMax: 1000000
```

Change these when:

- large PDFs time out
- memory pressure is causing failures
- you need stricter safety limits for a constrained environment

Be conservative. These are circuit breakers, not convenience settings.

### Model preload

```yaml
grobid:
  modelPreload: true
```

Use `true` when you want the service warm and ready after startup.

Use lazy loading only if you intentionally prefer slower first requests in exchange for different startup/memory tradeoffs.

## Dangerous or usually unnecessary changes

These settings are real, but most users should leave them alone.

### `grobidHome`

```yaml
grobid:
  grobidHome: "grobid-home"
```

Do not change this unless you know exactly why you are doing it.

Why:

- `grobid-home` contains models, native libraries, parser resources, and other bundled runtime assets
- changing it casually is one of the easiest ways to break a working install

### `pdfalto.path`

```yaml
pdf:
  pdfalto:
    path: "pdfalto"
```

Do not change this in normal Docker usage. The image already contains the correct bundled parser resources.

### Native library paths

```yaml
grobid:
  nativelibrary: "lib"
```

Leave this alone unless you are doing advanced local/native work.

## Common configuration scenarios

### 1. Run fully offline

If you do not want GROBID calling external services during processing:

- set request parameters such as `consolidateHeader=0` and `consolidateCitations=0`
- or configure consolidation so it is effectively off for your workflow

This is the simplest way to keep API behavior predictable.

### 2. Enable CrossRef politely

```yaml
consolidation:
  crossref:
    mailto: you@example.org
    timeoutSec: 10
```

The email is optional, but recommended if you want more reliable polite-pool behavior.

### 3. Enable biblio-glutton

```yaml
consolidation:
  service: "glutton"
  glutton:
    url: "http://localhost:8080"
    timeoutSec: 60
```

Use this when you want faster, more scalable consolidation than CrossRef alone.

### 4. Tune a CPU-only service for stability

Start by changing only:

- `concurrency`
- `pdf.pdfalto.memoryLimitMb`
- `pdf.pdfalto.timeoutSec`

Do not change many unrelated parameters at once. If the service gets worse, you want to know which change caused it.

### 5. Restrict browser access with CORS

```yaml
grobid:
  corsAllowedOrigins: "https://your-app.example"
  corsAllowedMethods: "OPTIONS,GET,PUT,POST,DELETE,HEAD"
  corsAllowedHeaders: "X-Requested-With,Content-Type,Accept,Origin"
```

Use this when a frontend application should be the only allowed browser origin.

## Logging and diagnostics

The default file log is typically written to:

```text
logs/grobid-service.log
```

In Docker, start with:

```powershell
docker logs <container_name_or_id>
```

If you are troubleshooting a config change, capture logs before changing several more things.

## Docker-specific advice

If you are using Docker:

- do not mount full `grobid-home` unless you intentionally seeded it from the image
- prefer mounting only the config file when you need an override
- keep the rest of the container runtime bundled by default

This is the safest path and avoids breaking `pdfalto`, native libraries, and model resources.

## Practical editing strategy

When changing config:

1. change one thing at a time
2. restart the service
3. test one known-good request
4. keep a copy of the last working config

This sounds obvious, but it prevents many self-inflicted debugging loops.

## Related pages

- [Docker Setup](./docker/docker-setup)
- [Docker Troubleshooting](./docker/docker-troubleshooting)
- [Troubleshooting](./troubleshooting)
- [REST API Usage](./api/rest-api-usage)
- [Configuration Reference](../reference/configuration-reference)
