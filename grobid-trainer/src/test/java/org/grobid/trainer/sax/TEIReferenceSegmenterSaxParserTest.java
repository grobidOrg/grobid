package org.grobid.trainer.sax;

import org.junit.Before;
import org.junit.Test;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

public class TEIReferenceSegmenterSaxParserTest {

    private TEIReferenceSegmenterSaxParser target;
    private SAXParserFactory spf;

    @Before
    public void setUp() {
        spf = SAXParserFactory.newInstance();
        target = new TEIReferenceSegmenterSaxParser();
    }

    private List<String> parse(String tei) throws Exception {
        SAXParser p = spf.newSAXParser();
        p.parse(new ByteArrayInputStream(tei.getBytes(StandardCharsets.UTF_8)), target);
        return target.getLabeledResult();
    }

    /**
     * Regression: '@' is in TextUtilities.fullPunctuations, so the punctuation
     * tokenizer used to shred the "@newline" sentinel into "@" and "newline",
     * each then mislabeled with the current tag. After the fix, the marker
     * survives as a single "@newline" entry and no labeled row starts with
     * "@ " or "newline ".
     */
    @Test
    public void newlineMarker_isPreservedAcrossPunctuationTokenizer() throws Exception {
        String tei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\"><text><listBibl>"
            + "<bibl><label>1</label><lb/> Final Report, Recommendations,<lb/>"
            + " Independent Commission on Banking,<lb/> September 2011.<lb/> </bibl>"
            + "</listBibl></text></TEI>";

        List<String> labeled = parse(tei);

        assertThat(labeled, hasItem(is("@newline")));
        assertThat(labeled, everyItem(not(startsWith("@ "))));
        assertThat(labeled, everyItem(not(startsWith("newline "))));
        assertThat(labeled, hasItems(
            is("1 I-<label>"),
            is("Final I-<reference>"),
            is("Report <reference>")
        ));
        assertThat(target.getTotalReferences(), is(1));
    }

    /**
     * Law-footnotes pattern: multiple <bibl> elements share an implicit footnote
     * number — the second <bibl> has no <label>. Its content must still be
     * labeled <reference> end-to-end (label propagation happens at runtime
     * elsewhere), with no stray "@"/"newline" rows.
     */
    @Test
    public void biblWithoutLabel_tagsContentAsReference() throws Exception {
        String tei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\"><text><listBibl>"
            + "<bibl><label>3</label><lb/> First citation here.<lb/> </bibl>"
            + "<bibl>Second citation, no label.<lb/> </bibl>"
            + "</listBibl></text></TEI>";

        List<String> labeled = parse(tei);

        assertThat(labeled, everyItem(not(startsWith("@ "))));
        assertThat(labeled, everyItem(not(startsWith("newline "))));
        assertThat(labeled, hasItems(
            is("3 I-<label>"),
            is("First I-<reference>"),
            is("Second I-<reference>"),
            is("citation <reference>")
        ));
        assertThat(target.getTotalReferences(), is(2));
    }

    /**
     * The new-style TEI training files (e.g. dh-law-footnotes flavor) carry a full
     * <teiHeader> with <sourceDesc><bibl>, application <label>s ("GROBID",
     * "PDF-TEI Editor") and lots of free text. None of that is part of the
     * reference list; it must not be counted toward totalReferences and must not
     * leak tokens into the labeled output.
     */
    @Test
    public void teiHeaderContent_isIgnored() throws Exception {
        String tei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">"
            + "<teiHeader>"
            + "  <fileDesc>"
            + "    <titleStmt><title>HeaderTitleToken</title></titleStmt>"
            + "    <sourceDesc>"
            + "      <bibl>HeaderBiblToken in sourceDesc must not be counted.</bibl>"
            + "    </sourceDesc>"
            + "  </fileDesc>"
            + "  <encodingDesc>"
            + "    <appInfo><application><label>GROBID</label></application></appInfo>"
            + "  </encodingDesc>"
            + "</teiHeader>"
            + "<text><listBibl>"
            + "<bibl><label>1</label> RealRefToken here.<lb/> </bibl>"
            + "</listBibl></text></TEI>";

        List<String> labeled = parse(tei);

        assertThat(target.getTotalReferences(), is(1));
        assertThat(labeled, everyItem(not(containsString("HeaderTitleToken"))));
        assertThat(labeled, everyItem(not(containsString("HeaderBiblToken"))));
        assertThat(labeled, everyItem(not(containsString("GROBID"))));
        assertThat(labeled, hasItems(
            is("1 I-<label>"),
            is("RealRefToken I-<reference>")
        ));
    }
}
