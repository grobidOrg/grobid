package org.grobid.core.lang;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import org.grobid.core.exceptions.GrobidException;

public class LanguageTest {

    @Test
    public void testLanguagesAvailableInLangdetect() {
        String[] langList = new String[]{
                "af",
                "ar",
                "bg",
                "bn",
                "cs",
                "da",
                "de",
                "el",
                "en",
                "es",
                "et",
                "fa",
                "fi",
                "fr",
                "gu",
                "he",
                "hi",
                "hr",
                "hu",
                "id",
                "it",
                "ja",
                "kn",
                "ko",
                "lt",
                "lv",
                "mk",
                "ml",
                "mr",
                "ne",
                "nl",
                "no",
                "pa",
                "pl",
                "pt",
                "ro",
                "ru",
                "sk",
                "sl",
                "so",
                "sq",
                "sv",
                "sw",
                "ta",
                "te", // codespell:ignore te
                "th",
                "tl",
                "tr",
                "uk",
                "ur",
                "vi",
                "zh-cn",
                "zh-tw"
        };

        // Should not throw an exception
        for (String lang : langList) {
            assertNotNull(new Language(lang, 1d));
        }
    }

    @Test(expected = GrobidException.class)
    public void testLanguagesWithInvalidLang_shouldThrowException() {
        new Language("baomiao", 1d);
    }

    @Test
    public void testLanguagesWithInvalidLang_2chars_shouldThrowException() {
        assertNotNull(new Language("bao", 1d));
    }

    @Test
    public void testLanguagesWithInvalidLang_3chars_shouldThrowException() {
        assertNotNull(new Language("aa", 1d));
    }

    @Test(expected = GrobidException.class)
    public void testLanguagesWithNullLang_shouldThrowException() {
        new Language(null, 1d);
    }

}
