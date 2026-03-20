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

    private val configBuilder = (GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            )

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
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>("raw affiliation 1"))
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
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>("A"))
        )
        Assert.assertThat<MutableList<String?>?>(
            "raw_affiliation",
            getXpathStrings(doc, "//note[@type=\"raw_affiliation\"]/text()"),
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>(" raw affiliation 1"))
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
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>("&"))
        )
        Assert.assertThat<MutableList<String?>?>(
            "raw_affiliation",
            getXpathStrings(doc, "//note[@type=\"raw_affiliation\"]/text()"),
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>(" raw affiliation 1"))
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
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>("raw affiliation 1"))
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
            CoreMatchers.`is`<MutableCollection<out String?>?>(Matchers.empty<String?>())
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
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>("10.1233/23232&3232"))
        )
        Assert.assertThat<MutableList<String?>?>(
            "ISSN",
            getXpathStrings(doc, "//idno[@type=\"ISSN\"]/text()"),
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>("0974&9756"))
        )
        Assert.assertThat<MutableList<String?>?>(
            "PMID",
            getXpathStrings(doc, "//idno[@type=\"PMID\"]/text()"),
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>("pmid&123"))
        )
        Assert.assertThat<MutableList<String?>?>(
            "Ark",
            getXpathStrings(doc, "//idno[@type=\"ark\"]/text()"),
            CoreMatchers.`is`<MutableList<String?>?>(mutableListOf<String?>("Noah & !"))
        )
    }

    @Test
    fun correct_empty_shouldNotFail() {
        BiblioItem.correct(BiblioItem(), BiblioItem())
    }


    @Test
    fun correct_1author_shouldWork() {
        val biblio1 = BiblioItem()
        var authors: MutableList<Person?> = ArrayList<Person?>()
        authors.add(createPerson("John", "Doe"))
        biblio1.setFullAuthors(authors)

        val biblio2 = BiblioItem()
        authors = ArrayList<Person?>()
        authors.add(createPerson("John1", "Doe"))
        biblio2.setFullAuthors(authors)

        BiblioItem.correct(biblio1, biblio2)

        Assert.assertThat<String?>(
            biblio1.getFirstAuthorSurname(),
            CoreMatchers.`is`<String?>(biblio2.getFirstAuthorSurname())
        )
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getFirstName(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(0).getFirstName())
        )
    }

    @Test
    fun correct_2authors_shouldMatchFullName_shouldUpdateAffiliation() {
        val biblio1 = BiblioItem()
        var authors: MutableList<Person?> = ArrayList<Person?>()
        authors.add(createPerson("John", "Doe"))
        authors.add(createPerson("Jane", "Will"))
        biblio1.setFullAuthors(authors)

        val biblio2 = BiblioItem()
        authors = ArrayList<Person?>()
        authors.add(createPerson("John", "Doe", "UCLA"))
        authors.add(createPerson("Jane", "Will", "Harward"))
        biblio2.setFullAuthors(authors)

        BiblioItem.correct(biblio1, biblio2)

        Assert.assertThat<String?>(
            biblio1.getFirstAuthorSurname(),
            CoreMatchers.`is`<String?>(biblio2.getFirstAuthorSurname())
        )
        Assert.assertThat<MutableList<Person?>?>(biblio1.getFullAuthors(), Matchers.hasSize<Person?>(2))
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getFirstName(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(0).getFirstName())
        )
        // biblio1 affiliations empty we update them with ones from biblio2
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString())
        )
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(1).getFirstName(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(1).getFirstName())
        )
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString())
        )
    }

    @Test
    fun correct_2authors_shouldMatchFullName_shouldKeepAffiliation() {
        val biblio1 = BiblioItem()
        var authors: MutableList<Person?> = ArrayList<Person?>()
        authors.add(createPerson("John", "Doe", "Stanford"))
        authors.add(createPerson("Jane", "Will", "Cambridge"))
        biblio1.setFullAuthors(authors)

        val biblio2 = BiblioItem()
        authors = ArrayList<Person?>()
        authors.add(createPerson("John", "Doe"))
        authors.add(createPerson("Jane", "Will", "UCLA"))
        biblio2.setFullAuthors(authors)

        BiblioItem.correct(biblio1, biblio2)

        Assert.assertThat<String?>(
            biblio1.getFirstAuthorSurname(),
            CoreMatchers.`is`<String?>(biblio2.getFirstAuthorSurname())
        )
        Assert.assertThat<MutableList<Person?>?>(biblio1.getFullAuthors(), Matchers.hasSize<Person?>(2))
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getFirstName(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(0).getFirstName())
        )
        // biblio1 affiliations not empty, we keep biblio1 as is
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio1.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString())
        )
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(1).getFirstName(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(1).getFirstName())
        )
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio1.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString())
        )
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString())
        )
    }

    @Test
    fun correct_2authors_initial_2_shouldUpdateAuthor() {
        val biblio1 = BiblioItem()
        var authors: MutableList<Person?> = ArrayList<Person?>()
        authors.add(createPerson("John", "Doe", "ULCA"))
        authors.add(createPerson("J", "Will", "Harward"))
        biblio1.setFullAuthors(authors)

        val biblio2 = BiblioItem()
        authors = ArrayList<Person?>()
        authors.add(createPerson("John1", "Doe", "Stanford"))
        authors.add(createPerson("Jane", "Will", "Berkeley"))
        biblio2.setFullAuthors(authors)

        BiblioItem.correct(biblio1, biblio2)

        Assert.assertThat<String?>(
            biblio1.getFirstAuthorSurname(),
            CoreMatchers.`is`<String?>(biblio2.getFirstAuthorSurname())
        )
        Assert.assertThat<MutableList<Person?>?>(biblio1.getFullAuthors(), Matchers.hasSize<Person?>(2))
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getFirstName(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(0).getFirstName())
        )
        // affiliation should be kept though since not empty
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio1.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString())
        )
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(1).getFirstName(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(1).getFirstName())
        )
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio1.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString())
        )
    }

    @Test
    fun correct_2authors_initial_shouldUpdateAuthor() {
        val biblio1 = BiblioItem()
        var authors: MutableList<Person?> = ArrayList<Person?>()
        authors.add(createPerson("John", "Doe", "ULCA"))
        authors.add(createPerson("Jane", "Will", "Harward"))
        biblio1.setFullAuthors(authors)

        val biblio2 = BiblioItem()
        authors = ArrayList<Person?>()
        authors.add(createPerson("John1", "Doe", "Stanford"))
        authors.add(createPerson("J", "Will", "Berkeley"))
        biblio2.setFullAuthors(authors)

        BiblioItem.correct(biblio1, biblio2)

        Assert.assertThat<String?>(
            biblio1.getFirstAuthorSurname(),
            CoreMatchers.`is`<String?>(biblio2.getFirstAuthorSurname())
        )
        Assert.assertThat<MutableList<Person?>?>(biblio1.getFullAuthors(), Matchers.hasSize<Person?>(2))
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getFirstName(),
            CoreMatchers.`is`<String?>(biblio2.getFullAuthors().get(0).getFirstName())
        )
        // affiliation should be kept though
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio1.getFullAuthors().get(0).getAffiliations().get(0).getAffiliationString())
        )
        //assertThat(biblio1.getFullAuthors().get(1).getFirstName(), is(biblio2.getFullAuthors().get(0).getFirstName()));
        Assert.assertThat<String?>(
            biblio1.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString(),
            CoreMatchers.`is`<String?>(biblio1.getFullAuthors().get(1).getAffiliations().get(0).getAffiliationString())
        )
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
        val affiliations: MutableList<Affiliation?> = ArrayList<Affiliation?>()
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
            CoreMatchers.`is`(mutableListOf<String?>("b5"))
        )
        Assert.assertThat(
            statuses,
            CoreMatchers.`is`(mutableListOf<String?>("consolidated"))
        )
        Assert.assertThat(
            langs,
            CoreMatchers.`is`(mutableListOf<String?>("en"))
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
        biblio.setFullAuthors(mutableListOf(
            createPerson("John", "Doe"),
            createPerson("Jane", "Smith")
        ))
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
            doc: Document?, xpath_expr: String?
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

                override fun getPrefix(namespaceURI: String?): String? {
                    return null
                }

                override fun getPrefixes(namespaceURI: String?): Iterator<String?>? {
                    return null
                }
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
