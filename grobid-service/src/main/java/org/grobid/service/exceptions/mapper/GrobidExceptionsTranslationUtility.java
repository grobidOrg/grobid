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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.MDC;

import org.grobid.core.exceptions.GrobidExceptionStatus;

@Provider
public class GrobidExceptionsTranslationUtility {

    @Inject
    public GrobidExceptionsTranslationUtility() {
    }

    public Response processException(Throwable exception, Response.Status status) {
        try {
            fillMdc(exception, status);
            List<String> descriptions = getExceptionDescriptions(exception, status);

            //            String requestUri = uriInfo.getRequestUri().toString();
            return Response.status(status)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(buildJson(getExceptionName(exception), descriptions, status, null, null))
                    .build();
        } finally {
            cleanMdc();
        }
    }

    public String getExceptionName(Throwable exception) {
        String exceptionName = exception.getClass().getCanonicalName();
        if (exception.getCause() != null) {
            exceptionName = exception.getCause().getClass().getCanonicalName();
        }
        return exceptionName;
    }

    public void fillMdc(Throwable exception, Response.Status status) {
        MDC.put("ExceptionName", getExceptionName(exception));
        MDC.put("StatusCode", String.valueOf(status.getStatusCode()));
        MDC.put("ReasonPhrase", status.getReasonPhrase());
        MDC.put("StatusFamily", status.getFamily().toString());
        MDC.put("StackTrace", Throwables.getStackTraceAsString(exception));
    }

    public void cleanMdc() {
        MDC.remove("ExceptionName");
        MDC.remove("StringErrorCode");
        MDC.remove("StatusCode");
        MDC.remove("ReasonPhrase");
        MDC.remove("StatusFamily");
        MDC.remove("StackTrace");
    }

    public List<String> getExceptionDescriptions(Throwable exception, Response.Status status) {
        List<String> descriptions = new ArrayList<>();

        Throwable currentException = exception;
        int maxIterations = 0;
        while (currentException != null) {
            StringBuilder sb = new StringBuilder(50);
            sb.append(currentException.getClass().getName());
            if (currentException.getMessage() != null) {
                sb.append(":").append(currentException.getMessage());
            }
            descriptions.add(sb.toString());
            currentException = currentException.getCause();
            maxIterations++;
            if (maxIterations > 4) {
                break;
            }
        }
        return descriptions;
    }

    public String buildJson(
            String type,
            List<String> descriptions,
            Response.Status status,
            GrobidExceptionStatus grobidExceptionStatus,
            String requestUri) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("type", type);
        root.put("description", Joiner.on("\n").join(descriptions));
        root.put("code", status.getStatusCode());
        root.put("requestUri", requestUri);
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            root.put("correlationId", correlationId);
        }
        if (grobidExceptionStatus != null) {
            root.put("grobidExceptionStatus", grobidExceptionStatus.name());
        }

        String json;
        try {
            json = mapper.writeValueAsString(root);
        } catch (IOException e) {
            //            LOGGER.warn("Error in ServiceExceptionMapper: ", e);
            json = "{\"description\": \"Internal error: " + e.getMessage() + "\"}";
        }
        return json;
    }
}
