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
package org.grobid.core.data.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.data.Affiliation;
import org.grobid.core.data.Person;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.LabeledTokensContainer;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.utilities.GrobidProperties;

public class AuthorAffiliationAssignerTest {

    @BeforeClass
    public static void setInitialContext() throws Exception {
        GrobidProperties.getInstance();
    }

    // --- Distribution tests ---

    @Test
    public void testSingleAuthorSingleAff() {
        List<Person> authors = authors("Doe");
        List<Affiliation> affs = affiliations("University of Nowhere");

        AuthorAffiliationAssigner.assign(authors, affs, null);

        assertThat(authors.get(0).getAffiliations(), hasSize(1));
        assertThat(
                authors.get(0).getAffiliations().get(0).getRawAffiliationString(),
                is("University of Nowhere"));
        assertFalse(affs.get(0).getFailAffiliation());
    }

    @Test
    public void testSingleAuthorMultipleAffs() {
        List<Person> authors = authors("Doe");
        List<Affiliation> affs = affiliations("Univ A", "Univ B", "Univ C");

        AuthorAffiliationAssigner.assign(authors, affs, null);

        assertThat(authors.get(0).getAffiliations(), hasSize(3));
        for (Affiliation aff : affs) {
            assertFalse(aff.getFailAffiliation());
        }
    }

    @Test
    public void testMultipleAuthorsSingleAff() {
        List<Person> authors = authors("Smith", "Jones", "Wang");
        List<Affiliation> affs = affiliations("MIT");

        AuthorAffiliationAssigner.assign(authors, affs, null);

        for (Person aut : authors) {
            assertThat(aut.getAffiliations(), hasSize(1));
            assertThat(aut.getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        }
        assertFalse(affs.get(0).getFailAffiliation());
    }

    // --- Marker matching tests (string-search approach) ---

    @Test
    public void testMarkerMatching_simple() {
        List<Person> authors = authors("Smith", "Wesson");

        Affiliation aff1 = affiliation("University of One");
        aff1.setMarker("1");
        Affiliation aff2 = affiliation("University of Two");
        aff2.setMarker("2");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        // Simulate the original author string with markers as superscripts
        String originalAuthors = "Smith 1, Wesson 2";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        assertThat(authors.get(0).getAffiliations(), hasSize(1));
        assertThat(authors.get(0).getAffiliations().get(0).getMarker(), is("1"));
        assertThat(authors.get(1).getAffiliations(), hasSize(1));
        assertThat(authors.get(1).getAffiliations().get(0).getMarker(), is("2"));
    }

    @Test
    public void testMarkerMatching_compound() {
        // An author with markers "1,2" should get both affiliations
        List<Person> authors = authors("Smith", "Jones");

        Affiliation aff1 = affiliation("University of One");
        aff1.setMarker("1");
        Affiliation aff2 = affiliation("University of Two");
        aff2.setMarker("2");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        // Smith has markers 1,2 and Jones has marker 2
        String originalAuthors = "Smith 1,2, Jones 2";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        // Smith gets aff 1 via marker "1" AND aff 2 via first occurrence of "2" (nearest to Smith)
        assertThat(authors.get(0).getAffiliations(), hasSize(2));
        // Jones gets aff 2 via second occurrence of "2"
        assertThat(authors.get(1).getAffiliations(), hasSize(1));
    }

    @Test
    public void testMarkerMatching_relaxed() {
        // Marker "*" on affiliation, present in original authors string
        Person a1 = person("Burda");
        Person a2 = person("Edwards");
        List<Person> authors = Arrays.asList(a1, a2);

        Affiliation aff1 = affiliation("OpenAI");
        aff1.setMarker("*");
        Affiliation aff2 = affiliation("Another Lab");
        aff2.setMarker("†");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        String originalAuthors = "Burda *, Edwards †";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        assertThat(a1.getAffiliations(), hasSize(1));
        assertThat(a1.getAffiliations().get(0).getRawAffiliationString(), is("OpenAI"));
        assertThat(a2.getAffiliations(), hasSize(1));
        assertThat(a2.getAffiliations().get(0).getRawAffiliationString(), is("Another Lab"));
    }

    @Test
    public void testMarkerMatching_noOriginalAuthors() {
        // When originalAuthors is null, marker matching is skipped and
        // fallback strategies should handle assignment
        List<Person> authors = authors("Smith", "Jones");
        // Give them layout tokens for proximity matching
        authors.get(0).setLayoutTokens(tokensAt(100, 50, 1));
        authors.get(1).setLayoutTokens(tokensAt(100, 200, 1));

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        aff1.setLayoutTokens(tokensAt(100, 60, 1));
        Affiliation aff2 = affiliation("Stanford");
        aff2.setMarker("2");
        aff2.setLayoutTokens(tokensAt(100, 190, 1));
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        AuthorAffiliationAssigner.assign(authors, affs, null);

        // Proximity fallback should assign
        assertThat(authors.get(0).getAffiliations(), hasSize(1));
        assertThat(authors.get(0).getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        assertThat(authors.get(1).getAffiliations(), hasSize(1));
        assertThat(authors.get(1).getAffiliations().get(0).getRawAffiliationString(), is("Stanford"));
    }

    @Test
    public void testMarkerMatching_preventsSingleDigitMatchingDoubleDigit() {
        // Marker "1" should not match inside "11" in the originalAuthors string
        List<Person> authors = authors("Smith", "Jones");

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        Affiliation aff11 = affiliation("Stanford");
        aff11.setMarker("11");
        List<Affiliation> affs = Arrays.asList(aff1, aff11);

        String originalAuthors = "Smith 1, Jones 11";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        // Smith should get "1" (MIT), Jones should get "11" (Stanford)
        assertThat(authors.get(0).getAffiliations(), hasSize(1));
        assertThat(authors.get(0).getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        assertThat(authors.get(1).getAffiliations(), hasSize(1));
        assertThat(authors.get(1).getAffiliations().get(0).getRawAffiliationString(), is("Stanford"));
    }

    // --- Proximity matching tests ---

    @Test
    public void testProximity_sharedAffiliation() {
        // Two authors close to same affiliation, one distant affiliation.
        // Proximity assigns both to OpenAI, then orphan rescue assigns
        // Stanford to the nearest author (Edwards at y=70 is closer to y=500).
        Person a1 = person("Burda");
        a1.setLayoutTokens(tokensAt(100, 50, 1));
        Person a2 = person("Edwards");
        a2.setLayoutTokens(tokensAt(100, 70, 1));
        List<Person> authors = Arrays.asList(a1, a2);

        Affiliation aff1 = affiliation("OpenAI");
        aff1.setLayoutTokens(tokensAt(100, 60, 1));
        Affiliation aff2 = affiliation("Stanford");
        aff2.setLayoutTokens(tokensAt(100, 500, 1));
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        AuthorAffiliationAssigner.assign(authors, affs, null);

        // Both get OpenAI via proximity, Stanford is rescued to nearest author
        assertThat(a1.getAffiliations(), hasSize(1));
        assertThat(a1.getAffiliations().get(0).getRawAffiliationString(), is("OpenAI"));
        // Edwards gets OpenAI + rescued Stanford
        assertThat(a2.getAffiliations(), hasSize(2));
        // No affiliation should be orphaned
        assertFalse(aff1.getFailAffiliation());
        assertFalse(aff2.getFailAffiliation());
    }

    @Test
    public void testProximity_interleaved() {
        // Authors and affiliations are interleaved vertically
        Person burda = person("Burda");
        burda.setLayoutTokens(tokensAt(100, 100, 1));

        Person edwards = person("Edwards");
        edwards.setLayoutTokens(tokensAt(100, 120, 1));

        Person storkey = person("Storkey");
        storkey.setLayoutTokens(tokensAt(100, 200, 1));

        Person klimov = person("Klimov");
        klimov.setLayoutTokens(tokensAt(100, 260, 1));

        List<Person> authors = Arrays.asList(burda, edwards, storkey, klimov);

        Affiliation openai = affiliation("OpenAI");
        openai.setLayoutTokens(tokensAt(100, 140, 1));

        Affiliation edinburgh = affiliation("Univ. of Edinburgh");
        edinburgh.setLayoutTokens(tokensAt(100, 220, 1));

        List<Affiliation> affs = Arrays.asList(openai, edinburgh);

        AuthorAffiliationAssigner.assign(authors, affs, null);

        assertThat(burda.getAffiliations(), hasSize(1));
        assertThat(burda.getAffiliations().get(0).getRawAffiliationString(), is("OpenAI"));
        assertThat(edwards.getAffiliations(), hasSize(1));
        assertThat(edwards.getAffiliations().get(0).getRawAffiliationString(), is("OpenAI"));
        assertThat(storkey.getAffiliations(), hasSize(1));
        assertThat(storkey.getAffiliations().get(0).getRawAffiliationString(), is("Univ. of Edinburgh"));
        assertThat(klimov.getAffiliations(), hasSize(1));
        assertThat(klimov.getAffiliations().get(0).getRawAffiliationString(), is("Univ. of Edinburgh"));
    }

    @Test
    public void testProximity_interleavedWithMarkerMatch() {
        // Marker-matched authors (via originalAuthors string), rest by proximity
        Person burda = person("Burda");
        burda.setLayoutTokens(tokensAt(100, 100, 1));

        Person edwards = person("Edwards");
        edwards.setLayoutTokens(tokensAt(100, 120, 1));

        Person storkey = person("Storkey");
        storkey.setLayoutTokens(tokensAt(100, 200, 1));

        Person klimov = person("Klimov");
        klimov.setLayoutTokens(tokensAt(100, 260, 1));

        List<Person> authors = Arrays.asList(burda, edwards, storkey, klimov);

        Affiliation openai = affiliation("OpenAI");
        openai.setMarker("*");
        openai.setLayoutTokens(tokensAt(100, 140, 1));

        Affiliation edinburgh = affiliation("Univ. of Edinburgh");
        edinburgh.setLayoutTokens(tokensAt(100, 220, 1));

        List<Affiliation> affs = Arrays.asList(openai, edinburgh);

        String originalAuthors = "Burda *, Edwards *, Storkey, Klimov";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        // Burda and Edwards match OpenAI via marker "*"
        assertThat(burda.getAffiliations(), hasSize(1));
        assertThat(burda.getAffiliations().get(0).getRawAffiliationString(), is("OpenAI"));
        assertThat(edwards.getAffiliations(), hasSize(1));
        assertThat(edwards.getAffiliations().get(0).getRawAffiliationString(), is("OpenAI"));

        // Storkey and Klimov get Edinburgh via proximity
        assertThat(storkey.getAffiliations(), hasSize(1));
        assertThat(storkey.getAffiliations().get(0).getRawAffiliationString(), is("Univ. of Edinburgh"));
        assertThat(klimov.getAffiliations(), hasSize(1));
        assertThat(klimov.getAffiliations().get(0).getRawAffiliationString(), is("Univ. of Edinburgh"));
    }

    // --- Sequential fallback tests ---

    @Test
    public void testSequentialFallback_noCoords() {
        // No markers, no coordinates → sequential 1:1
        List<Person> authors = authors("Smith", "Jones");
        List<Affiliation> affs = affiliations("MIT", "Stanford");

        AuthorAffiliationAssigner.assign(authors, affs, null);

        assertThat(authors.get(0).getAffiliations(), hasSize(1));
        assertThat(authors.get(0).getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        assertThat(authors.get(1).getAffiliations(), hasSize(1));
        assertThat(authors.get(1).getAffiliations().get(0).getRawAffiliationString(), is("Stanford"));
    }

    @Test
    public void testSequentialFallback_moreAuthorsThanAffs() {
        // 3 authors, 2 affiliations, no markers/coords → distribute remaining
        List<Person> authors = authors("Smith", "Jones", "Wang");
        List<Affiliation> affs = affiliations("MIT", "Stanford");

        AuthorAffiliationAssigner.assign(authors, affs, null);

        // With fewer affs than authors, distribute all affs to all authors
        for (Person aut : authors) {
            assertThat(aut.getAffiliations(), hasSize(2));
        }
    }

    // --- Mixed tests ---

    @Test
    public void testMixed_markersAndProximity() {
        // Author 1 matched by marker string search, author 2 by proximity
        Person a1 = person("Smith");
        a1.setLayoutTokens(tokensAt(100, 100, 1));
        Person a2 = person("Jones");
        a2.setLayoutTokens(tokensAt(100, 200, 1));
        List<Person> authors = Arrays.asList(a1, a2);

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        aff1.setLayoutTokens(tokensAt(100, 120, 1));
        Affiliation aff2 = affiliation("Stanford");
        aff2.setLayoutTokens(tokensAt(100, 210, 1));
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        String originalAuthors = "Smith 1, Jones";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        // Smith gets MIT via marker
        assertThat(a1.getAffiliations(), hasSize(1));
        assertThat(a1.getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        // Jones gets Stanford via proximity
        assertThat(a2.getAffiliations(), hasSize(1));
        assertThat(a2.getAffiliations().get(0).getRawAffiliationString(), is("Stanford"));
    }

    // --- Marker matching edge cases ---

    @Test
    public void testMarkerMatching_authorNameNotInOriginalString() {
        // Author name model returns a different form than what appears in the raw string.
        // The marker search should skip authors whose name isn't found rather than
        // producing a bogus match from a -1 indexOf result.
        Person a1 = person("Smith");
        Person a2 = person("Van der Berg"); // won't appear verbatim in originalAuthors
        List<Person> authors = Arrays.asList(a1, a2);

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        Affiliation aff2 = affiliation("Stanford");
        aff2.setMarker("2");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        // originalAuthors has "Berg" but not "Van der Berg"
        String originalAuthors = "Smith 1, Berg 2";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        // Smith is the only author whose name appears in originalAuthors,
        // so it gets both affiliations via nearest-author string search
        assertThat(a1.getAffiliations(), hasSize(2));
    }

    // --- Direct marker matching tests (Person.getMarkers() vs Affiliation.getMarker()) ---

    @Test
    public void testDirectMarkerMatching_simple() {
        Person a1 = person("Smith");
        a1.setMarkers(Arrays.asList("1"));
        Person a2 = person("Jones");
        a2.setMarkers(Arrays.asList("2"));
        List<Person> authors = Arrays.asList(a1, a2);

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        Affiliation aff2 = affiliation("Stanford");
        aff2.setMarker("2");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        AuthorAffiliationAssigner.assign(authors, affs, null);

        assertThat(a1.getAffiliations(), hasSize(1));
        assertThat(a1.getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        assertThat(a2.getAffiliations(), hasSize(1));
        assertThat(a2.getAffiliations().get(0).getRawAffiliationString(), is("Stanford"));
    }

    @Test
    public void testDirectMarkerMatching_multipleMarkersPerAuthor() {
        Person a1 = person("Smith");
        a1.setMarkers(Arrays.asList("1", "2"));
        List<Person> authors = Arrays.asList(a1);

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        Affiliation aff2 = affiliation("Stanford");
        aff2.setMarker("2");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        AuthorAffiliationAssigner.assign(authors, affs, null);

        assertThat(a1.getAffiliations(), hasSize(2));
        assertThat(a1.getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        assertThat(a1.getAffiliations().get(1).getRawAffiliationString(), is("Stanford"));
    }

    @Test
    public void testDirectMarkerMatching_sharedMarker() {
        Person a1 = person("Smith");
        a1.setMarkers(Arrays.asList("1"));
        Person a2 = person("Jones");
        a2.setMarkers(Arrays.asList("1"));
        List<Person> authors = Arrays.asList(a1, a2);

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        Affiliation aff2 = affiliation("Stanford");
        aff2.setMarker("2");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        AuthorAffiliationAssigner.assign(authors, affs, null);

        // Both authors share marker "1" → both get MIT
        // Stanford (marker "2") has no matching author but gets rescued
        // to the first author with fewest affiliations (Smith, since both have 1)
        assertThat(a1.getAffiliations(), hasSize(2));
        assertThat(a1.getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        assertThat(a1.getAffiliations().get(1).getRawAffiliationString(), is("Stanford"));
        assertThat(a2.getAffiliations(), hasSize(1));
        assertThat(a2.getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        // No affiliation should be orphaned
        assertFalse(aff1.getFailAffiliation());
        assertFalse(aff2.getFailAffiliation());
    }

    @Test
    public void testDirectMarkerMatching_fallsBackToStringSearch() {
        // Author A has person markers → matched by direct markers
        // Author B has no person markers → matched by string-search fallback
        Person a1 = person("Smith");
        a1.setMarkers(Arrays.asList("1"));
        Person a2 = person("Jones");
        // no markers set on a2
        List<Person> authors = Arrays.asList(a1, a2);

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        Affiliation aff2 = affiliation("Stanford");
        aff2.setMarker("2");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        String originalAuthors = "Smith 1, Jones 2";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        // Smith gets MIT via direct markers
        assertThat(a1.getAffiliations(), hasSize(1));
        assertThat(a1.getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        // Jones gets Stanford via string-search fallback
        assertThat(a2.getAffiliations(), hasSize(1));
        assertThat(a2.getAffiliations().get(0).getRawAffiliationString(), is("Stanford"));
    }

    @Test
    public void testDirectMarkerMatching_noPersonMarkers() {
        // No person markers set → direct marker matching is a no-op,
        // string-search handles everything
        List<Person> authors = authors("Smith", "Jones");

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        Affiliation aff2 = affiliation("Stanford");
        aff2.setMarker("2");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        String originalAuthors = "Smith 1, Jones 2";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        assertThat(authors.get(0).getAffiliations(), hasSize(1));
        assertThat(authors.get(0).getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        assertThat(authors.get(1).getAffiliations(), hasSize(1));
        assertThat(authors.get(1).getAffiliations().get(0).getRawAffiliationString(), is("Stanford"));
    }

    // --- Orphan rescue tests ---

    @Test
    public void testOrphanRescue_proximityAllSameButOrphanExists() {
        // 3 authors on same line, 3 affiliations stacked below.
        // Without rescue, proximity assigns all authors to nearest aff0,
        // leaving aff1 and aff2 orphaned.
        Person a1 = person("Jeong");
        a1.setLayoutTokens(tokensAt(100, 50, 1));
        Person a2 = person("Chang");
        a2.setLayoutTokens(tokensAt(250, 50, 1));
        Person a3 = person("Valdez");
        a3.setLayoutTokens(tokensAt(400, 50, 1));
        List<Person> authors = Arrays.asList(a1, a2, a3);

        Affiliation aff0 = affiliation("Simon Fraser");
        aff0.setLayoutTokens(tokensAt(100, 100, 1));
        Affiliation aff1 = affiliation("Texas A&M");
        aff1.setLayoutTokens(tokensAt(100, 130, 1));
        Affiliation aff2 = affiliation("UConn");
        aff2.setLayoutTokens(tokensAt(100, 160, 1));
        List<Affiliation> affs = Arrays.asList(aff0, aff1, aff2);

        AuthorAffiliationAssigner.assign(authors, affs, null);

        // All 3 affiliations should be assigned (no orphans)
        for (Affiliation aff : affs) {
            assertFalse(
                    "Affiliation '" + aff.getRawAffiliationString() + "' should not be orphaned",
                    aff.getFailAffiliation());
        }
        // Each author should have at least one affiliation
        for (Person aut : authors) {
            assertNotNull(aut.getAffiliations());
            assertFalse(
                    "Author '" + aut.getLastName() + "' should have affiliations",
                    aut.getAffiliations().isEmpty());
        }
    }

    @Test
    public void testOrphanRescue_noCoordinates() {
        // 2 authors, 3 affiliations, no markers, no coordinates.
        // Sequential assigns 1:1 (a1->aff0, a2->aff1), leaving aff2 orphaned.
        // Rescue should assign aff2 to the author with fewest affiliations.
        List<Person> authors = authors("Barua", "Maitra");
        List<Affiliation> affs = affiliations("ISI Kolkata", "UMinn", "Extra Lab");

        AuthorAffiliationAssigner.assign(authors, affs, null);

        // All affiliations should be assigned
        for (Affiliation aff : affs) {
            assertFalse(
                    "Affiliation '" + aff.getRawAffiliationString() + "' should not be orphaned",
                    aff.getFailAffiliation());
        }
    }

    // --- Null / empty tests ---

    @Test
    public void testNullAuthors() {
        AuthorAffiliationAssigner.assign(null, affiliations("MIT"), null);
        // no exception
    }

    @Test
    public void testNullAffiliations() {
        AuthorAffiliationAssigner.assign(authors("Smith"), null, null);
        assertNull(authors("Smith").get(0).getAffiliations());
    }

    @Test
    public void testEmptyAuthors() {
        AuthorAffiliationAssigner.assign(new ArrayList<>(), affiliations("MIT"), null);
        // no exception
    }

    // --- Regression: corresponding-author footnote produces phantom duplicate ---

    /**
     * Reproduces the arXiv 2102.12439 regression: the HEADER model labels the
     * "Corresponding author: Noémie Elhadad (...)" footnote as &lt;author&gt; too,
     * so AuthorParser emits a phantom Person with the same name signature
     * (lastname + first-letter-of-firstname) but no markers and layout tokens
     * near a non-claimed affiliation. If dedup runs AFTER attachAffiliations,
     * the phantom grabs that aff via proximity and the dedup-merge loop leaks
     * it onto the real author. HeaderParser was changed to dedup BEFORE
     * attachAffiliations; this test pins the post-fix behavior.
     */
    @Test
    public void testCorrespondingAuthorFootnoteDuplicate_dedupBeforeAssign() {
        Person kathy = personWithMarkers("Li", "Kathy", "1", "2");
        Person inigo = personWithMarkers("Urteaga", "Iñigo", "1", "2");
        Person amanda = personWithMarkers("Shea", "Amanda", "3");
        Person vitzthum = personWithMarkers("Vitzthum", "Virginia", "3", "4");
        Person wiggins = personWithMarkers("Wiggins", "Chris", "1", "2");
        Person elhadad = personWithMarkers("Elhadad", "Noémie", "*", "5", "2");

        // Phantom Person from the footnote: same signature ("Elhadad" + "N"),
        // no markers, layout tokens placed near aff3 in the page.
        Person phantomElhadad = new Person();
        phantomElhadad.setLastName("Elhadad");
        phantomElhadad.setFirstName("Noémie");
        phantomElhadad.setLayoutTokens(tokensAt(100, 800, 1));

        List<Person> authors = new ArrayList<>(Arrays.asList(
                kathy,
                inigo,
                amanda,
                vitzthum,
                wiggins,
                elhadad,
                phantomElhadad));

        Affiliation aff0 = affWithMarker("Dept of Applied Physics", "1");
        Affiliation aff1 = affWithMarker("Data Science Institute", "2");
        Affiliation aff2 = affWithMarker("Clue by BioWink", "3");
        Affiliation aff3 = affWithMarker("Kinsey Institute", "4");
        Affiliation aff4 = affWithMarker("Dept Biomedical Informatics", "5");
        // aff3's tokens sit close to the phantom — without dedup-first, proximity
        // would attach aff3 to the phantom and dedup would leak it onto Elhadad.
        aff3.setLayoutTokens(tokensAt(100, 750, 1));
        List<Affiliation> affs = new ArrayList<>(Arrays.asList(aff0, aff1, aff2, aff3, aff4));

        String originalAuthors = "Kathy Li 1, 2, Iñigo Urteaga 1, 2, Amanda Shea 3, "
                + "Virginia J. Vitzthum 3, 4, Chris H. Wiggins 1, 2, "
                + "and Noémie Elhadad *, 5, 2";

        // Post-fix HeaderParser order: deduplicate first, then attachAffiliations.
        authors = Person.deduplicate(authors);
        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        Person mergedElhadad = null;
        for (Person p : authors) {
            if ("Elhadad".equals(p.getLastName())) {
                mergedElhadad = p;
                break;
            }
        }
        assertNotNull("Elhadad should remain after dedup", mergedElhadad);
        assertThat(mergedElhadad.getAffiliations(), hasSize(2));

        Set<String> markers = new HashSet<>();
        for (Affiliation a : mergedElhadad.getAffiliations()) {
            markers.add(a.getMarker());
        }
        // markers 2 (Data Science Institute) and 5 (Dept Biomedical Informatics).
        // Crucially NOT 4 (Kinsey Institute) — that's Vitzthum's only.
        assertThat(markers, containsInAnyOrder("2", "5"));

        // Vitzthum still owns aff3 (marker 4)
        Set<String> vitzthumMarkers = new HashSet<>();
        for (Affiliation a : vitzthum.getAffiliations()) {
            vitzthumMarkers.add(a.getMarker());
        }
        assertThat(vitzthumMarkers, containsInAnyOrder("3", "4"));
    }

    /**
     * Reproduces arXiv 2311.05528: the HEADER model labels the
     * correspondence-line institution as &lt;affiliation&gt;, so
     * AffiliationAddressParser produces a second {@link Affiliation} that
     * mirrors aff0 minus the marker label and a "(C2SM)" parenthetical. After
     * marker matching links aff0 to both authors, aff1 remains an orphan.
     * Tier 6 ({@code rescueOrphanAffiliations}) used to dump it onto the
     * nearest author by proximity — always the corresponding author whose
     * name sits next to the corresp text. The duplicate guard added here
     * skips orphans whose normalized raw text is already covered by an
     * affiliation on the rescue target.
     */
    @Test
    public void testCorrespondenceDuplicateOrphan_2311_05528() {
        Person brigitta = personWithMarkers("Goger", "Brigitta", "1");
        brigitta.setLayoutTokens(tokensAt(100, 100, 1));
        Person anurag = personWithMarkers("Dipankar", "Anurag", "1");
        anurag.setLayoutTokens(tokensAt(100, 130, 1));

        Affiliation aff0 = affWithMarker(
                "1 Center for Climate Systems Modeling (C2SM) , ETH Zurich , Zurich , Switzerland",
                "1");
        aff0.setLayoutTokens(tokensAt(100, 200, 1));
        // The corresp-derived duplicate has no marker and sits near Brigitta.
        Affiliation aff1 = affiliation(
                "Center for Climate Systems Modeling , ETH Zurich , Zurich , Switzerland");
        aff1.setLayoutTokens(tokensAt(100, 110, 1));

        List<Person> authors = new ArrayList<>(Arrays.asList(brigitta, anurag));
        List<Affiliation> affs = new ArrayList<>(Arrays.asList(aff0, aff1));

        AuthorAffiliationAssigner.assign(authors, affs, "Brigitta Goger 1, Anurag Dipankar 1");

        assertThat(brigitta.getAffiliations(), hasSize(1));
        assertThat(brigitta.getAffiliations().get(0).getMarker(), is("1"));
        assertThat(anurag.getAffiliations(), hasSize(1));
        assertThat(anurag.getAffiliations().get(0).getMarker(), is("1"));
    }

    /**
     * Reproduces arXiv 2006.11386: Kevin Leyton-Brown's surname is hyphenated.
     * The HEADER_AUTHOR cluster tokenises as ["Kevin", "Leyton", "-", "Brown"]
     * and the original {@code matchPersonByCluster} took the last capitalised
     * word ("Brown") as the surname, which doesn't equal Person.lastName
     * "Leyton-Brown" → no match → preLink leaves Kevin floating and his block-3
     * UBC affiliation as an orphan. Post-fix: Person.lastName is matched as a
     * substring of the span's joined token text (which preserves the hyphen).
     */
    @Test
    public void test_preLink_hyphenatedSurname_2006_11386() {
        Person kevin = new Person();
        kevin.setFirstName("Kevin");
        kevin.setLastName("Leyton-Brown");

        List<LayoutToken> spanTokens = makeTokens("Kevin", " ", "Leyton", "-", "Brown");
        Person matched = AuthorAffiliationAssigner.matchPersonByCluster(spanTokens, Arrays.asList(kevin));
        assertNotNull("hyphenated surname must match Person.lastName", matched);
        assertEquals("Leyton-Brown", matched.getLastName());
    }

    /**
     * Multi-word surname (e.g. "Ojeda Valencia" from arXiv:2310.00185) must
     * also match via substring even though no single capitalised token equals
     * the full surname.
     */
    @Test
    public void test_preLink_multiWordSurname() {
        Person gabriela = new Person();
        gabriela.setFirstName("Gabriela");
        gabriela.setLastName("Ojeda Valencia");

        List<LayoutToken> spanTokens = makeTokens("Gabriela", " ", "Ojeda", " ", "Valencia");
        Person matched = AuthorAffiliationAssigner.matchPersonByCluster(spanTokens, Arrays.asList(gabriela));
        assertNotNull("multi-word surname must match Person.lastName", matched);
        assertEquals("Ojeda Valencia", matched.getLastName());
    }

    /**
     * Reproduces arXiv 2006.11386: 4 authors, 3 affiliations, no markers
     * anywhere. After {@code preLinkByPrecedingAuthorCluster} every
     * affiliation is claimed by its preceding author cluster. Victor (whose
     * own affiliation "Google" was tagged {@code <other>} upstream and lost)
     * is the only floating author. Tier 4 (proximity) used to fall back to
     * considering all affiliations when none were unresolved — Victor would
     * grab the nearest, semantically wrong assignment. Post-fix: when every
     * affiliation is claimed, the floating author stays empty.
     */
    @Test
    public void testProximity_floatingAuthorStaysEmptyWhenAllAffsClaimed() {
        Person jason = person("Hartford");
        jason.setLayoutTokens(tokensAt(100, 50, 1));
        Person victor = person("Veitch");
        victor.setLayoutTokens(tokensAt(100, 150, 1));
        Person dhanya = person("Sridhar");
        dhanya.setLayoutTokens(tokensAt(100, 250, 1));
        Person kevin = person("Leyton-Brown");
        kevin.setLayoutTokens(tokensAt(100, 350, 1));

        // All three affs already claimed (failAffiliation=false), as if preLink ran.
        Affiliation aff1 = affiliation("Department of Computer Science University of British Columbia");
        aff1.setLayoutTokens(tokensAt(100, 80, 1));
        aff1.setFailAffiliation(false);
        jason.addAffiliation(aff1);

        Affiliation aff2 = affiliation("Data Science Institute Columbia University");
        aff2.setLayoutTokens(tokensAt(100, 280, 1));
        aff2.setFailAffiliation(false);
        dhanya.addAffiliation(aff2);

        Affiliation aff3 = affiliation("Department of Computer Science University of British Columbia");
        aff3.setLayoutTokens(tokensAt(100, 380, 1));
        aff3.setFailAffiliation(false);
        kevin.addAffiliation(aff3);

        List<Person> authors = new ArrayList<>(Arrays.asList(jason, victor, dhanya, kevin));
        List<Affiliation> affs = new ArrayList<>(Arrays.asList(aff1, aff2, aff3));

        AuthorAffiliationAssigner.assign(authors, affs, null);

        // Victor's affiliation is missing from the header — he must NOT inherit
        // a neighbor's UBC by proximity.
        assertTrue(
                "Victor must stay without an affiliation",
                victor.getAffiliations() == null || victor.getAffiliations().isEmpty());

        // Other authors keep exactly their pre-linked aff.
        assertThat(jason.getAffiliations(), hasSize(1));
        assertThat(dhanya.getAffiliations(), hasSize(1));
        assertThat(kevin.getAffiliations(), hasSize(1));
    }

    @Test
    public void testIsContentDuplicate_substringAfterNormalization() {
        Affiliation a = new Affiliation();
        a.setRawAffiliationString(
                "1 Center for Climate Systems Modeling (C2SM) , ETH Zurich , Zurich , Switzerland");
        Affiliation b = new Affiliation();
        b.setRawAffiliationString(
                "Center for Climate Systems Modeling , ETH Zurich , Zurich , Switzerland");

        assertTrue(AuthorAffiliationAssigner.isContentDuplicate(b, Arrays.asList(a)));
        assertTrue(AuthorAffiliationAssigner.isContentDuplicate(a, Arrays.asList(b)));
    }

    @Test
    public void testIsContentDuplicate_distinctAffsNotDuplicate() {
        Affiliation a = new Affiliation();
        a.setRawAffiliationString("Department of Computer Science , University of British Columbia");
        Affiliation b = new Affiliation();
        b.setRawAffiliationString("Data Science Institute , Columbia University");

        assertFalse(AuthorAffiliationAssigner.isContentDuplicate(b, Arrays.asList(a)));
    }

    /**
     * Reproduces the arXiv:2310.00185 (CARLA) regression where authors with
     * multiple markers including a shared one lose their shared-marker
     * affiliations. The AUTHOR model labels the markers as listed below;
     * direct-marker matching (Tier 2) must give every author every aff
     * matching one of their markers.
     *
     * Note: Harvey's `a` is mislabelled as &lt;other&gt; by the AUTHOR model
     * upstream of this assigner (a known model defect, out of scope here).
     * His Person.markers therefore is just [*]. Tier 3 string-search must
     * then find the standalone `a` in the originalAuthors string and assign
     * aff0 to him via lastname proximity.
     */
    @Test
    public void test_2310_00185_sharedMarkers_multiAuthorAffs() {
        Person harvey = personWithMarkers("Huang", "Harvey", "*");
        Person gabriela = personWithMarkers("Valencia", "Gabriela", "b");
        Person nicholas = personWithMarkers("Gregg", "Nicholas", "c");
        Person osman = personWithMarkers("Osman", "Gamaleldin", "c", "f");
        Person morgan = personWithMarkers("Montoya", "Morgan", "b");
        Person worrell = personWithMarkers("Worrell", "Gregory", "b", "c");
        Person miller = personWithMarkers("Miller", "Kai", "b", "d");
        Person hermes = personWithMarkers("Hermes", "Dora", "*", "b", "c", "e");

        List<Person> authors = new ArrayList<>(Arrays.asList(
                harvey,
                gabriela,
                nicholas,
                osman,
                morgan,
                worrell,
                miller,
                hermes));

        Affiliation aff0 = affWithMarker("Mayo Clinic Medical Scientist Training Program", "a");
        Affiliation aff1 = affWithMarker("Department of Physiology and Biomedical Engineering", "b");
        Affiliation aff2 = affWithMarker("Department of Neurology", "c");
        Affiliation aff3 = affWithMarker("Department of Neurologic Surgery", "d");
        Affiliation aff4 = affWithMarker("Department of Radiology", "e");
        Affiliation aff5 = affWithMarker("Division of Child Neurology", "f");
        List<Affiliation> affs = new ArrayList<>(Arrays.asList(aff0, aff1, aff2, aff3, aff4, aff5));

        String originalAuthors = "Harvey Huang* a, Gabriela Ojeda Valencia b, "
                + "Nicholas M. Gregg c, Gamaleldin M. Osman c, f, "
                + "Morgan N. Montoya b, Gregory A. Worrell b, c, "
                + "Kai J. Miller b, d, Dora Hermes* b, c, e";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        assertThat(markersOf(harvey), containsInAnyOrder("a"));
        assertThat(markersOf(gabriela), containsInAnyOrder("b"));
        assertThat(markersOf(nicholas), containsInAnyOrder("c"));
        assertThat(markersOf(osman), containsInAnyOrder("c", "f"));
        assertThat(markersOf(morgan), containsInAnyOrder("b"));
        assertThat(markersOf(worrell), containsInAnyOrder("b", "c"));
        assertThat(markersOf(miller), containsInAnyOrder("b", "d"));
        assertThat(markersOf(hermes), containsInAnyOrder("b", "c", "e"));
    }

    // --- preLinkByPrecedingAuthorCluster tests ---

    /**
     * Reproduces arXiv 1902.04360: two authors both surnamed "Kim" (Taekyun
     * and Dae San), no markers anywhere, affiliations live at the back of the
     * paper with the per-author block pattern. The HEADER model labels the
     * "Corresponding Author" line as &lt;other&gt;, so only one of the two
     * affiliations is preceded by a clean &lt;author&gt; cluster — the other
     * is resolved by elimination via the existing tier waterfall.
     */
    @Test
    public void test_1902_04360_backOfPaper_perAuthorAffBlocks_noMarkers() {
        // Authors as extracted from the front of the paper (deduped).
        Person taekyun = new Person();
        taekyun.setFirstName("Taekyun");
        taekyun.setLastName("Kim");
        Person daeSan = new Person();
        daeSan.setFirstName("Dae");
        daeSan.setMiddleName("San");
        daeSan.setLastName("Kim");
        List<Person> authors = new ArrayList<>(Arrays.asList(taekyun, daeSan));

        // Affiliations as parsed by AffiliationAddressParser.
        Affiliation kwangwoon = affiliation(
                "Department of Mathematics Kwangwoon University, Seoul, 139-701, Republic of Korea");
        List<LayoutToken> kwangwoonTokens = makeTokens(
                "Department",
                "of",
                "Mathematics",
                "Kwangwoon",
                "University");
        kwangwoon.setLayoutTokens(kwangwoonTokens);

        Affiliation sogang = affiliation(
                "Department of Mathematics Sogang University, Seoul, 121-742, Republic of Korea");
        List<LayoutToken> sogangTokens = makeTokens(
                "Department",
                "of",
                "Mathematics",
                "Sogang",
                "University");
        sogang.setLayoutTokens(sogangTokens);

        List<Affiliation> affs = new ArrayList<>(Arrays.asList(kwangwoon, sogang));

        // HEADER model labelled cluster stream — back-of-paper region only
        // (front-of-paper <author>"Taekyun Kim, Dae San Kim" cluster is not
        // before any <affiliation>, so its presence/absence is irrelevant).
        List<TaggingTokenCluster> clusters = new ArrayList<>();
        // "Taekyun Kim: Corresponding Author" — mislabelled <other>
        clusters.add(
                buildCluster(
                        TaggingLabels.HEADER_OTHER,
                        makeTokens("Taekyun", "Kim", ":", "Corresponding", "Author")));
        // Kwangwoon affiliation — preceded by <other>, no link via this rule
        clusters.add(buildClusterWithSharedTokens(TaggingLabels.HEADER_AFFILIATION, kwangwoonTokens));
        // Email between Taekyun's and Dae San's blocks
        clusters.add(
                buildCluster(
                        TaggingLabels.HEADER_EMAIL,
                        makeTokens("tkkim", "@", "kw", ".", "ac", ".", "kr")));
        // "Dae San Kim" — back-of-paper second author block
        clusters.add(
                buildCluster(
                        TaggingLabels.HEADER_AUTHOR,
                        makeTokens("Dae", "San", "Kim")));
        // Sogang affiliation — directly preceded by <author>"Dae San Kim"
        clusters.add(buildClusterWithSharedTokens(TaggingLabels.HEADER_AFFILIATION, sogangTokens));

        AuthorAffiliationAssigner.preLinkByPrecedingAuthorCluster(authors, affs, clusters);

        // After pre-link: Dae San should own Sogang. Taekyun and Kwangwoon
        // are still floating, and assign() must resolve them by elimination.
        AuthorAffiliationAssigner.assign(authors, affs, null);

        assertThat(taekyun.getAffiliations(), hasSize(1));
        assertThat(
                taekyun.getAffiliations().get(0).getRawAffiliationString(),
                is(kwangwoon.getRawAffiliationString()));
        assertThat(daeSan.getAffiliations(), hasSize(1));
        assertThat(
                daeSan.getAffiliations().get(0).getRawAffiliationString(),
                is(sogang.getRawAffiliationString()));
        assertFalse(kwangwoon.getFailAffiliation());
        assertFalse(sogang.getFailAffiliation());
    }

    /**
     * The pre-link tier must be a pure no-op when any marker exists, so that
     * existing marker-driven flows keep their behaviour unchanged.
     */
    @Test
    public void test_preLink_isNoOpWhenAnyMarkerPresent() {
        Person taekyun = new Person();
        taekyun.setFirstName("Taekyun");
        taekyun.setLastName("Kim");
        Person daeSan = new Person();
        daeSan.setFirstName("Dae");
        daeSan.setLastName("Kim");
        // Marker on one author — gate must trip
        daeSan.addMarker("1");
        List<Person> authors = Arrays.asList(taekyun, daeSan);

        Affiliation kwangwoon = affiliation("Kwangwoon");
        List<LayoutToken> kwangwoonTokens = makeTokens("Kwangwoon", "University");
        kwangwoon.setLayoutTokens(kwangwoonTokens);
        Affiliation sogang = affiliation("Sogang");
        List<LayoutToken> sogangTokens = makeTokens("Sogang", "University");
        sogang.setLayoutTokens(sogangTokens);
        List<Affiliation> affs = Arrays.asList(kwangwoon, sogang);

        List<TaggingTokenCluster> clusters = Arrays.asList(
                buildCluster(TaggingLabels.HEADER_AUTHOR, makeTokens("Dae", "San", "Kim")),
                buildClusterWithSharedTokens(TaggingLabels.HEADER_AFFILIATION, sogangTokens));

        AuthorAffiliationAssigner.preLinkByPrecedingAuthorCluster(authors, affs, clusters);

        // No links should have been placed
        assertNull(taekyun.getAffiliations());
        assertNull(daeSan.getAffiliations());
        assertTrue(kwangwoon.getFailAffiliation());
        assertTrue(sogang.getFailAffiliation());
    }

    /**
     * Multi-author shared affiliation — front-of-paper pattern without markers.
     * Walking back through consecutive &lt;author&gt; clusters should link all
     * three authors to the single shared affiliation.
     */
    @Test
    public void test_preLink_multipleConsecutiveAuthorsShareAffiliation() {
        Person a1 = new Person();
        a1.setFirstName("Alice");
        a1.setLastName("Apple");
        Person a2 = new Person();
        a2.setFirstName("Bob");
        a2.setLastName("Banana");
        Person a3 = new Person();
        a3.setFirstName("Carol");
        a3.setLastName("Cherry");
        List<Person> authors = Arrays.asList(a1, a2, a3);

        Affiliation lab = affiliation("Shared Lab");
        List<LayoutToken> labTokens = makeTokens("Shared", "Lab");
        lab.setLayoutTokens(labTokens);
        List<Affiliation> affs = new ArrayList<>(Arrays.asList(lab));

        List<TaggingTokenCluster> clusters = Arrays.asList(
                buildCluster(TaggingLabels.HEADER_AUTHOR, makeTokens("Alice", "Apple")),
                buildCluster(TaggingLabels.HEADER_AUTHOR, makeTokens("Bob", "Banana")),
                buildCluster(TaggingLabels.HEADER_AUTHOR, makeTokens("Carol", "Cherry")),
                buildClusterWithSharedTokens(TaggingLabels.HEADER_AFFILIATION, labTokens));

        AuthorAffiliationAssigner.preLinkByPrecedingAuthorCluster(authors, affs, clusters);

        assertThat(a1.getAffiliations(), hasSize(1));
        assertThat(a2.getAffiliations(), hasSize(1));
        assertThat(a3.getAffiliations(), hasSize(1));
        assertFalse(lab.getFailAffiliation());
    }

    /**
     * When a non-&lt;author&gt; cluster sits between &lt;author&gt; and
     * &lt;affiliation&gt;, the walk-back stops and no link is made.
     */
    @Test
    public void test_preLink_interveningOtherClusterBlocksLink() {
        Person a = new Person();
        a.setFirstName("Alice");
        a.setLastName("Apple");
        Person b = new Person();
        b.setFirstName("Bob");
        b.setLastName("Banana");
        List<Person> authors = Arrays.asList(a, b);

        Affiliation lab = affiliation("Lab");
        List<LayoutToken> labTokens = makeTokens("Lab");
        lab.setLayoutTokens(labTokens);
        List<Affiliation> affs = Arrays.asList(lab);

        List<TaggingTokenCluster> clusters = Arrays.asList(
                buildCluster(TaggingLabels.HEADER_AUTHOR, makeTokens("Alice", "Apple")),
                buildCluster(TaggingLabels.HEADER_OTHER, makeTokens("Email", ":", "x", "@", "y")),
                buildClusterWithSharedTokens(TaggingLabels.HEADER_AFFILIATION, labTokens));

        AuthorAffiliationAssigner.preLinkByPrecedingAuthorCluster(authors, affs, clusters);

        assertNull(a.getAffiliations());
        assertNull(b.getAffiliations());
        assertTrue(lab.getFailAffiliation());
    }

    /**
     * Reproduces arXiv 0810.3241: the HEADER model emits a single
     * &lt;author&gt; cluster carrying multiple author names separated by ",".
     * Pre-link must split the cluster into name spans and link every matched
     * author to the affiliation; before the fix, only the last surname was
     * considered and the other authors stayed floating, sometimes ending up on
     * an unrelated affiliation via the proximity fallback.
     */
    @Test
    public void test_preLink_multiAuthorClusterLinksAllNames() {
        Person mankame = new Person();
        mankame.setFirstName("Devdatta");
        mankame.setLastName("Mankame");
        Person doi = new Person();
        doi.setFirstName("Takumi");
        doi.setLastName("Doi");
        Person draper = new Person();
        draper.setFirstName("Terrence");
        draper.setLastName("Draper");
        Person liu = new Person();
        liu.setFirstName("Keh-Fei");
        liu.setLastName("Liu");
        List<Person> authors = Arrays.asList(mankame, doi, draper, liu);

        Affiliation kentucky = affiliation("University of Kentucky");
        List<LayoutToken> kentuckyTokens = makeTokens("177", "Chem", ".", "-", "Phys", ".", "Building");
        kentucky.setLayoutTokens(kentuckyTokens);
        List<Affiliation> affs = Arrays.asList(kentucky);

        List<TaggingTokenCluster> clusters = Arrays.asList(
                buildCluster(
                        TaggingLabels.HEADER_AUTHOR,
                        makeTokens(
                                "Devdatta",
                                "Mankame",
                                ",",
                                "Takumi",
                                "Doi",
                                ",",
                                "Terrence",
                                "Draper",
                                ",",
                                "Keh-Fei",
                                "Liu")),
                buildClusterWithSharedTokens(TaggingLabels.HEADER_AFFILIATION, kentuckyTokens));

        AuthorAffiliationAssigner.preLinkByPrecedingAuthorCluster(authors, affs, clusters);

        assertThat(mankame.getAffiliations(), hasSize(1));
        assertThat(doi.getAffiliations(), hasSize(1));
        assertThat(draper.getAffiliations(), hasSize(1));
        assertThat(liu.getAffiliations(), hasSize(1));
        assertFalse(kentucky.getFailAffiliation());
    }

    /**
     * Multi-author cluster joined by "and": the keyword splits names too.
     */
    @Test
    public void test_preLink_multiAuthorClusterAndSeparator() {
        Person a = new Person();
        a.setFirstName("Alice");
        a.setLastName("Apple");
        Person b = new Person();
        b.setFirstName("Bob");
        b.setLastName("Banana");
        List<Person> authors = Arrays.asList(a, b);

        Affiliation lab = affiliation("Lab");
        List<LayoutToken> labTokens = makeTokens("Lab");
        lab.setLayoutTokens(labTokens);
        List<Affiliation> affs = Arrays.asList(lab);

        List<TaggingTokenCluster> clusters = Arrays.asList(
                buildCluster(
                        TaggingLabels.HEADER_AUTHOR,
                        makeTokens("Alice", "Apple", "and", "Bob", "Banana")),
                buildClusterWithSharedTokens(TaggingLabels.HEADER_AFFILIATION, labTokens));

        AuthorAffiliationAssigner.preLinkByPrecedingAuthorCluster(authors, affs, clusters);

        assertThat(a.getAffiliations(), hasSize(1));
        assertThat(b.getAffiliations(), hasSize(1));
        assertFalse(lab.getFailAffiliation());
    }

    /**
     * Reproduces arXiv 0706.2908: header model emits two authors in a single
     * &lt;author&gt; cluster with neither comma nor "and" tagged as a
     * separator. The pre-link tier's surname-search finds BOTH authors as
     * candidates, but the legacy single-Person fallback only returned the
     * one whose first initial matched the leading capital. With multi-author
     * matching, every candidate whose first initial appears as a capitalised
     * surface word in the span gets linked.
     */
    @Test
    public void test_preLink_multiAuthorClusterWithoutSeparator_0706_2908() {
        Person atkinson = personWithMarkers("Atkinson", "M.D.");
        Person willigenburg = personWithMarkers("Willigenburg", "S.J.");
        Person pfeiffer = personWithMarkers("Pfeiffer", "G.");
        List<Person> authors = Arrays.asList(atkinson, willigenburg, pfeiffer);

        Affiliation school = affiliation("School of Mathematical and Computational Sciences");
        List<LayoutToken> schoolTokens = makeTokens("School", "of", "Mathematical");
        school.setLayoutTokens(schoolTokens);
        Affiliation dept = affiliation("Department of Mathematics University College");
        List<LayoutToken> deptTokens = makeTokens("Department", "of", "Mathematics");
        dept.setLayoutTokens(deptTokens);
        List<Affiliation> affs = Arrays.asList(school, dept);

        // Critical: NO "," or "and" token between the two authors — mirrors
        // the buggy header-model output for 0706.2908.
        List<TaggingTokenCluster> clusters = Arrays.asList(
                buildCluster(
                        TaggingLabels.HEADER_AUTHOR,
                        makeTokens("M", ".", "D", ".", "Atkinson", "S", ".", "J", ".", "Willigenburg")),
                buildClusterWithSharedTokens(TaggingLabels.HEADER_AFFILIATION, schoolTokens),
                buildCluster(TaggingLabels.HEADER_AUTHOR, makeTokens("G", ".", "Pfeiffer")),
                buildClusterWithSharedTokens(TaggingLabels.HEADER_AFFILIATION, deptTokens));

        AuthorAffiliationAssigner.preLinkByPrecedingAuthorCluster(authors, affs, clusters);

        assertThat(atkinson.getAffiliations(), hasSize(1));
        assertThat(
                atkinson.getAffiliations().get(0).getRawAffiliationString(),
                containsString("School"));
        // Pre-fix: this assertion failed (size 0) because the legacy first-
        // initial filter only selected Atkinson, leaving Willigenburg
        // unmatched and unaffiliated.
        assertThat(willigenburg.getAffiliations(), hasSize(1));
        assertThat(
                willigenburg.getAffiliations().get(0).getRawAffiliationString(),
                containsString("School"));
        assertThat(pfeiffer.getAffiliations(), hasSize(1));
        assertThat(
                pfeiffer.getAffiliations().get(0).getRawAffiliationString(),
                containsString("Department"));
    }

    // --- Helper methods ---

    private static Set<String> markersOf(Person p) {
        Set<String> result = new HashSet<>();
        if (p.getAffiliations() != null) {
            for (Affiliation a : p.getAffiliations()) {
                result.add(a.getMarker());
            }
        }
        return result;
    }

    private static Person person(String lastName) {
        Person p = new Person();
        p.setLastName(lastName);
        return p;
    }

    private static Person personWithMarkers(String lastName, String firstName, String... markers) {
        Person p = new Person();
        p.setLastName(lastName);
        p.setFirstName(firstName);
        for (String m : markers) {
            p.addMarker(m);
        }
        return p;
    }

    private static Affiliation affWithMarker(String rawString, String marker) {
        Affiliation aff = new Affiliation();
        aff.setRawAffiliationString(rawString);
        aff.setMarker(marker);
        return aff;
    }

    private static Affiliation affiliation(String rawString) {
        Affiliation aff = new Affiliation();
        aff.setRawAffiliationString(rawString);
        return aff;
    }

    private static List<Person> authors(String... lastNames) {
        List<Person> list = new ArrayList<>();
        for (String name : lastNames) {
            list.add(person(name));
        }
        return list;
    }

    private static List<Affiliation> affiliations(String... rawStrings) {
        List<Affiliation> list = new ArrayList<>();
        for (String raw : rawStrings) {
            list.add(affiliation(raw));
        }
        return list;
    }

    /**
     * Create a list containing a single LayoutToken with the given coordinates.
     */
    private static List<LayoutToken> tokensAt(double x, double y, int page) {
        LayoutToken token = new LayoutToken();
        token.setX(x);
        token.setY(y);
        token.setPage(page);
        List<LayoutToken> tokens = new ArrayList<>();
        tokens.add(token);
        return tokens;
    }

    /**
     * Build a list of fresh LayoutTokens, one per surface word.
     */
    private static List<LayoutToken> makeTokens(String... words) {
        List<LayoutToken> tokens = new ArrayList<>();
        for (String word : words) {
            LayoutToken tok = new LayoutToken();
            tok.setText(word);
            tokens.add(tok);
        }
        return tokens;
    }

    /**
     * Build a TaggingTokenCluster carrying its own copy of the given tokens.
     * Use this for clusters that are distinct from any Affiliation's
     * layoutTokens (e.g. &lt;author&gt;, &lt;other&gt; clusters).
     */
    private static TaggingTokenCluster buildCluster(TaggingLabel label, List<LayoutToken> tokens) {
        TaggingTokenCluster cluster = new TaggingTokenCluster(label);
        cluster.addLabeledTokensContainer(new LabeledTokensContainer(tokens, "", label, true));
        return cluster;
    }

    /**
     * Build a TaggingTokenCluster that shares the exact same LayoutToken
     * instances as a parsed Affiliation's layoutTokens. Reference equality is
     * how the pre-link tier maps a HEADER &lt;affiliation&gt; cluster back to
     * its parsed Affiliation, so both must hold the same LayoutToken objects.
     */
    private static TaggingTokenCluster buildClusterWithSharedTokens(
            TaggingLabel label,
            List<LayoutToken> sharedTokens) {
        TaggingTokenCluster cluster = new TaggingTokenCluster(label);
        cluster.addLabeledTokensContainer(new LabeledTokensContainer(sharedTokens, "", label, true));
        return cluster;
    }
}
