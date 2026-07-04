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
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.Affiliation;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.features.FeaturesVectorAffiliationAddress;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.OffsetPosition;

public class AffiliationAddressParserTest {

    public static final Logger LOGGER = LoggerFactory.getLogger(AffiliationAddressParserTest.class);

    private static boolean NO_USE_PRELABEL = false;
    private static List<List<OffsetPosition>> NO_PLACES_POSITIONS = Arrays.asList(
            Collections.emptyList());

    private AffiliationAddressParser target;
    private GrobidAnalyzer analyzer;

    @Before
    public void setUp() throws Exception {
        this.target = new AffiliationAddressParser(GrobidModels.DUMMY);
        this.analyzer = GrobidAnalyzer.getInstance();
    }

    @BeforeClass
    public static void init() {
        //        LibraryLoader.load();
        GrobidProperties.getInstance();
    }

    @AfterClass
    public static void tearDown() {
        GrobidFactory.reset();
    }

    @Test
    public void shouldNotFailOnEmptyLabelResult() throws Exception {
        String labelResult = "";
        List<LayoutToken> tokenizations = Collections.emptyList();
        List<Affiliation> result = this.target.resultBuilder(
                labelResult,
                tokenizations,
                NO_USE_PRELABEL);
        assertThat("affiliations should be null", result, is(nullValue()));
    }

    private static List<String> getAffiliationBlocksWithLineFeed(List<LayoutToken> tokenizations) {
        ArrayList<String> affiliationBlocks = new ArrayList<String>();
        for (LayoutToken tok : tokenizations) {
            if (tok.getText().length() == 0)
                continue;
            if (!tok.getText().equals(" ")) {
                if (tok.getText().equals("\n")) {
                    affiliationBlocks.add("@newline");
                } else
                    affiliationBlocks.add(tok + " <affiliation>");
            }
        }
        return affiliationBlocks;
    }

    private static String addLabelsToFeatures(String header, List<String> labels) {
        String[] headerLines = header.split("\n");
        if (headerLines.length != labels.size()) {
            throw new IllegalArgumentException(String.format(
                    "number of header lines and labels must match, %d != %d",
                    headerLines.length,
                    labels.size()));
        }
        ArrayList<String> resultLines = new ArrayList<>(headerLines.length);
        for (int i = 0; i < headerLines.length; i++) {
            resultLines.add(headerLines[i] + " " + labels.get(i));
        }
        return Joiner.on("\n").join(resultLines);
    }

    private List<Affiliation> processLabelResults(
            List<String> tokens,
            List<String> labels) throws Exception {
        List<LayoutToken> tokenizations = LayoutTokensUtil.getLayoutTokensForTokenizedText(tokens);
        LOGGER.debug("tokenizations: {}", tokenizations);
        List<String> affiliationBlocks = getAffiliationBlocksWithLineFeed(tokenizations);
        String header = FeaturesVectorAffiliationAddress.addFeaturesAffiliationAddress(
                affiliationBlocks,
                Arrays.asList(tokenizations),
                NO_PLACES_POSITIONS,
                NO_PLACES_POSITIONS);
        LOGGER.debug("header: {}", header);
        String labelResult = addLabelsToFeatures(header, labels);
        LOGGER.debug("labelResult: {}", labelResult);
        return this.target.resultBuilder(
                labelResult,
                tokenizations,
                NO_USE_PRELABEL);
    }

    private List<Affiliation> processLabelResults(String[][] tokenLabelPairs) throws Exception {
        ArrayList<String> tokens = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        boolean prevWhitespace = false;
        for (String[] pair : tokenLabelPairs) {
            if (!tokens.isEmpty() && (!prevWhitespace)) {
                tokens.add(" ");
            }
            prevWhitespace = pair[0].trim().isEmpty();
            tokens.add(pair[0]);
            if (pair.length > 1) {
                labels.add(pair[1]);
            }
        }
        return this.processLabelResults(tokens, labels);
    }

    @Test
    public void shouldExtractSimpleAffiliation() throws Exception {
        List<Affiliation> affiliations = this.processLabelResults(
                new String[][]{
                        {"1", "I-<marker>"},
                        {"University", "I-<institution>"},
                        {"of", "<institution>"},
                        {"Science", "<institution>"}
                });
        assertThat("should have one affiliation", affiliations, is(hasSize(1)));
        Affiliation affiliation = affiliations.get(0);
        assertThat("institution.marker", affiliation.getMarker(), is("1"));
        assertThat(
                "institution.institutions",
                affiliation.getInstitutions(),
                is(Arrays.asList("University of Science")));
        assertThat(
                "institution.rawAffiliationString",
                affiliation.getRawAffiliationString(),
                is("University of Science"));
    }

    @Test
    public void shouldExtractMultipleInstitutions() throws Exception {
        List<Affiliation> affiliations = this.processLabelResults(
                new String[][]{
                        {"1", "I-<marker>"},
                        {"University", "I-<institution>"},
                        {"of", "<institution>"},
                        {"Science", "<institution>"},
                        {"University", "I-<institution>"},
                        {"of", "<institution>"},
                        {"Madness", "<institution>"}
                });
        assertThat("should have one affiliation", affiliations, is(hasSize(1)));
        Affiliation affiliation = affiliations.get(0);
        assertThat("institution.marker", affiliation.getMarker(), is("1"));
        assertThat(
                "institution.institutions",
                affiliation.getInstitutions(),
                is(Arrays.asList("University of Science", "University of Madness")));
        assertThat(
                "institution.rawAffiliationString",
                affiliation.getRawAffiliationString(),
                is("University of Science University of Madness"));
    }

    @Test
    public void shouldExtractSecondInstitutionAsSeparateAffiliationIfNewLine() throws Exception {
        List<Affiliation> affiliations = this.processLabelResults(
                new String[][]{
                        {"1", "I-<marker>"},
                        {"University", "I-<institution>"},
                        {"of", "<institution>"},
                        {"Science", "<institution>"},
                        {"\n"},
                        {"University", "I-<institution>"},
                        {"of", "<institution>"},
                        {"Madness", "<institution>"}
                });
        assertThat("should have one affiliation", affiliations, is(hasSize(2)));
        assertThat("(0).institution.marker", affiliations.get(0).getMarker(), is("1"));
        assertThat(
                "(0).institution.institutions",
                affiliations.get(0).getInstitutions(),
                is(Arrays.asList("University of Science")));
        assertThat(
                "(0).institution.rawAffiliationString",
                affiliations.get(0).getRawAffiliationString(),
                is("University of Science"));
        assertThat("(1).institution.marker", affiliations.get(1).getMarker(), is("1"));
        assertThat(
                "(1).institution.institutions",
                affiliations.get(1).getInstitutions(),
                is(Arrays.asList("University of Madness")));
        assertThat(
                "(1).institution.rawAffiliationString",
                affiliations.get(1).getRawAffiliationString(),
                is("University of Madness"));
    }

    @Test
    public void shouldExtractMultipleAffiliations() throws Exception {
        List<Affiliation> affiliations = this.processLabelResults(
                new String[][]{
                        {"1", "I-<marker>"},
                        {"University", "I-<institution>"},
                        {"of", "<institution>"},
                        {"Science", "<institution>"},
                        {"2", "I-<marker>"},
                        {"University", "I-<institution>"},
                        {"of", "<institution>"},
                        {"Madness", "<institution>"}
                });
        assertThat("should have one affiliation", affiliations, is(hasSize(2)));
        assertThat("institution.marker", affiliations.get(0).getMarker(), is("1"));
        assertThat(
                "institution.institutions",
                affiliations.get(0).getInstitutions(),
                is(Arrays.asList("University of Science")));
        assertThat(
                "institution.rawAffiliationString",
                affiliations.get(0).getRawAffiliationString(),
                is("University of Science"));
        assertThat("institution.marker", affiliations.get(1).getMarker(), is("2"));
        assertThat(
                "institution.institutions",
                affiliations.get(1).getInstitutions(),
                is(Arrays.asList("University of Madness")));
        assertThat(
                "institution.rawAffiliationString",
                affiliations.get(1).getRawAffiliationString(),
                is("University of Madness"));
    }

    @Test
    @Ignore("This test is used to show the failing input data")
    public void testResultExtractionLayoutTokensFromDLOutput() throws Exception {
        String result = "\n"
                +
                "\n"
                +
                "Department\tdepartment\tD\tDe\tDep\tDepa\tt\tnt\tent\tment\tLINESTART\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<department>\n"
                +
                "of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t1\t0\t1\t0\tNOPUNCT\txx\t<affiliation>\t<department>\n"
                +
                "Radiation\tradiation\tR\tRa\tRad\tRadi\tn\ton\tion\ttion\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n"
                +
                "Oncology\toncology\tO\tOn\tOnc\tOnco\ty\tgy\togy\tlogy\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n"
                +
                ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\t<other>\n"
                +
                "San\tsan\tS\tSa\tSan\tSan\tn\tan\tSan\tSan\tLINESTART\tINITCAP\tNODIGIT\t0\t0\t0\t0\t1\t0\tNOPUNCT\tXxx\t<affiliation>\tI-<institution>\n"
                +
                "Camillo\tcamillo\tC\tCa\tCam\tCami\to\tlo\tllo\tillo\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n"
                +
                "-\t-\t-\t-\t-\t-\t-\t-\t-\t-\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tHYPHEN\t-\t<affiliation>\t<institution>\n"
                +
                "Forlanini\tforlanini\tF\tFo\tFor\tForl\ti\tni\tini\tnini\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n"
                +
                "Hospital\thospital\tH\tHo\tHos\tHosp\tl\tal\ttal\tital\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n"
                +
                ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\t<other>\n"
                +
                "Circonvallazione\tcirconvallazione\tC\tCi\tCir\tCirc\te\tne\tone\tione\tLINESTART\tINITCAP\tNODIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<addrLine>\n"
                +
                "Gianicolense\tgianicolense\tG\tGi\tGia\tGian\te\tse\tnse\tense\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<addrLine>\n"
                +
                ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\t<other>\n"
                +
                "87\t87\t8\t87\t87\t87\t7\t87\t87\t87\tLINESTART\tNOCAPS\tALLDIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tdd\t<affiliation>\tI-<addrLine>\n"
                +
                "-\t-\t-\t-\t-\t-\t-\t-\t-\t-\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tHYPHEN\t-\t<affiliation>\t<addrLine>\n"
                +
                "00152\t00152\t0\t00\t001\t0015\t2\t52\t152\t0152\tLINEIN\tNOCAPS\tALLDIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tdddd\t<affiliation>\t<addrLine>\n"
                +
                ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\t<other>\n"
                +
                "Rome\trome\tR\tRo\tRom\tRome\te\tme\tome\tRome\tLINEIN\tINITCAP\tNODIGIT\t0\t1\t0\t0\t1\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<settlement>\n"
                +
                ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t1\t0\tCOMMA\t,\t<affiliation>\t<other>\n"
                +
                "Italy\titaly\tI\tIt\tIta\tItal\ty\tly\taly\ttaly\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t1\t1\tNOPUNCT\tXxxx\t<affiliation>\tI-<country>\n"
                +
                ";\t;\t;\t;\t;\t;\t;\t;\t;\t;\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tPUNCT\t;\t<affiliation>\t<country>\n";

        List<LayoutToken> tokenizations = Arrays.stream(result.split("\n"))
                .map(row -> new LayoutToken(row.split("\t")[0]))
                .collect(Collectors.toList());

        assertThat(target.resultExtractionLayoutTokens(result, tokenizations), hasSize(greaterThan(0)));
    }

    @Test
    public void testResultExtractionLayoutTokensFromCRFOutput() throws Exception {
        String result = "MD\tmd\tM\tMD\tMD\tMD\tD\tMD\tMD\tMD\tLINESTART\tALLCAPS\tNODIGIT\t0\t0\t0\t0\t1\t0\tNOPUNCT\tXX\t<affiliation>\tI-<institution>\n"
                +
                ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n"
                +
                "Department\tdepartment\tD\tDe\tDep\tDepa\tt\tnt\tent\tment\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<department>\n"
                +
                "of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t1\t0\t1\t0\tNOPUNCT\txx\t<affiliation>\t<department>\n"
                +
                "Radiation\tradiation\tR\tRa\tRad\tRadi\tn\ton\tion\ttion\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n"
                +
                "Oncology\toncology\tO\tOn\tOnc\tOnco\ty\tgy\togy\tlogy\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n"
                +
                ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n"
                +
                "San\tsan\tS\tSa\tSan\tSan\tn\tan\tSan\tSan\tLINESTART\tINITCAP\tNODIGIT\t0\t0\t0\t0\t1\t0\tNOPUNCT\tXxx\t<affiliation>\tI-<institution>\n"
                +
                "Camillo\tcamillo\tC\tCa\tCam\tCami\to\tlo\tllo\tillo\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n"
                +
                "-\t-\t-\t-\t-\t-\t-\t-\t-\t-\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tHYPHEN\t-\t<affiliation>\t<institution>\n"
                +
                "Forlanini\tforlanini\tF\tFo\tFor\tForl\ti\tni\tini\tnini\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n"
                +
                "Hospital\thospital\tH\tHo\tHos\tHosp\tl\tal\ttal\tital\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n"
                +
                ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n"
                +
                "Circonvallazione\tcirconvallazione\tC\tCi\tCir\tCirc\te\tne\tone\tione\tLINESTART\tINITCAP\tNODIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<addrLine>\n"
                +
                "Gianicolense\tgianicolense\tG\tGi\tGia\tGian\te\tse\tnse\tense\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<addrLine>\n"
                +
                ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n"
                +
                "87\t87\t8\t87\t87\t87\t7\t87\t87\t87\tLINESTART\tNOCAPS\tALLDIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tdd\t<affiliation>\tI-<postCode>\n"
                +
                "-\t-\t-\t-\t-\t-\t-\t-\t-\t-\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tHYPHEN\t-\t<affiliation>\t<postCode>\n"
                +
                "00152\t00152\t0\t00\t001\t0015\t2\t52\t152\t0152\tLINEIN\tNOCAPS\tALLDIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tdddd\t<affiliation>\t<postCode>\n"
                +
                ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n"
                +
                "Rome\trome\tR\tRo\tRom\tRome\te\tme\tome\tRome\tLINEIN\tINITCAP\tNODIGIT\t0\t1\t0\t0\t1\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<settlement>\n"
                +
                ",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t1\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n"
                +
                "Italy\titaly\tI\tIt\tIta\tItal\ty\tly\taly\ttaly\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t1\t1\tNOPUNCT\tXxxx\t<affiliation>\tI-<country>\n"
                +
                ";\t;\t;\t;\t;\t;\t;\t;\t;\t;\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tPUNCT\t;\t<affiliation>\t<country>";

        List<LayoutToken> tokenizations = Arrays.stream(result.split("\n"))
                .map(row -> new LayoutToken(row.split("\t")[0]))
                .collect(Collectors.toList());

        assertThat(target.resultExtractionLayoutTokens(result, tokenizations), hasSize(greaterThan(0)));
    }

    @Test
    public void testGetAffiliationBlocksFromSegments_1() throws Exception {
        String block1 = "Department of science, University of Science, University of Madness";
        List<LayoutToken> tokBlock1 = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(block1);
        tokBlock1.stream().forEach(t -> t.setOffset(t.getOffset() + 100));

        String block2 = "Department of mental health, University of happyness, Italy";
        List<LayoutToken> tokBlock2 = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(block2);
        tokBlock2.stream().forEach(t -> t.setOffset(t.getOffset() + 500));

        List<String> affiliationBlocksFromSegments = AffiliationAddressParser
                .getAffiliationBlocksFromSegments(Arrays.asList(tokBlock1, tokBlock2));

        assertThat(affiliationBlocksFromSegments, hasSize(22));
        assertThat(affiliationBlocksFromSegments.get(0), is(not(startsWith("\n"))));
        assertThat(affiliationBlocksFromSegments.get(11), is("\n"));
    }

    /**
     * Reproduces arXiv 2006.11386: three HEADER &lt;affiliation&gt; segments
     * (block1 dept+inst, block2 inst, block3 dept+inst) get flattened into
     * one labeled stream by {@link AffiliationAddressParser#processingLayoutTokens}.
     * The offset gap between segments mirrors what
     * {@code getAffiliationBlocksFromSegments} marks with a {@code "\n"}
     * separator (gap &gt; 2). Without the segment-boundary force-split in
     * {@code resultExtractionLayoutTokens}, block 3's DEPARTMENT cluster
     * silently attaches to block 2's INSTITUTION because the per-label split
     * logic only fires on label repetition. This pins the post-fix behavior:
     * exactly three Affiliation objects, each with its own block's content.
     */
    @Test
    public void testResultExtractionLayoutTokens_segmentBoundaryForceSplit() throws Exception {
        String result = "Department\tdepartment\tD\tDe\tDep\tDepa\tt\tnt\tent\tment\tLINESTART\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<department>\n"
                + "of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t1\t0\t1\t0\tNOPUNCT\txx\t<affiliation>\t<department>\n"
                + "Computer\tcomputer\tC\tCo\tCom\tComp\tr\ter\tter\tuter\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n"
                + "Science\tscience\tS\tSc\tSci\tScie\te\tce\tnce\tence\tLINEEND\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n"
                + "University\tuniversity\tU\tUn\tUni\tUniv\ty\tty\tity\tsity\tLINESTART\tINITCAP\tNODIGIT\t0\t0\t1\t0\t1\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<institution>\n"
                + "of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t1\t0\t1\t0\tNOPUNCT\txx\t<affiliation>\t<institution>\n"
                + "British\tbritish\tB\tBr\tBri\tBrit\th\tsh\tish\ttish\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t1\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n"
                + "Columbia\tcolumbia\tC\tCo\tCol\tColu\ta\tia\tbia\tmbia\tLINEEND\tINITCAP\tNODIGIT\t0\t0\t0\t0\t1\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n"
                + "Data\tdata\tD\tDa\tDat\tData\ta\tta\tata\tData\tLINESTART\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<institution>\n"
                + "Science\tscience\tS\tSc\tSci\tScie\te\tce\tnce\tence\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n"
                + "Institute\tinstitute\tI\tIn\tIns\tInst\te\tte\tute\ttute\tLINEEND\tINITCAP\tNODIGIT\t0\t0\t1\t0\t1\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n"
                + "Columbia\tcolumbia\tC\tCo\tCol\tColu\ta\tia\tbia\tmbia\tLINESTART\tINITCAP\tNODIGIT\t0\t0\t0\t0\t1\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n"
                + "University\tuniversity\tU\tUn\tUni\tUniv\ty\tty\tity\tsity\tLINEEND\tINITCAP\tNODIGIT\t0\t0\t1\t0\t1\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n"
                + "Department\tdepartment\tD\tDe\tDep\tDepa\tt\tnt\tent\tment\tLINESTART\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<department>\n"
                + "of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t1\t0\t1\t0\tNOPUNCT\txx\t<affiliation>\t<department>\n"
                + "Computer\tcomputer\tC\tCo\tCom\tComp\tr\ter\tter\tuter\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n"
                + "Science\tscience\tS\tSc\tSci\tScie\te\tce\tnce\tence\tLINEEND\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n"
                + "University\tuniversity\tU\tUn\tUni\tUniv\ty\tty\tity\tsity\tLINESTART\tINITCAP\tNODIGIT\t0\t0\t1\t0\t1\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<institution>\n"
                + "of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t1\t0\t1\t0\tNOPUNCT\txx\t<affiliation>\t<institution>\n"
                + "British\tbritish\tB\tBr\tBri\tBrit\th\tsh\tish\ttish\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t1\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n"
                + "Columbia\tcolumbia\tC\tCo\tCol\tColu\ta\tia\tbia\tmbia\tLINEEND\tINITCAP\tNODIGIT\t0\t0\t0\t0\t1\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>";

        // Per-token offsets that simulate three HEADER segments far apart in
        // the original document: the gap between segments is > 2 (the same
        // threshold used by getAffiliationBlocksFromSegments).
        // Block 1 indices 0..7, block 2 indices 8..12, block 3 indices 13..20.
        int[] segmentBoundaries = {8, 13};
        List<LayoutToken> tokenizations = new ArrayList<>();
        String[] rows = result.split("\n");
        int currentOffset = 0;
        for (int i = 0; i < rows.length; i++) {
            if (i == segmentBoundaries[0] || i == segmentBoundaries[1]) {
                currentOffset += 100; // gap > 2 → triggers segment boundary
            }
            String text = rows[i].split("\t")[0];
            LayoutToken tok = new LayoutToken(text);
            tok.setOffset(currentOffset);
            tokenizations.add(tok);
            currentOffset += text.length() + 1;
        }

        List<Affiliation> result2 = target.resultExtractionLayoutTokens(result, tokenizations);

        assertThat(result2, hasSize(3));

        // Block 1: dept "Department of Computer Science" + inst "University of British Columbia"
        Affiliation aff0 = result2.get(0);
        assertThat(aff0.getDepartments(), hasSize(1));
        assertThat(aff0.getDepartments().get(0), containsString("Department"));
        assertThat(aff0.getInstitutions(), hasSize(1));
        assertThat(aff0.getInstitutions().get(0), containsString("British"));
        assertThat(aff0.getInstitutions().get(0), containsString("Columbia"));

        // Block 2: inst only "Data Science Institute Columbia University"
        Affiliation aff1 = result2.get(1);
        assertThat(aff1.getInstitutions(), hasSize(1));
        assertThat(aff1.getInstitutions().get(0), containsString("Data"));
        assertThat(aff1.getInstitutions().get(0), containsString("Institute"));
        // Critical: block 3's department must NOT have leaked onto this affiliation.
        assertThat(aff1.getDepartments(), is(nullValue()));

        // Block 3: dept "Department of Computer Science" + inst "University of British Columbia"
        Affiliation aff2 = result2.get(2);
        assertThat(aff2.getDepartments(), hasSize(1));
        assertThat(aff2.getDepartments().get(0), containsString("Department"));
        assertThat(aff2.getInstitutions(), hasSize(1));
        assertThat(aff2.getInstitutions().get(0), containsString("British"));
        assertThat(aff2.getInstitutions().get(0), containsString("Columbia"));
    }

    /**
     * Reproduces arXiv 2105.12810: the affiliation-address model emits the
     * sequence institution → marker → institution → marker → marker
     * because each institution carries a different author marker
     * ("Concordia University¹ North South University²,³"). Without the split
     * on "marker between institutions", both institutions collapse into a
     * single Affiliation and downstream consumers (TEI's first-orgName
     * extraction) drop the second institution, breaking the link from
     * authors 2 and 3 to North South University.
     * Pins the post-fix behaviour: each institution gets its own
     * Affiliation and its own marker. The trailing extra marker (3) without
     * a following institution is left as a marker-only Affiliation; the
     * multi-marker-per-institution case is out of scope here.
     */
    @Test
    public void testResultExtractionLayoutTokens_splitsInstitutionsWhenMarkerBetweenThem() throws Exception {
        String result = makeCrfRow("Concordia", "I-<institution>")
                + makeCrfRow("University", "<institution>")
                + makeCrfRow("1", "I-<marker>")
                + makeCrfRow("North", "I-<institution>")
                + makeCrfRow("South", "<institution>")
                + makeCrfRow("University", "<institution>")
                + makeCrfRow("2", "I-<marker>")
                + makeCrfRow("3", "I-<marker>");
        // Trim trailing newline added by makeCrfRow so split() doesn't yield a blank row.
        result = result.substring(0, result.length() - 1);

        List<LayoutToken> tokenizations = Arrays.stream(result.split("\n"))
                .map(row -> new LayoutToken(row.split("\t")[0]))
                .collect(Collectors.toList());

        List<Affiliation> affiliations = target.resultExtractionLayoutTokens(result, tokenizations);

        assertThat(affiliations, hasSize(greaterThanOrEqualTo(2)));

        Affiliation aff0 = affiliations.get(0);
        assertThat(aff0.getInstitutions(), hasSize(1));
        assertThat(aff0.getInstitutions().get(0), containsString("Concordia"));
        assertThat(aff0.getMarker(), is("1"));

        Affiliation aff1 = affiliations.get(1);
        assertThat(aff1.getInstitutions(), hasSize(1));
        assertThat(aff1.getInstitutions().get(0), containsString("North"));
        assertThat(aff1.getInstitutions().get(0), containsString("South"));
        assertThat(aff1.getMarker(), is("2"));
    }

    /**
     * Negative regression: ensure the new "marker between institutions" split
     * does not over-fire when there is no marker between consecutive
     * institutions. Two institutions arriving back-to-back (no marker, no
     * line break, no address) must remain in a single Affiliation, the same
     * way the legacy active-path handled it.
     */
    @Test
    public void testResultExtractionLayoutTokens_keepsConsecutiveInstitutionsTogether() throws Exception {
        String result = makeCrfRow("1", "I-<marker>")
                + makeCrfRow("University", "I-<institution>")
                + makeCrfRow("of", "<institution>")
                + makeCrfRow("Science", "<institution>")
                + makeCrfRow("University", "I-<institution>")
                + makeCrfRow("of", "<institution>")
                + makeCrfRow("Madness", "<institution>");
        result = result.substring(0, result.length() - 1);

        List<LayoutToken> tokenizations = Arrays.stream(result.split("\n"))
                .map(row -> new LayoutToken(row.split("\t")[0]))
                .collect(Collectors.toList());

        List<Affiliation> affiliations = target.resultExtractionLayoutTokens(result, tokenizations);

        assertThat(affiliations, hasSize(1));
        Affiliation aff = affiliations.get(0);
        assertThat(aff.getMarker(), is("1"));
        assertThat(aff.getInstitutions(), hasSize(2));
    }

    /**
     * Build a synthetic CRF feature-vector row in the 23-column format that
     * {@link AffiliationAddressParser#resultExtractionLayoutTokens(String, List)}
     * accepts. Most feature columns are placeholders — only the token text,
     * pre-label, and predicted label are read from each row by the active path.
     */
    private static String makeCrfRow(String token, String label) {
        return token
                + "\t"
                + token.toLowerCase()
                + "\tX\tXX\tXXX\tXXXX"
                + "\tx\txx\txxx\txxxx"
                + "\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tXxxx"
                + "\t<affiliation>"
                + "\t"
                + label
                + "\n";
    }

    /**
     * Resolves the Nabeel-marker-3 residue from arXiv 2105.12810: when an
     * institution carries multiple author markers (e.g. "North South
     * University²,³"), the affiliation-address model emits institution →
     * marker → marker, and the parser previously closed the institution-
     * bearing affiliation on the first marker, then opened a fresh empty
     * affiliation for the second. Author 3 was left without an affiliation.
     * The fix clones the just-closed affiliation's institution content when
     * the previous cluster was a marker, so both markers resolve to the
     * same institution.
     */
    @Test
    public void testResultExtractionLayoutTokens_consecutiveMarkersCloneInstitution() throws Exception {
        String result = makeCrfRow("Concordia", "I-<institution>")
                + makeCrfRow("University", "<institution>")
                + makeCrfRow("1", "I-<marker>")
                + makeCrfRow("North", "I-<institution>")
                + makeCrfRow("South", "<institution>")
                + makeCrfRow("University", "<institution>")
                + makeCrfRow("2", "I-<marker>")
                + makeCrfRow("3", "I-<marker>");
        result = result.substring(0, result.length() - 1);

        List<LayoutToken> tokenizations = Arrays.stream(result.split("\n"))
                .map(row -> new LayoutToken(row.split("\t")[0]))
                .collect(Collectors.toList());

        List<Affiliation> affiliations = target.resultExtractionLayoutTokens(result, tokenizations);

        assertThat(affiliations, hasSize(3));

        // aff[0]: Concordia, marker 1
        assertThat(affiliations.get(0).getInstitutions(), hasSize(1));
        assertThat(affiliations.get(0).getInstitutions().get(0), containsString("Concordia"));
        assertThat(affiliations.get(0).getMarker(), is("1"));

        // aff[1]: North South University, marker 2
        assertThat(affiliations.get(1).getInstitutions(), hasSize(1));
        assertThat(affiliations.get(1).getInstitutions().get(0), containsString("North"));
        assertThat(affiliations.get(1).getMarker(), is("2"));

        // aff[2]: SAME institution as aff[1] (cloned), marker 3
        assertThat(affiliations.get(2).getInstitutions(), hasSize(1));
        assertThat(affiliations.get(2).getInstitutions().get(0), containsString("North"));
        assertThat(affiliations.get(2).getInstitutions().get(0), containsString("South"));
        assertThat(affiliations.get(2).getMarker(), is("3"));

        // Crucially the clone must not have shared the list with aff[1] —
        // mutating one must not affect the other. We can't easily mutate
        // here, but verify the lists are distinct objects.
        assertThat(
                affiliations.get(2).getInstitutions(),
                is(not(sameInstance(affiliations.get(1).getInstitutions()))));
    }

    @Test
    public void testGetAffiliationBlocksFromSegments_2() throws Exception {
        String block1 = "Department of science, University of Science, University of Madness";
        List<LayoutToken> tokBlock1 = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(block1);
        tokBlock1.stream().forEach(t -> t.setOffset(t.getOffset() + 100));

        String block2 = "Department of mental health, University of happyness, Italy";
        List<LayoutToken> tokBlock2 = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(block2);
        tokBlock2.stream().forEach(t -> t.setOffset(t.getOffset() + 100 + tokBlock1.size()));

        List<String> affiliationBlocksFromSegments = AffiliationAddressParser
                .getAffiliationBlocksFromSegments(Arrays.asList(tokBlock1, tokBlock2));

        assertThat(affiliationBlocksFromSegments, hasSize(21));
        assertThat(affiliationBlocksFromSegments.get(0), is(not(startsWith("\n"))));
        assertThat(affiliationBlocksFromSegments.get(11), is(not("@newline")));

    }
}
