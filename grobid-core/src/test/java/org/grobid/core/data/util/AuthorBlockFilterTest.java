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

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.grobid.core.data.Person;

public class AuthorBlockFilterTest {

    @Test
    public void testSingleBlockKept() {
        List<Person> result = AuthorBlockFilter.filter(
                blocks(block("Kerstin Seidel", "Pauline Marangoni")),
                Arrays.asList(false));

        assertThat(surnames(result), contains("Seidel", "Marangoni"));
    }

    @Test
    public void testSameSurnameCoAuthorsInFirstBlockKept() {
        List<Person> result = AuthorBlockFilter.filter(
                blocks(block("Ji Young Lee", "Ji Youl Lee", "Hye Seung Lee")),
                null);

        assertThat(surnames(result), contains("Lee", "Lee", "Lee"));
    }

    @Test
    public void testReMentionBlockDropped() {
        List<Person> result = AuthorBlockFilter.filter(
                blocks(block("Shaun Jackman", "Inanc Birol"), block("Inanc Birol")),
                Arrays.asList(false, false));

        assertThat(surnames(result), contains("Jackman", "Birol"));
    }

    @Test
    public void testReMentionBlockDroppedWithItsFragments() {
        // "Author ORCIDs Pauline Marangoni" parsed as "Pauline [Orcids]" + "[Marangoni]"
        List<Person> result = AuthorBlockFilter.filter(
                blocks(block("Kerstin Seidel", "Pauline Marangoni"), block("Pauline Orcids", "_ Marangoni")),
                null);

        assertThat(surnames(result), contains("Seidel", "Marangoni"));
    }

    @Test
    public void testNewNamesBeforeAbstractKept() {
        // author list split over two blocks, e.g. by interleaved affiliations
        List<Person> result = AuthorBlockFilter.filter(
                blocks(block("Kerstin Seidel"), block("Pauline Marangoni", "Cynthia Tang")),
                Arrays.asList(false, false));

        assertThat(surnames(result), contains("Seidel", "Marangoni", "Tang"));
    }

    @Test
    public void testBlockAfterAbstractDroppedWhenListBeforeAbstract() {
        List<Person> result = AuthorBlockFilter.filter(
                blocks(block("Brent Nannenga", "Tamir Gonen"), block("Howard Hughes")),
                Arrays.asList(false, true));

        assertThat(surnames(result), contains("Nannenga", "Gonen"));
    }

    @Test
    public void testOnlyAuthorBlockAfterAbstractKept() {
        List<Person> result = AuthorBlockFilter.filter(
                blocks(block("Brent Nannenga", "Tamir Gonen")),
                Arrays.asList(true));

        assertThat(surnames(result), contains("Nannenga", "Gonen"));
    }

    @Test
    public void testPositionsIgnoredWhenSizesDiffer() {
        List<Person> result = AuthorBlockFilter.filter(
                blocks(block("Brent Nannenga", "Tamir Gonen"), block("Howard Hughes")),
                Arrays.asList(false));

        assertThat(surnames(result), contains("Nannenga", "Gonen", "Hughes"));
    }

    @Test
    public void testReMentionWithDamagedNamesDropped() {
        // re-mention with a stray marker and a missing forename still counts as already listed
        List<Person> result = AuthorBlockFilter.filter(
                blocks(block("Carina Jagers", "Henk Roelink"), block("Carina Jagers1", "_ Roelink")),
                Arrays.asList(false, false));

        assertThat(surnames(result), contains("Jagers", "Roelink"));
    }

    /** "First Last", "_" for no first name */
    private static Person person(String name) {
        String[] parts = name.split(" ");
        Person person = new Person();
        if (!parts[0].equals("_")) {
            person.setFirstName(parts[0]);
        }
        person.setLastName(parts[parts.length - 1]);
        return person;
    }

    private static List<Person> block(String... names) {
        List<Person> block = new ArrayList<>();
        for (String name : names) {
            block.add(person(name));
        }
        return block;
    }

    @SafeVarargs
    private static List<List<Person>> blocks(List<Person>... blocks) {
        return Arrays.asList(blocks);
    }

    private static List<String> surnames(List<Person> persons) {
        List<String> result = new ArrayList<>();
        for (Person person : persons) {
            result.add(person.getLastName());
        }
        return result;
    }
}
