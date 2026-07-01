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
package org.grobid.core.features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.OffsetPosition;

/**
 * Class providing a toolkit for managing and creating features or string sequence tagging problems.
 *
 */
public class FeatureFactory {

    private static FeatureFactory instance;

    public boolean newline = true;
    public Lexicon lexicon = Lexicon.getInstance();

    public Pattern year = Pattern.compile("[1,2][0-9][0-9][0-9]");
    public Pattern http = Pattern.compile("http(s)?");
    public Pattern isDigit = Pattern.compile("^\\d+$");
    public Pattern email2 = Pattern.compile("\\w+([.-]\\w+)*@\\w+([.-]\\w+)+");
    public Pattern email = Pattern.compile(
            "^(?:[a-zA-Z0-9_'^&amp;/+-])+(?:\\.(?:[a-zA-Z0-9_'^&amp;/+-])+)*@(?:(?:\\[?(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))\\.){3}(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\]?)|(?:[a-zA-Z0-9-]+\\.)+(?:[a-zA-Z]){2,}\\.?)$");
    public Pattern acronym = Pattern.compile("[A-Z]\\.([A-Z]\\.)*");
    public Pattern isPunct = Pattern.compile("^[\\,\\:;\\?\\.]+$");

    static public List<String> KEYWORDSPUB = Arrays.asList(
            new String[]{
                    "Journal",
                    "journal",
                    "Proceedings",
                    "proceedings",
                    "Conference",
                    "conference",
                    "Workshop",
                    "workshop",
                    "Symposium",
                    "symposium"
            });

    static public List<String> MONTHS = new ArrayList<String>() {
        {
            add("january");
            add("february");
            add("march");
            add("april");
            add("may");
            add("june");
            add("july");
            add("august");
            add("september");
            add("october");
            add("november");
            add("december");
            add("jan");
            add("feb");
            add("mar");
            add("apr");
            add("may");
            add("jun");
            add("jul");
            add("aug");
            add("sep");
            add("oct");
            add("nov");
            add("dec");
        }
    };

    static public List<String> COUNTRY_CODES = new ArrayList<String>() {
        {
            add("US");
            add("EP");
            add("WO");
            add("DE");
            add("AU");
            add("GB");
            add("DK");
            add("BE");
            add("AT");
            add("CN");
            add("KR");
            add("EA");
            add("CH");
            add("JP");
            add("FR");
            add("UK");
            add("RU");
            add("CA");
            add("NL");
            add("DD");
            add("SE");
            add("FI");
            add("MX");
            add("OA");
            add("AP");
            add("AR");
            add("BR");
            add("BG");
            add("CL");
            add("GR");
            add("HU");
            add("IS");
            add("IN");
            add("IE");
            add("IL");
            add("IT");
            add("LU");
            add("NO");
            add("NZ");
            add("PL");
            add("RU");
            add("ES");
            add("TW");
            add("TR");
        }
    };

    static public List<String> KIND_CODES = new ArrayList<String>() {
        {
            add("A");
            add("B");
            add("C");
            add("U");
            add("P");
        }
    };;

    // hidden constructor
    private FeatureFactory() {
    }

    public static FeatureFactory getInstance() {
        if (instance == null) {
            synchronized (FeatureFactory.class) {
                if (instance == null) {
                    instance = new FeatureFactory();
                }
            }
        }
        return instance;
    }

    /**
     * Test if the first letter of the string is a capital letter
     */
    public boolean test_first_capital(String tok) {
        if (tok == null)
            return false;
        if (tok.length() == 0)
            return false;
        char a = tok.charAt(0);
        if (Character.isUpperCase(a))
            return true;
        else
            return false;
    }

    /**
     * Test if all the letters of the string are capital letters
     * (characters can be also digits which are then ignored)
     */
    public boolean test_all_capital(String tok) {
        if (tok == null)
            return false;
        if (tok.length() == 0)
            return false;
        char a;
        for (int i = 0; i < tok.length(); i++) {
            a = tok.charAt(i);
            if (Character.isLowerCase(a))
                return false;
        }
        return true;
    }

    /**
     * Test for a given character occurrence in the string
     */
    public boolean test_char(String tok, char c) {
        if (tok == null)
            return false;
        if (tok.length() == 0)
            return false;
        int i = tok.indexOf(c);
        if (i == -1)
            return false;
        else
            return true;
    }

    /**
     * Test for the current string contains at least one digit
     */
    public boolean test_digit(String tok) {
        if (tok == null)
            return false;
        if (tok.length() == 0)
            return false;
        char a;
        for (int i = 0; i < tok.length(); i++) {
            a = tok.charAt(i);
            if (Character.isDigit(a))
                return true;
        }
        return false;
    }

    /**
     * Test for the current string contains only digit
     */
    public boolean test_number(String tok) {
        if (tok == null)
            return false;
        if (tok.length() == 0)
            return false;
        char a;
        for (int i = 0; i < tok.length(); i++) {
            a = tok.charAt(i);
            if (!Character.isDigit(a))
                return false;
        }
        return true;
    }

    /**
     * Test for the current string is a number or a decimal number, i.e. containing only digits or ",", "."
     */
    public boolean test_complex_number(String tok) {
        if (tok == null)
            return false;
        if (tok.length() == 0)
            return false;
        char a;
        for (int i = 0; i < tok.length(); i++) {
            a = tok.charAt(i);
            if ((!Character.isDigit(a)) & (a != ',') & (a != '.'))
                return false;
        }
        return true;
    }

    /**
     * Test if the current string is a common name
     */
    public boolean test_common(String tok) {
        if (tok == null)
            return false;
        else if (tok.length() == 0)
            return false;
        else
            return lexicon.inDictionary(tok.trim().toLowerCase());
    }

    /**
     * Test if the current string is a first name or family name
     */
    public boolean test_names(String tok) {
        return (lexicon.inFirstNames(tok.toLowerCase()) || lexicon.inLastNames(tok.toLowerCase()));
    }

    /**
     * Test if the current string is a family name
     */
    public boolean test_first_names(String tok) {
        return lexicon.inFirstNames(tok.toLowerCase());
    }

    /**
     * Test if the current string is a family name
     */
    public boolean test_last_names(String tok) {
        return lexicon.inLastNames(tok.toLowerCase());
    }

    /**
     * Test if the current string refers to a month
     */
    public boolean test_month(String tok) {
        return MONTHS.contains(tok.toLowerCase());
    }

    /**
     * Test if the current string refers to country code
     */
    public boolean test_country_codes(String tok) {
        return COUNTRY_CODES.contains(tok);
    }

    /**
     * Test if the current string refers to a kind code
     */
    public boolean test_kind_codes(String tok) {
        return KIND_CODES.contains(tok);
    }

    /**
     * Test if the current string refers to a country
     */
    public boolean test_country(String tok) {
        return lexicon.isCountry(tok.toLowerCase());
    }

    /**
     * Test if the current string refers to a known city
     */
    public boolean test_city(String tok) {
        List<OffsetPosition> pos = lexicon.tokenPositionsCityNames(tok.toLowerCase());
        if ((pos != null) && (pos.size() > 0))
            return true;
        else
            return false;
    }

    /**
     * Given an integer value between 0 and total, discretized into nbBins following a linear scale
     */
    public int linearScaling(int pos, int total, int nbBins) {
        if (pos >= total)
            return nbBins;
        if (pos <= 0)
            return 0;
        float rel = (float) pos / total;
        float rel2 = (rel * nbBins);// + 1;
        return ((int) rel2);
    }

    /**
     * Given an double value between 0.0 and total, discretized into nbBins following a linear scale
     */
    public int linearScaling(double pos, double total, int nbBins) {
        if (pos >= total)
            return nbBins;
        if (pos <= 0)
            return 0;
        double rel = pos / total;
        double rel2 = (rel * nbBins);// + 1;
        return ((int) rel2);
    }

    /**
     * Given an double value between 0.0 and total, discretized into nbBins following a log scale
     */
    public int logScaling(double pos, double total, int nbBins) {
        //System.out.println("total: " + total + " / pos: " + pos);
        if (pos >= total)
            return nbBins;
        if (pos <= 0)
            return 0;
        double max = Math.log(total + 1);
        double val = Math.log(pos + 1);
        //System.out.println("max: " + max + " / val: " + val);
        double rel = val / max;
        double rel2 = (rel * nbBins);
        return ((int) rel2);
    }

    /**
     * Transform a text in a text pattern where punctuations are ignored and
     * remaining text in lowercase
     */
    public String getPattern(String text) {
        String pattern = text.replaceAll("[^a-zA-Z]", "").toLowerCase();
        //pattern = pattern.replaceAll("[0-9]", "X");
        return pattern;
    }
}
