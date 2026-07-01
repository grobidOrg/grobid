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
package org.grobid.core.lang;

import java.util.List;

import org.grobid.core.utilities.OffsetPosition;

/**
 * Interface for sentence recognition method/library
 */
public interface SentenceDetector {
    /**
     * Detects sentence boundaries
     * @param text text to detect sentence boundaries
     * @return a list of offset positions indicating start and end character
     *         position of the recognized sentence in the text
     */
    public List<OffsetPosition> detect(String text);

    /**
     * Detects sentence boundaries using a specified language
     * @param text text to detect sentence boundaries
     * @param lang language to be used for detecting sentence boundaries
     * @return a list of offset positions indicating start and end character
     *         position of the recognized sentence in the text
     */
    public List<OffsetPosition> detect(String text, Language lang);
}
