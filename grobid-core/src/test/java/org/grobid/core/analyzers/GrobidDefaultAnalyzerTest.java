package org.grobid.core.analyzers;

import org.grobid.core.layout.LayoutToken;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class GrobidDefaultAnalyzerTest {
    GrobidDefaultAnalyzer target;

    @Before
    public void setUp() throws Exception {
        target = GrobidDefaultAnalyzer.getInstance();
    }

    @Test
    public void testTokenizeWithLayoutToken() {
        final List<LayoutToken> layoutTokens = target
                .tokenizeWithLayoutToken("This is a normal \ntext,\n\n\n on several lines.\n");

        assertThat(layoutTokens, hasSize(22));
        assertThat(layoutTokens.get(0).getText(), is("This"));
        assertThat(layoutTokens.get(1).getText(), is(" "));
        assertThat(layoutTokens.get(6).getText(), is("normal"));
        assertThat(layoutTokens.get(7).getText(), is(" "));
        assertThat(layoutTokens.get(7).isNewLineAfter(), is(true));
        assertThat(layoutTokens.get(8).getText(), is("\n"));
        assertThat(layoutTokens.get(8).isNewLineAfter(), is(false));
        assertThat(layoutTokens.get(10).getText(), is(","));
        assertThat(layoutTokens.get(10).isNewLineAfter(), is(true));
        assertThat(layoutTokens.get(11).getText(), is("\n"));
        assertThat(layoutTokens.get(11).isNewLineAfter(), is(true));
        assertThat(layoutTokens.get(12).getText(), is("\n"));
        assertThat(layoutTokens.get(12).isNewLineAfter(), is(true));
        assertThat(layoutTokens.get(13).getText(), is("\n"));
        assertThat(layoutTokens.get(13).isNewLineAfter(), is(false));
    }

    @Test
    public void testTokenizeWithLayoutToken_emptyText() {
        assertThat(target.tokenizeWithLayoutToken(""), hasSize(0));
    }
}
