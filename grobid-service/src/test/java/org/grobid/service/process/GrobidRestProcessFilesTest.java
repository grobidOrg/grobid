package org.grobid.service.process;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.visualization.BlockVisualizer;
import org.grobid.core.visualization.CitationsVisualizer;
import org.grobid.core.visualization.FigureTableVisualizer;
import org.grobid.service.util.GrobidRestUtils;

public class GrobidRestProcessFilesTest {

    DocumentSource documentSourceMock;
    GrobidRestProcessFiles target;

    @Before
    public void setUp() {
        documentSourceMock = mock(DocumentSource.class);
        target = new GrobidRestProcessFiles();
    }

    @Test
    public void dispatchProcessing_selectionCitation_shouldWork() throws Exception {
        try (MockedStatic<CitationsVisualizer> citationsVisualizer = Mockito.mockStatic(CitationsVisualizer.class)) {
            citationsVisualizer.when(
                    () -> CitationsVisualizer.annotatePdfWithCitations(
                            nullable(PDDocument.class),
                            nullable(Document.class),
                            nullable(List.class)))
                    .thenReturn(null);

            target.dispatchProcessing(
                    GrobidRestUtils.Annotation.CITATION,
                    null,
                    null,
                    null);

            citationsVisualizer.verify(
                    () -> CitationsVisualizer.annotatePdfWithCitations(
                            nullable(PDDocument.class),
                            nullable(Document.class),
                            nullable(List.class)));
        }
    }

    @Test
    public void dispatchProcessing_selectionBlock_shouldWork() throws Exception {
        try (MockedStatic<BlockVisualizer> blockVisualizer = Mockito.mockStatic(BlockVisualizer.class)) {
            blockVisualizer.when(
                    () -> BlockVisualizer.annotateBlocks(
                            nullable(PDDocument.class),
                            nullable(File.class),
                            nullable(Document.class),
                            anyBoolean(),
                            anyBoolean(),
                            anyBoolean()))
                    .thenReturn(null);

            File fakeFile = File.createTempFile("justForTheTest", "baomiao");
            fakeFile.deleteOnExit();
            when(documentSourceMock.getXmlFile()).thenReturn(fakeFile);

            target.dispatchProcessing(
                    GrobidRestUtils.Annotation.BLOCK,
                    null,
                    documentSourceMock,
                    null);

            blockVisualizer.verify(
                    () -> BlockVisualizer.annotateBlocks(
                            nullable(PDDocument.class),
                            nullable(File.class),
                            nullable(Document.class),
                            anyBoolean(),
                            anyBoolean(),
                            anyBoolean()));
            Mockito.verify(documentSourceMock).getXmlFile();
        }
    }

    @Test
    public void dispatchProcessing_selectionFigure_shouldWork() throws Exception {
        try (MockedStatic<FigureTableVisualizer> figureTableVisualizer = Mockito
                .mockStatic(FigureTableVisualizer.class)) {
            File fakeFile = File.createTempFile("justForTheTest", "baomiao");
            fakeFile.deleteOnExit();
            figureTableVisualizer.when(
                    () -> FigureTableVisualizer.annotateFigureAndTables(
                            nullable(PDDocument.class),
                            nullable(File.class),
                            nullable(Document.class),
                            anyBoolean(),
                            anyBoolean(),
                            anyBoolean(),
                            anyBoolean(),
                            anyBoolean()))
                    .thenReturn(null);
            when(documentSourceMock.getXmlFile()).thenReturn(fakeFile);

            target.dispatchProcessing(
                    GrobidRestUtils.Annotation.FIGURE,
                    null,
                    documentSourceMock,
                    null);

            figureTableVisualizer.verify(
                    () -> FigureTableVisualizer.annotateFigureAndTables(
                            nullable(PDDocument.class),
                            nullable(File.class),
                            nullable(Document.class),
                            anyBoolean(),
                            anyBoolean(),
                            anyBoolean(),
                            anyBoolean(),
                            anyBoolean()));
            Mockito.verify(documentSourceMock).getXmlFile();
        }
    }
}
