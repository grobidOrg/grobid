---
title: "Platform Guide"
description: "macOS, Windows WSL2, Linux, and ARM64"
sidebar_position: 3
---

# Platform Guide

Use this page when you need a practical answer to: "What is the safest GROBID Docker path on my platform?"

This guide is intentionally qualitative. It does not rely on stale benchmark numbers. The goal is to help you choose a setup that is likely to work well before you start tuning.

## Image repositories and tags

Two Docker Hub repositories matter here:

- `lfoppiano/grobid`
- `grobid/grobid`

Repository workflows in this codebase publish builds to `lfoppiano/grobid` first, and the repo also contains a separate retag/promotion workflow for `grobid/grobid`.

Practical guidance:

- for current docs and quickstart usage, prefer `lfoppiano/grobid`
- treat tags like `latest-crf`, `latest-full`, and `latest-develop` as moving convenience tags
- use `latest-crf` as the default onboarding path unless you already know you need something else

## Short version

| Platform | Best first choice | Notes |
| --- | --- | --- |
| Linux x86_64 | `latest-crf` | Cleanest and most predictable starting point |
| Windows | Docker Desktop + WSL2 + `latest-crf` | Preferred over native Windows execution |
| macOS Intel | `latest-crf` | Safe first path |
| macOS Apple Silicon | `latest-crf` first | Be conservative with full image expectations |
| Linux ARM64 | proceed carefully | Prefer validated paths and simple CRF-first testing |

## The safest default across platforms

If you do not have a strong reason to do otherwise, start with:

- `lfoppiano/grobid:latest-crf`
- no consolidation at first
- admin port enabled
- one known-good PDF

This keeps the number of variables low and avoids mixing platform issues with image complexity.

## Linux x86_64

This is the cleanest and most predictable Docker path.

Why:

- container behavior is closest to the primary deployment assumptions
- both general Docker usage and GPU-backed workflows are more straightforward here
- it is the best platform to validate more advanced setups later

If you are doing serious operational tuning, Linux x86_64 is the best baseline.

## Windows

### Recommended path

Use Docker Desktop with WSL2 backend.

Do **not** treat native Windows execution as the primary route.

Why:

- native Windows support has historically been high-friction
- Docker avoids many native parser/library/runtime issues
- most users just want the service exposed via the REST API anyway

### Practical expectation

- you can type the command in PowerShell, CMD, or WSL Bash
- the container runtime still lives in the Docker/WSL2 environment
- shell choice mostly affects path syntax and line continuation, not the container's logical runtime model

### Best first setup on Windows

- `latest-crf`
- no consolidation initially
- verify `http://localhost:8070`
- only add config overrides after the simple path works

## macOS Intel

This is generally a reasonable Docker path for getting started.

Best advice:

- start with `latest-crf`
- keep the setup simple first
- add complexity only after proving the basic flow works

If you hit memory pressure in Docker Desktop, increase Docker's available memory before changing many GROBID settings.

## macOS Apple Silicon

Apple Silicon needs extra caution.

Practical guidance:

- start with the CRF path first
- do not assume the full image path is the right first move
- validate one clean working extraction before trying to optimize anything

This is a platform where reducing variables early matters a lot.

## Linux ARM64

Treat this as a path that may need more validation and more conservative decisions.

Practical guidance:

- start simple
- prefer the CRF-first path
- avoid treating full-image behavior as the default expectation

If your environment is ARM-based and your goal is just reliable extraction, simplicity matters more than chasing the theoretically richest image path first.

## CRF image vs full image by platform

### `latest-crf`

Better first choice when you want:

- lower operational complexity
- CPU-friendly behavior
- simpler debugging
- broader "just get it running" compatibility

### `latest-full`

Better only when you explicitly want the deeper model stack and understand the trade-offs:

- heavier resource usage
- more operational sensitivity
- more risk on constrained or unusual environments

Use the full image because you need it, not because it sounds more advanced.

## GPU expectations

Do not assume GPU use is equally realistic on every platform.

Practical rule:

- if your goal is a dependable first deployment, ignore GPU first
- validate the CPU path first
- only then decide whether GPU-backed tuning is worth the complexity

For most users, GPU is not the first problem to solve.

## Platform-specific failure patterns to watch for

### Windows

- wrong shell syntax
- wrong path format
- trying native Windows instead of Docker

### macOS

- memory pressure inside Docker Desktop
- ARM/Apple Silicon expectations that are too optimistic too early

### Linux

- local/native library or locale issues if you step outside Docker
- over-tuning concurrency before validating the baseline

## When to switch platforms mentally

If you are spending too much time debugging the platform itself instead of using GROBID, the best move is often:

- go back to Docker
- use `latest-crf`
- remove config overrides
- test one known-good PDF

This is especially true on Windows and on more unusual hardware environments.

## If you need the next level of detail

- [Docker Setup](./docker-setup)
- [Docker Troubleshooting](./docker-troubleshooting)
- [Performance Tuning](../performance-tuning)

Those pages explain how to move from “best platform default” to “my workload is stable and now I want to optimize it.”
