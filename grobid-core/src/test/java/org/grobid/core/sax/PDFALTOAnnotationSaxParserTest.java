package org.grobid.core.sax;

import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.layout.PDFAnnotation;
import org.junit.Before;
import org.junit.Test;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PDFALTOAnnotationSaxParserTest {
    SAXParserFactory spf = SAXParserFactory.newInstance();

    PDFALTOAnnotationSaxHandler target;
    DocumentSource mockDocumentSource;
    Document document;

    @Before
    public void setUp() throws Exception {

        mockDocumentSource = createMock(DocumentSource.class);

        document = Document.createFromText("");
        target = new PDFALTOAnnotationSaxHandler(document, new ArrayList<PDFAnnotation>());
    }

    @Test
    public void testParsing_pdf2XMLAnnotations_ShouldWork() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("pdfalto.xml_annot.xml");

        SAXParser p = spf.newSAXParser();
        p.parse(is, target);

        List<PDFAnnotation> pdfAnnotations = target.getPDFAnnotations();
        // System.out.println(pdfAnnotations.size());
        assertTrue(pdfAnnotations.size() > 0);
        assertThat(pdfAnnotations, hasSize(520));

    }

}
