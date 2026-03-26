---
title: "Quick Start (Docker)"
description: "Run GROBID in Docker with the builder-first path that avoids the most common setup mistakes"
sidebar_position: 2
---

# Quick Start (Docker)

This is the fastest recommended path for most users.

Use the Docker Builder to generate a shell-specific command, create the right config file when needed, and avoid the most common setup problems.

If you just want the safest default path, use:

- image: `latest-crf`
- GPU: off
- consolidation: off
- admin port: on

## Before you start

You need:

- Docker Desktop on Windows or macOS, or Docker Engine on Linux
- Enough memory for the image you choose
- A place on disk for optional PDF input folders and config files

If you are on Windows, Docker Desktop runs Linux containers through WSL2 even if you type commands in PowerShell or CMD.

## 1. Open the Docker Builder

[Open the Docker Builder](../guides/docker/docker-setup)

The builder helps you choose:

- your operating system and shell
- the image variant (`latest-crf` or `latest-full`)
- whether you need PDF mounting
- whether you want consolidation through biblio-glutton or CrossRef

## 2. Choose the image variant

For most users, start with:

- `lfoppiano/grobid:latest-crf`

Use the full image only if you specifically want the deep-learning-enabled setup and accept the larger image size and narrower platform support.

Why `lfoppiano/grobid` here?

- the repository workflows publish Docker builds there first
- the repo also contains a separate retag/promotion workflow for `grobid/grobid`

For getting started, `lfoppiano/grobid:latest-crf` is the clearest default.

If you are unsure, do not optimize for maximum accuracy first. Optimize for a clean successful startup first.

## 3. If you enable consolidation, do Step 1 first

When you enable biblio-glutton or CrossRef in the builder, it generates a full `grobid.yaml` based on the real repo config.

Do this in order:

1. Copy the create-file command
2. Open the generated `grobid.yaml`
3. Save the generated config content
4. Then run the `docker run` command

If you skip the config-file step, Docker may mount the wrong thing and GROBID will fail to start.

## 4. Run the generated command

The generated command already accounts for:

- your selected shell syntax
- Windows path conversion when needed
- optional admin port exposure
- optional PDF input mounting
- optional config override mounting

After startup, you should see log lines indicating that the GROBID REST service is being initialized.

If the container exits immediately, do not keep changing many things at once. Go to [Docker Troubleshooting](../guides/docker/docker-troubleshooting) and reduce the setup back to the simple CRF path.

## 5. Verify that GROBID is running

Open these URLs in your browser:

- service: `http://localhost:8070`
- admin: `http://localhost:8071` (if enabled)

Also useful:

- version: `http://localhost:8070/api/version`
- liveness: `http://localhost:8070/api/isalive`
- readiness: `http://localhost:8070/api/health`

Then continue with:

- [REST API Usage](../guides/api/rest-api-usage)
- [API Endpoints](../reference/api-endpoints)

## Recommended first setup

If you just want a working local service:

- OS: your real host OS
- Image: `latest-crf`
- GPU: off
- Mount PDFs: on, only if you want batch-style local input folders
- Consolidation: off at first, unless you already know you need it
- Admin port: on

This keeps the setup simple and gives you the fewest moving parts.

Once that path works, add complexity only one step at a time:

- mount PDFs if you need them
- enable consolidation if you need enriched metadata
- switch images only if you really need the full variant

## If something goes wrong

Go straight to:

- [Troubleshooting](../guides/troubleshooting)
- [Docker Troubleshooting](../guides/docker/docker-troubleshooting)

Common failure causes include:

- mounting the wrong host path
- creating a config file in the wrong location
- trying to override too much of the container filesystem
- shell-specific quoting or line-continuation mistakes

If the service starts but your request fails, continue with [REST API Usage](../guides/api/rest-api-usage) first, then [Troubleshooting](../guides/troubleshooting).
