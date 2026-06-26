package org.grobid.service.metrics;

import java.time.Duration;

import io.dropwizard.lifecycle.Managed;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.instrumentation.runtimemetrics.java17.RuntimeMetrics;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.service.OtlpConfiguration;

/**
 * Pushes metrics to an OTLP receiver on a fixed interval, as the push-based counterpart to the
 * pull-based {@code /metrics/prometheus} scrape endpoint.
 *
 * <p>Wired into Dropwizard's lifecycle as a {@link Managed} bean: {@link #start()} stands up the
 * OpenTelemetry SDK and begins exporting JVM/process runtime metrics (heap, GC, threads, CPU,
 * classes); {@link #stop()} flushes a final batch and releases resources during graceful shutdown.
 * When {@link OtlpConfiguration#isEnabled()} is false the reporter is inert — no SDK is created.</p>
 */
public class OtlpMetricsReporter implements Managed {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtlpMetricsReporter.class);

    private final OtlpConfiguration config;

    // Held so stop() can flush + shut them down. Null while disabled or before start().
    private OpenTelemetrySdk openTelemetry;
    private RuntimeMetrics runtimeMetrics;

    public OtlpMetricsReporter(OtlpConfiguration config) {
        this.config = config;
    }

    @Override
    public void start() {
        if (config == null || !config.isEnabled()) {
            LOGGER.info("OTLP metrics push is disabled (grobid.otlp.enabled=false)");
            return;
        }

        this.openTelemetry = buildOpenTelemetry();
        // Registers observable gauges/counters for JVM + process internals against the SDK's meter.
        // They are collected and pushed on every interval; closed in stop().
        this.runtimeMetrics = RuntimeMetrics.create(openTelemetry);

        LOGGER.info(
                "OTLP metrics push enabled -> {} ({}), every {}s, service.name={}",
                config.getEndpoint(),
                config.getProtocol(),
                config.getIntervalSeconds(),
                config.getServiceName());
    }

    @Override
    public void stop() {
        if (runtimeMetrics != null) {
            runtimeMetrics.close();
        }
        if (openTelemetry != null) {
            // close() force-flushes any buffered batch, then shuts the exporter down — so metrics
            // recorded right up to shutdown are not silently dropped.
            openTelemetry.close();
            LOGGER.info("OTLP metrics push stopped (final batch flushed)");
        }
    }

    /**
     * Assembles the OpenTelemetry SDK: a {@link Resource} identifying this service, a
     * {@link PeriodicMetricReader} driving the export interval, and the OTLP {@link MetricExporter}
     * produced by {@link #createExporter()}.
     */
    private OpenTelemetrySdk buildOpenTelemetry() {
        Resource resource = Resource.getDefault()
                .toBuilder()
                .put(AttributeKey.stringKey("service.name"), config.getServiceName())
                .build();

        PeriodicMetricReader reader = PeriodicMetricReader.builder(createExporter())
                .setInterval(Duration.ofSeconds(config.getIntervalSeconds()))
                .build();

        return OpenTelemetrySdk.builder()
                .setMeterProvider(
                        SdkMeterProvider.builder()
                                .setResource(resource)
                                .registerMetricReader(reader)
                                .build())
                .build();
    }

    /**
     * Builds the OTLP {@link MetricExporter} from {@link #config}.
     *
     * <p>TODO(you): implement this. Both {@link OtlpHttpMetricExporter} and
     * {@link OtlpGrpcMetricExporter} implement {@link MetricExporter} and share the same builder
     * shape: {@code .setEndpoint(String)}, {@code .addHeader(String, String)},
     * {@code .setTimeout(Duration)}, {@code .build()}. The meaningful decisions:</p>
     *
     * <ul>
     *   <li><b>Protocol</b> — pick the gRPC exporter when {@code config.getProtocol()} indicates
     *       grpc (port 4317), otherwise the HTTP/protobuf exporter (port 4318). HTTP is the
     *       firewall-friendly default; gRPC streams more efficiently.</li>
     *   <li><b>Headers</b> — apply every entry of {@code config.getHeaders()} via
     *       {@code addHeader}; this is how auth tokens reach a hosted backend (e.g. Grafana Cloud's
     *       {@code Authorization} header).</li>
     *   <li><b>Timeout</b> — optional; the default is 10s. Consider whether a slow/unreachable
     *       receiver should block the exporter thread that long every interval.</li>
     * </ul>
     *
     * @return a configured exporter; never null (it is read once at {@link #start()}).
     */
    private MetricExporter createExporter() {
        boolean grpc = config.getProtocol() != null
                && config.getProtocol().toLowerCase().contains("grpc");

        if (grpc) {
            var builder = OtlpGrpcMetricExporter.builder()
                    .setEndpoint(config.getEndpoint());
            if (config.getHeaders() != null) {
                config.getHeaders().forEach(builder::addHeader);
            }
            return builder.build();
        }

        var builder = OtlpHttpMetricExporter.builder()
                .setEndpoint(httpMetricsEndpoint(config.getEndpoint()));
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(builder::addHeader);
        }
        return builder.build();
    }

    /**
     * OTLP/HTTP requires the full signal path on the endpoint (the exporter appends nothing), so a
     * base URL such as {@code http://host:4318} must become {@code http://host:4318/v1/metrics}.
     * gRPC, by contrast, takes a bare {@code host:port} and routes by service method. An endpoint
     * that already carries the {@code /v1/metrics} path is left untouched.
     */
    static String httpMetricsEndpoint(String endpoint) {
        if (endpoint == null) {
            return null;
        }
        String trimmed = endpoint.replaceAll("/+$", "");
        return trimmed.endsWith("/v1/metrics") ? trimmed : trimmed + "/v1/metrics";
    }
}
