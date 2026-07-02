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
package org.grobid.core.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.data.Date;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.utilities.GrobidProperties;

public class TestDate extends EngineTest {

    @BeforeClass
    public static void setUp() {
        GrobidProperties.getInstance();
    }

    @AfterClass
    public static void tearDown() {
        GrobidFactory.reset();
    }

    @Test
    public void testDateCompare_completeVSYearMonth_moreSpecifiedWins() {
        Date date1 = new Date();
        date1.setYear(2000);
        date1.setMonth(10);
        date1.setDay(2);

        Date date2 = new Date();
        date2.setYear(2000);
        date2.setMonth(10);

        assertThat(date1.compareTo(date2), is(-1));
    }

    @Test
    public void testDateCompare_YearMonthVsYear_moreSpecifiedWins() {
        Date date1 = new Date();
        date1.setYear(2007);

        Date date2 = new Date();
        date2.setYear(2007);
        date2.setMonth(9);

        assertThat(date1.compareTo(date2), is(1));
    }

    @Test
    public void testDateParser_cleanInput() throws Exception {
        String dateSequence1 = "10 January 2001";
        List<Date> res = engine.processDate(dateSequence1);
        assertNotNull(res);
        assertThat(res, hasSize(1));

        Date date = res.get(0);
        assertThat(date.toTEI(), is("<date when=\"2001-1-10\" />"));

        assertThat(date.getDayString(), is("10"));
        assertThat(date.getMonthString(), is("January"));
        assertThat(date.getYearString(), is("2001"));

        assertThat(date.getDay(), is(10));
        assertThat(date.getMonth(), is(1));
        assertThat(date.getYear(), is(2001));
    }

    @Test
    public void testDateParser_inputWithSpaces() throws Exception {

        String dateSequence2 = "19 November 1 999";
        List<Date> res = engine.processDate(dateSequence2);
        assertNotNull(res);
        assertThat(res, hasSize(1));

        Date date = res.get(0);
        assertThat(date.toTEI(), is("<date when=\"1999-11-19\" />"));

        assertThat(date.getDayString(), is("19"));
        assertThat(date.getMonthString(), is("November"));
        assertThat(date.getYearString(), is("1999"));

        assertThat(date.getDay(), is(19));
        assertThat(date.getMonth(), is(11));
        assertThat(date.getYear(), is(1999));
    }

    @Test
    public void testDateParser_inputWithSpecialFormat() throws Exception {
        String dateSequence3 = "15-08-2007";
        List<Date> res = engine.processDate(dateSequence3);
        assertNotNull(res);
        assertThat(res, hasSize(1));
        Date date = res.get(0);
        assertThat(date.toTEI(), is("<date when=\"2007-8-15\" />"));

        assertThat(date.getDayString(), is("15"));
        assertThat(date.getMonthString(), is("08"));
        assertThat(date.getYearString(), is("2007"));

        assertThat(date.getDay(), is(15));
        assertThat(date.getMonth(), is(8));
        assertThat(date.getYear(), is(2007));
    }

    @Test
    public void testDateParser_DifferentOrdering() throws Exception {
        String dateSequence4 = "November 14 1999";
        List<Date> res = engine.processDate(dateSequence4);
        assertNotNull(res);
        assertThat(res, hasSize(1));

        Date date = res.get(0);
        assertThat(date.toTEI(), is("<date when=\"1999-11-14\" />"));

        assertThat(date.getDayString(), is("14"));
        assertThat(date.getMonthString(), is("November"));
        assertThat(date.getYearString(), is("1999"));

        assertThat(date.getDay(), is(14));
        assertThat(date.getMonth(), is(11));
        assertThat(date.getYear(), is(1999));
    }

}
