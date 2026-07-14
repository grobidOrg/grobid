package org.grobid.service.metrics;

import io.dropwizard.lifecycle.Managed;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.metrics.exporter.opentelemetry.OpenTelemetryExporter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.simpleclient.bridge.SimpleclientCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.service.OtlpConfiguration;

/**
 * Pushes metrics to an OTLP receiver on a fixed interval, as the push-based counterpart to the
 * pull-based {@code /metrics/prometheus} scrape endpoint.
 *
 * <p>Rather than instrumenting a second time with the OpenTelemetry SDK, this reuses the existing
 * Prometheus instrumentation: a {@link SimpleclientCollector} bridge exposes everything registered
 * in the simpleclient {@link CollectorRegistry#defaultRegistry} (the {@link ApplicationMetrics}
 * instruments plus the hotspot JVM/process exports), and the Prometheus Java client's
 * {@link OpenTelemetryExporter} pushes that same series set over OTLP. Both delivery paths therefore
 * carry identical metrics by construction.
 *
 * <p>Wired into Dropwizard's lifecycle as a {@link Managed} bean: {@link #start()} attaches the
 * bridge and starts the exporter's push scheduler; {@link #stop()} shuts it down during graceful
 * shutdown. When {@link OtlpConfiguration#isEnabled()} is false the reporter is inert.</p>
 */
public class OtlpMetricsReporter implements Managed {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtlpMetricsReporter.class);

    private final OtlpConfiguration config;

    // Held so stop() can shut it down. Null while disabled or before start().
    private OpenTelemetryExporter exporter;

    public OtlpMetricsReporter(OtlpConfiguration config) {
        this.config = config;
    }

    @Override
    public void start() {
        if (config == null || !config.isEnabled()) {
            LOGGER.info("OTLP metrics push is disabled (grobid.otlp.enabled=false)");
            return;
        }

        // A dedicated registry (rather than PrometheusRegistry.defaultRegistry) keeps the bridge
        // re-registrable: a second start() in the same JVM would otherwise duplicate the series.
        PrometheusRegistry bridged = new PrometheusRegistry();
        SimpleclientCollector.builder()
                .collectorRegistry(CollectorRegistry.defaultRegistry)
                .register(bridged);

        OpenTelemetryExporter.Builder builder = OpenTelemetryExporter.builder()
                .registry(bridged)
                // The exporter validates the protocol ("grpc" or "http/protobuf") and, for
                // http/protobuf, appends "/v1/metrics" to the endpoint when missing.
                .protocol(config.getProtocol())
                .endpoint(config.getEndpoint())
                .intervalSeconds(config.getIntervalSeconds())
                .timeoutSeconds(config.getTimeoutSeconds())
                .serviceName(config.getServiceName());
        if (config.getHeaders() != null) {
            // Typically auth for a hosted backend, e.g. Grafana Cloud's Authorization header.
            config.getHeaders().forEach(builder::header);
        }
        this.exporter = builder.buildAndStart();

        LOGGER.info(
                "OTLP metrics push enabled -> {} ({}), every {}s, service.name={}",
                config.getEndpoint(),
                config.getProtocol(),
                config.getIntervalSeconds(),
                config.getServiceName());
    }

    @Override
    public void stop() {
        if (exporter != null) {
            exporter.close();
            exporter = null;
            LOGGER.info("OTLP metrics push stopped");
        }
    }
}
