package org.grobid.service.main;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.service.metrics.ApplicationMetrics;

/**
 * Jersey filter that instruments every request for both metrics delivery paths and emits a
 * structured access-log line carrying the client's provenance.
 *
 * <p>As a {@link ContainerRequestFilter} it stamps the start time and increments the in-flight
 * gauge; as a {@link ContainerResponseFilter} it records the request outcome (count, duration, size)
 * through {@link ApplicationMetrics} — which fans it out to both Prometheus and OTLP — decrements the
 * in-flight gauge, and keeps the pre-existing {@code grobid_files_*} upload counters.
 *
 * <p>Client IP is deliberately logged rather than attached as a metric label: raw IPs are unbounded
 * cardinality and would inflate Prometheus/hosted-backend series (and billing). The access log
 * (logger {@code org.grobid.service.access}) preserves provenance for inspection in a log backend
 * (Loki, …) without that cost. Behind a reverse proxy (e.g. Hugging Face Spaces) the real client is
 * taken from the first hop of {@code X-Forwarded-For}, falling back to the socket remote address.
 */
public class GrobidMetricsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidMetricsFilter.class);
    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("org.grobid.service.access");

    /** Nanosecond start timestamp stashed on the request context by {@link #filter(ContainerRequestContext)}. */
    private static final String START_NANOS = "org.grobid.service.metrics.startNanos";

    private final ApplicationMetrics metrics;

    @Context
    private HttpServletRequest httpRequest;

    public GrobidMetricsFilter(ApplicationMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty(START_NANOS, System.nanoTime());
        metrics.incInFlight();
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        try {
            String endpoint = endpointOf(requestContext);
            int status = responseContext.getStatus();
            long sizeBytes = requestContext.getLength(); // Content-Length, or -1 when unknown
            double durationSeconds = elapsedSeconds(requestContext);

            metrics.recordRequest(endpoint, status, durationSeconds, sizeBytes);

            // Pre-existing upload counters: only the file-processing (multipart) endpoints.
            if (isFileUpload(requestContext)) {
                metrics.recordFileProcessed();
                if (status >= 500) {
                    metrics.recordFileProcessingError();
                }
            }

            writeAccessLog(endpoint, status, durationSeconds, sizeBytes);
        } catch (RuntimeException e) {
            // Never let instrumentation break the response.
            LOGGER.warn("Failed to record request metrics", e);
        } finally {
            metrics.decInFlight();
        }
    }

    /** The JAX-RS request path (e.g. {@code processFulltextDocument}); GROBID paths carry no path params. */
    private static String endpointOf(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        return StringUtils.isBlank(path) ? "root" : StringUtils.strip(path, "/");
    }

    private static boolean isFileUpload(ContainerRequestContext requestContext) {
        MediaType mediaType = requestContext.getMediaType();
        return mediaType != null && MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(mediaType);
    }

    private static double elapsedSeconds(ContainerRequestContext requestContext) {
        Object start = requestContext.getProperty(START_NANOS);
        if (!(start instanceof Long)) {
            return 0d; // request filter did not run (should not happen for served requests)
        }
        return (System.nanoTime() - (Long) start) / 1_000_000_000d;
    }

    private void writeAccessLog(String endpoint, int status, double durationSeconds, long sizeBytes) {
        if (!ACCESS_LOG.isInfoEnabled()) {
            return;
        }
        ACCESS_LOG.info(
                "client={} endpoint={} status={} duration_ms={} bytes={}",
                clientIp(),
                endpoint,
                status,
                Math.round(durationSeconds * 1000),
                sizeBytes >= 0 ? sizeBytes : "unknown");
    }

    /** Real client IP: first hop of {@code X-Forwarded-For} if present, else the socket peer. */
    private String clientIp() {
        if (httpRequest == null) {
            return "unknown";
        }
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwarded)) {
            return StringUtils.strip(StringUtils.substringBefore(forwarded, ","));
        }
        String remote = httpRequest.getRemoteAddr();
        return StringUtils.isNotBlank(remote) ? remote : "unknown";
    }
}
