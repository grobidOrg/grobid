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
package org.grobid.core.utilities;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.data.BiblioItem;

public class ConsolidationTest {

    @BeforeClass
    public static void setInitialContext() throws Exception {
        GrobidProperties.getInstance();
    }

    /**
     * Diacritics in the author surname and title must reach the consolidation query
     * arguments unchanged. Transliterating e.g. "Dörre" -> "Doerre" used to break
     * Crossref matching (issue #271). The current field-extraction path performs no
     * such folding; this test locks that behaviour in.
     */
    @Test
    public void testExtractFields_preservesDiacritics_issue271() {
        BiblioItem bibl = new BiblioItem();
        bibl.setAuthors("Dörre");
        bibl.setTitle("Über die Wärmeleitung");

        Consolidation.BibQueryFields fields = Consolidation.extractFieldsFromBiblioItem(bibl);

        assertThat(fields.author, is("Dörre"));
        assertThat(fields.title, is("Über die Wärmeleitung"));
    }
}
