package org.grobid.trainer.sax;

import org.junit.Before;
import org.junit.Test;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class TEIHeaderArticleLightSaxParserTest {

    TEIHeaderArticleLightSaxParser target;
    SAXParserFactory spf;

    @Before
    public void setUp() throws Exception {
        spf = SAXParserFactory.newInstance();
        target = new TEIHeaderArticleLightSaxParser();
    }

    @Test
    public void testHeader() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/10.1371_journal.pone.0210387.training.header.tei.xml");

        final SAXParser p = spf.newSAXParser();
        p.parse(is, target);

        assertThat(target.getLabeledResult(), hasSize(greaterThan(0)));
        assertThat(target.getLabeledResult(), hasSize(681));
        assertThat(target.getLabeledResult().get(0), endsWith(" I-<other>\n"));
        assertThat(target.getLabeledResult().get(1), endsWith(" <other>\n"));
        assertThat(target.getLabeledResult().get(2), endsWith(" I-<title>\n"));
        assertThat(target.getLabeledResult().get(3), endsWith(" <title>\n"));
        assertThat(target.getLabeledResult().get(13), endsWith(" I-<author>\n"));
        assertThat(target.getLabeledResult().get(15), endsWith(" <author>\n"));
    }

    @Test
    public void testHeader2() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/EJM_CavitationModelsComparison.training.header.tei.xml");

        final SAXParser p = spf.newSAXParser();
        p.parse(is, target);

        assertThat(target.getLabeledResult(), hasSize(greaterThan(0)));
        assertThat(target.getLabeledResult(), hasSize(197));
        assertThat(target.getLabeledResult().get(0), endsWith(" I-<title>\n"));
        assertThat(target.getLabeledResult().get(1), endsWith(" <title>\n"));
        assertThat(target.getLabeledResult().get(2), endsWith(" <title>\n"));
        assertThat(target.getLabeledResult().get(10), endsWith(" I-<author>\n"));
        assertThat(target.getLabeledResult().get(11), endsWith(" <author>\n"));
    }

}
