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
package org.grobid.core.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.ArrayList;
import java.util.List;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import org.junit.Test;

/**
 * The visualizer queries run on pdfalto output, which is namespaced (SVG and ALTO v3).
 */
public class XQueryProcessorTest {

    private static List<String> run(String xml, String queryName) throws Exception {
        XQueryProcessor processor = new XQueryProcessor(xml);
        SequenceIterator it = processor.getSequenceIterator(XQueryProcessor.getQueryFromResources(queryName));
        List<String> results = new ArrayList<>();
        Item item;
        while ((item = it.next()) != null) {
            results.add(item.getStringValue());
        }
        return results;
    }

    @Test
    public void testVectorCoords_pdfaltoSvg() throws Exception {
        String svg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\">"
                + "<g style=\"stroke: #000000;\" id=\"p5_s1\">"
                + "<path d=\"M335.9100,409.0200 L338.1150,409.0200\"/>"
                + "<path d=\"M560.4700,401.5000 L558.2650,409.0200\"/>"
                + "</g>"
                + "<clipPath id=\"p5_s2\"/>"
                + "<g style=\"fill: #FFFFFF;\" id=\"p5_s3\">"
                + "<path d=\"M10,20 C15,10 25,10 30,20 Z\"/>"
                + "</g>"
                + "<g id=\"p5_s4\"/>"
                + "</svg>";

        assertThat(run(svg, "vector-coords.xq"), contains("335.91,401.5,224.56,7.52", "10,10,20,10"));
    }

    @Test
    public void testFigureCoordsPdfalto_altoV3() throws Exception {
        String alto = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<alto xmlns=\"http://www.loc.gov/standards/alto/ns-v3#\"><Layout>"
                + "<Page ID=\"Page1\" PHYSICAL_IMG_NR=\"1\" WIDTH=\"594.72\" HEIGHT=\"840.96\"><PrintSpace>"
                + "<Illustration ID=\"p1_s259\" HPOS=\"60\" VPOS=\"80.329\" WIDTH=\"453.916\" HEIGHT=\"65.531\" TYPE=\"svg\"/>"
                + "</PrintSpace></Page>"
                + "<Page ID=\"Page2\" PHYSICAL_IMG_NR=\"2\" WIDTH=\"594.72\" HEIGHT=\"840.96\"><PrintSpace>"
                + "<Illustration ID=\"p2_s652\" HPOS=\"0\" VPOS=\"0\" WIDTH=\"0\" HEIGHT=\"0\" TYPE=\"svg\"/>"
                + "<Illustration ID=\"p2_i1\" HPOS=\"100\" VPOS=\"200\" WIDTH=\"300\" HEIGHT=\"150.5\" TYPE=\"image\"/>"
                + "</PrintSpace></Page>"
                + "</Layout></alto>";

        assertThat(
                run(alto, "figure-coords-pdfalto.xq"),
                contains("1,60,80.329,453.916,65.531", "2,100,200,300,150.5"));
    }
}
