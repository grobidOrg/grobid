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
