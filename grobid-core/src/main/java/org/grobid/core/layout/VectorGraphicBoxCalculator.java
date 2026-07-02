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
package org.grobid.core.layout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.document.Document;
import org.grobid.core.utilities.XQueryProcessor;

/**
 * Working with vector graphics
 */
public class VectorGraphicBoxCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorGraphicBoxCalculator.class);

    public static final int MINIMUM_VECTOR_BOX_AREA = 3000;
    public static final int VEC_GRAPHICS_FILE_SIZE_LIMIT = 100 * 1024 * 1024;

    public static Multimap<Integer, GraphicObject> calculate(Document document) throws IOException, XPathException {

        Multimap<Integer, Block> blockMultimap = HashMultimap.create();
        Multimap<Integer, GraphicObject> result = LinkedHashMultimap.create();

        for (int pageNum = 1; pageNum <= document.getPages().size(); pageNum++) {
            BoundingBox mainPageArea = document.getPage(pageNum).getMainArea();

            String q = XQueryProcessor.getQueryFromResources("vector-coords.xq");
            File vecFile = new File(document.getDocumentSource().getXmlFile().getAbsolutePath() + "_data",
                    "image-" + pageNum + ".svg");
            if (vecFile.exists()) {
                if (vecFile.length() > VEC_GRAPHICS_FILE_SIZE_LIMIT) {
                    LOGGER.error(
                            "The vector file " + vecFile + " is too large to be processed, size: " + vecFile.length());
                    continue;
                }

                XQueryProcessor pr = new XQueryProcessor(vecFile);

                SequenceIterator it = pr.getSequenceIterator(q);
                Item item;
                List<BoundingBox> boxes = new ArrayList<>();

                while ((item = it.next()) != null) {
                    String c = item.getStringValue();
                    // TODO: figure out why such string are returned at all (AS:602281691082754@1520606553791)
                    if (c.equals(",,,")) {
                        continue;
                    }
                    String coords = pageNum + "," + c;
                    BoundingBox e = BoundingBox.fromString(coords);
                    if (!mainPageArea.contains(e) || e.area() / mainPageArea.area() > 0.7) {
                        continue;
                    }
                    boxes.add(e);
                }

                List<BoundingBox> remainingBoxes = mergeBoxes(boxes);
                for (int i = 0; i < remainingBoxes.size(); i++) {
                    Collection<Block> col = blockMultimap.get(pageNum);
                    for (Block bl : col) {
                        //                    if (!bl.getPage().getMainArea().contains(b)) {
                        //                        continue;
                        //                    }

                        BoundingBox b = BoundingBox
                                .fromPointAndDimensions(pageNum, bl.getX(), bl.getY(), bl.getWidth(), bl.getHeight());
                        if (remainingBoxes.get(i).intersect(b)) {
                            remainingBoxes.set(i, remainingBoxes.get(i).boundBox(b));
                        }
                    }
                }

                remainingBoxes = mergeBoxes(remainingBoxes);
                for (BoundingBox b : remainingBoxes) {
                    if (b.area() > MINIMUM_VECTOR_BOX_AREA) {
                        result.put(pageNum, new GraphicObject(b, GraphicObjectType.VECTOR_BOX));
                    }
                }

            }
        }
        return result;
    }

    public static List<BoundingBox> mergeBoxes(List<BoundingBox> boxes) {
        boolean allMerged = false;
        while (!allMerged) {
            allMerged = true;
            for (int i = 0; i < boxes.size(); i++) {
                BoundingBox a = boxes.get(i);
                if (a == null)
                    continue;
                for (int j = i + 1; j < boxes.size(); j++) {
                    BoundingBox b = boxes.get(j);
                    if (b != null) {
                        if (a.intersect(b)) {
                            allMerged = false;
                            a = a.boundBox(b);
                            boxes.set(i, a);
                            boxes.set(j, null);
                        }
                    }
                }
            }
        }

        return Lists.newArrayList(Iterables.filter(boxes, new Predicate<BoundingBox>() {
            @Override
            public boolean apply(BoundingBox boundingBox) {
                if (boundingBox == null) {
                    return false;
                }
                if (boundingBox.getHeight() < 5 || boundingBox.getWidth() < 5) {
                    return false;
                }
                return true;
            }
        }));
    }
}
