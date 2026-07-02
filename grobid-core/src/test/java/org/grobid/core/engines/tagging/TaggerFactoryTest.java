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
package org.grobid.core.engines.tagging;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.grobid.core.GrobidModels;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidTestUtils;

public class TaggerFactoryTest {

    @Before
    public void setUp() throws Exception {
        LibraryLoader.load();

        GrobidTestUtils.setStaticField(TaggerFactory.class, "cache", new HashMap<>());
    }

    @After
    public void tearDown() throws Exception {
        GrobidTestUtils.setStaticField(TaggerFactory.class, "cache", new HashMap<>());
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
