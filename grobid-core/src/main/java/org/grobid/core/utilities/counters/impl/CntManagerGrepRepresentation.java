package org.grobid.core.utilities.counters.impl;

import org.grobid.core.utilities.counters.CntManager;
import org.grobid.core.utilities.counters.CntManagerRepresentation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

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
