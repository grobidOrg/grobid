package org.grobid.core.engines.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.grobid.core.GrobidModel;

/**
 * Accumulator for raw sequence-labelling output produced during a request when
 * the {@code debugMode} flag is set on the REST API. Parsers append the unparsed
 * CRF tagger output (token + features + label, one row per token) keyed by the
 * canonical model name. The REST layer reads the snapshot after the engine
 * returns and serializes it into the response body.
 *
 * <p>Insertion order is preserved both across keys (so the dump reflects the
 * order in which parsers fired) and within a key (so a parser invoked N times
 * for N citations yields a list of N entries).
 */
public class DebugLabelingCollector {

    private final Map<String, List<String>> entries = new LinkedHashMap<>();

    public synchronized void record(GrobidModel model, String rawLabelOutput) {
        if (model == null || rawLabelOutput == null) {
            return;
        }
        entries.computeIfAbsent(model.getModelName(), k -> new ArrayList<>()).add(rawLabelOutput);
    }

    public synchronized Map<String, List<String>> snapshot() {
        Map<String, List<String>> copy = new LinkedHashMap<>(entries.size());
        for (Map.Entry<String, List<String>> e : entries.entrySet()) {
            copy.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    public synchronized Set<String> modelKeys() {
        return Collections.unmodifiableSet(new LinkedHashMap<>(entries).keySet());
    }

    public synchronized boolean isEmpty() {
        return entries.isEmpty();
    }
}
