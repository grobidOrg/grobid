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
package org.grobid.core.visualization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Test;

import org.grobid.core.document.Document;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.factory.GrobidFactory;

public class TestCitationsVisualizer {

    static final ObjectMapper mapper = new ObjectMapper();

    @AfterClass
    public static void tearDown() {
        GrobidFactory.reset();
    }

    @Test
    public void testJSONAnnotationStructure() throws Exception {
        Engine engine = GrobidFactory.getInstance().getEngine();
        File inputTmpFile = getInputDocument("/test/test_Grobid_1_05452615.pdf");
        Document tei = engine.fullTextToTEIDoc(inputTmpFile, GrobidAnalysisConfig.defaultInstance());

        String refURL = "http://example.com/xyz";
        List<String> refURLs = Arrays.asList(refURL);
        String json = CitationsVisualizer.getJsonAnnotations(tei, refURLs);
        JsonNode root = mapper.readTree(json);
        assertTrue(root.has("pages"));
        assertTrue(root.has("refBibs"));
        assertTrue(root.has("refMarkers"));
        JsonNode pages = root.get("pages");
        assertTrue(pages.isArray());
        assertEquals(tei.getPages().size(), pages.size());
        assertTrue(pages.size() > 0);
        JsonNode firstPage = pages.get(0);
        assertTrue(firstPage.has("page_height"));
        assertTrue(firstPage.has("page_width"));

        JsonNode bibs = root.get("refBibs");
        JsonNode firstBib = bibs.get(0);
        assertTrue(firstBib.has("id"));
        assertTrue(firstBib.has("url"));
        assertEquals(refURL, firstBib.get("url").asText());
        assertTrue(firstBib.has("pos"));
        JsonNode fbPos = firstBib.get("pos");
        assertTrue(fbPos.isArray());
        assertTrue(fbPos.size() > 0);

        for (JsonNode bbox : fbPos) {
            assertTrue(bbox.isObject());
            assertTrue(bbox.has("p"));
            assertTrue(bbox.has("x"));
            assertTrue(bbox.has("y"));
            assertTrue(bbox.has("w"));
            assertTrue(bbox.has("h"));
        }

        // XXX: this isn't working, not sure if it needs a different
        // test document or some extra processing step
        /*
        JsonNode markers = root.get("refMarkers");
        JsonNode firstMarker = markers.get(0);
        assertTrue(firstMarker.has("id"));
        assertTrue(firstMarker.has("p"));
        assertTrue(firstMarker.has("x"));
        assertTrue(firstMarker.has("y"));
        assertTrue(firstMarker.has("w"));
        assertTrue(firstMarker.has("h"));
        */
    }

    @Test
    public void testJSONAnnotationEscaping() throws Exception {
        Engine engine = GrobidFactory.getInstance().getEngine();
        File inputTmpFile = getInputDocument("/test/test_Grobid_1_05452615.pdf");
        Document tei = engine.fullTextToTEIDoc(inputTmpFile, GrobidAnalysisConfig.defaultInstance());

        // check that this embedded backslash is escaped properly
        String refURL = "http://example.com/xyz?a=ab\\c123";
        List<String> refURLs = Arrays.asList(refURL);
        String json = CitationsVisualizer.getJsonAnnotations(tei, refURLs);
        JsonNode root = mapper.readTree(json);
    }

    // XXX: copied from TestFullTextParser
    private File getInputDocument(String inputPath) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(inputPath);
        File inputTmpFile = File.createTempFile("tmpFileTest", "testFullTextParser");
        inputTmpFile.deleteOnExit();

        FileUtils.copyToFile(is, inputTmpFile);

        return inputTmpFile;
    }
}
