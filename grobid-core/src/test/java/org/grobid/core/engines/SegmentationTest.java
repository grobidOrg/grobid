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
package org.grobid.core.engines;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.factory.AbstractEngineFactory;

public class SegmentationTest {

    Segmentation target;

    @BeforeClass
    public static void setInitialContext() throws Exception {
        //        MockContext.setInitialContext();
        AbstractEngineFactory.init();
    }

    @AfterClass
    public static void destroyInitialContext() throws Exception {
        //        MockContext.destroyInitialContext();
    }

    @Before
    public void setUp() throws Exception {
        target = new Segmentation();
    }

    @Test
    public void testGetAllLinesFeatures_SimpleDocument_shouldWork() throws Exception {
        File input = new File(this.getClass().getResource("samplePdf.segmentation.pdf").toURI());
        DocumentSource doc = DocumentSource.fromPdf(input);

        final Document document = new Document(doc);
        document.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());
        String output = target.getAllLinesFeatured(document);

        String[] splittedOutput = output.split("\n");

        assertThat(splittedOutput.length, is(25));
        assertThat(splittedOutput[0], startsWith("Title"));
        assertThat(
                splittedOutput[0],
                is(
                        "Title Title title T Ti Tit Titl BLOCKSTART PAGESTART NEWFONT HIGHERFONT 1 0 INITCAP NODIGIT 0 0 1 0 0 0 0 0 12 12 no 0 10 0 1 0 0 1"));

        doc.close(true, true, true);
    }

    @Test
    public void testPrepareDocument_SimpleDocument_shouldWork() throws Exception {
        File input = new File(this.getClass().getResource("samplePdf.segmentation.pdf").toURI());
        DocumentSource doc = DocumentSource.fromPdf(input);

        final Document document = new Document(doc);
        document.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());
        Document output = target.prepareDocument(document);

        assertThat(output, notNullValue());
        assertThat(output.getPages().size(), is(1));
        //        assertThat(output.getBody(), notNullValue());
        assertThat(output.getBlocks().size(), is(3));
        assertThat(output.getTokenizations().size(), is(344));

        doc.close(true, true, true);
    }

}
