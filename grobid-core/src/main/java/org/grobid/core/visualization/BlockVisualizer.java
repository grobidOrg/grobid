package org.grobid.core.visualization;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.grobid.core.document.Document;
import org.grobid.core.layout.Block;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.utilities.XQueryProcessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.grobid.core.layout.VectorGraphicBoxCalculator.mergeBoxes;


/**
 * Visualizing blocks
 */

public class BlockVisualizer {

    public static PDDocument annotateBlocks(PDDocument document, File xmlFile, Document teiDoc,
                                            boolean visualizeBlocks,
                                            boolean visualizePageMainArea,
                                            boolean visualizeVectorGraphics) throws IOException, XPathException {

        Multimap<Integer, Block> blockMultimap = HashMultimap.create();


        for (Block b : teiDoc.getBlocks()) {
            if (visualizeBlocks) {
                AnnotationUtil.annotatePage(document, b.getPageNumber() + "," + b.getX() + "," + b.getY() +
                    "," + b.getWidth() + "," + b.getHeight(), 0);
                blockMultimap.put(b.getPageNumber(), b);
            }
        }


        for (int pageNum = 1; pageNum <= document.getNumberOfPages(); pageNum++) {
            BoundingBox mainPageArea = teiDoc.getPage(pageNum).getMainArea();
            if (visualizePageMainArea) {
                AnnotationUtil.annotatePage(document,
                    mainPageArea.toString(), 10);
            }
            File f = new File(xmlFile.getAbsolutePath() + "_data", "image-" + pageNum + ".svg");
            if (f.exists()) {
                String q = XQueryProcessor.getQueryFromResources("vector-coords.xq");
                XQueryProcessor pr = new XQueryProcessor(f);
                SequenceIterator it = pr.getSequenceIterator(q);
                Item item;
                List<BoundingBox> boxes = new ArrayList<>();

                while ((item = it.next()) != null) {
                    String c = item.getStringValue();
                    String coords = pageNum + "," + c;
                    BoundingBox e = BoundingBox.fromString(coords);

                    if (!mainPageArea.contains(e) || e.area() / mainPageArea.area() > 0.8) {
                        continue;
                    }


                    AnnotationUtil.annotatePage(document, e.toString(), 3);

                    boxes.add(e);
                }

                if (visualizeVectorGraphics) {
                    List<BoundingBox> remainingBoxes = mergeBoxes(boxes);

                    for (int i = 0; i < remainingBoxes.size(); i++) {
                        Collection<Block> col = blockMultimap.get(pageNum);
                        for (Block bl : col) {
//                    if (!bl.getPage().getMainArea().contains(b)) {
//                        continue;
//                    }

                            BoundingBox b = BoundingBox.fromPointAndDimensions(pageNum, bl.getX(), bl.getY(), bl.getWidth(), bl.getHeight());
                            if (remainingBoxes.get(i).intersect(b)) {
                                remainingBoxes.set(i, remainingBoxes.get(i).boundBox(b));
                            }
                        }
                    }

                    remainingBoxes = mergeBoxes(remainingBoxes);

                    for (BoundingBox b : remainingBoxes) {
                        if (b.area() > 500) {
                            AnnotationUtil.annotatePage(document, b.toString(), 1);
                        }
                    }
                }
            }
        }
        return document;
    }


}
