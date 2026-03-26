---
title: "References"
description: "Academic citations and related projects"
sidebar_position: 3
---

# References

Use this page for software citation, evaluation-related references, datasets, and nearby open-source tools.

## Cite the project

If you want to cite GROBID, cite the project itself rather than a particular person.

Project reference:

```text
GROBID (2008-2025) <https://github.com/kermitt2/grobid>
```

BibTeX using the Software Heritage project-level identifier:

```bibtex
@misc{GROBID,
    title = {GROBID},
    howpublished = {\url{https://github.com/kermitt2/grobid}},
    publisher = {GitHub},
    year = {2008--2025},
    archivePrefix = {swh},
    eprint = {1:dir:dab86b296e3c3216e2241968f0d63b68e8209d3c}
}
```

## Evaluation and usage references

These references are provided as useful context around GROBID evaluation and usage. They do not define the current capabilities of the project.

- M. Lipinski, K. Yao, C. Breitinger, J. Beel, and B. Gipp. 2013. [Evaluation of Header Metadata Extraction Approaches and Tools for Scientific PDF Documents](http://docear.org/papers/Evaluation_of_Header_Metadata_Extraction_Approaches_and_Tools_for_Scientific_PDF_Documents.pdf).
- Joseph Boyd. 2015. [Automatic Metadata Extraction The High Energy Physics Use Case](https://preprints.cern.ch/record/2039361/files/CERN-THESIS-2015-105.pdf).
- Phil Gooch and Kris Jack. 2015. [How well does Mendeley’s Metadata Extraction Work?](https://krisjack.wordpress.com/2015/03/12/how-well-does-mendeleys-metadata-extraction-work/).
- [Meta-eval](https://github.com/allenai/meta-eval). 2015.
- D. Tkaczyk, A. Collins, P. Sheridan, and J. Beel. 2018. [Evaluation and Comparison of Open Source Bibliographic Reference Parsers: A Business Use Case](https://arxiv.org/abs/1802.01168).
- Kyle Lo, Lucy Lu Wang, Mark Neumann, Rodney Kinney and Dan S. Weld. 2019. [S2ORC: The Semantic Scholar Open Research Corpus](https://arxiv.org/abs/1911.02782).
- [CORD-19: The COVID-19 Open Research Dataset](https://arxiv.org/abs/2004.10706). 2020.
- Mark Grennan and Joeran Beel. 2020. [Synthetic vs. Real Reference Strings for Citation Parsing, and the Importance of Re-training and Out-Of-Sample Data for Meaningful Evaluations](https://arxiv.org/abs/2004.10410).
- J.M. Nicholson, M. Mordaunt, P. Lopez, A. Uppala, D. Rosati, N.P. Rodrigues, P. Grabitz, and S.C. Rife. 2021. [scite: a smart citation index that displays the context of citations and classifies their intent using deep learning](https://doi.org/10.1101/2021.03.15.435418).
- P. Lopez, C. Du, J. Cohoon, K. Ram, and J. Howison. 2021. [Mining Software Entities in Scientific Literature](https://doi.org/10.1145/3459637.3481936).

## Foundational reference-parsing papers

- Fuchun Peng and Andrew McCallum. [Accurate Information Extraction from Research Papers using Conditional Random Fields](https://www.aclweb.org/anthology/N04-1042.pdf). 2004.
- Isaac G. Councill, C. Lee Giles, and Min-Yen Kan. [ParsCit: An open-source CRF reference string parsing package](http://www.lrec-conf.org/proceedings/lrec2008/pdf/166_paper.pdf). 2008.

## Datasets

For end-to-end evaluation, GROBID makes available corpora of PDF/XML pairs at [https://zenodo.org/record/7708580](https://zenodo.org/record/7708580).

This includes:

- `PMC_sample_1943`
- an updated version of [bioRxiv 10k](https://zenodo.org/record/3873702)
- PLOS (1000 articles)
- eLife (984 articles)

See the evaluation documentation for methodology details.

For layout and zoning identification:

- [GROTOAP2](https://repod.icm.edu.pl/dataset.xhtml?persistentId=doi:10.18150/8527338)
- [PubLayNet](https://github.com/ibm-aur-nlp/PubLayNet)
- [DocBank](https://github.com/doc-analysis/DocBank)

## Related open-source tools

- [parsCit](https://github.com/knmnyn/ParsCit)
- [Neural-ParsCit](https://github.com/WING-NUS/Neural-ParsCit)
- [CERMINE](https://github.com/CeON/CERMINE)
- [Science Parse](https://github.com/allenai/science-parse)
- [science Parse v2](https://github.com/allenai/spv2)
- [BILBO](https://github.com/OpenEdition/bilbo)
- [AnyStyle](https://github.com/inukshuk/anystyle)

## Transformer and layout-model approaches

- [LayoutLM](https://github.com/microsoft/unilm/tree/master/layoutlm)
- [LayoutLMv2](https://github.com/microsoft/unilm/tree/master/layoutlmv2)
- [VILA](https://github.com/allenai/VILA)

## More resources

For broader document layout analysis resources, see [DocumentLayoutAnalysis](https://github.com/BobLd/DocumentLayoutAnalysis/).
