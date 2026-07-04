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
package org.grobid.trainer.sax;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Before;
import org.junit.Test;

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
