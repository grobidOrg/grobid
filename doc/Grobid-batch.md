# GROBID batch mode

!!! warning "Deprecated method"
    We do **not** recommend to use the batch mode. For the best performance, benchmarking and for exploiting multithreading, we recommend to use the service, see [Use GROBID as a service](Grobid-service.md), and not the batch mode. Clients for GROBID services are provided in [Python](https://github.com/kermitt2/grobid-client-python), [Java](https://github.com/kermitt2/grobid-client-java), [node.js](https://github.com/kermitt2/grobid-client-node) and [Go](https://github.com/miku/grobidclient). 

!!! tip
    If you do not need good runtime and just need to casually process some inputs, the batch mode is available for convenience. 

## Using the batch

Be sure that the GROBID project is built, see [Install GROBID](Install-Grobid.md).

Go under the project directory `grobid/`:
```bash
> cd grobid/
```

The following command display some help for the batch commands:
```bash
> java -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-`<current version>`-onejar.jar -h
```

Be sure to replace `<current version>` with the current version of GROBID that you have installed and built. For example:
```bash
> java -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-0.9.0-onejar.jar -h
```

The available batch commands are listed below. For those commands, at least `-Xmx1G` is used to set the JVM memory to avoid *OutOfMemoryException* given the current size of the Grobid models and the crazyness of some PDF. For complete fulltext processing, which involve all the GROBID models, `-Xmx4G` is recommended (although allocating less memory is usually fine). 

The so called "GROBID home" in GROBID is the path to `grobid-home` (by default `grobid/grobid-home`). Pay attention that it is not the installation path to the full grobid project (e.g. to `grobid/`). In the following batch command lines, the GROBID home path can be specified with parameters `-gH` (default is `grobid/grobid-home`). 


### processHeader
'processHeader' batch command will extract, structure and normalise in TEI the header of pdf files. The output is a TEI file corresponding to the structured article header.
The needed parameters for that command are:

* -gH: path to grobid-home directory

* -dIn: path to the directory of input PDF files

* -dOut: path to the output directory (if omitted the current directory)

* -r: recursive processing of files in the sub-directories (by default not recursive)

Example:
```bash
> java -Xmx1G -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-0.9.0-onejar.jar -gH grobid-home -dIn /path/to/input/directory -dOut /path/to/output/directory -r -exe processHeader 
```

WARNING: the expected extension of the PDF files to be processed is .pdf

### processFullText
`processFullText` batch command will extract, structure and normalize in TEI the full text of pdf files. The needed parameters for that command are:

* -gH: path to grobid-home directory

* -dIn: path to the directory of input PDF files

* -dOut: path to the output directory (if omitted the current directory)

* -r: recursive processing of files in the sub-directories (by default not recursive)

* -ignoreAssets: do not extract and save the PDF assets (bitmaps, vector graphics), by default the assets are extracted and saved

* -teiCoordinates: output a subset of the identified structures with coordinates in the original PDF, by default no coordinates are present

* -addElementId: add xml:id attribute automatically to the XML elements in the resulting TEI XML, by default no xml:id are added

* -segmentSentences: add sentence segmentation level structures for paragraphs in the TEI XML result, by default no sentence segmentation is done 

Example:
```bash
> java -Xmx4G -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-0.9.0-onejar.jar -gH grobid-home -dIn /path/to/input/directory -dOut /path/to/output/directory -exe processFullText 
```

WARNING: the expected extension of the PDF files to be processed is .pdf

### processDate
`processDate` batch command will parse and format in XML/TEI the date given as string input. The needed parameters for that command are:

* -gH: path to grobid-home directory

* -s: the input date to format as raw string

Example:
```bash
> java -Xmx1G -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-0.9.0-onejar.jar -gH grobid-home -exe processDate -s "some date to extract and format"
```

### processAuthorsHeader
`processAuthorsHeader` batch command will parse and format in TEI the authors given in input. The needed parameters for that command are:

* -gH: path to grobid-home directory

* -s: the input header author sequence as raw string

Example:
```bash
> java -Xmx1G -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-0.9.0-onejar.jar -gH grobid-home -exe processAuthorsHeader -s "some authors"
```

### processAuthorsCitation
`processAuthorsCitation` batch command will parse and format in TEI the authors given in input. The needed parameters for that command are:

* -gH: path to grobid-home directory

* -s: the input citation author sequence as raw string 

Example:
```bash
> java -Xmx1G -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-0.9.0-onejar.jar -gH grobid-home -exe processAuthorsCitation -s "some authors"
```

### processAffiliation
`processAffiliation` batch command will parse and format in TEI the affiliation/address given in input. The needed parameters for that command are:

* -gH: path to grobid-home directory

* -s: the input affiliation/address as raw string

Example:
```bash
> java -Xmx1G -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-0.9.0-onejar.jar -gH grobid-home -exe processAffiliation -s "some affiliation"
```

### processRawReference
`processRawReference` batch command will parse and format in TEI the raw bibliographical reference given in input. The needed parameters for that command are:

* -gH: path to grobid-home directory

* -s: the input bibliographical reference in raw text

Example:
```bash
> java -Xmx1G -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-0.9.0-onejar.jar -gH grobid-home -exe processRawReference -s "a reference string"
```

### processReferences
`processRawReference` batch command will process, extract and format in TEI all the references in the PDF files present in the directory given in input. The needed parameters for that command are:

* -gH: path to grobid-home directory

* -dIn: path to the directory of input PDF files

* -dOut: path to the output directory (if omitted the current directory)

* -r: recursive processing of files in the sub-directories (by default not recursive)

Example:
```bash
> java -Xmx2G -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-0.9.0-onejar.jar -gH grobid-home -dIn /path/to/input/directory -dOut /path/to/output/directory -exe processReferences
```

WARNING: the expected extension of the PDF files to be processed is `.pdf`

### processCitationPatentST36
`processCitationPatentST36` batch command will process, extract and format the citations in the patents encoded in ST.36 given in input. The needed parameters for that command are:

* -gH: path to grobid-home directory

* -dIn: path to the directory of input xml files

* -dOut: path to save the tei results

Example:
```bash
> java -Xmx1G -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-0.9.0-onejar.jar -gH grobid-home -dIn /path/to/input/directory -dOut /path/to/output/directory -exe processCitationPatentST36
```

WARNING: extension of the ST.36 files to be processed must be `.xml`

### processCitationPatentTXT
`processCitationPatentTXT` batch command will process, extract and format the citations in the patents encoded in UTF-8 text given in input. The needed parameters for that command are:

* -gH: path to grobid-home directory

* -dIn: path to the directory of input text files

* -dOut: path to save the tei results

Example:
```
> java -Xmx1G -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-0.9.0-onejar.jar -gH grobid-home -dIn /path/to/input/directory -dOut /path/to/output/directory -exe processCitationPatentTXT
```

WARNING: extension of the text files to be processed must be `.txt`, and expected encoding is `UTF-8`

### processCitationPatentPDF
`processCitationPatentPDF` batch command will process, extract and format the citations in the patents available in pdf given in input. The needed parameters for that command are:

* -gH: path to grobid-home directory

* -dIn: path to the directory of input pdf files

* -dOut: path to save the tei results

Example:
```
> java -Xmx1G -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-0.9.0-onejar.jar -gH grobid-home -dIn /path/to/input/directory -dOut /path/to/output/directory -exe processCitationPatentPDF
```

WARNING: extension of the text files to be processed must be `.pdf` 

### createTraining
`createTraining` batch command will generate the GROBID training data file for all the models from PDF files. The needed parameters for that command are:

* -gH: path to grobid-home directory

* -dIn: path to the directory of input pdf files

* -dOut: path to save the trained data

Example:
```bash
> java -Xmx4G -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-0.9.0-onejar.jar -gH grobid-home -dIn /path/to/input/directory -dOut /path/to/output/directory -exe createTraining
```

WARNING: the expected extension of the PDF files to be processed is `.pdf`

### createTrainingBlank
`createTrainingBlank` batch command will generate a blank GROBID training data file from PDF files, i.e. a TEI file with only text together with the default feature file. This TEI file can be used to start a new model from scratch to be applied directly to a PDF, like a high-level segmentation model. The needed parameters for that command are:

* -gH: path to grobid-home directory

* -dIn: path to the directory of input pdf files

* -dOut: path to save the trained data

Example:
```bash
> java -Xmx4G -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-0.9.0-onejar.jar -gH grobid-home -dIn /path/to/input/directory -dOut /path/to/output/directory -exe createTrainingBlank
```

WARNING: the expected extension of the PDF files to be processed is `.pdf`

### processPDFAnnotation
The batch command `processPDFAnnotation` will annotations add to the PDF. These annotations correspond to the citation information, more precisely PDF "goto" annotations for reference callout in the article text and URL link annotations for the bibliographical section (by default to the DOI registry when the DOI is recognized, to the arXiv articles when the arXiv id is recognised or to the indicated URL if present in the reference). 

The needed parameters for that command are:

* -gH: path to grobid-home directory

* -dIn: path to the directory of input PDF files

* -dOut: path to save the PDF result files

Example:
```bash
>  java -Xmx2G -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-0.9.0-onejar.jar -gH grobid-home -dIn /path/to/input/directory -dOut /path/to/output/directory -r -exe processPDFAnnotation
```

WARNING: extension of the text files to be processed must be `.pdf` 
