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
package org.grobid.core.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PersonMatchingTest {

    @Test
    public void testFirstNamesCompatible_blankCandidate() {
        assertTrue(Person.firstNamesCompatible(person("John", null, "Smith"), person(null, null, "Smith")));
    }

    @Test
    public void testFirstNamesCompatible_blankReference() {
        assertFalse(Person.firstNamesCompatible(person(null, null, "Smith"), person("John", null, "Smith")));
    }

    @Test
    public void testFirstNamesCompatible_initialOnEitherSide() {
        assertTrue(Person.firstNamesCompatible(person("J", null, "Smith"), person("John", null, "Smith")));
        assertTrue(Person.firstNamesCompatible(person("John", null, "Smith"), person("J", null, "Smith")));
    }

    @Test
    public void testFirstNamesCompatible_caseAndAccentInsensitive() {
        assertTrue(Person.firstNamesCompatible(person("JOSÉ", null, "Garcia"), person("Jose", null, "Garcia")));
    }

    @Test
    public void testFirstNamesCompatible_middleNamesAndPartialForenames() {
        assertTrue(Person.firstNamesCompatible(person("John", "Andrew", "Smith"), person("J", "A", "Smith")));
        assertTrue(Person.firstNamesCompatible(person("Richard", "F", "Oppong"), person("F", null, "Oppong")));
        assertTrue(Person.firstNamesCompatible(person("Lei", null, "Ding"), person("X Lei", null, "Ding")));
        assertTrue(Person.firstNamesCompatible(person("Jean-Pierre", null, "Dupont"), person("J.-P.", null, "Dupont")));
    }

    @Test
    public void testFirstNamesCompatible_differentPersons() {
        assertFalse(Person.firstNamesCompatible(person("Jane", null, "Smith"), person("John", null, "Smith")));
        assertFalse(Person.firstNamesCompatible(person("Jon", null, "Smith"), person("John", null, "Smith")));
        assertFalse(Person.firstNamesCompatible(person("John", "Andrew", "Smith"), person("A", "J", "Smith")));
        assertFalse(Person.firstNamesCompatible(person("Ji", "Young", "Lee"), person("Ji", "Youl", "Lee")));
    }

    @Test
    public void testIsSamePerson_softSurname() {
        assertTrue(Person.isSamePerson(person("Carina", null, "Jagers"), person("Carina", null, "Jagers1")));
        assertTrue(Person.isSamePerson(person("Maria", null, "Martín"), person("Maria", null, "MARTIN")));
    }

    @Test
    public void testIsSamePerson_differentPersons() {
        assertFalse(Person.isSamePerson(person("John", null, "Smith"), person("John", null, "Johnson")));
        assertFalse(Person.isSamePerson(person("Jane", null, "Smithe"), person("John", null, "Smith")));
        assertFalse(Person.isSamePerson(person("John", null, null), person("John", null, "Smith")));
    }

    private static Person person(String first, String middle, String last) {
        Person person = new Person();
        person.setFirstName(first);
        person.setMiddleName(middle);
        person.setLastName(last);
        return person;
    }
}
