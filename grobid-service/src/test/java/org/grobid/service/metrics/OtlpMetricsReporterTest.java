package org.grobid.service.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class OtlpMetricsReporterTest {

    @Test
    public void httpMetricsEndpoint_appendsSignalPathToBaseUrl() {
        assertEquals("http://localhost:4318/v1/metrics",
                OtlpMetricsReporter.httpMetricsEndpoint("http://localhost:4318"));
    }

    @Test
    public void httpMetricsEndpoint_stripsTrailingSlashesBeforeAppending() {
        assertEquals("http://collector:4318/v1/metrics",
                OtlpMetricsReporter.httpMetricsEndpoint("http://collector:4318///"));
    }

    @Test
    public void httpMetricsEndpoint_leavesExplicitSignalPathUntouched() {
        assertEquals("https://otlp.example.com/v1/metrics",
                OtlpMetricsReporter.httpMetricsEndpoint("https://otlp.example.com/v1/metrics"));
    }

    @Test
    public void httpMetricsEndpoint_preservesIntermediateBasePath() {
        // Hosted backends often namespace the OTLP route under a prefix; only the signal path is added.
        assertEquals("https://otlp.example.com/otlp/v1/metrics",
                OtlpMetricsReporter.httpMetricsEndpoint("https://otlp.example.com/otlp"));
    }

    @Test
    public void httpMetricsEndpoint_passesNullThrough() {
        assertNull(OtlpMetricsReporter.httpMetricsEndpoint(null));
    }
}
