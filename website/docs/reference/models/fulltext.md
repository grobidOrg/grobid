---
title: "Fulltext Model"
description: "Fulltext body labels and tags"
sidebar_position: 3
---

# Fulltext Model

This model structures the main body of the article after segmentation has already identified the body zone.

## Typical structures recognized

- paragraphs
- section titles
- figures
- tables
- formulas
- lists and list items
- inline callouts to references, figures, tables, formulas, and boxed content

## Main annotation concepts

- `<p>` for paragraph text
- `<head>` for section titles
- `<figure>` for figures
- `<figure type="table">` for tables at this stage
- `<figure type="box">` for boxed content
- `<formula>` for stand-alone formulas
- `<list>` and `<item>` for list structure
- `<ref type="...">` for inline callouts

## Important structural rules

- formulas are block-level structures, not paragraph children in the final logical model
- list items should live inside `<list>`, not directly inside `<p>`
- figure and table markers in the body are inline references, not the figures themselves
- short inline formulas are usually treated as part of surrounding text rather than as separate formula blocks

## Practical annotation rule

Do not change the text flow. You can reformat whitespace for readability, but the extracted content and `<lb/>` stream still need to reflect the PDF-derived sequence.

## Related pages

- [Segmentation Model](./segmentation)
- [TEI Output Format](../tei-output-format)
