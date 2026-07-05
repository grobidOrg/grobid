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

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentPiece;
import org.grobid.core.document.DocumentPointer;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.factory.AbstractEngineFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ReferenceSegmenterParserTest {
    @BeforeClass
    public static void setInitialContext() {
        AbstractEngineFactory.init();
    }

    @Test
    public void createTrainingData_shouldIgnoreEmptyLabeledLines() {
        Document doc = Document.createFromText("Alpha Beta");
        SortedSetMultimap<String, DocumentPiece> labeledBlocks = TreeMultimap.create();
        int lastTokenIndex = doc.getTokenizations().size() - 1;
        DocumentPiece references = new DocumentPiece(
            new DocumentPointer(doc, 0, 0),
            new DocumentPointer(doc, 0, lastTokenIndex));
        labeledBlocks.put(SegmentationLabels.REFERENCES.getLabel(), references);
        doc.setLabeledBlocks(labeledBlocks);

        Pair<String, String> result = new ReferenceSegmenterParser() {
            @Override
            public String label(String data) {
                StringBuilder output = new StringBuilder();
                String[] lines = data.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.isEmpty()) {
                        output.append("\n");
                        continue;
                    }
                    String token = line.split("\\s+")[0];
                    output.append(token).append("\t<reference>\n");
                    if (i == 0) {
                        output.append("\n");
                    }
                }
                return output.toString();
            }
        }.createTrainingData(doc, 1);

        assertThat(result, notNullValue());
        assertThat(result.getLeft(), notNullValue());
        assertThat(result.getRight(), notNullValue());
    }
}
