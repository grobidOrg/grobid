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
