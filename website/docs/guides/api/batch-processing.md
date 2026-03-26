---
title: "Batch Processing"
description: "Process many PDFs efficiently"
sidebar_position: 4
---

# Batch Processing

Use this page when one successful request is no longer enough and you want to process many PDFs without collapsing the service.

The most common mistake at this stage is simple: users discover that one request works, then immediately send too many requests in parallel and treat `503` as a server failure. This page is here to prevent that.

## The first rule: use the service, not ad hoc batch mode

For large-scale processing, prefer the GROBID REST service plus an official client.

Why:

- the service is thread-safe
- it manages a pool of parser instances
- it scales better than old single-threaded batch-style command invocations
- it gives you clearer retry and throughput behavior

## Recommended clients

Official clients:

- Python
- Java
- Node.js

Practical recommendation:

- if you are starting from scratch, the Python client is often the easiest place to begin
- use an official client before writing your own naïve parallel request loop

These clients already align better with GROBID's concurrency model and are easier to adapt safely.

## What `503` really means in batch workflows

In GROBID, `503` usually means the service is currently saturated and all worker capacity is in use.

It does **not** automatically mean the service is broken.

Treat it as backpressure.

Your client should:

- wait
- retry
- avoid increasing concurrency blindly

## Suggested retry windows

These are practical starting points:

- `processHeaderDocument`: about 2 seconds
- `processReferences`: about 3 to 6 seconds
- `processFulltextDocument`: about 5 to 10 seconds
- lighter text endpoints such as `processDate`: about 1 second

If the service stays overloaded, reduce concurrency instead of only adding retries.

## Start with the simplest scaling path

Before optimizing heavily:

1. use the CRF image
2. keep consolidation off
3. process one known-good PDF successfully
4. increase parallelism gradually

This gives you a stable baseline before GPU, consolidation, full-image behavior, or large-document variability start to complicate diagnosis.

## Concurrency: server side and client side

There are always two sides to scaling:

- server-side concurrency in `grobid.yaml`
- client-side parallel request count

### Server side

The most important server-side config is:

```yaml
grobid:
  concurrency: 10
  poolMaxWait: 1
```

What it means:

- `concurrency` controls the size of the processing pool
- `poolMaxWait` controls how long a request waits for a worker before failing with backpressure behavior

### Client side

Your client also controls how many requests it sends in parallel.

That means you can overload GROBID even when the server is configured correctly.

## Practical tuning heuristics

### CRF image on CPU-only machines

Start with:

- server `concurrency` around your available thread count or slightly above
- client concurrency slightly above server concurrency, not dramatically above it

This is often the best throughput-per-complexity path.

### Full image with GPU

If the server has a real GPU, the full image may still scale reasonably because deep-learning inference is not hitting CPU the same way.

But you should still test incrementally instead of assuming the GPU makes every bottleneck disappear.

### Full image on CPU only

Be much more conservative.

Deep-learning inference on CPU increases pressure on:

- CPU
- memory
- throughput stability

A safer rule of thumb is to lower server concurrency and keep client concurrency at or near that lower value.

## Timeouts and `408` errors

When batch jobs fail with timeouts:

- do not assume the server is dead
- first reduce concurrency
- then increase client timeout
- then revisit image choice and memory limits

Typical reasons:

- too many simultaneous requests
- large or difficult PDFs
- full image on CPU-only hardware
- consolidation slowing the requests further

## Consolidation at scale

Be careful with consolidation in batch mode.

Why:

- it adds external dependency latency
- it lowers effective throughput
- it can create rate-limit or timeout problems

Recommendations:

- keep consolidation off until the plain extraction pipeline is stable
- use CrossRef for moderate enrichment needs
- use biblio-glutton for heavier scale if you have a reliable endpoint

If you consolidate citations heavily with CrossRef, scaling may become limited by the external service rather than GROBID itself.

## Memory pressure

If batch processing becomes unstable, reduce pressure before raising complexity.

Try this order:

1. lower client concurrency
2. lower server concurrency
3. keep or switch to the CRF image
4. raise memory if the host allows it
5. revisit `pdfalto` memory/timeouts only after simpler changes fail

## Monitoring and diagnostics

Useful checks while batch processing:

- `docker logs <container_name_or_id>`
- `http://localhost:8070/api/health`
- `http://localhost:8071` if admin port is exposed

The readiness endpoint and admin metrics help you distinguish:

- service not ready
- service saturated
- service crashed

## A safe batch rollout strategy

Use this progression:

1. one known-good PDF
2. a small batch with low parallelism
3. a larger batch with retries enabled
4. only then tune concurrency, consolidation, and image choice

This avoids the common mistake of debugging too many variables at once.

## When to move to performance tuning

Go to [Performance Tuning](../performance-tuning) when:

- you already have stable batch runs
- the next problem is throughput, not correctness
- you need to reason about CPU vs GPU, preload, memory, and production-scale settings more deliberately

## Related pages

- [REST API Usage](./rest-api-usage)
- [Consolidation](./consolidation)
- [Configuration Guide](../configuration)
- [Troubleshooting](../troubleshooting)
