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
import org.grobid.core.document.TEIFormatter.SectionGroupingInfo;
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

    private static TaggingTokenCluster sectionCluster(String text) {
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        TaggingTokenCluster cluster = new TaggingTokenCluster(TaggingLabels.SECTION);
        cluster.addLabeledTokensContainer(
                new LabeledTokensContainer(tokens, text, TaggingLabels.SECTION, true));
        return cluster;
    }

    private static TaggingTokenCluster paragraphCluster(String text) {
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        TaggingTokenCluster cluster = new TaggingTokenCluster(TaggingLabels.PARAGRAPH);
        cluster.addLabeledTokensContainer(
                new LabeledTokensContainer(tokens, text, TaggingLabels.PARAGRAPH, true));
        return cluster;
    }

    // outline: root -> title -> { "2 Methods" -> "2.1 Data", "3 Results" -> "3.1 Findings" }
    private static DocumentNode buildOutline() {
        DocumentNode root = new DocumentNode();
        DocumentNode title = new DocumentNode("Some Article Title", null);
        DocumentNode methods = new DocumentNode("2 Methods", null);
        DocumentNode data = new DocumentNode("2.1 Data", null);
        DocumentNode results = new DocumentNode("3 Results", null);
        DocumentNode findings = new DocumentNode("3.1 Findings", null);
        root.addChild(title);
        title.addChild(methods);
        methods.addChild(data);
        title.addChild(results);
        results.addChild(findings);
        return root;
    }

    @Test
    public void testComputeSectionNodes_inactiveWithoutAdjacentHeads() {
        TaggingTokenCluster methods = sectionCluster("2 Methods");
        TaggingTokenCluster para = paragraphCluster("Some body text.");
        TaggingTokenCluster data = sectionCluster("2.1 Data");

        SectionGroupingInfo info = TEIFormatter.computeSectionNodes(
                List.of(methods, para, data),
                buildOutline());

        // no two section heads are adjacent, so the mechanism stays off
        assertThat(info.active, is(false));
    }

    @Test
    public void testComputeSectionNodes_inactiveWithoutOutline() {
        TaggingTokenCluster methods = sectionCluster("2 Methods");
        TaggingTokenCluster data = sectionCluster("2.1 Data");

        SectionGroupingInfo info = TEIFormatter.computeSectionNodes(List.of(methods, data), null);

        assertThat(info.active, is(false));
    }

    @Test
    public void testComputeSectionNodes_subHeadIsDescendantOfMainHead() {
        TaggingTokenCluster methods = sectionCluster("2 Methods");
        TaggingTokenCluster data = sectionCluster("2.1 Data");
        TaggingTokenCluster para = paragraphCluster("Body of the data section.");

        SectionGroupingInfo info = TEIFormatter.computeSectionNodes(
                List.of(methods, data, para),
                buildOutline());

        assertThat(info.active, is(true));
        // the sub-head is a descendant of the main head -> it folds into the same div
        assertThat(
                DocumentNode.isDescendantOf(info.nodes.get(data), info.nodes.get(methods)),
                is(true));
    }

    @Test
    public void testComputeSectionNodes_missedParentHeadDoesNotFoldSubHead() {
        // GROBID missed the "3 Results" heading: its sub-head "3.1 Findings" appears right after the
        // "2 Methods" section. It must NOT be treated as a descendant of Methods (else it would fold
        // into the Methods div), so grouping degrades to opening a fresh div for it.
        TaggingTokenCluster methods = sectionCluster("2 Methods");
        TaggingTokenCluster data = sectionCluster("2.1 Data");
        TaggingTokenCluster findings = sectionCluster("3.1 Findings");

        SectionGroupingInfo info = TEIFormatter.computeSectionNodes(
                List.of(methods, data, findings),
                buildOutline());

        assertThat(info.active, is(true));
        assertThat(
                DocumentNode.isDescendantOf(info.nodes.get(findings), info.nodes.get(methods)),
                is(false));
    }

    @Test
    public void testFindOutlineNodeForHead_matchesWhenOutlineOmitsSectionNumber() {
        // Many outlines store the plain title while the detected head carries a number. A short
        // title like "Results" scores below the similarity threshold against "2. Results", so the
        // number-stripped fallback is what makes the parent head matchable at all.
        DocumentNode root = new DocumentNode();
        DocumentNode results = new DocumentNode("Results", null);
        DocumentNode sub = new DocumentNode("Design of Primers for U. virens Detection", null);
        root.addChild(results);
        results.addChild(sub);

        assertThat(TEIFormatter.findOutlineNodeForHead(root, "2. Results"), is(results));
        // the long sub-head still matches directly, number prefix and all
        assertThat(
                TEIFormatter.findOutlineNodeForHead(root, "2.1. Design of Primers for U. virens Detection"),
                is(sub));
        // and the sub-head is a descendant of the now-matched parent -> it can fold in
        assertThat(DocumentNode.isDescendantOf(sub, results), is(true));
    }

    @Test
    public void testNumberingContradictsNesting() {
        // consistent nesting -> does not contradict
        assertThat(TEIFormatter.numberingContradictsNesting("2", "2.1"), is(false));
        assertThat(TEIFormatter.numberingContradictsNesting("2.", "2.3.1"), is(false));
        assertThat(TEIFormatter.numberingContradictsNesting("3.1", "3.1.2"), is(false));
        // sibling or unrelated -> contradicts (guards against a degenerate outline)
        assertThat(TEIFormatter.numberingContradictsNesting("2", "3"), is(true));
        assertThat(TEIFormatter.numberingContradictsNesting("2", "3.1"), is(true));
        assertThat(TEIFormatter.numberingContradictsNesting("2.1", "2.2"), is(true));
        assertThat(TEIFormatter.numberingContradictsNesting("2", "2"), is(true));
        // missing / non-decimal numbering (roman, letter, none) -> defer to the outline, no contradiction
        assertThat(TEIFormatter.numberingContradictsNesting(null, "2.1"), is(false));
        assertThat(TEIFormatter.numberingContradictsNesting("II", "1"), is(false));
        assertThat(TEIFormatter.numberingContradictsNesting("A", "2"), is(false));
    }

    @Test
    public void testComputeSectionNodes_unmatchedHeadNotRecorded() {
        TaggingTokenCluster methods = sectionCluster("2 Methods");
        TaggingTokenCluster data = sectionCluster("2.1 Data");
        // a head that does not exist in the outline: must not be recorded (falls back to a new div)
        TaggingTokenCluster unknown = sectionCluster("Appendix Z Nonexistent");

        SectionGroupingInfo info = TEIFormatter.computeSectionNodes(
                List.of(methods, data, unknown),
                buildOutline());

        assertThat(info.active, is(true));
        assertThat(info.nodes.containsKey(unknown), is(false));
    }

}
