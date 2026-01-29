package org.grobid.core.test;

import org.grobid.core.utilities.GrobidProperties;
import org.junit.Ignore;

@Ignore
public class TestEbookParser extends EngineTest {
    private String newTrainingPath = null;

    private void getTestResourcePath() {
        newTrainingPath = GrobidProperties.getInstance().getTempPath().getAbsolutePath();
    }

    // @Test
    public void testEbookParser() throws Exception {
        /*
         * engine = new BookStructureParser();
         *
         * getTestResourcePath();
         *
         * String pdfPath = testPath + "/littleessaysoflo00elliuoft.pdf"; engine.createTrainingFullTextEbook(pdfPath,
         * newTrainingPath, newTrainingPath, 0);
         */
    }
}
