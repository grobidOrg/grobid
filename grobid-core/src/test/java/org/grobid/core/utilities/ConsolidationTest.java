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
