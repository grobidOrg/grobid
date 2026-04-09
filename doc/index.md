# GROBID Documentation


## Getting Started

New to GROBID? Start here to get up and running quickly.

* [Quick start](getting_started.md) — install and launch GROBID in minutes

* [Run with Docker](Grobid-docker.md) — the easiest way to deploy GROBID

* [Troubleshooting and FAQ](Frequently-asked-questions.md) — common issues and solutions

## Upgrading

* [Upgrade guide](Upgrading.md) — what to know when moving between major GROBID versions

## User Guide

Everything you need to use GROBID once it's running.

* [Using the REST API](Grobid-service.md) — endpoints, parameters, and client libraries

* [Understanding the output (TEI)](TEI-encoding-of-results.md) — structure of the TEI XML results

* [PDF coordinates](Coordinates-in-PDF.md) — extracting bounding boxes for structures in the original PDF

* [Configuration](Configuration.md) — tuning GROBID for your use case

* [Consolidation service](Consolidation.md) — linking extracted references to external metadata

* [Specialized processes](Grobid-specialized-processes.md) — patents, medical, and other domain-specific workflows

## About

* [Introduction](Introduction.md) — what GROBID is and what it does

* [How GROBID works](Principles.md) — architecture and processing pipeline

* [Benchmarks](benchmarks/Benchmarking.md) — evaluation methodology and overview of results

* [References](References.md) — publications about GROBID

* [License](License.md)

## Developer Guide

Building, training, and extending GROBID.

* [Build from source](Install-Grobid.md) — set up a development environment

* [Training and evaluating models](Training-the-models-of-Grobid.md) — retrain or fine-tune GROBID models

* [End-to-end evaluation](End-to-end-evaluation.md) — evaluate full pipeline performance

* [Deep Learning models](Deep-Learning-models.md) — using DL models instead of default CRF

* [Developer notes](Notes-grobid-developers.md) — internal conventions and tips for contributors

* [Recompiling CRF libraries](Recompiling-and-integrating-CRF-libraries.md) — rebuilding native CRF dependencies

## Annotation Guidelines

Guidelines for annotating training data.

* [General principles](training/General-principles.md)

* [Segmentation model](training/segmentation.md)

* [Fulltext model](training/fulltext.md)

* [Header model](training/header.md)

* [Bibliographical references](training/Bibliographical-references.md)

* [Affiliation-address model](training/affiliation-address.md)

* [Date model](training/date.md)

## Benchmarking

Detailed evaluation results on specific datasets.

* [PubMed Central](benchmarks/Benchmarking-pmc.md)

* [bioRxiv](benchmarks/Benchmarking-biorxiv.md)

* [PLOS](benchmarks/Benchmarking-plos.md)

* [eLife](benchmarks/Benchmarking-elife.md)

* [Model comparison](benchmarks/Benchmarking-models.md)

## Archive

Deprecated features kept for reference.

* [Batch mode (deprecated)](Grobid-batch.md)

* [Java library (deprecated)](Grobid-java-library.md)
