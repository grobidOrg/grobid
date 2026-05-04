<h1>TEI customisation</h1>

## Motivation

Using tabular or POJO representations is adapted to bibliographical records, which are relatively flat and simple structure. However, the goal of Grobid is complete PDF document ingestion. In other terms, Grobid aims at extracting and restructuring the complete full text of a document. 

Representing a complete document requires a much more sophisticated data model. A document model has to cover multiple embedded/recursive sections, reference mechanisms, lists, etc. Derived from SGML, XML has been designed exactly for that. Among the different XML standards, the [TEI (Text Encoding Initiative)](<http://www.tei-c.org>) is probably the most comprehensive and mature encoding standard for machine-readable texts. TEI allows very precise text annotations while maintaining human readability. The Guidelines define some 500 different textual components and concepts (word, sentence, character, glyph, person, etc.), defined and refined over the last 25 years. 

The TEI appears therefore as a suitable document model and XML implementation for the Grobid structured document extraction.


## The schemas

Any usages of the TEI for a particular sort of documents suppose a customization of the TEI to their specificities. The TEI define a very large and rich set of encoding possibilities, often redundant taken as a whole. A customization adapts and constraints the richness of the TEI to a well scoped and tuned schema. To quote the TEI Guidelines: "it is almost impossible to use the TEI scheme without customizing or personalizing it in some way."

For customisation, the TEI community has created a specification language called [ODD ("One Document Does it all")](<http://www.tei-c.org/Guidelines/Customization/odds.xml>) to modify the general TEI schema. Having ODD descriptions, it is possible with a tool called [Roma](<http://www.tei-c.org/Roma>) to generate automatically customised TEI schemas (xsd, relaxNG, etc.) and some documentation. 

The Grobid ODD specifications can be found under `grobid-home/schemas/odd/Grobid.odd`.

Different schemas generated from the ODD specifications by ROMA are available in the repository:

* XML RelaxNG: `grobid-home/schemas/rng/Grobid.rng`

* Compact notation RelaxNG: `grobid-home/schemas/rng/Grobid.rnc`

* DTD, `grobid-home/schemas/dtd/Grobid.dtd`

* W3C XML schemas, under `grobid-home/schemas/xsd`

A documentation of Grobid customized TEI is available `grobid-home/schemas/doc/Grobid_doc.html`. 

We recommend to use the RelaxNG schemas, more robust, easier to use, etc., and avoid W3C schemas which are recurrent sources of problems... 

## Well-formedness and validation

Grobid ensures that all the generated results are well-formed XML. It is actually not always straightforward to achieve 100% well-formedness because the PDF input documents can be very difficult to parse, the content very noisy, not always fully recoverable, and the Grobid parsing can include many errors. In case you observe a result from Grobid not well-formed, do not hesitate to report an issue. This error should be very rare, we tested recently Grobid on around 300k PDF from the HAL research archive and all the resulting TEI documents were well-formed. 

The very large majority of Grobid TEI results will validate against the provided schemas. At this stage, we considered that 100% valid results might not be desirable, because validation is a way to detect that a document is particularly problematic. With a fully automated extraction and restructuring of unconstrained PDF full texts, a variety of unexpected structural errors can occur. A very small amount of training data is currently used by the Grobid full text model, so inconsistent structures are sometimes predicted. Failure of validation is viewed here as a way to spot important failure of Grobid, and thus to indicate the need of filtering or further process. 


## Binding with JAXB 2.0

Developers might be interested in generating Java bindings for XML with [JAXB 2.0](<https://jaxb.java.net>) in order to avoid writing an XML parser for GROBID's results. It appears that the XSD schema for TEI in general is not compiling out-of-the-box with JAXB - unfortunately as many real world complex XML schemas such as MathML. 

However, it is possible to generate the Java classes by using a better binding algorithm than the default one defined by the JAXB specifications, as explained in the following [blog entry](<https://weblogs.java.net/blog/kohsuke/archive/2006/03/simple_and_bett.html>). Using the provided extension with JAXB version 2.0 minimum, the Java classes will be generated without conflicts. 

    > xjc -d generated -extension ~/grobid/grobid-home/schemas/xsd/simpleMode.xsd ~/grobid/grobid-home/schemas/xsd/Grobid.xsd
	parsing a schema...
	compiling a schema...
	org/tei_c/ns/_1/Abstract.java
	org/tei_c/ns/_1/AddrLine.java
	org/tei_c/ns/_1/Address.java
	org/tei_c/ns/_1/Affiliation.java
	org/tei_c/ns/_1/Analytic.java
	org/tei_c/ns/_1/Anchor.java
	org/tei_c/ns/_1/Author.java
	org/tei_c/ns/_1/Availability.java
	org/tei_c/ns/_1/Back.java
	
	etc.
	
These generated Java classes can be used to parse automatically TEI results produced by GROBID (successful usage of these Java bindings have been reported [here](<https://github.com/kermitt2/grobid/issues/113#issuecomment-239162316>)). 
