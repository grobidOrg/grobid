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

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.grobid.core.data.BiblioItem;
import org.grobid.core.main.LibraryLoader;

public class ConsolidationIntegrationTest {

    private Consolidation target = null;

    public static String[] DOIs = {
            "10.1086/107043",
            "10.1086/102351",
            "10.1086/100853",
            "10.1086/105172"
    };

    @Before
    public void setUp() {
        LibraryLoader.load();
        GrobidProperties.getInstance();

        target = Consolidation.getInstance();
    }

    @Test
    @Ignore("Crossref API not reliable enough")
    public void testConsolidationDOISimple() throws Exception {
        BiblioItem biblio = new BiblioItem();
        biblio.setDOI(DOIs[0]);
        BiblioItem bib = target.consolidate(biblio, null, 1);
        boolean found = false;
        if (bib != null)
            found = true;
        assertEquals("The consolidation has not the expected outcome", true, found);

    }

}
