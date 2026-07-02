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

import jakarta.ws.rs.core.Response;

import org.grobid.core.exceptions.GrobidExceptionStatus;

public class GrobidStatusToHttpStatusMapper {
    public static Response.Status getStatusCode(GrobidExceptionStatus status) {
        switch (status) {
            case BAD_INPUT_DATA :
                return Response.Status.BAD_REQUEST;
            case TAGGING_ERROR :
                return Response.Status.INTERNAL_SERVER_ERROR;
            case PARSING_ERROR :
                return Response.Status.INTERNAL_SERVER_ERROR;
            case TIMEOUT :
                return Response.Status.CONFLICT;
            case TOO_MANY_BLOCKS :
                return Response.Status.CONFLICT;
            case NO_BLOCKS :
                return Response.Status.BAD_REQUEST;
            case PDFALTO_CONVERSION_FAILURE :
                return Response.Status.INTERNAL_SERVER_ERROR;
            case TOO_MANY_TOKENS :
                return Response.Status.CONFLICT;
            case GENERAL :
                return Response.Status.INTERNAL_SERVER_ERROR;
            default :
                return Response.Status.INTERNAL_SERVER_ERROR;
        }
    }
}
