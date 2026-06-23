# Annotation guidelines for bibliographical references

## Introduction

This section describes how to annotate training data for the __citation__ model. This model parses a bibliographical reference in isolation (as typically present in the bibliographical section at the end of an article).

In the model cascading sequence, a bibliographical reference in isolation is obtained from the model _reference segmenter_ which aims at segmenting a 
bibliographical section into a list of individual bibliographical references. 

Note that this mark-up for training data for bibliographical references follows overall the [TEI](http://www.tei-c.org). 

## Analysis

The complete bibliographical reference is enclosed in a `<bibl>` element. One `<bibl>` structure must match with exactly one bibliographical reference.
Then the following tags are used for structuring the bibliographical references:

* `<author>` for the complete sequence of authors. One tag pair can enclose multiple author names. Authors should be human names; for projects, organizations, collaborations, or companies, see the alternatives below.

* `<orgName type="collaboration">` a collaboration is a project-based grouping of authors from different affiliations limited in time. Some examples: In high energy particules the [ATLAS](https://atlas.cern/), [CMS](http://cms.web.cern.ch/content/cms-collaboration) and [DELPHI](http://delphiwww.cern.ch/) collaborations (the current world record holder for number of authorship [arXiv:1503.07589](http://arxiv.org/abs/1503.07589) combines the two CERN collaborations ATLAS and CMS), or the [LUNA](https://luna.lngs.infn.it/) collaboration in astrophysics. The particularity of collaborations is to be used both as _authorship_ and _affiliation_ component.

* `<title level="a">` for article title or chapter title (but not thesis, see below). Here "a" stands for analytics (a part of a monograph)

* `<title level="j">` for journal title

* `<title level="s">` for series title (e.g. "Lecture Notes in Computer Science")

* `<title level="m">` for non journal bibliographical item holding the cited article, e.g. _conference proceedings_ title. Note if a book is cited, the title of the book is annotated with `<title level="m">`. If a thesis is cited, the title of the thesis is annotated with `<title level="m">`, and the type of thesis as `<note type="report">`. If a webpage is cited with just the page title, the title is `<title level="m">`. If a webpage is cited as part of a website, the name of the website itself is annotated with `<title level="m">`, and the page title as `<title level="a">`. Here `m` stands for monograph.

* `<date>` the date sequence (excluding parenthesis, etc.)

* `<biblScope unit="page">` the full range of pages of the article. `unit="page"` is preferred over the legacy `unit="pp"`.

* `<biblScope unit="volume">` the value of the volume (e.g. `vol. <biblScope unit="volume">7</biblScope>,`)

* `<biblScope unit="issue">` the value of the issue, also known as number, (e.g. `no. <biblScope type="issue">3</biblScope>,`)

* `<orgName>` the institution for theses or technical reports

* `<publisher>` the name of the publisher. Also used instead of `<author>` for things like web pages or reports created by a business, for example "Google Inc". If "Wikipedia" is placed in the author position, use `<publisher>` instead of `<author>`.

* `<pubPlace>` publication place, or location of the "publishing" institution

* `<editor>` for a sequence of editors (single tag pair can enclose one or more editor names)

* `<ptr type="web">` for web URL. Do not include prefixes like `URL:` or suffixes like a period or `Accessed:`.

* `<idno>` for the document-specific identifier, in particular DOI and arXiv identifiers, optionally the type of identifier can be given by the attribute `@type`, for instance, `<idno type="DOI">...</idno>`, `<idno type="arXiv">..</idno>` (`type="arxiv"` is also allowed). All components of the identifier should be enclosed in the tag, including prefixes like `DOI=` and `arxiv:`, or suffixes like `[cs.LG]`. For report identifiers `<idno type="report">...</idno>` is used 

* `<note type="report">` in the case of technical report at large, encode the indication of the kind of report, this includes "technical report" from an institution, but also the kind of thesis ("Ph.D. thesis", "M.Sc. thesis", etc.)

* `<note>` for any indications related to the reference and not covered by one of the previous tags (for instance "personal communication")

Additional text/characters that do not belong to one of these elements (punctuations, syntactic sugar, etc.) has to be be left untagged under the `<bibl>` elements. This is the case for instance for the tag `<date>`, the characters such as parenthesis have to be put outside this element (see the example below).

Example: _Biostatistics (2008), 9(2), pp. 234–248_

```xml
<?xml version="1.0" encoding="UTF-8"?>
<tei xmlns="http://www.tei-c.org/ns/1.0" 
	  xmlns:xlink="http://www.w3.org/1999/xlink" 
	  xmlns:mml="http://www.w3.org/1998/Math/MathML">

	<listBibl>
		<bibl>
			<title level="j">Biostatistics</title> (<date>2008</date>), 
			<biblScope type="vol">9</biblScope>(<biblScope type="issue">2</biblScope>), 
			pp. <biblScope type="page">234–248</biblScope>
    	</bibl>
	</listBibl>

</tei>

```

Example: _Microsoft. 2019. Azure REST API Specifications. https://github.com/Azure/azurerest-api-specs Last accessed 2019-11-22._

```xml
<?xml version="1.0" encoding="UTF-8"?>
<tei xmlns="http://www.tei-c.org/ns/1.0" 
	  xmlns:xlink="http://www.w3.org/1999/xlink" 
	  xmlns:mml="http://www.w3.org/1998/Math/MathML">

	<listBibl>
		<bibl>
            <publisher>Microsoft</publisher>. <date>2019</date>.
            <title level="m">Azure REST API Specifications</title>.
            <ptr type="web">https://github.com/Azure/azurerest-api-specs</ptr> Last accessed <date>2019-11-22</date>.
    	</bibl>
	</listBibl>

</tei>

```

### Special case with years

In case a letter is added to year, for instance following the _Harvard_ bibliographical reference style: 

> Gavazzi G., Piertini D., Boselli A., Tuffs R., __1996c__, A&AS, 120, 489(Paper I) 

with _(Gavazzi et al, 1996c)_ used to reference the citation in the full text. 

The year with letter are tagged together: 

```xml
<listBibl>
	<bibl>
		<author>Gavazzi G., Piertini D., Boselli A., Tuffs R.</author>, <date>1996c</date>, 
		<title level="j">A&amp;AS</title>, <biblScope unit="volume">120</biblScope>, 
		<biblScope unit="page">489</biblScope>(<note>Paper I</note>) 
	</bibl>
</listBibl>
```

### No segmentation between fields

In examples like the following one:

>  _D. Foo, P. Bar, Phys. Rev. __D95__, 34(2017)_

there is no spacing between the title of the journal `Phys. Rev. D` and the volume `95`. For annotating this case, we tag these two fields without separation (not introducing a space or end-of-line between *D* and *95*): 

```xml
<bibl>
	<author>D. Foo, P. Bar</author>, 
	<title level="j">Phys. Rev. D</title><biblScope unit="volume">95</biblScope>, 
	<biblScope unit="page">34</biblScope>(<date>2017</date>)
</bibl>
```

### Identifiers

In the case of identifiers, we expect the following identifier types to be indicated: `DOI`, `PMID`, `PMC` (for PMC ID), `ISSN`, `PII`, `ISBN` and `arxiv` (in lowercase). Identifier for technical reports are identicated as type `report` (e.g. `<idno type="report">`).

As a principle for identifiers, the identifier type string/prefix must be kept in the tagged field (except when it's impossible, for instance due to a formatting error in the PDF). This will help to disambiguate the recognized identifiers when the trained models are labelling new content. For example, see the different `<idno>` in the following reference:

```xml
<bibl>
	<author>Dillman, Allissa A et al.</author> (<date>2013</date>). 
	"<title level="a">mRNA expression, splicing and editing in the embryonic and adultmouse cerebral cortex</title>". eng. 
	In :<title level="j">Nature neuroscience</title> <biblScope unit="volume">16</biblScope>. 
	<biblScope unit="issue">4</biblScope>. <idno type="PMID">PMID :23416452</idno> 
	<idno type="PMC">PMCID: PMC3609882</idno>, pp. <biblScope unit="page">499-506</biblScope>. 
	<idno type="ISSN">issn: 1546- 1726</idno>. 
	<idno type="DOI">doi: 10. 1038/nn.3332</idno>.
</bibl>
```
