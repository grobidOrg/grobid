---
title: "Architecture"
description: "How GROBID works internally"
sidebar_position: 1
---

# Architecture

GROBID converts noisy scholarly PDFs into structured TEI XML using a staged pipeline rather than a single monolithic parser.

## The core idea

GROBID treats document extraction as a structured prediction problem.

Instead of trying to infer an entire document in one pass, it breaks the job into smaller steps:

- recover layout-aware tokens from the PDF
- segment the document into major zones
- run specialized models on those zones
- assemble the results into one logical TEI document

This makes the system easier to train, easier to tune, and easier to adapt to different document structures.

## Why GROBID works on PDFs at all

At large scale, scientific content is often available only as PDF. Even when publisher XML exists, it can be inconsistent across publishers and difficult to normalize for shared downstream processing.

GROBID exists to produce one predictable structured representation from noisy PDF input. In related workflows, publisher XML can be normalized through companion tooling such as Pub2TEI so that PDF-derived and XML-derived content can converge on a similar TEI model.

## The pipeline shape

At a high level, GROBID works like this:

1. parse the PDF into layout-aware tokens
2. detect broad document zones such as header, body, references, notes, and figures
3. send those zones to narrower task-specific models
4. merge the outputs into TEI XML

That lets GROBID use different model assumptions for different structures instead of forcing one model to solve everything.

## Why there are multiple models

Different document structures behave differently.

Examples:

- header metadata has different cues than bibliographic references
- author names in a header look different from author names inside citation strings
- a whole-document segmentation task benefits heavily from layout information
- a narrow date or affiliation parser can often work with simpler inputs

That is why GROBID uses several specialized models rather than one general model.

## Context-free models, context-aware pipeline

Individual models are generally reused as context-free components, but the pipeline gives them context by deciding where they are applied.

For example:

- the segmentation model identifies the header area
- the header parser runs only on that extracted area
- a name parser can be used differently for header names and citation names

This combination keeps each model relatively small while still producing a rich final structure.

## Error propagation and recovery

Because GROBID is a cascade, upstream mistakes can affect downstream steps.

That is a trade-off, but the modular design also makes the system more recoverable:

- downstream models can be trained on realistic noisy inputs
- individual stages can be improved without rebuilding the whole system
- different models can be swapped between CRF and deep-learning engines depending on the task

## Coordinates are a first-class outcome

GROBID keeps structured output aligned with PDF geometry so that extracted structures can point back to the source document.

That enables:

- annotation overlays
- clickable citations and references
- figure/table highlighting
- PDF + structured-data interfaces

See [Coordinates](../reference/coordinates) for the output details.

## Related pages

- [ML Pipeline](./ml-pipeline)
- [Design Principles](./design-principles)
- [Deep Learning](./deep-learning)
