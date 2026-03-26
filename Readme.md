# GROBID

[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Coverage Status](https://coveralls.io/repos/kermitt2/grobid/badge.svg)](https://coveralls.io/r/kermitt2/grobid)
[![GitHub release](https://img.shields.io/github/release/kermitt2/grobid.svg)](https://github.com/kermitt2/grobid/releases/)
[![Demo lfoppiano-grobid.hf.space](https://img.shields.io/website-up-down-green-red/https/lfoppiano-grobid.hf.space.svg)](https://lfoppiano-grobid.hf.space)
[![Docker Hub](https://img.shields.io/docker/pulls/grobid/grobid.svg)](https://hub.docker.com/r/grobid/grobid/ "Docker Pulls")
[![Docker Hub](https://img.shields.io/docker/pulls/lfoppiano/grobid.svg)](https://hub.docker.com/r/lfoppiano/grobid/ "Docker Pulls")
[![SWH](https://archive.softwareheritage.org/badge/origin/https://github.com/kermitt2/grobid/)](https://archive.softwareheritage.org/browse/origin/?origin_url=https://github.com/kermitt2/grobid)

> [!TIP]
> Getting started: <https://grobidorg.github.io/grobid/getting-started/quickstart-docker>

## Summary

GROBID (or Grobid, but not GroBid nor GroBiD) means **G**ene**R**ation **O**f **BI**bliographic **D**ata.

GROBID is a machine learning library for extracting, parsing and re-structuring raw documents such as PDF into structured XML/TEI encoded documents with a particular focus on technical and scientific publications. First developments started in 2008 as a hobby, following a suggestion by Laurent Romary (Inria, France). In 2011, the tool has been made available in open source. Work on GROBID has been steady as a side project since the beginning and is expected to continue as such, facilitated in particular to the continuous support of Inria.

The following functionalities are available:

- __Header extraction and parsing__ from article in PDF format. The extraction here covers the usual bibliographical information (e.g. title, abstract, authors, affiliations, keywords, etc.).
- __References extraction and parsing__ from articles in PDF format, around .87 F1-score against on an independent PubMed Central set of 1943 PDF containing 90,125 references, and around .90 on a similar bioRxiv set of 2000 PDF (using the Deep Learning citation model). All the usual publication metadata are covered (including DOI, PMID, etc.).
- __Citation contexts recognition and resolution__ of the full bibliographical references of the article. The accuracy of citation contexts resolution is between .76 and .91 F1-score depending on the evaluation collection (this corresponds to both the correct identification of the citation callout and its correct association with a full bibliographical reference).
- __Full text extraction and structuring__ from PDF articles, including a model for the overall document segmentation and models for the structuring of the text body (paragraph, section titles, reference and footnote callouts, figures, tables, data availability statements, etc.). 
- __PDF coordinates__ for extracted information, allowing to create "augmented" interactive PDF based on bounding boxes of the identified structures.
- Parsing of __references in isolation__ (above .90 F1-score at instance-level, .95 F1-score at field level, using the Deep Learning model).
- __Parsing of names__ (e.g. person title, forenames, middle name, etc.), in particular author names in header, and author names in references (two distinct models).
- __Parsing of affiliation and address__ blocks.
- __Parsing of dates__, ISO normalized day, month, year.
- __Consolidation/resolution of the extracted bibliographical references__ using the [biblio-glutton](https://github.com/kermitt2/biblio-glutton) service or the [CrossRef REST API](https://github.com/CrossRef/rest-api-doc). In both cases, DOI/PMID resolution performance is higher than 0.95 F1-score from PDF extraction.
- __Extraction and parsing of patent and non-patent references in patent__ publications.
- __Extraction of Funders and funding information__ with optional matching of extracted funders with the CrossRef Funder Registry.
- __Identification of copyrights' owner and license associated to the document__, e.g. publisher or authors copyrights, CC-BY/CC-BY-NC/etc. license.

In a complete PDF processing, GROBID manages 68 final labels used to build relatively fine-grained structures, from traditional publication metadata (title, author first/last/middle names, affiliation types, detailed address, journal, volume, issue, pages, DOI, PMID, etc.) to full text structures (section title, paragraph, reference markers, head/foot notes, figure captions, etc.).

GROBID includes a comprehensive [web service API](https://grobidorg.github.io/grobid/reference/api-endpoints), [Docker deployment path](https://grobidorg.github.io/grobid/guides/docker/docker-setup), [batch processing guidance](https://grobidorg.github.io/grobid/guides/api/batch-processing), a Java API, a training and evaluation framework, historical benchmark material in the archive docs, and semi-automatic generation of training data.

GROBID can be considered as production ready. Deployments in production includes ResearchGate, Semantic Scholar, HAL Research Archive, scite.ai, Academia.edu, Internet Archive Scholar, INIST-CNRS, CERN (Invenio), and many more. The tool is designed for speed and high scalability in order to address the full scientific literature corpus.

## Requirements

- **OpenJDK 21** for building GROBID from source
- Linux (64 bits) or macOS (Intel and ARM) for native builds
- [Optional] Python 3.8+ with JEP for Deep Learning models 
- [Optional] NVIDIA GPU with CUDA support for faster Deep Learning models

> [!TIP]
> We bump to OpenJDK 21, however some dependencies may require an earlier version, so we might increase the runtime backward compatibility to JDK 17+ in the next release, > 0.8.2. 

For detailed installation instructions, including JDK setup and platform-specific requirements, see the local build guide: <https://grobidorg.github.io/grobid/getting-started/quickstart-local>

GROBID should run properly "out of the box" on Linux (64 bits) and macOS (Intel and ARM). We cannot ensure currently support for Windows as we did before (help welcome!).

GROBID uses Deep Learning models relying on the [DeLFT](https://github.com/kermitt2/delft) library, a task-agnostic Deep Learning framework for sequence labelling and text classification, via [JEP](https://github.com/ninia/jep). GROBID can run Deep Learning architectures (RNN or transformers with or without layout feature channels) or with feature engineered CRF (default), or any mixtures of CRF and DL to balance scalability and accuracy. These models use joint text and visual/layout information provided by [pdfalto](https://github.com/kermitt2/pdfalto). 

Note that by default the Deep Learning models are not used, only CRF are selected in the default configuration to accommodate out-of-the-box hardware. For improved accuracy, select deep-learning-backed models deliberately based on your workload and hardware capacity. See <https://grobidorg.github.io/grobid/explanation/deep-learning> and <https://grobidorg.github.io/grobid/guides/training/model-selection>.

## Demo

### Demo server

For testing purposes, two public GROBID demo servers are available thanks to HuggingFace, hosted as [spaces](https://huggingface.co/kermitt2).

A GROBID demo server with a combination of Deep Learning models and CRF models is available at the following address: [https://kermitt2-grobid.hf.space/](https://kermitt2-grobid.hf.space/) or at [https://huggingface.co/spaces/kermitt2/grobid](https://huggingface.co/spaces/kermitt2/grobid). This demo runs however on CPU only. If you have GPU for your own server deployment, it will be significantly faster. 

A faster demo with CRF only is available at [https://kermitt2-grobid-crf.hf.space/](https://kermitt2-grobid-crf.hf.space/) or [https://huggingface.co/spaces/kermitt2/grobid-crf](https://huggingface.co/spaces/kermitt2/grobid-crf). However, accuracy is lower.

The web services are documented here: <https://grobidorg.github.io/grobid/reference/api-endpoints>

_Warning_: Some quota and query limitation apply to the demo server! Please be courteous and do not overload the demo server. 
For any serious work, deploy your own GROBID server. Start with the Docker setup guide: <https://grobidorg.github.io/grobid/guides/docker/docker-setup>

### Try in Play With Docker

<a href="https://labs.play-with-docker.com/?stack=https://raw.githubusercontent.com/kermitt2/grobid/master/compose.yml">
  <img src="https://raw.githubusercontent.com/play-with-docker/stacks/master/assets/images/button.png" alt="Try in PWD"/>
</a>

Wait for 30 seconds for Grobid container to be created before opening a browser tab on port 8080. This demo container runs only with CRF models. Note that there is an additional 60s needed when processing a PDF for the first time for the loading of the models on the "cold" container. Then this Grobid container is available just for you during 4 hours. 

## Clients

For using the GROBID service at scale, we provide clients written in Python, Java, and Node.js using the web services for parallel batch processing:

- <a href="https://github.com/kermitt2/grobid-client-python" target="_blank">Python GROBID client</a> (the most complete one in term of supported services and options)
- <a href="https://github.com/kermitt2/grobid-client-java" target="_blank">Java GROBID client</a>
- <a href="https://github.com/kermitt2/grobid-client-node" target="_blank">Node.js GROBID client</a>

A third party client for Go is available offering functionality similar to the Python client:

- <a href="https://github.com/miku/grobidclient" target="_blank">Go GROBID client</a>

All these clients take advantage of multi-threading for scaling large PDF processing workloads. As a consequence, they are much more efficient than the legacy batch command lines and should be preferred.

For example, we have been able to run the complete full-text processing at around 10.6 PDF per second (around 915,000 PDF per day, around 20M pages per day) with the node.js client listed above during one week on one 16 CPU machine (16 threads, 32GB RAM, no SDD, articles from mainstream publishers), see [here](https://github.com/kermitt2/grobid/issues/443#issuecomment-505208132) (11.3M PDF were processed in 6 days by 2 servers without interruption).

In addition, a Java example project is available to illustrate how to use GROBID as a Java library: [https://github.com/kermitt2/grobid-example](https://github.com/kermitt2/grobid-example). The example project is using GROBID Java API for extracting header metadata and citations from a PDF and output the results in BibTeX format.  

Finally, the following python utilities can be used to create structured full text corpora of scientific articles. The tool simply takes a list of strong identifiers like DOI or PMID, performing the identification of online Open Access PDF, full text harvesting, metadata aggregation and Grobid processing in one workflow at scale: [article-dataset-builder](https://github.com/kermitt2/article-dataset-builder)

## How GROBID works 

For the current documentation set, see:

- architecture: <https://grobidorg.github.io/grobid/explanation/architecture>
- ML pipeline: <https://grobidorg.github.io/grobid/explanation/ml-pipeline>
- design principles: <https://grobidorg.github.io/grobid/explanation/design-principles>

To summarize, the key design principles of GROBID are:

- GROBID uses a cascade of sequence labeling models to parse a document.

- The different models do not work on text only, but on layout-aware tokens to exploit visual and positional information.

- GROBID does not rely only on publisher XML-derived data, but emphasizes smaller, high-quality manually labeled training sets.

- Technical choices and default settings are driven by the ability to process PDF quickly, with commodity hardware and good scalability.

Historical benchmark material is preserved in the archive docs; treat it as reference material rather than current product guidance.

## GROBID Modules

A series of additional modules have been developed for performing __structure aware__ text mining directly on scholar PDF, reusing GROBID's PDF processing and sequence labelling weaponry:

- [software-mention](https://github.com/ourresearch/software-mentions): recognition of software mentions and associated attributes in scientific literature
- [datastet](https://github.com/kermitt2/datastet): identification of sections and sentences introducing datasets in a scientific article, identification of dataset names and attributes (implict and named datasets) and classification of the type of datasets
- [grobid-quantities](https://github.com/kermitt2/grobid-quantities): recognition and normalization of physical quantities/measurements
- [grobid-superconductors](https://github.com/lfoppiano/grobid-superconductors): recognition of superconductor material and properties in scientific literature
- [entity-fishing](https://github.com/kermitt2/entity-fishing), a tool for extracting Wikidata entities from text and document, which can also use Grobid to pre-process scientific articles in PDF, leading to more precise and relevant entity extraction and the capacity to annotate the PDF with interactive layout
- [grobid-ner](https://github.com/kermitt2/grobid-ner): named entity recognition
- [grobid-astro](https://github.com/kermitt2/grobid-astro): recognition of astronomical entities in scientific papers
- [grobid-bio](https://github.com/kermitt2/grobid-bio): a toy bio-entity tagger using BioNLP/NLPBA 2004 dataset
- [grobid-dictionaries](https://github.com/MedKhem/grobid-dictionaries): structuring dictionaries in raw PDF format

## Release and changes

See the [Changelog](CHANGELOG.md).

## License

GROBID is distributed under [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0). 

The documentation is distributed under [CC-0](https://creativecommons.org/publicdomain/zero/1.0/) license and the annotated data under [CC-BY](https://creativecommons.org/licenses/by/4.0/) license.

If you contribute to GROBID, you agree to share your contribution following these licenses. 

Main author and contact: Patrice Lopez (<patrice.lopez@science-miner.com>)

## Sponsors

ej-technologies provided us a free open-source license for its Java Profiler: <http://www.ej-technologies.com/products/jprofiler/overview.html>

JetBrains provided us with a free licence for the development: 

[![JetBrains logo.](https://resources.jetbrains.com/storage/products/company/brand/logos/jetbrains.svg)](https://jb.gg/OpenSource)


## How to cite

If you want reference this software, please refer to the present GitHub project, together with the [Software Heritage](https://www.softwareheritage.org/) project-level permanent identifier.

For example, the BibTeX would look like this:

```bibtex
@misc{GROBID,
    title = {GROBID},
    howpublished = {\url{https://github.com/kermitt2/grobid}},
    publisher = {GitHub},
    year = {2008--2026},
    archivePrefix = {swh},
    eprint = {1:dir:dab86b296e3c3216e2241968f0d63b68e8209d3c}
}
```

> [!TIP]
> To fetch the latest SWID you can use the following command line (requires `curl` and `jq`):
    
```
curl -s "https://archive.softwareheritage.org/api/1/origin/https://github.com/kermitt2/grobid/visit/latest/" \
  -H "Accept: application/json" | jq -r '.snapshot' | \
  xargs -I {} curl -s "https://archive.softwareheritage.org/api/1/snapshot/{}/" | \
  jq -r '.branches["refs/heads/master"].target' | \
  xargs -I {} echo "swh:1:dir:{}"
swh:1:dir:324a18113b0c7624a66a21550bd0e8522e328b4e
```


See the documentation references page for more related resources: <https://grobidorg.github.io/grobid/community/references>
