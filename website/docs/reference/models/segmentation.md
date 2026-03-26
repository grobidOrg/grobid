---
title: "Segmentation Model"
description: "Document segmentation labels and tags"
sidebar_position: 1
---

# Segmentation Model

This model splits the document into its major zones before narrower parsers run.

## Main structures recognized

- `<titlePage>` for publisher cover pages
- `<front>` for front-matter/header regions
- `<body>` for the main article body
- `<listBibl>` for the bibliography section
- `<page>` for page numbers
- `<note place="headnote">` and `<note place="footnote">` for page header/footer style material
- annex-style `<div type="...">` sections such as acknowledgment, availability, conflict, contribution, funding, toc, and generic annex

## Important boundary rules

- figures and tables are considered part of the body at segmentation level
- footnotes referenced from the body stay outside header/front material even if they appear on page one
- bibliographic and article-front information can appear outside the visual top of page one and still belong under `<front>`
- appendix-like material after the body should become annex-style divisions rather than body text

## Practical edge cases

- a publisher-added cover page can be fully enclosed in `<titlePage>`
- articles can start mid-page after preceding unrelated material; the earlier article fragment should be ignored entirely
- hidden text extracted from the PDF but not actually visible should usually remain untagged so the model learns to ignore it

## Related pages

- [Header Model](./header)
- [Fulltext Model](./fulltext)
