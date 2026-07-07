/*
 * Copyright 2008-2026 GROBID contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grobid.core.data

import org.grobid.core.engines.config.GrobidAnalysisConfig
import org.grobid.core.main.LibraryLoader
import org.grobid.core.utilities.Consolidation
import org.grobid.core.utilities.GrobidProperties
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import java.util.*
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

class BiblioItemTest {
    @Before
    @Throws(Exception::class)
    fun setUp() {
        LibraryLoader.load()
    }

    private val configBuilder = (GrobidAnalysisConfig.GrobidAnalysisConfigBuilder())

    @Test
    @Throws(Exception::class)
    fun shouldGenerateRawAffiliationTextIfEnabled() {
        val config = configBuilder.includeRawAffiliations(true).build()
        val aff = Affiliation()
        aff.setRawAffiliationString("raw affiliation 1")
        aff.setFailAffiliation(false)
        val author = Person()
        author.setLastName("Smith")
        author.setAffiliations(Arrays.asList<Affiliation?>(aff))
        val biblioItem = BiblioItem()
        biblioItem.setFullAuthors(Arrays.asList<Person?>(author))
        biblioItem.setFullAffiliations(Arrays.asList<Affiliation?>(aff))
        val tei = biblioItem.toTEI(0, 2, config)
        LOGGER.debug("tei: {}", tei)
        val doc: Document? = parseXml(tei)
        Assert.assertThat<MutableList<String?>?>(
            "raw_affiliation",
            getXpathStrings(doc, "//note[@type=\"raw_affiliation\"]/text()"),
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>("raw affiliation 1")),
        )
    }

    @Test
    @Throws(Exception::class)
    fun shouldIncludeMarkerInRawAffiliationText() {
        val config = configBuilder.includeRawAffiliations(true).build()
        val aff = Affiliation()
        aff.setMarker("A")
        aff.setRawAffiliationString("raw affiliation 1")
        aff.setFailAffiliation(false)
        val author = Person()
        author.setLastName("Smith")
        author.setAffiliations(Arrays.asList<Affiliation?>(aff))
        val biblioItem = BiblioItem()
        biblioItem.setFullAuthors(Arrays.asList<Person?>(author))
        biblioItem.setFullAffiliations(Arrays.asList<Affiliation?>(aff))
        val tei = biblioItem.toTEI(0, 2, config)
        LOGGER.debug("tei: {}", tei)
        val doc: Document? = parseXml(tei)
        Assert.assertThat<MutableList<String?>?>(
            "raw_affiliation label",
            getXpathStrings(doc, "//note[@type=\"raw_affiliation\"]/label/text()"),
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>("A")),
        )
        Assert.assertThat<MutableList<String?>?>(
            "raw_affiliation",
            getXpathStrings(doc, "//note[@type=\"raw_affiliation\"]/text()"),
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>(" raw affiliation 1")),
        )
    }

    @Test
    @Throws(Exception::class)
    fun shouldIncludeEscapedMarkerInRawAffiliationText() {
        val config = configBuilder.includeRawAffiliations(true).build()
        val aff = Affiliation()
        aff.setMarker("&")
        aff.setRawAffiliationString("raw affiliation 1")
        aff.setFailAffiliation(false)
        val author = Person()
        author.setLastName("Smith")
        author.setAffiliations(Arrays.asList<Affiliation?>(aff))
        val biblioItem = BiblioItem()
        biblioItem.setFullAuthors(Arrays.asList<Person?>(author))
        biblioItem.setFullAffiliations(Arrays.asList<Affiliation?>(aff))
        val tei = biblioItem.toTEI(0, 2, config)
        LOGGER.debug("tei: {}", tei)
        val doc: Document? = parseXml(tei)
        Assert.assertThat<MutableList<String?>?>(
            "raw_affiliation label",
            getXpathStrings(doc, "//note[@type=\"raw_affiliation\"]/label/text()"),
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>("&")),
        )
        Assert.assertThat<MutableList<String?>?>(
            "raw_affiliation",
            getXpathStrings(doc, "//note[@type=\"raw_affiliation\"]/text()"),
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>(" raw affiliation 1")),
        )
    }

    @Test
    @Throws(Exception::class)
    fun shouldGenerateRawAffiliationTextForFailAffiliationsIfEnabled() {
        val config = configBuilder.includeRawAffiliations(true).build()
        val aff = Affiliation()
        aff.setRawAffiliationString("raw affiliation 1")
        aff.setFailAffiliation(true)
        val biblioItem = BiblioItem()
        biblioItem.setFullAffiliations(Arrays.asList<Affiliation?>(aff))
        val tei = biblioItem.toTEI(0, 2, config)
        LOGGER.debug("tei: {}", tei)
        val doc: Document? = parseXml(tei)
        Assert.assertThat<MutableList<String?>?>(
            "raw_affiliation",
            getXpathStrings(doc, "//note[@type=\"raw_affiliation\"]/text()"),
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>("raw affiliation 1")),
        )
    }

    @Test
    @Throws(Exception::class)
    fun shouldNotGenerateRawAffiliationTextIfNotEnabled() {
        val config = configBuilder.includeRawAffiliations(false).build()
        val aff = Affiliation()
        aff.setRawAffiliationString("raw affiliation 1")
        val author = Person()
        author.setLastName("Smith")
        author.setAffiliations(Arrays.asList<Affiliation?>(aff))
        val biblioItem = BiblioItem()
        biblioItem.setFullAuthors(Arrays.asList<Person?>(author))
        biblioItem.setFullAffiliations(Arrays.asList<Affiliation?>(aff))
        val tei = biblioItem.toTEI(0, 2, config)
        LOGGER.debug("tei: {}", tei)
        val doc: Document? = parseXml(tei)
        Assert.assertThat<MutableList<String?>?>(
            "raw_affiliation",
            getXpathStrings(doc, "//note[@type=\"raw_affiliation\"]/text()"),
            CoreMatchers.`is`<MutableCollection<out String?>?>(Matchers.empty<String?>()),
        )
    }

    @Test
    fun injectIdentifiers() {
        val item1 = BiblioItem()
        item1.setDOI("10.1233/23232/3232")
        item1.setPMID("pmid")
        item1.setPMCID("bao")
        item1.setPII("miao")
        item1.setIstexId("zao")
        item1.setArk("Noah!")

        val item2 = BiblioItem()
        BiblioItem.injectIdentifiers(item2, item1)

        Assert.assertThat<String?>(item2.getDOI(), CoreMatchers.`is`<String?>("10.1233/23232/3232"))
        Assert.assertThat<String?>(item2.getPMID(), CoreMatchers.`is`<String?>("pmid"))
        Assert.assertThat<String?>(item2.getPMCID(), CoreMatchers.`is`<String?>("bao"))
        Assert.assertThat<String?>(item2.getPII(), CoreMatchers.`is`<String?>("miao"))
        Assert.assertThat<String?>(item2.getIstexId(), CoreMatchers.`is`<String?>("zao"))
        Assert.assertThat<String?>(item2.getArk(), CoreMatchers.`is`<String?>("Noah!"))
    }

    @Test
    @Throws(Exception::class)
    fun shouldEscapeIdentifiers() {
        val item1 = BiblioItem()
        item1.setJournal("Dummy Journal Title")
        item1.setDOI("10.1233/23232&3232")
        item1.setPMID("pmid & 123")
        item1.setArk("Noah & !")
        item1.setISSN("0974&9756")

        val config = configBuilder.build()
        val tei = item1.toTEI(0, 2, config)
        LOGGER.debug("tei: {}", tei)
        val doc: Document? = parseXml(tei)
        Assert.assertThat<MutableList<String?>?>(
            "DOI",
            getXpathStrings(doc, "//idno[@type=\"DOI\"]/text()"),
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>("10.1233/23232&3232")),
        )
        Assert.assertThat<MutableList<String?>?>(
            "ISSN",
            getXpathStrings(doc, "//idno[@type=\"ISSN\"]/text()"),
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>("0974&9756")),
        )
        Assert.assertThat<MutableList<String?>?>(
            "PMID",
            getXpathStrings(doc, "//idno[@type=\"PMID\"]/text()"),
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>("pmid&123")),
        )
        Assert.assertThat<MutableList<String?>?>(
            "Ark",
            getXpathStrings(doc, "//idno[@type=\"ark\"]/text()"),
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>("Noah & !")),
        )
    }

    @Test
    fun correct_empty_shouldNotFail() {
        BiblioItem.correct(BiblioItem(), BiblioItem())
    }

    @Test
    fun correct_1author_shouldWork() {
        val biblio1 = BiblioItem()
        var authors: MutableList<Person?> = mutableListOf()
        authors.add(createPerson("John", "Doe"))
        biblio1.setFullAuthors(authors)

        val biblio2 = BiblioItem()
        authors = mutableListOf()
        authors.add(createPerson("John1", "Doe"))
        biblio2.setFullAuthors(authors)

        BiblioItem.correct(biblio1, biblio2)

        Assert.assertThat<String?>(
            biblio1.getFirstAuthorSurname(),
            CoreMatchers.`is`<String?>(biblio2.getFirstAuthorSurname()),
        )
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getFirstName(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(0).getFirstName()),
        )
    }

    @Test
    fun correct_2authors_shouldMatchFullName_shouldUpdateAffiliation() {
        val biblio1 = BiblioItem()
        var authors: MutableList<Person?> = mutableListOf()
        authors.add(createPerson("John", "Doe"))
        authors.add(createPerson("Jane", "Will"))
        biblio1.setFullAuthors(authors)

        val biblio2 = BiblioItem()
        authors = mutableListOf()
        authors.add(createPerson("John", "Doe", "UCLA"))
        authors.add(createPerson("Jane", "Will", "Harvard"))
        biblio2.setFullAuthors(authors)

        BiblioItem.correct(biblio1, biblio2)

        Assert.assertThat<String?>(
            biblio1.getFirstAuthorSurname(),
            CoreMatchers.`is`<String?>(biblio2.getFirstAuthorSurname()),
        )
        Assert.assertThat<MutableList<Person?>?>(biblio1.getFullAuthors(), Matchers.hasSize<Person?>(2))
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getFirstName(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(0).getFirstName()),
        )
        // biblio1 affiliations empty we update them with ones from biblio2
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString()),
        )
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(1).getFirstName(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(1).getFirstName()),
        )
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString()),
        )
    }

    @Test
    fun correct_2authors_shouldMatchFullName_shouldKeepAffiliation() {
        val biblio1 = BiblioItem()
        var authors: MutableList<Person?> = mutableListOf()
        authors.add(createPerson("John", "Doe", "Stanford"))
        authors.add(createPerson("Jane", "Will", "Cambridge"))
        biblio1.setFullAuthors(authors)

        val biblio2 = BiblioItem()
        authors = mutableListOf()
        authors.add(createPerson("John", "Doe"))
        authors.add(createPerson("Jane", "Will", "UCLA"))
        biblio2.setFullAuthors(authors)

        BiblioItem.correct(biblio1, biblio2)

        Assert.assertThat<String?>(
            biblio1.getFirstAuthorSurname(),
            CoreMatchers.`is`<String?>(biblio2.getFirstAuthorSurname()),
        )
        Assert.assertThat<MutableList<Person?>?>(biblio1.getFullAuthors(), Matchers.hasSize<Person?>(2))
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getFirstName(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(0).getFirstName()),
        )
        // biblio1 affiliations not empty, we keep biblio1 as is
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio1.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString()),
        )
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(1).getFirstName(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(1).getFirstName()),
        )
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio1.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString()),
        )
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString()),
        )
    }

    @Test
    fun correct_2authors_initial_2_shouldUpdateAuthor() {
        val biblio1 = BiblioItem()
        var authors: MutableList<Person?> = mutableListOf()
        authors.add(createPerson("John", "Doe", "ULCA"))
        authors.add(createPerson("J", "Will", "Harward"))
        biblio1.setFullAuthors(authors)

        val biblio2 = BiblioItem()
        authors = mutableListOf()
        authors.add(createPerson("John1", "Doe", "Stanford"))
        authors.add(createPerson("Jane", "Will", "Berkeley"))
        biblio2.setFullAuthors(authors)

        BiblioItem.correct(biblio1, biblio2)

        Assert.assertThat<String?>(
            biblio1.getFirstAuthorSurname(),
            CoreMatchers.`is`<String?>(biblio2.getFirstAuthorSurname()),
        )
        Assert.assertThat<MutableList<Person?>?>(biblio1.getFullAuthors(), Matchers.hasSize<Person?>(2))
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getFirstName(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(0).getFirstName()),
        )
        // affiliation should be kept though since not empty
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio1.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString()),
        )
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(1).getFirstName(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(1).getFirstName()),
        )
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio1.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString()),
        )
    }

    @Test
    fun correct_2authors_initial_shouldUpdateAuthor() {
        val biblio1 = BiblioItem()
        var authors: MutableList<Person?> = mutableListOf()
        authors.add(createPerson("John", "Doe", "ULCA"))
        authors.add(createPerson("Jane", "Will", "Harvard"))
        biblio1.setFullAuthors(authors)

        val biblio2 = BiblioItem()
        authors = mutableListOf()
        authors.add(createPerson("John1", "Doe", "Stanford"))
        authors.add(createPerson("J", "Will", "Berkeley"))
        biblio2.setFullAuthors(authors)

        BiblioItem.correct(biblio1, biblio2)

        Assert.assertThat<String?>(
            biblio1.getFirstAuthorSurname(),
            CoreMatchers.`is`<String?>(biblio2.getFirstAuthorSurname()),
        )
        Assert.assertThat<MutableList<Person?>?>(biblio1.getFullAuthors(), Matchers.hasSize<Person?>(2))
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getFirstName(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(0).getFirstName()),
        )
        // affiliation should be kept though
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio1.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString()),
        )
        // assertThat(biblio1.getFullAuthors().get(1).getFirstName(), is(biblio2.getFullAuthors().get(0).getFirstName()));
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio1.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString()),
        )
    }

    @Test
    fun correct_2authors_shouldPreservePdfOrcid_whenCrossrefHasNone() {
        // CrossRef result (bibo) has no ORCIDs
        val biblio1 = BiblioItem()
        var authors: MutableList<Person?> = mutableListOf()
        authors.add(createPerson("John", "Doe"))
        authors.add(createPerson("Jane", "Will"))
        biblio1.setFullAuthors(authors)

        // PDF-extracted (bib) has ORCIDs from PDF annotations
        val biblio2 = BiblioItem()
        authors = mutableListOf()
        val pdfAuthor1 = createPerson("John", "Doe")
        pdfAuthor1.setORCID("0000-0001-2345-6789")
        authors.add(pdfAuthor1)
        val pdfAuthor2 = createPerson("Jane", "Will")
        pdfAuthor2.setORCID("0000-0002-3456-7890")
        authors.add(pdfAuthor2)
        biblio2.setFullAuthors(authors)

        BiblioItem.correct(biblio2, biblio1)

        // PDF-extracted ORCIDs should be preserved
        Assert.assertThat(biblio2.getFullAuthors().get(0).getORCID(), CoreMatchers.`is`("0000-0001-2345-6789"))
        Assert.assertThat(biblio2.getFullAuthors().get(1).getORCID(), CoreMatchers.`is`("0000-0002-3456-7890"))
    }

    @Test
    fun correct_2authors_shouldPreserveAffiliationByPosition_whenNameMatchFails() {
        val extractedBiblio = BiblioItem()
        var authors: MutableList<Person?> = mutableListOf()
        authors.add(createPerson("Barreiro", "FH", "CERN"))
        authors.add(createPerson("Jane", "Will", "Harvard"))
        extractedBiblio.setFullAuthors(authors)

        val crossrefBiblio = BiblioItem()
        authors = mutableListOf()
        authors.add(createPerson("F H", "Barreiro"))
        authors.add(createPerson("Jane", "Will"))
        crossrefBiblio.setFullAuthors(authors)

        BiblioItem.correct(extractedBiblio, crossrefBiblio)

        Assert.assertThat(extractedBiblio.getFullAuthors(), Matchers.hasSize<Person?>(2))
        Assert.assertThat(extractedBiblio.getFullAuthors().get(0).getFirstName(), CoreMatchers.`is`("F H"))
        Assert.assertThat(extractedBiblio.getFullAuthors().get(0).getLastName(), CoreMatchers.`is`("Barreiro"))
        Assert.assertThat(
            extractedBiblio.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`("CERN"),
        )
        Assert.assertThat(
            extractedBiblio.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`("Harvard"),
        )
    }

    @Test
    fun correct_2authors_shouldNotReassignAffiliationByPosition_whenExtractedAlreadyNameMatched() {
        // Lists are the same size but in a different order: the CrossRef author at position 0
        // has no counterpart in the PDF, while the extracted author at position 0 was already
        // consumed by a name match at another position. The positional fallback must NOT hand
        // that already-claimed affiliation to the unrelated CrossRef author.
        val extractedBiblio = BiblioItem()
        var authors: MutableList<Person?> = mutableListOf()
        authors.add(createPerson("Jane", "Will", "Harvard"))
        authors.add(createPerson("Foo", "Bar", "SomeAff"))
        extractedBiblio.setFullAuthors(authors)

        val crossrefBiblio = BiblioItem()
        authors = mutableListOf()
        authors.add(createPerson("Xyz", "Newauthor"))
        authors.add(createPerson("Jane", "Will"))
        crossrefBiblio.setFullAuthors(authors)

        BiblioItem.correct(extractedBiblio, crossrefBiblio)

        Assert.assertThat(extractedBiblio.getFullAuthors(), Matchers.hasSize<Person?>(2))
        // The unrelated CrossRef author keeps no affiliation instead of inheriting "Harvard"
        Assert.assertThat(extractedBiblio.getFullAuthors().get(0).getLastName(), CoreMatchers.`is`("Newauthor"))
        Assert.assertTrue(extractedBiblio.getFullAuthors().get(0).getAffiliations().isNullOrEmpty())
        // The name-matched author still receives its affiliation from the PDF
        Assert.assertThat(
            extractedBiblio.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`("Harvard"),
        )
    }

    @Test
    fun correct_2authors_shouldPreserveAffiliationBlocksAndMarkersByPosition_whenNameMatchFails() {
        // The positional fallback carries over all three affiliation-related fields, not just the
        // affiliation list: affiliationBlocks and affiliationMarkers must survive as well.
        val extracted = Person()
        extracted.setFirstName("Barreiro")
        extracted.setLastName("FH")
        val aff = Affiliation()
        aff.setAffiliationString("CERN")
        extracted.setAffiliations(mutableListOf(aff))
        extracted.setAffiliationBlocks(mutableListOf("CERN, Geneva"))
        extracted.setAffiliationMarkers(mutableListOf("1"))

        val extractedBiblio = BiblioItem()
        extractedBiblio.setFullAuthors(mutableListOf(extracted, createPerson("Jane", "Will", "Harvard")))

        val crossrefBiblio = BiblioItem()
        crossrefBiblio.setFullAuthors(mutableListOf(createPerson("F H", "Barreiro"), createPerson("Jane", "Will")))

        BiblioItem.correct(extractedBiblio, crossrefBiblio)

        val author0 = extractedBiblio.getFullAuthors().get(0)
        Assert.assertThat(author0.getLastName(), CoreMatchers.`is`("Barreiro"))
        Assert.assertThat(author0.getAffiliations().get(0).getAffiliationString(), CoreMatchers.`is`("CERN"))
        Assert.assertThat(author0.getAffiliationBlocks(), CoreMatchers.`is`(mutableListOf("CERN, Geneva")))
        Assert.assertThat(author0.getAffiliationMarkers(), CoreMatchers.`is`(mutableListOf("1")))
    }

    @Test
    fun correct_2authors_shouldNotMisassignByPosition_whenListsReorderedAndNameMatchPartiallyFails() {
        // Lists are reordered AND one author fails to name-match (atypical tokenization). Position
        // is therefore unreliable: the fallback must not hand the mistokenized author's affiliation
        // to the unrelated consolidated author that happens to sit at the same index.
        // extracted: [Barreiro (tokenized as first=Barreiro/last=FH) with CERN, Jones (no aff)]
        val extractedBiblio = BiblioItem()
        var authors: MutableList<Person?> = mutableListOf()
        authors.add(createPerson("Barreiro", "FH", "CERN"))
        authors.add(createPerson("John", "Jones"))
        extractedBiblio.setFullAuthors(authors)

        // crossref (reordered): [Jones, Barreiro]
        val crossrefBiblio = BiblioItem()
        authors = mutableListOf()
        authors.add(createPerson("John", "Jones"))
        authors.add(createPerson("F H", "Barreiro"))
        crossrefBiblio.setFullAuthors(authors)

        BiblioItem.correct(extractedBiblio, crossrefBiblio)

        Assert.assertThat(extractedBiblio.getFullAuthors(), Matchers.hasSize<Person?>(2))
        // Jones (name-matched, at index 0) must NOT inherit Barreiro's CERN affiliation by position
        Assert.assertThat(extractedBiblio.getFullAuthors().get(0).getLastName(), CoreMatchers.`is`("Jones"))
        Assert.assertTrue(extractedBiblio.getFullAuthors().get(0).getAffiliations().isNullOrEmpty())
        // Barreiro sits at index 1 where the extracted counterpart (Jones) was name-matched, so the
        // fallback correctly refuses to guess rather than risk a wrong affiliation
        Assert.assertThat(extractedBiblio.getFullAuthors().get(1).getLastName(), CoreMatchers.`is`("Barreiro"))
        Assert.assertTrue(extractedBiblio.getFullAuthors().get(1).getAffiliations().isNullOrEmpty())
    }

    @Test
    fun correct_2authors_shouldKeepCrossrefOrcid_whenPdfHasNone() {
        // CrossRef result (bibo) has ORCIDs
        val biblio1 = BiblioItem()
        var authors: MutableList<Person?> = mutableListOf()
        val crossrefAuthor1 = createPerson("John", "Doe")
        crossrefAuthor1.setORCID("0000-0001-1111-1111")
        authors.add(crossrefAuthor1)
        val crossrefAuthor2 = createPerson("Jane", "Will")
        crossrefAuthor2.setORCID("0000-0002-2222-2222")
        authors.add(crossrefAuthor2)
        biblio1.setFullAuthors(authors)

        // PDF-extracted (bib) has no ORCIDs
        val biblio2 = BiblioItem()
        authors = mutableListOf()
        authors.add(createPerson("John", "Doe"))
        authors.add(createPerson("Jane", "Will"))
        biblio2.setFullAuthors(authors)

        BiblioItem.correct(biblio2, biblio1)

        // CrossRef ORCIDs should be kept
        Assert.assertThat(biblio2.getFullAuthors().get(0).getORCID(), CoreMatchers.`is`("0000-0001-1111-1111"))
        Assert.assertThat(biblio2.getFullAuthors().get(1).getORCID(), CoreMatchers.`is`("0000-0002-2222-2222"))
    }

    @Test
    fun correct_2authors_shouldNotOverwriteCrossrefOrcid_whenBothHaveOrcid() {
        // CrossRef result (bibo) has ORCIDs
        val biblio1 = BiblioItem()
        var authors: MutableList<Person?> = mutableListOf()
        val crossrefAuthor1 = createPerson("John", "Doe")
        crossrefAuthor1.setORCID("0000-0001-1111-1111")
        authors.add(crossrefAuthor1)
        authors.add(createPerson("Jane", "Will"))
        biblio1.setFullAuthors(authors)

        // PDF-extracted (bib) has different ORCIDs
        val biblio2 = BiblioItem()
        authors = mutableListOf()
        val pdfAuthor1 = createPerson("John", "Doe")
        pdfAuthor1.setORCID("0000-0001-9999-9999")
        authors.add(pdfAuthor1)
        val pdfAuthor2 = createPerson("Jane", "Will")
        pdfAuthor2.setORCID("0000-0002-8888-8888")
        authors.add(pdfAuthor2)
        biblio2.setFullAuthors(authors)

        BiblioItem.correct(biblio2, biblio1)

        // CrossRef ORCID should take priority (not overwritten by PDF)
        Assert.assertThat(biblio2.getFullAuthors().get(0).getORCID(), CoreMatchers.`is`("0000-0001-1111-1111"))
        // PDF ORCID preserved when CrossRef doesn't have one
        Assert.assertThat(biblio2.getFullAuthors().get(1).getORCID(), CoreMatchers.`is`("0000-0002-8888-8888"))
    }

    @Test
    fun correct_1author_shouldPreservePdfOrcid_whenCrossrefHasNone() {
        // CrossRef result (bibo) with single author, no ORCID
        val biblio1 = BiblioItem()
        var authors: MutableList<Person?> = mutableListOf()
        authors.add(createPerson("John", "Doe"))
        biblio1.setFullAuthors(authors)

        // PDF-extracted (bib) with ORCID
        val biblio2 = BiblioItem()
        authors = mutableListOf()
        val pdfAuthor = createPerson("John", "Doe")
        pdfAuthor.setORCID("0000-0001-2345-6789")
        authors.add(pdfAuthor)
        biblio2.setFullAuthors(authors)

        BiblioItem.correct(biblio2, biblio1)

        // setORCID(null) is a no-op, so PDF ORCID is preserved when CrossRef has none
        Assert.assertThat(biblio2.getFullAuthors().get(0).getORCID(), CoreMatchers.`is`("0000-0001-2345-6789"))
    }

    @Test
    @Throws(Exception::class)
    fun testCleanDOIxPrefix1_shouldRemovePrefix() {
        val doi = "doi:10.1063/1.1905789"
        val cleanDoi = BiblioItem.cleanDOI(doi)

        Assert.assertThat<String?>(cleanDoi, Matchers.`is`<String?>("10.1063/1.1905789"))
    }

    @Test
    @Throws(Exception::class)
    fun testCleanDOIPrefix2_shouldRemovePrefix() {
        val doi = "doi/10.1063/1.1905789"
        val cleanDoi = BiblioItem.cleanDOI(doi)

        Assert.assertThat<String?>(cleanDoi, Matchers.`is`<String?>("10.1063/1.1905789"))
    }

    @Test
    @Throws(Exception::class)
    fun testCleanDOI_cleanCommonExtractionPatterns() {
        val doi = "43-61.DOI:10.1093/jpepsy/14.1.436/7"
        val cleanedDoi = BiblioItem.cleanDOI(doi)

        Assert.assertThat<String?>(cleanedDoi, CoreMatchers.`is`<String?>("10.1093/jpepsy/14.1.436/7"))
    }

    @Test
    @Throws(Exception::class)
    fun testCleanDOI_removeURL_http() {
        val doi = "http://doi.org/10.1063/1.1905789"
        val cleanDoi = BiblioItem.cleanDOI(doi)

        Assert.assertThat<String?>(cleanDoi, Matchers.`is`<String?>("10.1063/1.1905789"))
    }

    @Test
    @Throws(Exception::class)
    fun testCleanDOI_removeURL_https() {
        val doi = "https://doi.org/10.1063/1.1905789"
        val cleanDoi = BiblioItem.cleanDOI(doi)

        Assert.assertThat<String?>(cleanDoi, Matchers.`is`<String?>("10.1063/1.1905789"))
    }

    @Test
    @Throws(Exception::class)
    fun testCleanDOI_removeURL_file() {
        val doi = "file://doi.org/10.1063/1.1905789"
        val cleanDoi = BiblioItem.cleanDOI(doi)

        Assert.assertThat<String?>(cleanDoi, Matchers.`is`<String?>("10.1063/1.1905789"))
    }

    @Test
    @Throws(Exception::class)
    fun testCleanDOI_diactric() {
        val doi = "10.1063/1.1905789͔"

        val cleanDoi = BiblioItem.cleanDOI(doi)

        Assert.assertThat<String?>(cleanDoi, Matchers.`is`<String?>("10.1063/1.1905789"))
    }

    // ------------------------------------------------------------------------------------------
    // Multi-round author consolidation matching (exact -> soft -> positional) in BiblioItem.correct
    // correct(extractedBiblio, consolidatedBiblio): consolidatedBiblio (CrossRef) wins the author
    // list; each consolidated author is enriched from the matching extracted (PDF) author.
    // ------------------------------------------------------------------------------------------

    private fun biblioOf(vararg authors: Person?): BiblioItem {
        val biblio = BiblioItem()
        biblio.setFullAuthors(authors.toMutableList())
        return biblio
    }

    private fun affiliationStringOf(biblio: BiblioItem, index: Int): String? {
        val affiliations = biblio.getFullAuthors().get(index).getAffiliations()
        return if (affiliations.isNullOrEmpty()) null else affiliations.get(0).getAffiliationString()
    }

    // ---- Round 1: exact last-name match ----

    @Test
    fun correct_exact_twoAuthors_copiesAffiliations() {
        val pdf = biblioOf(createPerson("John", "Doe", "MIT"), createPerson("Jane", "Roe", "Yale"))
        val crossref = biblioOf(createPerson("John", "Doe"), createPerson("Jane", "Roe"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertThat(affiliationStringOf(pdf, 0), CoreMatchers.`is`("MIT"))
        Assert.assertThat(affiliationStringOf(pdf, 1), CoreMatchers.`is`("Yale"))
    }

    @Test
    fun correct_exact_caseInsensitiveSurname_copies() {
        val pdf = biblioOf(createPerson("John", "SMITH", "MIT"), createPerson("Amy", "JONES", "UCLA"))
        val crossref = biblioOf(createPerson("John", "smith"), createPerson("Amy", "jones"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertThat(affiliationStringOf(pdf, 0), CoreMatchers.`is`("MIT"))
        Assert.assertThat(affiliationStringOf(pdf, 1), CoreMatchers.`is`("UCLA"))
    }

    @Test
    fun correct_exact_consolidatedInitial_pdfFullName_copiesAndAdoptsFullName() {
        val pdf = biblioOf(createPerson("John", "Smith", "MIT"), createPerson("Amy", "Jones", "UCLA"))
        val crossref = biblioOf(createPerson("J", "Smith"), createPerson("A", "Jones"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertThat(pdf.getFullAuthors().get(0).getFirstName(), CoreMatchers.`is`("John"))
        Assert.assertThat(affiliationStringOf(pdf, 0), CoreMatchers.`is`("MIT"))
    }

    @Test
    fun correct_exact_pdfBlankFirstName_copies() {
        val pdf = biblioOf(createPerson(null, "Smith", "MIT"), createPerson(null, "Jones", "UCLA"))
        val crossref = biblioOf(createPerson("John", "Smith"), createPerson("Amy", "Jones"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertThat(affiliationStringOf(pdf, 0), CoreMatchers.`is`("MIT"))
        Assert.assertThat(affiliationStringOf(pdf, 1), CoreMatchers.`is`("UCLA"))
    }

    @Test
    fun correct_exact_incompatibleFirstNames_doesNotCopy() {
        // unequal list sizes (2 vs 3) so the positional fallback cannot mask the guard
        val pdf = biblioOf(
            createPerson("John", "Smith", "MIT"),
            createPerson("Amy", "Jones", "UCLA"),
            createPerson("Zed", "Extra"),
        )
        val crossref = biblioOf(createPerson("Jane", "Smith"), createPerson("Amy", "Jones"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertTrue(pdf.getFullAuthors().get(0).getAffiliations().isNullOrEmpty())
        Assert.assertThat(affiliationStringOf(pdf, 1), CoreMatchers.`is`("UCLA"))
    }

    @Test
    fun correct_exact_extractedAuthorConsumedOnce() {
        // two consolidated "Smith", only one extracted "Smith" -> only the first is enriched
        val pdf = biblioOf(
            createPerson("John", "Smith", "MIT"),
            createPerson("Amy", "Jones", "UCLA"),
            createPerson("Zed", "Extra"),
        )
        val crossref = biblioOf(createPerson("John", "Smith"), createPerson("Jon", "Smith"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertThat(affiliationStringOf(pdf, 0), CoreMatchers.`is`("MIT"))
        Assert.assertTrue(pdf.getFullAuthors().get(1).getAffiliations().isNullOrEmpty())
    }

    @Test
    fun correct_exact_consolidatedAlreadyHasAffiliation_isNotOverwritten() {
        val pdf = biblioOf(createPerson("John", "Smith", "Madrid"), createPerson("Amy", "Jones", "UCLA"))
        val crossref = biblioOf(createPerson("John", "Smith", "ProviderAff"), createPerson("Amy", "Jones"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertThat(affiliationStringOf(pdf, 0), CoreMatchers.`is`("ProviderAff"))
        Assert.assertThat(affiliationStringOf(pdf, 1), CoreMatchers.`is`("UCLA"))
    }

    // ---- Round 2: soft last-name match ----

    @Test
    fun correct_soft_strayMarkerInSurname_recoversAffiliation() {
        val pdf = biblioOf(createPerson("Andrea", "Enguita-Marruedo \$", "Madrid"), createPerson("Bob", "Other", "X"))
        val crossref = biblioOf(createPerson("Andrea", "Enguita-Marruedo"), createPerson("Bob", "Other"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertThat(affiliationStringOf(pdf, 0), CoreMatchers.`is`("Madrid"))
    }

    @Test
    fun correct_soft_accentedSurname_recoversAffiliation() {
        val pdf = biblioOf(createPerson("Maria", "Martín", "Madrid"), createPerson("Bob", "Other", "X"))
        val crossref = biblioOf(createPerson("Maria", "Martin"), createPerson("Bob", "Other"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertThat(affiliationStringOf(pdf, 0), CoreMatchers.`is`("Madrid"))
    }

    @Test
    fun correct_soft_belowThreshold_doesNotCopy() {
        val pdf = biblioOf(
            createPerson("John", "Johnson", "MIT"),
            createPerson("Amy", "Jones", "UCLA"),
            createPerson("Zed", "Extra"),
        )
        val crossref = biblioOf(createPerson("John", "Smith"), createPerson("Amy", "Jones"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertTrue(pdf.getFullAuthors().get(0).getAffiliations().isNullOrEmpty())
        Assert.assertThat(affiliationStringOf(pdf, 1), CoreMatchers.`is`("UCLA"))
    }

    @Test
    fun correct_soft_firstNameGuard_blocksCrossAssignment() {
        // "Smithe" ~ "Smith" >= 0.90 but different first names -> must NOT merge
        val pdf = biblioOf(
            createPerson("John", "Smith", "MIT"),
            createPerson("Amy", "Jones", "UCLA"),
            createPerson("Zed", "Extra"),
        )
        val crossref = biblioOf(createPerson("Jane", "Smithe"), createPerson("Amy", "Jones"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertTrue(pdf.getFullAuthors().get(0).getAffiliations().isNullOrEmpty())
        Assert.assertThat(affiliationStringOf(pdf, 1), CoreMatchers.`is`("UCLA"))
    }

    @Test
    fun correct_soft_picksQualifyingCandidateOverNonQualifying() {
        val pdf = biblioOf(
            createPerson("Maria", "Rodriguez!", "Madrid"),
            createPerson("Maria", "Johnson", "Berlin"),
            createPerson("Zed", "Extra"),
        )
        val crossref = biblioOf(createPerson("Maria", "Rodriguez"), createPerson("X", "Y"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertThat(affiliationStringOf(pdf, 0), CoreMatchers.`is`("Madrid"))
    }

    @Test
    fun correct_soft_picksHigherSimilarityAmongQualifying() {
        val pdf = biblioOf(
            createPerson("Maria", "Zimmermann.", "AFF_CLOSER"),
            createPerson("Maria", "Zimmerman", "AFF_FARTHER"),
            createPerson("Zed", "Extra"),
        )
        val crossref = biblioOf(createPerson("Maria", "Zimmermann"), createPerson("X", "Y"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertThat(affiliationStringOf(pdf, 0), CoreMatchers.`is`("AFF_CLOSER"))
    }

    @Test
    fun correct_soft_doesNotReuseExactMatchedExtracted() {
        val pdf = biblioOf(createPerson("John", "Smith", "MIT"), createPerson("Zed", "Extra"))
        val crossref = biblioOf(createPerson("John", "Smith"), createPerson("John", "Smithh"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertThat(affiliationStringOf(pdf, 0), CoreMatchers.`is`("MIT"))
        // the second consolidated "Smithh" must not steal the already-consumed extracted "Smith"
        Assert.assertTrue(pdf.getFullAuthors().get(1).getAffiliations().isNullOrEmpty())
    }

    // ---- Round 3: positional fallback ----

    @Test
    fun correct_positional_unequalSize_doesNotCopy() {
        val pdf = biblioOf(
            createPerson("Aaa", "Bbb", "Madrid"),
            createPerson("Ccc", "Ddd", "Berlin"),
            createPerson("Eee", "Fff", "Paris"),
        )
        val crossref = biblioOf(createPerson("Ggg", "Hhh"), createPerson("Iii", "Jjj"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertTrue(pdf.getFullAuthors().get(0).getAffiliations().isNullOrEmpty())
        Assert.assertTrue(pdf.getFullAuthors().get(1).getAffiliations().isNullOrEmpty())
    }

    @Test
    fun correct_positional_onlyCopiesAffiliation_notOrcidOrEmail() {
        val pdfAuthor = createPerson("Eee", "Fff", "Madrid")
        pdfAuthor.setORCID("0000-0001-2222-3333")
        pdfAuthor.setEmail("eee@example.org")
        val pdf = biblioOf(pdfAuthor, createPerson("Ggg", "Hhh", "Berlin"))
        val crossref = biblioOf(createPerson("Aaa", "Bbb"), createPerson("Ccc", "Ddd"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertThat(affiliationStringOf(pdf, 0), CoreMatchers.`is`("Madrid"))
        Assert.assertTrue(pdf.getFullAuthors().get(0).getORCID().isNullOrEmpty())
        Assert.assertTrue(pdf.getFullAuthors().get(0).getEmail().isNullOrEmpty())
    }

    @Test
    fun correct_positional_doesNotOverwriteExistingConsolidatedAffiliation() {
        val pdf = biblioOf(createPerson("Eee", "Fff", "Madrid"), createPerson("Ggg", "Hhh", "Berlin"))
        val crossref = biblioOf(createPerson("Aaa", "Bbb", "ProviderAff"), createPerson("Ccc", "Ddd"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertThat(affiliationStringOf(pdf, 0), CoreMatchers.`is`("ProviderAff"))
    }

    // ---- Edge / unlikely cases ----

    @Test
    fun correct_extractedAuthorsEmpty_takesConsolidatedList() {
        val pdf = BiblioItem()
        val crossref = biblioOf(createPerson("A", "B"), createPerson("C", "D"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertThat(pdf.getFullAuthors(), Matchers.hasSize<Person?>(2))
    }

    @Test
    fun correct_nullSurnames_doNotCrash() {
        val pdf = biblioOf(createPerson("A", null, "Madrid"), createPerson("C", "D", "Berlin"))
        val crossref = biblioOf(createPerson("A", null), createPerson("C", "D"))
        BiblioItem.correct(pdf, crossref)
        Assert.assertThat(affiliationStringOf(pdf, 1), CoreMatchers.`is`("Berlin"))
    }

    @Test
    fun correct_softMatch_blankConsolidatedSurname_isSkipped() {
        val pdf = biblioOf(createPerson("A", "Something", "Madrid"), createPerson("C", "D", "Berlin"))
        val crossref = biblioOf(createPerson("A", null), createPerson("C", "D"))
        BiblioItem.correct(pdf, crossref)
        // consolidated[0] has a blank surname -> no soft match attempted; only positional may apply
        Assert.assertThat(affiliationStringOf(pdf, 1), CoreMatchers.`is`("Berlin"))
    }

    @Test
    fun correct_manyAuthors_mixedExactSoftPositional() {
        val pdf = biblioOf(
            createPerson("John", "Doe", "AFF_DOE"), // exact
            createPerson("Maria", "García", "AFF_GARCIA"), // soft (accent)
            createPerson("Xy", "Zz-marker \$", "AFF_POS"), // positional (surname too different for soft)
        )
        val crossref = biblioOf(
            createPerson("John", "Doe"),
            createPerson("Maria", "Garcia"),
            createPerson("Xy", "Zz"),
        )
        BiblioItem.correct(pdf, crossref)
        Assert.assertThat(affiliationStringOf(pdf, 0), CoreMatchers.`is`("AFF_DOE"))
        Assert.assertThat(affiliationStringOf(pdf, 1), CoreMatchers.`is`("AFF_GARCIA"))
        Assert.assertThat(affiliationStringOf(pdf, 2), CoreMatchers.`is`("AFF_POS"))
    }

    private fun createPerson(firstName: String?, secondName: String?): Person {
        val person = Person()
        person.setFirstName(firstName)
        person.setLastName(secondName)
        return person
    }

    private fun createPerson(firstName: String?, secondName: String?, affiliation: String?): Person {
        val person = createPerson(firstName, secondName)
        val affiliation1 = Affiliation()
        affiliation1.setAffiliationString(affiliation)
        val affiliations: MutableList<Affiliation?> = mutableListOf()
        affiliations.add(affiliation1)
        person.setAffiliations(affiliations)
        return person
    }

    @Test
    @Throws(Exception::class)
    fun testToTEI_ShouldUseCorrectAttributeNames() {
        val config = configBuilder.generateTeiIds(true).build()
        val biblioItem = BiblioItem()
        biblioItem.setLanguage("en")
        biblioItem.setStatus(Consolidation.CONSOLIDATION_STATUS_CONSOLIDATED)
        val tei = biblioItem.toTEI(5, 1, config)
        val doc: Document? = parseXml(tei)

        // Check xml:id and status attributes (namespace-aware)
        val xmlIds: MutableList<String?> = getXpathStrings(doc, "/biblStruct/@xml:id")
        val statuses: MutableList<String?> = getXpathStrings(doc, "/biblStruct/@status")
        val langs: MutableList<String?> = getXpathStrings(doc, "/biblStruct/@xml:lang")
        Assert.assertThat(
            xmlIds,
            CoreMatchers.`is`(mutableListOf<String?>("b5")),
        )
        Assert.assertThat(
            statuses,
            CoreMatchers.`is`(mutableListOf<String?>("consolidated")),
        )
        Assert.assertThat(
            langs,
            CoreMatchers.`is`(mutableListOf<String?>("en")),
        )
    }

    // Helper to create a Person with middleName
    private fun createPersonWithMiddleName(firstName: String?, lastName: String?, middleName: String?): Person {
        val person = Person()
        person.setFirstName(firstName)
        person.setLastName(lastName)
        person.setMiddleName(middleName)
        return person
    }

    // --- Author formatting via toBibTeX() ---

    @Test
    fun toBibTeX_authorWithLastAndFirstName() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPerson("John", "Doe")))
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("author = {Doe, John}"))
    }

    @Test
    fun toBibTeX_authorWithSingleLetterFirstName() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPerson("J", "Doe")))
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("author = {Doe, J.}"))
    }

    @Test
    fun toBibTeX_authorWithMiddleName() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPersonWithMiddleName("John", "Doe", "W")))
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("author = {Doe, John W.}"))
    }

    @Test
    fun toBibTeX_authorWithMultiLetterMiddleName() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPersonWithMiddleName("John", "Doe", "William")))
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("author = {Doe, John William}"))
    }

    @Test
    fun toBibTeX_authorWithFirstAndMiddleInitials() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPersonWithMiddleName("K", "Dill", "A")))
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("author = {Dill, K. A.}"))
    }

    @Test
    fun toBibTeX_authorWithHyphenatedFirstName() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPerson("J-L", "Dupont")))
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("author = {Dupont, J.-L.}"))
    }

    @Test
    fun toBibTeX_authorWithSpacedInitials() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPerson("W S", "Smith")))
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("author = {Smith, W. S.}"))
    }

    @Test
    fun toBibTeX_authorWithOnlyMiddleName() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPersonWithMiddleName(null, "Doe", "M")))
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("author = {Doe, M.}"))
    }

    @Test
    fun toBibTeX_authorWithOnlyMiddleNameNoLast() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPersonWithMiddleName(null, null, "M")))
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("author = {M.}"))
        // No leading space before M.
        Assert.assertThat(bibtex, CoreMatchers.not(Matchers.containsString("author = { M.}")))
    }

    @Test
    fun toBibTeX_multipleAuthors() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(
            mutableListOf(
                createPerson("John", "Doe"),
                createPerson("Jane", "Smith"),
            ),
        )
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("author = {Doe, John and Smith, Jane}"))
    }

    @Test
    fun toBibTeX_authorWithBlankFirstName() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPerson("  ", "Doe")))
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("author = {Doe}"))
    }

    @Test
    fun toBibTeX_nullFullAuthors_fallsBackToAuthorsString() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(null)
        biblio.setAuthors("Doe, John; Smith, Jane")
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("author = {Doe, John and Smith, Jane}"))
    }

    @Test
    fun toBibTeX_emptyFullAuthors_fallsBackToAuthorsString() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf())
        biblio.setAuthors("Doe, John; Smith, Jane")
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("author = {Doe, John and Smith, Jane}"))
    }

    // --- Editor formatting via toBibTeX() ---

    @Test
    fun toBibTeX_editorWithLastAndFirstName() {
        val biblio = BiblioItem()
        biblio.setFullEditors(mutableListOf(createPerson("Jane", "Smith")))
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("editor = {Smith, Jane}"))
    }

    @Test
    fun toBibTeX_editorWithMiddleName() {
        val biblio = BiblioItem()
        biblio.setFullEditors(mutableListOf(createPersonWithMiddleName("Jane", "Smith", "M")))
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("editor = {Smith, Jane M.}"))
    }

    @Test
    fun toBibTeX_allBlankEditors_fallsBackToEditorsString() {
        val biblio = BiblioItem()
        biblio.setFullEditors(mutableListOf(createPerson("  ", "  ")))
        biblio.setEditors("Smith, Jane")
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("editor = {Smith, Jane}"))
    }

    @Test
    fun toBibTeX_emptyFullEditors_fallsBackToEditorsString() {
        val biblio = BiblioItem()
        biblio.setFullEditors(mutableListOf())
        biblio.setEditors("Smith, Jane")
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("editor = {Smith, Jane}"))
    }

    // --- BibTeX key generation ---

    @Test
    fun generateBibTeXKey_authorYearTitle() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPerson("John", "Smith")))
        val date = Date()
        date.year = 2023
        biblio.setNormalizedPublicationDate(date)
        biblio.setTitle("Machine learning for beginners")
        Assert.assertThat(biblio.generateBibTeXKey(), CoreMatchers.`is`("smith2023machine"))
    }

    @Test
    fun generateBibTeXKey_shortFirstTitleWordMergesWithSecond() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPerson("Jane", "Doe")))
        val date = Date()
        date.year = 2019
        biblio.setNormalizedPublicationDate(date)
        biblio.setTitle("An introduction to parsing")
        Assert.assertThat(biblio.generateBibTeXKey(), CoreMatchers.`is`("doe2019anintroduction"))
    }

    @Test
    fun generateBibTeXKey_singleCharFirstWordMergesWithSecond() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPerson("Alice", "Wang")))
        val date = Date()
        date.year = 2020
        biblio.setNormalizedPublicationDate(date)
        biblio.setTitle("A survey of deep learning")
        Assert.assertThat(biblio.generateBibTeXKey(), CoreMatchers.`is`("wang2020asurvey"))
    }

    @Test
    fun generateBibTeXKey_missingAuthor() {
        val biblio = BiblioItem()
        val date = Date()
        date.year = 2023
        biblio.setNormalizedPublicationDate(date)
        biblio.setTitle("Machine learning for beginners")
        Assert.assertThat(biblio.generateBibTeXKey(), CoreMatchers.`is`("2023machine"))
    }

    @Test
    fun generateBibTeXKey_missingYear() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPerson("John", "Smith")))
        biblio.setTitle("Machine learning for beginners")
        Assert.assertThat(biblio.generateBibTeXKey(), CoreMatchers.`is`("smithmachine"))
    }

    @Test
    fun generateBibTeXKey_missingTitle() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPerson("John", "Smith")))
        val date = Date()
        date.year = 2023
        biblio.setNormalizedPublicationDate(date)
        Assert.assertThat(biblio.generateBibTeXKey(), CoreMatchers.`is`("smith2023"))
    }

    @Test
    fun generateBibTeXKey_allMissing() {
        val biblio = BiblioItem()
        Assert.assertThat(biblio.generateBibTeXKey(), CoreMatchers.`is`("unknown"))
    }

    @Test
    fun generateBibTeXKey_specialCharsInAuthorName() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPerson("José", "O'Brien-Smith")))
        val date = Date()
        date.year = 2021
        biblio.setNormalizedPublicationDate(date)
        biblio.setTitle("Testing")
        Assert.assertThat(biblio.generateBibTeXKey(), CoreMatchers.`is`("obriensmith2021testing"))
    }

    @Test
    fun generateBibTeXKey_fallbackToPublicationDateString() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPerson("John", "Smith")))
        biblio.setPublicationDate("2018")
        biblio.setTitle("Testing")
        Assert.assertThat(biblio.generateBibTeXKey(), CoreMatchers.`is`("smith2018testing"))
    }

    @Test
    fun generateBibTeXKey_fallsBackToBookTitle() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPerson("S", "Kolb")))
        val date = Date()
        date.year = 2014
        biblio.setNormalizedPublicationDate(date)
        biblio.setBookTitle("Towards Application Portability in Platform as a Service")
        Assert.assertThat(biblio.generateBibTeXKey(), CoreMatchers.`is`("kolb2014towards"))
    }

    @Test
    fun generateBibTeXKey_prefersTitle() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPerson("John", "Smith")))
        val date = Date()
        date.year = 2023
        biblio.setNormalizedPublicationDate(date)
        biblio.setTitle("Machine learning")
        biblio.setBookTitle("Proceedings of something")
        Assert.assertThat(biblio.generateBibTeXKey(), CoreMatchers.`is`("smith2023machine"))
    }

    @Test
    fun generateBibTeXKey_toBibTeXUsesGeneratedKey() {
        val biblio = BiblioItem()
        biblio.setFullAuthors(mutableListOf(createPerson("John", "Smith")))
        val date = Date()
        date.year = 2023
        biblio.setNormalizedPublicationDate(date)
        biblio.setTitle("Machine learning for beginners")
        val bibtex = biblio.toBibTeX()
        Assert.assertThat(bibtex, Matchers.containsString("{smith2023machine,"))
    }

    // --- Date string fields kept in sync with the normalized date (issue #15) ---

    @Test
    fun setNormalizedPublicationDate_populatesYearMonthDay_issue15() {
        val date = Date()
        date.year = 2013
        date.month = 6
        date.day = 20

        val biblio = BiblioItem()
        biblio.setNormalizedPublicationDate(date)

        // these used to stay null outside of toTEI()
        Assert.assertThat(biblio.getYear(), CoreMatchers.`is`("2013"))
        Assert.assertThat(biblio.getMonth(), CoreMatchers.`is`("6"))
        Assert.assertThat(biblio.getDay(), CoreMatchers.`is`("20"))
    }

    @Test
    fun setNormalizedPublicationDate_doesNotClobberExistingYear_issue15() {
        val biblio = BiblioItem()
        biblio.setYear("2010")

        val date = Date()
        date.year = 2013
        biblio.setNormalizedPublicationDate(date)

        // an explicitly-set value must be preserved
        Assert.assertThat(biblio.getYear(), CoreMatchers.`is`("2010"))
    }

    @Test
    fun setNormalizedPublicationDate_partialDateLeavesMissingFieldsNull_issue15() {
        val date = Date()
        date.year = 2013 // month and day remain unset (-1)

        val biblio = BiblioItem()
        biblio.setNormalizedPublicationDate(date)

        Assert.assertThat(biblio.getYear(), CoreMatchers.`is`("2013"))
        Assert.assertNull(biblio.getMonth())
        Assert.assertNull(biblio.getDay())
    }

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(BiblioItemTest::class.java)

        @BeforeClass
        fun init() {
            GrobidProperties.getInstance()
        }

        @Throws(ParserConfigurationException::class, SAXException::class, IOException::class)
        private fun parseXml(xml: String): Document? {
            val domFactory = DocumentBuilderFactory.newInstance()
            domFactory.setNamespaceAware(true)
            val builder = domFactory.newDocumentBuilder()
            return builder.parse(InputSource(StringReader(xml)))
        }

        @Throws(XPathExpressionException::class)
        private fun getXpathStrings(
            doc: Document?,
            xpath_expr: String?,
        ): MutableList<String?> {
            val xpath = XPathFactory.newInstance().newXPath()
            // Add support for xml namespace
            xpath.setNamespaceContext(object : NamespaceContext {
                override fun getNamespaceURI(prefix: String?): String {
                    if ("xml" == prefix) {
                        return "http://www.w3.org/XML/1998/namespace"
                    }
                    return XMLConstants.NULL_NS_URI
                }

                override fun getPrefix(namespaceURI: String?): String? = null

                override fun getPrefixes(namespaceURI: String?): Iterator<String?>? = null
            })
            val expr = xpath.compile(xpath_expr)
            val nodes = expr.evaluate(doc, XPathConstants.NODESET) as NodeList
            val matchingStrings = ArrayList<String?>()
            for (i in 0 until nodes.getLength()) {
                matchingStrings.add(nodes.item(i).getNodeValue())
            }
            return matchingStrings
        }
    }
}
