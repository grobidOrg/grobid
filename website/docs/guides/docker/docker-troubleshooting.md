---
title: "Docker Troubleshooting"
description: "Common Docker errors and solutions"
sidebar_position: 5
---

# Docker Troubleshooting

Use this page when the Docker path fails before GROBID becomes usable.

This page assumes you are using the [Docker Builder](./docker-setup). If you are not, start there first.

## Fast diagnosis table

| Symptom | Likely cause | Fastest fix |
| --- | --- | --- |
| `not a directory` during startup | file mounted where directory expected, or directory mounted where file expected | verify file-vs-directory mount types |
| `pdfalto path does not exist` | full `grobid-home` mount replaced bundled resources | remove the full `grobid-home` bind mount |
| startup `NullPointerException` after config override | partial or invalid `grobid.yaml` | use the full generated config from the builder |
| command fails only on Windows | wrong shell syntax or wrong path format | select the correct shell in the builder |
| container starts but API unreachable | wrong port, port conflict, or port not exposed | verify `-p 8070:8070` and optional `8071` |
| full image does not run on ARM/macOS path | unsupported image/platform combination | use `latest-crf` or supported x86_64 environment |

## Container does not start

### Error says `not a directory`

This usually means Docker is mounting the wrong host type.

Examples:

- a file path is missing, so Docker effectively mounts a directory
- a directory is mounted to a path that expects a single file

Fix:

- verify the host-side path exists before running the command
- verify it is the expected type:
  - config override source must be a file
  - PDF input source must be a directory
- if you enabled consolidation, run Step 1 first and save the generated `grobid.yaml` before running Docker

### Error mentions `/opt/grobid/grobid-home/config/grobid.yaml` and mount failure

This usually means one of these:

- you mounted the wrong host path to the config target
- the host file does not exist yet
- you mounted a partial config file that breaks startup

Fix:

- use the exact generated config file location from the builder
- create the directory first
- save the generated full YAML, not only the consolidation subsection

If the host file does not exist yet, Docker may behave as if it is mounting the wrong type. Create the file before running the container.

## Config override problems

### I mounted `grobid.yaml`, but GROBID crashes on startup

Cause:

- the override replaced the full config, but your file is incomplete or malformed

Fix:

- use the builder-generated full config based on the real repo file
- do not handwrite a tiny YAML fragment like only `consolidation:` unless you also keep the rest of the config intact
- make sure the config matches the selected image variant

### I mounted full `grobid-home` and now bundled resources are missing

Cause:

- the image already contains models, `pdfalto`, and native libraries
- mounting your host directory over `/opt/grobid/grobid-home` replaces those bundled resources

Fix:

- do not mount full `grobid-home` in the normal path
- mount only:
  - optional PDF input directory
  - optional config override file

Only advanced users should override full `grobid-home`, and only after seeding it from the container image.

## Shell and path problems

### The generated command works in one shell but not another

Cause:

- PowerShell, CMD, and WSL Bash use different path expectations and line-continuation syntax

Fix:

- choose your real shell in the builder before copying the command
- PowerShell uses backticks for multiline commands
- WSL Bash expects `/mnt/...` style paths
- CMD and PowerShell use native Windows paths

### Windows paths or WSL paths are wrong

Fix:

- enter normal Windows paths if you are using PowerShell or CMD
- enter either Windows or WSL-style paths if the builder indicates auto-conversion
- if something still looks wrong, simplify the path and avoid spaces or unusual characters first

## Ports and reachability

### Container starts, but `localhost:8070` does not respond

Check:

- port `8070` is exposed in the generated command
- another service is not already using that port
- the container is still running and did not exit immediately after startup

Also check the logs:

```powershell
docker logs <container_name_or_id>
```

If needed, change the host port in the builder and retry.

### Admin endpoints are not reachable

If you want health and admin routes, expose `8071` as well.

Use the builder's admin-port option so the generated command stays consistent.

## Image and platform mismatches

### Full image fails or is the wrong choice

Use `latest-crf` unless you explicitly need the full image.

The full image is heavier and more constrained. If your goal is simply to run GROBID reliably, start with CRF first and add complexity only when needed.

### ARM or macOS path feels unreliable

If you are on Apple Silicon or ARM hardware, prefer the builder defaults and the CRF image path first.

Do not debug GPU or deep-learning variants until the simple path works.

## Consolidation problems

### Biblio-glutton or CrossRef is enabled, but startup breaks

Check:

- the config file exists
- the config file is a full generated YAML
- the URL or email value is filled correctly
- the config file path is mounted as a file, not a directory

For biblio-glutton:

- verify the URL is reachable from the container
- if you use `host.docker.internal`, make sure you actually have the service running on the host

For CrossRef:

- email is optional, but recommended if you want the polite pool behavior

## Memory and throughput problems

### Container is killed, unstable, or large PDFs fail under load

Likely causes:

- too little memory for the selected image and workload
- too much client-side concurrency
- using the full image where the lightweight CRF image would be enough

Fix:

- start with the CRF image if your goal is reliability and speed
- reduce parallel client requests first
- increase Docker memory allocation if your Docker environment is constrained
- retry with a known-good small PDF before concluding the service is broken

### I see many `503` responses while sending many requests

This usually means GROBID is saturated, not dead.

Fix:

- back off and retry instead of treating `503` as a fatal failure
- reduce client concurrency
- tune request rate deliberately instead of just increasing parallelism

## Safe recovery path

If you are stuck, reduce the setup to this:

- image: `latest-crf`
- no consolidation
- no custom config override
- optional PDF mount only
- `8070` exposed, optionally `8071`

Once that works, add one variable at a time.
