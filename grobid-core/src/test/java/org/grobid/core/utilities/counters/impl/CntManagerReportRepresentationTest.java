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
package org.grobid.core.utilities.counters.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class CntManagerReportRepresentationTest {

    CntManagerImpl cntManager;
    CntManagerReportRepresentation target;

    @Before
    public void setUp() {
        cntManager = new CntManagerImpl();
        target = new CntManagerReportRepresentation();
    }

    @Test
    public void getRepresentation_shouldRenderAlignedReport() {
        cntManager.i("figures", "element", 2);
        cntManager.i("figures", "verylongcountername", 40);

        String report = target.getRepresentation(cntManager);

        // banner and group name
        assertThat(report, containsString("COUNTER: figures"));
        // counter names and their values are present
        assertThat(report, containsString("element: "));
        assertThat(report, containsString("verylongcountername: "));
        // the shorter name is right-padded so its value aligns with the longer name's value.
        // "element" (7) padded up to "verylongcountername" (19) => 12 spaces before the value.
        assertThat(report, containsString("  element: " + " ".repeat(12) + "2"));
        // it must NOT be the raw ToStringBuilder object dump
        assertThat(report, not(containsString("CntManagerImpl[")));
    }

    @Test
    public void getRepresentation_emptyManager_shouldNotFail() {
        String report = target.getRepresentation(cntManager);

        assertThat(report, not(containsString("COUNTER:")));
        assertThat(report, not(containsString("CntManagerImpl[")));
    }
}
