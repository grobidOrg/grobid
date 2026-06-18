package org.grobid.core.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class TableTest {

    @Test
    public void testIsCompleteForTEI_withHeaderAndCaption_shouldBeComplete() {
        Table table = new Table();
        table.appendHeader("Table 1");
        table.appendCaption("Some caption");

        assertThat(table.isCompleteForTEI(), is(true));
    }

    @Test
    public void testIsCompleteForTEI_withoutHeaderOrCaption_shouldNotBeComplete() {
        Table table = new Table();

        assertThat(table.isCompleteForTEI(), is(false));
    }

    @Test
    public void testIsCompleteForTEI_withHeaderOnly_shouldNotBeComplete() {
        Table table = new Table();
        table.appendHeader("Table 1");

        assertThat(table.isCompleteForTEI(), is(false));
    }

    @Test
    public void testIsCompleteForTEI_fromTypedArea_shouldBeCompleteWithoutHeaderOrCaption() {
        // A user-provided typed area carries only the table grid; it must survive TEI serialization
        // even though the sequence-labelling model did not produce a head/caption.
        Table table = new Table();
        table.setFromTypedArea(true);

        assertThat(table.isCompleteForTEI(), is(true));
    }
}
