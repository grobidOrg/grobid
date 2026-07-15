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

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

/**
 * Single source of truth for GROBID's application/business metrics. Instruments live in a
 * Prometheus {@link CollectorRegistry} and are delivered on both paths from there:
 *
 * <ul>
 *   <li><b>Prometheus (pull)</b> — served verbatim by the existing {@code /metrics/prometheus}
 *       servlet on the admin connector. Always active.</li>
 *   <li><b>OpenTelemetry (push)</b> — the {@link OtlpMetricsReporter} bridges this registry into
 *       the OTLP exporter, so the same series are pushed when {@code grobid.otlp.enabled=true}.</li>
 * </ul>
 *
 * <p>Recording in a single place is what keeps the two paths from drifting. Every caller (the Jersey
 * request/response filter and the exception mappers) goes through the {@code record*} methods here.
 *
 * <p>The default-registry instance is a JVM-wide singleton (see {@link #defaultInstance()}):
 * simpleclient registries reject duplicate collector names, so the collectors must register into the
 * global default registry exactly once per JVM — even when several Guice injectors are created in
 * the same JVM, as the service tests do.
 */
public class ApplicationMetrics {

    private static volatile ApplicationMetrics defaultInstance;

    // Latency buckets (seconds) tuned for PDF processing: sub-second string endpoints up to
    // multi-minute full-text extraction of large documents.
    private static final double[] DURATION_BUCKETS_SECONDS = {0.1, 0.25, 0.5, 1, 2.5, 5, 10, 30, 60, 120, 300};
    // Request/document size buckets (bytes): a few KB (string calls) up to ~100 MB PDFs.
    private static final double[] SIZE_BUCKETS_BYTES = {1_000, 10_000, 100_000, 1_000_000, 5_000_000, 10_000_000,
            50_000_000, 100_000_000};

    private final Counter requestsTotal;
    private final Histogram requestDurationSeconds;
    private final Counter errorsTotal;
    private final Gauge requestsInFlight;
    private final Histogram requestSizeBytes;
    // Pre-existing, documented counters (issue #920) — kept for backward compatibility.
    private final Counter filesProcessedTotal;
    private final Counter filesProcessingErrorsTotal;

    /** The JVM-wide instance backed by {@link CollectorRegistry#defaultRegistry}, created lazily once. */
    public static ApplicationMetrics defaultInstance() {
        ApplicationMetrics instance = defaultInstance;
        if (instance == null) {
            synchronized (ApplicationMetrics.class) {
                instance = defaultInstance;
                if (instance == null) {
                    instance = new ApplicationMetrics(CollectorRegistry.defaultRegistry);
                    defaultInstance = instance;
                }
            }
        }
        return instance;
    }

    /** Registry-injectable constructor so tests can use an isolated {@link CollectorRegistry}. */
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
    }

    // ---- recording -----------------------------------------------------------------------------

    /**
     * Record one completed request. {@code sizeBytes} is skipped when negative (unknown length).
     */
    public void recordRequest(String endpoint, int httpStatus, double durationSeconds, long sizeBytes) {
        requestsTotal.labels(endpoint, Integer.toString(httpStatus)).inc();
        requestDurationSeconds.labels(endpoint).observe(durationSeconds);
        if (sizeBytes >= 0) {
            requestSizeBytes.labels(endpoint).observe(sizeBytes);
        }
    }

    /**
     * Record one failed request, keyed by GROBID reason (a {@code GrobidExceptionStatus} name such as
     * {@code TOO_MANY_TOKENS}/{@code NO_BLOCKS}, or {@code http_<code>} for non-GROBID errors).
     */
    public void recordError(String endpoint, String reason) {
        errorsTotal.labels(endpoint, reason).inc();
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
        requestsInFlight.inc();
    }

    public void decInFlight() {
        requestsInFlight.dec();
    }
}
