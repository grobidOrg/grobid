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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.*;

import org.grobid.core.data.Date;
import org.grobid.core.factory.AbstractEngineFactory;

/**
 * Created by lfoppiano on 02/01/17.
 */
public class DateParserIntegrationTest {
    DateParser target;

    @BeforeClass
    public static void setInitialContext() throws Exception {
        //        MockContext.setInitialContext();
        AbstractEngineFactory.init();
    }

    @AfterClass
    public static void destroyInitialContext() throws Exception {
        //        MockContext.destroyInitialContext();
    }

    @Before
    public void setUp() throws Exception {
        target = new DateParser();
    }

    @Test
    public void processing_englishStandardDate_shouldWork() throws Exception {
        List<Date> output = target.process("19 January 1983");
        assertThat(output, hasSize(1));
        final Date date = output.get(0);
        assertThat(date.getDay(), is(19));
        assertThat(date.getMonth(), is(1));
        assertThat(date.getYear(), is(1983));

        assertThat(date.getDayString(), is("19"));
        assertThat(date.getMonthString(), is("January"));
        assertThat(date.getYearString(), is("1983"));
    }

    @Test
    public void processing_englishStandardDate1_shouldWork() throws Exception {
        List<Date> output = target.process("19. January 19 83");
        assertThat(output, hasSize(1));
        final Date date = output.get(0);
        assertThat(date.getDay(), is(19));
        assertThat(date.getMonth(), is(1));
        assertThat(date.getYear(), is(1983));

        assertThat(date.getDayString(), is("19"));
        assertThat(date.getMonthString(), is("January"));

        // TODO: With the clusteror the space is removed...
        //        assertThat(date.getYearString(), is("19 83"));
        assertThat(date.getYearString(), is("1983"));
    }

    @Test
    public void processing_englishStandardDate2_shouldWork() throws Exception {

        List<Date> output = target.process("1918-1939");
        assertThat(output, hasSize(2));
        final Date date1 = output.get(0);
        assertThat(date1.getDay(), is(-1));
        assertThat(date1.getMonth(), is(-1));
        assertThat(date1.getYear(), is(1918));

        assertThat(date1.getDayString(), nullValue());
        assertThat(date1.getMonthString(), nullValue());
        assertThat(date1.getYearString(), is("1918"));

        final Date date2 = output.get(1);
        assertThat(date2.getDay(), is(-1));
        assertThat(date2.getMonth(), is(-1));
        assertThat(date2.getYear(), is(1939));

        assertThat(date2.getDayString(), nullValue());
        assertThat(date2.getMonthString(), nullValue());
        assertThat(date2.getYearString(), is("1939"));
    }

    @Test
    public void processing_englishStandardDate3_shouldWork() throws Exception {

        List<Date> output = target.process("16.06.1942-28.04.1943");
        assertThat(output, hasSize(2));
        final Date date1 = output.get(0);
        assertThat(date1.getDay(), is(16));
        assertThat(date1.getMonth(), is(6));
        assertThat(date1.getYear(), is(1942));

        assertThat(date1.getDayString(), is("16"));
        assertThat(date1.getMonthString(), is("06"));
        assertThat(date1.getYearString(), is("1942"));

        final Date date2 = output.get(1);
        assertThat(date2.getDay(), is(28));
        assertThat(date2.getMonth(), is(4));
        assertThat(date2.getYear(), is(1943));

        assertThat(date2.getDayString(), is("28"));
        assertThat(date2.getMonthString(), is("04"));
        assertThat(date2.getYearString(), is("1943"));
    }

    @Ignore("Need more training data, perhaps")
    @Test
    public void processing_englishStandardDate4_shouldWork() throws Exception {

        List<Date> output = target.process("4.01.1943-21.10.1943");
        assertThat(output, hasSize(2));
        final Date date1 = output.get(0);
        assertThat(date1.getDay(), is(4));
        assertThat(date1.getMonth(), is(1));
        assertThat(date1.getYear(), is(1943));

        assertThat(date1.getDayString(), is("4"));
        assertThat(date1.getMonthString(), is("1"));
        assertThat(date1.getYearString(), is("1943"));

        final Date date2 = output.get(1);
        assertThat(date2.getDay(), is(21));
        assertThat(date2.getMonth(), is(10));
        assertThat(date2.getYear(), is(1943));

        assertThat(date2.getDayString(), is("21"));
        assertThat(date2.getMonthString(), is("04"));
        assertThat(date2.getYearString(), is("1943"));
    }

    @Test
    public void processing_englishStandardDate5_shouldWork() throws Exception {

        List<Date> output = target.process("12.03.1942-10.1943");
        assertThat(output, hasSize(2));
        final Date date1 = output.get(0);
        assertThat(date1.getDay(), is(12));
        assertThat(date1.getMonth(), is(3));
        assertThat(date1.getYear(), is(1942));

        assertThat(date1.getDayString(), is("12"));
        assertThat(date1.getMonthString(), is("03"));
        assertThat(date1.getYearString(), is("1942"));

        final Date date2 = output.get(1);
        assertThat(date2.getDay(), is(-1));
        assertThat(date2.getMonth(), is(10));
        assertThat(date2.getYear(), is(1943));

        assertThat(date2.getDayString(), nullValue());
        assertThat(date2.getMonthString(), is("10"));
        assertThat(date2.getYearString(), is("1943"));
    }

    @Ignore("Need more training data, perhaps")
    @Test
    public void processing_englishStandardDate6_shouldWork() throws Exception {

        List<Date> output = target.process("1941-45");
        assertThat(output, hasSize(2));
        final Date date1 = output.get(0);
        assertThat(date1.getDay(), is(-1));
        assertThat(date1.getMonth(), is(-1));
        assertThat(date1.getYear(), is(1941));

        assertThat(date1.getDayString(), nullValue());
        assertThat(date1.getMonthString(), nullValue());
        assertThat(date1.getYearString(), is("1941"));

        final Date date2 = output.get(1);
        assertThat(date2.getDay(), is(-1));
        assertThat(date2.getMonth(), is(-1));
        assertThat(date2.getYear(), is(45));

        assertThat(date2.getDayString(), nullValue());
        assertThat(date2.getMonthString(), nullValue());
        assertThat(date2.getYearString(), is("45"));
    }

    @Test
    public void processing_englishStandardDate7_shouldWork() throws Exception {

        List<Date> output = target.process("2015-10-21");
        assertThat(output, hasSize(1));
        final Date date1 = output.get(0);
        assertThat(date1.getDay(), is(21));
        assertThat(date1.getMonth(), is(10));
        assertThat(date1.getYear(), is(2015));

        assertThat(date1.getDayString(), is("" + date1.getDay()));
        assertThat(date1.getMonthString(), is("" + date1.getMonth()));
        assertThat(date1.getYearString(), is("" + date1.getYear()));

    }

    @Test
    public void processing_englishStandardDate9_shouldWork() throws Exception {

        List<Date> output = target.process("2015-10-21 10-12-2016");
        assertThat(output, hasSize(2));
        final Date date1 = output.get(0);
        assertThat(date1.getDay(), is(21));
        assertThat(date1.getMonth(), is(10));
        assertThat(date1.getYear(), is(2015));

        assertThat(date1.getDayString(), is("" + date1.getDay()));
        assertThat(date1.getMonthString(), is("" + date1.getMonth()));
        assertThat(date1.getYearString(), is("" + date1.getYear()));

        final Date date2 = output.get(1);
        assertThat(date2.getDay(), is(10));
        assertThat(date2.getMonth(), is(12));
        assertThat(date2.getYear(), is(2016));

        assertThat(date2.getDayString(), is("" + date2.getDay()));
        assertThat(date2.getMonthString(), is("" + date2.getMonth()));
        assertThat(date2.getYearString(), is("" + date2.getYear()));
    }

    @Test
    public void testTrainingExtraction_simpleDate1() throws Exception {
        List<String> input = Arrays.asList("December 1943");

        StringBuilder sb = target.trainingExtraction(input);
        String output = sb.toString();

        assertThat(output, is("\t<date><month>December</month> <year>1943</year></date>\n"));

    }

    @Test
    public void testTrainingExtraction_simpleDate2() throws Exception {
        List<String> input = Arrays.asList("15 March 1942");

        StringBuilder sb = target.trainingExtraction(input);
        String output = sb.toString();

        assertThat(output, is("\t<date><day>15</day> <month>March</month> <year>1942</year></date>\n"));
    }

    @Test
    public void testTrainingExtraction_simpleDate3() throws Exception {
        List<String> input = Arrays.asList("1943-1944");

        StringBuilder sb = target.trainingExtraction(input);
        String output = sb.toString();

        assertThat(output, is("\t<date><year>1943</year>-</date>\n\t<date><year>1944</year></date>\n"));
    }

    @Test
    public void testTrainingExtraction_emptyInput() throws Exception {
        assertThat(target.trainingExtraction(null), nullValue());
    }

    @Test
    public void testMayAndMarchOverlap_1() throws Exception {
        List<Date> dates = target.process("Mar 2003");
        assertEquals(1, dates.size());
        Date date = dates.get(0);
        assertEquals(2003, date.getYear());
        assertEquals(3, date.getMonth());
    }

    @Test
    public void testMayAndMarchOverlap_2() throws Exception {

        List<Date> dates = target.process("May 2003");
        assertEquals(1, dates.size());
        Date date = dates.get(0);
        assertEquals(2003, date.getYear());
        assertEquals(5, date.getMonth());
    }
}
