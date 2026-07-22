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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import nu.xom.Element;
import nu.xom.Node;
import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.Figure;
import org.grobid.core.data.Note;
import org.grobid.core.data.Table;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.LabeledTokensContainer;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.LayoutTokensUtil;

public class TEIFormatterTest {

    @BeforeClass
    public static void setInitialContext() throws Exception {
        GrobidProperties.getInstance();
    }

    @Test
    public void testMakeFootNote() throws Exception {

        String text = "1 This is a footnote";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        List<Note> footnotes = new TEIFormatter(null, null).makeNotes(tokens, text, Note.NoteType.FOOT, 0);
        assertThat(footnotes.size(), is(1));

        Note footnote = footnotes.get(0);

        assertThat(footnote.getText(), is("This is a footnote"));
        assertThat(LayoutTokensUtil.toText(footnote.getTokens()), is("This is a footnote"));
        assertThat(footnote.getLabel(), is("1"));
    }

    @Test
    public void testMakeNotes() throws Exception {
        String text = "198 U.S. Const. art. I,  § §9 & 10. \n199 To be sure, there are revisionist arguments that the Ex Post Facto clause itself extends to retroactive civil laws too. See Eastern Enterprises v. Apfel, 524 U.S. 498, 538-39 (1998) (Thomas, J., concurring). And as with bills of attainder, in the wake of the Civil War the Supreme Court held that Ironclad  Oath requirements were ex post facto laws as well. Cummings, 71 U.S. at 326-332; Garland, 71 U.S.  at 377-368. But as discussed in the text, even these principles do not ensnare Section Three going  forward, on a non-ex-post-facto basis \n200 3 U.S. at 378-80 (arguments of counsel). \n201 Id. \n202 Id. at 382. See Baude & Sachs, Eleventh Amendment, supra note 9, at 626-627.   Electronic copy available at: https://ssrn.com/abstract=4532751";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        text = text.replace("\n", " ");
        tokens.stream().forEach(t -> t.setOffset(t.getOffset() + 403));
        List<Note> footnotes = new TEIFormatter(null, null)
                .makeNotes(tokens, text, Note.NoteType.FOOT, 37);

        assertThat(footnotes, hasSize(5));
        assertThat(footnotes.get(0).getLabel(), is("198"));
        assertThat(footnotes.get(0).getTokens(), hasSize(greaterThan(0)));
        assertThat(footnotes.get(1).getLabel(), is("199"));
        assertThat(footnotes.get(1).getTokens(), hasSize(greaterThan(0)));
        assertThat(footnotes.get(2).getLabel(), is("200"));
        assertThat(footnotes.get(2).getTokens(), hasSize(greaterThan(0)));
        assertThat(footnotes.get(3).getLabel(), is("201"));
        assertThat(footnotes.get(3).getText(), is("Id. "));
        assertThat(footnotes.get(3).getTokens(), hasSize(greaterThan(0)));
        assertThat(footnotes.get(4).getLabel(), is("202"));
        assertThat(footnotes.get(4).getTokens(), hasSize(greaterThan(0)));
    }

    @Test
    public void testGenerateURLRef() throws Exception {
        String input = "http:// github.com/ lfoppiano/ grobid-bla";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        Element node = new TEIFormatter(null, null)
                .generateURLRef("http:// github.com/ lfoppiano/ grobid-bla", tokens, false);

        assertThat(
                node.toXML(),
                is(
                        "<ref xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"url\" target=\"http://github.com/lfoppiano/grobid-bla\">http:// github.com/ lfoppiano/ grobid-bla</ref>"));
    }

    @Test
    public void testMarkReferencesFigureTEI() throws Exception {
        String input = "3C and 3D";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<LayoutToken> tokensWithOffset = tokens.stream()
                .peek(t -> t.setOffset(t.getOffset() + 51393))
                .collect(Collectors.toList());

        Figure f1 = new Figure();
        f1.setLabel(new StringBuilder("1"));
        Figure f2 = new Figure();
        f2.setLabel(new StringBuilder("2"));
        Figure f3 = new Figure();
        f3.setLabel(new StringBuilder(""));

        List<Figure> figures = List.of(f1, f2, f3);

        List<Node> nodes = new TEIFormatter(null, null)
                .markReferencesFigureTEI(
                        input,
                        tokensWithOffset,
                        figures,
                        false);

        assertThat(nodes, hasSize(4));
        assertThat(
                ((Element) nodes.get(0)).toXML(),
                is("<ref xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"figure\">3C</ref>"));
        assertThat(nodes.get(1).toXML(), is(" and"));
        assertThat(nodes.get(2).toXML(), is(" "));
        assertThat(
                ((Element) nodes.get(3)).toXML(),
                is("<ref xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"figure\">3D</ref>"));
    }

    @Test
    public void testMarkReferencesFigureTEI_truncatedRef_andSeparator_referenceAtBeginning() throws Exception {
        String input = "3D and";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<LayoutToken> tokensWithOffset = tokens.stream()
                .peek(t -> t.setOffset(t.getOffset() + 51393))
                .collect(Collectors.toList());

        Figure f1 = new Figure();
        f1.setLabel(new StringBuilder("1"));
        Figure f2 = new Figure();
        f2.setLabel(new StringBuilder("2"));
        Figure f3 = new Figure();
        f3.setLabel(new StringBuilder(""));

        List<Figure> figures = List.of(f1, f2, f3);

        List<Node> nodes = new TEIFormatter(null, null)
                .markReferencesFigureTEI(
                        input,
                        tokensWithOffset,
                        figures,
                        false);

        assertThat(nodes, hasSize(2));
        assertThat(
                ((Element) nodes.get(0)).toXML(),
                is("<ref xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"figure\">3D</ref>"));
        assertThat(nodes.get(1).toXML(), is(" and"));
    }

    @Test
    public void testMarkReferencesFigureTEI_truncatedRef_referenceAtTheEnd() throws Exception {
        String input = "and 3D";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<LayoutToken> tokensWithOffset = tokens.stream()
                .peek(t -> t.setOffset(t.getOffset() + 51393))
                .collect(Collectors.toList());

        Figure f1 = new Figure();
        f1.setLabel(new StringBuilder("1"));
        Figure f2 = new Figure();
        f2.setLabel(new StringBuilder("2"));
        Figure f3 = new Figure();
        f3.setLabel(new StringBuilder(""));

        List<Figure> figures = List.of(f1, f2, f3);

        List<Node> nodes = new TEIFormatter(null, null)
                .markReferencesFigureTEI(
                        input,
                        tokensWithOffset,
                        figures,
                        false);

        assertThat(nodes, hasSize(3));
        assertThat(nodes.get(0).toXML(), is("and"));
        assertThat(nodes.get(1).toXML(), is(" "));
        assertThat(
                ((Element) nodes.get(2)).toXML(),
                is("<ref xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"figure\">3D</ref>"));
    }

    @Test
    public void testMarkReferencesFigureTEI_truncatedRef_referenceAtBeginning() throws Exception {
        String input = "5, & ";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<LayoutToken> tokensWithOffset = tokens.stream()
                .peek(t -> t.setOffset(t.getOffset() + 51393))
                .collect(Collectors.toList());

        Figure f1 = new Figure();
        f1.setLabel(new StringBuilder("1"));
        Figure f2 = new Figure();
        f2.setLabel(new StringBuilder("2"));
        Figure f3 = new Figure();
        f3.setLabel(new StringBuilder(""));

        List<Figure> figures = List.of(f1, f2, f3);

        List<Node> nodes = new TEIFormatter(null, null)
                .markReferencesFigureTEI(input, tokensWithOffset, figures, false);

        assertThat(nodes, hasSize(6));
        assertThat(
                ((Element) nodes.get(0)).toXML(),
                is("<ref xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"figure\">5</ref>"));
        assertThat(nodes.get(1).toXML(), is(","));
        assertThat(nodes.get(2).toXML(), is(" "));
        assertThat(nodes.get(3).toXML(), is("&amp;"));
        assertThat(nodes.get(4).toXML(), is(""));
        assertThat(nodes.get(5).toXML(), is(" "));
    }

    @Test
    public void testMarkReferencesFigureTEI_truncatedRefWithComma_referenceAtTheEnd() throws Exception {
        String input = ", 3D";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<LayoutToken> tokensWithOffset = tokens.stream()
                .peek(t -> t.setOffset(t.getOffset() + 51393))
                .collect(Collectors.toList());

        Figure f1 = new Figure();
        f1.setLabel(new StringBuilder("1"));
        Figure f2 = new Figure();
        f2.setLabel(new StringBuilder("2"));
        Figure f3 = new Figure();
        f3.setLabel(new StringBuilder(""));

        List<Figure> figures = List.of(f1, f2, f3);

        List<Node> nodes = new TEIFormatter(null, null)
                .markReferencesFigureTEI(input, tokensWithOffset, figures, false);

        assertThat(nodes, hasSize(3));
        assertThat(nodes.get(0).toXML(), is(","));
        assertThat(nodes.get(1).toXML(), is(" "));
        assertThat(
                ((Element) nodes.get(2)).toXML(),
                is("<ref xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"figure\">3D</ref>"));
    }

    @Test
    public void testMarkReferencesFigureTEI_truncatedRefWithComma_referenceAtBeginning() throws Exception {
        String input = "5, ";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<LayoutToken> tokensWithOffset = tokens.stream()
                .peek(t -> t.setOffset(t.getOffset() + 51393))
                .collect(Collectors.toList());

        Figure f1 = new Figure();
        f1.setLabel(new StringBuilder("1"));
        Figure f2 = new Figure();
        f2.setLabel(new StringBuilder("2"));
        Figure f3 = new Figure();
        f3.setLabel(new StringBuilder(""));

        List<Figure> figures = List.of(f1, f2, f3);

        List<Node> nodes = new TEIFormatter(null, null)
                .markReferencesFigureTEI(input, tokensWithOffset, figures, false);

        assertThat(nodes, hasSize(4));
        assertThat(
                ((Element) nodes.get(0)).toXML(),
                is("<ref xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"figure\">5</ref>"));
        assertThat(nodes.get(1).toXML(), is(","));
        assertThat(nodes.get(2).toXML(), is(""));
        assertThat(nodes.get(3).toXML(), is(" "));
    }

    @Test
    public void testMarkReferencesTableTEI() throws Exception {
        String input = "3C and 3D";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<LayoutToken> tokensWithOffset = tokens.stream()
                .peek(t -> t.setOffset(t.getOffset() + 51393))
                .collect(Collectors.toList());

        Table t1 = new Table();
        t1.setLabel(new StringBuilder("1"));
        Table t2 = new Table();
        t2.setLabel(new StringBuilder("2"));
        Table t3 = new Table();
        t3.setLabel(new StringBuilder(""));

        List<Table> tables = List.of(t1, t2, t3);

        List<Node> nodes = new TEIFormatter(null, null)
                .markReferencesTableTEI(
                        input,
                        tokensWithOffset,
                        tables,
                        false);
        assertThat(nodes, hasSize(4));
        assertThat(
                ((Element) nodes.get(0)).toXML(),
                is("<ref xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"table\">3C</ref>"));
        assertThat(nodes.get(1).toXML(), is(" and"));
        assertThat(nodes.get(2).toXML(), is(" "));
        assertThat(
                ((Element) nodes.get(3)).toXML(),
                is("<ref xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"table\">3D</ref>"));
    }

    @Test
    public void testMarkReferencesTableTEI_truncatedRef_referenceAtTheEnd() throws Exception {
        String input = "and 3D";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<LayoutToken> tokensWithOffset = tokens.stream()
                .peek(t -> t.setOffset(t.getOffset() + 51393))
                .collect(Collectors.toList());

        Table t1 = new Table();
        t1.setLabel(new StringBuilder("1"));
        Table t2 = new Table();
        t2.setLabel(new StringBuilder("2"));
        Table t3 = new Table();
        t3.setLabel(new StringBuilder(""));

        List<Table> tables = List.of(t1, t2, t3);

        List<Node> nodes = new TEIFormatter(null, null)
                .markReferencesTableTEI(
                        input,
                        tokensWithOffset,
                        tables,
                        false);
        assertThat(nodes, hasSize(3));
        assertThat(nodes.get(0).toXML(), is("and"));
        assertThat(nodes.get(1).toXML(), is(" "));
        assertThat(
                ((Element) nodes.get(2)).toXML(),
                is("<ref xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"table\">3D</ref>"));
    }

    @Test
    public void testMarkReferencesTableTEI_truncatedRef_referenceAtBeginning() throws Exception {
        String input = "5, & ";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<LayoutToken> tokensWithOffset = tokens.stream()
                .peek(t -> t.setOffset(t.getOffset() + 51393))
                .collect(Collectors.toList());

        Table t1 = new Table();
        t1.setLabel(new StringBuilder("1"));
        Table t2 = new Table();
        t2.setLabel(new StringBuilder("2"));
        Table t3 = new Table();
        t3.setLabel(new StringBuilder(""));

        List<Table> tables = List.of(t1, t2, t3);

        List<Node> nodes = new TEIFormatter(null, null)
                .markReferencesTableTEI(
                        input,
                        tokensWithOffset,
                        tables,
                        false);

        assertThat(nodes, hasSize(6));
        assertThat(
                ((Element) nodes.get(0)).toXML(),
                is("<ref xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"table\">5</ref>"));
        assertThat(nodes.get(1).toXML(), is(","));
        assertThat(nodes.get(2).toXML(), is(" "));
        assertThat(nodes.get(3).toXML(), is("&amp;"));
        assertThat(nodes.get(4).toXML(), is(""));
        assertThat(nodes.get(5).toXML(), is(" "));
    }

    @Test
    public void testMarkReferencesTableTEI_truncatedRef2_referenceAtBeginning() throws Exception {
        String input = "5 , & ";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<LayoutToken> tokensWithOffset = tokens.stream()
                .peek(t -> t.setOffset(t.getOffset() + 51393))
                .collect(Collectors.toList());

        Table t1 = new Table();
        t1.setLabel(new StringBuilder("1"));
        Table t2 = new Table();
        t2.setLabel(new StringBuilder("2"));
        Table t3 = new Table();
        t3.setLabel(new StringBuilder(""));

        List<Table> tables = List.of(t1, t2, t3);

        List<Node> nodes = new TEIFormatter(null, null)
                .markReferencesTableTEI(input, tokensWithOffset, tables, false);

        assertThat(nodes, hasSize(6));
        assertThat(
                ((Element) nodes.get(0)).toXML(),
                is("<ref xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"table\">5</ref>"));
        assertThat(nodes.get(1).toXML(), is(" ,"));
        assertThat(nodes.get(2).toXML(), is(" "));
        assertThat(nodes.get(3).toXML(), is("&amp;"));
        assertThat(nodes.get(4).toXML(), is(""));
        assertThat(nodes.get(5).toXML(), is(" "));
    }

    // --- isNewParagraph: layout-based split after a callout (replaces #1482) ---
    // The two #1482 scenarios are kept here as the first two cases, now asserting the
    // corrected (layout-driven) contract instead of the isBeginning() one.

    @Test
    public void testIsNewParagraph_afterMarkerBeginningLabel_noLayoutEvidenceContinues() {
        // #1482 asserted this split because the resumed cluster carried isBeginning(). That
        // flag is uninformative after a callout, so with no layout to go on the paragraph
        // continues.
        TaggingTokenCluster paragraphCluster = new TaggingTokenCluster(TaggingLabels.PARAGRAPH);
        paragraphCluster.addLabeledTokensContainer(
                new LabeledTokensContainer(List.of(), "Because", TaggingLabels.PARAGRAPH, true));

        boolean isNewParagraph = TEIFormatter
                .isNewParagraph(TaggingLabels.CITATION_MARKER, new Element("p"), paragraphCluster, null);

        assertThat(isNewParagraph, is(false));
    }

    @Test
    public void testIsNewParagraph_afterMarkerContinuationLabel_continues() {
        TaggingTokenCluster paragraphCluster = new TaggingTokenCluster(TaggingLabels.PARAGRAPH);
        paragraphCluster.addLabeledTokensContainer(
                new LabeledTokensContainer(List.of(), "continuation", TaggingLabels.PARAGRAPH, false));

        boolean isNewParagraph = TEIFormatter
                .isNewParagraph(TaggingLabels.CITATION_MARKER, new Element("p"), paragraphCluster, null);

        assertThat(isNewParagraph, is(false));
    }

    @Test
    public void testIsNewParagraph_nonMarkerLabelAlwaysStartsANewParagraph() {
        assertThat(
                TEIFormatter.isNewParagraph(TaggingLabels.SECTION, new Element("p"), null, null),
                is(true));
    }

    @Test
    public void testIsNewParagraph_nullParagraphAlwaysStartsANewParagraph() {
        assertThat(
                TEIFormatter.isNewParagraph(TaggingLabels.CITATION_MARKER, null, null, null),
                is(true));
    }

    @Test
    public void testIsNewParagraph_twoArgOverloadKeeps090Behaviour() {
        // The Table path and any external caller use the 2-arg overload: never split after a
        // marker/figure/table, split after anything else.
        assertThat(TEIFormatter.isNewParagraph(TaggingLabels.CITATION_MARKER, new Element("p")), is(false));
        assertThat(TEIFormatter.isNewParagraph(TaggingLabels.FIGURE, new Element("p")), is(false));
        assertThat(TEIFormatter.isNewParagraph(TaggingLabels.SECTION, new Element("p")), is(true));
    }

    @Test
    public void testIsNewParagraph_textResumingOnTheSameLineContinuesTheParagraph() {
        List<LayoutToken> preceding = List.of(token("]", 300.0, 400.0, 0));
        TaggingTokenCluster cluster = clusterOf(token("Because", 310.0, 400.0, 0));

        assertThat(
                TEIFormatter.isNewParagraph(TaggingLabels.CITATION_MARKER, new Element("p"), cluster, preceding),
                is(false));
    }

    @Test
    public void testIsNewParagraph_newLineInTheSameBlockContinuesTheParagraph() {
        List<LayoutToken> preceding = List.of(token("]", 500.0, 400.0, 7));
        TaggingTokenCluster cluster = clusterOf(token("Because", 72.0, 412.0, 7));

        assertThat(
                TEIFormatter.isNewParagraph(TaggingLabels.CITATION_MARKER, new Element("p"), cluster, preceding),
                is(false));
    }

    @Test
    public void testIsNewParagraph_newBlockOnANewLineStartsANewParagraph() {
        List<LayoutToken> preceding = List.of(token("]", 500.0, 400.0, 7));
        TaggingTokenCluster cluster = clusterOf(token("Because", 72.0, 412.0, 8));

        assertThat(
                TEIFormatter.isNewParagraph(TaggingLabels.CITATION_MARKER, new Element("p"), cluster, preceding),
                is(true));
    }

    @Test
    public void testIsNewParagraph_newBlockResumingOnLowerCaseContinuesTheParagraph() {
        List<LayoutToken> preceding = List.of(token("]", 500.0, 400.0, 7));
        TaggingTokenCluster cluster = clusterOf(token("because", 72.0, 412.0, 8));

        assertThat(
                TEIFormatter.isNewParagraph(TaggingLabels.CITATION_MARKER, new Element("p"), cluster, preceding),
                is(false));
    }

    @Test
    public void testIsNewParagraph_newBlockResumingOnPunctuationContinuesTheParagraph() {
        List<LayoutToken> preceding = List.of(token("]", 500.0, 400.0, 7));
        TaggingTokenCluster cluster = clusterOf(token(".", 72.0, 412.0, 8));

        assertThat(
                TEIFormatter.isNewParagraph(TaggingLabels.CITATION_MARKER, new Element("p"), cluster, preceding),
                is(false));
    }

    @Test
    public void testIsNewParagraph_textWrappingToTheTopOfTheNextColumnContinuesTheParagraph() {
        List<LayoutToken> preceding = List.of(token("]", 280.0, 700.0, 7));
        TaggingTokenCluster cluster = clusterOf(token("Because", 320.0, 90.0, 8));

        assertThat(
                TEIFormatter.isNewParagraph(TaggingLabels.CITATION_MARKER, new Element("p"), cluster, preceding),
                is(false));
    }

    @Test
    public void testIsNewParagraph_acrossAPageBreakContinuesTheParagraph() {
        List<LayoutToken> preceding = List.of(pageToken("]", 280.0, 700.0, 7, 4));
        TaggingTokenCluster cluster = clusterOf(pageToken("Because", 72.0, 90.0, 8, 5));

        assertThat(
                TEIFormatter.isNewParagraph(TaggingLabels.CITATION_MARKER, new Element("p"), cluster, preceding),
                is(false));
    }

    @Test
    public void testIsNewParagraph_unpositionedTrailingSpaceIsSkipped() {
        LayoutToken blank = new LayoutToken();
        blank.setText(" ");
        blank.setX(-1.0);
        blank.setY(-1.0);
        List<LayoutToken> preceding = List.of(token("]", 300.0, 400.0, 7), blank);
        TaggingTokenCluster cluster = clusterOf(token("Because", 310.0, 400.0, 7));

        assertThat(
                TEIFormatter.isNewParagraph(TaggingLabels.CITATION_MARKER, new Element("p"), cluster, preceding),
                is(false));
    }

    private static LayoutToken token(String text, double x, double y, int blockPtr) {
        return pageToken(text, x, y, blockPtr, 1);
    }

    private static LayoutToken pageToken(String text, double x, double y, int blockPtr, int page) {
        LayoutToken t = new LayoutToken();
        t.setText(text);
        t.setX(x);
        t.setY(y);
        t.setHeight(10.0);
        t.setBlockPtr(blockPtr);
        t.setPage(page);
        return t;
    }

    private static TaggingTokenCluster clusterOf(LayoutToken... tokens) {
        TaggingTokenCluster cluster = new TaggingTokenCluster(TaggingLabels.PARAGRAPH);
        cluster.addLabeledTokensContainer(
                new LabeledTokensContainer(
                        List.of(tokens), tokens[0].getText(), TaggingLabels.PARAGRAPH, true));
        return cluster;
    }
}
