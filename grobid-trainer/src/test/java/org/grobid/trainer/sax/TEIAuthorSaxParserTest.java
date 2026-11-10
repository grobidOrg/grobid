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
package org.grobid.trainer.sax;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParserFactory;

import org.junit.Test;

import org.grobid.core.layout.LayoutToken;

public class TEIAuthorSaxParserTest {

    private static final String TEI = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<tei xmlns=\"http://www.tei-c.org/ns/1.0\"><teiHeader><fileDesc><sourceDesc><biblStruct><analytic>"
            + "<author>"
            + "<persName><forename>Eri</forename> <surname>Takeda</surname> <marker>1</marker> ,</persName>"
            + "<persName><forename>Go</forename> <surname>Matsuda</surname> <marker>1,2</marker> and</persName>"
            + "<persName><forename>Yoko</forename> <surname>Aida</surname></persName>"
            + "</author>"
            + "</analytic></biblStruct></sourceDesc></fileDesc></teiHeader></tei>";

    @Test
    public void testConnectorAtTheEndOfPersNameIsKept() throws Exception {
        TEIAuthorSaxParser parser = new TEIAuthorSaxParser();
        SAXParserFactory.newInstance().newSAXParser()
                .parse(new ByteArrayInputStream(TEI.getBytes(StandardCharsets.UTF_8)), parser);

        assertThat(parser.getTokensResult(), hasSize(1));
        List<LayoutToken> tokens = parser.getTokensResult().get(0);
        List<String> labels = parser.getLabeledResult().get(0);

        List<String> labelled = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (labels.get(i) != null) {
                labelled.add(tokens.get(i).getText() + "/" + labels.get(i));
            }
        }
        assertThat(labelled, contains(
                "Eri/I-<forename>", "Takeda/I-<surname>", "1/I-<marker>", ",/I-<other>",
                "Go/I-<forename>", "Matsuda/I-<surname>", "1/I-<marker>", ",/<marker>", "2/<marker>", "and/I-<other>",
                "Yoko/I-<forename>", "Aida/I-<surname>"));
    }
}
