package org.grobid.core.test;

import org.grobid.core.data.BibDataSet;

import java.io.File;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class TestReferencesParser extends EngineTest {

    public static final String TEST_RESOURCES_PATH = "./src/test/resources/test";

    // @Test
    public void testReferences() throws Exception {
        String testPath = TEST_RESOURCES_PATH;

        String pdfPath = testPath + File.separator + "Wang-paperAVE2008.pdf";
        List<BibDataSet> resRefs = engine.processReferences(new File(pdfPath), 1);

        assertNotNull(resRefs);
        assertThat(resRefs.size(), is(12));
    }

}
