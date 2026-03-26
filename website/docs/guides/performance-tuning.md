---
title: "Performance Tuning"
description: "Optimize memory, concurrency, and throughput"
---

# Performance Tuning

Use this page when GROBID is already working and your next problem is speed, throughput, or stability under load.

Do not start here. First make sure you have:

- a clean startup
- one successful request
- a small stable batch run

Only then start tuning.

## Tune in this order

The safest tuning order is:

1. choose the right image
2. tune concurrency
3. tune client parallelism
4. revisit memory
5. only then revisit deeper config and model choices

Most performance problems come from getting the early decisions wrong, not from missing exotic flags.

## 1. Choose the right image first

### CRF image (`latest-crf`)

Best for:

- CPU-only systems
- high throughput
- lower memory usage
- simpler and more predictable operations

This is usually the best tuning baseline.

### Full image (`latest-full`)

Best for:

- users who explicitly want the deep-learning-enhanced path
- systems with enough resources, ideally GPU-backed where applicable

Trade-offs:

- larger image and memory footprint
- more operational complexity
- slower and less predictable on CPU-only hardware

If you have not proven that you need the full image, do not tune around it first.

## 2. Tune server-side concurrency

The main server-side knob is:

```yaml
grobid:
  concurrency: 10
  poolMaxWait: 1
```

`concurrency` controls how many processing workers can run in parallel.

General guidance:

- for CRF on CPU, start around available thread count or slightly above
- for full image on CPU only, start lower and be more conservative
- if the service becomes unstable, reduce it before doing anything else

`poolMaxWait` is normally not the first thing to tune.

## 3. Tune client-side parallelism

Server tuning alone is not enough.

If the client sends too many simultaneous requests, you will still overload the service.

Practical guidance:

- keep client concurrency near the server's actual capacity
- increase gradually rather than jumping to large values
- treat repeated `503` responses as a sign to reduce pressure, not to panic

If you are using an official client, prefer adjusting its concurrency settings there instead of writing your own uncontrolled request flood.

## 4. Understand `503` correctly

`503` in GROBID usually means the service is saturated and protecting itself from collapse.

That means:

- it is often a tuning signal, not a crash signal
- reduce concurrency first
- add backoff and retry second
- only then consider deeper changes

For endpoint-specific retry guidance, see [Batch Processing](./api/batch-processing).

## 5. Memory tuning

If the service is unstable under load, memory is one of the next things to revisit.

Main levers:

- Docker/container memory allocation
- server-side concurrency
- client-side concurrency
- `pdfalto` memory/timeouts in config

### If you are memory constrained

Try this order:

1. lower client concurrency
2. lower server concurrency
3. use or switch back to `latest-crf`
4. increase available memory if possible
5. only then change parser safety limits

This order avoids turning a resource problem into a hard-to-debug configuration problem.

## 6. PDF parser safety limits

Relevant config:

```yaml
pdf:
  pdfalto:
    memoryLimitMb: 6096
    timeoutSec: 60
  blocksMax: 100000
  tokensMax: 1000000
```

These are safety controls, not routine performance knobs.

Use them when:

- very large PDFs time out repeatedly
- memory pressure causes parser instability
- you need tighter circuit breakers for a constrained environment

Do not lower them aggressively without evidence.

## 7. Model preload

```yaml
grobid:
  modelPreload: true
```

`modelPreload: true` is a good default for service stability and predictable warm behavior.

Use lazy loading only if you intentionally prefer different startup/memory trade-offs and accept slower first requests.

## 8. CRF vs full image tuning heuristics

### CRF on CPU

This is the easiest path to tune well.

Good default strategy:

- keep `modelPreload: true`
- tune `concurrency` upward gradually
- keep client concurrency only slightly above the service capacity

### Full image with GPU

If you have a real GPU-backed setup, full-image tuning can be more forgiving than CPU-only full-image use.

Still:

- do not assume GPU removes every bottleneck
- watch request latency and throughput together
- keep the rest of the setup simple while validating GPU benefits

### Full image on CPU only

Be conservative.

Why:

- deep-learning inference can push CPU much harder
- memory pressure rises
- throughput becomes less regular

A safer pattern is to lower both server concurrency and client concurrency compared with the CRF path.

## 9. Consolidation and throughput

Consolidation changes performance behavior materially.

If you turn it on:

- request latency rises
- throughput drops
- external service behavior becomes part of your runtime

Practical rule:

- keep consolidation off until plain extraction is stable
- if you need scaling with enrichment, biblio-glutton is usually a better operational fit than CrossRef alone

See [Consolidation](./api/consolidation) for the trade-offs.

## 10. Practical baseline recommendations

### Small CPU-only deployment

- image: `latest-crf`
- no consolidation initially
- moderate concurrency
- admin port enabled for diagnostics

### Higher-throughput CPU deployment

- image: `latest-crf`
- increase `concurrency` gradually
- use official clients with bounded parallelism
- tune memory only after concurrency is understood

### Accuracy-first deployment with stronger hardware

- full image only if you know why you need it
- validate resource headroom first
- keep batch pressure conservative at the start

## 11. Signs you are tuning the wrong thing

You may be tuning the wrong layer if:

- a single known-good PDF still fails
- startup is unstable before load increases
- config overrides are still changing frequently
- Docker mounts or shell syntax are still part of the problem

If so, go back to:

- [Docker Troubleshooting](./docker/docker-troubleshooting)
- [Troubleshooting](./troubleshooting)
- [Configuration Guide](./configuration)

## Related pages

- [Batch Processing](./api/batch-processing)
- [Configuration Guide](./configuration)
- [Consolidation](./api/consolidation)
- [Docker Troubleshooting](./docker/docker-troubleshooting)
