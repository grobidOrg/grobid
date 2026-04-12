<h1>End-to-end evaluation</h1>

Individual models can be evaluated as explained in [Training the different models of Grobid](Training-the-models-of-Grobid.md).

For an end-to-end evaluation, covering the whole extraction process from the parsing of PDF to the end result of the cascading of several sequence labelling models, GROBID includes two possible evaluation progresses:

* against JATS-encoded (NLM) articles, such as [PubMed Central](http://www.ncbi.nlm.nih.gov/pmc), [bioRxiv](https://www.biorxiv.org), [PLOS](https://plos.org/ ) or [eLife](https://elifesciences.org/ ). For example, PubMed Central provides both PDF and fulltext XML files in the [NLM](http://www.ncbi.nlm.nih.gov/pmc/pmcdoc/tagging-guidelines/article/style.html) format. Keeping in mind some limits described bellow, it is possible to estimate the ability of Grobid to extract and normalize the content of the PDF documents for matching the quality of the NLM file. bioRxiv is used in Grobid to evaluate more precisely performance on preprint articles. 

* against TEI documents produced by [Pub2TEI](https://github.com/kermitt2/Pub2TEI). Pub2TEI is a set of XSLT that permit to transform various _native_ XML publishers (including Elsevier, Wiley, Springer, etc. XML formats) into a common TEI format. This TEI format can be used as ground-truth structure information for evaluating GROBID output, keeping in mind some limits described bellow. 

For actual benchmarks, see the [Benchmarking page](benchmarks/Benchmarking.md). We describe below the datasets and how to run the benchmarks.  

## Datasets

The corpus used for the end-to-end evaluation of Grobid are all available in a single Hugging Face dataset: [https://huggingface.co/datasets/sciencialab/grobid-evaluation](https://huggingface.co/datasets/sciencialab/grobid-evaluation). Some of these datasets have been further annotated to make the evaluation of certain sub-structures possible (in particular code and data availability sections & funding sections).

*Previously, these datasets were archived on Zenodo at [https://zenodo.org/record/7708580](https://zenodo.org/record/7708580). The Hugging Face repository is now the canonical source.*

These resources are originally published under CC-BY license. Our additional annotations are similarly under CC-BY. We thank NIH, bioRxiv, PLOS and eLife for making these resources Open Access and reusable. 

### PubMedCentral gold-standard data 

Since ages, we are evaluating GROBID using the `PMC_sample_1943` dataset compiled by Alexandru Constantin. The dataset is available at this [url](https://huggingface.co/datasets/sciencialab/grobid-evaluation) (around 1.5GB in size). The sample dataset contains 1943 articles from 1943 different journals corresponding to the latest publications from a 2011 snapshot. 

Any similar PubMed Central set of articles could normally be used, as long they follow the same directory structure: one directory per article containing at least the corresponding PDF file and the reference NLM file. 

We suppose in the following that the archive is decompressed under `PATH_TO_PMC/PMC_sample_1943/`.

### The bioRxiv gold-standard data 

For evaluation on preprint articles, we are using the balanced bioRxiv 10k dataset originally compiled with care and published by Daniel Ecer ([eLife](https://elifesciences.org)), available on [Hugging Face](https://huggingface.co/datasets/sciencialab/grobid-evaluation). More precisely we publish benchmarks using the test subset of 2000 articles. The zip archive is similar in structure to the above PMC sample 1943 dataset and further documented below. 

### The PLOS 1000 dataset

This is a set of 1000 PLOS articles, called `PLOS_1000` and available on [Hugging Face](https://huggingface.co/datasets/sciencialab/grobid-evaluation), randomly selected from the full [PLOS Open Access collection](https://allof.plos.org/allofplos.zip). Again, for each article, the published PDF is available with the corresponding publisher JATS XML file, around 1.3GB total size.

### eLife 984 dataset

The `eLife_984` dataset is a set of 984 articles from eLife, available on [Hugging Face](https://huggingface.co/datasets/sciencialab/grobid-evaluation), randomly selected from their [open collection available on GitHub](https://github.com/elifesciences/elife-article-xml). Every articles come with the published PDF, the publisher JATS XML file and the eLife public HTML file (as bonus, not used), all in their latest version, around 4.5G total.

## Getting publisher gold-standard data 

Some publishers release publications in XML format complementary to PDF in Open Access, allowing text mining (see for instance the dedicated subset of PMC publications). On contractual basis, it is possible to acquire native XML from mainstream publishers. Unfortunately, each publisher uses a different XML schema and covering all these formats would be a very time-consuming work. To ease the processing of these XML documents, the projet [Pub2TEI](https://github.com/kermitt2/Pub2TEI) permits to transform the native XML formats of a dozen mainstream publishers into a common TEI format which is the same as the output of GROBID. 

See [Pub2TEI](https://github.com/kermitt2/Pub2TEI) for converting native publisher XML into usable TEI. 

## Directory structure

For running the evaluation, the tool assumes that the files are organised in a set of directory in the following way: 

* a root directory containing one sub-directory per article

* each article sub-directory containing at least the PDF version and a gold XML structured version of the article (in NLM format for PubMedCentral evaluation or in TEI format for the Pub2TEI-based evaluation). See the diagram bellow - the name of the sub-directory and the files is free.  

* extension for files generated with [Pub2TEI](https://github.com/kermitt2/Pub2TEI) is `.pub2tei.tei.xml`. Extension for NLM files is `.nxlm` (PMC) or `xml` (bioRxiv). GROBID will generate additional TEI files with extension `.fulltext.tei.xml`. 

```
├── article1
│   ├── article1.pdf
│   └── article1.pub2tei.tei.xml
│   └── article1.nxml
│  
└── articles2
│   ├── article2.pdf
│   └── article2.pub2tei.tei.xml
│   └── article2.nxml
...
```

## Warning on JATS/NLM format

JATS/NLM is a very loose XML format, in the sense that there are multiple ways to encode the same information. As a consequence, there are a variety of JATS flavors depending on the publisher and it is not possible to garantee that any JATS files will be supported as gold standard dataset by the `jatsEval` process. PMC and bioRxiv JATS articles are supported, but for a larger variety of JATS files it is recommanded to convert them first into TEI with [Pub2TEI](https://github.com/kermitt2/Pub2TEI) and use the `teiEval` process. [Pub2TEI](https://github.com/kermitt2/Pub2TEI) supports all the JATS/NLM variants we are aware of, and convert them into a constrained and unambiguous single TEI format without information loss. 

## Running and evaluating 

### JATS encoded corpus, e.g. PubMed Central, bioRxiv, PLOS, eLife

Under ```grobid/```, the following command line is used to run and evaluate Grobid on the dataset:
```bash
> ./gradlew jatsEval -Pp2t=ABS_PATH_TO_JATS_DATASET/DATASET -Prun=1
```

Replace the absolute path and directory dataset name by the selected dataset for end-to-end evaluation, for example `PMC_sample_1943`, `biorxiv-10k-test-2000`, `PLOS_1000` or `eLife_984` (see above for downloading these datasets). 

The parameters `run` indicates if GROBID has to be executed on all the PDF of the data set. The resulting TEI file will be added in each article subdirectory. If you only want to run the evaluation without re-executing Grobid on the PDF, set the parameter to 0:
```bash
> ./gradlew jatsEval -Pp2t=ABS_PATH_TO_JATS_DATASET/DATASET -Prun=0
```
It is also possible to set a ratio of evaluation data to be used expressed as a number between 0 and 1 introduced by the parameter `fileRatio`. For instance, if you want to evaluate Grobid against only 10% of the PubMedCentral files, use:
```bash
> ./gradlew jatsEval -Pp2t=ABS_PATH_TO_JATS_DATASET/DATASET -Prun=0 -PfileRatio=0.1
```

### Pub2TEI-based

Under ```grobid/```, the following command line is used to run and evaluate Grobid on the dataset:
```bash
> ./gradlew teiEval -Pp2t=ABS_PATH_TO_TEI/ -Prun=1
```
The parameters `run` indicates if GROBID has to be executed on all the PDF of the data set. The resulting GROBID TEI file will be added in each article subdirectory. If you only want to run the evaluation without re-executing Grobid on the PDF, set the parameter to 0:
```bash
> ./gradlew teiEval -Pp2t=ABS_PATH_TO_TEI/ -Prun=0
```
It is also possible to set a ratio of evaluation data to be used expressed as a number between 0 and 1 introduced by the parameter `fileRatio`. For instance, if you want to evaluate Grobid against only 10% of the Pub2TEI-produced files, use:
```bash
> ./gradlew teiEval -Pp2t=ABS_PATH_TO_TEI/ -Prun=0 -PfileRatio=0.1
```

## Evaluation results

The evaluation provides precision, recall and F1-score for the different fields in the header and bibliographical references. In addition, the scores are also computed at *instance* level, which means at the level of a complete header or complete citation.

An experimental evaluation for the structures of the full text body is also proposed. This is not reliable in the current state, because most of the annotations of the full texts in PudMed Central are not uniform. For instance, the numbering of the section header is sometime included in the section header annotation, sometime not. The PubMed Central annotations will need to be standardized as a pre-process for a meaningful evaluation, which is a task planned in the next releases. 

## Matching techniques


The evaluation covers four different string matching techniques for textual fields, based on the existing evaluation approaches observed in the litterature:

* __strict__, i.e. exact match,

* __soft__ corresponding to matching ignoring punctuations, character case and space character mismatches,

* __relative Levenshtein distance__ relative to the max length of two strings

* [__Ratcliff/Obershelp similarity__](http://xlinux.nist.gov/dads/HTML/ratcliffObershelp.html) 

These macthing variants only apply to textual fields, not numerical and dates fields (such as volume, issue, dates, pages).



## Limits

### Non structured information in XML "gold" data

A relatively important number of citations in the NLM files (and other native publisher XML) are encoded only as raw string, for example in the first file of the set `AAPS_J_2011_Mar_9_13(2)_230-239/12248_2011_Article_9260.nxml`: 

```xml
	  ...
      <ref id="CR9">
        <label>9.</label>
        <mixed-citation publication-type="other">Pira&#xF1;a and PCluster: a modeling environment and cluster infrastructure for NONMEM. Keizer RJ, van Benten M, Beijnen JH, Schellens JH, Huitema AD. Comput Methods Programs Biomed. 2011;101(1):72&#x2013;9.</mixed-citation>
      </ref>
      <ref id="CR10">
        <label>10.</label>
        <mixed-citation publication-type="other">Holford, N. VPC, the visual predictive check&#x2014;superiority to standard diagnostic (Rorschach) plots. In: PAGE 2005. 2005.</mixed-citation>
      </ref>
	  ...
```
(this file contains for instance 3 non-encoded citations out of 18)

As a consequence, the fields extracted by GROBID will not match any reference 'expected' values and will all be considered as false positive. The scores for the citation structures are thus lower than the actual performance of the system. 

### Non encoded information in XML "gold" data for intervals

In the reference NLM/JATS files from PMC, in the case of range citation callout, e.g. __[1-4]__, usually only the label number visible in the text are annotated. For example, the following text

```
Recent studies have described the use of contrast material with high iodine content (370 mg I/ml or 400 mg I/ml) for coronary CT angiography (CCTA) (1 – 4).
```

which is annotated as follow:

```xml
Recent studies have described the use of contrast material with high iodine content (370 mg I/ml or 400 mg I/ml) for coronary CT angiography (CCTA) (<xref ref-type="bibr" rid="b1">1</xref>&#x2013;<xref ref-type="bibr" rid="b4">4</xref>).
```

The references 2 and 3 are thus missing.  

GROBID expends intervals and will likely identify and match these "intermediary" callouts (including 2 and 3 in the above example). However these additional correct extractions and matching from GROBID will be counted as false positive in the evaluation because missing from the "gold" data.


### Character encoding and glyphs

XML and PDF content frequently contain many differences at character-level. This is due to PDF which tend to use particular glyphs for enhancing visual rendering. Those special glyths are often loaded in the PDF itself and uses particular unicode not matching unicode of characters in XML. Similarly some special charcaters are expressed with fonts (for instance using a Greek font to render a λ, using the unicode of the letter _l_). 

### Ordering and presentation variants of structures and sub-structures

The order of some structures might also be changed from the logical representation (XML) to the particular PDF presentation base on (unknown) style transformation. 

Still related to style rendering, text can be post-processed. For instance, in a bibliographical reference, forenames can be present in full form in the XML, but shorten to intials only in the PDF.

### Evaluation criteria

The tool uses currently rather strict evaluation criteria. For instance, `authors` field is considered correct only if the whole set of authors, including the order of authors, match. More partial and fine-grained matching is not implemented yet. 

*Given these limit, this evaluation cannot be considered currently as a reliable absolute evaluation (how good GROBID will extract valid and usable structures from PDF), but rather as a way to keep track of progress from one version of GROBID to another one and avoid regressions.*

