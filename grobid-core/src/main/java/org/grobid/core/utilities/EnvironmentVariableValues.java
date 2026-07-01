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
