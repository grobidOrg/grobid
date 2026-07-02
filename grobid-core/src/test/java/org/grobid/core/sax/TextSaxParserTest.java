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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Before;
import org.junit.Test;

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
