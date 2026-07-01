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

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.grobid.core.data.util.AuthorEmailAssigner;
import org.grobid.core.data.util.ClassicAuthorEmailAssigner;

public class AuthorEmailAssignerTest {

    @Test
    public void testEmailAssignment() {
        AuthorEmailAssigner assigner = new ClassicAuthorEmailAssigner();

        List<Person> authors = l(
                p("Jalal Al-Muhtadi"),
                p("Manish Anand"),
                p("M.", "Dennis Mickunas"),
                p("Roy Campbell"));
        assigner.assign(
                authors,
                l("almuhtad@uiuc.edu", "manand@uiuc.edu", "mickunas@uiuc.edu", "rhc@uiuc.edu"));

        System.out.println(authors);

    }

    private Person p(String name) {
        String[] split = name.split(" ");
        return p(split[0], split[1]);
    }

    private Person p(String fistName, String lastName) {
        Person p = new Person();
        p.setFirstName(fistName.trim());
        p.setLastName(lastName.trim());
        return p;
    }

    public static <T> List<T> l(T... els) {
        return Arrays.asList(els);
    }

}
