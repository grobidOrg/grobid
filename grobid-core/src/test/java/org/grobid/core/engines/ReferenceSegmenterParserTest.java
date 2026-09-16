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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.SortedSet;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentPiece;
import org.grobid.core.document.DocumentPointer;
import org.grobid.core.engines.citations.LabeledReferenceResult;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.factory.AbstractEngineFactory;

public class ReferenceSegmenterParserTest {
    @BeforeAll
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
        }.createTrainingData(doc, "sample");

        assertThat(result, notNullValue());
        assertThat(result.getLeft(), notNullValue());
        assertThat(result.getRight(), notNullValue());
    }

    @Test
    public void extract_shouldDropTrailingLabelWithoutReferenceText() {
        Document doc = Document.createFromText("Alpha Beta 18");
        SortedSetMultimap<String, DocumentPiece> labeledBlocks = TreeMultimap.create();
        int lastTokenIndex = doc.getTokenizations().size() - 1;
        DocumentPiece references = new DocumentPiece(
                new DocumentPointer(doc, 0, 0),
                new DocumentPointer(doc, 0, lastTokenIndex));
        labeledBlocks.put(SegmentationLabels.REFERENCES.getLabel(), references);
        doc.setLabeledBlocks(labeledBlocks);
        SortedSet<DocumentPiece> referencesParts = doc.getDocumentPart(SegmentationLabels.REFERENCES);

        // the labeler tags the last token as a bare <label> not followed by any <reference> token
        List<LabeledReferenceResult> result = new ReferenceSegmenterParser() {
            @Override
            public String label(String data) {
                StringBuilder output = new StringBuilder();
                String[] lines = data.split("\n");
                int lastLine = lines.length - 1;
                while (lastLine > 0 && lines[lastLine].trim().isEmpty()) {
                    lastLine--;
                }
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    String token = line.split("\\s+")[0];
                    String label = i == 0 ? "I-<reference>" : (i == lastLine ? "I-<label>" : "<reference>");
                    output.append(token).append("\t").append(label).append("\n");
                }
                return output.toString();
            }
        }.extract(doc, referencesParts, false);

        assertThat(result, notNullValue());
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getReferenceText(), is("Alpha Beta"));
        assertThat(result.get(0).getLabel(), nullValue());
    }
}
