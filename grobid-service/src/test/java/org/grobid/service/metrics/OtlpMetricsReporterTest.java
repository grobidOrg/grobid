package org.grobid.service.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import org.junit.Test;

import org.grobid.service.OtlpConfiguration;

public class OtlpMetricsReporterTest {

    private static MetricExporter exporterFor(String protocol) {
        OtlpConfiguration config = new OtlpConfiguration();
        config.setProtocol(protocol);
        return new OtlpMetricsReporter(config, null).createExporter();
    }

    @Test
    public void createExporter_selectsGrpcExporterForGrpcProtocol() {
        assertTrue(exporterFor("grpc") instanceof OtlpGrpcMetricExporter);
    }

    @Test
    public void createExporter_selectsHttpExporterForHttpProtobuf() {
        assertTrue(exporterFor("http/protobuf") instanceof OtlpHttpMetricExporter);
    }

    @Test
    public void createExporter_fallsBackToHttpForUnrecognisedProtocol() {
        assertTrue(exporterFor("carrier-pigeon") instanceof OtlpHttpMetricExporter);
    }

    @Test
    public void isGrpc_matchesOnlyGrpcProtocols() {
        assertTrue(OtlpMetricsReporter.isGrpc("grpc"));
        assertTrue(OtlpMetricsReporter.isGrpc("GRPC"));
        assertFalse(OtlpMetricsReporter.isGrpc("http/protobuf"));
        assertFalse(OtlpMetricsReporter.isGrpc(null));
    }

    @Test
    public void httpMetricsEndpoint_appendsSignalPathToBaseUrl() {
        assertEquals(
                "http://localhost:4318/v1/metrics",
                OtlpMetricsReporter.httpMetricsEndpoint("http://localhost:4318"));
    }

    @Test
    public void httpMetricsEndpoint_stripsTrailingSlashesBeforeAppending() {
        assertEquals(
                "http://collector:4318/v1/metrics",
                OtlpMetricsReporter.httpMetricsEndpoint("http://collector:4318///"));
    }

    @Test
    public void httpMetricsEndpoint_leavesExplicitSignalPathUntouched() {
        assertEquals(
                "https://otlp.example.com/v1/metrics",
                OtlpMetricsReporter.httpMetricsEndpoint("https://otlp.example.com/v1/metrics"));
    }

    @Test
    public void httpMetricsEndpoint_preservesIntermediateBasePath() {
        // Hosted backends often namespace the OTLP route under a prefix; only the signal path is added.
        assertEquals(
                "https://otlp.example.com/otlp/v1/metrics",
                OtlpMetricsReporter.httpMetricsEndpoint("https://otlp.example.com/otlp"));
    }

    @Test
    public void httpMetricsEndpoint_passesNullThrough() {
        assertNull(OtlpMetricsReporter.httpMetricsEndpoint(null));
    }
}
