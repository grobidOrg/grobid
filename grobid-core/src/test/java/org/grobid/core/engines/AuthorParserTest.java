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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

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
}
