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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class NoteTest {

    @Test
    public void testGetName_shouldBeMargin() throws Exception {
        Note note = new Note();
        note.setNoteType(Note.NoteType.MARGIN);

        assertThat(note.getNoteTypeName(), is("margin"));
    }

    @Test
    public void testGetName_shouldBeFoot() throws Exception {
        Note note = new Note();
        note.setNoteType(Note.NoteType.FOOT);

        assertThat(note.getNoteTypeName(), is("foot"));
    }

}
