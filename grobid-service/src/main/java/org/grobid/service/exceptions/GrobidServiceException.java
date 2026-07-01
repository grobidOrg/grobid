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
package org.grobid.service.exceptions;

import jakarta.ws.rs.core.Response;

import org.grobid.core.exceptions.GrobidException;

public class GrobidServiceException extends GrobidException {

    private static final long serialVersionUID = -756089338090769910L;
    private Response.Status responseCode;

    public GrobidServiceException(Response.Status responseCode) {
        super();
        this.responseCode = responseCode;
    }

    public GrobidServiceException(String msg, Response.Status responseCode) {
        super(msg);
        this.responseCode = responseCode;
    }

    public GrobidServiceException(String msg, Throwable cause, Response.Status responseCode) {
        super(msg, cause);
        this.responseCode = responseCode;
    }

    public Response.Status getResponseCode() {
        return responseCode;
    }
}
