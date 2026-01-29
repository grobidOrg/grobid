package org.grobid.trainer.sax;

import org.junit.Before;
import org.junit.Test;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class TEICitationSaxParserTest {

    TEICitationSaxParser target;
    SAXParserFactory spf;

    @Before
    public void setUp() throws Exception {
        spf = SAXParserFactory.newInstance();
        target = new TEICitationSaxParser();
    }

    @Test
    public void testCitation() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/31-1708.04410.training.references.tei.xml");

        final SAXParser p = spf.newSAXParser();
        p.parse(is, target);

        assertThat(target.getLabeledResult(), hasSize(25));
        assertThat(target.getLabeledResult().get(0), hasSize(49));
        assertThat(target.getLabeledResult().get(0).get(0).toString(), is("I-<author>"));
        assertThat(target.getTokensResult().get(0).get(0).toString(), is("H"));
        assertThat(target.getLabeledResult().get(0).get(1).toString(), is("<author>"));
        assertThat(target.getTokensResult().get(0).get(1).toString(), is("."));

    }

}
