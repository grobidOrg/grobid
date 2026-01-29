package org.grobid.core.document

import org.grobid.core.analyzers.GrobidAnalyzer
import org.grobid.core.data.Figure
import org.grobid.core.layout.Block
import org.grobid.core.layout.BoundingBox
import org.grobid.core.utilities.GrobidProperties
import org.grobid.core.utilities.LayoutTokensUtil
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import javax.xml.parsers.SAXParserFactory

/**
 * See https://github.com/kermitt2/grobid/pull/475
 *
 */
class DocumentTest {
    @Before
    @Throws(Exception::class)
    fun setUp() {
        GrobidProperties.getInstance()
    }

    @Test
    @Throws(Exception::class)
    fun shouldNotFailToParseValidXml() {
        Document.parseInputStream(
            ByteArrayInputStream(validXmlBytes),
            SAXParserFactory.newInstance(),
            DefaultHandler(),
        )
    }

    @Test
    @Throws(Exception::class)
    fun shouldNotFailToParseInvalidUtf8ByteSequenceXmlByDefault() {
        Document.parseInputStream(
            ByteArrayInputStream(xmlBytesWithInvalidUtf8Sequence),
            SAXParserFactory.newInstance(),
            DefaultHandler(),
        )
    }

    @Test
    @Throws(Exception::class)
    fun testGetFigureLayoutTokens_paragraphFarFromCaption_shouldRemoveParagraph() {
        val text = "This is some garbage that comes before the figure..\n" +
            "d\n" +
            "d\n" +
            "d\n" +
            "sss\n" +
            "Figure 1: This is a caption.\n" +
            "d\n" +
            "and a paragraph we want to keep or revert back into the fulltext.\n"

        val tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text)
        val figure = Figure()

        val block1 = Block()
        block1.tokens = tokens.subList(0, 20)
        block1.boundingBox = BoundingBox.fromPointAndDimensions(1, 10.0, 10.0, 10.0, 10.0)
        val block2 = Block()
        block2.tokens = tokens.subList(20, 25)
        block2.boundingBox = BoundingBox.fromPointAndDimensions(1, 10.0, 20.0, 10.0, 10.0)
        val block3 = Block()
        block3.tokens = tokens.subList(25, 28)
        block3.boundingBox = BoundingBox.fromPointAndDimensions(1, 10.0, 30.0, 10.0, 10.0)
        val block4 = Block()
        block4.tokens = tokens.subList(28, 41)
        block4.boundingBox = BoundingBox.fromPointAndDimensions(1, 10.0, 50.0, 10.0, 10.0)
        val block5 = Block()
        block5.tokens = tokens.subList(41, 71)
        block5.boundingBox = BoundingBox.fromPointAndDimensions(1, 10.0, 80.0, 10.0, 10.0)

        val doc = Document()
        doc.blocks = Arrays.asList(block1, block2, block3, block4, block5)

        figure.blockPtrs = TreeSet(Arrays.asList(0, 1, 2, 3, 4))

        val captionString = "This is a caption."
        val captionLayoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(captionString)
        figure.setCaption(StringBuilder(captionString))
        figure.layoutTokens = tokens
        figure.captionLayoutTokens = captionLayoutTokens

        val output = doc.getFigureLayoutTokens(figure)

        assertThat(LayoutTokensUtil.toText(output.left), `is`("Figure 1: This is a caption."))
        assertThat(output.right, hasSize(1))
        assertThat(
            LayoutTokensUtil.toText(output.right[0]),
            `is`(LayoutTokensUtil.toText(block5.tokens)),
        )
    }

    @Test
    @Throws(Exception::class)
    fun testGetFigureLayoutTokens_paragraphFarFromCaptionWithBlockNotMatching_shouldRemoveParagraph() {
        val text = "This is some garbage that comes before the figure..\n" +
            "d\n" +
            "d\n" +
            "d\n" +
            "sss\n" +
            "Figure 1: This is a caption.\n" +
            "d\n" +
            "and a paragraph we want to keep or revert back into the fulltext.\n"

        val tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text)
        val figure = Figure()

        val block1 = Block()
        block1.tokens = tokens.subList(0, 20)
        block1.boundingBox = BoundingBox.fromPointAndDimensions(1, 10.0, 10.0, 10.0, 10.0)
        val block2 = Block()
        block2.tokens = tokens.subList(20, 25)
        block2.boundingBox = BoundingBox.fromPointAndDimensions(1, 10.0, 20.0, 10.0, 10.0)
        val block3 = Block()
        block3.tokens = tokens.subList(25, 28)
        block3.boundingBox = BoundingBox.fromPointAndDimensions(1, 10.0, 30.0, 10.0, 10.0)
        val block4 = Block()
        block4.tokens = tokens.subList(28, 41)
        block4.boundingBox = BoundingBox.fromPointAndDimensions(1, 10.0, 50.0, 10.0, 10.0)
        val block5 = Block()
        val block5OriginalTokens = tokens.subList(41, 71)
        block5.tokens = block5OriginalTokens + GrobidAnalyzer.getInstance().tokenizeWithLayoutToken("Some additional text.")
        block5.boundingBox = BoundingBox.fromPointAndDimensions(1, 10.0, 80.0, 10.0, 10.0)

        val doc = Document()
        doc.blocks = Arrays.asList(
            block1,
            block2,
            block3,
            block4,
            block5,
        )

        figure.blockPtrs = TreeSet(listOf(0, 1, 2, 3, 4))

        val captionString = "This is a caption."
        val captionLayoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(captionString)
        figure.setCaption(StringBuilder(captionString))
        figure.layoutTokens = tokens
        figure.captionLayoutTokens = captionLayoutTokens

        val output = doc.getFigureLayoutTokens(figure)

        assertThat(LayoutTokensUtil.toText(output.left), `is`("Figure 1: This is a caption."))
        assertThat(
            LayoutTokensUtil.toText(output.right[0]),
            `is`(LayoutTokensUtil.toText(block5OriginalTokens)),
        )
    }

    @Test
    @Throws(Exception::class)
    fun testGetFigureLayoutTokens_paragraphClosedFromCaption_shouldNotRemoveParagraph() {
        val text = "This is some garbage that comes before the figure..\n" +
            "d\n" +
            "d\n" +
            "d\n" +
            "sss\n" +
            "Figure 1: This is a caption.\n" +
            "d\n" +
            "and a paragraph we want to keep or revert back into the fulltext.\n"

        val tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text)
        val figure = Figure()

        val block1 = Block()
        block1.tokens = tokens.subList(0, 20)
        block1.boundingBox = BoundingBox.fromPointAndDimensions(1, 10.0, 10.0, 10.0, 10.0)
        val block2 = Block()
        block2.tokens = tokens.subList(20, 25)
        block2.boundingBox = BoundingBox.fromPointAndDimensions(1, 10.0, 20.0, 10.0, 10.0)
        val block3 = Block()
        block3.tokens = tokens.subList(25, 28)
        block3.boundingBox = BoundingBox.fromPointAndDimensions(1, 10.0, 30.0, 10.0, 10.0)
        val block4 = Block()
        block4.tokens = tokens.subList(28, 41)
        block4.boundingBox = BoundingBox.fromPointAndDimensions(1, 10.0, 40.0, 10.0, 10.0)
        val block5 = Block()
        block5.tokens = tokens.subList(41, 71)
        block5.boundingBox = BoundingBox.fromPointAndDimensions(1, 10.0, 50.0, 10.0, 10.0)

        val doc = Document()
        doc.blocks = Arrays.asList(block1, block2, block3, block4, block5)

        figure.blockPtrs = TreeSet(Arrays.asList(0, 1, 2, 3, 4))

        val captionString = "This is a caption."
        val captionLayoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(captionString)
        figure.setCaption(StringBuilder(captionString))
        figure.layoutTokens = tokens
        figure.captionLayoutTokens = captionLayoutTokens

        val output = doc.getFigureLayoutTokens(figure)

        assertThat(
            LayoutTokensUtil.toText(output.left),
            `is`("Figure 1: This is a caption.\nd\nand a paragraph we want to keep or revert back into the fulltext.\n"),
        )
    }

    companion object {
        @JvmStatic
        @BeforeClass
        @Throws(Exception::class)
        fun setInitialContext() {
            GrobidProperties.getInstance()
        }

        private val validXmlBytes: ByteArray
            get() = "<xml>test</xml>".toByteArray()

        @get:Throws(IOException::class)
        private val xmlBytesWithInvalidUtf8Sequence: ByteArray
            get() {
                val out = ByteArrayOutputStream()
                out.write("<xml>".toByteArray())
                out.write(0xe0)
                out.write(0xd8)
                out.write(0x35)
                out.write("</xml>".toByteArray())
                return out.toByteArray()
            }
    }
}
