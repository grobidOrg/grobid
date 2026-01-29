package org.grobid.core.utilities

import org.apache.commons.lang3.StringUtils
import org.grobid.core.utilities.GrobidConfig.ModelParameters
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeAll
import java.util.*
import java.util.stream.Collectors
import kotlin.test.Test

class LabelUtilsTest {

    @Test
    fun testPostProcessLabeledAbstract_shouldTransformTableLabelInParagraphLabel() {
        val resultWithTables =
            "This\tthis\tT\tTh\tThi\tThis\ts\tis\this\tThis\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tNEWFONT\tHIGHERFONT\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t0\t10\t0\tNUMBER\t0\t0\tI-<table>\n" +
                "study\tstudy\ts\tst\tstu\tstud\ty\tdy\tudy\ttudy\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t0\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "was\twas\tw\twa\twas\twas\ts\tas\twas\twas\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t0\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "supported\tsupported\ts\tsu\tsup\tsupp\td\ted\tted\trted\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t0\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "by\tby\tb\tby\tby\tby\ty\tby\tby\tby\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t0\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "the\tthe\tt\tth\tthe\tthe\te\the\tthe\tthe\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t1\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "South\tsouth\tS\tSo\tSou\tSout\th\tth\tuth\touth\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t1\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "Asian\tasian\tA\tAs\tAsi\tAsia\tn\tan\tian\tsian\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t1\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "Clinical\tclinical\tC\tCl\tCli\tClin\tl\tal\tcal\tical\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t1\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "Toxicology\ttoxicology\tT\tTo\tTox\tToxi\ty\tgy\togy\tlogy\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t1\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "Research\tresearch\tR\tRe\tRes\tRese\th\tch\trch\tarch\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t2\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "Collaboration\tcollaboration\tC\tCo\tCol\tColl\tn\ton\tion\ttion\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t2\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tCOMMA\t3\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "which\twhich\tw\twh\twhi\twhic\th\tch\tich\thich\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t3\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "is\tis\ti\tis\tis\tis\ts\tis\tis\tis\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t3\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "funded\tfunded\tf\tfu\tfun\tfund\td\ted\tded\tnded\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t3\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "by\tby\tb\tby\tby\tby\ty\tby\tby\tby\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t3\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "The\tthe\tT\tTh\tThe\tThe\te\the\tThe\tThe\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t3\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "Wellcome\twellcome\tW\tWe\tWel\tWell\te\tme\tome\tcome\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t4\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "Trust\ttrust\tT\tTr\tTru\tTrus\tt\tst\tust\trust\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t4\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "/\t/\t/\t/\t/\t/\t/\t/\t/\t/\tBLOCKIN\tLINEEND\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t4\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "National\tnational\tN\tNa\tNat\tNati\tl\tal\tnal\tonal\tBLOCKIN\tLINESTART\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t4\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "Health\thealth\tH\tHe\tHea\tHeal\th\tth\tlth\talth\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t5\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "and\tand\ta\tan\tand\tand\td\tnd\tand\tand\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t5\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "Medical\tmedical\tM\tMe\tMed\tMedi\tl\tal\tcal\tical\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t5\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "Research\tresearch\tR\tRe\tRes\tRese\th\tch\trch\tarch\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t5\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "Council\tcouncil\tC\tCo\tCou\tCoun\tl\til\tcil\tncil\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t6\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "International\tinternational\tI\tIn\tInt\tInte\tl\tal\tnal\tonal\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t6\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "Collaborative\tcollaborative\tC\tCo\tCol\tColl\te\tve\tive\ttive\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t6\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "Research\tresearch\tR\tRe\tRes\tRese\th\tch\trch\tarch\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t7\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "Grant\tgrant\tG\tGr\tGra\tGran\tt\tnt\tant\trant\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t7\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "GR071669MA\tgr071669ma\tG\tGR\tGR0\tGR07\tA\tMA\t9MA\t69MA\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tCONTAINSDIGITS\t0\tNOPUNCT\t8\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t8\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "The\tthe\tT\tTh\tThe\tThe\te\the\tThe\tThe\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t8\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "funding\tfunding\tf\tfu\tfun\tfund\tg\tng\ting\tding\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "bodies\tbodies\tb\tbo\tbod\tbodi\ts\tes\ties\tdies\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "had\thad\th\tha\thad\thad\td\tad\thad\thad\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "no\tno\tn\tno\tno\tno\to\tno\tno\tno\tBLOCKIN\tLINEEND\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "role\trole\tr\tro\trol\trole\te\tle\tole\trole\tBLOCKIN\tLINESTART\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "in\tin\ti\tin\tin\tin\tn\tin\tin\tin\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "analyzing\tanalyzing\ta\tan\tana\tanal\tg\tng\ting\tzing\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "or\tor\to\tor\tor\tor\tr\tor\tor\tor\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t10\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "interpreting\tinterpreting\ti\tin\tint\tinte\tg\tng\ting\tting\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t10\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "the\tthe\tt\tth\tthe\tthe\te\the\tthe\tthe\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t10\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "data\tdata\td\tda\tdat\tdata\ta\tta\tata\tdata\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t10\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "or\tor\to\tor\tor\tor\tr\tor\tor\tor\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t11\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "writing\twriting\tw\twr\twri\twrit\tg\tng\ting\tting\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t11\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "the\tthe\tt\tth\tthe\tthe\te\the\tthe\tthe\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t11\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                "article\tarticle\ta\tar\tart\tarti\te\tle\tcle\ticle\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t11\t10\t0\tNUMBER\t0\t0\t<table>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t11\t10\t0\tNUMBER\t0\t0\t<table>"

        val postprocessed = LabelUtils.postProcessFullTextLabeledText(resultWithTables)

        assertThat(
            Arrays.stream(StringUtils.split(postprocessed, "\n"))
                .filter { l -> l.endsWith("<table>") }
                .count(),
            `is`(0L),
        )

        assertThat(
            Arrays.stream(StringUtils.split(postprocessed, "\n"))
                .filter { l -> l.endsWith("<paragraph>") }
                .count(),
            `is`(
                Arrays.stream(StringUtils.split(resultWithTables, "\n"))
                    .filter { l -> l.endsWith("<table>") }
                    .count(),
            ),
        )
    }

//    fun testAdjustInvalidSequenceOfStartLabels() {
//        val inputStream = javaClass.getResourceAsStream("bodyResults-sample.1.txt")
//        val bodyResult = inputStream?.bufferedReader().use { it.readText() }
//
//        val postProcessed = LabelUtils.postProcessFullTextLabeledText(bodyResult)
//    }

    @Test
    fun testPostProcessFulltextFixInvalidTableOrFigure_noChangeNeeded_shouldReturnSameTableOrFigureSequence() {
        val bodyResult =
            "B\tb\tB\tB\tB\tB\tB\tB\tB\tB\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tHIGHERFONT\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t8\t11\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKEND\tLINEEND\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t8\t11\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "014306\t014306\t0\t01\t014\t0143\t6\t06\t306\t4306\tBLOCKSTART\tLINESTART\tLINEINDENT\tSAMEFONT\tLOWERFONT\t0\t0\tNOCAPS\tALLDIGIT\t0\tNOPUNCT\t8\t11\t0\tNUMBER\t0\t0\tI-<paragraph>\n" +
                "-\t-\t-\t-\t-\t-\t-\t-\t-\t-\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tHYPHEN\t8\t11\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "4\t4\t4\t4\t4\t4\t4\t4\t4\t4\tBLOCKEND\tLINEEND\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t8\t11\t0\tNUMBER\t1\t0\t<paragraph>\n" +
                "FIG\tfig\tF\tFI\tFIG\tFIG\tG\tIG\tFIG\tFIG\tBLOCKSTART\tLINESTART\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t1\t0\tI-<figure_marker>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t8\t3\t0\tNUMBER\t0\t0\tI-<paragraph>\n" +
                "The\tthe\tT\tTh\tThe\tThe\te\the\tThe\tThe\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "average\taverage\ta\tav\tave\taver\te\tge\tage\trage\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "distances\tdistances\td\tdi\tdis\tdist\ts\tes\tces\tnces\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "between\tbetween\tb\tbe\tbet\tbetw\tn\ten\teen\tween\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "nucleons\tnucleons\tn\tnu\tnuc\tnucl\ts\tns\tons\teons\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "in\tin\ti\tin\tin\tin\tn\tin\tin\tin\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "10\t10\t1\t10\t10\t10\t0\t10\t10\t10\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tLOWERFONT\t0\t0\tNOCAPS\tALLDIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t1\t1\t<paragraph>\n" +
                "Be\tbe\tB\tBe\tBe\tBe\te\tBe\tBe\tBe\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tHIGHERFONT\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "(\t(\t(\t(\t(\t(\t(\t(\t(\t(\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tOPENBRACKET\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "+\t+\t+\t+\t+\t+\t+\t+\t+\t+\tBLOCKIN\tLINEIN\tLINEINDENT\tNEWFONT\tLOWERFONT\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t1\t<paragraph>\n" +
                "1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tBLOCKIN\tLINEIN\tLINEINDENT\tNEWFONT\tHIGHERFONT\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t1\t0\t<paragraph>\n" +
                ")\t)\t)\t)\t)\t)\t)\t)\t)\t)\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tENDBRACKET\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tBLOCKIN\tLINEEND\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tCOMMA\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "10\t10\t1\t10\t10\t10\t0\t10\t10\t10\tBLOCKIN\tLINESTART\tALIGNEDLEFT\tSAMEFONT\tLOWERFONT\t0\t0\tNOCAPS\tALLDIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t1\t1\t<paragraph>\n" +
                "B\tb\tB\tB\tB\tB\tB\tB\tB\tB\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tHIGHERFONT\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "(\t(\t(\t(\t(\t(\t(\t(\t(\t(\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tOPENBRACKET\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "3\t3\t3\t3\t3\t3\t3\t3\t3\t3\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t1\t0\t<paragraph>\n" +
                "+\t+\t+\t+\t+\t+\t+\t+\t+\t+\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tNEWFONT\tLOWERFONT\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t1\t<paragraph>\n" +
                "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tNEWFONT\tHIGHERFONT\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                ")\t)\t)\t)\t)\t)\t)\t)\t)\t)\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tENDBRACKET\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tCOMMA\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "10\t10\t1\t10\t10\t10\t0\t10\t10\t10\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tLOWERFONT\t0\t0\tNOCAPS\tALLDIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t1\t1\tI-<citation_marker>\n" +
                "B\tb\tB\tB\tB\tB\tB\tB\tB\tB\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tHIGHERFONT\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\tI-<paragraph>\n" +
                "(\t(\t(\t(\t(\t(\t(\t(\t(\t(\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tOPENBRACKET\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t1\t0\t<paragraph>\n" +
                "+\t+\t+\t+\t+\t+\t+\t+\t+\t+\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tNEWFONT\tLOWERFONT\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t1\t<paragraph>\n" +
                "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tNEWFONT\tHIGHERFONT\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                ")\t)\t)\t)\t)\t)\t)\t)\t)\t)\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tENDBRACKET\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tCOMMA\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "and\tand\ta\tan\tand\tand\td\tnd\tand\tand\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "10\t10\t1\t10\t10\t10\t0\t10\t10\t10\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tLOWERFONT\t0\t0\tNOCAPS\tALLDIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t1\t1\t<paragraph>\n" +
                "C\tc\tC\tC\tC\tC\tC\tC\tC\tC\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tHIGHERFONT\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "(\t(\t(\t(\t(\t(\t(\t(\t(\t(\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tOPENBRACKET\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "+\t+\t+\t+\t+\t+\t+\t+\t+\t+\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tNEWFONT\tLOWERFONT\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t1\t<paragraph>\n" +
                "1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tNEWFONT\tHIGHERFONT\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t1\t0\t<paragraph>\n" +
                ")\t)\t)\t)\t)\t)\t)\t)\t)\t)\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tENDBRACKET\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "states\tstates\ts\tst\tsta\tstat\ts\tes\ttes\tates\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "The\tthe\tT\tTh\tThe\tThe\te\the\tThe\tThe\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "solid\tsolid\ts\tso\tsol\tsoli\td\tid\tlid\tolid\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "lines\tlines\tl\tli\tlin\tline\ts\tes\tnes\tines\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "denote\tdenote\td\tde\tden\tdeno\te\tte\tote\tnote\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "the\tthe\tt\tth\tthe\tthe\te\the\tthe\tthe\tBLOCKIN\tLINEEND\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "average\taverage\ta\tav\tave\taver\te\tge\tage\trage\tBLOCKIN\tLINESTART\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "distances\tdistances\td\tdi\tdis\tdist\ts\tes\tces\tnces\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "r\tr\tr\tr\tr\tr\tr\tr\tr\tr\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tNEWFONT\tSAMEFONTSIZE\t0\t1\tNOCAPS\tNODIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "N\tn\tN\tN\tN\tN\tN\tN\tN\tN\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tLOWERFONT\t0\t1\tALLCAP\tNODIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t1\tALLCAP\tNODIGIT\t1\tCOMMA\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "N\tn\tN\tN\tN\tN\tN\tN\tN\tN\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t1\tALLCAP\tNODIGIT\t1\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "between\tbetween\tb\tbe\tbet\tbetw\tn\ten\teen\tween\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tNEWFONT\tHIGHERFONT\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "two\ttwo\tt\ttw\ttwo\ttwo\to\two\ttwo\ttwo\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "valence\tvalence\tv\tva\tval\tvale\te\tce\tnce\tence\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "nucleons\tnucleons\tn\tnu\tnuc\tnucl\ts\tns\tons\teons\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "The\tthe\tT\tTh\tThe\tThe\te\the\tThe\tThe\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t8\t3\t0\tNUMBER\t0\t0\t<paragraph>\n"

        val postProcessed = LabelUtils.postProcessFulltextFixInvalidTableOrFigure(bodyResult)

        assertThat(postProcessed, `is`(bodyResult))
    }

    @Test
    fun testPostProcessFulltextFixInvalidTableOrFigure_singleChangeNeeded_shouldCorrectTheTableOrFigureSequence() {
        val bodyResult =
            "of\tof\to\tof\tof\tof\tf\tof\tof\tof\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "10\t10\t1\t10\t10\t10\t0\t10\t10\t10\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tLOWERFONT\t0\t0\tNOCAPS\tALLDIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t1\t1\tI-<citation_marker>\n" +
                "B\tb\tB\tB\tB\tB\tB\tB\tB\tB\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tHIGHERFONT\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\tI-<figure>\n" +
                "and\tand\ta\tan\tand\tand\td\tnd\tand\tand\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\tI-<figure>\n" +
                "the\tthe\tt\tth\tthe\tthe\te\the\tthe\tthe\tBLOCKIN\tLINEEND\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\tI-<figure>\n" +
                "1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tBLOCKIN\tLINESTART\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t9\t5\t0\tNUMBER\t1\t0\t<figure>\n" +
                "+\t+\t+\t+\t+\t+\t+\t+\t+\t+\tBLOCKIN\tLINEEND\tALIGNEDLEFT\tNEWFONT\tLOWERFONT\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t1\t<figure>\n" +
                "1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tBLOCKIN\tLINESTART\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t9\t5\t0\tNUMBER\t1\t0\t<figure>\n" +
                "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tHIGHERFONT\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                "state\tstate\ts\tst\tsta\tstat\te\tte\tate\ttate\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                "of\tof\to\tof\tof\tof\tf\tof\tof\tof\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                "10\t10\t1\t10\t10\t10\t0\t10\t10\t10\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tLOWERFONT\t0\t0\tNOCAPS\tALLDIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t1\t1\t<figure>\n" +
                "B\tb\tB\tB\tB\tB\tB\tB\tB\tB\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tHIGHERFONT\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                "The\tthe\tT\tTh\tThe\tThe\te\the\tThe\tThe\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                "panels\tpanels\tp\tpa\tpan\tpane\ts\tls\tels\tnels\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                "(\t(\t(\t(\t(\t(\t(\t(\t(\t(\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tOPENBRACKET\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                "a\ta\ta\ta\ta\ta\ta\ta\ta\ta\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t1\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                ")\t)\t)\t)\t)\t)\t)\t)\t)\t)\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tENDBRACKET\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                "are\tare\ta\tar\tare\tare\te\tre\tare\tare\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                "calculated\tcalculated\tc\tca\tcal\tcalc\td\ted\tted\tated\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n"

        val postProcessed = LabelUtils.postProcessFulltextFixInvalidTableOrFigure(bodyResult)

        assertThat(postProcessed, not(bodyResult))

        val splitResult =
            Arrays.stream(postProcessed.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                .map<List<String>> { l: String ->
                    Arrays.stream(
                        l.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(),
                    )
                        .collect(Collectors.toList())
                }
                .collect(Collectors.toList())

        val countStartingFigure = splitResult.stream()
            .map { l: List<String> -> l.last() }
            .filter { l: String -> l.equals("I-<figure>") }
            .count()

        assertThat(countStartingFigure, `is`(1))
    }

    @Test
    fun testPostProcessFulltextFixInvalidTableOrFigure_MultipleChangeNeeded_shouldCorrectTheTableOrFigureSequence() {
        val bodyResult =
            "of\tof\to\tof\tof\tof\tf\tof\tof\tof\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<paragraph>\n" +
                "10\t10\t1\t10\t10\t10\t0\t10\t10\t10\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tLOWERFONT\t0\t0\tNOCAPS\tALLDIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t1\t1\tI-<citation_marker>\n" +
                "B\tb\tB\tB\tB\tB\tB\tB\tB\tB\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tHIGHERFONT\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\tI-<figure>\n" +
                "and\tand\ta\tan\tand\tand\td\tnd\tand\tand\tBLOCKIN\tLINEIN\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\tI-<figure>\n" +
                "the\tthe\tt\tth\tthe\tthe\te\the\tthe\tthe\tBLOCKIN\tLINEEND\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\tI-<figure>\n" +
                "1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tBLOCKIN\tLINESTART\tALIGNEDLEFT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t9\t5\t0\tNUMBER\t1\t0\t<figure>\n" +
                "+\t+\t+\t+\t+\t+\t+\t+\t+\t+\tBLOCKIN\tLINEEND\tALIGNEDLEFT\tNEWFONT\tLOWERFONT\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t1\t<figure>\n" +
                "1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tBLOCKIN\tLINESTART\tLINEINDENT\tNEWFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t9\t5\t0\tNUMBER\t1\t0\t<figure>\n" +
                "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tHIGHERFONT\t0\t0\tNOCAPS\tALLDIGIT\t1\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                "state\tstate\ts\tst\tsta\tstat\te\tte\tate\ttate\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                "of\tof\to\tof\tof\tof\tf\tof\tof\tof\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                "10\t10\t1\t10\t10\t10\t0\t10\t10\t10\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tLOWERFONT\t0\t0\tNOCAPS\tALLDIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t1\t1\t<figure>\n" +
                "B\tb\tB\tB\tB\tB\tB\tB\tB\tB\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tHIGHERFONT\t0\t0\tALLCAP\tNODIGIT\t1\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tDOT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                "The\tthe\tT\tTh\tThe\tThe\te\the\tThe\tThe\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tINITCAP\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\tI-<figure>\n" +
                "panels\tpanels\tp\tpa\tpan\tpane\ts\tls\tels\tnels\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\tI-<figure>\n" +
                "(\t(\t(\t(\t(\t(\t(\t(\t(\t(\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tOPENBRACKET\t9\t5\t0\tNUMBER\t0\t0\tI-<figure>\n" +
                "a\ta\ta\ta\ta\ta\ta\ta\ta\ta\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t1\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                ")\t)\t)\t)\t)\t)\t)\t)\t)\t)\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tALLCAP\tNODIGIT\t1\tENDBRACKET\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                "are\tare\ta\tar\tare\tare\te\tre\tare\tare\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                "calculated\tcalculated\tc\tca\tcal\tcalc\td\ted\tted\tated\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<figure>\n" +
                "calculated\tcalculated\tc\tca\tcal\tcalc\td\ted\tted\tated\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\tI-<table>\n" +
                "calculated\tcalculated\tc\tca\tcal\tcalc\td\ted\tted\tated\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\tI-<table>\n" +
                "calculated\tcalculated\tc\tca\tcal\tcalc\td\ted\tted\tated\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\tI-<table>\n" +
                "calculated\tcalculated\tc\tca\tcal\tcalc\td\ted\tted\tated\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<table>\n" +
                "calculated\tcalculated\tc\tca\tcal\tcalc\td\ted\tted\tated\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<table>\n" +
                "calculated\tcalculated\tc\tca\tcal\tcalc\td\ted\tted\tated\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<table>\n" +
                "calculated\tcalculated\tc\tca\tcal\tcalc\td\ted\tted\tated\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<table>\n" +
                "calculated\tcalculated\tc\tca\tcal\tcalc\td\ted\tted\tated\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<table>\n" +
                "calculated\tcalculated\tc\tca\tcal\tcalc\td\ted\tted\tated\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<table>\n" +
                "calculated\tcalculated\tc\tca\tcal\tcalc\td\ted\tted\tated\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<table>\n" +
                "calculated\tcalculated\tc\tca\tcal\tcalc\td\ted\tted\tated\tBLOCKIN\tLINEIN\tLINEINDENT\tSAMEFONT\tSAMEFONTSIZE\t0\t0\tNOCAPS\tNODIGIT\t0\tNOPUNCT\t9\t5\t0\tNUMBER\t0\t0\t<table>\n"

        val postProcessed = LabelUtils.postProcessFulltextFixInvalidTableOrFigure(bodyResult)

        assertThat(postProcessed, not(bodyResult))

        val splitResult =
            Arrays.stream<String>(postProcessed.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                .map<List<String>> { l: String ->
                    Arrays.stream<String>(
                        l.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(),
                    )
                        .collect(Collectors.toList<String>())
                }
                .collect(Collectors.toList<List<String>>())

        val countStartingFigure = splitResult.stream()
            .map { l: List<String> -> l.last() }
            .filter { l: String -> l.equals("I-<figure>") }
            .count()

        assertThat(countStartingFigure, `is`(2))

        val countStartingTables = splitResult.stream()
            .map { l: List<String> -> l.last() }
            .filter { l: String -> l.equals("I-<table>") }
            .count()

        assertThat(countStartingTables, `is`(1))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        @Throws(Exception::class)
        fun before() {
            val modelParameters = ModelParameters()
            modelParameters.name = "bao"
            GrobidProperties.addModel(modelParameters)
        }
    }
}
