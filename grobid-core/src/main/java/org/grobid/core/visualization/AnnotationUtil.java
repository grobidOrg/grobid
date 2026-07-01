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

import java.io.IOException;
import java.util.Random;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.layout.BoundingBox;

/**
 * Utilities for annotating PDF
 */

public class AnnotationUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationUtil.class);

    public static BoundingBox getBoundingBoxForPdf(PDDocument document, String coords) {
        String[] split = coords.split(",");

        Long pageNum = Long.valueOf(split[0], 10) - 1;
        PDPage page = (PDPage) document.getDocumentCatalog().getPages().get(pageNum.intValue());

        PDRectangle mediaBox = page.getCropBox();
        if (mediaBox == null) {
            mediaBox = page.getMediaBox();
            // this will look for the main media box of the page up in the PDF element hierarchy
            if (mediaBox == null) {
                // last hope
                mediaBox = page.getArtBox();
                if (mediaBox == null) {
                    // we tried our best given PDFBox
                    LOGGER.warn("Media box for page " + pageNum.intValue() + " not found.");
                    return null;
                }
            }
        }

        if (mediaBox == null) {
            System.out.println("Null mediabox for page: " + (pageNum + 1));
            return null;
        }
        float height = mediaBox.getHeight();
        float lowerX = mediaBox.getLowerLeftX();
        float lowerY = mediaBox.getLowerLeftY();

        float x = Float.parseFloat(split[1]);
        float y = Float.parseFloat(split[2]);
        float w = Float.parseFloat(split[3]);
        String nextString = split[4];
        if (nextString.indexOf(";") != -1)
            nextString = nextString.substring(0, nextString.indexOf(";"));
        float h = Float.parseFloat(nextString);

        float annX = x + lowerX;
        float annY = (height - (y + h)) + lowerY;
        float annRightX = x + w + lowerX;
        float annTopY = height - y + lowerY;
        return BoundingBox.fromTwoPoints(pageNum.intValue(), annX, annY, annRightX, annTopY);
    }

    public static void annotatePage(PDDocument document, String coords, int seed) throws IOException {
        annotatePage(document, coords, seed, 1);
    }

    public static void annotatePage(PDDocument document, String coords, int seed, int lineWidth) throws IOException {
        if (coords == null) {
            return;
        }
        //System.out.println("Annotating for coordinates: " + coords);

        BoundingBox box = getBoundingBoxForPdf(document, coords);
        if (box == null) {
            //System.out.println("Null bounding box for coords: " + coords);
            // nothing to do
            return;
        }

        PDPage page = document.getDocumentCatalog().getPages().get(box.getPage());
        float annX = (float) box.getX();
        float annY = (float) box.getY();
        float annRightX = (float) box.getX2();
        float annTopY = (float) box.getY2();

        PDRectangle rect = new PDRectangle();

        rect.setLowerLeftX(annX);
        rect.setLowerLeftY(annY);
        rect.setUpperRightX(annRightX);
        rect.setUpperRightY(annTopY);

        PDBorderStyleDictionary borderULine = new PDBorderStyleDictionary();
        borderULine.setStyle(PDBorderStyleDictionary.STYLE_SOLID);
        // so that a border is not visible at all
        borderULine.setWidth(0);

        PDAnnotationLink txtLink = new PDAnnotationLink();
        txtLink.setBorderStyle(borderULine);

        //linkColor rectangle border color (ideally, should be transparent)
        COSArray linkColor = new COSArray();

        Random r = new Random(seed);

        //        linkColor.setFloatArray(new float[]{r.nextInt(128) + 127, r.nextInt(255), r.nextInt(255)});
        linkColor.setFloatArray(new float[]{224, 9, 56});
        txtLink.setColor(new PDColor(linkColor, PDDeviceRGB.INSTANCE));
        txtLink.setReadOnly(true);

        //so that
        txtLink.setHighlightMode(PDAnnotationLink.HIGHLIGHT_MODE_PUSH);

        PDActionURI action = new PDActionURI();
        action.setURI("");
        txtLink.setAction(action);
        txtLink.setRectangle(rect);

        // ADDING LINK TO THE REFERENCE
        //        page.getAnnotations().add(txtLink);

        //draw a line
        PDBorderStyleDictionary borderThick = new PDBorderStyleDictionary();
        borderThick.setWidth(lineWidth);  // 12th inch

        //            PDAnnotationLine line = new PDAnnotationLine();
        //            line.setLine(new float[]{annX, annY, annRightX, annY});
        //            line.setRectangle(rect);
        //            line.setBorderStyle(borderThick);
        //            line.setReadOnly(true);
        //            line.setLocked(true);
        //
        //            COSArray rgLineColor = new COSArray();
        //            rgLineColor.setFloatArray(new float[]{85 / 255f, 177 / 255f, 245 / 255f});
        //            PDGamma col = new PDGamma(rgLineColor);
        //            line.setColour(col);

        // ADDING LINE TO THE REFERENCE
        //            page.getAnnotations().add(line);

        // ADDING LINE TO THE REFERENCE
        PDPageContentStream stream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND,
                false, true);
        //        Random r = new Random(seed + 1);
        //
        //
        ////        stream.setStrokingColor(85, 177, 245);
        stream.setStrokingColor(r.nextInt(255), r.nextInt(255), r.nextInt(255));
        stream.setLineWidth(lineWidth);
        stream.drawLine(annX, annY, annRightX, annY);
        stream.drawLine(annX, annTopY, annRightX, annTopY);
        stream.drawLine(annX, annY, annX, annTopY);
        stream.drawLine(annRightX, annY, annRightX, annTopY);
        stream.close();
        //        }
        //        return 1;
    }

    public static String getCoordString(BoundingBox b) {
        if (b == null) {
            return null;
        }
        return b.getPage() + "," + b.getX() + "," + b.getY() + "," + b.getWidth() + "," + b.getHeight();
    }

    public static String getCoordString(int page, double x, double y, double w, double h) {
        return page + "," + x + "," + y + "," + w + "," + h;
    }

}
