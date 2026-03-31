package org.grobid.core.test;

import org.apache.commons.io.FileUtils;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentPiece;
import org.grobid.core.document.DocumentPointer;
import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.layout.Block;
import org.grobid.core.utilities.GrobidProperties;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

import nu.xom.Element;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestFullTextParser extends EngineTest {

    @BeforeClass
    public static void init() {
        GrobidProperties.getInstance();
    }

    @AfterClass
    public static void tearDown(){
        GrobidFactory.reset();
    }

    @Test
    public void testFullTextParser_1() throws Exception {
        File inputTmpFile = getInputDocument("/test/Wang-paperAVE2008.pdf");

        Document tei = engine.fullTextToTEIDoc(inputTmpFile, GrobidAnalysisConfig.defaultInstance());
        assertTei(tei);
    }

    private File getInputDocument(String inputPath) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(inputPath);
        File inputTmpFile  = File.createTempFile("tmpFileTest", "testFullTextParser");
        inputTmpFile.deleteOnExit();

        FileUtils.copyToFile(is, inputTmpFile);

        return inputTmpFile;
    }

    @Test
    public void testFullTextParser_2() throws Exception {
        File inputTmpFile = getInputDocument("/test/two_pages.pdf");

        Document tei = engine.fullTextToTEIDoc(inputTmpFile, GrobidAnalysisConfig.defaultInstance());
        assertTei(tei);
    }

    @Test
    public void testFullTextParser_3() throws Exception {
        File inputTmpFile = getInputDocument("/test/MullenJSSv18i03.pdf");
        Document tei = engine.fullTextToTEIDoc(inputTmpFile, GrobidAnalysisConfig.defaultInstance());
        assertTei(tei);
    }

    @Test
    public void testFullTextParser_4() throws Exception {
        File inputTmpFile = getInputDocument("/test/1001._0908.0054.pdf");
        Document tei = engine.fullTextToTEIDoc(inputTmpFile, GrobidAnalysisConfig.defaultInstance());
        assertTei(tei);
    }

    @Test
    public void testFullTextParser_5() throws Exception {
        File inputTmpFile = getInputDocument("/test/submission_161.pdf");
        Document tei = engine.fullTextToTEIDoc(inputTmpFile, GrobidAnalysisConfig.defaultInstance());
        assertTei(tei);
    }

    @Test
    public void testFullTextParser_6() throws Exception {
        File inputTmpFile = getInputDocument("/test/submission_363.pdf");
        Document tei = engine.fullTextToTEIDoc(inputTmpFile, GrobidAnalysisConfig.defaultInstance());
        assertTei(tei);
    }

    private void assertTei(Document doc) {
        assertDocAndBlockTokenizationSync(doc);
        assertNotNull(doc.getTei());

        //check that XML is valid
        XmlBuilderUtils.fromString(doc.getTei());
    }

    private void assertDocAndBlockTokenizationSync(Document doc) {
        List<Block> blocks = doc.getBlocks();

        for (Block block : blocks) {
            if (block.getNbTokens() == 0)
                continue;

            int start = block.getStartToken();
            int end = block.getEndToken();

            if (start == -1 || start == end) {
                continue;
            }

//            for (int i = start; i < end; i++) {
            for (int i = start; i <= end; i++) {
                //assertEquals(doc.getTokenizations().get(i).getText(), block.getTokens().get(i - start).getText());
                assertEquals(doc.getTokenizations().get(i), block.getTokens().get(i - start));
            }
//            assertTrue(endPtr.getTokenBlockPos() < endBlock.getTokens().size());
        }

        for (TaggingLabel l : Arrays.asList(SegmentationLabels.BODY, SegmentationLabels.REFERENCES, SegmentationLabels.HEADER, SegmentationLabels.ACKNOWLEDGEMENT, SegmentationLabels.ANNEX,
            SegmentationLabels.FOOTNOTE, SegmentationLabels.HEADNOTE, SegmentationLabels.TOC)) {
            SortedSet<DocumentPiece> parts = doc.getDocumentPart(l);
            if (parts == null) {
                continue;
            }
            for (DocumentPiece p : parts) {
                DocumentPointer startPtr = p.getLeft();
                DocumentPointer endPtr = p.getRight();

                Block endBlock = doc.getBlocks().get(endPtr.getBlockPtr());
                assertTrue(endPtr.getTokenBlockPos() < endBlock.getTokens().size());
            }
        }

    }

}
