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

/**
 * Interface for language recognition method/library
 */
public interface LanguageDetector {
    /**
     * Detects a language id that must consist of two letter together with a confidence coefficient.
     * If coefficient cannot be provided for some reason, it should be 1.0
     * @param text text to detect a language from
     * @return a language id together with a confidence coefficient
     */
    public Language detect(String text);
}
