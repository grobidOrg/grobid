---
title: "Troubleshooting"
description: "Common errors and how to fix them"
---

# Troubleshooting

Use this page when GROBID starts failing and you need the fastest path from symptom to fix.

If you are using Docker, also check [Docker Troubleshooting](./docker/docker-troubleshooting). Many setup failures come from mounts, config overrides, shell syntax, or platform mismatches rather than from GROBID itself.

## Start with the symptom you see

| Symptom | Likely cause | Go to |
| --- | --- | --- |
| Container or service does not start | wrong config file, wrong mount, bad image/platform combination | [Startup failures](#startup-failures) |
| Service starts, but requests fail | bad input, timeout, memory pressure, corrupted PDF | [Request and processing failures](#request-and-processing-failures) |
| HTTP 503, timeout, or empty output | service unavailable, PDF parser timeout, unstable local setup | [HTTP and API failures](#http-and-api-failures) |
| Windows-specific native errors | native Windows support is fragile; Docker is the safer path | [Platform-specific failures](#platform-specific-failures) |
| Path, quoting, or unreadable file errors | invalid path, spaces, permissions, unreadable input | [Input and path problems](#input-and-path-problems) |

## Startup failures

### GROBID does not start after I launch it

Likely causes:

- your config override is invalid or incomplete
- you mounted the wrong host path into the container
- you replaced bundled resources by mounting over `grobid-home`
- the selected image does not match your platform

Fastest fix:

- If you are using Docker, go back to the [Docker Builder](./docker/docker-setup)
- Do not mount full `grobid-home` unless you intentionally seeded it from the image
- If consolidation is enabled, make sure the generated full `grobid.yaml` is saved before you run `docker run`
- Prefer `lfoppiano/grobid:latest-crf` unless you specifically need the full image

### Error mentions `pdfalto` path does not exist

This usually means you overrode the container's built-in `grobid-home` with an incomplete host directory.

Fix:

- remove the full `grobid-home` bind mount
- mount only the config file and optional PDF folder
- let the container use its bundled `pdfalto`, native libs, and models

If you need a custom config, mount only:

```powershell
-v C:\path\to\config\grobid.yaml:/opt/grobid/grobid-home/config/grobid.yaml:ro
```

### Error mentions `NullPointerException` during startup

Common causes:

- config file missing required fields
- invalid config file shape
- wrong file mounted where a file is expected

Fix:

- use the builder-generated full config, not a partial YAML fragment
- make sure the host-side `grobid.yaml` is a file, not a directory
- if you edited the YAML manually, compare it against the repo default config and keep unrelated sections intact

## Request and processing failures

### Service starts, but PDF processing fails

Likely causes:

- bad input format
- unreadable file or wrong permissions
- corrupted or pathological PDF
- memory or timeout pressure

Fix:

- verify the input file is readable by the process or container
- retry with a smaller or known-good PDF
- increase memory if the failure appears only on large files
- check whether the same request succeeds through Docker when local/native setup fails

### Empty output or unexpected partial output

Common explanations:

- the PDF is malformed or not extractable enough for the parser path
- the request endpoint or parameters are wrong for your expected output
- the file is being processed but times out or degrades on a problematic document

Fix:

- confirm you are using the right endpoint for the task
- test with a known-good input first
- check whether the failure is document-specific rather than installation-specific

## HTTP and API failures

### HTTP 503 or service unavailable

Likely causes:

- service not fully started yet
- unstable local/native setup, especially on Windows
- upstream parsing process crashed or timed out

Important: in GROBID, `503` often means the service is currently saturated and protecting itself from collapse. It does not automatically mean the server is broken.

Fix:

- wait for startup logs to finish before sending requests
- prefer Docker over native Windows execution
- retry on a small known-good PDF to distinguish startup failure from document-specific failure
- if you are sending many requests at once, reduce client concurrency and retry with backoff instead of treating `503` as a fatal error

### Request times out or large PDFs fail

Likely causes:

- PDF2XML/pdfalto timeout
- memory pressure
- too many threads or too much concurrency for the host

Fix:

- lower concurrency first
- increase available memory
- test with the CRF image before adding more variables
- if you are running locally, compare against the Docker path to eliminate native environment issues

If your client times out first, increase the client timeout as well. A `408` or local timeout can come from the client giving up before GROBID finishes.

### I do not understand the failure response from the API

Use:

- [REST API Usage](./api/rest-api-usage)
- [API Endpoints](../reference/api-endpoints)

Look for:

- wrong endpoint for the task
- missing or unreadable request input
- service not ready yet

Common input-related responses:

- `BAD_INPUT_DATA`: the PDF is damaged, unsupported enough to fail parsing, or otherwise unusable as input
- `NO_BLOCKS`: the PDF does not expose usable text blocks and may require OCR before GROBID can help

These often indicate an input problem rather than an installation problem.

## Input and path problems

### Paths with spaces or quoting issues break processing

This has shown up repeatedly in native and JNI-related setups.

Fix:

- avoid spaces in critical model or binary paths when possible
- if you are on Windows, prefer Docker instead of native setup
- if you stay on local/native setup, quote paths consistently and keep them simple

### Files exist, but GROBID behaves as if they do not

Likely causes:

- wrong working directory
- unreadable file permissions
- wrong bind mount path
- file mounted as a directory or vice versa

Fix:

- verify host path exists and is the expected type
- verify container mount target expects a file or directory correctly
- for Docker-specific mount errors, go to [Docker Troubleshooting](./docker/docker-troubleshooting)

If you are not using Docker, also verify that the process can actually read the files. Some failures that look like parser bugs are unreadable-file or permission problems.

## Platform-specific failures

### Windows-native setup is unstable or unsupported

This is one of the strongest patterns in the issue history.

Recommendation:

- use Docker on Windows instead of native execution
- treat PowerShell/CMD/WSL differences as command-entry differences, not container-runtime differences
- avoid native Windows Wapiti/pdfalto troubleshooting unless you have a strong reason not to use Docker

### Linux-specific native library or locale issues

Examples seen in issue triage:

- `GLIBC_*` mismatch
- locale-sensitive Wapiti model loading

Fix:

- prefer current container images over older native setups
- test locale-related fixes such as `LC_ALL=C` only if you are staying on local/native execution

### JEP or DeLFT fails with `CXXABI` / `libstdc++` errors

If you run deep-learning-backed models outside Docker, JEP initialization can fail because your Python environment expects a newer `libstdc++` than the system loader is finding.

Fix:

- prepend your Python environment's `lib` directory to `LD_LIBRARY_PATH`
- then retry the local/native run

Examples:

```bash
export LD_LIBRARY_PATH="${CONDA_PREFIX}/lib${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}"
```

```bash
export LD_LIBRARY_PATH="${VIRTUAL_ENV}/lib${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}"
```

If you are using the Docker full image instead, prefer validating that path first before debugging local linker behavior.

### macOS-specific issues

Most users should still prefer Docker. If you see native parser or model-loading failures locally, validate the same workflow in Docker before debugging the local environment further.

## If you are still blocked

Use this escalation order:

1. Reproduce with the [Docker Builder](./docker/docker-setup)
2. Check [Docker Troubleshooting](./docker/docker-troubleshooting)
3. Compare your request against [REST API Usage](./api/rest-api-usage)
4. Reduce variables: CRF image, no consolidation, small known-good PDF

That order usually separates setup errors from document-specific or API-specific failures quickly.

## Where to get logs

- Docker: `docker logs <container_name_or_id>`
- local service runs: `logs/grobid-service.log`

If you need to investigate further or report an issue later, capture the logs before changing too many variables.
