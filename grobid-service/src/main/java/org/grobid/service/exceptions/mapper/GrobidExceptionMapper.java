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
package org.grobid.service.exceptions.mapper;

import com.google.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.grobid.core.exceptions.GrobidException;
import org.grobid.service.metrics.ApplicationMetrics;

@Provider
public class GrobidExceptionMapper implements ExceptionMapper<GrobidException> {

    @Context
    protected HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    @Inject
    private GrobidExceptionsTranslationUtility mapper;

    private final ApplicationMetrics metrics;

    @Inject
    public GrobidExceptionMapper(ApplicationMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public Response toResponse(GrobidException exception) {
        // Break errors down by GROBID reason (TOO_MANY_TOKENS, NO_BLOCKS, TIMEOUT, ...) — only known
        // here, not in the response filter, which sees just the HTTP status.
        metrics.recordError(endpoint(), String.valueOf(exception.getStatus()));
        return mapper.processException(exception, GrobidStatusToHttpStatusMapper.getStatusCode(exception.getStatus()));
    }

    private String endpoint() {
        String path = uriInfo != null ? uriInfo.getPath() : null;
        return path == null || path.isBlank() ? "unknown" : path.replaceAll("^/+|/+$", "");
    }
}
