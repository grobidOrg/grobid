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
package org.grobid.core.utilities.counters;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

public class GrobidTimerTest {

    /**
     * The Grobid timer.
     */
    GrobidTimer timer;

    @Test
    public void testGrobidTimerEmptyConstructor() {
        timer = new GrobidTimer();
        assertNotNull("The Map should be initiated.", timer.getAllSavedTimes());
        assertTrue(
                "The Map should be empty",
                timer.getAllSavedTimes()
                        .isEmpty());
    }

    @Test
    public void testGrobidTimerBoolConstructorTrue() {
        timer = new GrobidTimer(true);
        assertNotNull("The Map should be initiated.", timer.getAllSavedTimes());
        assertEquals(
                "The Map should have 1 element (START)",
                1,
                timer
                        .getAllSavedTimes()
                        .size());
        assertNotNull(
                "The START time should not be null",
                timer.getTime(GrobidTimer.START));
    }

    @Test
    public void testGrobidTimerBoolConstructorFalse() {
        timer = new GrobidTimer(false);
        assertNotNull("The Map should be initiated.", timer.getAllSavedTimes());
        assertTrue(
                "The Map should be empty",
                timer.getAllSavedTimes()
                        .isEmpty());
        assertNull(
                "The START time should be null",
                timer.getTime(GrobidTimer.START));
    }

    //@Test
    public void testStartStop() {
        timer = new GrobidTimer();
        timer.start();
        timer.stop("STOP");
        Map<String, Long> times = timer.getAllSavedTimes();
        long elapsedTime = times.get(GrobidTimer.START) - times.get("STOP");
        assertEquals("2 times should be saved in Grobid Timer", 2, times.size());
        assertEquals(
                "Not matching times",
                elapsedTime,
                (long) timer.getElapsedTimeFromStart("STOP"));
        assertEquals(
                "Not matching times",
                elapsedTime,
                (long) timer.getElapsedTime(GrobidTimer.START, "STOP"));
    }

}
