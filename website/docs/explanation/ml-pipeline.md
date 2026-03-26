---
title: "ML Pipeline"
description: "CRF and Deep Learning model cascade"
sidebar_position: 2
---

# ML Pipeline

GROBID uses a cascade of sequence-labeling models. Some stages run well with CRF, some benefit from deep learning, and the full system can mix both.

## Pipeline overview

The pipeline is modular.

Typical stages include:

- document segmentation
- header parsing
- citation and reference-segment parsing
- affiliation parsing
- name parsing
- date parsing
- figure and table structuring
- funding and acknowledgement extraction

These stages do not all operate on the same kind of input. Some work on line-level sequences, others on token-level sequences, and some are reused in multiple places.

## Why a cascade instead of one giant model

Sequence labeling works on linear input. Documents are hierarchical.

GROBID bridges that gap by chaining multiple sequence-labeling steps together:

- broad models identify major regions first
- narrower models then structure those regions more precisely

This keeps label spaces smaller and training data more manageable while still producing a detailed final document structure.

## Example: segmentation to header parsing

One useful mental model is:

1. the segmentation model finds the header zone
2. the header model parses title, authors, affiliations, abstract, and related fields inside that zone
3. subparsers then normalize pieces such as names, affiliations, or dates where needed

The same pattern applies elsewhere in the cascade.

## Line-level vs token-level tasks

Different pipeline stages use different granularities.

- segmentation benefits from line-level processing because it runs over the whole document and relies heavily on layout cues
- header and citation parsing often operate at token level because they need more detailed labeling inside smaller regions

This is part of why the cascade remains efficient enough for large-scale use.

## CRF and deep learning can be mixed

GROBID does not force one engine everywhere.

In practice:

- CRF remains the default because it is fast, scalable, and inexpensive operationally
- deep-learning models can be enabled selectively for tasks where they provide a meaningful accuracy gain

This mixed-engine design is one of the most important practical properties of the system.

## The current trade-off

The pipeline is designed around balancing:

- accuracy
- throughput
- memory usage
- operational simplicity

That is why some tasks still default to CRF even when deep-learning alternatives exist. The best engine is task-dependent, not ideology-dependent.

## Related pages

- [Architecture](./architecture)
- [Design Principles](./design-principles)
- [Deep Learning](./deep-learning)
- [Model Selection](../guides/training/model-selection)
