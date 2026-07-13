package org.grobid.service.exceptions.mapper;

import com.google.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.service.exceptions.GrobidServiceException;
import org.grobid.service.metrics.ApplicationMetrics;

@Provider
public class GrobidServiceExceptionMapper implements ExceptionMapper<GrobidServiceException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionMapper.class);

    @Context
    protected HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    @Inject
    private GrobidExceptionsTranslationUtility mapper;

    private final ApplicationMetrics metrics;

    @Inject
    public GrobidServiceExceptionMapper(ApplicationMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public Response toResponse(GrobidServiceException exception) {
        // GrobidServiceException carries an HTTP status rather than a GrobidExceptionStatus, so key
        // the error breakdown by the HTTP code (e.g. http_503).
        metrics.recordError(endpoint(), "http_" + exception.getResponseCode().getStatusCode());
        return mapper.processException(exception, exception.getResponseCode());
    }

    private String endpoint() {
        String path = uriInfo != null ? uriInfo.getPath() : null;
        return path == null || path.isBlank() ? "unknown" : path.replaceAll("^/+|/+$", "");
    }
}
