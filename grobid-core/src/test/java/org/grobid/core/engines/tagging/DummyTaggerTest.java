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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.GrobidProperties;

public class DummyTaggerTest {
    GenericTagger target;

    @BeforeClass
    public static void init() {
        GrobidProperties.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        target = TaggerFactory.getTagger(GrobidModels.DUMMY);
    }

    @Test
    public void testDummyTagger_shouldReturnDummyLabel() {
        assertThat(target.label("bao"), is("<dummy>"));
    }

    @Test
    public void testDummyTagger() {
        assertThat(
                target.label(Arrays.asList("bao", "miao", "ciao")),
                is(equalTo("bao\t<dummy>\nmiao\t<dummy>\nciao\t<dummy>")));
    }

    @Test(expected = GrobidException.class)
    public void testWrongModelInitialisation_shouldThrowException() {
        target = new DummyTagger(GrobidModels.HEADER);
    }
}
