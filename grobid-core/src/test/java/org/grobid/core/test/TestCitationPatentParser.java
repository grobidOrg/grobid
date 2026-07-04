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
package org.grobid.core.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Test;

import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.PatentItem;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.utilities.GrobidProperties;

//@Ignore
public class TestCitationPatentParser extends EngineTest {
    private String newTrainingPath = null;

    @AfterClass
    public static void tearDown() {
        GrobidFactory.reset();
    }

    public File getResourceDir(String resourceDir) {
        File file = new File(resourceDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new GrobidException("Cannot start test, because test resource folder is not correctly set.");
            }
        }
        return (file);
    }

    private void getTestResourcePath() {
        newTrainingPath = GrobidProperties.getInstance().getTempPath().getAbsolutePath();
    }

    //@Test
    public void testCitationPatentParser() throws Exception {
        File textFile = new File(
                this.getResourceDir("./src/test/resources/").getAbsoluteFile() + "/patents/sample1.txt");
        if (!textFile.exists()) {
            throw new GrobidException("Cannot start test, because test resource folder is not correctly set.");
        }

        String text = FileUtils.readFileToString(textFile, "UTF-8");

        List<BibDataSet> nplResults = new ArrayList<BibDataSet>();
        List<PatentItem> patentResults = new ArrayList<PatentItem>();
        boolean consolidateCitations = false;
        /*engine.processAllCitationsInPatent(text, nplResults, patentResults, consolidateCitations);
        assertThat(patentResults.size(), is(26));
        assertThat(nplResults.size(), is(0));*/

        textFile = new File(this.getResourceDir("./src/test/resources/").getAbsoluteFile() + "/patents/sample2.txt");
        if (!textFile.exists()) {
            throw new GrobidException("Cannot start test, because test resource folder is not correctly set.");
        }
        text = FileUtils.readFileToString(textFile, "UTF-8");

        nplResults = new ArrayList<BibDataSet>();
        patentResults = new ArrayList<PatentItem>();
        /*engine.processAllCitationsInPatent(text, nplResults, patentResults, consolidateCitations);
        assertThat(patentResults.size(), is(420));
        assertThat(nplResults.size(), is(80));*/

        File xmlFile = new File(this.getResourceDir("./src/test/resources/").getAbsoluteFile()
                + "/patents/EP1059354A2.xml");
        if (!xmlFile.exists()) {
            throw new GrobidException("Cannot start test, because test resource folder is not correctly set.");
        }
        /*engine.processAllCitationsInXMLPatent(xmlFile.getPath(), nplResults, patentResults, consolidateCitations);
        System.out.println("Patent references: " + patentResults.size());
        System.out.println("Non patent references: " + nplResults.size());*/
    }

    //@Test
    public void testTrainingPatent() throws Exception {
        getTestResourcePath();

        String xmlPath = this.getResourceDir("./src/test/resources/").getAbsoluteFile() + "/patents/sample1.xml";
        engine.createTrainingPatentCitations(xmlPath, newTrainingPath);

        xmlPath = this.getResourceDir("./src/test/resources/").getAbsoluteFile() + "/patents/sample2.xml";
        engine.createTrainingPatentCitations(xmlPath, newTrainingPath);
    }

    @Test
    public void testCitationPatentParserFromText() throws Exception {
        String text = "this patent refers to US-8303618, 	and filed in continuation of US patent 8153667 and  European Patent publications 1000000 and    1000001. ";
        System.out.println("text to parse: " + text);

        List<BibDataSet> articles = new ArrayList<BibDataSet>();
        List<PatentItem> patents = new ArrayList<PatentItem>();
        int consolidateCitations = 0;
        boolean includeRawCitations = false;
        engine.processAllCitationsInPatent(text, articles, patents, consolidateCitations, includeRawCitations);

        assertEquals(4, patents.size());
        assertEquals(0, articles.size());

        PatentItem patent = patents.get(0);
        assertEquals("8303618", patent.getNumberEpoDoc());
        System.out.println("context=" + patent.getContext());
        System.out.println(
                "offset start/end/raw="
                        + patent.getOffsetBegin()
                        + "/"
                        + patent.getOffsetEnd()
                        + "/"
                        + patent.getOffsetRaw());
        System.out.println("corresponding span: " + text.substring(patent.getOffsetBegin(), patent.getOffsetEnd() + 1));

        patent = patents.get(1);
        assertEquals("8153667", patent.getNumberEpoDoc());
        System.out.println("context=" + patent.getContext());
        System.out.println(
                "offset start/end/raw="
                        + patent.getOffsetBegin()
                        + "/"
                        + patent.getOffsetEnd()
                        + "/"
                        + patent.getOffsetRaw());
        System.out.println("corresponding span: " + text.substring(patent.getOffsetBegin(), patent.getOffsetEnd() + 1));

        patent = patents.get(2);
        assertEquals("1000000", patent.getNumberEpoDoc());
        System.out.println("context=" + patent.getContext());
        System.out.println(
                "offset start/end/raw="
                        + patent.getOffsetBegin()
                        + "/"
                        + patent.getOffsetEnd()
                        + "/"
                        + patent.getOffsetRaw());
        System.out.println("corresponding span: " + text.substring(patent.getOffsetBegin(), patent.getOffsetEnd() + 1));

        patent = patents.get(3);
        assertEquals("1000001", patent.getNumberEpoDoc());
        System.out.println("context=" + patent.getContext());
        System.out.println(
                "offset start/end/raw="
                        + patent.getOffsetBegin()
                        + "/"
                        + patent.getOffsetEnd()
                        + "/"
                        + patent.getOffsetRaw());
        System.out.println("corresponding span: " + text.substring(patent.getOffsetBegin(), patent.getOffsetEnd() + 1));
    }
}
