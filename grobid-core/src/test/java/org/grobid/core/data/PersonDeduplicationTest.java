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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class PersonDeduplicationTest {
    Person target;

    @Test
    public void testDeduplication0() {
        // test nothing to deduplicate
        target = new Person();
        target.setFirstName("OJ");
        target.setLastName("Simpson");
        target.normalizeName();
        List<Person> persons = new ArrayList<Person>();
        persons.add(target);
        target.deduplicate(persons);
        assertThat(persons.size(), is(1));
    }

    @Test
    public void testDeduplication1() {
        // test simple deduplication, removal of second
        target = new Person();
        target.setFirstName("OJ");
        target.setLastName("Simpson");
        target.normalizeName();

        Person other = new Person();
        other.setFirstName("O");
        other.setLastName("Simpson");
        other.normalizeName();

        List<Person> persons = new ArrayList<Person>();
        persons.add(target);
        persons.add(other);

        target.deduplicate(persons);
        assertThat(persons.size(), is(1));
        assertThat(persons.get(0).getFirstName(), is("O"));
        assertThat(persons.get(0).getMiddleName(), is("J"));
        assertThat(persons.get(0).getLastName(), is("Simpson"));
    }

    @Test
    public void testDeduplication2() {
        // test simple deduplication, removal of first
        target = new Person();
        target.setFirstName("O");
        target.setLastName("Simpson");
        target.normalizeName();

        Person other = new Person();
        other.setFirstName("O");
        other.setMiddleName("J");
        other.setLastName("Simpson");
        other.normalizeName();

        List<Person> persons = new ArrayList<Person>();
        persons.add(target);
        persons.add(other);

        target.deduplicate(persons);
        assertThat(persons.size(), is(1));
        assertThat(persons.get(0).getFirstName(), is("O"));
        assertThat(persons.get(0).getMiddleName(), is("J"));
        assertThat(persons.get(0).getLastName(), is("Simpson"));
    }

    @Test
    public void testDeduplication3() {
        // test less simple deduplication, keep most detailed firstname
        target = new Person();
        target.setFirstName("O");
        target.setMiddleName("J");
        target.setLastName("Simpson");
        target.normalizeName();

        Person other = new Person();
        other.setFirstName("Orenthal");
        other.setMiddleName("James");
        other.setLastName("Simpson");
        other.normalizeName();

        List<Person> persons = new ArrayList<Person>();
        persons.add(target);
        persons.add(other);

        target.deduplicate(persons);
        assertThat(persons.size(), is(1));
        assertThat(persons.get(0).getFirstName(), is("Orenthal"));
        assertThat(persons.get(0).getMiddleName(), is("James"));
        assertThat(persons.get(0).getLastName(), is("Simpson"));
    }

    @Test
    public void testDeduplication4() {
        // test less simple deduplication, keep most detailed firstname
        target = new Person();
        target.setFirstName("Orenthal");
        target.setMiddleName("J");
        target.setLastName("Simpson");
        target.normalizeName();

        Person other = new Person();
        other.setFirstName("Orenthal");
        other.setMiddleName("James");
        other.setLastName("Simpson");
        other.normalizeName();

        List<Person> persons = new ArrayList<Person>();
        persons.add(target);
        persons.add(other);

        target.deduplicate(persons);
        assertThat(persons.size(), is(1));
        assertThat(persons.get(0).getFirstName(), is("Orenthal"));
        assertThat(persons.get(0).getMiddleName(), is("James"));
        assertThat(persons.get(0).getLastName(), is("Simpson"));
    }

    @Test
    public void testDeduplication5() {
        // test deduplication with more duplicated guys
        target = new Person();
        target.setFirstName("O");
        target.setMiddleName("J");
        target.setLastName("Simpson");
        target.normalizeName();

        Person other = new Person();
        other.setFirstName("Orenthal");
        other.setMiddleName("James");
        other.setLastName("Simpson");
        other.normalizeName();

        Person other2 = new Person();
        other2.setFirstName("Orenthal");
        other2.setLastName("Simpson");
        other2.normalizeName();

        Person other3 = new Person();
        other3.setFirstName("O");
        other3.setLastName("Simpson");
        other3.normalizeName();

        Person other4 = new Person();
        other4.setFirstName("Orenthal");
        other4.setMiddleName("J");
        other4.setLastName("Simpson");
        other4.normalizeName();

        List<Person> persons = new ArrayList<Person>();
        persons.add(target);
        persons.add(other);
        persons.add(other2);
        persons.add(other3);
        persons.add(other4);

        target.deduplicate(persons);
        assertThat(persons.size(), is(1));
        assertThat(persons.get(0).getFirstName(), is("Orenthal"));
        assertThat(persons.get(0).getMiddleName(), is("James"));
        assertThat(persons.get(0).getLastName(), is("Simpson"));
    }

    @Test
    public void testDeduplication6() {
        // test deduplication with affiliation to be kept from other guy
        target = new Person();
        target.setFirstName("O");
        target.setMiddleName("J");
        target.setLastName("Simpson");
        target.normalizeName();

        Person other = new Person();
        other.setFirstName("O");
        other.setLastName("Simpson");
        other.normalizeName();
        Affiliation aff = new Affiliation();
        aff.addInstitution("National Football League");
        other.addAffiliation(aff);

        List<Person> persons = new ArrayList<Person>();
        persons.add(target);
        persons.add(other);

        target.deduplicate(persons);
        assertThat(persons.size(), is(1));
        assertThat(persons.get(0).getFirstName(), is("O"));
        assertThat(persons.get(0).getMiddleName(), is("J"));
        assertThat(persons.get(0).getLastName(), is("Simpson"));
        assertThat(persons.get(0).getAffiliations(), notNullValue());
    }

    @Test
    public void testDeduplication7() {
        // test no deduplication, middlename clashing
        target = new Person();
        target.setFirstName("O");
        target.setMiddleName("J");
        target.setLastName("Simpson");
        target.normalizeName();

        Person other = new Person();
        other.setFirstName("O");
        other.setMiddleName("P");
        other.setLastName("Simpson");
        other.normalizeName();

        List<Person> persons = new ArrayList<Person>();
        persons.add(target);
        persons.add(other);

        target.deduplicate(persons);
        assertThat(persons.size(), is(2));
    }

}
