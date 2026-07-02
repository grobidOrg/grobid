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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Before;
import org.junit.Test;

import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.Date;
import org.grobid.core.features.FeaturesVectorDate;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.GrobidConfig;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.GrobidTestUtils;

public class DateParserTest {

    private DateParser target;

    @Before
    public void setUp() throws Exception {
        GrobidConfig.ModelParameters modelParameters = new GrobidConfig.ModelParameters();
        modelParameters.name = "bao";
        GrobidProperties.addModel(modelParameters);
        target = new DateParser(GrobidModels.DUMMY);
    }

    @Test
    public void testPostValidation_invalidYear_onlyString_shouldSetNull() {
        Date inputDate = new Date();
        inputDate.setYearString("10360 10370");
        Date outputDate = DateParser.cleaning(inputDate);

        assertThat(outputDate.getYearString(), is(nullValue()));
    }

    @Test
    public void testPostValidation_invalidYear2_onlyString_shouldSetNull() {
        Date inputDate = new Date();
        inputDate.setYearString("10360 10370 10380 10390 10400");
        Date outputDate = DateParser.cleaning(inputDate);

        assertThat(outputDate.getYearString(), is(nullValue()));
    }

    @Test
    public void testPostValidation_invalidYear_bothStringAndInt_shouldSetNull() {
        Date inputDate = new Date();
        inputDate.setYear(1036010370);
        inputDate.setYearString("10360 10370");
        Date outputDate = DateParser.cleaning(inputDate);

        assertThat(outputDate.getYearString(), is(nullValue()));
        assertThat(outputDate.getYear(), is(-1));
    }

    @Test
    public void testPostValidation_invalidMonth_bothStringAndInt_shouldSetNull() {
        Date inputDate = new Date();
        inputDate.setMonth(1234);
        inputDate.setMonthString("1234");
        Date outputDate = DateParser.cleaning(inputDate);

        assertThat(outputDate.getMonthString(), is(nullValue()));
        assertThat(outputDate.getMonth(), is(-1));
    }

    @Test
    public void testPostValidation_invalidDay_bothStringAndInt_shouldSetNull() {
        Date inputDate = new Date();
        inputDate.setDay(12345);
        inputDate.setDayString("12345");
        Date outputDate = DateParser.cleaning(inputDate);

        assertThat(outputDate.getDayString(), is(nullValue()));
        assertThat(outputDate.getDay(), is(-1));
    }

    @Test
    public void testNormalize_yearContainsWholeDate_shouldReconstructCorrectly() {
        Date inputDate = new Date();
        inputDate.setYear(20021212);
        Date outputDate = target.normalizeAndClean(inputDate);

        assertThat(outputDate.getMonth(), is(12));
        assertThat(outputDate.getDay(), is(12));
        assertThat(outputDate.getYear(), is(2002));
    }

    @Test
    public void testNormalize_dayContainsWholeDate_shouldReturnEmptyDate() {
        Date inputDate = new Date();
        inputDate.setDay(20021212);
        Date outputDate = target.normalizeAndClean(inputDate);

        assertThat(outputDate.getMonth(), is(-1));
        assertThat(outputDate.getDay(), is(-1));
        assertThat(outputDate.getYear(), is(-1));
    }

    @Test
    public void testNormalize_monthContainsWholeDate_shouldReturnEmptyDate() {
        Date inputDate = new Date();
        inputDate.setMonth(20021212);
        Date outputDate = target.normalizeAndClean(inputDate);

        assertThat(outputDate.getMonth(), is(-1));
        assertThat(outputDate.getDay(), is(-1));
        assertThat(outputDate.getYear(), is(-1));
    }

    @Test
    public void testNormalize_yearOnly_validValue_shouldParseYearCorrectly() {
        Date inputDate = new Date();
        inputDate.setYearString("2002");
        Date outputDate = target.normalizeAndClean(inputDate);

        assertThat(outputDate.getMonth(), is(-1));
        assertThat(outputDate.getDay(), is(-1));
        assertThat(outputDate.getYear(), is(2002));
        assertThat(outputDate.getYearString(), is("2002"));
    }

    @Test
    public void testNormalize_wholeDate_invalidYearValue_shouldRemoveValue() {
        Date inputDate = new Date();
        inputDate.setDayString("12");
        inputDate.setMonthString("12");
        inputDate.setYearString("2222222012");
        Date outputDate = target.normalizeAndClean(inputDate);

        assertThat(outputDate.getDay(), is(12));
        assertThat(outputDate.getDayString(), is("12"));
        assertThat(outputDate.getMonth(), is(12));
        assertThat(outputDate.getMonthString(), is("12"));
        assertThat(outputDate.getYear(), is(-1));
        assertThat(outputDate.getYearString(), is(nullValue()));
    }

    @Test
    public void testNormalize_monthOnly_validValue_shouldParseMonthCorrectly() {
        Date inputDate = new Date();
        inputDate.setMonthString("12");
        Date outputDate = target.normalizeAndClean(inputDate);

        assertThat(outputDate.getMonth(), is(12));
        assertThat(outputDate.getDay(), is(-1));
        assertThat(outputDate.getYear(), is(-1));
        assertThat(outputDate.getMonthString(), is("12"));
    }

    @Test
    public void testNormalize_wholeDate_invalidMonthValue_shouldRemoveValue() {
        Date inputDate = new Date();
        inputDate.setDayString("12");
        inputDate.setMonthString("1222222222");
        inputDate.setYearString("2012");
        Date outputDate = target.normalizeAndClean(inputDate);

        assertThat(outputDate.getMonth(), is(-1));
        assertThat(outputDate.getMonthString(), is(nullValue()));
        assertThat(outputDate.getDay(), is(12));
        assertThat(outputDate.getDayString(), is("12"));
        assertThat(outputDate.getYear(), is(2012));
        assertThat(outputDate.getYearString(), is("2012"));
    }

    @Test
    public void testNormalize_dayOnly_validValue_shouldParseDayCorrectly() {
        Date inputDate = new Date();
        inputDate.setDayString("12");
        Date outputDate = target.normalizeAndClean(inputDate);

        assertThat(outputDate.getMonth(), is(-1));
        assertThat(outputDate.getDay(), is(12));
        assertThat(outputDate.getYear(), is(-1));
        assertThat(outputDate.getDayString(), is("12"));
    }

    @Test
    public void testNormalize_wholeDate_invalidDayValue_shouldRemoveValue() {
        Date inputDate = new Date();
        inputDate.setDayString("1221");
        inputDate.setMonthString("12");
        inputDate.setYearString("2012");
        Date outputDate = target.normalizeAndClean(inputDate);

        assertThat(outputDate.getDay(), is(-1));
        assertThat(outputDate.getDayString(), is(nullValue()));
        assertThat(outputDate.getMonth(), is(12));
        assertThat(outputDate.getMonthString(), is("12"));
        assertThat(outputDate.getYear(), is(2012));
        assertThat(outputDate.getYearString(), is("2012"));
    }

    @Test
    public void testResultExtraction_StandardDate_shouldWork() throws Exception {
        String input = "1983-1-1";

        List<LayoutToken> inputAsLayoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<String> features = generateFeatures(inputAsLayoutTokens);

        // These triples made in following way: label, starting index (included), ending index (excluded)
        List<Triple<String, Integer, Integer>> labels = Arrays.asList(
                Triple.of("<year>", 0, 1),
                Triple.of("<month>", 2, 3),
                Triple.of("<day>", 4, 5));

        String result = GrobidTestUtils.getWapitiResult(features, labels);

        List<Date> dates = target.resultExtraction(result, inputAsLayoutTokens);

        assertThat(dates, hasSize(1));
        assertThat(dates.get(0).getYearString(), is("1983"));
        assertThat(dates.get(0).getMonthString(), is("1"));
        assertThat(dates.get(0).getDayString(), is("1"));
    }

    @Test
    public void testResultExtraction_DoubleDate_shouldWork() throws Exception {
        String input = "1983-1-1 1982-1-2";

        List<LayoutToken> inputAsLayoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<String> features = generateFeatures(inputAsLayoutTokens);

        // These triples made in following way: label, starting index (included), ending index (excluded)
        List<Triple<String, Integer, Integer>> labels = Arrays.asList(
                Triple.of("<year>", 0, 1),
                Triple.of("<month>", 2, 3),
                Triple.of("<day>", 4, 5),
                Triple.of("<year>", 5, 6),
                Triple.of("<month>", 7, 8),
                Triple.of("<day>", 9, 10));

        String result = GrobidTestUtils.getWapitiResult(features, labels);

        List<Date> dates = target.resultExtraction(result, inputAsLayoutTokens);

        assertThat(dates, hasSize(2));
        assertThat(dates.get(0).getYearString(), is("1983"));
        assertThat(dates.get(0).getMonthString(), is("1"));
        assertThat(dates.get(0).getDayString(), is("1"));

        assertThat(dates.get(1).getYearString(), is("1982"));
        assertThat(dates.get(1).getMonthString(), is("1"));
        assertThat(dates.get(1).getDayString(), is("2"));
    }

    private List<String> generateFeatures(List<LayoutToken> layoutTokens) throws Exception {
        List<String> tokensAsStrings = layoutTokens.stream()
                .map(layoutToken -> layoutToken.getText() + " " + "<date>")
                .collect(Collectors.toList());
        String features = FeaturesVectorDate.addFeaturesDate(tokensAsStrings);
        return Arrays.asList(features.split("\n"));
    }
}
