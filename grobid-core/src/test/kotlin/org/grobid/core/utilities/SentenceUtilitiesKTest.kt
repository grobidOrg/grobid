package org.grobid.core.utilities

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SentenceUtilitiesKTest {

    @Test
    fun testToSkipToken_shouldReturnTrue() {
        val tokens = arrayOf("-", " ", "\n", "\t")

        tokens.forEach { token ->
            assertTrue(SentenceUtilities.toSkipToken(token))
        }
    }

    @Test
    fun testToSkipTokenNoHypen_shouldReturnTrue() {
        val tokens = arrayOf(" ", "\n", "\t")

        tokens.forEach { token ->
            assertTrue(SentenceUtilities.toSkipTokenNoHyphen(token))
        }

        assertFalse { SentenceUtilities.toSkipTokenNoHyphen("-") }
    }
}
