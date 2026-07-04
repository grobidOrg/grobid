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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.data.Person;
import org.grobid.core.main.LibraryLoader;

public class AuthorParserTest {

    @BeforeClass
    public static void init() {
        LibraryLoader.load();
    }

    @Test
    public void testEtAlCitation() throws Exception {
        AuthorParser parser = new AuthorParser();
        String input = "Smith, J. et al.";
        List<Person> persons = parser.processingCitation(input);

        assertNotNull(persons);
        assertFalse("Output should not contain 'et al'", persons.toString().contains("et al"));
    }

    @Test
    public void testEtAlHeader() throws Exception {
        AuthorParser parser = new AuthorParser();
        String input = "Smith, J. et al.";
        List<Person> persons = parser.processingHeader(input);
        assertNotNull(persons);
        assertFalse("Output should not contain 'et al'", persons.toString().contains("et al"));
    }

    @Test
    public void testEtAlWithGarbage() throws Exception {
        AuthorParser parser = new AuthorParser();
        String input = "Smith, J. et al. some garbage text here";
        List<Person> persons = parser.processingCitation(input);

        assertNotNull(persons);
        assertFalse("Output should not contain 'et al'", persons.toString().contains("et al"));
        assertFalse("Output should not contain trailing garbage text", persons.toString().contains("garbage"));
    }

    @Test
    public void testEtAlWithLeadingWhitespace() throws Exception {
        AuthorParser parser = new AuthorParser();
        String input = "  Smith, J. et al.";
        List<Person> persons = parser.processingCitation(input);

        assertNotNull(persons);
        assertFalse("Output should not contain 'et al'", persons.toString().contains("et al"));
    }

    @Test
    public void testNoEtAl() throws Exception {
        AuthorParser parser = new AuthorParser();
        String input = "Smith, J.";
        List<Person> persons = parser.processingCitation(input);
        assertNotNull(persons);
        // Should contain Smith
    }

    /**
     * Diagnoses the arXiv:2310.00185 (CARLA) regression by feeding the paper's
     * author block to the AUTHOR (NAMES_HEADER) model end-to-end and asserting
     * each Person's extracted markers. With the AuthorParser fix from commit
     * 0bc3b83d4 ("fix: author marker extraction"), multi-marker clusters like
     * "b, c" should split into individual markers ["b", "c"].
     *
     * If this test fails for Worrell/Miller/Hermes/Osman, the bug is in
     * AuthorParser's marker-cluster handling (or the AUTHOR model's labelling
     * of these specific token sequences). If it passes, the markers are being
     * extracted correctly and the upstream regression must be elsewhere
     * (e.g. how the HEADER model carves the author block into segments, or
     * something specific to PDF tokenisation).
     */
    @Test
    public void test_2310_00185_authorBlock_markerExtraction() throws Exception {
        AuthorParser parser = new AuthorParser();
        String input = "Harvey Huang* a, Gabriela Ojeda Valencia b, "
                + "Nicholas M. Gregg c, Gamaleldin M. Osman c, f, "
                + "Morgan N. Montoya b, Gregory A. Worrell b, c, "
                + "Kai J. Miller b, d, Dora Hermes* b, c, e";

        List<Person> persons = parser.processingHeader(input);

        assertNotNull(persons);
        // 8 authors expected
        assertTrue(
                "Expected 8 authors, got " + persons.size() + ": " + persons,
                persons.size() == 8);

        Map<String, List<String>> got = new HashMap<>();
        for (Person p : persons) {
            List<String> ms = p.getMarkers() != null ? p.getMarkers() : new ArrayList<>();
            got.put(p.getLastName(), ms);
        }

        // Single-marker authors
        assertThat(
                "Valencia",
                got.get("Ojeda Valencia") != null
                        ? got.get("Ojeda Valencia")
                        : got.get("Valencia"),
                containsInAnyOrder("b"));
        assertThat("Gregg", got.get("Gregg"), containsInAnyOrder("c"));
        assertThat("Montoya", got.get("Montoya"), containsInAnyOrder("b"));

        // Multi-marker, shared-marker authors — the regression target
        assertThat("Osman", got.get("Osman"), containsInAnyOrder("c", "f"));
        assertThat("Worrell", got.get("Worrell"), containsInAnyOrder("b", "c"));
        assertThat("Miller", got.get("Miller"), containsInAnyOrder("b", "d"));
        assertThat("Hermes", got.get("Hermes"), containsInAnyOrder("*", "b", "c", "e"));
    }
}
