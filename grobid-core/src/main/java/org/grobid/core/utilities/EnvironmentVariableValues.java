package org.grobid.core.utilities;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentVariableValues {
    private final Map<String, String> configParameters = new HashMap<>();

    public EnvironmentVariableValues(String matcher) {
        this(System.getenv(), matcher);
    }

    public EnvironmentVariableValues(Map<String, String> environmentVariablesMap, String matcher) {
        for (Map.Entry<String, String> entry : environmentVariablesMap.entrySet()) {
            if (!entry.getKey().matches(matcher)) {
                continue;
            }
            this.configParameters.put(entry.getKey(), entry.getValue());
        }
    }

    public Map<String, String> getConfigParameters() {
        return this.configParameters;
    }
}
