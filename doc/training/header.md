# Annotation guidelines for the 'header' model

## Introduction

For the following guidelines, it is expected that training data has been generated as explained [here](../Training-the-models-of-Grobid.md#generation-of-training-data).

In Grobid, the document "header" corresponds to the bibliographical/metadata information sections about the document. This is typically all the information at the beginning of the article (often called the "front", title, authors, publication information, affiliations, abstract, keywords, correspondence information, submission information, etc.), before the start of the document body (e.g. typically before the introduction section), but not only. Some of these elements can be located in the footnotes of the first page (e.g. affiliation of the authors), or at the end of the article (full list of authors, detailed affiliation and contact, how to cite, copyrights/licence and Open Access information). 

For identifying the exact pieces of information to be part of the "header" segments, see the [annotation guidelines of the segmentation model](segmentation.md). 

The following TEI elements are used by the header model:

* `<titlePart>` for the document title ([notes](#title))
* `<docAuthor>`  for the author list, including callout markers ([notes](#authors)) 
* `<affiliation>` for the authors affiliation information, including callout markers ([notes](#affiliation-and-address))
* `<address>` identifies the address elements of the affiliations ([notes](#affiliation-and-address))
* `<note type="doctype">` for indication on the document type ([notes](#document-types))
* `<div type="abstract">` for the document abstract ([notes](#abstract))
* `<keyword>` identifies the list of keywords, subject terms or classifications for the document ([notes](#keywords))
* `<reference>` identifies the reference information (how to cite) for the document that can appear in the document itself ([notes](#reference)) 
* `<email>` for the email of author or editor ([notes](#emails))
* `<editor>` for the person name information of the document editors ([notes](#editors))
* `<note type="submission">` identifies the submission/acceptance information about the document ([notes](#submission-and-peer-review-information))
* `<note type="copyright">` identifies copyrights statements (copyrights holder, waiver like CC licenses, etc.) ([notes](#copyrights))
* `<note type="funding">` identifies funding statements (grants, awards, etc.) ([notes](#funding-statements))
* `<note type="availability">` identifies data and code availability statements  ([notes](#availability-statements))    
* `<note type="contribution">` identifies the author contribution statements     
* `<note type="conflict">` identifies conflict of interest statements   
* `<idno>` for the strong identifiers of the document (DOI, arXiv identifier, PII, etc.) ([notes](#strong-identifiers))
* `<phone>` for phone number ([notes](#phone-number))
* `<page>` for identifying a page number present in the header parts (this is the first page of the document) ([notes](#page-number))
* `<note type="group">` to identify a group name (typically a working group or a collaboration) ([notes](#group-name))
* `<title level="j">` to identify the name of the journal where the article is published ([notes](#journal-titles))
* `<meeting>` to identify the meeting information associated to the publication, if relevant ([notes](#meeting-information))
* `<publisher>` for identifying mention of the publisher appearing in isolation ([notes](#publisher))

Note that the mark-up follows approximately the [TEI](http://www.tei-c.org) when used for inline encoding. 

Encoding the header section is challenging because of the variety of information that can appear in these parts, sometimes in unexpected imbricated manners. In addition, some information are often redundant (for example authors and affiliations mentioned two times with different level of details). These annotation guidelines are thus particularly important to follow to ensure stable encoding practices in the complete training data and to avoid the machine learning models to learn contradictory labelling, resulting in poorer performance and less valuable training data. 

!!! tip
    It is recommended to study the existing training documents for the header model first to see some examples of how these elements should be used.

## Analysis

The following sections provide detailed information and examples on how to handle certain typical cases.

### Space and new lines

Spaces and new line in the XNL annotated files are not significant and will be all considered by the XML parser as default separator. So it is possible to add and remove freely space characters and new lines to improve the readability of the annotated document without any impacts. 

Similarly, line break tags `<lb/>` are present in the generated XML training data, but they will be considered as a default separator by the XML parser. They are indicated to help the annotator to identify a piece of text in the original PDF if necessary. Actual line breaks are identified in the PDF and added by aligning the XML TEI with the feature file generated in parallel which contains all the PDF layout information. 


### Exclude the name of fields if it appears

It is common that abstract is introduced by a prefix `Abstract` or `Summary`, that authors are prefixed with `Authors:` or keywords by `Keywords:`. As a general principle for header annotation, all the prefix names of fields should be excluded from the annotation element and remain outside mark-ups (we only encode the "useful" content): 

```xml
    Abstract<lb/>
    <div type="abstract">Subdivision surfaces provide a curved surface representation that 
        is useful in a number of applications, in-<lb/>cluding modeling surfaces of 
        arbitrary topological type [5] , fitting scattered data [6] , and geometric 
        compression<lb/> and automatic level-of-detail generation using wavelets [8]... 
```

```xml
    Title: 
    <docTitle>
        <titlePart>PMIPv6 Integrated with MIH for Flow Mobility Management: a Real Testbed 
        with<lb/> Simultaneous Multi-Access in Heterogeneous Mobile Networks<lb/></titlePart>
    </docTitle>

    Authors:<lb/>
    • 
    <byline>
    <docAuthor>Hugo Alves</docAuthor>
    </byline>
```

```xml
    Availability and implementation<lb/> 
    <note type="availability">The implementation of UniqTag is available at<lb/> 
    https://github.com/sjackman/uniqtag<lb/> Supplementary data and code to reproduce it is 
    available at<lb/> https://github.com/sjackman/uniqtag-paper<lb/> </note>
```


### Title

Title encoding is realized following the TEI inline scheme:

```xml
    <docTitle>
        <titlePart>A linear response model of the vertical<lb/> 
        electromagnetic force on a vessel applicable<lb/> 
        to ITER and future tokamaks<lb/></titlePart>
    </docTitle>
```

Subtitles are labelled similarly as title but as an independent field. It's important to keep a break (in term of XML tagging) between the main title and possible subtitles, even if there are next to each other in the text stream. 

Running titles are not labelled at all. 

```xml
    <address>Villejuif, France<lb/></address>

    Running title: HBsAg quantification in anti-HBs positive HBV carriers<lb/>

    Abstract word count: 250<lb/> 
```

In the case of an article written in non-english language having an additional English title as translation of the original title, we annotate the English title with a tag `<note type="english-title">`.

### Authors

All mentions of the authors are labelled, including possible repetition of the authors in the correspondence section. The author information might be more detailed in the correspondence part and it will be then part of the job of Grobid to identify repeated authors and to "merge" them. 

```xml
CORRESPONDENCE<lb/> Address correspondence to
    <byline>
    <docAuthor>Andrea Z. LaCroix, PhD,</docAuthor>
    </byline>
```

As illustrated above, titles like "Ph.D.", "MD", "Dr.", etc. must be **included** in the author field. 

The only exception is when indication of authors are given around an email or a phone number. In this case we consider that the occurrence of an author name (including abbreviated names) is purely for practical reasons and should be ignored. 

```xml
Email: Calum J Maclean* -
     <email>calum.maclean@ucl.ac.uk</email>; 
```   

```xml
    *Corresponding author. Emails:
    <email>cmoser@g.harvard.edu</email>
     (C.J.M.);
    <email>sam@wjh.harvard.edu</email> 
     (S.A.M.)
```

Full job names like "Dean of...", "Research associate at..." should be excluded when possible from the tagged field, i.e. when it does not break the author sequence:

```xml
    <byline>
        <docAuthor>Peter O&apos;Shannassy</docAuthor>
    </byline> 

    (Ranger, 
```

```xml
    <byline>
        <docAuthor>A Dienel</docAuthor>
    </byline> 

    head of clinical trials department<lb/> 
```

It is important to keep markers **inside** the labelled author fields (e.g. index numbers, symbols like `*`), because they are used to associate the right affiliations to the authors.

```xml
    <byline>
    <docAuthor>Susanna L Cooke 1,2 , Jessica CM Pole 1 , Suet-Feung Chin 2 , Ian O Ellis 3 ,<lb/> Carlos Caldas 2 and Paul AW Edwards* 1<lb/></docAuthor>
    </byline>
```

### Affiliation and address

Similarly as authors, all the mentions of an affiliation are labelled, including in the correspondence parts. Grobid will have to merge appropriately redundant affiliations. It is important to keep markers **inside** the labelled fields, because they are used to associate the right affiliations to the authors.

Address are labelled with their own tag `<address>`. 

```xml
    <byline>
        <affiliation>2 Institut für Angewandte Physik, Heinrich-Heine-Universität<lb/></affiliation>
    </byline>

    <address>40225 Düsseldorf, Germany<lb/></address>
```


### Document types

Indication of document types are labelled. These indications depend on the editor, document source, etc. We consider as _document type_ the nature of the document (article, review, editorial, etc.), but also some specific aspects that can be highlighted in the presentation format, for instance indication of an "Open Access" publication expressed independently form the copyrights to characterize the document.

```xml
    <title level="j">Annals of General Hospital Psychiatry<lb/></title>

    <note type="doctype">Open Access<lb/></note>

    <note type="doctype">Primary research<lb/></note>

    <docTitle>
        <titlePart>Brain choline concentrations may not be altered in euthymic<lb/> 
            bipolar disorder patients chronically treated with either lithium or<lb/> 
            sodium valproate<lb/></titlePart>
    </docTitle>
```

### Abstract

In case of several abstracts (in particular the same abstract in difference languages), they are all labelled. 
When the article includes "Key Points" or "Highlights" section, they should also be labelled as separate abstract blocks.
In case two abstracts are adjacent in terms of text stream, they must be tagged into different abstract blocks, and not under a single `<abstract>` tag. 

!!! warning "Remember to exclude the abstract title"
    Attention must be paid to exclude functional words like "Abstract", "Summary", etc. from the labelled abstract. These are indications that will be exploited by Grobid to predict the start of the abstract but it's not something that we want to see in the final extraction result.

```xml
    Abstract. -
    <div type="abstract">The anisotropy ∆S of the thermopower in thin films of the high-Tc superconduc-<lb/>
    tor Bi2Sr2CaCu2O8 is investigated using off-c-axis epitaxial film growth and the off-diagonal<lb/> 
    Seebeck effect. The measurements represent a new method for the investigation of anisotropic<lb/> 
    transport properties in solids, taking advantage of the availability of oriented grown crystalline<lb/> 
    thin films instead of using bulk crystals. 
```

### Keywords

The `<keyword>` field covers all type of keywords, subject header, classification symbols, etc. 

Specific keyword scheme names like "PACS" or "Mathematics Subject Classification" must be included in the labelled field. 


```xml
<keyword>Mathematics Subject Classification: 83C15, 81U15, 81V80, 17B80, 81R12<lb/></keyword> 
           
<keywords type="pacs">PACS numbers: 02.30.Ik, 03.65.Fd Fd<lb/></keywords>
```

However, generic words like "Keywords", "Key words", etc. which does not bring any information about the nature of the keywords, must be excluded from the field. 

```xml
<date>17 July 2007<lb/></date>

Keywords: 
<keyword>body mass index; weight change; coronary heart disease; 
    follow-up study; Japanese Japanese<lb/></keyword>
```

### Reference

The reference field aims at identifying a text fragment describing the bibliographical reference information to be used to cite the document which is annotated. As a consequence, the reference field must contain several bibliographical information, ideally all the key information to identify the document in a unique manner following the publication standards. We typically expect here a container name (journal, proceedings name) with volume/issue/page information, possibly with a date information. 

Strong identifiers present with a reference should preferably been excluded: 

```xml
    Citation:
    <reference>Collins, D. B. G., and R. L. Bras (2008), Climatic control of 
        sediment yield in dry lands following climate and land cover<lb/> 
        change, Water Resour. Res., 44, W10405, </reference>

    <idno>doi:10.1029/2007WR006474</idno>.<lb/>
```

If the reference includes an identifier, in particular a DOI, which cannot be tagged separately without breaking the reference sequence, the identifier must be included in the reference field. For instance in the example below, the DOI is followed by the date information, it would be necessary to segment the reference into two fragments to keep the DOI as separated field, so we annotate the whole sequence as reference: 


```xml
    <reference>WATER RESOURCES RESEARCH, VOL. 44, W01433, 
        doi:10.1029/2007WR006109, 2008<lb/></reference>
```

If the title of the journal where the atticle is published appears in isolation, it is not enough to have a "reference", and the tags `<title level="j">` must be used. 


### Emails

Email must be tagged in a way that is limited to an actual email, excluding "Email" word, punctuations and person name information. 

```xml
    Email: Ren H Wu -
    <email>wurh20000@sina.com</email>; 
```   

### Editors

The name of the editor are tagged similarly as author names. Titles like "Prof.", "Dr.", "MD." are included in the field, but functional words as "Editor" or "Edited by" must be excluded. 

```xml
    Decision Editor:
    <editor>Luigi Ferrucci, MD, PhD</editor>
```

Some affiliation/address information related to the editor can follow, they are encoded with the normal affiliation and address mark-ups.

### Submission and peer review information

The `<note type="submission">` tag is used to identify, in a raw manner, the submission and peer review information present in the header parts. The date information given in this field are not further labelled. 

```xml
    <note type="submission">Received September 14, 2009; Revised September 29, 2009; Accepted September 30, 2009</note>
```

Be careful not to include publication date information under this block, the publication date needs to be encoded with a specific `<date>` element. 

### Copyrights

`<note type="copyright">` is used to identify passages about the copyrights holders of the document and any relevant licensing information, in particular CC licenses. Email and URL present in this section must not be further tagged:

```xml
    <note type="copyright">© 2014 The Author(s). Published by Taylor &amp; Francis.<lb/> 
    This is an Open Access article distributed under the terms of the Creative 
    Commons Attribution-NonCommercial-NoDerivatives<lb/> 
    License (http://creativecommons.org/licenses/by-nc-nd/4.0/), which permits 
    non-commercial reuse, distribution, and reproduc-<lb/>
    tion in any medium, provided the original work is properly cited, and is not 
    altered, transformed, or built upon in any way.</note>
```

### Strong identifiers

`<idno>` is used to identify strong identifiers of the document, in particular DOI, PII, ISSN, ISBN and the major Open Access repository identifiers - arXiv identifiers, HAL ID, ...  

We do not tag report numbers, the identifiers here must have a global level of acceptance beyond a local source of identification. 

The identifier name is kept with the identifier value so that Grobid can classify more easily the type of identifier:

```xml
<idno>PII S0090-3019(97)00159-6</idno> 
```

```xml
<idno>DOI 10.1186/s12889-015-2574-8<lb/></idno> 
```

```xml
<idno>ISSN 1356-1294<lb/></idno>
```
In the case of DOI, the identifier might look like a URL, but should be encoded with `<idno>`:

```xml
<idno>http://dx.doi.org/10.1097/MD.0000000000028156<lb/></idno>
```    

There is no need to specify the type of strong identifier (it will be inferred by pattern matching).

### Phone number

We label phone number, including international prefix symbols, but not fax numbers. Punctuation and words like "phone", "telephone", etc. must be excluded from the label field. 

```xml
    <address>Box 457, SE 405 30<lb/> Göteborg, Sweden.</address>

    Tel.: <phone>+46 31 7866104</phone>.<lb/> 

    E-mail address: <email>eva.brink@gu.se</email>
```

### Page number

If a page number appears in the header part, it is identified with the `<page>` tag. The page number must stand in isolation, not part of a reference information (it will then be labelled as part of the reference under a `<reference>` tag). If a total page number is associated with the page number, it is also encoded: 

```xml
<page>1 / 13<lb/></page>
```

### Group name

In contrast to affiliation, a group correspond to a temporary association of persons and/or institutions for a given work. Group name include working groups for standards, for experiments, for particular collaborative study, review or work, or larger "collaboration" as we see in Big Science efforts like astronomy or High Energy Particules. 

In general, group names are introduced as such, in a distinctive manner from affiliations (which are usually also associated to a physical address).

```xml
    , for the 
    <note type="group">JPHC Study Group</note>
```

### Journal titles

In case the name of the journal appears alone in the header part, not part of a reference, it is tagged specifically.

```xml
    <lb/>
    <title level="j">EUROPHYSICS LETTERS<lb/></title>
     
    <date>1 October 1997<lb/></date>
```

If the journal title appears as part of a reference (e.g. "how to cite"), it is then part of the `<reference>` element.

### Meeting information

Publications can be associated to a particular meeting event, in particular a conference or a working event for the development of standards. In this case, the meeting information, usually covering a location and dates, and optionally a meeting even name, are labelled in a raw manner with the `<meeting>` tag. Dates and location/address should not be further labelled and detailed. 

```xml
    Presented at the
    <meeting>Annual Meeting of the American Society of An-<lb/>
    drology, Baltimore, Maryland, February 1997.<lb/></meeting>
```

```xml
    <meeting>12TH INTERNATIONAL SYMPOSIUM ON FLOW VISUALIZATION<lb/> 
    September 10-14, 2006, German Aerospace Center (DLR), Göttingen, Germany<lb/>
    </meeting>
```  

If the meeting information is part of a larger reference (e.g. definition the citation information of the Proceedings of a conference where the article is published), then it has to be labelled also as `<reference>` (this is similar to the journal title case just above). For instance, in the following example, pages are indicated and we refer to the container of the article and not just to a meeting. 

```xml
    In:
    <reference>Proceedings of CoNLL-2000 and LLL-2000, pages 154-156, Lisbon, Portugal, 2000.<lb/></reference>
     
```


### Publisher

Name of the publisher might appear in isolation in the header. It is then labelled with the `<publisher>` tag.

```xml
<front>
     
    <publisher>IOP PUBLISHING</publisher>
  
    <title level="j">PLASMA PHYSICS AND CONTROLLED FUSION<lb/> </title>
```

Note that this tag must only be used when no other tag can be applied. In particular, if the publisher name appears in a reference, it is labelled inside the reference tags. If the publisher name appears in the copyright statement, it is labelled inside the copyright mark-up. In practice, a publisher name in isolation in a header is not frequent.
 

### Funding statements

Some indication about the funding of the research work presented in a paper sometimes appear within the header. We mark the whole raw statement under `<note type="funding">` tag. The statement can include related disclosure information: 

```xml
    Funding: 
    <note type="funding">This work is supported in part by ARPA and Philips Labs under contract DASG60-92-0055 to Department<lb/> 
    of Computer Science, University of Maryland, and by National Science Foundation Grant No. NCR 89-04590. The<lb/> 
    views, opinions, and/or ndings contained in this report are those of the author(s) and should not be interpreted as<lb/> 
    representing the o cial policies, either expressed or implied, of the Advanced Research Projects Agency, PL, NSF,<lb/> 
    or the U.S. Government. Computer facilities were provided in part by NSF grant CCR-8811954.</note> 
```

### Availability statements

It happens that data and/or code availability statements are part of the header. Such a statement is marked with a `<note type="availability">` element.

```xml
Data Availability Statement: 
    <note type="availability">Data are available<lb/> from Figshare at https://figshare.com/s/<lb/> 6c396e16f3991d7eaa00 
    and under the DOI: 10.<lb/> 6084/m9.figshare.5917225.<lb/></note>
```

