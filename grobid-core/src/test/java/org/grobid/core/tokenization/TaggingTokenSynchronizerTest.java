package org.grobid.core.tokenization;

import org.grobid.core.GrobidModels;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.Pair;
import org.grobid.core.utilities.GrobidProperties;
import org.junit.Test;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Testing synchronization
 */
public class TaggingTokenSynchronizerTest {
    public static final String P = "<paragraph>";
    public static final String F = "<figure>";

    @BeforeClass
    public static void init() {
        GrobidProperties.getInstance();
    }

    @Test
    public void testBasic() {
        TaggingTokenSynchronizer synchronizer = new TaggingTokenSynchronizer(GrobidModels.modelFor("fulltext"),
                generateResult(p("This", P), p("Figure", F)), toks("This", " ", "Figure"));

        int cnt = 0;
        boolean spacesPresent = false;
        for (LabeledTokensContainer el : synchronizer) {
            String text = LayoutTokensUtil.toText(el.getLayoutTokens());
            assertFalse(text.startsWith(" "));
            if (text.contains(" ")) {
                spacesPresent = true;
            }
            cnt++;
        }

        assertThat(cnt, is(2));
        assertThat(spacesPresent, is(true));
    }

    @Test(expected = IllegalStateException.class)
    public void testFailure() {
        TaggingTokenSynchronizer synchronizer = new TaggingTokenSynchronizer(GrobidModels.modelFor("fulltext"),
                generateResult(p("This", P), p("Figure", F)), toks("This", " ", "Fig"));

        for (LabeledTokensContainer el : synchronizer) {
            LayoutTokensUtil.toText(el.getLayoutTokens());
        }
    }

    private static String generateResult(Pair<String, String>... tokens) {
        StringBuilder res = new StringBuilder();
        for (Pair<String, String> p : tokens) {
            res.append(p.a).append("\t").append(p.b).append("\n");
        }
        return res.toString();
    }

    private static List<LayoutToken> toks(String... toks) {
        List<LayoutToken> res = new ArrayList<>();
        for (String t : toks) {
            res.add(new LayoutToken(t));
        }
        return res;
    }

    private static Pair<String, String> p(String tok, String label) {
        return new Pair<>(tok, label);
    }
}
