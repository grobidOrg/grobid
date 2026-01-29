package org.grobid.service.process;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.easymock.EasyMock;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.visualization.BlockVisualizer;
import org.grobid.core.visualization.CitationsVisualizer;
import org.grobid.core.visualization.FigureTableVisualizer;
import org.grobid.service.util.GrobidRestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.List;

import static org.easymock.EasyMock.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CitationsVisualizer.class, BlockVisualizer.class, FigureTableVisualizer.class})
public class GrobidRestProcessFilesTest {

    // static {
    // JerseyGuiceUtils.install((s, serviceLocator) -> null);
    // }

    DocumentSource documentSourceMock;
    GrobidRestProcessFiles target;

    @Before
    public void setUp() {
        documentSourceMock = createMock(DocumentSource.class);
        target = new GrobidRestProcessFiles();
    }

    @Test
    public void dispatchProcessing_selectionCitation_shouldWork() throws Exception {
        PowerMock.mockStatic(CitationsVisualizer.class);

        expect(
                CitationsVisualizer.annotatePdfWithCitations(
                        anyObject(PDDocument.class),
                        anyObject(Document.class),
                        anyObject(List.class)))
                .andReturn(null);

        PowerMock.replay(CitationsVisualizer.class);

        target.dispatchProcessing(GrobidRestUtils.Annotation.CITATION, null, null, null);

        PowerMock.verify(CitationsVisualizer.class);
    }

    @Test
    public void dispatchProcessing_selectionBlock_shouldWork() throws Exception {
        PowerMock.mockStatic(BlockVisualizer.class);

        expect(
                BlockVisualizer.annotateBlocks(
                        (PDDocument) anyObject(),
                        EasyMock.<File>anyObject(),
                        EasyMock.<Document>anyObject(),
                        anyBoolean(),
                        anyBoolean(),
                        anyBoolean()))
                .andReturn(null);

        File fakeFile = File.createTempFile("justForTheTest", "baomiao");
        fakeFile.deleteOnExit();
        expect(documentSourceMock.getXmlFile()).andReturn(fakeFile);

        PowerMock.replay(BlockVisualizer.class);
        replay(documentSourceMock);

        target.dispatchProcessing(GrobidRestUtils.Annotation.BLOCK, null, documentSourceMock, null);

        PowerMock.verify(BlockVisualizer.class);
        verify(documentSourceMock);
    }

    @Test
    public void dispatchProcessing_selectionFigure_shouldWork() throws Exception {
        PowerMock.mockStatic(FigureTableVisualizer.class);

        File fakeFile = File.createTempFile("justForTheTest", "baomiao");
        fakeFile.deleteOnExit();
        expect(
                FigureTableVisualizer.annotateFigureAndTables(
                        anyObject(),
                        EasyMock.anyObject(),
                        EasyMock.anyObject(),
                        anyBoolean(),
                        anyBoolean(),
                        anyBoolean(),
                        anyBoolean(),
                        anyBoolean()))
                .andReturn(null);
        expect(documentSourceMock.getXmlFile()).andReturn(fakeFile);

        PowerMock.replay(FigureTableVisualizer.class);
        replay(documentSourceMock);

        target.dispatchProcessing(GrobidRestUtils.Annotation.FIGURE, null, documentSourceMock, null);

        PowerMock.verify(FigureTableVisualizer.class);
        verify(documentSourceMock);
    }
}
