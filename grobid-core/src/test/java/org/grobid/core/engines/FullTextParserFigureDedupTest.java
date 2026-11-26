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
package org.grobid.core.engines;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.data.Figure;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.GraphicObject;
import org.grobid.core.layout.GraphicObjectType;
import org.grobid.core.main.LibraryLoader;

public class FullTextParserFigureDedupTest {

    @BeforeClass
    public static void init() {
        LibraryLoader.load();
        GrobidFactory.getInstance().createEngine();
    }

    @Test
    public void dropsCaptionlessFigureCoveredByCaptionedFigure() {
        Figure captioned = figure(1, 10, 10, 200, 200, "Figure 1. A useful caption");
        Figure captionless = figure(1, 50, 50, 50, 50, null);
        Set<Figure> duplicates = FullTextParser.findCaptionlessDuplicateFigures(
                Arrays.asList(captioned, captionless));
        assertTrue(duplicates.contains(captionless));
        assertFalse(duplicates.contains(captioned));
    }

    @Test
    public void keepsCaptionlessFigureWithoutCaptionedOverlap() {
        Figure captioned = figure(1, 10, 10, 50, 50, "Figure 1. A useful caption");
        Figure captionless = figure(1, 100, 100, 50, 50, null);
        assertTrue(
                FullTextParser.findCaptionlessDuplicateFigures(
                        Arrays.asList(captioned, captionless)).isEmpty());
    }

    @Test
    public void keepsBothCaptionedOverlappingFigures() {
        Figure first = figure(1, 10, 10, 100, 100, "Figure 1. First caption");
        Figure second = figure(1, 20, 20, 100, 100, "Figure 2. Second caption");
        assertTrue(
                FullTextParser.findCaptionlessDuplicateFigures(
                        Arrays.asList(first, second)).isEmpty());
    }

    @Test
    public void requiresHalfOfSmallerBoxToOverlap() {
        Figure captioned = figure(1, 0, 0, 100, 100, "Figure 1. A useful caption");
        Figure belowThreshold = figure(1, 90, 0, 100, 100, null);
        Figure atThreshold = figure(1, 50, 0, 100, 100, null);
        Set<Figure> duplicates = FullTextParser.findCaptionlessDuplicateFigures(
                Arrays.asList(captioned, belowThreshold, atThreshold));
        assertFalse(duplicates.contains(belowThreshold));
        assertTrue(duplicates.contains(atThreshold));
    }

    /**
     * A multi-panel figure carries one box per panel, so a typed region sitting between
     * the panels overlaps none of them individually while clearly covering the same
     * figure. Comparing the per-page union catches it; comparing box by box does not.
     */
    @Test
    public void matchesAcrossThePerPageUnionOfPanelBoxes() {
        Figure captioned = multiBoxFigure(
                1,
                "Figure 1. A multi-panel caption",
                BoundingBox.fromPointAndDimensions(1, 0, 0, 10, 10),
                BoundingBox.fromPointAndDimensions(1, 90, 0, 10, 10));
        Figure captionless = figure(1, 40, 0, 20, 10, null);
        Set<Figure> duplicates = FullTextParser.findCaptionlessDuplicateFigures(
                Arrays.asList(captioned, captionless));
        assertTrue(duplicates.contains(captionless));
        assertFalse(duplicates.contains(captioned));
    }

    @Test
    public void doesNotMergeBoxesAcrossPages() {
        Figure captioned = multiBoxFigure(
                1,
                "Figure 1. Spans two pages",
                BoundingBox.fromPointAndDimensions(1, 0, 0, 10, 10),
                BoundingBox.fromPointAndDimensions(2, 90, 0, 10, 10));
        Figure captionless = figure(2, 40, 0, 20, 10, null);
        assertTrue(
                FullTextParser.findCaptionlessDuplicateFigures(
                        Arrays.asList(captioned, captionless)).isEmpty());
    }

    private static Figure multiBoxFigure(int page, String caption, BoundingBox... boxes) {
        Figure figure = new Figure();
        List<GraphicObject> objects = new ArrayList<>();
        for (BoundingBox box : boxes) {
            objects.add(new GraphicObject(box, GraphicObjectType.VECTOR_BOX));
        }
        figure.setGraphicObjects(objects);
        figure.setCaption(new StringBuilder(caption));
        return figure;
    }

    private static Figure figure(
            int page,
            double x,
            double y,
            double width,
            double height,
            String caption) {
        Figure figure = new Figure();
        BoundingBox box = BoundingBox.fromPointAndDimensions(page, x, y, width, height);
        figure.setGraphicObjects(
                Collections.singletonList(
                        new GraphicObject(box, GraphicObjectType.VECTOR_BOX)));
        if (caption != null) {
            figure.setCaption(new StringBuilder(caption));
        }
        return figure;
    }
}
