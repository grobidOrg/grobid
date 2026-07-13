package org.grobid.service.metrics;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.google.inject.Singleton;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

/**
 * Single source of truth for GROBID's application/business metrics, recorded once and exported on
 * <em>both</em> delivery paths:
 *
 * <ul>
 *   <li><b>Prometheus (pull)</b> — native simpleclient instruments in a {@link CollectorRegistry},
 *       served verbatim by the existing {@code /metrics/prometheus} servlet. Always active.</li>
 *   <li><b>OpenTelemetry (push)</b> — the OTLP counterpart instruments, created from the SDK
 *       {@link Meter} the {@link OtlpMetricsReporter} hands over via {@link #bind(Meter)} at startup.
 *       Until then (and whenever {@code grobid.otlp.enabled=false}) they are backed by a
 *       {@link OpenTelemetry#noop() no-op} meter, so recording is always safe and never depends on
 *       OTLP being enabled.</li>
 * </ul>
 *
 * <p>Recording in a single place is what keeps the two paths from drifting. Every caller (the Jersey
 * request/response filter and the exception mappers) goes through the {@code record*} methods here.
 *
 * <p>The OTLP instrument-timing subtlety this class exists to sidestep: an OpenTelemetry instrument
 * obtained from the global SDK <em>before</em> that SDK is installed stays a no-op forever. Instead
 * of relying on the global, the reporter binds a real {@link Meter} into this holder once the SDK is
 * built, at which point {@link #bind(Meter)} swaps the no-op instruments for live ones.
 */
@Singleton
public class ApplicationMetrics {

    static final String INSTRUMENTATION_SCOPE = "org.grobid.service";

    private static final AttributeKey<String> ENDPOINT = AttributeKey.stringKey("endpoint");
    private static final AttributeKey<String> HTTP_STATUS = AttributeKey.stringKey("http_status");
    private static final AttributeKey<String> REASON = AttributeKey.stringKey("reason");

    // Latency buckets (seconds) tuned for PDF processing: sub-second string endpoints up to
    // multi-minute full-text extraction of large documents.
    private static final double[] DURATION_BUCKETS_SECONDS = {0.1, 0.25, 0.5, 1, 2.5, 5, 10, 30, 60, 120, 300};
    // Request/document size buckets (bytes): a few KB (string calls) up to ~100 MB PDFs.
    private static final double[] SIZE_BUCKETS_BYTES = {1_000, 10_000, 100_000, 1_000_000, 5_000_000, 10_000_000,
            50_000_000, 100_000_000};

    // ---- Prometheus (pull) — always active ------------------------------------------------------
    private final Counter requestsTotal;
    private final Histogram requestDurationSeconds;
    private final Counter errorsTotal;
    private final Gauge requestsInFlight;
    private final Histogram requestSizeBytes;
    // Pre-existing, documented counters (issue #920) — kept for backward compatibility.
    private final Counter filesProcessedTotal;
    private final Counter filesProcessingErrorsTotal;

    // ---- OpenTelemetry (push) — no-op until bind() ----------------------------------------------
    private volatile LongCounter otelRequests;
    private volatile DoubleHistogram otelDuration;
    private volatile LongCounter otelErrors;
    private volatile DoubleHistogram otelSize;
    // Source of truth for in-flight; the Prometheus gauge and the OTel observable gauge both mirror it.
    private final AtomicLong inFlight = new AtomicLong();
    private ObservableLongGauge otelInFlightGauge;

    public ApplicationMetrics() {
        this(CollectorRegistry.defaultRegistry);
    }

    /** Registry-injectable constructor so callers/tests can use an isolated {@link CollectorRegistry}. */
    public ApplicationMetrics(CollectorRegistry registry) {
        this.requestsTotal = Counter.build()
                .name("grobid_requests_total")
                .labelNames("endpoint", "http_status")
                .help("Total number of GROBID API requests, by endpoint and HTTP status.")
                .register(registry);
        this.requestDurationSeconds = Histogram.build()
                .name("grobid_request_duration_seconds")
                .labelNames("endpoint")
                .buckets(DURATION_BUCKETS_SECONDS)
                .help("GROBID API request processing time in seconds, by endpoint.")
                .register(registry);
        this.errorsTotal = Counter.build()
                .name("grobid_errors_total")
                .labelNames("endpoint", "reason")
                .help(
                        "Total number of failed GROBID requests, by endpoint and reason "
                                + "(GrobidExceptionStatus name, e.g. TOO_MANY_TOKENS, or http_<code>).")
                .register(registry);
        this.requestsInFlight = Gauge.build()
                .name("grobid_requests_in_flight")
                .help("Number of GROBID API requests currently being processed.")
                .register(registry);
        this.requestSizeBytes = Histogram.build()
                .name("grobid_request_size_bytes")
                .labelNames("endpoint")
                .buckets(SIZE_BUCKETS_BYTES)
                .help("Size in bytes of the request payload submitted to GROBID, by endpoint.")
                .register(registry);
        this.filesProcessedTotal = Counter.build()
                .name("grobid_files_processed_total")
                .help(
                        "Total number of files submitted to GROBID file-processing endpoints "
                                + "(multipart/form-data uploads).")
                .register(registry);
        this.filesProcessingErrorsTotal = Counter.build()
                .name("grobid_files_processing_errors_total")
                .help("Total number of file-processing requests that failed with a 5xx server error.")
                .register(registry);

        // Start with no-op OTel instruments so recording is safe before the SDK is bound.
        bindOtelInstruments(OpenTelemetry.noop().getMeter(INSTRUMENTATION_SCOPE));
    }

    /**
     * Install the live SDK {@link Meter}, replacing the no-op OTLP instruments with real ones so that
     * subsequent recordings are pushed over OTLP. Called by {@link OtlpMetricsReporter#start()} once
     * the OpenTelemetry SDK has been built. Safe to call once; the reporter only builds the SDK when
     * OTLP is enabled.
     */
    public synchronized void bind(Meter meter) {
        if (otelInFlightGauge != null) {
            otelInFlightGauge.close();
        }
        bindOtelInstruments(meter);
    }

    private void bindOtelInstruments(Meter meter) {
        this.otelRequests = meter.counterBuilder("grobid.requests")
                .setDescription("Total number of GROBID API requests.")
                .build();
        this.otelDuration = meter.histogramBuilder("grobid.request.duration")
                .setUnit("s")
                .setDescription("GROBID API request processing time.")
                .setExplicitBucketBoundariesAdvice(toList(DURATION_BUCKETS_SECONDS))
                .build();
        this.otelErrors = meter.counterBuilder("grobid.errors")
                .setDescription("Total number of failed GROBID requests, by reason.")
                .build();
        this.otelSize = meter.histogramBuilder("grobid.request.size")
                .setUnit("By")
                .setDescription("Size of the request payload submitted to GROBID.")
                .setExplicitBucketBoundariesAdvice(toList(SIZE_BUCKETS_BYTES))
                .build();
        this.otelInFlightGauge = meter.gaugeBuilder("grobid.requests.in_flight")
                .ofLongs()
                .setDescription("Number of GROBID API requests currently being processed.")
                .buildWithCallback(measurement -> measurement.record(inFlight.get()));
    }

    // ---- recording -----------------------------------------------------------------------------

    /**
     * Record one completed request. {@code sizeBytes} is skipped when negative (unknown length).
     */
    public void recordRequest(String endpoint, int httpStatus, double durationSeconds, long sizeBytes) {
        String status = Integer.toString(httpStatus);
        requestsTotal.labels(endpoint, status).inc();
        requestDurationSeconds.labels(endpoint).observe(durationSeconds);
        otelRequests.add(1, Attributes.of(ENDPOINT, endpoint, HTTP_STATUS, status));
        otelDuration.record(durationSeconds, Attributes.of(ENDPOINT, endpoint));
        if (sizeBytes >= 0) {
            requestSizeBytes.labels(endpoint).observe(sizeBytes);
            otelSize.record(sizeBytes, Attributes.of(ENDPOINT, endpoint));
        }
    }

    /**
     * Record one failed request, keyed by GROBID reason (a {@code GrobidExceptionStatus} name such as
     * {@code TOO_MANY_TOKENS}/{@code NO_BLOCKS}, or {@code http_<code>} for non-GROBID errors).
     */
    public void recordError(String endpoint, String reason) {
        errorsTotal.labels(endpoint, reason).inc();
        otelErrors.add(1, Attributes.of(ENDPOINT, endpoint, REASON, reason));
    }

    /** Increment the file-upload throughput counter (multipart/form-data endpoints). */
    public void recordFileProcessed() {
        filesProcessedTotal.inc();
    }

    /** Increment the file-upload 5xx error counter. */
    public void recordFileProcessingError() {
        filesProcessingErrorsTotal.inc();
    }

    public void incInFlight() {
        inFlight.incrementAndGet();
        requestsInFlight.inc();
    }

    public void decInFlight() {
        inFlight.decrementAndGet();
        requestsInFlight.dec();
    }

    private static List<Double> toList(double[] values) {
        Double[] boxed = new Double[values.length];
        for (int i = 0; i < values.length; i++) {
            boxed[i] = values[i];
        }
        return List.of(boxed);
    }
}
