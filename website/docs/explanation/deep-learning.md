---
title: "Deep Learning"
description: "DeLFT integration and model architecture"
sidebar_position: 3
---

# Deep Learning

GROBID can use DeLFT-based deep-learning models instead of the default CRF engine for selected tasks.

## What deep learning means in GROBID

Deep learning in GROBID is not an all-or-nothing mode.

In practice, you choose which individual models use:

- `wapiti` for CRF
- `delft` for deep learning

This mixed-engine setup is important because some tasks benefit more than others from the extra complexity.

## How it is integrated

The integration is done through JEP, which embeds Python from the JVM side so GROBID can call DeLFT-backed models directly.

That means local/native deep-learning setups are more sensitive than the Docker path because they depend on:

- Python environment layout
- native libraries
- linker behavior
- JEP compatibility

For most users, the full Docker image is the easiest way to use deep-learning-backed models.

## Where deep learning helps most

The project history and old docs consistently point to the strongest gains in tasks such as:

- citation parsing
- affiliation-address parsing
- reference-segmenter
- some header parsing scenarios
- funding and acknowledgement extraction

By contrast, the `fulltext` model remains a poor fit for current deep-learning sequence-labeling approaches because the input sequences are too large for the same strategy to work well.

## Why CRF still remains the default

Deep learning is not the default because the trade-off is real:

- more native/runtime complexity
- higher memory use
- often slower CPU-only inference
- more operational sensitivity

For large-scale production and commodity hardware, CRF is still the simpler and more scalable baseline.

## Recommended usage pattern

Use deep learning when:

- accuracy matters more than operational simplicity
- you know which task-specific model you want to improve
- you can validate the runtime impact on your real workload

Do not enable deep learning everywhere just because it sounds more advanced.

## Local/native caution

If you are not using Docker, expect the Python/JEP/native-library path to be the fragile part.

Typical local setup concerns include:

- JEP installation
- virtualenv or conda path selection
- `LD_LIBRARY_PATH` / `libstdc++` issues
- selecting the correct model engine and architecture in `grobid.yaml`

For practical model-selection advice, see [Model Selection](../guides/training/model-selection).

## Related pages

- [ML Pipeline](./ml-pipeline)
- [Model Selection](../guides/training/model-selection)
- [Training Workflow](../guides/training/training-workflow)
- [Troubleshooting](../guides/troubleshooting)
