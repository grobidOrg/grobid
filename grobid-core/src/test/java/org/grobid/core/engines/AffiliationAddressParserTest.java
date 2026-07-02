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
