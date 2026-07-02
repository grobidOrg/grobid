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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Before;
import org.junit.Test;

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
