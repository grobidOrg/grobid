package org.grobid.core.utilities;

import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LayoutTokensUtilIntegrationTest {

    @BeforeClass
    public static void setUp() throws Exception {
        TestEngineUtils.initGrobidForceWapiti();
    }

    @Test
    public void testDoesRequireDehyphenization2() throws Exception {

        DocumentSource documentSource = DocumentSource.fromPdf(new File("src/test/resources/org/grobid/core/utilities/dehypenisation1.pdf"));
        Document result = Engine.getEngine(false).getParsers().getSegmentationParser().processing(documentSource, GrobidAnalysisConfig.defaultInstance());

        assertThat(LayoutTokensUtil.doesRequireDehypenisation(result.getTokenizations(), 7), is(true));

    }

    @Test
    public void testDoesRequireDehyphenization() throws Exception {

        DocumentSource documentSource = DocumentSource.fromPdf(new File("src/test/resources/org/grobid/core/utilities/dehypenisation2.pdf"));
        Document result = Engine.getEngine(false).getParsers().getSegmentationParser().processing(documentSource, GrobidAnalysisConfig.defaultInstance());

        assertThat(LayoutTokensUtil.doesRequireDehypenisation(result.getTokenizations(), 7), is(true));

    }

}