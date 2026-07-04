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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * GrobidTimer is a timer that can memorize multiple stop times.<br>
 *
 * Example of use: <code>
 * GrobidTimer timer= new GrobidTimer();
 * timer.start();
 * timer.stop("Time1");
 * timer.stop("Time2");
 * System.out.println("The elapsed time between start and Time1 is "+timer.getElapsedTimeFromStart("Time1"));
 * System.out.println("The elapsed time between Time1 and Time2 is "+timer.getElapsedTime("Time1","Time2"));
 * </code>
 *
 *
 */
public class GrobidTimer {

    /**
     * The tag under which the start time is saved.
     */
    public static final String START = "START";

    /**
     * The tag end.
     */
    public static final String STOP = "STOP";

    /**
     * Date format.
     */
    private static final String MIN_SEC_MILI = "s 'seconds' S 'milliseconds'";

    /**
     * Map containing all the tagged times.
     */
    private Map<String, Long> times;

    /**
     * Contains the time when the timer count has been paused.
     */
    private Long timePauseStarted;

    /**
     * Constructor.
     */
    public GrobidTimer() {
        this(false);
    }

    /**
     * Constructor.
     *
     * @param pStartNow
     *            if true the timer will be started from now, else the start
     *            method will have to be called.
     */
    public GrobidTimer(final boolean pStartNow) {
        times = new HashMap<String, Long>();
        if (pStartNow) {
            start();
        }
    }

    /**
     * Start the timer.
     */
    public void start() {
        times.put(START, System.currentTimeMillis());
    }

    /**
     * Store the current time with the name pTag.
     *
     * @param pTag
     *            the name under which the current time will be saved.
     */
    public void stop(final String pTag) {
        times.put(pTag, System.currentTimeMillis());
    }

    /**
     * Compute the time elapsed between the start of the timer and the stop
     * time.
     *
     * @param pTag
     *            the tag of the stop time.
     * @return the time elapsed from start to stop.
     */
    public Long getElapsedTimeFromStart(final String pTag) {
        return times.get(pTag) - times.get(START);
    }

    /**
     * Compute the time elapsed between pTagStart and pTagStop.
     *
     * @param pTagStart
     *            the tag of the start time.
     * @param pTagStop
     *            the tag of the stop time.
     * @return the time elapsed from start to stop.
     */
    public Long getElapsedTime(final String pTagStart, final String pTagStop) {
        return times.get(pTagStop) - times.get(pTagStart);
    }

    /**
     * Get all the time saved in the timer. <br>
     * The start tag is {@link #START}.
     *
     * @return the time corresponding to the tag.
     */
    public Long getTime(final String pTag) {
        return times.get(pTag);
    }

    /**
     * Compute the time elapsed between the start of the timer and the stop
     * time.<br>
     * Return the time formatted: {@link #MIN_SEC_MILI}.
     *
     * @param pTag
     *            the tag of the stop time.
     * @return the time elapsed from start to stop.
     */
    public String getElapsedTimeFromStartFormated(final String pTag) {
        return formatTime(getElapsedTimeFromStart(pTag));
    }

    /**
     * Compute the time elapsed between pTagStart and pTagStop. <br>
     * Return the time formatted: {@link #MIN_SEC_MILI}.
     *
     * @param pTagStart
     *            the tag of the start time.
     * @param pTagStop
     *            the tag of the stop time.
     * @return the time elapsed from start to stop.
     */
    public String getElapsedTimeFormated(final String pTagStart, final String pTagStop) {
        return formatTime(getElapsedTime(pTagStart, pTagStop));
    }

    /**
     * Get all the time saved in the timer. <br>
     * The start tag is {@link #START}. <br>
     * Return the time formatted: {@link #MIN_SEC_MILI}.
     *
     * @return the time corresponding to the tag.
     */
    public String getTimeFormated(final String pTag) {
        return formatTime(getTime(pTag));
    }

    /**
     * Stop time count. To restart the timer: {@link #restartTimer()}.
     */
    public void pauseTimer() {
        timePauseStarted = System.currentTimeMillis();
    }

    /**
     * Restart the timer when it has been paused by {@link #pauseTimer()}.
     */
    public void restartTimer() {
        times.put(START, times.get(START) + (System.currentTimeMillis() - timePauseStarted));
    }

    /**
     * Return the complete Map of all stored times.
     *
     * @return Map<String,Long>
     */
    public Map<String, Long> getAllSavedTimes() {
        return times;
    }

    /**
     * Format a time from Long to String using the following format:
     * {@link #MIN_SEC_MILI}.
     *
     * @param pTime
     *            the time to format.
     * @return formatted time.
     */
    public static String formatTime(final Long pTime) {
        return org.grobid.core.utilities.Utilities.dateToString(new Date(pTime), MIN_SEC_MILI);
    }
}
