---
title: "GPU Setup"
description: "Enable NVIDIA GPU acceleration in Docker"
sidebar_position: 2
---

# GPU Setup

Use this page when you are deciding whether GPU matters for your GROBID Docker setup and what safe expectations to have before you start tuning.

## Short answer

Most users do **not** need GPU first.

If your goal is simply to get GROBID working reliably, start with:

- `latest-crf`
- CPU only
- one known-good PDF

Only think about GPU after the simple path is already stable.

## When GPU is useful

GPU is mainly relevant when you are using the full image and want the deep-learning-enabled path.

That means GPU is more relevant when:

- you intentionally chose `latest-full`
- you care about workloads where the deep-learning-backed models matter
- you are running on hardware and a platform where GPU-backed containers are realistically usable

## When GPU is not the main lever

GPU is usually **not** the first thing to optimize when:

- you are using `latest-crf`
- your workload is small or exploratory
- your real bottleneck is PDF parsing, I/O, or configuration
- consolidation latency is dominating request time

In other words: GPU does not fix every kind of slowness.

## Image choice matters first

### `latest-crf`

This is the safe default and the best first path for most users.

For this image, GPU is usually not the point of the setup.

### `latest-full`

This is the image where GPU starts to matter more.

Use it only when:

- you know why you need the deeper model stack
- you are ready for higher resource usage and more operational complexity

Do not switch to the full image only because it sounds more advanced.

## Platform expectations

### Linux

This is the most realistic place to expect useful GPU-backed Docker behavior.

If you are serious about GPU-backed GROBID usage, Linux is the best baseline platform.

### Windows and macOS

Be much more conservative in your expectations.

Even if your host machine has capable hardware, Docker GPU behavior is not the first thing you should rely on when the basic CPU path is not yet proven.

Practical rule:

- prove the CPU path first
- then test the full image
- only then spend time on GPU-specific debugging

## A safer GPU decision table

| Situation | Recommendation |
| --- | --- |
| I just want GROBID working | Ignore GPU, use `latest-crf` |
| I am on CPU-only hardware | Stay on CRF first |
| I want better accuracy and have a strong Linux GPU setup | Consider `latest-full` with GPU |
| I am unsure whether GPU helps my workload | Validate CPU first, then compare |

## What the Docker Builder already does

When GPU is enabled in the builder for the full image, it also adds:

```text
TF_FORCE_GPU_ALLOW_GROWTH=true
```

Why:

- TensorFlow otherwise tends to grab all available GPU memory eagerly
- allowing growth makes the setup friendlier to constrained or shared GPU environments

This does not make the setup magically correct, but it avoids one common source of avoidable GPU pain.

## What to expect at startup

Do not expect the full image to load all models into GPU memory.

`latest-full` is a mixed setup:

- some models use DeLFT/TensorFlow and can benefit from GPU
- many models still use Wapiti CRF and remain CPU-bound
- in particular, `segmentation` and `fulltext` remain CRF-based

That means all of the following can be true at the same time:

- the full image is configured correctly
- TensorFlow sees your GPU
- startup VRAM usage stays relatively low
- large parts of the overall extraction pipeline still run on CPU

If `TF_FORCE_GPU_ALLOW_GROWTH=true` is enabled, TensorFlow usually allocates GPU memory gradually instead of reserving most VRAM at startup. So modest idle GPU memory after container start does **not** by itself indicate CPU fallback.

Also note that TensorFlow may detect multiple GPUs even if GROBID does not effectively spread inference across all of them.

## How to verify that GPU is actually being used

If you are on Linux and expect GPU-backed execution, verify it at the host level instead of assuming Docker picked it up correctly.

Practical options include:

- `nvidia-smi`
- `nvtop`

To make this check meaningful, watch GPU activity during a request that exercises DeLFT-backed models such as:

- `header`
- `citation`
- `reference-segmenter`
- `affiliation-address`
- `funding-acknowledgement`

Do **not** use `fulltext` alone as proof that GPU is or is not working, because `fulltext` remains CRF-based even in the full image.

If GPU usage is not visible there, treat it as a host/container GPU setup problem first.

## Common mistakes

### Enabling GPU while still using the CRF path

This usually adds complexity without giving you the benefit you think you are getting.

### Treating GPU as the first debugging variable

If the service does not even start or process one known-good PDF correctly, GPU is not your first problem.

### Assuming full image + GPU is always the best choice

The full image is heavier and more sensitive operationally. Sometimes the simpler CRF path is the right production decision.

### Blaming GROBID for host/container GPU setup problems

Sometimes the real issue is:

- Docker GPU exposure
- host driver setup
- platform limitations

Validate the underlying container GPU path before assuming the issue is inside GROBID itself.

## Recommended adoption path

Use this order:

1. get a clean CPU-only CRF setup working
2. validate one successful request
3. if needed, switch to the full image
4. if still needed, validate GPU-backed behavior on your real workload
5. only then tune performance aggressively

This keeps the debugging surface small.

## Related pages

- [Docker Setup](./docker-setup)
- [Platform Guide](./docker-platforms)
- [Performance Tuning](../performance-tuning)
- [Docker Troubleshooting](./docker-troubleshooting)
