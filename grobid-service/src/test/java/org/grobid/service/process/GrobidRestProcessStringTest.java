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
package org.grobid.service.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.grobid.core.data.BiblioItem;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.factory.GrobidPoolingFactory;
import org.grobid.service.util.ExpectedResponseType;

public class GrobidRestProcessStringTest {

    private static final String VALID_CITATION = "Graff, Expert. Opin. Ther. Targets (2002) 6(1): 103-113";
    private static final String EMPTY_CITATION = "";
    private static final String NBSP_CITATION = "\u00A0";
    private static final String TEI_FRAGMENT = "<biblStruct xml:id=\"b0\"/>";
    private static final String BIBTEX_FRAGMENT = "@article{graff2002,}";

    private GrobidRestProcessString target;
    private GrobidAnalysisConfig config;

    @Before
    public void setUp() {
        target = new GrobidRestProcessString();
        config = GrobidAnalysisConfig.defaultInstance();
    }

    @Test
    public void processCitationList_emptyAndNbspAmongCitations_xml_doesNotReturn500() throws Exception {
        BiblioItem parsed = parsedItem();
        List<String> citations = Arrays.asList(VALID_CITATION, EMPTY_CITATION, NBSP_CITATION);
        // CitationParser inserts null BiblioItem for blank / NBSP-only raw strings.
        List<BiblioItem> parsedItems = Arrays.asList(parsed, null, null);

        Response response = processCitationList(citations, parsedItems, ExpectedResponseType.XML);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String body = (String) response.getEntity();
        assertTrue(body.contains(TEI_FRAGMENT));
        // Output biblStruct count must match input citation count (including empty slots).
        assertEquals(citations.size(), countOccurrences(body, "<biblStruct"));
        assertTrue(body.contains("xml:id=\"b1\""));
        assertTrue(body.contains("xml:id=\"b2\""));
    }

    @Test
    public void processCitationList_emptyAndNbspAmongCitations_bibtex_doesNotReturn500() throws Exception {
        BiblioItem parsed = parsedItem();
        List<String> citations = Arrays.asList(VALID_CITATION, EMPTY_CITATION, NBSP_CITATION);
        List<BiblioItem> parsedItems = Arrays.asList(parsed, null, null);

        Response response = processCitationList(citations, parsedItems, ExpectedResponseType.BIBTEX);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String body = (String) response.getEntity();
        assertTrue(body.contains(BIBTEX_FRAGMENT));
        // One BibTeX entry per input citation, including empty slots.
        assertEquals(citations.size(), countOccurrences(body, "@"));
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int from = 0;
        while ((from = haystack.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }

    private BiblioItem parsedItem() {
        BiblioItem parsed = mock(BiblioItem.class);
        when(parsed.toTEI(anyInt(), any(GrobidAnalysisConfig.class))).thenReturn(TEI_FRAGMENT);
        when(parsed.generateBibTeXKey()).thenReturn("graff2002");
        when(parsed.toBibTeX(any(), any(GrobidAnalysisConfig.class))).thenReturn(BIBTEX_FRAGMENT);
        return parsed;
    }

    private Response processCitationList(
            List<String> citations,
            List<BiblioItem> parsedItems,
            ExpectedResponseType responseType) throws Exception {
        Engine engine = mock(Engine.class);
        when(engine.processRawReferences(citations, config.getConsolidateCitations())).thenReturn(parsedItems);

        try (MockedStatic<Engine> engineStatic = Mockito.mockStatic(Engine.class);
                MockedStatic<GrobidPoolingFactory> poolingFactory = Mockito.mockStatic(GrobidPoolingFactory.class)) {
            engineStatic.when(() -> Engine.getEngine(true)).thenReturn(engine);
            poolingFactory.when(() -> GrobidPoolingFactory.returnEngine(engine)).thenAnswer(invocation -> null);

            return target.processCitationList(citations, config, responseType);
        }
    }
}
