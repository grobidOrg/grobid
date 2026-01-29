package org.grobid.core.utilities;

import org.grobid.core.exceptions.GrobidPropertyException;
import org.grobid.core.main.GrobidHomeFinder;
import org.junit.Test;
import org.junit.After;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertTrue;

/**
 * Testing location of grobid home
 */
public class GrobidHomeFinderTest {

    @After
    public void tearDown() throws Exception {
        GrobidProperties.reset();
    }

    @Test
    public void testDefault() {
        GrobidProperties.getInstance();
    }

    /*
     * @Test public void testViaProp() { System.setProperty(GrobidPropertyKeys.PROP_GROBID_HOME, "../grobid-home");
     * assertPath(new GrobidHomeFinder(Collections.<String>emptyList()).findGrobidHomeOrFail()); }
     */

    @Test(expected = GrobidPropertyException.class)
    public void testNoDefaultLocations() {
        assertPath(new GrobidHomeFinder(Collections.<String>emptyList()).findGrobidHomeOrFail());
    }

    private void assertPath(File p) {
        assertTrue("Not exists or not a directory " + p, p.exists() && p.isDirectory());
    }

}
