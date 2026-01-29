package org.grobid.core.engines

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.Triple
import org.grobid.core.analyzers.GrobidAnalyzer
import org.grobid.core.data.Figure
import org.grobid.core.data.Table
import org.grobid.core.document.Document
import org.grobid.core.document.DocumentPiece
import org.grobid.core.document.DocumentPointer
import org.grobid.core.engines.label.TaggingLabels.*
import org.grobid.core.factory.GrobidFactory
import org.grobid.core.layout.LayoutToken
import org.grobid.core.main.LibraryLoader
import org.grobid.core.utilities.GrobidConfig
import org.grobid.core.utilities.GrobidProperties
import org.grobid.core.utilities.GrobidTestUtils
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.hasSize
import org.hamcrest.collection.IsCollectionWithSize
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.util.*
import java.util.stream.Collectors

class FullTextParserTest {
    private lateinit var target: FullTextParser

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val modelParameters = GrobidConfig.ModelParameters()
        modelParameters.name = "bao"
        GrobidProperties.addModel(modelParameters)
        target = FullTextParser(EngineParsers())
    }

    companion object {
        @JvmStatic
        @BeforeClass
        @Throws(java.lang.Exception::class)
        fun init() {
            LibraryLoader.load()
            GrobidProperties.getInstance()
        }

        @JvmStatic
        @AfterClass
        @Throws(java.lang.Exception::class)
        fun tearDown() {
            GrobidFactory.reset()
        }
    }

    @Test
    @Throws(Exception::class)
    fun testProcessTrainingDataFigures_single_figure() {
        val text = "The mechanism for superconductivity FIG. 1. λ(T) vs . T for YBCO"
        val tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text)
        val rese =
            "The\tthe\tT\tTh\tThe\tThe\te\the\tThe\tThe\tBLOCKSTART\tLINESTART\tALIGNEDLEFT\tNEWFONT\tHIGHERFONT\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t0\t4\t0\tNUMBER\t0\t0\tI-<paragraph>\n" +
                "mechanism\tmechanism\tm\tme\tmec\tmech\tm\tsm\tism\tnism\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t0\t4\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "for\tfor\tf\tfo\tfor\tfor\tr\tor\tfor\tfor\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t0\t4\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "superconductivity\tsuperconductivity\ts\tsu\tsup\tsupe\ty\tty\tity\tvity\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t0\t4\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "FIG\tfig\tF\tFI\tFIG\tFIG\tG\tIG\tFIG\tFIG\tBLOCKSTART\tLINESTART\tLINEINDENT\tNEWFONT\tHIGHERFONT\t0\t0\tALLCAP\tNODIGIT\t0\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\tI-<figure>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                "1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t10\t3\t0\tNUMBER\t1\t0\t<figure>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                "λ\tλ\tλ\tλ\tλ\tλ\tλ\tλ\tλ\tλ\tBLOCKIN\tLINEIN\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t1\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                "(\t(\t(\t(\t(\t(\t(\t(\t(\t(\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tOPENBRACKET\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                "T\tt\tT\tT\tT\tT\tT\tT\tT\tT\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                ")\t)\t)\t)\t)\t)\t)\t)\t)\t)\tBLOCKIN\tLINEIN\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tENDBRACKET\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                "vs\tvs\tv\tvs\tvs\tvs\ts\tvs\tvs\tvs\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEEND\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                "T\tt\tT\tT\tT\tT\tT\tT\tT\tT\tBLOCKIN\tLINESTART\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                "for\tfor\tf\tfo\tfor\tfor\tr\tor\tfor\tfor\tBLOCKIN\tLINEIN\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                "YBCO\tybco\tY\tYB\tYBC\tYBCO\tO\tCO\tBCO\tYBCO\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t0\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n\n"

        val stringStringPair = target!!.processTrainingDataFigures(rese, tokens, "123")

        val tei = stringStringPair.left
        val tokenisation = stringStringPair.right
        val reconstructedText =
            Arrays.stream(tokenisation.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                .map { l: String -> l.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0] }
                .collect(Collectors.joining(" "))

        assertThat(reconstructedText, CoreMatchers.`is`("FIG . 1 . λ ( T ) vs . T for YBCO"))
        assertThat(
            tokenisation.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size,
            CoreMatchers.`is`(13),
        )
    }

    @Test
    @Throws(Exception::class)
    fun testProcessTrainingDataFigures_multiple_figures() {
        val text = "The mechanism for superconductivity FIG. 1. λ(T) vs . T for YBCO"
        val tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text)
        val rese =
            "The\tthe\tT\tTh\tThe\tThe\te\the\tThe\tThe\tBLOCKSTART\tLINESTART\tALIGNEDLEFT\tNEWFONT\tHIGHERFONT\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t0\t4\t0\tNUMBER\t0\t0\tI-<paragraph>\n" +
                "mechanism\tmechanism\tm\tme\tmec\tmech\tm\tsm\tism\tnism\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t0\t4\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "for\tfor\tf\tfo\tfor\tfor\tr\tor\tfor\tfor\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t0\t4\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "superconductivity\tsuperconductivity\ts\tsu\tsup\tsupe\ty\tty\tity\tvity\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t0\t4\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "FIG\tfig\tF\tFI\tFIG\tFIG\tG\tIG\tFIG\tFIG\tBLOCKSTART\tLINESTART\tLINEINDENT\tNEWFONT\tHIGHERFONT\t0\t0\tALLCAP\tNODIGIT\t0\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\tI-<figure>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                "1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t10\t3\t0\tNUMBER\t1\t0\t<figure>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                "λ\tλ\tλ\tλ\tλ\tλ\tλ\tλ\tλ\tλ\tBLOCKIN\tLINEIN\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t1\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                "(\t(\t(\t(\t(\t(\t(\t(\t(\t(\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tOPENBRACKET\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                "T\tt\tT\tT\tT\tT\tT\tT\tT\tT\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                ")\t)\t)\t)\t)\t)\t)\t)\t)\t)\tBLOCKIN\tLINEIN\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tENDBRACKET\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                "vs\tvs\tv\tvs\tvs\tvs\ts\tvs\tvs\tvs\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\tI-<figure>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEEND\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                "T\tt\tT\tT\tT\tT\tT\tT\tT\tT\tBLOCKIN\tLINESTART\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                "for\tfor\tf\tfo\tfor\tfor\tr\tor\tfor\tfor\tBLOCKIN\tLINEIN\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n" +
                "YBCO\tybco\tY\tYB\tYBC\tYBCO\tO\tCO\tBCO\tYBCO\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t0\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<figure>\n\n"

        val stringStringPair = target!!.processTrainingDataFigures(rese, tokens, "123")

        val tei = stringStringPair.left
        val tokenisation = stringStringPair.right
        val output: MutableList<String> = ArrayList()
        for (block in tokenisation.split("\n\n\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val collect = Arrays.stream(block.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                .map { l: String -> l.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0] }
                .collect(Collectors.joining(" "))
            if (StringUtils.isNotBlank(collect)) {
                output.add(collect)
            }
        }

        assertThat<List<String>>(output, IsCollectionWithSize.hasSize(2))
        assertThat(output[0], CoreMatchers.`is`("FIG . 1 . λ ( T )"))
        assertThat(output[1], CoreMatchers.`is`("vs . T for YBCO"))
        assertThat(
            tokenisation.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size,
            CoreMatchers.`is`(15),
        )
    }

    @Test
    @Throws(Exception::class)
    fun testProcessTrainingDataTables_single_table() {
        val text = "The mechanism for superconductivity FIG. 1. λ(T) vs . T for YBCO"
        val tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text)
        val rese =
            "The\tthe\tT\tTh\tThe\tThe\te\the\tThe\tThe\tBLOCKSTART\tLINESTART\tALIGNEDLEFT\tNEWFONT\tHIGHERFONT\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t0\t4\t0\tNUMBER\t0\t0\tI-<paragraph>\n" +
                "mechanism\tmechanism\tm\tme\tmec\tmech\tm\tsm\tism\tnism\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t0\t4\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "for\tfor\tf\tfo\tfor\tfor\tr\tor\tfor\tfor\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t0\t4\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "superconductivity\tsuperconductivity\ts\tsu\tsup\tsupe\ty\tty\tity\tvity\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t0\t4\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "FIG\tfig\tF\tFI\tFIG\tFIG\tG\tIG\tFIG\tFIG\tBLOCKSTART\tLINESTART\tLINEINDENT\tNEWFONT\tHIGHERFONT\t0\t0\tALLCAP\tNODIGIT\t0\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\tI-<table>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                "1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t10\t3\t0\tNUMBER\t1\t0\t<table>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                "λ\tλ\tλ\tλ\tλ\tλ\tλ\tλ\tλ\tλ\tBLOCKIN\tLINEIN\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t1\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                "(\t(\t(\t(\t(\t(\t(\t(\t(\t(\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tOPENBRACKET\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                "T\tt\tT\tT\tT\tT\tT\tT\tT\tT\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                ")\t)\t)\t)\t)\t)\t)\t)\t)\t)\tBLOCKIN\tLINEIN\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tENDBRACKET\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                "vs\tvs\tv\tvs\tvs\tvs\ts\tvs\tvs\tvs\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEEND\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                "T\tt\tT\tT\tT\tT\tT\tT\tT\tT\tBLOCKIN\tLINESTART\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                "for\tfor\tf\tfo\tfor\tfor\tr\tor\tfor\tfor\tBLOCKIN\tLINEIN\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                "YBCO\tybco\tY\tYB\tYBC\tYBCO\tO\tCO\tBCO\tYBCO\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t0\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n\n"

        val stringStringPair = target!!.processTrainingDataTables(rese, tokens, "123")

        val tei = stringStringPair.left
        val tokenisation = stringStringPair.right
        val reconstructedText =
            Arrays.stream(tokenisation.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                .map { l: String -> l.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0] }
                .collect(Collectors.joining(" "))

        assertThat(reconstructedText, CoreMatchers.`is`("FIG . 1 . λ ( T ) vs . T for YBCO"))
        assertThat(
            tokenisation.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size,
            CoreMatchers.`is`(13),
        )
    }

    @Test
    @Throws(Exception::class)
    fun testProcessTrainingDataTable_multiple_tables() {
        val text = "The mechanism for superconductivity FIG. 1. λ(T) vs . T for YBCO"
        val tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text)
        val rese =
            "The\tthe\tT\tTh\tThe\tThe\te\the\tThe\tThe\tBLOCKSTART\tLINESTART\tALIGNEDLEFT\tNEWFONT\tHIGHERFONT\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t0\t4\t0\tNUMBER\t0\t0\tI-<paragraph>\n" +
                "mechanism\tmechanism\tm\tme\tmec\tmech\tm\tsm\tism\tnism\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t0\t4\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "for\tfor\tf\tfo\tfor\tfor\tr\tor\tfor\tfor\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t0\t4\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "superconductivity\tsuperconductivity\ts\tsu\tsup\tsupe\ty\tty\tity\tvity\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t0\t4\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "FIG\tfig\tF\tFI\tFIG\tFIG\tG\tIG\tFIG\tFIG\tBLOCKSTART\tLINESTART\tLINEINDENT\tNEWFONT\tHIGHERFONT\t0\t0\tALLCAP\tNODIGIT\t0\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\tI-<table>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                "1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t10\t3\t0\tNUMBER\t1\t0\t<table>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                "λ\tλ\tλ\tλ\tλ\tλ\tλ\tλ\tλ\tλ\tBLOCKIN\tLINEIN\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t1\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                "(\t(\t(\t(\t(\t(\t(\t(\t(\t(\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tOPENBRACKET\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                "T\tt\tT\tT\tT\tT\tT\tT\tT\tT\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                ")\t)\t)\t)\t)\t)\t)\t)\t)\t)\tBLOCKIN\tLINEIN\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tENDBRACKET\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                "vs\tvs\tv\tvs\tvs\tvs\ts\tvs\tvs\tvs\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\tI-<table>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEEND\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                "T\tt\tT\tT\tT\tT\tT\tT\tT\tT\tBLOCKIN\tLINESTART\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                "for\tfor\tf\tfo\tfor\tfor\tr\tor\tfor\tfor\tBLOCKIN\tLINEIN\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n" +
                "YBCO\tybco\tY\tYB\tYBC\tYBCO\tO\tCO\tBCO\tYBCO\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t0\tNOPUNCT\t10\t3\t0\tNUMBER\t0\t0\t<table>\n\n"

        val stringStringPair = target!!.processTrainingDataTables(rese, tokens, "123")

        val tei = stringStringPair.left
        val tokenisation = stringStringPair.right
        val output: MutableList<String> = ArrayList()
        for (block in tokenisation.split("\n\n\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val collect = Arrays.stream(block.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                .map { l: String -> l.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0] }
                .collect(Collectors.joining(" "))
            if (StringUtils.isNotBlank(collect)) {
                output.add(collect)
            }
        }

        assertThat<List<String>>(output, IsCollectionWithSize.hasSize(2))
        assertThat(output[0], CoreMatchers.`is`("FIG . 1 . λ ( T )"))
        assertThat(output[1], CoreMatchers.`is`("vs . T for YBCO"))
        assertThat(
            tokenisation.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size,
            CoreMatchers.`is`(15),
        )
    }

    @Test
    fun testFindCandidates_shouldFindMultipleResults() {
        // I need to prepare a sequence where there might be multiple matches,
        // and then verify that the sequence is correctly used for discrimination
        var sequence = "This article solves the problem where some of our interaction are fauly. " +
            "a 8 9 j 92j 3 3j 9 j 9j Table 1: The reconstruction of the national anthem " +
            "We are interested in the relation between certain information and " +
            "a b b d 1 2 3 4 s 3 3 d9 Table 2: The relation between information and noise " +
            "the related affectionality. " +
            "a b b d 1 2 3 4 5 6 7 Table 3: The relation between homicides and donuts eating " +
            "The relation between homicides and donuts eating is a very important one. "

        var tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(sequence)

        // These triples made in following way: label, starting index (included), ending index (excluded)
        val labels = listOf(
            Triple.of("I-<paragraph>", 0, 1),
            Triple.of("<paragraph>", 1, 24),
            Triple.of("I-<table>", 25, 26),
            Triple.of("<table>", 26, 61),
            Triple.of("I-<paragraph>", 62, 63),
            Triple.of("<paragraph>", 63, 81),
            Triple.of("I-<table>", 82, 83),
            Triple.of("<table>", 82, 118),
            Triple.of("I-<paragraph>", 119, 120),
            Triple.of("<paragraph>", 120, 129),
            Triple.of("I-<table>", 130, 131),
            Triple.of("<table>", 131, 171),
            Triple.of("I-<paragraph>", 171, 172),
            Triple.of("<paragraph>", 172, 195),
        )

        val features = tokens.stream().map { it.text }.collect(Collectors.toList())
        val wapitiResults = GrobidTestUtils.getWapitiResult(features, labels, "\t")

        val wapitiResultsAsList =
            Arrays.stream(wapitiResults.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                .map<List<String>> { l: String ->
                    Arrays.stream(
                        l.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(),
                    )
                        .collect(Collectors.toList())
                }
                .collect(Collectors.toList())

        val table1Tokens = tokens.subList(25, 61)
        val foundCandidateIndex = FullTextParser.findCandidateIndex(table1Tokens, wapitiResultsAsList, TABLE_LABEL)

        assertThat(foundCandidateIndex, hasSize(3))
        assertThat(foundCandidateIndex.get(0), `is`(13))
        assertThat(foundCandidateIndex.get(1), `is`(42))
        assertThat(foundCandidateIndex.get(2), `is`(67))
    }

    @Test
    fun testConsolidateResultCandidateThroughSequence() {
        //        var mockDocumentSource = createMock<DocumentSource>(DocumentSource::class.java)
        //        var document = Document.createFromText("")
        val sequence = "This article solves the problem where some of our interaction are fauly. " +
            "a 8 9 j 92j 3 3j 9 j 9j Table 1: The reconstruction of the national anthem " +
            "We are interested in the relation between certain information and " +
            "a b b d 1 2 3 4 s 3 3 d9 Table 2: The relation between information and noise " +
            "the related affectionality. " +
            "a b b d 1 2 3 4 5 6 7 Table 3: The relation between homicides and donuts eating " +
            "The relation between homicides and donuts eating is a very important one. "

        val tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(sequence)

        // These triples made in following way: label, starting index (included), ending index (excluded)
        val labels = listOf(
            Triple.of("I-<paragraph>", 0, 1),
            Triple.of("<paragraph>", 1, 24),
            Triple.of("I-<table>", 25, 26),
            Triple.of("<table>", 26, 61),
            Triple.of("I-<paragraph>", 62, 63),
            Triple.of("<paragraph>", 63, 81),
            Triple.of("I-<table>", 82, 83),
            Triple.of("<table>", 82, 118),
            Triple.of("I-<paragraph>", 119, 120),
            Triple.of("<paragraph>", 120, 129),
            Triple.of("I-<table>", 130, 131),
            Triple.of("<table>", 131, 171),
            Triple.of("I-<paragraph>", 171, 172),
            Triple.of("<paragraph>", 172, 195),
        )

        val features = tokens.stream().map { it.text }.collect(Collectors.toList())

        val wapitiResults = GrobidTestUtils.getWapitiResult(features, labels, "\t")
        val wapitiResultsAsList =
            Arrays.stream(wapitiResults.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                .map<List<String>> { l: String ->
                    Arrays.stream(
                        l.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(),
                    )
                        .collect(Collectors.toList())
                }
                .collect(Collectors.toList())

        val table1Tokens = tokens.subList(25, 61)

        val sequenceTokenWithoutSpacesTable1: List<String> = table1Tokens.stream()
            .map { obj: LayoutToken -> obj.text }
            .map { str: String? -> StringUtils.strip(str) }
            .filter { cs: String? -> StringUtils.isNotBlank(cs) }
            .collect(Collectors.toList())

        val candidatesIndexes = Arrays.asList(
            13,
            42,
            67,
        )
        val consolidatedTable1ResultCandidateThroughSequence = FullTextParser.consolidateResultCandidateThroughSequence(
            candidatesIndexes,
            wapitiResultsAsList,
            sequenceTokenWithoutSpacesTable1,
        )

        assertThat(consolidatedTable1ResultCandidateThroughSequence, `is`(13))

        val table2Tokens = tokens.subList(82, 118)

        var sequenceTokenWithoutSpacesTable2: MutableList<String>? = table2Tokens.stream()
            .map { obj: LayoutToken -> obj.text }
            .map { str: String? -> StringUtils.strip(str) }
            .filter { cs: String? -> StringUtils.isNotBlank(cs) }
            .collect(Collectors.toList())

        val consolidatedTable2ResultCandidateThroughSequence = FullTextParser.consolidateResultCandidateThroughSequence(
            candidatesIndexes,
            wapitiResultsAsList,
            sequenceTokenWithoutSpacesTable2,
        )

        assertThat(consolidatedTable2ResultCandidateThroughSequence, `is`(42))

        val table3Tokens = tokens.subList(130, 171)

        var sequenceTokenWithoutSpacesTable3: MutableList<String>? = table3Tokens.stream()
            .map { obj: LayoutToken -> obj.text }
            .map { str: String? -> StringUtils.strip(str) }
            .filter { cs: String? -> StringUtils.isNotBlank(cs) }
            .collect(Collectors.toList())

        val consolidatedTable3ResultCandidateThroughSequence = FullTextParser.consolidateResultCandidateThroughSequence(
            candidatesIndexes,
            wapitiResultsAsList,
            sequenceTokenWithoutSpacesTable3,
        )

        assertThat(consolidatedTable3ResultCandidateThroughSequence, `is`(67))
    }

    @Test
    @Throws(Exception::class)
    fun testShouldOutputBlockStartForRegularBlock() {
        val blockText = "This is a block"
        val doc = Document.createFromText(blockText)
        val documentParts = getWholeDocumentParts(doc)
        val dataAndTokens = FullTextParser.getBodyTextFeatured(doc, documentParts)
        //        LOGGER.debug("data debug: {}", dataAndTokens.getLeft());
        val lines = dataAndTokens.left.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        assertThat(
            "lines[0] fields",
            Arrays.asList(
                *lines[0].split("\\s".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray(),
            ),
            `is`(Matchers.hasItem("BLOCKSTART")),
        )
    }

    @Test
    @Throws(Exception::class)
    fun testShouldOutputBlockStartForBlockStartingWithLineFeed() {
        val blockText = "\nThis is a block"
        val doc = Document.createFromText(blockText)
        assertThat(
            "doc.block[0].tokens[0].text",
            doc.blocks[0].getTokens()[0].text,
            CoreMatchers.`is`("\n"),
        )
        val documentParts = getWholeDocumentParts(doc)
        val dataAndTokens = FullTextParser.getBodyTextFeatured(doc, documentParts)
        //        LOGGER.debug("data debug: {}", dataAndTokens.getLeft());
        val lines = dataAndTokens.left.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        assertThat(
            "lines[0] fields",
            Arrays.asList(*lines[0].split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()),
            `is`(Matchers.hasItem("BLOCKSTART")),
        )
    }

    private fun getWholeDocumentPiece(doc: Document): DocumentPiece = DocumentPiece(
        DocumentPointer(0, 0, 0),
        DocumentPointer(0, doc.tokenizations.size - 1, doc.tokenizations.size - 1),
    )

    private fun getWholeDocumentParts(doc: Document): SortedSet<DocumentPiece> = TreeSet(
        setOf(
            getWholeDocumentPiece(doc),
        ),
    )

    @Test
    fun testRevertResultsForBadItems_shouldRemoveOneTable() {
        var sequence = "This article solves the problem where some of our interaction are fauly. " +
            "a 8 9 j 92j 3 3j 9 j 9j Table 1: The reconstruction of the national anthem " +
            "We are interested in the relation between certain information and " +
            "a b b d 1 2 3 4 s 3 3 d9 Table 2: The relation between information and noise " +
            "the related affectionality. " +
            "a b b d 1 2 3 4 5 6 7 Table 3: The relation between homicides and donuts eating " +
            "The relation between homicides and donuts eating is a very important one. "

        var tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(sequence)

        // These triples made in following way: label, starting index (included), ending index (excluded)
        val labels = listOf(
            Triple.of("I-<paragraph>", 0, 1),
            Triple.of("<paragraph>", 1, 24),
            Triple.of("I-<table>", 25, 26),
            Triple.of("<table>", 26, 61),
            Triple.of("I-<paragraph>", 62, 63),
            Triple.of("<paragraph>", 63, 81),
            Triple.of("I-<table>", 82, 83),
            Triple.of("<table>", 82, 118),
            Triple.of("I-<paragraph>", 119, 120),
            Triple.of("<paragraph>", 120, 129),
            Triple.of("I-<table>", 130, 131),
            Triple.of("<table>", 131, 171),
            Triple.of("I-<paragraph>", 171, 172),
            Triple.of("<paragraph>", 172, 195),
        )

        val features = tokens.stream().map { it.text }.collect(Collectors.toList())
        val wapitiResults = GrobidTestUtils.getWapitiResult(features, labels, "\t")

        val table1 = Table()
        table1.layoutTokens = tokens.subList(25, 61)

        val output = FullTextParser.revertResultsForBadItems(listOf(table1), wapitiResults, true)

        val wapitiResultsAsList = convertResultsToList(output)

        val tablesCount = wapitiResultsAsList.stream()
            .filter { l: List<String> -> l[1] == "I-$TABLE_LABEL" }
            .count()

        assertThat(tablesCount, `is`(2))
    }

    @Test
    fun testRevertResultsForBadItems_shouldLeaveOneFigures() {
        val sequence = "This article solves the problem where some of our interaction are fauly. " +
            "a 8 9 j 92j 3 3j 9 j 9j Table 1: The reconstruction of the national anthem " +
            "We are interested in the relation between certain information and " +
            "a b b d 1 2 3 4 s 3 3 d9 Table 2: The relation between information and noise " +
            "the related affectionality. " +
            "a b b d 1 2 3 4 5 6 7 Table 3: The relation between homicides and donuts eating " +
            "The relation between homicides and donuts eating is a very important one. "

        val tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(sequence)

        // These triples made in following way: label, starting index (included), ending index (excluded)
        val labels = listOf(
            Triple.of("I-<paragraph>", 0, 1),
            Triple.of("<paragraph>", 1, 24),
            Triple.of("I-<figure>", 25, 26),
            Triple.of("<figure>", 26, 61),
            Triple.of("I-<paragraph>", 62, 63),
            Triple.of("<paragraph>", 63, 81),
            Triple.of("I-<figure>", 82, 83),
            Triple.of("<figure>", 82, 118),
            Triple.of("I-<paragraph>", 119, 120),
            Triple.of("<paragraph>", 120, 129),
            Triple.of("I-<figure>", 130, 131),
            Triple.of("<figure>", 131, 171),
            Triple.of("I-<paragraph>", 171, 172),
            Triple.of("<paragraph>", 172, 195),
        )

        val features = tokens.stream().map { it.text }.collect(Collectors.toList())
        val wapitiResults = GrobidTestUtils.getWapitiResult(features, labels, "\t")

        val badFigure1 = Figure()
        badFigure1.layoutTokens = tokens.subList(82, 118)

        val badFigure2 = Figure()
        badFigure2.layoutTokens = tokens.subList(130, 171)

        val output = FullTextParser.revertResultsForBadItems(listOf(badFigure1, badFigure2), wapitiResults, true)

        val wapitiResultsAsList = convertResultsToList(output)

        val figuresCount = wapitiResultsAsList.stream()
            .filter { l: List<String> -> l[1] == "I-$FIGURE_LABEL" }
            .count()

        val tablesCount = wapitiResultsAsList.stream()
            .filter { l: List<String> -> l[1] == "I-$TABLE_LABEL" }
            .count()

        assertThat(figuresCount, `is`(1))
        assertThat(tablesCount, `is`(0))
    }

    @Test
    fun testRevertResultsForBadItems_mixedFiguresTables_shouldRemoveOneFiguresAndOneTable() {
        val sequence = "This article solves the problem where some of our interaction are fauly. " +
            "a 8 9 j 92j 3 3j 9 j 9j Table 1: The reconstruction of the national anthem " +
            "We are interested in the relation between certain information and " +
            "a b b d 1 2 3 4 s 3 3 d9 Table 2: The relation between information " +
            "and noise the related affectionality. " +
            "a b b d 1 2 3 4 5 6 7 Table 3: The relation between homicides and donuts eating " +
            "The relation between homicides and donuts eating is a very important one. "

        val tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(sequence)

        // These triples made in following way: label, starting index (included), ending index (excluded)
        val labels = listOf(
            Triple.of("I-<paragraph>", 0, 1),
            Triple.of("<paragraph>", 1, 24),
            Triple.of("I-<figure>", 25, 26),
            Triple.of("<figure>", 26, 61),
            Triple.of("I-<paragraph>", 62, 63),
            Triple.of("<paragraph>", 63, 81),
            Triple.of("I-<table>", 82, 83),
            Triple.of("<table>", 82, 118),
            Triple.of("I-<paragraph>", 119, 120),
            Triple.of("<paragraph>", 120, 129),
            Triple.of("I-<figure>", 130, 131),
            Triple.of("<figure>", 131, 171),
            Triple.of("I-<paragraph>", 171, 172),
            Triple.of("<paragraph>", 172, 195),
        )

        val features = tokens.stream().map { it.text }.collect(Collectors.toList())
        val wapitiResults = GrobidTestUtils.getWapitiResult(features, labels, "\t")

        val badTable1 = Table()
        badTable1.layoutTokens = tokens.subList(82, 118)

        val badFigure1 = Figure()
        badFigure1.layoutTokens = tokens.subList(130, 171)

        val output = FullTextParser.revertResultsForBadItems(listOf(badTable1, badFigure1), wapitiResults, true)

        val wapitiResultsAsList = convertResultsToList(output)

        val figuresCount = wapitiResultsAsList.stream()
            .filter { l: List<String> -> l[1] == "I-$FIGURE_LABEL" }
            .count()
        val tablesCount = wapitiResultsAsList.stream()
            .filter { l: List<String> -> l[1] == "I-$TABLE_LABEL" }
            .count()

        assertThat(figuresCount, `is`(1))
        assertThat(tablesCount, `is`(0))
    }

    @Test
    fun testRevertDiscardedTokensInMainResults_mixedFiguresTables_shouldRemoveOneFiguresAndOneTable() {
        val sequence = "This article solves the problem where some of our interaction are fauly. " +
            "a 8 9 j 92j 3 3j 9 j 9j Table 1: The reconstruction of the national anthem " +
            "We are interested in the relation between certain information and " +
            "a b b d 1 2 3 4 s 3 3 d9 Table 2: The relation between information " +
            "and noise the related affectionality. " +
            "a b b d 1 2 3 4 5 6 7 Table 3: The relation between homicides and donuts eating " +
            "The relation between homicides and donuts eating is a very important one. "

        val tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(sequence)

        // These triples made in following way: label, starting index (included), ending index (excluded)
        val labels = listOf(
            Triple.of("I-<paragraph>", 0, 1),
            Triple.of("<paragraph>", 1, 25),
            Triple.of("I-<figure>", 25, 26),
            Triple.of("<figure>", 26, 71),
            Triple.of("I-<paragraph>", 71, 73),
            Triple.of("<paragraph>", 73, 81),
            Triple.of("I-<table>", 82, 83),
            Triple.of("<table>", 83, 118),
            Triple.of("I-<paragraph>", 119, 120),
            Triple.of("<paragraph>", 120, 129),
            Triple.of("I-<figure>", 130, 131),
            Triple.of("<figure>", 131, 171),
            Triple.of("I-<paragraph>", 171, 172),
            Triple.of("<paragraph>", 172, 195),
        )

        val features = tokens.stream().map { it.text }.collect(Collectors.toList())
        val wapitiResults = GrobidTestUtils.getWapitiResult(features, labels, "\t")

        val discardedElements = tokens.subList(62, 71)

        val output = FullTextParser.revertDiscardedTokensInMainResults(listOf(discardedElements), wapitiResults)

        val wapitiResultsAsList = convertResultsToList(output)

        val paragraphsCount = wapitiResultsAsList.stream()
            .filter { l: List<String> -> l[1] == "I-$PARAGRAPH_LABEL" }
            .count()
        val figuresCount = wapitiResultsAsList.stream()
            .filter { l: List<String> -> l[1] == "I-$FIGURE_LABEL" }
            .count()
        val tablesCount = wapitiResultsAsList.stream()
            .filter { l: List<String> -> l[1] == "I-$TABLE_LABEL" }
            .count()

        assertThat(paragraphsCount, `is`(5))
        assertThat(figuresCount, `is`(2))
        assertThat(tablesCount, `is`(1))
    }

    private fun convertResultsToList(output: String): MutableList<List<String>> = Arrays.stream(output.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        .map<List<String>> { l: String ->
            Arrays.stream(
                l.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(),
            )
                .collect(Collectors.toList())
        }
        .collect(Collectors.toList())
}
