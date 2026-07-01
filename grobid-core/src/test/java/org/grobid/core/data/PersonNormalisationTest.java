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
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class PersonNormalisationTest {
    Person target;

    @Test
    public void testAgglitinatedInitialsNormalisation1() {
        target = new Person();
        target.setFirstName("OJ");
        target.setLastName("Simpson");
        target.normalizeName();
        assertThat(target.getFirstName(), is("O"));
        assertThat(target.getMiddleName(), is("J"));
        assertThat(target.getLastName(), is("Simpson"));
    }

    @Test
    public void testCrossRefNameNormlisation1() {
        target = new Person();
        target.setFirstName("M. L.");
        target.normalizeCrossRefFirstName();
        assertThat(target.getFirstName(), is("M"));
        assertThat(target.getMiddleName(), is("L"));
    }

    @Test
    public void testCrossRefNameNormlisation2() {
        target = new Person();
        target.setFirstName("L.S.");
        target.normalizeCrossRefFirstName();
        assertThat(target.getFirstName(), is("L"));
        assertThat(target.getMiddleName(), is("S"));
    }

    @Test
    public void testCrossRefNameNormlisation3() {
        target = new Person();
        target.setFirstName("Nicholas J.");
        target.normalizeCrossRefFirstName();
        assertThat(target.getFirstName(), is("Nicholas"));
        assertThat(target.getMiddleName(), is("J"));
    }

    @Test
    public void testCrossRefNameNormlisation4() {
        target = new Person();
        target.setFirstName("John W.S.");
        target.normalizeCrossRefFirstName();
        assertThat(target.getFirstName(), is("John"));
        assertThat(target.getMiddleName(), is("W S"));
    }

    @Test
    public void testCrossRefNameNormlisation5() {
        target = new Person();
        target.setFirstName("John W. S.");
        target.normalizeCrossRefFirstName();
        assertThat(target.getFirstName(), is("John"));
        assertThat(target.getMiddleName(), is("W S"));
    }

    @Test
    public void testCrossRefNameNormlisation6() {
        target = new Person();
        target.setFirstName("G. Arjen");
        target.normalizeCrossRefFirstName();
        assertThat(target.getFirstName(), is("G"));
        assertThat(target.getMiddleName(), is("Arjen"));
    }

    @Test
    public void testCrossRefNameNormlisation7() {
        target = new Person();
        target.setFirstName("HermanHG");
        target.normalizeCrossRefFirstName();
        assertThat(target.getFirstName(), is("Herman"));
        assertThat(target.getMiddleName(), is("H G"));
    }

    @Test
    public void testCrossRefNameNormlisation8() {
        target = new Person();
        target.setFirstName("HaHGP");
        target.normalizeCrossRefFirstName();
        assertThat(target.getFirstName(), is("Ha"));
        assertThat(target.getMiddleName(), is("H G P"));
    }

    @Test
    public void testCrossRefNameNormlisation9() {
        target = new Person();
        target.setFirstName("HaP");
        target.normalizeCrossRefFirstName();
        assertThat(target.getFirstName(), is("Ha"));
        assertThat(target.getMiddleName(), is("P"));
    }

    @Test
    public void testCrossRefNameNormlisation10() {
        target = new Person();
        target.setFirstName("Zs.");
        target.setLastName("Biró");
        target.normalizeCrossRefFirstName();
        assertThat(target.getFirstName(), is("Z"));
        assertThat(target.getMiddleName(), is("S"));
        assertThat(target.getLastName(), is("Biró"));
    }

    @Test
    public void testCrossRefNameNormlisation11() {
        target = new Person();
        target.setFirstName("J.-L.");
        target.normalizeCrossRefFirstName();
        assertThat(target.getFirstName(), is("J-L"));
        assertThat(target.getMiddleName(), is(nullValue()));
    }

    @Test
    public void testCrossRefNameNormlisation12() {
        target = new Person();
        target.setFirstName("J-L.");
        target.normalizeCrossRefFirstName();
        assertThat(target.getFirstName(), is("J-L"));
        assertThat(target.getMiddleName(), is(nullValue()));
    }

}
