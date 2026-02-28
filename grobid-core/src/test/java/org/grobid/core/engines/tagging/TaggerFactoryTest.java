package org.grobid.core.engines.tagging;

import org.grobid.core.GrobidModels;
import org.grobid.core.main.LibraryLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TaggerFactoryTest {

    @Before
    public void setUp() throws Exception {
        LibraryLoader.load();

        Whitebox.setInternalState(TaggerFactory.class, "cache", new HashMap<>());
    }

    @After
    public void tearDown() throws Exception {
        Whitebox.setInternalState(TaggerFactory.class, "cache", new HashMap<>());
    }


    @Test
    public void testGetTagger_shouldReturnDummyTagger() {
        GenericTagger tagger = TaggerFactory.getTagger(GrobidModels.DUMMY);

        assertThat(tagger instanceof DummyTagger, is(true));
    }

    @Ignore("Requires JEP/DeLFT environment")
    @Test
    public void testGetDelftTagger_existingModel_shouldReturn() {
        GenericTagger tagger = TaggerFactory.getTagger(GrobidModels.HEADER, GrobidCRFEngine.DELFT);

        assertThat(tagger instanceof DeLFTTagger, is(true));
    }

    @Test
    public void testGetWapitiTagger_existingModel_shouldReturn() {
        GenericTagger tagger = TaggerFactory.getTagger(GrobidModels.DATE, GrobidCRFEngine.WAPITI);

        assertThat(tagger instanceof WapitiTagger, is(true));
    }

}
