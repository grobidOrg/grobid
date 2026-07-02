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

import org.grobid.core.exceptions.GrobidException;

/**
 * Language recognition result
 */
public final class Language {
    //common language constants (TBD use an external ISO_639-1 reference lib.)
    public static final String EN = "en";
    public static final String DE = "de";
    public static final String FR = "fr";
    public static final String IT = "it";
    public static final String ES = "es";
    public static final String JA = "ja";
    public static final String AR = "ar";
    public static final String ZH = "zh";
    public static final String RU = "ru";
    public static final String PT = "pt";
    public static final String UK = "uk";
    public static final String LN = "nl";
    public static final String PL = "pl";
    public static final String SV = "sv";
    public static final String KO = "ko";

    private String lang;
    private double conf;

    // default construction for jackson mapping
    public Language() {
    }

    public Language(String langId) {
        if (langId == null) {
            throw new GrobidException("Language id cannot be null");
        }

        if ((langId.length() != 3 && langId.length() != 2 && (!langId.equals("sorb")) &&
                (!langId.equals("zh-cn")) && (!langId.equals("zh-tw")))
                || !(Character.isLetter(langId.charAt(0))
                        && Character.isLetter(langId.charAt(1)))) {
            throw new GrobidException("Language id should consist of two or three letters, but was: " + langId);
        }

        this.lang = langId;
        this.conf = 1.0;
    }

    public Language(String langId, double confidence) {
        if (langId == null) {
            throw new GrobidException("Language id cannot be null");
        }

        if ((langId.length() != 3 && langId.length() != 2 && (!langId.equals("sorb")) &&
                (!langId.equals("zh-cn")) && (!langId.equals("zh-tw")))
                || !(Character.isLetter(langId.charAt(0))
                        && Character.isLetter(langId.charAt(1)))) {
            throw new GrobidException("Language id should consist of two or three letters, but was: " + langId);
        }

        this.lang = langId;
        this.conf = confidence;
    }

    public boolean isChinese() {
        return "zh".equals(lang) || "zh-cn".equals(lang) || "zh-tw".equals(lang);
    }

    public boolean isJapaneses() {
        return "ja".equals(lang);
    }

    public boolean isKorean() {
        return "kr".equals(lang) || "ko".equals(lang);
    }

    public boolean isArabic() {
        return "ar".equals(lang);
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public double getConf() {
        return conf;
    }

    public void setConf(double conf) {
        this.conf = conf;
    }

    @Override
    public String toString() {
        return lang + ";" + conf;
    }

    public String toJSON() {
        return "{\"lang\":\"" + lang + "\", \"conf\": " + conf + "}";
    }
}
