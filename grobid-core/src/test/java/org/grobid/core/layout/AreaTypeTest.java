package org.grobid.core.layout;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AreaTypeTest {

    @Test
    public void testFromString_figure() {
        AreaType type = AreaType.fromString("figure");
        assertThat(type, is(AreaType.FIGURE));
    }

    @Test
    public void testFromString_table() {
        AreaType type = AreaType.fromString("table");
        assertThat(type, is(AreaType.TABLE));
    }

    @Test
    public void testFromString_ignore() {
        AreaType type = AreaType.fromString("ignore");
        assertThat(type, is(AreaType.IGNORE));
    }

    @Test
    public void testFromString_paratext() {
        AreaType type = AreaType.fromString("paratext");
        assertThat(type, is(AreaType.PARATEXT));
    }

    @Test
    public void testFromString_caseInsensitive() {
        assertThat(AreaType.fromString("FIGURE"), is(AreaType.FIGURE));
        assertThat(AreaType.fromString("TABLE"), is(AreaType.TABLE));
        assertThat(AreaType.fromString("IGNORE"), is(AreaType.IGNORE));
        assertThat(AreaType.fromString("PARATEXT"), is(AreaType.PARATEXT));
        assertThat(AreaType.fromString("Figure"), is(AreaType.FIGURE));
        assertThat(AreaType.fromString("Paratext"), is(AreaType.PARATEXT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromString_invalidType() {
        AreaType.fromString("invalid");
    }

    @Test
    public void testGetValue() {
        assertThat(AreaType.FIGURE.getValue(), is("figure"));
        assertThat(AreaType.TABLE.getValue(), is("table"));
        assertThat(AreaType.IGNORE.getValue(), is("ignore"));
        assertThat(AreaType.PARATEXT.getValue(), is("paratext"));
    }
}
