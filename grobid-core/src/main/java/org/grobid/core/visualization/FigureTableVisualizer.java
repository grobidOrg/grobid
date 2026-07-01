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
package org.grobid.core.visualization;

import static org.grobid.core.utilities.PathUtil.getOneFile;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;

import org.grobid.core.GrobidModels;
import org.grobid.core.data.Figure;
import org.grobid.core.data.Table;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.GraphicObject;
import org.grobid.core.layout.GraphicObjectType;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.BoundingBoxCalculator;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.XQueryProcessor;

/**
 * Visualize figures and tables
 */

public class FigureTableVisualizer {

    public static final boolean VISUALIZE_VECTOR_BOXES = true;

    private static Set<Integer> blacklistedPages;
    private static File inputPdf;
    private static boolean annotated;
    private static boolean annotatedFigure;
    static boolean singleFile = true;

    private static Set<Integer> getVectorGraphicPages(File pdfaltoDirectory) throws XPathException, IOException {

        //TODO: temp

        if (true) {
            return new HashSet<>();
        }
        XQueryProcessor xq = new XQueryProcessor(getOneFile(pdfaltoDirectory, ".xml"));
        String query = XQueryProcessor.getQueryFromResources("self-contained-images.xq");

        SequenceIterator it = xq.getSequenceIterator(query);
        Item item;
        Set<Integer> blacklistedPages = new HashSet<>();
        while ((item = it.next()) != null) {
            blacklistedPages.add(Integer.parseInt(item.getStringValue()));
            it.next();
        }
        return blacklistedPages;
    }

    private static void processPdfFile(File input, File outputFolder, GrobidModels.Flavor flavor) throws Exception {
        inputPdf = input;
        annotated = false;
        annotatedFigure = false;
        final PDDocument document = PDDocument.load(input);
        File outPdf = new File("/tmp/testFigures.pdf");

        final Engine engine = setupEngine();

        File contentDir = new File("/tmp/contentDir");
        FileUtils.deleteDirectory(contentDir);

        File assetPath = new File(contentDir, "tei");

        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
                .pdfAssetPath(assetPath)
                .withPreprocessImages(false)
                .generateTeiCoordinates(Lists.newArrayList("figure"))
                .withProcessVectorGraphics(true)
                .build();

        DocumentSource documentSource = DocumentSource.fromPdf(input, -1, -1, true, false, false);

        File pdfaltoDirectory = new File(contentDir, "pdfalto");
        pdfaltoDirectory.mkdirs();
        FileUtils.copyFileToDirectory(input, contentDir);
        File copiedFile = new File(pdfaltoDirectory, "input.xml");
        FileUtils.copyFile(documentSource.getXmlFile(), copiedFile);
        FileUtils.copyDirectory(
                new File(documentSource.getXmlFile().getAbsolutePath() + "_data"),
                new File(pdfaltoDirectory, documentSource.getXmlFile().getName() + "_data"));

        System.out.println(documentSource.getXmlFile());

        blacklistedPages = getVectorGraphicPages(pdfaltoDirectory);

        Document teiDoc = engine.fullTextToTEIDoc(documentSource, flavor, config);

        PDDocument out = annotateFigureAndTables(
                document,
                copiedFile,
                teiDoc,
                false,
                false,
                true,
                true,
                VISUALIZE_VECTOR_BOXES);

        if (out != null) {
            out.save(outPdf);
            if (singleFile) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(outPdf);
                }
            }
        }

        if (outputFolder != null) {
            if (annotated) {
                Engine.getCntManager().i("TABLES_TEST", "ANNOTATED_PDFS");
                FileUtils.copyFile(
                        outPdf,
                        new File(outputFolder,
                                annotated
                                        ? (annotatedFigure ? input.getName() + "_annotatedFigure.pdf"
                                                : input.getName() + "_annotated.pdf")
                                        : input.getName()));
            }
        }
    }

    private static Engine setupEngine() {
        GrobidProperties.setGrobidHome("grobid-home");
        GrobidProperties.setGrobidConfigPath("grobid-home/config/grobid.yaml");
        LibraryLoader.load();
        return GrobidFactory.getInstance().getEngine();
    }

    public static PDDocument annotateFigureAndTables(
            PDDocument document,
            File xmlFile,
            Document teiDoc,
            boolean visualizeTeiFigures,
            boolean visualizePdfaltoImages,
            boolean visualizeGraphicObjects,
            boolean visualizeTables,
            boolean visualizeVectorBoxes) throws IOException, XPathException {
        String q = XQueryProcessor.getQueryFromResources("figure-table-coords.xq");
        String tei = teiDoc.getTei();
        XQueryProcessor pr = new XQueryProcessor(tei);
        SequenceIterator it = pr.getSequenceIterator(q);
        Item item;

        // visualizing TEI image coords
        if (visualizeTeiFigures) {
            while ((item = it.next()) != null) {
                String coords = item.getStringValue();
                String stringValue = it.next().getStringValue();
                boolean isFigure = Boolean.parseBoolean(stringValue);
                AnnotationUtil.annotatePage(document, coords, isFigure ? 1 : 2);
            }
        }

        //VISUALIZING "IMAGE" elements from pdfalto
        if (visualizePdfaltoImages) {
            q = XQueryProcessor.getQueryFromResources("figure-coords-pdfalto.xq");

            pr = new XQueryProcessor(xmlFile);
            it = pr.getSequenceIterator(q);
            while ((item = it.next()) != null) {
                String coords = item.getStringValue();
                AnnotationUtil.annotatePage(document, coords, 3);
            }
        }

        if (visualizeGraphicObjects) {
            int i = 10;
            if (teiDoc.getFigures() != null) {
                for (Figure f : teiDoc.getFigures()) {
                    if (f == null) {
                        continue;
                    }

                    i++;
                    List<GraphicObject> boxedGo = f.getBoxedGraphicObjects();

                    if (f.getTextArea() != null) {
                        for (BoundingBox b : f.getTextArea()) {
                            annotated = true;
                            AnnotationUtil.annotatePage(
                                    document,
                                    b.toString(),
                                    //                        AnnotationUtil.getCoordString(f.getPage(), f.getX(), f.getY(),
                                    //                                f.getWidth(), f.getHeight()),
                                    i,
                                    boxedGo == null ? 1 : 2);
                        }
                    }

                    if (boxedGo != null) {
                        for (GraphicObject go : boxedGo) {
                            annotatedFigure = true;
                            AnnotationUtil.annotatePage(
                                    document,
                                    AnnotationUtil.getCoordString(
                                            go.getPage(),
                                            go.getX(),
                                            go.getY(),
                                            go.getWidth(),
                                            go.getHeight()),
                                    i,
                                    2);

                        }
                    }
                }
            }
        }

        if (visualizeVectorBoxes) {
            if (teiDoc.getImages() != null) {
                for (GraphicObject img : teiDoc.getImages()) {
                    if (img.getType() == GraphicObjectType.VECTOR_BOX) {
                        BoundingBox go = img.getBoundingBox();
                        AnnotationUtil.annotatePage(
                                document,
                                AnnotationUtil.getCoordString(
                                        go.getPage(),
                                        go.getX(),
                                        go.getY(),
                                        go.getWidth(),
                                        go.getHeight()),
                                12,
                                3);
                    }
                }
            }
        }
        if (visualizeTables) {
            boolean hasSomeTables = false;
            if (teiDoc.getTables() != null) {
                for (Table t : teiDoc.getTables()) {
                    hasSomeTables = true;
                    if (!t.isGoodTable()) {
                        //System.out.println("Skipping bad table on page: " + t.getTextArea().get(0).getPage());
                        Engine.getCntManager().i("TABLES_TEST", "BAD_TABLES");

                        continue;
                    }

                    BoundingBox contentBox = BoundingBoxCalculator.calculateOneBox(t.getContentTokens());
                    BoundingBox descBox = BoundingBoxCalculator.calculateOneBox(t.getFullDescriptionTokens());

                    System.out.println("Annotating TABLE on page: " + contentBox.getPage());
                    AnnotationUtil.annotatePage(
                            document,
                            AnnotationUtil.getCoordString(descBox),
                            100,
                            2);
                    AnnotationUtil.annotatePage(
                            document,
                            AnnotationUtil.getCoordString(contentBox),
                            101,
                            2);
                    annotatedFigure = true;
                    annotated = true;
                    Engine.getCntManager().i("TABLES_TEST", "ANNOTATED_TABLES");
                }
            }

            if (hasSomeTables) {
                Engine.getCntManager().i("TABLES_TEST", "PDF_HAS_SOME_TABLES");
            }
        }

        return document;
    }

}
