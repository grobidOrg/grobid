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
import static org.junit.Assert.*;

import org.junit.Test;

public class GenericTaggerUtilsTest {

    @Test
    public void testGetPlainLabel_normalValue() throws Exception {
        assertThat(GenericTaggerUtils.getPlainLabel("<status>"), is("<status>"));
    }

    @Test
    public void testGetPlainLabel_startingValue() throws Exception {
        assertThat(GenericTaggerUtils.getPlainLabel("I-<status>"), is("<status>"));
    }

    @Test
    public void testGetPlainLabel_I_startingValue() throws Exception {
        assertThat(GenericTaggerUtils.getPlainLabel("I-<status>"), is("<status>"));
    }

    @Test
    public void testGetPlainLabel_nullValue() throws Exception {
        assertNull(GenericTaggerUtils.getPlainLabel(null));
    }

    @Test
    public void testIsBeginningOfEntity_true() throws Exception {
        assertTrue(GenericTaggerUtils.isBeginningOfEntity("I-<status>"));
    }

    @Test
    public void testIsBeginningOfEntity_false() throws Exception {
        assertFalse(GenericTaggerUtils.isBeginningOfEntity("<status>"));
    }

    @Test
    public void testIsBeginningOfEntity_false2() throws Exception {
        assertFalse(GenericTaggerUtils.isBeginningOfEntity("<I-status>"));
    }

    @Test
    public void testIsBeginningOfIOBEntity_B_true() throws Exception {
        assertTrue(GenericTaggerUtils.isBeginningOfIOBEntity("B-<status>"));
    }

    @Test
    public void testIsBeginningOfEntity_B_false2() throws Exception {
        assertFalse(GenericTaggerUtils.isBeginningOfEntity("<B-status>"));
    }
}
