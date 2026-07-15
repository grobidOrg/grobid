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
package org.grobid.core.utilities.crossref;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.data.BiblioItem;
import org.grobid.core.main.LibraryLoader;

public class WorkDeserializerTest {

    @BeforeClass
    public static void init() throws Exception {
        LibraryLoader.load();
    }

    /**
     * CrossRef represents collaboration/group authors with a "name" field instead of
     * "given"/"family" (e.g. "ATLAS Collaboration"). Such entries must not become invalid
     * null-named authors: they would inflate the author count and defeat the position-based
     * affiliation merge in BiblioItem.correct.
     */
    @Test
    public void parse_shouldSkipNamelessCollaborationAuthor() throws Exception {
        String json = "{\"author\":["
                + "{\"given\":\"F H\",\"family\":\"Barreiro Megino\",\"sequence\":\"first\"},"
                + "{\"given\":\"M\",\"family\":\"Borodin\",\"sequence\":\"additional\"},"
                + "{\"name\":\"ATLAS Collaboration\",\"sequence\":\"additional\"}"
                + "]}";

        List<BiblioItem> results = new WorkDeserializer().parse(json);

        assertThat(results, hasSize(1));
        BiblioItem biblio = results.get(0);
        // only the two named authors are kept; the "name"-only entry is skipped
        assertThat(biblio.getFullAuthors(), hasSize(2));
        assertThat(biblio.getFullAuthors().get(0).getLastName(), is("Barreiro Megino"));
        assertThat(biblio.getFullAuthors().get(1).getLastName(), is("Borodin"));
    }

    @Test
    public void parse_shouldKeepAllNamedAuthors() throws Exception {
        String json = "{\"author\":["
                + "{\"given\":\"Jane\",\"family\":\"Doe\"},"
                + "{\"given\":\"John\",\"family\":\"Smith\"}"
                + "]}";

        List<BiblioItem> results = new WorkDeserializer().parse(json);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getFullAuthors(), hasSize(2));
    }
}
