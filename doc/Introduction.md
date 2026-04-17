<h1>Introduction</h1>

## Status

[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Coverage Status](https://coveralls.io/repos/kermitt2/grobid/badge.svg)](https://coveralls.io/r/kermitt2/grobid)
[![Documentation Status](https://readthedocs.org/projects/grobid/badge/?version=latest)](https://readthedocs.org/projects/grobid/?badge=latest)
[![GitHub release](https://img.shields.io/github/release/grobidOrg/grobid.svg)](https://github.com/grobidOrg/grobid/releases/)
[![Demo grobid.science-miner.com](https://img.shields.io/website-up-down-green-red/https/grobid.science-miner.com.svg)](http://grobid.science-miner.com)
[![Docker Hub](https://img.shields.io/docker/pulls/grobid/grobid.svg)](https://hub.docker.com/r/grobid/grobid/ "Docker Pulls")
[![Docker Hub](https://img.shields.io/docker/pulls/lfoppiano/grobid.svg)](https://hub.docker.com/r/lfoppiano/grobid/ "Docker Pulls")
[![SWH](https://archive.softwareheritage.org/badge/origin/https://github.com/grobidOrg/grobid/)](https://archive.softwareheritage.org/browse/origin/?origin_url=https://github.com/grobidOrg/grobid)

## Purpose

GROBID (or Grobid, but not GroBid nor GroBiD) means GeneRation Of BIbliographic Data. 

GROBID is a machine learning library for extracting, parsing and re-structuring raw documents such as PDF into structured XML/TEI encoded documents with a particular focus on technical and scientific publications. First developments started in 2008 as a hobby, following a suggestion by Laurent Romary (Inria). In 2011 the tool has been made available in open source. Work on GROBID has been steady as side project since the beginning and is expected to continue as such.

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
- __Consolidation/resolution of the extracted bibliographical references__ using the [biblio-glutton](https://github.com/kermitt2/biblio-glutton) service or the [Crossref REST API](https://api.crossref.org). In both cases, DOI/PMID resolution performance is higher than 0.95 F1-score from PDF extraction.
- __Extraction and parsing of patent and non-patent references in patent__ publications.
- __Extraction of Funders and funding information__ with optional matching of extracted funders with the Crossref Funder Registry.

In a complete PDF processing, GROBID manages more than 55 final labels used to build relatively fine-grained structures, from traditional publication metadata (title, author first/last/middle names, affiliation types, detailed address, journal, volume, issue, pages, DOI, PMID, etc.) to full text structures (section title, paragraph, reference markers, head/foot notes, figure captions, etc.).

GROBID includes a comprehensive [web service API](https://grobid.readthedocs.io/en/latest/Grobid-service/), [Docker images](https://grobid.readthedocs.io/en/latest/Grobid-docker/), [batch processing](https://grobid.readthedocs.io/en/latest/Grobid-batch/), a JAVA API, a generic [training and evaluation framework](https://grobid.readthedocs.io/en/latest/Training-the-models-of-Grobid/) (precision, recall, etc., n-fold cross-evaluation), systematic [end-to-end benchmarking](https://grobid.readthedocs.io/en/latest/Benchmarking/) on thousand documents and the semi-automatic generation of training data.

GROBID can be considered as production ready. Deployments in production includes ResearchGate, Academia.edu, HAL Research Archive, the European Patent Office, INIST-CNRS, Mendeley, CERN (Invenio), Internet Archive, and many others.

The key aspects of GROBID are the following ones:

+ Written in Java, with JNI call to native CRF libraries and/or Deep Learning libraries via Python JNI bridge. 
+ Speed - on low profile Linux machine (8 threads): header extraction from 4000 PDF in 2 minutes (36 PDF per second with the REST API), parsing of 3500 references in 4 seconds, full processing of 4000 PDF (full body, header and reference, structured) in 26 minutes (around 2.5 PDF per second). 
+ Scalability and robustness: We have been able recently to run the complete fulltext processing at around 10.6 PDF per second (around 915,000 PDF per day, around 20M pages per day) during one week on one 16 CPU machine (16 threads, 32GB RAM, no SDD, articles from mainstream publishers), see [here](https://github.com/kermitt2/grobid/issues/443#issuecomment-505208132) (11.3M PDF were processed in 6 days by 2 servers without crash).
+ Optional lazy loading of models and resources. Depending on the selected process, only the required data are loaded in memory. For instance, extracting only metadata header from a PDF requires less than 2 GB memory in a multithreading usage, extracting citations uses around 3GB and extracting all the PDF structures around 4GB.  
+ Robust and fast PDF processing with [pdfalto](https://github.com/kermitt2/pdfalto), based on xpdf, and dedicated post-processing.
+ Modular and reusable machine learning models for sequence labelling. The default extractions are based on Linear Chain Conditional Random Fields, with the possibility to use various Deep Learning architectures for sequence labelling (including ELMo and BERT-CRF) for improving accuracy. The specialized sequence labelling models are cascaded to build a complete (hierarchical) document structure.  
+ Full encoding in [__TEI__](http://www.tei-c.org/Guidelines/P5/index.xml), both for the training corpus and the parsed results.
+ Optional consolidation of extracted bibliographical data via online call to [biblio-glutton](https://github.com/kermitt2/biblio-glutton) or the [Crossref REST API](https://api.crossref.org), export to OpenURL, BibTeX, etc. for easier integration into Digital Library environments. For scalability, reliability and accuracy, we recommend to use [biblio-glutton](https://github.com/kermitt2/biblio-glutton) when possible.
+ Rich bibliographical processing: fine grained parsing of author names, dates, affiliations, addresses, etc. but also for instance quite reliable automatic attachment of affiliations and emails to authors. 
+ _Automatic generation_ of pre-annotated training data from new PDF documents based on current models, for supporting semi-automatic training data generation. 
+ Support for CJK and Arabic languages based on customized Lucene analyzers provided by WIPO.
+ PDF coordinates for extracted information, providing the positions of the identified structures as bounding boxes.

By default, the GROBID extraction and parsing algorithms use a [fork](https://github.com/kermitt2/wapiti) of [Wapiti CRF library](http://wapiti.limsi.fr). As alternative, it is possible to perform the sequence labelling with [DeLFT](https://github.com/kermitt2/delft) deep learning models (typically BidLSTM-CRF with or without ELMo, or BERT-CRF, with additional feature channels) instead of Wapiti CRF models, using a native integration via [JEP](https://github.com/ninia/jep). The native libraries, in particular TensorFlow, are transparently integrated as JNI with dynamic call based on the current OS. Deep Learning models should be used when accuracy is the main priority. Without GPU Deep Learning models might involve reduced scalability. See the related [benchmarking](End-to-end-evaluation.md). 

GROBID should run properly "out of the box" on Linux (64 bits) and macOS (Intel and ARM). 

## Credits

The main author is Patrice Lopez (patrice.lopez@science-miner.com). 

Core committers and maintenance: Patrice Lopez (science-miner) and Luca Foppiano (ScienciaLAB).

Many thanks to:

* Laurent Romary (Inria)
* Vyacheslav Zholudev (Sumsub, formerly at ResearchGate)
* Achraf Azhar (CCSD CNRS)
* Daniel Ecer (eLife)
* Vitalii Bezsheiko (PKP)
* Bryan Newbold (Internet Archive)
* Christopher Boumenot (Microsoft) in particular for the (former) Windows support
* CERN contributors Andreas la Roi and Micha Moskovic
* Florian Zipser (Humboldt University) who developed the first historical version of the REST API in 2011
* the other contributors from ResearchGate: Michael Häusler, Kyryl Bilokurov, Artem Oboturov
* Damien Ridereau (U IRIS, formely at Infotel)
* Oliver Kopp (JabRef research)
* Bruno Pouliquen (WIPO) for the custom analyzers for Eastern languages
* Thomas Lavergne, Olivier Cappé and François Yvon for Wapiti
* The JEP team for their great JVM CPython embedding solution
* Taku Kudo for CRF++ (not used anymore, but all the same, thanks!)
* Hervé Déjean and his colleagues from Xerox Research Centre Europe, for xml2pdf
* Walid R. Rashed (@kurdi-dev) for the Play with Docker settings
* and the other contributors: @elonzh, Jakob Fix, Tanti Kristanti, Benedikt Tutzer, Dmitry Katsubo, Phil Gooch, Romain Loth, Maud Medves, Chris Mattmann, Sujen Shah, Joseph Boyd, Guillaume Muller, ...
