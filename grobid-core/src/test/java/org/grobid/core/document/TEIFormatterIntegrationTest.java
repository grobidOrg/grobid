package org.grobid.core.document;

import org.grobid.core.data.Note;
import org.grobid.core.engines.EngineParsers;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.List;

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

        /*
         * assertThat(teiNotes, hasSize(1)); assertThat(teiNotes.get(0).getText(), is(" http://wikipedia.org  "));
         * assertThat(teiNotes.get(0).getLabel(), is("1")); assertThat(teiNotes.get(0).getPageNumber(), is(1));
         */
    }

}
