---
title: "Design Principles"
description: "Architecture decisions and philosophy"
sidebar_position: 5
---

# Design Principles

This page explains the design choices that shape GROBID's defaults.

## Quality over quantity in training data

GROBID has historically favored smaller, higher-quality manually corrected training sets over very large automatically aligned datasets.

Why:

- PDF and publisher XML are often hard to align cleanly at full-document scale
- noisy auto-aligned data can make models less trustworthy
- adding a few targeted error cases to a compact dataset is often more useful than diluting them into massive weakly aligned corpora

This does not mean large datasets are useless. It means the project optimizes for targeted iteration and controllable annotation quality.

## Holdout evaluation over self-congratulatory validation

GROBID puts significant weight on end-to-end holdout evaluation against realistic PDF/XML pairs.

Why:

- cross-validation on hand-crafted training data can overstate real-world performance
- end-to-end evaluation includes PDF noise, parser failures, and error propagation across the full pipeline
- realistic holdout benchmarks are better for tracking regressions over time

## Layout matters, not just text

GROBID works on layout-aware tokens rather than plain text strings.

That lets the system use:

- font and style information
- line and block structure
- indentation and spacing
- geometric positions and bounding boxes

This is a major reason GROBID can recover document structure from PDFs more reliably than text-only approaches.

## Default for scalability, optional path for higher accuracy

The default operational posture is conservative:

- CRF first
- CPU-friendly defaults
- predictable throughput
- optional selective deep-learning upgrades

This reflects the project's real deployment goal: process large scholarly corpora without making GPU-heavy complexity mandatory.

## One structured target format

GROBID standardizes on TEI XML as the main output model because scholarly documents are richer than flat metadata records.

That allows one representation to capture:

- metadata
- body structure
- citations and references
- notes, figures, tables, and coordinates

## Progress through iteration

The project is designed to improve incrementally:

- identify real failure cases
- add targeted training examples
- rerun evaluation
- measure regressions and improvements on stable datasets

This is more sustainable than repeatedly redesigning the whole pipeline.

## Related pages

- [Architecture](./architecture)
- [ML Pipeline](./ml-pipeline)
- [Why TEI XML?](./tei-rationale)
- [Evaluation](../guides/training/evaluation)
