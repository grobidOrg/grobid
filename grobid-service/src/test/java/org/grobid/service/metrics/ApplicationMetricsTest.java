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
package org.grobid.service.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import io.prometheus.client.CollectorRegistry;
import org.junit.Before;
import org.junit.Test;

public class ApplicationMetricsTest {

    private CollectorRegistry registry;
    private ApplicationMetrics metrics;

    @Before
    public void setUp() {
        // Isolated registry so each test starts from a clean slate (avoids default-registry clashes).
        registry = new CollectorRegistry();
        metrics = new ApplicationMetrics(registry);
    }

    @Test
    public void recordRequest_populatesPrometheusCountDurationAndSize() {
        metrics.recordRequest("processFulltextDocument", 200, 0.812, 48_213);

        assertEquals(
                1.0,
                sample(
                        "grobid_requests_total",
                        new String[]{"endpoint", "http_status"},
                        new String[]{"processFulltextDocument", "200"}),
                0.0);
        assertEquals(
                1.0,
                sample(
                        "grobid_request_duration_seconds_count",
                        new String[]{"endpoint"},
                        new String[]{"processFulltextDocument"}),
                0.0);
        assertEquals(
                1.0,
                sample(
                        "grobid_request_size_bytes_count",
                        new String[]{"endpoint"},
                        new String[]{"processFulltextDocument"}),
                0.0);
    }

    @Test
    public void recordRequest_skipsSizeWhenNegative() {
        metrics.recordRequest("processHeaderDocument", 200, 0.1, -1);

        assertNull(
                "size sample should be absent for unknown length",
                registry.getSampleValue(
                        "grobid_request_size_bytes_count",
                        new String[]{"endpoint"},
                        new String[]{"processHeaderDocument"}));
    }

    @Test
    public void recordError_breaksDownByReason() {
        metrics.recordError("processFulltextDocument", "TOO_MANY_TOKENS");
        metrics.recordError("processFulltextDocument", "TOO_MANY_TOKENS");
        metrics.recordError("processFulltextDocument", "NO_BLOCKS");

        assertEquals(
                2.0,
                sample(
                        "grobid_errors_total",
                        new String[]{"endpoint", "reason"},
                        new String[]{"processFulltextDocument", "TOO_MANY_TOKENS"}),
                0.0);
        assertEquals(
                1.0,
                sample(
                        "grobid_errors_total",
                        new String[]{"endpoint", "reason"},
                        new String[]{"processFulltextDocument", "NO_BLOCKS"}),
                0.0);
    }

    @Test
    public void inFlight_tracksConcurrency() {
        metrics.incInFlight();
        metrics.incInFlight();
        assertEquals(2.0, sample("grobid_requests_in_flight", new String[]{}, new String[]{}), 0.0);
        metrics.decInFlight();
        assertEquals(1.0, sample("grobid_requests_in_flight", new String[]{}, new String[]{}), 0.0);
    }

    @Test
    public void defaultInstance_isSharedAndSafeToRequestRepeatedly() {
        // The default registry rejects duplicate collector names, so requesting the instance twice
        // (as happens when several Guice injectors exist in one JVM) must reuse the same object
        // rather than re-register.
        assertSame(ApplicationMetrics.defaultInstance(), ApplicationMetrics.defaultInstance());
    }

    private double sample(String name, String[] labelNames, String[] labelValues) {
        Double value = registry.getSampleValue(name, labelNames, labelValues);
        assertNotNull("expected sample " + name + " to be present", value);
        return value;
    }
}
