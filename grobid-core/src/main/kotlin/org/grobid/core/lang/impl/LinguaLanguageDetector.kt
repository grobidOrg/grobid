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
