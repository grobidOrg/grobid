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
package org.grobid.service.main;

import java.util.List;

import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.dropwizard.samplebuilder.DefaultSampleBuilder;
import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;

/**
 * Prometheus {@link SampleBuilder} that converts Dropwizard metric names to snake_case.
 *
 * <p>Dropwizard metric names are dotted paths built from Java identifiers (e.g.
 * {@code org.grobid.service.GrobidRestService.processFulltextDocument}), i.e. camelCase. The
 * stock {@link DefaultSampleBuilder} only sanitizes illegal characters ({@code .} -&gt; {@code _})
 * to make the names <em>legal</em> Prometheus names; it deliberately does not transliterate
 * camelCase to snake_case. That leaves names that {@code promtool check metrics} flags with
 * "metric names should be written in 'snake_case' not 'camelCase'" (issue #920).
 *
 * <p>This builder delegates the sanitization to {@link DefaultSampleBuilder} and then lower-cases
 * the result, inserting underscores at camelCase boundaries.
 */
public class SnakeCaseSampleBuilder implements SampleBuilder {

    private final DefaultSampleBuilder delegate = new DefaultSampleBuilder();

    @Override
    public Sample createSample(
            String dropwizardName,
            String nameSuffix,
            List<String> additionalLabelNames,
            List<String> additionalLabelValues,
            double value) {
        Sample sample = delegate.createSample(
                dropwizardName,
                nameSuffix,
                additionalLabelNames,
                additionalLabelValues,
                value);
        return new Sample(
                toSnakeCase(sample.name),
                sample.labelNames,
                sample.labelValues,
                sample.value,
                sample.timestampMs);
    }

    /**
     * Convert a sanitized metric name to snake_case. Existing underscores are preserved, so this
     * only needs to break camelCase boundaries and lower-case the result. Acronym runs are handled
     * so that {@code ABCWord} becomes {@code abc_word} rather than {@code a_b_c_word}.
     */
    static String toSnakeCase(String name) {
        String snake = name
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2") // ABCWord -> ABC_Word
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2");   // fooBar  -> foo_Bar
        return snake.toLowerCase();
    }
}
