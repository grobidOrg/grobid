package org.grobid.core.engines

import nu.xom.Builder
import nu.xom.Document
import nu.xom.Element
import org.grobid.core.GrobidModels
import org.grobid.core.analyzers.GrobidAnalyzer
import org.grobid.core.data.Funder
import org.grobid.core.data.Funding
import org.grobid.core.layout.LayoutToken
import org.grobid.core.utilities.GrobidConfig
import org.grobid.core.utilities.GrobidProperties
import org.grobid.core.utilities.LayoutTokensUtil
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.Before
import org.junit.Test

class FundingAcknowledgementParserTest {

    private lateinit var target: FundingAcknowledgementParser

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val modelParameters = GrobidConfig.ModelParameters()
        modelParameters.name = "bao"
        GrobidProperties.addModel(modelParameters)
        target = FundingAcknowledgementParser(GrobidModels.DUMMY)
    }

    @Test
    fun testGetExtractionResult() {
        val input = "Our warmest thanks to Patrice Lopez, the author of Grobid [22], DeLFT [20], and other open-source projects for his continuous support and inspiration with ideas, suggestions, and fruitful discussions. We thank Pedro Baptista de Castro for his support during this work. Special thanks to Erina Fujita for useful tips on the manuscript."

        val results: String = "Our\tour\tO\tOu\tOur\tOur\tr\tur\tOur\tOur\tLINESTART\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<other>\n" +
            "warmest\twarmest\tw\twa\twar\twarm\tt\tst\test\tmest\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "thanks\tthanks\tt\tth\ttha\tthan\ts\tks\tnks\tanks\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "to\tto\tt\tto\tto\tto\to\tto\tto\tto\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "Patrice\tpatrice\tP\tPa\tPat\tPatr\te\tce\tice\trice\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<person>\n" +
            "Lopez\tlopez\tL\tLo\tLop\tLope\tz\tez\tpez\topez\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<person>\n" +
            ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tCOMMA\t0\tI-<other>\n" +
            "the\tthe\tt\tth\tthe\tthe\te\the\tthe\tthe\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "author\tauthor\ta\tau\taut\tauth\tr\tor\thor\tthor\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "Grobid\tgrobid\tG\tGr\tGro\tGrob\td\tid\tbid\tobid\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "[\t[\t[\t[\t[\t[\t[\t[\t[\t[\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tOPENBRACKET\t0\t<other>\n" +
            "22\t22\t2\t22\t22\t22\t2\t22\t22\t22\tLINEIN\tNOCAPS\tALLDIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "]\t]\t]\t]\t]\t]\t]\t]\t]\t]\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tENDBRACKET\t0\t<other>\n" +
            ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tCOMMA\t0\t<other>\n" +
            "DeLFT\tdelft\tD\tDe\tDeL\tDeLF\tT\tFT\tLFT\teLFT\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "[\t[\t[\t[\t[\t[\t[\t[\t[\t[\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tOPENBRACKET\t0\t<other>\n" +
            "20\t20\t2\t20\t20\t20\t0\t20\t20\t20\tLINEIN\tNOCAPS\tALLDIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "]\t]\t]\t]\t]\t]\t]\t]\t]\t]\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tENDBRACKET\t0\t<other>\n" +
            ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tCOMMA\t0\t<other>\n" +
            "and\tand\ta\tan\tand\tand\td\tnd\tand\tand\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "other\tother\to\tot\toth\tothe\tr\ter\ther\tther\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "open\topen\to\top\tope\topen\tn\ten\tpen\topen\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "-\t-\t-\t-\t-\t-\t-\t-\t-\t-\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tHYPHEN\t0\t<other>\n" +
            "source\tsource\ts\tso\tsou\tsour\te\tce\trce\turce\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "projects\tprojects\tp\tpr\tpro\tproj\ts\tts\tcts\tects\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "for\tfor\tf\tfo\tfor\tfor\tr\tor\tfor\tfor\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "his\this\th\thi\this\this\ts\tis\this\this\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "continuous\tcontinuous\tc\tco\tcon\tcont\ts\tus\tous\tuous\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "support\tsupport\ts\tsu\tsup\tsupp\tt\trt\tort\tport\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "and\tand\ta\tan\tand\tand\td\tnd\tand\tand\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "inspiration\tinspiration\ti\tin\tins\tinsp\tn\ton\tion\ttion\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "with\twith\tw\twi\twit\twith\th\tth\tith\twith\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "ideas\tideas\ti\tid\tide\tidea\ts\tas\teas\tdeas\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tCOMMA\t0\t<other>\n" +
            "suggestions\tsuggestions\ts\tsu\tsug\tsugg\ts\tns\tons\tions\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tCOMMA\t0\t<other>\n" +
            "and\tand\ta\tan\tand\tand\td\tnd\tand\tand\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "fruitful\tfruitful\tf\tfr\tfru\tfrui\tl\tul\tful\ttful\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "discussions\tdiscussions\td\tdi\tdis\tdisc\ts\tns\tons\tions\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tDOT\t0\t<other>\n" +
            "We\twe\tW\tWe\tWe\tWe\te\tWe\tWe\tWe\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "thank\tthank\tt\tth\ttha\tthan\tk\tnk\tank\thank\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "Pedro\tpedro\tP\tPe\tPed\tPedr\to\tro\tdro\tedro\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<person>\n" +
            "Baptista\tbaptista\tB\tBa\tBap\tBapt\ta\tta\tsta\tista\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<person>\n" +
            "de\tde\td\tde\tde\tde\te\tde\tde\tde\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<person>\n" +
            "Castro\tcastro\tC\tCa\tCas\tCast\to\tro\ttro\tstro\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<person>\n" +
            "for\tfor\tf\tfo\tfor\tfor\tr\tor\tfor\tfor\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<other>\n" +
            "his\this\th\thi\this\this\ts\tis\this\this\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "support\tsupport\ts\tsu\tsup\tsupp\tt\trt\tort\tport\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "during\tduring\td\tdu\tdur\tduri\tg\tng\ting\tring\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "this\tthis\tt\tth\tthi\tthis\ts\tis\this\tthis\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "work\twork\tw\two\twor\twork\tk\trk\tork\twork\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tDOT\t0\t<other>\n" +
            "Special\tspecial\tS\tSp\tSpe\tSpec\tl\tal\tial\tcial\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "thanks\tthanks\tt\tth\ttha\tthan\ts\tks\tnks\tanks\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "to\tto\tt\tto\tto\tto\to\tto\tto\tto\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "Erina\terina\tE\tEr\tEri\tErin\ta\tna\tina\trina\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<person>\n" +
            "Fujita\tfujita\tF\tFu\tFuj\tFuji\ta\tta\tita\tjita\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<person>\n" +
            "for\tfor\tf\tfo\tfor\tfor\tr\tor\tfor\tfor\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<other>\n" +
            "useful\tuseful\tu\tus\tuse\tusef\tl\tul\tful\teful\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "tips\ttips\tt\tti\ttip\ttips\ts\tps\tips\ttips\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "on\ton\to\ton\ton\ton\tn\ton\ton\ton\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "the\tthe\tt\tth\tthe\tthe\te\the\tthe\tthe\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "manuscript\tmanuscript\tm\tma\tman\tmanu\tt\tpt\tipt\tript\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEEND\tALLCAP\tNODIGIT\t1\t0\t0\tDOT\t0\t<other>"

        val tokens: List<LayoutToken> = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input)

        val (element, fundingAcknowledgmentParse) = target.getExtractionResult(tokens, results)

        assertThat(fundingAcknowledgmentParse.fundings, hasSize(0))
        assertThat(fundingAcknowledgmentParse.persons, hasSize(3))
        assertThat(fundingAcknowledgmentParse.persons.get(0).rawName, `is`("Patrice Lopez"))
        assertThat(fundingAcknowledgmentParse.persons.get(1).rawName, `is`("Pedro Baptista de Castro"))
        assertThat(fundingAcknowledgmentParse.persons.get(2).rawName, `is`("Erina Fujita"))
        assertThat(fundingAcknowledgmentParse.affiliations, hasSize(0))
    }

    @Test
    fun testGetExtractionResult2() {
        val input = "This work was partly supported by MEXT Program: Data Creation and Utilization-Type Material Research and Development Project (Digital Transformation Initiative Center for Magnetic Materials) Grant Number [JPMXP1122715503]."

        val results: String = "This\tthis\tT\tTh\tThi\tThis\ts\tis\this\tThis\tLINESTART\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<other>\n" +
            "work\twork\tw\two\twor\twork\tk\trk\tork\twork\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "was\twas\tw\twa\twas\twas\ts\tas\twas\twas\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "partly\tpartly\tp\tpa\tpar\tpart\ty\tly\ttly\trtly\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "supported\tsupported\ts\tsu\tsup\tsupp\td\ted\tted\trted\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "by\tby\tb\tby\tby\tby\ty\tby\tby\tby\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "MEXT\tmext\tM\tME\tMEX\tMEXT\tT\tXT\tEXT\tMEXT\tLINEIN\tALLCAP\tNODIGIT\t0\t1\t0\tNOPUNCT\t0\tI-<funderName>\n" +
            "Program\tprogram\tP\tPr\tPro\tProg\tm\tam\tram\tgram\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<other>\n" +
            ":\t:\t:\t:\t:\t:\t:\t:\t:\t:\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tPUNCT\t0\t<other>\n" +
            "Data\tdata\tD\tDa\tDat\tData\ta\tta\tata\tData\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<programName>\n" +
            "Creation\tcreation\tC\tCr\tCre\tCrea\tn\ton\tion\ttion\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "and\tand\ta\tan\tand\tand\td\tnd\tand\tand\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "Utilization\tutilization\tU\tUt\tUti\tUtil\tn\ton\tion\ttion\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "-\t-\t-\t-\t-\t-\t-\t-\t-\t-\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tHYPHEN\t0\t<programName>\n" +
            "Type\ttype\tT\tTy\tTyp\tType\te\tpe\type\tType\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "Material\tmaterial\tM\tMa\tMat\tMate\tl\tal\tial\trial\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "Research\tresearch\tR\tRe\tRes\tRese\th\tch\trch\tarch\tLINEIN\tINITCAP\tNODIGIT\t0\t1\t0\tNOPUNCT\t0\t<programName>\n" +
            "and\tand\ta\tan\tand\tand\td\tnd\tand\tand\tLINEIN\tNOCAPS\tNODIGIT\t0\t1\t0\tNOPUNCT\t0\t<programName>\n" +
            "Development\tdevelopment\tD\tDe\tDev\tDeve\tt\tnt\tent\tment\tLINEIN\tINITCAP\tNODIGIT\t0\t1\t0\tNOPUNCT\t0\t<programName>\n" +
            "Project\tproject\tP\tPr\tPro\tProj\tt\tct\tect\tject\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "(\t(\t(\t(\t(\t(\t(\t(\t(\t(\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tOPENBRACKET\t0\t<programName>\n" +
            "Digital\tdigital\tD\tDi\tDig\tDigi\tl\tal\ttal\tital\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "Transformation\ttransformation\tT\tTr\tTra\tTran\tn\ton\tion\ttion\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "Initiative\tinitiative\tI\tIn\tIni\tInit\te\tve\tive\ttive\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "Center\tcenter\tC\tCe\tCen\tCent\tr\ter\tter\tnter\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "for\tfor\tf\tfo\tfor\tfor\tr\tor\tfor\tfor\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "Magnetic\tmagnetic\tM\tMa\tMag\tMagn\tc\tic\ttic\tetic\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "Materials\tmaterials\tM\tMa\tMat\tMate\ts\tls\tals\tials\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            ")\t)\t)\t)\t)\t)\t)\t)\t)\t)\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tENDBRACKET\t0\t<programName>\n" +
            "Grant\tgrant\tG\tGr\tGra\tGran\tt\tnt\tant\trant\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<other>\n" +
            "Number\tnumber\tN\tNu\tNum\tNumb\tr\ter\tber\tmber\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "[\t[\t[\t[\t[\t[\t[\t[\t[\t[\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tOPENBRACKET\t0\t<other>\n" +
            "JPMXP1122715503\tjpmxp1122715503\tJ\tJP\tJPM\tJPMX\t3\t03\t503\t5503\tLINEIN\tALLCAP\tCONTAINSDIGITS\t0\t0\t0\tNOPUNCT\t0\tI-<grantNumber>\n" +
            "]\t]\t]\t]\t]\t]\t]\t]\t]\t]\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tENDBRACKET\t0\tI-<other>\n" +
            ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEEND\tALLCAP\tNODIGIT\t1\t0\t0\tDOT\t0\t<other>"

        val tokens: List<LayoutToken> = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input)

        val (element, fundingAcknowledgmentParse) = target.getExtractionResult(tokens, results)

        assertThat(fundingAcknowledgmentParse.fundings, hasSize(1))
        val funding1: Funding = fundingAcknowledgmentParse.fundings.get(0)
        val funder1: Funder = funding1.funder
//        assertThat(funder1.fullName, `is`("MEXT"))
        assertThat(funding1.programFullName, `is`("Data Creation and Utilization-Type Material Research and Development Project (Digital Transformation Initiative Center for Magnetic Materials)"))
        assertThat(funder1.fullName, `is`("Ministry of Education, Culture, Sports, Science and Technology"))
        assertThat(fundingAcknowledgmentParse.persons, hasSize(0))
        assertThat(fundingAcknowledgmentParse.affiliations, hasSize(0))
    }

    @Test
    fun extractSentencesAndPositionsFromParagraphElement_shouldReturnValidIntervals() {
        // Here the namespace is already removed as it must be removed when the node arrives at the method we are testing
        val input = "\n" +
            "\t\t\t<div type=\"acknowledgement\">\n" +
            "<div><head>Acknowledgements</head><p><s>Our warmest thanks to Patrice Lopez, the author of Grobid <ref type=\"bibr\" target=\"#b21\">[22]</ref>, DeLFT <ref type=\"bibr\" target=\"#b19\">[20]</ref>, and other open-source projects for his continuous support and inspiration with ideas, suggestions, and fruitful discussions.</s><s>We thank Pedro Baptista de Castro for his support during this work.</s><s>Special thanks to Erina Fujita for useful tips on the manuscript.</s></p></div>\n" +
            "\t\t\t</div>\n\n"

        val parser = Builder()
        val localDoc: Document = parser.build(input, null)
        val root = localDoc.rootElement
        val paragraphs = root.query("//p")

        val firstParagraphText = paragraphs[0].value

        val (strings, offsetPositions) = FundingAcknowledgementParser.extractSentencesAndPositionsFromParagraphElement(
            paragraphs[0] as Element?,
        )

        assertThat(strings, hasSize(3))
        assertThat(offsetPositions, hasSize(3))
        assertThat(
            firstParagraphText.substring(offsetPositions[0].start, offsetPositions[0].end),
            `is`("Our warmest thanks to Patrice Lopez, the author of Grobid [22], DeLFT [20], and other open-source projects for his continuous support and inspiration with ideas, suggestions, and fruitful discussions."),
        )
        assertThat(
            firstParagraphText.substring(offsetPositions[1].start, offsetPositions[1].end),
            `is`("We thank Pedro Baptista de Castro for his support during this work."),
        )
        assertThat(
            firstParagraphText.substring(offsetPositions[2].start, offsetPositions[2].end),
            `is`("Special thanks to Erina Fujita for useful tips on the manuscript."),
        )
    }

    @Test
    fun testGetExtractionResultNew1_ShouldReturnCorrectElementsAndPositions() {
        val input = "Our warmest thanks to Patrice Lopez, the author of Grobid [22], DeLFT [20], and other open-source projects for his continuous support and inspiration with ideas, suggestions, and fruitful discussions. We thank Pedro Baptista de Castro for his support during this work. Special thanks to Erina Fujita for useful tips on the manuscript."

        val results: String = "Our\tour\tO\tOu\tOur\tOur\tr\tur\tOur\tOur\tLINESTART\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<other>\n" +
            "warmest\twarmest\tw\twa\twar\twarm\tt\tst\test\tmest\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "thanks\tthanks\tt\tth\ttha\tthan\ts\tks\tnks\tanks\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "to\tto\tt\tto\tto\tto\to\tto\tto\tto\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "Patrice\tpatrice\tP\tPa\tPat\tPatr\te\tce\tice\trice\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<person>\n" +
            "Lopez\tlopez\tL\tLo\tLop\tLope\tz\tez\tpez\topez\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<person>\n" +
            ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tCOMMA\t0\tI-<other>\n" +
            "the\tthe\tt\tth\tthe\tthe\te\the\tthe\tthe\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "author\tauthor\ta\tau\taut\tauth\tr\tor\thor\tthor\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "Grobid\tgrobid\tG\tGr\tGro\tGrob\td\tid\tbid\tobid\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "[\t[\t[\t[\t[\t[\t[\t[\t[\t[\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tOPENBRACKET\t0\t<other>\n" +
            "22\t22\t2\t22\t22\t22\t2\t22\t22\t22\tLINEIN\tNOCAPS\tALLDIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "]\t]\t]\t]\t]\t]\t]\t]\t]\t]\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tENDBRACKET\t0\t<other>\n" +
            ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tCOMMA\t0\t<other>\n" +
            "DeLFT\tdelft\tD\tDe\tDeL\tDeLF\tT\tFT\tLFT\teLFT\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "[\t[\t[\t[\t[\t[\t[\t[\t[\t[\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tOPENBRACKET\t0\t<other>\n" +
            "20\t20\t2\t20\t20\t20\t0\t20\t20\t20\tLINEIN\tNOCAPS\tALLDIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "]\t]\t]\t]\t]\t]\t]\t]\t]\t]\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tENDBRACKET\t0\t<other>\n" +
            ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tCOMMA\t0\t<other>\n" +
            "and\tand\ta\tan\tand\tand\td\tnd\tand\tand\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "other\tother\to\tot\toth\tothe\tr\ter\ther\tther\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "open\topen\to\top\tope\topen\tn\ten\tpen\topen\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "-\t-\t-\t-\t-\t-\t-\t-\t-\t-\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tHYPHEN\t0\t<other>\n" +
            "source\tsource\ts\tso\tsou\tsour\te\tce\trce\turce\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "projects\tprojects\tp\tpr\tpro\tproj\ts\tts\tcts\tects\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "for\tfor\tf\tfo\tfor\tfor\tr\tor\tfor\tfor\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "his\this\th\thi\this\this\ts\tis\this\this\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "continuous\tcontinuous\tc\tco\tcon\tcont\ts\tus\tous\tuous\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "support\tsupport\ts\tsu\tsup\tsupp\tt\trt\tort\tport\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "and\tand\ta\tan\tand\tand\td\tnd\tand\tand\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "inspiration\tinspiration\ti\tin\tins\tinsp\tn\ton\tion\ttion\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "with\twith\tw\twi\twit\twith\th\tth\tith\twith\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "ideas\tideas\ti\tid\tide\tidea\ts\tas\teas\tdeas\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tCOMMA\t0\t<other>\n" +
            "suggestions\tsuggestions\ts\tsu\tsug\tsugg\ts\tns\tons\tions\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tCOMMA\t0\t<other>\n" +
            "and\tand\ta\tan\tand\tand\td\tnd\tand\tand\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "fruitful\tfruitful\tf\tfr\tfru\tfrui\tl\tul\tful\ttful\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "discussions\tdiscussions\td\tdi\tdis\tdisc\ts\tns\tons\tions\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tDOT\t0\t<other>\n" +
            "We\twe\tW\tWe\tWe\tWe\te\tWe\tWe\tWe\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "thank\tthank\tt\tth\ttha\tthan\tk\tnk\tank\thank\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "Pedro\tpedro\tP\tPe\tPed\tPedr\to\tro\tdro\tedro\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<person>\n" +
            "Baptista\tbaptista\tB\tBa\tBap\tBapt\ta\tta\tsta\tista\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<person>\n" +
            "de\tde\td\tde\tde\tde\te\tde\tde\tde\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<person>\n" +
            "Castro\tcastro\tC\tCa\tCas\tCast\to\tro\ttro\tstro\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<person>\n" +
            "for\tfor\tf\tfo\tfor\tfor\tr\tor\tfor\tfor\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<other>\n" +
            "his\this\th\thi\this\this\ts\tis\this\this\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "support\tsupport\ts\tsu\tsup\tsupp\tt\trt\tort\tport\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "during\tduring\td\tdu\tdur\tduri\tg\tng\ting\tring\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "this\tthis\tt\tth\tthi\tthis\ts\tis\this\tthis\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "work\twork\tw\two\twor\twork\tk\trk\tork\twork\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tDOT\t0\t<other>\n" +
            "Special\tspecial\tS\tSp\tSpe\tSpec\tl\tal\tial\tcial\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "thanks\tthanks\tt\tth\ttha\tthan\ts\tks\tnks\tanks\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "to\tto\tt\tto\tto\tto\to\tto\tto\tto\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "Erina\terina\tE\tEr\tEri\tErin\ta\tna\tina\trina\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<person>\n" +
            "Fujita\tfujita\tF\tFu\tFuj\tFuji\ta\tta\tita\tjita\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<person>\n" +
            "for\tfor\tf\tfo\tfor\tfor\tr\tor\tfor\tfor\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<other>\n" +
            "useful\tuseful\tu\tus\tuse\tusef\tl\tul\tful\teful\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "tips\ttips\tt\tti\ttip\ttips\ts\tps\tips\ttips\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "on\ton\to\ton\ton\ton\tn\ton\ton\ton\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "the\tthe\tt\tth\tthe\tthe\te\the\tthe\tthe\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "manuscript\tmanuscript\tm\tma\tman\tmanu\tt\tpt\tipt\tript\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEEND\tALLCAP\tNODIGIT\t1\t0\t0\tDOT\t0\t<other>"

        val tokens: List<LayoutToken> = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input)

        val (spans, statement) = target.getExtractionResult(tokens, results)

        assertThat(statement.fundings, hasSize(0))
        assertThat(statement.persons, hasSize(3))
        assertThat(statement.persons[0].rawName, `is`("Patrice Lopez"))
        assertThat(statement.persons[1].rawName, `is`("Pedro Baptista de Castro"))
        assertThat(statement.persons[2].rawName, `is`("Erina Fujita"))
        assertThat(statement.affiliations, hasSize(0))

        assertThat(spans, hasSize(3))
        val span0 = spans[0]
        val offsetPosition0 = span0.offsetPosition
        val element0 = span0.annotationNode

        assertThat(LayoutTokensUtil.toText(tokens.subList(offsetPosition0.start, offsetPosition0.end)), `is`("Patrice Lopez"))
        assertThat(element0.toXML(), `is`("<rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Patrice Lopez</rs>"))

        val span1 = spans[1]
        val offsetPosition1 = span1.offsetPosition
        val element1 = span1.annotationNode

        assertThat(LayoutTokensUtil.toText(tokens.subList(offsetPosition1.start, offsetPosition1.end)), `is`("Pedro Baptista de Castro"))
        assertThat(element1.toXML(), `is`("<rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Pedro Baptista de Castro</rs>"))

        val span2 = spans[2]
        val offsetPosition2 = span2.offsetPosition
        val element2 = span2.annotationNode

        assertThat(LayoutTokensUtil.toText(tokens.subList(offsetPosition2.start, offsetPosition2.end)), `is`("Erina Fujita"))
        assertThat(element2.toXML(), `is`("<rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Erina Fujita</rs>"))
    }

    @Test
    fun testGetExtractionResultNew2_ShouldReturnCorrectElementsAndPositions() {
        val input = "This work was partly supported by MEXT Program: Data Creation and Utilization-Type Material Research and Development Project (Digital Transformation Initiative Center for Magnetic Materials) Grant Number [JPMXP1122715503]."

        val results: String = "This\tthis\tT\tTh\tThi\tThis\ts\tis\this\tThis\tLINESTART\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<other>\n" +
            "work\twork\tw\two\twor\twork\tk\trk\tork\twork\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "was\twas\tw\twa\twas\twas\ts\tas\twas\twas\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "partly\tpartly\tp\tpa\tpar\tpart\ty\tly\ttly\trtly\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "supported\tsupported\ts\tsu\tsup\tsupp\td\ted\tted\trted\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "by\tby\tb\tby\tby\tby\ty\tby\tby\tby\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "MEXT\tmext\tM\tME\tMEX\tMEXT\tT\tXT\tEXT\tMEXT\tLINEIN\tALLCAP\tNODIGIT\t0\t1\t0\tNOPUNCT\t0\tI-<funderName>\n" +
            "Program\tprogram\tP\tPr\tPro\tProg\tm\tam\tram\tgram\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<other>\n" +
            ":\t:\t:\t:\t:\t:\t:\t:\t:\t:\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tPUNCT\t0\t<other>\n" +
            "Data\tdata\tD\tDa\tDat\tData\ta\tta\tata\tData\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<programName>\n" +
            "Creation\tcreation\tC\tCr\tCre\tCrea\tn\ton\tion\ttion\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "and\tand\ta\tan\tand\tand\td\tnd\tand\tand\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "Utilization\tutilization\tU\tUt\tUti\tUtil\tn\ton\tion\ttion\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "-\t-\t-\t-\t-\t-\t-\t-\t-\t-\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tHYPHEN\t0\t<programName>\n" +
            "Type\ttype\tT\tTy\tTyp\tType\te\tpe\type\tType\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "Material\tmaterial\tM\tMa\tMat\tMate\tl\tal\tial\trial\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "Research\tresearch\tR\tRe\tRes\tRese\th\tch\trch\tarch\tLINEIN\tINITCAP\tNODIGIT\t0\t1\t0\tNOPUNCT\t0\t<programName>\n" +
            "and\tand\ta\tan\tand\tand\td\tnd\tand\tand\tLINEIN\tNOCAPS\tNODIGIT\t0\t1\t0\tNOPUNCT\t0\t<programName>\n" +
            "Development\tdevelopment\tD\tDe\tDev\tDeve\tt\tnt\tent\tment\tLINEIN\tINITCAP\tNODIGIT\t0\t1\t0\tNOPUNCT\t0\t<programName>\n" +
            "Project\tproject\tP\tPr\tPro\tProj\tt\tct\tect\tject\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "(\t(\t(\t(\t(\t(\t(\t(\t(\t(\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tOPENBRACKET\t0\t<programName>\n" +
            "Digital\tdigital\tD\tDi\tDig\tDigi\tl\tal\ttal\tital\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "Transformation\ttransformation\tT\tTr\tTra\tTran\tn\ton\tion\ttion\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "Initiative\tinitiative\tI\tIn\tIni\tInit\te\tve\tive\ttive\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "Center\tcenter\tC\tCe\tCen\tCent\tr\ter\tter\tnter\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "for\tfor\tf\tfo\tfor\tfor\tr\tor\tfor\tfor\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "Magnetic\tmagnetic\tM\tMa\tMag\tMagn\tc\tic\ttic\tetic\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            "Materials\tmaterials\tM\tMa\tMat\tMate\ts\tls\tals\tials\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<programName>\n" +
            ")\t)\t)\t)\t)\t)\t)\t)\t)\t)\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tENDBRACKET\t0\t<programName>\n" +
            "Grant\tgrant\tG\tGr\tGra\tGran\tt\tnt\tant\trant\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<other>\n" +
            "Number\tnumber\tN\tNu\tNum\tNumb\tr\ter\tber\tmber\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<other>\n" +
            "[\t[\t[\t[\t[\t[\t[\t[\t[\t[\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tOPENBRACKET\t0\t<other>\n" +
            "JPMXP1122715503\tjpmxp1122715503\tJ\tJP\tJPM\tJPMX\t3\t03\t503\t5503\tLINEIN\tALLCAP\tCONTAINSDIGITS\t0\t0\t0\tNOPUNCT\t0\tI-<grantNumber>\n" +
            "]\t]\t]\t]\t]\t]\t]\t]\t]\t]\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tENDBRACKET\t0\tI-<other>\n" +
            ".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEEND\tALLCAP\tNODIGIT\t1\t0\t0\tDOT\t0\t<other>"

        val tokens: List<LayoutToken> = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input)

        val (spans, statement) = target.getExtractionResult(tokens, results)

        assertThat(statement.fundings, hasSize(1))
        assertThat(statement.persons, hasSize(0))
        assertThat(statement.affiliations, hasSize(0))

        assertThat(spans, hasSize(3))
        val span0 = spans[0]
        val offsetPosition0 = span0.offsetPosition
        val element0 = span0.annotationNode

        assertThat(LayoutTokensUtil.toText(tokens.subList(offsetPosition0.start, offsetPosition0.end)), `is`("MEXT"))
        assertThat(element0.toXML(), `is`("<rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"funder\">MEXT</rs>"))

        val span1 = spans[1]
        val offsetPosition1 = span1.offsetPosition
        val element1 = span1.annotationNode

        assertThat(LayoutTokensUtil.toText(tokens.subList(offsetPosition1.start, offsetPosition1.end)), `is`("Data Creation and Utilization-Type Material Research and Development Project (Digital Transformation Initiative Center for Magnetic Materials)"))
        assertThat(element1.toXML(), `is`("<rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"programName\">Data Creation and Utilization-Type Material Research and Development Project (Digital Transformation Initiative Center for Magnetic Materials)</rs>"))

        val span2 = spans[2]
        val offsetPosition2 = span2.offsetPosition
        val element2 = span2.annotationNode

        assertThat(LayoutTokensUtil.toText(tokens.subList(offsetPosition2.start, offsetPosition2.end)), `is`("JPMXP1122715503"))
        assertThat(element2.toXML(), `is`("<rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"grantNumber\">JPMXP1122715503</rs>"))
    }

    @Test
    fun testGetExtractionResult_ErrorCase_ShouldReturnCorrectElementsAndPositions() {
        val input = "Christophe Castagne, Claudie Marec, Claudie Marec, Claudio Stalder,"

        val results: String = "Christophe\tchristophe\tC\tCh\tChr\tChri\te\the\tphe\tophe\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<person>\n" +
            "Castagne\tcastagne\tC\tCa\tCas\tCast\te\tne\tgne\tagne\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<person>\n" +
            ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tCOMMA\t0\tI-<other>\n" +
            "Claudie\tclaudie\tC\tCl\tCla\tClau\te\tie\tdie\tudie\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<person>\n" +
            "Marec\tmarec\tM\tMa\tMar\tMare\tc\tec\trec\tarec\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<person>\n" +
            ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tCOMMA\t0\tI-<other>\n" +
            "Claudie\tclaudie\tC\tCl\tCla\tClau\te\tie\tdie\tudie\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<person>\n" +
            "Marec\tmarec\tM\tMa\tMar\tMare\tc\tec\trec\tarec\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<person>\n" +
            ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tCOMMA\t0\tI-<other>\n" +
            "Claudio\tclaudio\tC\tCl\tCla\tClau\to\tio\tdio\tudio\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\tI-<person>\n" +
            "Stalder\tstalder\tS\tSt\tSta\tStal\tr\ter\tder\tlder\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\tNOPUNCT\t0\t<person>\n" +
            ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAP\tNODIGIT\t1\t0\t0\tCOMMA\t0\tI-<other>\n"

        val tokens: List<LayoutToken> = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input)

        val (spans, statement) = target.getExtractionResult(tokens, results)

        assertThat(statement.fundings, hasSize(0))
        assertThat(statement.persons, hasSize(4))
        assertThat(statement.affiliations, hasSize(0))

        assertThat(spans, hasSize(4))
        val span0 = spans[0]
        val offsetPosition0 = span0.offsetPosition
        val element0 = span0.annotationNode

        assertThat(LayoutTokensUtil.toText(tokens.subList(offsetPosition0.start, offsetPosition0.end)), `is`("Christophe Castagne"))
        assertThat(element0.toXML(), `is`("<rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Christophe Castagne</rs>"))

        val span1 = spans[1]
        val offsetPosition1 = span1.offsetPosition
        val element1 = span1.annotationNode

        assertThat(LayoutTokensUtil.toText(tokens.subList(offsetPosition1.start, offsetPosition1.end)), `is`("Claudie Marec"))
        assertThat(element1.toXML(), `is`("<rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Claudie Marec</rs>"))

        val span2 = spans[2]
        val offsetPosition2 = span2.offsetPosition
        val element2 = span2.annotationNode

        assertThat(LayoutTokensUtil.toText(tokens.subList(offsetPosition2.start, offsetPosition2.end)), `is`("Claudie Marec"))
        assertThat(element2.toXML(), `is`("<rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Claudie Marec</rs>"))

        // The name is the same, but the offset should be different
        assertThat(offsetPosition2.start, `is`(not(offsetPosition1.start)))
        assertThat(offsetPosition2.end, `is`(not(offsetPosition1.end)))

        val span3 = spans[3]
        val offsetPosition3 = span3.offsetPosition
        val element3 = span3.annotationNode

        assertThat(LayoutTokensUtil.toText(tokens.subList(offsetPosition3.start, offsetPosition3.end)), `is`("Claudio Stalder"))
        assertThat(element3.toXML(), `is`("<rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Claudio Stalder</rs>"))
    }
}
