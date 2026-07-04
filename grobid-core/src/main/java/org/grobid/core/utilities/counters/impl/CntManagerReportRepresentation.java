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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import org.grobid.core.utilities.counters.CntManager;
import org.grobid.core.utilities.counters.CntManagerRepresentation;

/**
 * Human-readable report of all the counters held by a {@link CntManager}: one banner per
 * counter group followed by column-aligned {@code name: value} lines, and a trailing
 * METRICS section. This is the format historically produced by
 * {@code CntManagerImpl.toString()} and printed at the end of the evaluation and CLI
 * processing; it is now a dedicated formatter so that {@code toString()} can remain a
 * plain debug representation.
 */
public class CntManagerReportRepresentation implements CntManagerRepresentation {

    private static final int BAR_LENGTH = 84;

    @Override
    public String getRepresentation(CntManager cntManager) {
        StringBuilder sb = new StringBuilder(1000);
        for (Map.Entry<String, Map<String, Long>> group : cntManager.getAllCounters().entrySet()) {
            sb.append("\n")
                    .append(StringUtils.repeat('*', BAR_LENGTH))
                    .append("\n")
                    .append("COUNTER: ")
                    .append(group.getKey())
                    .append("\n")
                    .append(StringUtils.repeat('*', BAR_LENGTH))
                    .append("\n")
                    .append(StringUtils.repeat('-', BAR_LENGTH))
                    .append("\n");

            int maxLength = 0;
            for (String name : group.getValue().keySet()) {
                if (maxLength < name.length()) {
                    maxLength = name.length();
                }
            }

            for (Map.Entry<String, Long> cs : group.getValue().entrySet()) {
                sb.append("  ")
                        .append(cs.getKey())
                        .append(": ")
                        .append(StringUtils.repeat(' ', maxLength - cs.getKey().length()))
                        .append(cs.getValue())
                        .append("\n");
            }
            sb.append(StringUtils.repeat('=', BAR_LENGTH)).append("\n");
        }

        Map<String, String> metrics = cntManager.getMetricsRepresentation();
        if (metrics != null && !metrics.isEmpty()) {
            sb.append("\n")
                    .append(StringUtils.repeat('+', BAR_LENGTH))
                    .append("\n")
                    .append("METRICS\n")
                    .append(StringUtils.repeat('+', BAR_LENGTH))
                    .append("\n");
            for (Map.Entry<String, String> e : metrics.entrySet()) {
                sb.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
            }
        }
        sb.append(StringUtils.repeat('=', BAR_LENGTH)).append("\n");

        return sb.toString();
    }
}
