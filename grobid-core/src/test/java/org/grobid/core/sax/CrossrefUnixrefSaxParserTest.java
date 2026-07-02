/*
 * Copyright 2008-2026 GROBID contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grobid.core.sax;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.data.BiblioItem;
import org.grobid.core.main.LibraryLoader;

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
