package org.grobid.core.lang.impl

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import kotlin.test.Test

class LinguaLanguageDetectorTest {

    private val target = LinguaLanguageDetector()

    @Test
    fun testDetect_englishText_shouldReturnEnglish() {
        val result = target.detect("This is a simple English sentence for language detection.")

        assertThat(result!!.lang, `is`("en"))
        assertThat(result.conf, greaterThan(0.0))
    }

    @Test
    fun testDetect_frenchText_shouldReturnFrench() {
        val result = target.detect("Ceci est une phrase en français pour la détection de langue.")

        assertThat(result!!.lang, `is`("fr"))
        assertThat(result.conf, greaterThan(0.0))
    }

    @Test
    fun testDetect_germanText_shouldReturnGerman() {
        val result = target.detect("Dies ist ein einfacher deutscher Satz zur Spracherkennung.")

        assertThat(result!!.lang, `is`("de"))
        assertThat(result.conf, greaterThan(0.0))
    }

    @Test
    fun testDetect_emptyText_shouldReturnNull() {
        val result = target.detect("")

        assertThat(result, `is`(nullValue()))
    }

    @Test
    fun testDetect_nullText_shouldReturnNull() {
        val result = target.detect(null)

        assertThat(result, `is`(nullValue()))
    }

    @Test
    fun testDetect_blankText_shouldReturnNull() {
        val result = target.detect("   ")

        assertThat(result, `is`(nullValue()))
    }
}
