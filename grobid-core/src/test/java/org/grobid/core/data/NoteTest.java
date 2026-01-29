package org.grobid.core.data;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
