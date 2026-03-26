---
title: "Annotation Guidelines"
description: "How to annotate training data"
sidebar_position: 2
---

# Annotation Guidelines

Use this page when you are preparing or correcting GROBID training data.

## Core principle

Annotation quality matters more than annotation volume.

In practice, GROBID training works best when examples are:

- consistent
- manually reviewed
- chosen to cover real failure cases
- kept aligned with the actual model task

## Do not annotate only the labels you personally care about

For a given model, all labels in that task need coherent annotation.

Selective annotation usually hurts the labels you thought you were optimizing, because the model loses the surrounding context that helps it separate neighboring structures.

## Use pre-annotation as a starting point, not gold data

The practical workflow is usually:

1. generate training TEI from PDFs
2. open the generated files in an annotation or correction tool
3. manually correct the result
4. move trusted files into the model dataset directories

The older docs specifically point to `pdf-tei-editor` as a useful web-based correction tool for this workflow.

Typical generated files include model-specific TEI files such as:

- `*.training.segmentation.tei.xml`
- `*.training.header.tei.xml`
- `*.training.fulltext.tei.xml`
- `*.training.references.tei.xml`

Layout-aware tasks also produce companion feature files without the `.tei.xml` suffix. Those feature files are necessary for training, but they are not the files you manually edit as annotation targets.

## Keep the text stream untouched

When correcting pre-annotated files, the most important rule is:

- move tags if needed
- do not rewrite the extracted text itself

That includes keeping noisy PDF text, OCR artifacts, and unexpected Unicode as-is when they came from the extraction stream.

Practical exception:

- XML whitespace can be reformatted for readability
- `<lb/>` should still remain aligned with the original text flow

## Modify or delete generated files, but do not invent missing ones casually

If a pre-generated training fragment is clearly wrong, fix or remove it.

If a specialized generated file is missing because the upstream detection failed, do not casually invent a new parallel training file by hand. Correct the material through the intended workflow and keep the dataset organization coherent.

## Keep training and evaluation discipline

When possible:

- keep held-out evaluation files separate
- add new examples because they expose real failures, not just because they are easy to annotate
- prefer iterative learning curves and error-driven corpus growth over blind accumulation

## Naming and organization

Training data is organized by model under `grobid-trainer/resources/dataset/<MODEL>/`.

Be careful to place files under the correct model directory. A common mistake is to prepare examples for one task and then train a different model name.

## Language and domain adaptation

GROBID can be adapted beyond mainstream English scholarly articles, but do it iteratively.

Typical order:

- segmentation
- header
- fulltext
- references and related subparsers

## Related pages

- [Training Overview](./training-overview)
- [Training Workflow](./training-workflow)
- [Evaluation](./evaluation)
