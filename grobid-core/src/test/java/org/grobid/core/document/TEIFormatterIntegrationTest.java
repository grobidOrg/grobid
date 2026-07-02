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
package org.grobid.core.document;

import java.io.File;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.data.Note;
import org.grobid.core.engines.EngineParsers;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;

public class TEIFormatterIntegrationTest {

    @BeforeClass
    public static void setInitialContext() throws Exception {
        GrobidProperties.getInstance();
        LibraryLoader.load();
    }

    @Test
    public void testGetTeiNotes() throws Exception {
        EngineParsers engine = new EngineParsers();
        File input = new File(this.getClass().getResource("/footnotes/test.pdf").toURI());
        Document doc = engine.getSegmentationParser()
                .processing(DocumentSource.fromPdf(input), GrobidAnalysisConfig.defaultInstance());

        List<Note> teiNotes = new TEIFormatter(null, null).getTeiNotes(doc);

        /*assertThat(teiNotes, hasSize(1));
        assertThat(teiNotes.get(0).getText(), is(" http://wikipedia.org  "));
        assertThat(teiNotes.get(0).getLabel(), is("1"));
        assertThat(teiNotes.get(0).getPageNumber(), is(1));*/
    }

}
