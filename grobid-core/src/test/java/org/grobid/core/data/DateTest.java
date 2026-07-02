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
package org.grobid.core.data;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class DateTest {
    Date target;
    Date other;

    @Test
    public void testDateMerging_yearVsYearMonth_shouldReturnYearMonth() {
        // "2010" "2010-10" -> "2010-10"

        target = new Date();
        target.setYear(2010);

        other = new Date();
        other.setYear(2010);
        other.setMonth(10);

        Date merged = Date.merge(target, other);

        assertThat(merged.getYear(), is(2010));
        assertThat(merged.getMonth(), is(10));
    }

    @Test
    public void testDateMerging_yearVsYearMonthDay_shouldReturnYearMonthDay() {
        // "2010" "2010-10-27" -> "2010-10-27"

        target = new Date();
        target.setYear(2010);

        other = new Date();
        other.setYear(2010);
        other.setMonth(10);
        other.setDay(27);

        Date merged = Date.merge(target, other);

        assertThat(merged.getYear(), is(2010));
        assertThat(merged.getMonth(), is(10));
        assertThat(merged.getDay(), is(27));
    }

    @Test
    public void testDateMerging_yearMonthVsYearMonthDay_shouldReturnYearMonthDay() {
        // "2010-10" "2010-10-27" -> "2010-10-27"

        target = new Date();
        target.setYear(2010);
        target.setMonth(10);

        other = new Date();
        other.setYear(2010);
        other.setMonth(10);
        other.setDay(27);

        Date merged = Date.merge(target, other);

        assertThat(merged.getYear(), is(2010));
        assertThat(merged.getMonth(), is(10));
        assertThat(merged.getDay(), is(27));
    }

    @Test
    public void testDateMerging_YearMonthDayVsYearMonth_shouldReturnYearMonthDay() {
        // "2010-10-27" "2010-10" -> "2010-10-27"

        target = new Date();
        target.setYear(2010);
        target.setMonth(10);
        target.setDay(27);

        other = new Date();
        other.setYear(2010);
        other.setMonth(10);

        Date merged = Date.merge(target, other);

        assertThat(merged.getYear(), is(2010));
        assertThat(merged.getMonth(), is(10));
        assertThat(merged.getDay(), is(27));
    }

    @Test
    public void testDateMerging_differentDates_yearMonth_shouldReturnOriginal() {
        // "2011-10" "2010-10-27" -> "2011-10"

        target = new Date();
        target.setYear(2011);
        target.setMonth(10);

        other = new Date();
        other.setYear(2010);
        other.setMonth(10);
        other.setDay(27);

        Date merged = Date.merge(target, other);

        assertThat(merged.getYear(), is(2011));
        assertThat(merged.getMonth(), is(10));
    }

    @Test
    public void testDateMerging_differentDates_year_shouldReturnOriginal() {
        // "2010" "2016-10-27" -> "2010"

        target = new Date();
        target.setYear(2010);

        other = new Date();
        other.setYear(2016);
        other.setMonth(10);
        other.setDay(27);

        Date merged = Date.merge(target, other);

        assertThat(merged.getYear(), is(2010));
    }

    @Test
    public void testDateMerging_differentDates_missingYearFromTarget() {
        // "" "2016-10-27" -> "2016-10-27"

        target = new Date();

        other = new Date();
        other.setYear(2016);
        other.setMonth(10);
        other.setDay(27);

        Date merged = Date.merge(target, other);

        assertThat(merged.getYear(), is(2016));
        assertThat(merged.getMonth(), is(10));
        assertThat(merged.getDay(), is(27));
    }

    @Test
    public void testDateMerging_differentDates_onlyYear_shouldReturnOriginal() {
        // "2011" "2010" -> 2011

        target = new Date();
        target.setYear(2011);

        other = new Date();
        other.setYear(2010);

        Date merged = Date.merge(target, other);

        assertThat(merged.getYear(), is(2011));
    }

    @Test
    public void testToISOString_onlyYear() {
        Date date = new Date();
        date.setYear(2016);
        date.setMonth(10);
        date.setDay(27);

        assertThat(Date.toISOString(date), is("2016-10-27"));
    }

    @Test
    public void testToISOString_onlyYear_WithoutPrefix() {
        Date date = new Date();
        date.setYear(16);
        date.setMonth(10);
        date.setDay(27);

        assertThat(Date.toISOString(date), is("0016-10-27"));
    }

    @Test
    public void testToISOString_completeDate_missingMonth() {
        Date date = new Date();
        date.setYear(2016);
        date.setDay(27);

        assertThat(Date.toISOString(date), is("2016"));
    }

    @Test
    public void testToISOString_onlyDay() {
        Date date = new Date();
        date.setDay(27);

        assertThat(Date.toISOString(date), is(""));
    }
}
