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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.nio.file.FileSystems;

import org.junit.AfterClass;
import org.junit.Test;

import org.grobid.core.data.BiblioItem;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.utilities.GrobidProperties;

public class TestHeaderParser extends EngineTest {

    private String testPath = null;
    private String newTrainingPath = null;

    public static final String TEST_RESOURCES_PATH = "./src/test/resources/test";

    @AfterClass
    public static void tearDown() {
        GrobidFactory.reset();
    }

    private void getTestResourcePath() {
        testPath = TEST_RESOURCES_PATH;
        GrobidProperties.getInstance();
        newTrainingPath = GrobidProperties.getTempPath().getAbsolutePath();
    }

    @Test
    public void testHeaderHeader() throws Exception {
        getTestResourcePath();

        String pdfPath = testPath + File.separator + "Wang-paperAVE2008.pdf";
        File pdfFile = new File(pdfPath);
        BiblioItem resHeader = new BiblioItem();

        String tei = engine.processHeader(pdfFile.getAbsolutePath(), resHeader);

        assertNotNull(resHeader);
        assertThat(resHeader.getTitle(), is("Information Synthesis for Answer Validation"));
        assertThat(
                resHeader.getKeyword(),
                is("Answer Validation, Recognizing Textual Entailment, Information Synthesis"));
        assertNotNull(resHeader.getFullAuthors());

        String absolutePath = FileSystems.getDefault().getPath(testPath).normalize().toAbsolutePath().toString();
        pdfPath = absolutePath + File.separator + "ZFN-A-054-0304-0272.pdf";
        resHeader = new BiblioItem();
        tei = engine.processHeader(pdfPath, resHeader);

        assertNotNull(resHeader);
        //System.out.println(tei);

        pdfPath = absolutePath + File.separator + "ZNC-1988-43c-0034.pdf";
        resHeader = new BiblioItem();
        tei = engine.processHeader(pdfPath, resHeader);
        //System.out.println(tei);

        //assertNotNull(resHeader);

        pdfPath = absolutePath + File.separator + "ZNC-1988-43c-0065.pdf";
        resHeader = new BiblioItem();
        tei = engine.processHeader(pdfPath, resHeader);

        assertNotNull(resHeader);
        //System.out.println(tei);

    }

    /*@Test
    public void testSegmentationHeader() throws Exception {
        getTestResourcePath();

        File pdfPath = new File(testPath + File.separator + "Wang-paperAVE2008.pdf");
        BiblioItem resHeader = new BiblioItem();

        String tei = engine.segmentAndProcessHeader(pdfPath, 0, resHeader);

        assertNotNull(resHeader);
        assertThat(resHeader.getTitle(), is("Information Synthesis for Answer Validation"));
        assertThat(resHeader.getKeyword(),
                is("Answer Validation, Recognizing Textual Entailment, Information Synthesis"));
        assertNotNull(resHeader.getFullAuthors());
    }*/
}
