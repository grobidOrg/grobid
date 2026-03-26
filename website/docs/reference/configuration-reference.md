---
title: "Configuration Reference"
description: "All grobid.yaml parameters"
sidebar_position: 2
---

# Configuration Reference

Use this page when you need a quick lookup of important `grobid.yaml` sections and parameter meanings.

If you are still deciding what to change, start with the [Configuration Guide](../guides/configuration) first.

## General rules

- restart the service after changing `grobid.yaml`
- change one thing at a time
- keep a copy of the last working config
- for Docker, prefer mounting only the config file rather than replacing full `grobid-home`

## Core runtime paths

| Key | Meaning | Change it? |
| --- | --- | --- |
| `grobid.grobidHome` | Main runtime resource directory | Usually no |
| `grobid.temp` | Temporary file location | Rarely |
| `grobid.nativelibrary` | Native library directory | Usually no |

Example:

```yaml
grobid:
  grobidHome: "grobid-home"
  temp: "tmp"
  nativelibrary: "lib"
```

These are among the easiest settings to break if changed casually.

## PDF parser safety settings

| Key | Meaning |
| --- | --- |
| `pdf.pdfalto.path` | Location of bundled `pdfalto` parser |
| `pdf.pdfalto.memoryLimitMb` | Memory cap for one PDF parsing operation |
| `pdf.pdfalto.timeoutSec` | Max runtime for one PDF parsing operation |
| `pdf.blocksMax` | Max ALTO text blocks |
| `pdf.tokensMax` | Max extracted tokens |

Example:

```yaml
pdf:
  pdfalto:
    path: "pdfalto"
    memoryLimitMb: 6096
    timeoutSec: 60
  blocksMax: 100000
  tokensMax: 1000000
```

Do not change `pdfalto.path` in normal Docker usage.

## Consolidation settings

| Key | Meaning |
| --- | --- |
| `consolidation.service` | Which consolidation backend to use |
| `consolidation.crossref.mailto` | Contact email for CrossRef polite usage |
| `consolidation.crossref.token` | CrossRef Plus token, if available |
| `consolidation.crossref.timeoutSec` | CrossRef timeout |
| `consolidation.glutton.url` | biblio-glutton endpoint |
| `consolidation.glutton.timeoutSec` | biblio-glutton timeout |

CrossRef example:

```yaml
consolidation:
  crossref:
    mailto: you@example.org
    timeoutSec: 10
```

biblio-glutton example:

```yaml
consolidation:
  service: "glutton"
  glutton:
    url: "http://localhost:8080"
    timeoutSec: 60
```

## Proxy settings

Use when external consolidation calls must go through a proxy.

```yaml
proxy:
  host:
  port:
```

## Service behavior and throughput

| Key | Meaning |
| --- | --- |
| `grobid.concurrency` | Max number of parallel processing workers |
| `grobid.poolMaxWait` | Max wait time for a worker from the pool |
| `grobid.modelPreload` | Load models eagerly at startup or lazily on first use |

Example:

```yaml
grobid:
  concurrency: 10
  poolMaxWait: 1
  modelPreload: true
```

If `concurrency` is too low for your workload, you may see more `503` responses under load. If it is too high for your machine, you may destabilize the service.

## Service ports

```yaml
server:
  type: custom
  applicationConnectors:
    - type: http
      port: 8070
  adminConnectors:
    - type: http
      port: 8071
  registerDefaultExceptionMappers: false
```

Default roles:

- `8070`: application/API port
- `8071`: admin and health-related port

## CORS

```yaml
grobid:
  corsAllowedOrigins: "*"
  corsAllowedMethods: "OPTIONS,GET,PUT,POST,DELETE,HEAD"
  corsAllowedHeaders: "X-Requested-With,Content-Type,Accept,Origin"
```

Restrict these if a browser-based frontend should only be callable from specific origins.

## Language and sentence segmentation

| Key | Meaning |
| --- | --- |
| `grobid.languageDetectorFactory` | Language detection implementation |
| `grobid.sentenceDetectorFactory` | Sentence segmentation implementation |

Example:

```yaml
grobid:
  languageDetectorFactory: "org.grobid.core.lang.impl.CybozuLanguageDetectorFactory"
  sentenceDetectorFactory: "org.grobid.core.lang.impl.OpenNLPSentenceDetectorFactory"
```

For most users, these remain advanced settings.

## Logging

Minimal shape:

```yaml
logging:
  level: INFO
  appenders:
    - type: console
      threshold: WARN
    - type: file
      currentLogFilename: logs/grobid-service.log
      threshold: INFO
```

Useful default file log location:

```text
logs/grobid-service.log
```

## Training- and engine-related settings

These exist, but most runtime users do not need them immediately.

### Wapiti

```yaml
wapiti:
  nbThreads: 0
```

### DeLFT

```yaml
delft:
  install: "../delft"
  pythonVirtualEnv: "../delft/env"
```

### Per-model configuration

Model-level entries control:

- engine choice (`wapiti` vs `delft`)
- architecture
- training parameters
- runtime parameters

These belong mostly to training, experimentation, and advanced deployment scenarios.

## High-risk settings

Treat these with care:

- `grobid.grobidHome`
- `grobid.nativelibrary`
- `pdf.pdfalto.path`
- deep model path and runtime overrides

If one of these changes breaks startup, compare against the default config and revert aggressively.

## Related pages

- [Configuration Guide](../guides/configuration)
- [Troubleshooting](../guides/troubleshooting)
- [Docker Troubleshooting](../guides/docker/docker-troubleshooting)
