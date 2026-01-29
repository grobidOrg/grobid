package org.grobid.trainer.sax;

import org.junit.Before;
import org.junit.Test;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class TEISegmentationArticleLightSaxParserTest {

    TEISegmentationArticleLightSaxParser target;
    SAXParserFactory spf;

    @Before
    public void setUp() throws Exception {
        spf = SAXParserFactory.newInstance();
        target = new TEISegmentationArticleLightSaxParser();
    }

    @Test
    public void testSegmentation() throws Exception {
        InputStream is = this.getClass()
                .getResourceAsStream("/10.1371_journal.pone.0210128.training.segmentation.tei.xml");

        final SAXParser p = spf.newSAXParser();
        p.parse(is, target);

        assertThat(target.getLabeledResult(), hasSize(greaterThan(0)));
        assertThat(target.getLabeledResult().get(0), is("RESEARCH I-<header>\n"));
        assertThat(target.getLabeledResult().get(35), is("PLOS I-<header>\n"));
        assertThat(target.getLabeledResult().get(65), is("PLOS I-<body>\n"));

    }

}
