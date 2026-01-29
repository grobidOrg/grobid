package org.grobid.core.sax;

import org.junit.Before;
import org.junit.Test;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.InputStream;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class TextSaxParserTest {
    SAXParserFactory spf = SAXParserFactory.newInstance();

    TextSaxParser target;

    @Before
    public void setUp() throws Exception {
        target = new TextSaxParser();
        target.addFilter("description");
        target.addFilter("p");
        target.addFilter("heading");
        target.addFilter("head");
    }

    @Test
    public void testParseSize() throws Exception {
        // get a factory
        SAXParserFactory spf = SAXParserFactory.newInstance();

        InputStream is = this.getClass().getResourceAsStream("patent.xml");

        SAXParser p = spf.newSAXParser();
        p.parse(is, target);

        List<String> segments = target.getTexts();
        assertThat(segments, hasSize(7));
    }

    @Test
    public void testParseContent() throws Exception {
        // get a factory
        SAXParserFactory spf = SAXParserFactory.newInstance();

        InputStream is = this.getClass().getResourceAsStream("patent.xml");

        SAXParser p = spf.newSAXParser();
        p.parse(is, target);

        List<String> segments = target.getTexts();
        assertThat(segments.get(0), is("TECHNICAL FIELD"));
    }
}
