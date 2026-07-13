package org.grobid.service.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
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
    public void bind_pushesTheSameRecordingsToOtel() {
        InMemoryMetricReader reader = InMemoryMetricReader.create();
        try (SdkMeterProvider provider = SdkMeterProvider.builder().registerMetricReader(reader).build()) {
            Meter meter = provider.get(ApplicationMetrics.INSTRUMENTATION_SCOPE);
            metrics.bind(meter);

            metrics.recordRequest("processFulltextDocument", 200, 0.5, 1000);
            metrics.recordRequest("processFulltextDocument", 200, 0.7, 2000);
            metrics.recordError("processFulltextDocument", "TIMEOUT");

            Collection<MetricData> exported = reader.collectAllMetrics();

            assertTrue(
                    "grobid.requests should be exported over OTLP",
                    exported.stream().anyMatch(m -> m.getName().equals("grobid.requests")));
            assertTrue(
                    "grobid.request.duration should be exported over OTLP",
                    exported.stream().anyMatch(m -> m.getName().equals("grobid.request.duration")));
            assertTrue(
                    "grobid.errors should be exported over OTLP",
                    exported.stream().anyMatch(m -> m.getName().equals("grobid.errors")));

            long requestCount = exported.stream()
                    .filter(m -> m.getName().equals("grobid.requests"))
                    .flatMap(m -> m.getLongSumData().getPoints().stream())
                    .mapToLong(p -> p.getValue())
                    .sum();
            assertEquals(2, requestCount);
        }
    }

    @Test
    public void recordingBeforeBind_isSafeAndPrometheusStillWorks() {
        // No bind() called: OTel instruments are no-ops, but Prometheus must still record.
        metrics.recordRequest("processDate", 204, 0.02, 12);
        assertEquals(
                1.0,
                sample(
                        "grobid_requests_total",
                        new String[]{"endpoint", "http_status"},
                        new String[]{"processDate", "204"}),
                0.0);
    }

    private double sample(String name, String[] labelNames, String[] labelValues) {
        Double value = registry.getSampleValue(name, labelNames, labelValues);
        assertNotNull("expected sample " + name + " to be present", value);
        return value;
    }
}
