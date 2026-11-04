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
package org.grobid.core.engines;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.grobid.core.factory.AbstractEngineFactory;

public class CitationParserTest {
    @BeforeAll
    public static void setInitialContext() {
        AbstractEngineFactory.init();
    }

    @Test
    public void trainingExtraction_shouldSkipEmptyInputsAndKeepTheOthers() {
        List<String> inputs = Arrays.asList("Alpha Beta", "", "   ", null, "Gamma Delta");

        StringBuilder result = new CitationParser(new EngineParsers()) {
            @Override
            public String label(String data) {
                StringBuilder output = new StringBuilder();
                boolean first = true;
                for (String line : data.split("\n")) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    String token = line.split("\\s+")[0];
                    output.append(token).append("\t").append(first ? "I-<title>" : "<title>").append("\n");
                    first = false;
                }
                return output.toString();
            }
        }.trainingExtraction(inputs);

        assertThat(result, notNullValue());
        assertThat(StringUtils.countMatches(result.toString(), "<bibl>"), is(2));
        assertThat(result.toString(), org.hamcrest.CoreMatchers.containsString("Alpha Beta"));
        assertThat(result.toString(), org.hamcrest.CoreMatchers.containsString("Gamma Delta"));
    }
}
