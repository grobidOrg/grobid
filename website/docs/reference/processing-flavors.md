---
title: "Processing Flavors"
description: "Available model configurations"
sidebar_position: 5
---

# Processing Flavors

Use this page when you need to understand what the `flavor` parameter does and which alternative processing variants exist.

Processing flavors are not just output presets. They change which model variants are selected for parts of the processing cascade.

## What a flavor is

A flavor is a named alternative processing path used when the standard scholarly-article model stack is not the best fit.

In practice, a flavor can provide alternative models for stages such as:

- header parsing
- segmentation
- fulltext processing

If a flavor-specific model does not exist for a stage, GROBID falls back to the standard model for that stage.

## How to use a flavor

For REST API requests, pass `flavor` as a request parameter.

Example:

```bash
curl -v -H "Accept: application/xml" --form input=@./thefile.pdf --form flavor=article/light-ref localhost:8070/api/processFulltextDocument
```

## Where flavor is supported

The repo shows user-facing flavor support on:

- `/api/processFulltextDocument`
- `/api/processFulltextAssetDocument`
- `/api/createTraining`

The most common practical entry point is still `/api/processFulltextDocument`.

## Supported flavor labels

Code-level accepted labels include:

- `blank`
- `article/light`
- `article/light-ref`
- `sdo/ietf`
- `sdo/3gpp`

However, not all flavors are equally documented or equally backed by visible training/evaluation material in this repository.

## The most relevant flavors

### `article/light`

Purpose:

- simplified processing for article-like documents where the standard scholarly-article structure may be a poor fit

Typical behavior:

- simplified header extraction
- body represented in a simpler way
- less structural ambition than the standard path

Good fit for:

- corrections
- editorials
- letters
- article-adjacent documents that do not behave like normal full papers

Trade-off:

- simpler structure can be more robust on these documents, but you lose some richness compared with the standard path

### `article/light-ref`

Purpose:

- similar to `article/light`, but with reference-related handling included

Good fit for:

- article-like variants that still contain bibliographic references

Think of it as the more reference-aware version of `article/light`.

### `sdo/ietf`

Purpose:

- specialized processing for IETF standards-style documents

Good fit for:

- documents following a standards/specification template rather than a normal scholarly-article template

This is one of the more clearly evidenced specialized non-article flavors in the repository.

### `blank`

This one is special.

`blank` is not a normal structured flavor in the same sense as the others. It short-circuits the usual fulltext structuring path and produces a much more minimal TEI body with raw tokenized text content.

Use it when:

- you want a minimal raw-text-oriented TEI shell
- you explicitly do not want the normal structured fulltext cascade

Do not expect it to behave like a simple "lighter standard fulltext mode". It is conceptually different.

## Flavors that are less clearly documented

### `sdo/3gpp`

This label is accepted by code, but the repository provides less visible user-facing documentation and training/evaluation evidence for it than for `article/light`, `article/light-ref`, or `sdo/ietf`.

That means:

- it exists as a supported label in code
- you should still validate behavior carefully on your own corpus before relying on it heavily

## What flavors change in practice

Flavors can change:

- which models are loaded for certain stages
- how aggressively structure is inferred
- how well non-standard document genres are handled
- the shape and richness of the result

They do **not** simply reformat the same underlying output.

## When to try a flavor

Try a flavor when:

- the standard fulltext path is a poor fit for your document genre
- you are processing article-adjacent documents rather than standard papers
- you are working with standards/specification documents

Do **not** start with flavors unless you already know the default path is not giving you the right shape of result.

## Suggested decision guide

| Situation | Try |
| --- | --- |
| Standard scholarly article | default processing first |
| Editorial, correction, letter, atypical article variant | `article/light` |
| Atypical article variant with references still important | `article/light-ref` |
| IETF standards/spec documents | `sdo/ietf` |
| Raw-text-like minimal TEI body needed | `blank` |

## Training and evaluation note

This repository contains clearer training/evaluation evidence for:

- `article/light`
- `article/light-ref`
- `sdo/ietf`

That does not mean other flavors are impossible. It means the strongest visible evidence in this codebase currently clusters around those variants.

## Important caution

Because flavors alter model selection, you should validate them on your own target document set.

Do not assume that a flavor is "better" in general. A flavor is better only when its assumptions match your document genre.

## Related pages

- [REST API Usage](../guides/api/rest-api-usage)
- [API Endpoints](./api-endpoints)
- [Understanding the Output](../getting-started/understanding-the-output)
