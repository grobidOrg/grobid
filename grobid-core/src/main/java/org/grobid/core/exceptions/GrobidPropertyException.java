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
package org.grobid.core.exceptions;

public class GrobidPropertyException extends GrobidException {

    private static final long serialVersionUID = -3337770841815682150L;

    public GrobidPropertyException() {
        super();
    }

    public GrobidPropertyException(String message) {
        super(message);
    }

    public GrobidPropertyException(Throwable cause) {
        super(cause);
    }

    public GrobidPropertyException(String message, Throwable cause) {
        super(message, cause);
    }
}
