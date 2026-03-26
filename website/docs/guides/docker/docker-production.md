---
title: "Production Deployment"
description: "Deploy GROBID in production environments"
sidebar_position: 4
---

# Production Deployment

Use this page when GROBID is already working and you want a safer operational baseline for long-running or higher-volume deployments.

## Start with the simple production shape

For most production setups, the safest first baseline is:

- `latest-crf`
- no consolidation at first
- admin port enabled
- bounded client concurrency
- `modelPreload: true`

This keeps the runtime predictable while you validate your real workload.

## Choose the image deliberately

### `latest-crf`

Best default for production when you want:

- simpler operations
- lower memory usage
- easier throughput tuning
- CPU-friendly behavior

### `latest-full`

Use this only when you intentionally need the deeper model stack and you understand the operational cost.

It is a better fit when:

- deep-learning-backed models matter for your workload
- you have stronger hardware
- you are ready for higher memory usage and more tuning effort

## Concurrency guidance

The key production control is server-side `concurrency`, together with bounded client-side parallelism.

Practical starting points:

- CRF on CPU: set server `concurrency` near available thread count or slightly above, then keep client concurrency only slightly above that
- full image on CPU only: start much lower, often around half the available thread count
- full image with GPU: validate carefully, but you can usually be less conservative than full image on CPU only

If you see many `503` responses, treat that as backpressure. Reduce client pressure first before increasing complexity.

## Memory expectations

Memory needs depend on the endpoint mix, image choice, and load pattern.

Useful mental model:

- header-heavy usage needs less memory than fulltext extraction
- CRF deployments are easier to size and stabilize
- full-image CPU deployments need more memory headroom than CRF
- citation consolidation at scale adds latency and external dependency pressure even when local memory is fine

If the service is unstable under load:

1. reduce client concurrency
2. reduce server concurrency
3. simplify back to `latest-crf` if possible
4. increase available memory

## Consolidation in production

Do not enable consolidation immediately unless you already know you need it.

Why:

- it changes result content
- it increases latency
- it reduces throughput
- it introduces external dependency behavior into your runtime

If you need scaling with enrichment, biblio-glutton is usually the better operational fit than CrossRef alone.

## Admin and health endpoints

Expose the admin port when practical. It gives you clearer operational signals during rollout and troubleshooting.

Useful checks:

- `http://localhost:8070/api/version`
- `http://localhost:8070/api/isalive`
- `http://localhost:8070/api/health`
- `http://localhost:8071` for admin routes when exposed

## Logging

Useful log locations:

- Docker: `docker logs <container_name_or_id>`
- local service runs: `logs/grobid-service.log`

Capture logs before changing many variables at once.

## A safe rollout order

Use this progression:

1. one known-good PDF
2. a small stable batch
3. bounded retries with backoff
4. only then tune concurrency, image choice, and enrichment

This keeps setup mistakes, workload problems, and scaling problems from getting mixed together.

## Related pages

- [Docker Setup](./docker-setup)
- [Docker Troubleshooting](./docker-troubleshooting)
- [Performance Tuning](../performance-tuning)
- [Configuration Guide](../configuration)
