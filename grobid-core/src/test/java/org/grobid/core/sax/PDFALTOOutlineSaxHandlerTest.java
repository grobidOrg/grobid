package org.grobid.core.sax;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Before;
import org.junit.Test;

import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentNode;
import org.grobid.core.document.DocumentSource;

public class PDFALTOOutlineSaxHandlerTest {
    SAXParserFactory spf = SAXParserFactory.newInstance();

    PDFALTOOutlineSaxHandler target;
    DocumentSource mockDocumentSource;
    Document document;

    @Before
    public void setUp() throws Exception {

        mockDocumentSource = createMock(DocumentSource.class);

        document = Document.createFromText("");
        target = new PDFALTOOutlineSaxHandler(document);
    }

    @Test
    public void testParsing_pdf2XMLOutline_ShouldWork() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("pdfalto.xml_outline.xml");

        SAXParser p = spf.newSAXParser();
        p.parse(is, target);

        DocumentNode root = target.getRootNode();
        assertTrue(root.getChildren().size() > 0);
        assertThat(root.getChildren(), hasSize(9));
        assertThat(root.getChildren().get(0).getLabel(), is("Abstract"));
        assertThat(root.getChildren().get(0).getChildren(), is(nullValue()));
        assertThat(root.getChildren().get(0).getBoundingBox().getPage(), is(1));
        //<LINK page="1" top="592.00" bottom="0.00" left="0.00" right="0.00"/>
        //        assertThat(root.getChildren().get(0).getBoundingBox().getY(), is(0.0));
        //        assertThat(root.getChildren().get(0).getBoundingBox().getHeight(), is(-1.0));
        //        assertThat(root.getChildren().get(0).getBoundingBox().getX(), is(0.0));
        //        assertThat(root.getChildren().get(0).getBoundingBox().getWidth(), is(0.0));
    }

    @Test
    public void testParsing_pdf2XMLOutline_errorcase_ShouldWork() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("test_outline.xml");

        SAXParser p = spf.newSAXParser();
        p.parse(is, target);

        DocumentNode root = target.getRootNode();
        assertThat(root.getChildren(), hasSize(5));

        assertThat(root.getChildren().get(0).getLabel(), is("A Identification"));
        assertThat(root.getChildren().get(0).getChildren(), is(nullValue()));
        //<LINK page="2" top="71.0000" bottom="0.0000" left="68.0000" right="0.0000"/>
        assertThat(root.getChildren().get(0).getBoundingBox().getPage(), is(2));
        //        assertThat(root.getChildren().get(0).getBoundingBox().getY(), is(71.000));
        //        assertThat(root.getChildren().get(0).getBoundingBox().getHeight(), is(0.0));
        //        assertThat(root.getChildren().get(0).getBoundingBox().getX(), is(68.000));
        //        assertThat(root.getChildren().get(0).getBoundingBox().getWidth(), is(0.0));

        assertThat(root.getChildren().get(1).getLabel(), is("B Résumé consolidé public."));
        assertThat(root.getChildren().get(1).getChildren(), hasSize(1));
        //<LINK page="2" top="377.000" bottom="0.0000" left="68.0000" right="0.0000"/>
        assertThat(root.getChildren().get(1).getBoundingBox().getPage(), is(2));
        //        assertThat(root.getChildren().get(1).getBoundingBox().getY(), is(377.000));
        //        assertThat(root.getChildren().get(1).getBoundingBox().getHeight(), is(0.0));
        //        assertThat(root.getChildren().get(1).getBoundingBox().getX(), is(68.000));
        //        assertThat(root.getChildren().get(1).getBoundingBox().getWidth(), is(0.0));

        assertThat(root.getChildren().get(1).getChildren(), hasSize(1));
        assertThat(
                root.getChildren().get(1).getChildren().get(0).getLabel(),
                is("B.1 Résumé consolidé public en français"));
        //<LINK page="2" top="412.000" bottom="0.0000" left="68.0000" right="0.0000"/>
        assertThat(root.getChildren().get(1).getChildren().get(0).getBoundingBox().getPage(), is(2));
        //        assertThat(root.getChildren().get(1).getChildren().get(0).getBoundingBox().getY(), is(412.000));
        //        assertThat(root.getChildren().get(1).getChildren().get(0).getBoundingBox().getHeight(), is(0.0));
        //        assertThat(root.getChildren().get(1).getChildren().get(0).getBoundingBox().getX(), is(68.000));
        //        assertThat(root.getChildren().get(1).getChildren().get(0).getBoundingBox().getWidth(), is(0.0));

        assertThat(root.getChildren().get(2).getLabel(), is("C Mémoire scientifique en français"));
        assertThat(root.getChildren().get(2).getChildren(), hasSize(6));
        assertThat(
                root.getChildren().get(2).getChildren().get(2).getLabel(),
                is("C.3 Approche scientifique et technique"));
        assertThat(root.getChildren().get(3).getLabel(), is("D Liste des livrables"));
        assertThat(root.getChildren().get(3).getChildren(), is(nullValue()));
        assertThat(root.getChildren().get(4).getLabel(), is("E Impact du projet"));
        assertThat(root.getChildren().get(4).getChildren(), hasSize(4));
        assertThat(
                root.getChildren().get(4).getChildren().get(1).getLabel(),
                is("E.2 Liste des publications et communications"));
        assertThat(
                root.getChildren().get(4).getChildren().get(2).getLabel(),
                is("E.3 Liste des autres valorisations scientifiques"));
        //<LINK page="1" top="170.000" bottom="0.0000" left="68.0000" right="0.0000"/>
        assertThat(root.getChildren().get(4).getChildren().get(2).getBoundingBox().getPage(), is(1));
        //        assertThat(root.getChildren().get(4).getChildren().get(2).getBoundingBox().getY(), is(170.000));
        //        assertThat(root.getChildren().get(4).getChildren().get(2).getBoundingBox().getHeight(), is(0.0));
        //        assertThat(root.getChildren().get(4).getChildren().get(2).getBoundingBox().getX(), is(68.000));
        //        assertThat(root.getChildren().get(4).getChildren().get(2).getBoundingBox().getWidth(), is(0.0));
    }

}
