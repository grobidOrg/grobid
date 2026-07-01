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
package org.grobid.service.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.GrobidModels;
import org.grobid.core.engines.config.DebugLabelingCollector;
import org.grobid.service.exceptions.GrobidServiceException;

/**
 * Helpers for the {@code debugMode} REST parameter — parses the {@code models}
 * filter, validates each entry against {@link GrobidModels}, and serializes a
 * {@link DebugLabelingCollector} snapshot into a {@code text/plain} body with
 * section delimiters between models.
 */
public final class GrobidDebugUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidDebugUtils.class);

    /**
     * Canonical names of every model declared in {@link GrobidModels}. Used as
     * the allowlist for the {@code models} filter. Computed once from the enum
     * values rather than via {@code GrobidModels.modelFor(name)}, since the
     * latter silently fabricates anonymous models for unknown names instead of
     * returning null.
     */
    private static final Set<String> KNOWN_MODEL_NAMES;

    static {
        Set<String> names = new HashSet<>();
        for (GrobidModels model : GrobidModels.values()) {
            names.add(model.getModelName());
        }
        KNOWN_MODEL_NAMES = Set.copyOf(names);
    }

    private GrobidDebugUtils() {
    }

    public static boolean isTruthy(String value) {
        return value != null && (value.equals("1") || value.equalsIgnoreCase("true"));
    }

    /**
     * Parse and validate the comma-separated {@code models} filter. Returns
     * {@code null} when the filter is empty (meaning: include all captured models).
     * Throws if any token is not a known model name.
     */
    public static Set<String> parseModelsFilter(String modelsParam) {
        if (StringUtils.isBlank(modelsParam)) {
            return null;
        }
        Set<String> requested = new LinkedHashSet<>();
        List<String> unknown = new ArrayList<>();
        for (String raw : Arrays.asList(modelsParam.split(","))) {
            String name = StringUtils.trimToNull(raw);
            if (name == null) {
                continue;
            }
            String normalized = name.toLowerCase();
            if (KNOWN_MODEL_NAMES.contains(normalized)) {
                requested.add(normalized);
            } else {
                unknown.add(name);
            }
        }
        if (!unknown.isEmpty()) {
            throw new GrobidServiceException(
                    "Unknown model name(s) in 'models' parameter: " + String.join(", ", unknown),
                    Response.Status.BAD_REQUEST);
        }
        return requested;
    }

    /**
     * Render a debug-mode response body. Sections appear in the order entries
     * were recorded; if a model fired more than once, each occurrence gets an
     * "(occurrence i of n)" suffix. When {@code modelsFilter} is non-null, only
     * sections whose key is in the filter are emitted.
     */
    public static String formatResponseBody(DebugLabelingCollector collector, Set<String> modelsFilter) {
        StringBuilder sb = new StringBuilder();
        Map<String, List<String>> snapshot = collector.snapshot();
        for (Map.Entry<String, List<String>> entry : snapshot.entrySet()) {
            String modelName = entry.getKey();
            if (modelsFilter != null && !modelsFilter.contains(modelName)) {
                continue;
            }
            List<String> occurrences = entry.getValue();
            int n = occurrences.size();
            for (int i = 0; i < n; i++) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                if (n == 1) {
                    sb.append("=== model: ").append(modelName).append(" ===\n");
                } else {
                    sb.append("=== model: ")
                            .append(modelName)
                            .append(" (occurrence ")
                            .append(i + 1)
                            .append(" of ")
                            .append(n)
                            .append(") ===\n");
                }
                sb.append(occurrences.get(i));
                if (!occurrences.get(i).endsWith("\n")) {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }
}
