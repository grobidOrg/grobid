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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.grobid.core.utilities.counters.CntManager;
import org.grobid.core.utilities.counters.CntManagerRepresentation;

public class CntManagerGrepRepresentation implements CntManagerRepresentation {
    public static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public String getRepresentation(CntManager cntManager) {
        StringBuilder sb = new StringBuilder();
        synchronized (df) {
            sb.append("|").append(df.format(new Date())).append('\n');
        }
        for (Map.Entry<String, Map<String, Long>> m : cntManager.getAllCounters().entrySet()) {
            sb.append('=').append(m.getKey()).append('\n');
            for (Map.Entry<String, Long> cs : m.getValue().entrySet()) {
                sb.append(m.getKey()).append("+").append(cs.getKey()).append(":").append(cs.getValue()).append('\n');
            }
            sb.append('\n');
        }

        return sb.toString();
    }
}
