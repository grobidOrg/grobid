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
package org.grobid.core.lang.impl

import com.github.pemistahl.lingua.api.LanguageDetectorBuilder
import org.grobid.core.lang.Language
import org.grobid.core.lang.LanguageDetector
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LinguaLanguageDetector : LanguageDetector {
    private val detector: com.github.pemistahl.lingua.api.LanguageDetector = LanguageDetectorBuilder
        .fromAllLanguages()
        .withLowAccuracyMode()
        .build()

    override fun detect(text: String?): Language? {
        if (text.isNullOrBlank()) {
            return null
        }

        val languages = detector.computeLanguageConfidenceValues(text = text)

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug(languages.toString())
        }

        if (languages.isEmpty()) {
            return null
        }

        val l = languages.firstKey()
        val p = languages[l] ?: 0.0

        return Language(l.isoCode639_1.toString(), p)
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(LinguaLanguageDetector::class.java)
    }
}
