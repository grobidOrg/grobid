package org.grobid.core.sax;

import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.layout.GraphicObject;
import org.grobid.core.layout.LayoutToken;
import org.junit.Before;
import org.junit.Test;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PDFALTOSaxHandlerTest {
    SAXParserFactory spf = SAXParserFactory.newInstance();

    PDFALTOSaxHandler target;
    DocumentSource mockDocumentSource;
    Document document;
    private List<GraphicObject> images;

    @Before
    public void setUp() throws Exception {

        mockDocumentSource = createMock(DocumentSource.class);

        document = Document.createFromText("");
        images = new ArrayList<>();
        target = new PDFALTOSaxHandler(document, images);
    }

    @Test
    public void testParsing_pdf2XMLwithNoIMages_ShouldWork() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("pdfalto_noImages.xml");

        SAXParser p = spf.newSAXParser();
        p.parse(is, target);

        List<LayoutToken> tokenList = target.getTokenization();

        assertTrue(tokenList.size() > 0);
        assertTrue(document.getImages().size() == 0);
        assertTrue(images.size() == 0);
        assertTrue(document.getPages().size() == 4);
        assertTrue(document.getBlocks().size() == 26);
    }

    //@Test
    public void testParsing_pdf2XMLwithIMages_ShouldWork() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("pdfalto_Images.xml");

        SAXParser p = spf.newSAXParser();
        p.parse(inputStream, target);

        List<LayoutToken> tokenList = target.getTokenization();

        assertTrue(tokenList.size() > 0);
        assertThat(images.size(), is(16));
        assertThat(document.getImages().size(), is(16));
        assertTrue(document.getPages().size() == 4);
        assertThat(document.getBlocks().size(), is(26));
    }

    @Test
    public void testParsing_shouldWork() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("JPS081033701-CC.xml");

        SAXParser p = spf.newSAXParser();
        p.parse(inputStream, target);

        List<LayoutToken> tokenList = target.getTokenization();

        assertThat(tokenList.stream().filter(t -> t.getText().equals("newly")).count(), is(1L));

        assertThat(tokenList.get(0).getText(), is("Microscopic"));
        assertThat(tokenList.get(0).isBold(), is(true));
        assertThat(tokenList.get(25).getText(), is("BaFe"));
        assertThat(tokenList.get(25).isSubscript(), is(false));
        assertThat(tokenList.get(27).getText(), is("2"));
        assertThat(tokenList.get(27).isSubscript(), is(true));
    }

    @Test
    public void testParsing_BoldItalic_shouldWork() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("s3xKQzHmBR.xml");

        SAXParser p = spf.newSAXParser();
        p.parse(inputStream, target);

        List<LayoutToken> tokenList = target.getTokenization();

        assertThat(tokenList.stream().filter(LayoutToken::isSuperscript).count(), is(4L));
        assertThat(tokenList.stream().filter(LayoutToken::isSubscript).count(), is(3L));

        assertThat(tokenList, hasSize(greaterThan(0)));

        assertThat(tokenList.get(0).getText(), is("We"));
        assertThat(tokenList.get(0).isSubscript(), is(false));
        assertThat(tokenList.get(0).isSuperscript(), is(false));
        assertThat(tokenList.get(0).isBold(), is(false));
        assertThat(tokenList.get(0).isItalic(), is(false));

        assertThat(tokenList.get(14).getText(), is("CO"));
        assertThat(tokenList.get(14).isSubscript(), is(false));
        assertThat(tokenList.get(14).isSuperscript(), is(false));
        assertThat(tokenList.get(14).isBold(), is(false));
        assertThat(tokenList.get(14).isItalic(), is(false));

        assertThat(tokenList.get(16).getText(), is("2"));
        assertThat(tokenList.get(16).isSubscript(), is(true));
        assertThat(tokenList.get(16).isSuperscript(), is(false));
        assertThat(tokenList.get(16).isBold(), is(false));
        assertThat(tokenList.get(16).isItalic(), is(false));

        assertThat(tokenList.get(35).getText(), is("Ur"));
        assertThat(tokenList.get(35).isSubscript(), is(false));
        assertThat(tokenList.get(35).isSuperscript(), is(false));
        assertThat(tokenList.get(35).isBold(), is(true));
        assertThat(tokenList.get(35).isItalic(), is(true));

        assertThat(tokenList.get(37).getText(), is("123"));
        assertThat(tokenList.get(37).isSubscript(), is(true));
        assertThat(tokenList.get(37).isSuperscript(), is(false));
        assertThat(tokenList.get(37).isBold(), is(true));
        assertThat(tokenList.get(37).isItalic(), is(true));

        assertThat(tokenList.get(39).getText(), is("6a"));
        assertThat(tokenList.get(39).isSubscript(), is(false));
        assertThat(tokenList.get(39).isSuperscript(), is(true));
        assertThat(tokenList.get(39).isBold(), is(false));
        assertThat(tokenList.get(39).isItalic(), is(true));
    }

}