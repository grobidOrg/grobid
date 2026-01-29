package org.grobid.core.sax;

import org.grobid.core.data.BiblioItem;
import org.grobid.core.main.LibraryLoader;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CrossrefUnixrefSaxParserTest {

    SAXParserFactory spf = SAXParserFactory.newInstance();
    CrossrefUnixrefSaxParser target;
    BiblioItem item;

    @BeforeClass
    public static void init() throws Exception {
        LibraryLoader.load();
    }

    @Before
    public void setUp() throws Exception {
        item = new BiblioItem();
        target = new CrossrefUnixrefSaxParser(item);
    }

    @Test
    public void testParseCrossrefDoi() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("crossref_response.doi.xml");

        SAXParser p = spf.newSAXParser();
        p.parse(inputStream, target);

        assertThat(item.getDOI(), is("10.1007/s00005-009-0056-3"));
    }

    @Test
    public void testParseCrossrefDoi_References() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("crossref_response.doi.2.xml");

        SAXParser p = spf.newSAXParser();
        p.parse(inputStream, target);

        assertThat(item.getDOI(), is("10.1111/j.1467-8659.2007.01100.x"));
    }

}
