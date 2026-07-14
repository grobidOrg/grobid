package org.grobid.service.metrics;

import org.junit.Test;

import org.grobid.service.OtlpConfiguration;

public class OtlpMetricsReporterTest {

    private static OtlpConfiguration enabledConfig(String protocol, String endpoint) {
        OtlpConfiguration config = new OtlpConfiguration();
        config.setEnabled(true);
        config.setProtocol(protocol);
        config.setEndpoint(endpoint);
        // Long interval so no export is attempted while the test runs.
        config.setIntervalSeconds(3600);
        config.getHeaders().put("Authorization", "Basic dGVzdDp0ZXN0");
        return config;
    }

    @Test
    public void disabled_startAndStopAreInert() throws Exception {
        OtlpMetricsReporter reporter = new OtlpMetricsReporter(new OtlpConfiguration());
        reporter.start();
        reporter.stop();
    }

    @Test
    public void nullConfig_startAndStopAreInert() throws Exception {
        OtlpMetricsReporter reporter = new OtlpMetricsReporter(null);
        reporter.start();
        reporter.stop();
    }

    @Test
    public void enabledHttpProtobuf_startsAndStopsCleanly() throws Exception {
        // The exporter only connects when a push is due, so an unreachable endpoint is fine here;
        // this exercises config -> exporter mapping (incl. headers) and the Managed lifecycle.
        OtlpMetricsReporter reporter = new OtlpMetricsReporter(enabledConfig("http/protobuf", "http://localhost:4318"));
        reporter.start();
        reporter.stop();
    }

    @Test
    public void enabledGrpc_startsAndStopsCleanly() throws Exception {
        OtlpMetricsReporter reporter = new OtlpMetricsReporter(enabledConfig("grpc", "http://localhost:4317"));
        reporter.start();
        reporter.stop();
    }

    @Test
    public void stopWithoutStart_isSafe() throws Exception {
        new OtlpMetricsReporter(enabledConfig("http/protobuf", "http://localhost:4318")).stop();
    }
}
