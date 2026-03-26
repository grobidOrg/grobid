---
title: "Training Overview"
description: "When and why to train custom models"
sidebar_position: 1
---

# Training Overview

Use this page when you need to retrain or evaluate GROBID models rather than just run the service.

## When training makes sense

Train custom models if you need to:

- improve behavior on a specific document genre
- adapt to a new layout style or language
- recover recurring extraction failures with targeted examples
- experiment with CRF vs deep-learning model choices

If the default models already work for your workload, do not start here.

## What can be trained

GROBID uses multiple task-specific models rather than one giant model.

Examples include:

- `affiliation-address`
- `date`
- `citation`
- `header`
- `name-citation`
- `name-header`
- `patent`
- `segmentation`
- `reference-segmenter`
- `fulltext`
- `figure`
- `table`
- `funding-acknowledgement`

Model files live under `grobid-home/models`.

## Where training data lives

Training data is organized under `grobid-trainer/resources/dataset/<MODEL>/`.

Typical layout:

- `corpus/` for training data
- `evaluation/` for held-out evaluation data when you manage the split manually

## The practical philosophy

The project historically favors:

- smaller but high-quality manually corrected data
- iterative improvement based on real failures
- holdout or end-to-end evaluation instead of trusting training-set validation alone

That makes training slower to bootstrap, but usually more trustworthy.

## Main training paths

The usual paths are:

- simple train-and-evaluate Gradle tasks such as `./gradlew train_header`
- more flexible trainer-jar commands for train-only, eval-only, split-and-eval, or n-fold runs
- automatic pre-annotation through `createTraining`, followed by manual correction

## Before you start

Make sure you have:

- a working local build
- the right dataset directory for the target model
- a clear evaluation plan before you overwrite any model outputs

## Recommended reading order

1. [Training Workflow](./training-workflow)
2. [Annotation Guidelines](./annotation-guidelines)
3. [Model Selection](./model-selection)
4. [Evaluation](./evaluation)
