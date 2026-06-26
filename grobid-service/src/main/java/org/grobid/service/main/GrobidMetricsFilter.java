package org.grobid.service.main;

import java.io.IOException;

import io.prometheus.client.Counter;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;

/**
 * Jersey response filter that exports application-level Prometheus counters for file processing.
 *
 * <p>Registered once on the Jersey environment, it observes every response and — for the
 * file-processing endpoints, identified as {@code multipart/form-data} uploads — increments:
 *
 * <ul>
 *   <li>{@code grobid_files_processed_total} for every file-upload request handled, and</li>
 *   <li>{@code grobid_files_processing_errors_total} when the response is a 5xx server error
 *       (no GROBID engine available, internal processing failure, …).</li>
 * </ul>
 *
 * <p>These are native Prometheus counters registered in the default {@code CollectorRegistry}, so
 * they are served verbatim (idiomatic snake_case, proper {@code _total} suffix) by the same
 * {@code /metrics/prometheus} servlet that exposes the bridged Dropwizard metrics, without going
 * through {@link GrobidDropwizardExports}.
 *
 * <p>Useful queries:
 * <pre>
 *   rate(grobid_files_processed_total[5m])                                  # throughput
 *   rate(grobid_files_processing_errors_total[5m])
 *       / rate(grobid_files_processed_total[5m])                            # error ratio
 * </pre>
 */
public class GrobidMetricsFilter implements ContainerResponseFilter {

    private static final Counter FILES_PROCESSED = Counter.build()
            .name("grobid_files_processed_total")
            .help(
                    "Total number of files submitted to GROBID file-processing endpoints "
                            + "(multipart/form-data uploads).")
            .register();

    private static final Counter FILES_PROCESSING_ERRORS = Counter.build()
            .name("grobid_files_processing_errors_total")
            .help("Total number of file-processing requests that failed with a 5xx server error.")
            .register();

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        MediaType mediaType = requestContext.getMediaType();
        if (mediaType == null || !MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(mediaType)) {
            return; // not a file upload (e.g. string endpoints, isAlive, version) — ignore
        }
        FILES_PROCESSED.inc();
        if (responseContext.getStatus() >= 500) {
            FILES_PROCESSING_ERRORS.inc();
        }
    }
}
