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
package org.grobid.core.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;

/**
 * Class for holding static methods for text processing.
 *
 */
public class TextUtilities {

    public static final String punctuations = " •*,:;?.!)-−–\"“”‘’'`$]*\u2666\u2665\u2663\u2660\u00A0。、，・";
    public static final String fullPunctuations = "(（[ •*,:;?.!/)）-−–‐«»„\"“”‘’'`$#@]*\u2666\u2665\u2663\u2660\u00A0。、，・";
    public static final String restrictedPunctuations = ",:;?.!/-–«»„\"“”‘’'`*\u2666\u2665\u2663\u2660。、，・";
    public static String delimiters = "\n\r\t\f\u00A0\u200C" + fullPunctuations;

    public static final String OR = "|";
    public static final String NEW_LINE = "\n";
    public static final String SPACE = " ";
    public static final String COMMA = ",";
    public static final String QUOTE = "'";
    public static final String END_BRACKET = ")";
    public static final String START_BRACKET = "(";
    public static final String SHARP = "#";
    public static final String COLON = ":";
    public static final String DOUBLE_QUOTE = "\"";
    public static final String ESC_DOUBLE_QUOTE = "&quot;";
    public static final String LESS_THAN = "<";
    public static final String ESC_LESS_THAN = "&lt;";
    public static final String GREATER_THAN = ">";
    public static final String ESC_GREATER_THAN = "&gt;";
    public static final String AND = "&";
    public static final String ESC_AND = "&amp;";
    public static final String SLASH = "/";

    // note: be careful of catastrophic backtracking here as a consequence of PDF noise!

    private static final String ORCIDRegex = "^\\s*(?:(?:https?://)?orcid.org/)?([0-9]{4})\\-?([0-9]{4})\\-?([0-9]{4})\\-?([0-9]{3}[\\dX])\\s*$";
    static public final Pattern ORCIDPattern = Pattern.compile(ORCIDRegex);

    // the magical DOI regular expression...
    static public final Pattern DOIPattern = Pattern
            .compile("(10\\.\\d{4,5}\\/[\\S]+[^;,.\\s])");

    // a regular expression for arXiv identifiers
    // see https://arxiv.org/help/arxiv_identifier and https://arxiv.org/help/arxiv_identifier_for_services
    static public final Pattern arXivPattern = Pattern
            .compile(
                    "(arXiv\\s?(\\.org)?\\s?\\:\\s?\\d{4}\\s?\\.\\s?\\d{4,5}(v\\d+)?)|(arXiv\\s?(\\.org)?\\s?\\:\\s?[ a-zA-Z\\-\\.]*\\s?/\\s?\\d{7}(v\\d+)?)");

    // regular expression for PubMed identifiers, last group gives the PMID digits
    static public final Pattern pmidPattern = Pattern
            .compile("((PMID)|(Pub(\\s)?Med(\\s)?(ID)?))(\\s)?(\\:)?(\\s)*(\\d{1,8})");

    // regular expression for PubMed Central identifiers (note: contrary to PMID, we include the prefix PMC here, see
    // https://www.ncbi.nlm.nih.gov/pmc/pmctopmid/ for instance), last group gives the PMC ID digits
    static public final Pattern pmcidPattern = Pattern
            .compile("((PMC\\s?(ID)?)|(Pub(\\s)?Med(\\s)?(Central)?(\\s)?(ID)?))(\\s)?(\\:)?(\\s)*(\\d{1,9})");

    // a regular expression for identifying url pattern in text
    // TODO: maybe find a better regex (better == more robust, not more "standard")
    static public final Pattern urlPattern0 = Pattern
            .compile("(?i)(https?|ftp)\\s?:\\s?//\\s?[-A-Z0-9+&@#/%?=~_()|!:,.;]*[-A-Z0-9+&@#/%=~_()|]");
    static public final Pattern urlPattern = Pattern
            .compile("(?i)(https?|ftp)\\s{0,2}:\\s{0,2}//\\s{0,2}[-A-Z0-9+&@#/%?=~_()|!:.;]*[-A-Z0-9+&@#/%=~_()]");
    static public final Pattern urlPattern1 = Pattern
            .compile(
                    "(?i)(https?|ftp)\\s{0,2}:\\s{0,2}//\\s{0,2}[-A-Z0-9+&@#/%?=~_()|!:.;]*[-A-Z0-9+&@#/%=~_()]|www\\s{0,2}\\.\\s{0,2}[-A-Z0-9+&@#/%?=~_()|!:.;]*[-A-Z0-9+&@#/%=~_()]");

    // a regular expression for identifying email pattern in text
    // TODO: maybe find a better regex (better == more robust, not more "standard")
    static public final Pattern emailPattern = Pattern
            .compile("\\w+((\\.|-|_|,)\\w+)?\\s?((\\.|-|_|,)\\w+)?\\s?@\\s?\\w+(\\s?(\\.|-)\\s?\\w+)+");
    // variant: \w+(\s?(\.|-|_|,)\w+)?(\s?(\.|-|_|,)\w+)?\s?@\s?\w+(\s?(\.|\-)\s?\w+)+

    /**
     * Replace numbers in the string by a dummy character for string distance evaluations
     *
     * @param string the string to be processed.
     * @return Returns the string with numbers replaced by 'X'.
     */
    public static String shadowNumbers(String string) {
        int i = 0;
        if (string == null)
            return string;
        String res = "";
        while (i < string.length()) {
            char c = string.charAt(i);
            if (Character.isDigit(c))
                res += 'X';
            else
                res += c;
            i++;
        }
        return res;
    }

    private static int getLastPunctuationCharacter(String section) {
        int res = -1;
        for (int i = section.length() - 1; i >= 0; i--) {
            if (fullPunctuations.contains("" + section.charAt(i))) {
                res = i;
            }
        }
        return res;
    }

    /** @use LayoutTokensUtil.dehyphenize(List<LayoutToken> tokens) **/
    @Deprecated
    public static List<LayoutToken> dehyphenize(List<LayoutToken> tokens) {
        return LayoutTokensUtil.dehyphenize(tokens);
    }

    /** @use LayoutTokenUtils.doesRequireDehypenisation(List<LayoutToken> tokens, int i)**/
    @Deprecated
    protected static boolean doesRequireDehypenisation(List<LayoutToken> tokens, int i) {
        return LayoutTokensUtil.doesRequireDehypenisation(tokens, i);
    }

    public static String dehyphenize(String text) {
        GrobidAnalyzer analyser = GrobidAnalyzer.getInstance();

        final List<LayoutToken> layoutTokens = analyser.tokenizeWithLayoutToken(text);

        return LayoutTokensUtil.toText(LayoutTokensUtil.dehyphenize(layoutTokens));
    }

    public static String getLastToken(String section) {
        String lastToken = section;
        int lastSpaceIndex = section.lastIndexOf(' ');

        //The last parenthesis cover the case 'this is a (special-one) case'
        // where the lastToken before the hyphen should be 'special' and not '(special'
        /*        int lastParenthesisIndex = section.lastIndexOf('(');
        if (lastParenthesisIndex > lastSpaceIndex)
            lastSpaceIndex = lastParenthesisIndex;*/

        if (lastSpaceIndex != -1) {
            lastToken = section.substring(lastSpaceIndex + 1, section.length());
        } else {
            lastToken = section.substring(0, section.length());
        }
        return lastToken;
    }

    public static String getFirstToken(String section) {
        int firstSpaceIndex = section.indexOf(' ');

        if (firstSpaceIndex == 0) {
            return getFirstToken(section.substring(1, section.length()));
        } else if (firstSpaceIndex != -1) {
            return section.substring(0, firstSpaceIndex);
        } else {
            return section.substring(0, section.length());
        }
    }

    /**
     * Text extracted from a PDF is usually hyphenized, which is not desirable.
     * This version supposes that the end of line are lost and than hyphenation
     * could appear everywhere. So a dictionary is used to control the recognition
     * of hyphen.
     *
     * @param text the string to be processed without preserved end of lines.
     * @return Returns the dehyphenized string.
     * <p>
     * Deprecated method, not needed anymore since the @newline are preserved thanks to the LayoutTokens
     * @Use LayoutTokensUtil.dehypenize()
     */
    @Deprecated
    public static String dehyphenizeHard(String text) {
        if (text == null)
            return null;
        String res = "";

        text.replaceAll("\n", SPACE);

        StringTokenizer st = new StringTokenizer(text, "-");
        boolean hyphen = false;
        boolean failure = false;
        String lastToken = null;
        while (st.hasMoreTokens()) {
            String section = st.nextToken().trim();

            if (hyphen) {
                // we get the first token
                StringTokenizer st2 = new StringTokenizer(section, " ,.);!");
                if (st2.countTokens() > 0) {
                    String firstToken = st2.nextToken();

                    // we check if the composed token is in the lexicon
                    String hyphenToken = lastToken + firstToken;
                    //System.out.println(hyphenToken);
                    /*if (lex == null)
                             featureFactory.loadLexicon();*/
                    Lexicon lex = Lexicon.getInstance();

                    if (lex.inDictionary(hyphenToken.toLowerCase()) &
                            !(test_digit(hyphenToken))) {
                        // if yes, it is hyphenization
                        res += firstToken;
                        section = section.substring(firstToken.length(), section.length());
                    } else {
                        // if not
                        res += "-";
                        failure = true;
                    }
                } else {
                    res += "-";
                }
                hyphen = false;
            }

            // we get the last token
            hyphen = true;
            lastToken = getLastToken(section);

            if (failure) {
                res += section;
                failure = false;
            } else
                res += SPACE + section;
        }

        res = res.replace(" . ", ". ");
        res = res.replace("  ", SPACE);

        return res.trim();
    }

    /**
     * Levenstein distance between two strings
     *
     * @param s the first string to be compared.
     * @param t the second string to be compared.
     * @return Returns the Levenshtein distance.
     */
    public static int getLevenshteinDistance(String s, String t) {
        //if (s == null || t == null) {
        //	throw new IllegalArgumentException("Strings must not be null");
        //}
        int n = s.length(); // length of s
        int m = t.length(); // length of t

        if (n == 0) {
            return m;
        } else if (m == 0) {
            return n;
        }

        int p[] = new int[n + 1]; //'previous' cost array, horizontally
        int d[] = new int[n + 1]; // cost array, horizontally
        int _d[]; //placeholder to assist in swapping p and d

        // indexes into strings s and t
        int i; // iterates through s
        int j; // iterates through t

        char t_j; // jth character of t

        int cost; // cost

        for (i = 0; i <= n; i++) {
            p[i] = i;
        }

        for (j = 1; j <= m; j++) {
            t_j = t.charAt(j - 1);
            d[0] = j;

            for (i = 1; i <= n; i++) {
                cost = s.charAt(i - 1) == t_j ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
            }

            // copy current distance counts to 'previous row' distance counts
            _d = p;
            p = d;
            d = _d;
        }

        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n];
    }

    /**
     * Appending nb times the char c to the a StringBuffer...
     */
    public final static void appendN(StringBuffer buffer, char c, int nb) {
        for (int i = 0; i < nb; i++) {
            buffer.append(c);
        }
    }

    /**
     * To replace accented characters in a unicode string by unaccented equivalents:
     * é -> e, ü -> ue, ß -> ss, etc. following the standard transcription conventions
     *
     * @param input the string to be processed.
     * @return Returns the string without accent.
     */
    public final static String removeAccents(String input) {
        if (input == null)
            return null;
        final StringBuffer output = new StringBuffer();
        for (int i = 0; i < input.length(); i++) {
            switch (input.charAt(i)) {
                case '\u00C0' : // Ã€
                case '\u00C1' : // Ã
                case '\u00C2' : // Ã‚
                case '\u00C3' : // Ãƒ
                case '\u00C5' : // Ã…
                    output.append("A");
                    break;
                case '\u00C4' : // Ã„
                case '\u00C6' : // Ã†
                    output.append("AE");
                    break;
                case '\u00C7' : // Ã‡
                    output.append("C");
                    break;
                case '\u00C8' : // Ãˆ
                case '\u00C9' : // Ã‰
                case '\u00CA' : // ÃŠ
                case '\u00CB' : // Ã‹
                    output.append("E");
                    break;
                case '\u00CC' : // ÃŒ
                case '\u00CD' : // Ã
                case '\u00CE' : // ÃŽ
                case '\u00CF' : // Ã
                    output.append("I");
                    break;
                case '\u00D0' : // Ã
                    output.append("D");
                    break;
                case '\u00D1' : // Ã‘
                    output.append("N");
                    break;
                case '\u00D2' : // Ã’
                case '\u00D3' : // Ã“
                case '\u00D4' : // Ã”
                case '\u00D5' : // Ã•
                case '\u00D8' : // Ã˜
                    output.append("O");
                    break;
                case '\u00D6' : // Ã–
                case '\u0152' : // Å’
                    output.append("OE");
                    break;
                case '\u00DE' : // Ãž
                    output.append("TH");
                    break;
                case '\u00D9' : // Ã™
                case '\u00DA' : // Ãš
                case '\u00DB' : // Ã›
                    output.append("U");
                    break;
                case '\u00DC' : // Ãœ
                    output.append("UE");
                    break;
                case '\u00DD' : // Ã
                case '\u0178' : // Å¸
                    output.append("Y");
                    break;
                case '\u00E0' : // Ã
                case '\u00E1' : // Ã¡
                case '\u00E2' : // Ã¢
                case '\u00E3' : // Ã£
                case '\u00E5' : // Ã¥
                    output.append("a");
                    break;
                case '\u00E4' : // Ã¤
                case '\u00E6' : // Ã¦
                    output.append("ae");
                    break;
                case '\u00E7' : // Ã§
                    output.append("c");
                    break;
                case '\u00E8' : // Ã¨
                case '\u00E9' : // Ã©
                case '\u00EA' : // Ãª
                case '\u00EB' : // Ã«
                    output.append("e");
                    break;
                case '\u00EC' : // Ã¬
                case '\u00ED' : // Ã
                case '\u00EE' : // Ã®
                case '\u00EF' : // Ã¯
                    output.append("i");
                    break;
                case '\u00F0' : // Ã°
                    output.append("d");
                    break;
                case '\u00F1' : // Ã±
                    output.append("n");
                    break;
                case '\u00F2' : // Ã²
                case '\u00F3' : // Ã³
                case '\u00F4' : // Ã´
                case '\u00F5' : // Ãµ
                case '\u00F8' : // Ã¸
                    output.append("o");
                    break;
                case '\u00F6' : // Ã¶
                case '\u0153' : // Å“
                    output.append("oe");
                    break;
                case '\u00DF' : // ÃŸ
                    output.append("ss");
                    break;
                case '\u00FE' : // Ã¾
                    output.append("th");
                    break;
                case '\u00F9' : // Ã¹
                case '\u00FA' : // Ãº
                case '\u00FB' : // Ã»
                    output.append("u");
                    break;
                case '\u00FC' : // Ã¼
                    output.append("ue");
                    break;
                case '\u00FD' : // Ã½
                case '\u00FF' : // Ã¿
                    output.append("y");
                    break;
                default :
                    output.append(input.charAt(i));
                    break;
            }
        }
        return output.toString();
    }

    // ad hoc stopword list for the cleanField method
    public final static List<String> stopwords = Arrays
            .asList("the", "of", "and", "du", "de le", "de la", "des", "der", "an", "und", "for");

    /**
     * Remove useless punctuation at the end and beginning of a metadata field.
     * <p/>
     * Still experimental ! Use with care !
     */
    public final static String cleanField(String input0, boolean applyStopwordsFilter) {
        if (input0 == null) {
            return null;
        }
        if (input0.length() == 0) {
            return null;
        }
        String input = input0.replace(",,", ",");
        input = input.replace(", ,", ",");
        int n = input.length();

        // characters at the end
        for (int i = input.length() - 1; i > 0; i--) {
            char c = input.charAt(i);
            if ((c == ',') ||
                    (c == ' ') ||
                    (c == '.') ||
                    (c == '-') ||
                    (c == '_') ||
                    (c == '/') ||
                    //(c == ')') ||
                    //(c == '(') ||
                    (c == ':')) {
                n = i;
            } else if (c == ';') {
                // we have to check if we have an html entity finishing
                if (i - 3 >= 0) {
                    char c0 = input.charAt(i - 3);
                    if (c0 == '&') {
                        break;
                    }
                }
                if (i - 4 >= 0) {
                    char c0 = input.charAt(i - 4);
                    if (c0 == '&') {
                        break;
                    }
                }
                if (i - 5 >= 0) {
                    char c0 = input.charAt(i - 5);
                    if (c0 == '&') {
                        break;
                    }
                }
                if (i - 6 >= 0) {
                    char c0 = input.charAt(i - 6);
                    if (c0 == '&') {
                        break;
                    }
                }
                n = i;
            } else
                break;
        }

        input = input.substring(0, n);

        // characters at the beginning
        n = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ((c == ',') ||
                    (c == ' ') ||
                    (c == '.') ||
                    (c == ';') ||
                    (c == '-') ||
                    (c == '_') ||
                    //(c == ')') ||
                    //(c == '(') ||
                    (c == ':')) {
                n = i;
            } else
                break;
        }

        input = input.substring(n, input.length()).trim();

        if ((input.endsWith(")")) && (input.startsWith("("))) {
            input = input.substring(1, input.length() - 1).trim();
        }

        if ((input.length() > 12) &&
                (input.endsWith("&quot;")) &&
                (input.startsWith("&quot;"))) {
            input = input.substring(6, input.length() - 6).trim();
        }

        if (applyStopwordsFilter) {
            boolean stop = false;
            while (!stop) {
                stop = true;
                for (String word : stopwords) {
                    if (input.endsWith(SPACE + word)) {
                        input = input.substring(0, input.length() - word.length()).trim();
                        stop = false;
                        break;
                    }
                }
            }
        }

        return input.trim();
    }

    /**
     * Segment piece of text following a list of segmentation characters.
     * "hello, world." -> [ "hello", ",", "world", "." ]
     *
     * @param input the string to be processed.
     * @param input the characters creating a segment (typically space and punctuations).
     * @return Returns the string without accent.
     */
    public final static List<String> segment(String input, String segments) {
        if (input == null)
            return null;
        ArrayList<String> result = new ArrayList<String>();
        String token = null;
        String seg = " \n\t";
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int ind = seg.indexOf(c);
            if (ind != -1) {
                if (token != null) {
                    result.add(token);
                    token = null;
                }
            } else {
                int ind2 = segments.indexOf(c);
                if (ind2 == -1) {
                    if (token == null)
                        token = "" + c;
                    else
                        token += c;
                } else {
                    if (token != null) {
                        result.add(token);
                        token = null;
                    }
                    result.add("" + segments.charAt(ind2));
                }
            }
        }
        if (token != null)
            result.add(token);
        return result;
    }

    /**
     * Encode a string to be displayed in HTML
     * <p/>
     * If fullHTML encode, then all unicode characters above 7 bits are converted into
     * HTML entities
     */
    public static String HTMLEncode(String string) {
        return HTMLEncode(string, false);
    }

    public static String HTMLEncode(String string, boolean fullHTML) {
        if (string == null)
            return null;
        if (string.length() == 0)
            return string;
        //string = string.replace("@BULLET", "•");
        StringBuffer sb = new StringBuffer(string.length());
        // true if last char was blank
        boolean lastWasBlankChar = false;
        int len = string.length();
        char c;

        for (int i = 0; i < len; i++) {
            c = string.charAt(i);
            if (c == ' ') {
                // blank gets extra work,
                // this solves the problem you get if you replace all
                // blanks with &nbsp;, if you do that you loss
                // word breaking
                if (lastWasBlankChar) {
                    lastWasBlankChar = false;
                    //sb.append("&nbsp;");
                } else {
                    lastWasBlankChar = true;
                    sb.append(' ');
                }
            } else {
                lastWasBlankChar = false;
                //
                // HTML Special Chars
                if (c == '"')
                    sb.append("&quot;");
                else if (c == '\'')
                    sb.append("&apos;");
                else if (c == '&') {
                    boolean skip = false;
                    // we don't want to recode an existing hmlt entity
                    if (string.length() > i + 3) {
                        char c2 = string.charAt(i + 1);
                        char c3 = string.charAt(i + 2);
                        char c4 = string.charAt(i + 3);
                        if (c2 == 'a') {
                            if (c3 == 'm') {
                                if (c4 == 'p') {
                                    if (string.length() > i + 4) {
                                        char c5 = string.charAt(i + 4);
                                        if (c5 == ';') {
                                            skip = true;
                                        }
                                    }
                                }
                            }
                        } else if (c2 == 'q') {
                            if (c3 == 'u') {
                                if (c4 == 'o') {
                                    if (string.length() > i + 5) {
                                        char c5 = string.charAt(i + 4);
                                        char c6 = string.charAt(i + 5);
                                        if (c5 == 't') {
                                            if (c6 == ';') {
                                                skip = true;
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (c2 == 'l' || c2 == 'g') {
                            if (c3 == 't') {
                                if (c4 == ';') {
                                    skip = true;
                                }
                            }
                        }
                    }
                    if (!skip) {
                        sb.append("&amp;");
                    } else {
                        sb.append("&");
                    }
                } else if (c == '<')
                    sb.append("&lt;");
                else if (c == '>')
                    sb.append("&gt;");
                /*else if (c == '\n') {
                     // warning: this can be too much html!
                     sb.append("&lt;br/&gt;");
                 }*/
                else {
                    int ci = 0xffff & c;
                    if (ci < 160) {
                        // nothing special only 7 Bit
                        sb.append(c);
                    } else {
                        if (fullHTML) {
                            // Not 7 Bit use the unicode system
                            sb.append("&#");
                            sb.append(Integer.valueOf(ci).toString());
                            sb.append(';');
                        } else
                            sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    public static String normalizeRegex(String string) {
        string = string.replace("&", "\\\\&");
        string = string.replace("&", "\\\\&");
        string = string.replace("+", "\\\\+");
        return string;
    }

    /*
     * To convert the InputStream to String we use the BufferedReader.readLine()
     * method. We iterate until the BufferedReader return null which means
     * there's no more data to read. Each line will appended to a StringBuilder
     * and returned as String.
     */
    static public String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            //			e.printStackTrace();
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                //				e.printStackTrace();
                throw new GrobidException("An exception occurred while running Grobid.", e);
            }
        }

        return sb.toString();
    }

    /**
     * Count the number of digit in a given string.
     *
     * @param text the string to be processed.
     * @return Returns the number of digit chracaters in the string...
     */
    static public int countDigit(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c))
                count++;
        }
        return count;
    }

    /**
     * Map special ligature and characters coming from the pdf
     */
    static public String clean(String token) {
        if (token == null)
            return null;
        if (token.length() == 0)
            return token;
        String res = "";
        int i = 0;
        while (i < token.length()) {
            switch (token.charAt(i)) {
                // ligature
                case '\uFB00' : {
                    res += "ff";
                    break;
                }
                case '\uFB01' : {
                    res += "fi";
                    break;
                }
                case '\uFB02' : {
                    res += "fl";
                    break;
                }
                case '\uFB03' : {
                    res += "ffi";
                    break;
                }
                case '\uFB04' : {
                    res += "ffl";
                    break;
                }
                case '\uFB06' : {
                    res += "st";
                    break;
                }
                case '\uFB05' : {
                    res += "ft";
                    break;
                }
                // NB: \u00E6/\u00C6 (U+00E6/U+00C6) and \u0153/\u0152 (U+0153/U+0152) are intentionally NOT
                // folded here. Unlike the typographic ligatures above (\uFB00\uFB01\uFB02\u2026, genuine PDF
                // rendering artifacts), these are distinct letters used deliberately in
                // Danish, Norwegian, Icelandic, French, etc. Folding them to "ae"/"oe" was
                // data loss for those languages (issue #728); they now pass through unchanged.
                // quote
                case '\u201C' : {
                    res += "\"";
                    break;
                }
                case '\u201D' : {
                    res += "\"";
                    break;
                }
                case '\u201E' : {
                    res += "\"";
                    break;
                }
                case '\u201F' : {
                    res += "\"";
                    break;
                }
                case '\u2019' : {
                    res += "'";
                    break;
                }
                case '\u2018' : {
                    res += "'";
                    break;
                }
                // bullet uniformity
                case '\u2022' : {
                    res += "•";
                    break;
                }
                case '\u2023' : {
                    res += "•";
                    break;
                }
                case '\u2043' : {
                    res += "•";
                    break;
                }
                case '\u204C' : {
                    res += "•";
                    break;
                }
                case '\u204D' : {
                    res += "•";
                    break;
                }
                case '\u2219' : {
                    res += "•";
                    break;
                }
                case '\u25C9' : {
                    res += "•";
                    break;
                }
                case '\u25D8' : {
                    res += "•";
                    break;
                }
                case '\u25E6' : {
                    res += "•";
                    break;
                }
                case '\u2619' : {
                    res += "•";
                    break;
                }
                case '\u2765' : {
                    res += "•";
                    break;
                }
                case '\u2767' : {
                    res += "•";
                    break;
                }
                case '\u29BE' : {
                    res += "•";
                    break;
                }
                case '\u29BF' : {
                    res += "•";
                    break;
                }
                // asterix
                case '\u2217' : {
                    res += " * ";
                    break;
                }
                // typical author/affiliation markers
                case '\u2020' : {
                    res += SPACE + '\u2020';
                    break;
                }
                case '\u2021' : {
                    res += SPACE + '\u2021';
                    break;
                }
                case '\u00A7' : {
                    res += SPACE + '\u00A7';
                    break;
                }
                case '\u00B6' : {
                    res += SPACE + '\u00B6';
                    break;
                }
                case '\u204B' : {
                    res += SPACE + '\u204B';
                    break;
                }
                case '\u01C2' : {
                    res += SPACE + '\u01C2';
                    break;
                }
                // default
                default : {
                    res += token.charAt(i);
                    break;
                }
            }
            i++;
        }
        return res;
    }

    public static String formatTwoDecimals(double d) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern("#.##");
        return df.format(d);
    }

    public static String formatFourDecimals(double d) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern("#.####");
        return df.format(d);
    }

    public static boolean isAllUpperCase(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isUpperCase(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAllLowerCase(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isLowerCase(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static List<String> generateEmailVariants(String firstName, String lastName) {
        // current heuristics:
        // "First Last"
        // "First L"
        // "F Last"
        // "First"
        // "Last"
        // "Last First"
        // "Last F"

        List<String> variants = new ArrayList<String>();

        if (lastName != null) {
            variants.add(lastName);

            if (firstName != null) {
                variants.add(firstName + SPACE + lastName);
                variants.add(lastName + SPACE + firstName);

                if (firstName.length() > 1) {
                    String firstInitial = firstName.substring(0, 1);

                    variants.add(firstInitial + SPACE + lastName);
                    variants.add(lastName + SPACE + firstInitial);
                }

                if (lastName.length() > 1) {
                    String lastInitial = lastName.substring(0, 1);

                    variants.add(firstName + SPACE + lastInitial);
                }
            }
        } else {
            if (firstName != null) {
                variants.add(firstName);
            }
        }

        return variants;
    }

    /**
     * This is a re-implementation of the capitalizeFully of Apache commons lang, because it appears not working
     * properly.
     * <p/>
     * Convert a string so that each word is made up of a titlecase character and then a series of lowercase
     * characters. Words are defined as token delimited by one of the character in delimiters or the beginning
     * of the string.
     */
    public static String capitalizeFully(String input, String delimiters) {
        if (input == null) {
            return null;
        }

        //input = input.toLowerCase();
        String output = "";
        boolean toUpper = true;
        for (int c = 0; c < input.length(); c++) {
            char ch = input.charAt(c);

            if (delimiters.indexOf(ch) != -1) {
                toUpper = true;
                output += ch;
            } else {
                if (toUpper == true) {
                    output += Character.toUpperCase(ch);
                    toUpper = false;
                } else {
                    output += Character.toLowerCase(ch);
                }
            }
        }
        return output;
    }

    public static String wordShape(String word) {
        StringBuilder shape = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (Character.isLetter(c)) {
                if (Character.isUpperCase(c)) {
                    shape.append("X");
                } else {
                    shape.append("x");
                }
            } else if (Character.isDigit(c)) {
                shape.append("d");
            } else {
                shape.append(c);
            }
        }

        StringBuilder finalShape = new StringBuilder().append(shape.charAt(0));

        String suffix = "";
        if (word.length() > 2) {
            suffix = shape.substring(shape.length() - 2);
        } else if (word.length() > 1) {
            suffix = shape.substring(shape.length() - 1);
        }

        StringBuilder middle = new StringBuilder();
        if (shape.length() > 3) {
            char ch = shape.charAt(1);
            for (int i = 1; i < shape.length() - 2; i++) {
                middle.append(ch);
                while (ch == shape.charAt(i) && i < shape.length() - 2) {
                    i++;
                }
                ch = shape.charAt(i);
            }

            if (ch != middle.charAt(middle.length() - 1)) {
                middle.append(ch);
            }
        }
        return finalShape.append(middle).append(suffix).toString();

    }

    public static String wordShapeTrimmed(String word) {
        StringBuilder shape = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (Character.isLetter(c)) {
                if (Character.isUpperCase(c)) {
                    shape.append("X");
                } else {
                    shape.append("x");
                }
            } else if (Character.isDigit(c)) {
                shape.append("d");
            } else {
                shape.append(c);
            }
        }

        StringBuilder middle = new StringBuilder();

        char ch = shape.charAt(0);
        for (int i = 0; i < shape.length(); i++) {
            middle.append(ch);
            while (ch == shape.charAt(i) && i < shape.length() - 1) {
                i++;
            }
            ch = shape.charAt(i);
        }

        if (ch != middle.charAt(middle.length() - 1)) {
            middle.append(ch);
        }

        return middle.toString();

    }

    /**
     * Give the punctuation profile of a line, i.e. the concatenation of all the punctuations
     * occurring in the line.
     *
     * @param line the string corresponding to a line
     * @return the punctuation profile as a string, empty string is no punctuation
     * @throws Exception
     */
    public static String punctuationProfile(String line) {
        String profile = "";
        if ((line == null) || (line.length() == 0)) {
            return profile;
        }
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ') {
                continue;
            }
            if (fullPunctuations.indexOf(c) != -1)
                profile += c;
        }
        return profile;
    }

    /**
     * Return the number of token in a line given an existing global tokenization and a current
     * start position of the line in this global tokenization.
     *
     * @param line           the string corresponding to a line
     * @param currentLinePos position of the line in the tokenization
     * @param tokenization   the global tokenization where the line appears
     * @return the punctuation profile as a string, empty string is no punctuation
     * @throws Exception
     */
    public static int getNbTokens(String line, int currentLinePos, List<String> tokenization)
            throws Exception {
        if ((line == null) || (line.length() == 0))
            return 0;
        String currentToken = tokenization.get(currentLinePos);
        while ((currentLinePos < tokenization.size()) &&
                (currentToken.equals(" ") || currentToken.equals("\n"))) {
            currentLinePos++;
            currentToken = tokenization.get(currentLinePos);
        }
        if (!line.trim().startsWith(currentToken)) {
            System.out.println("out of sync. : " + currentToken);
            throw new IllegalArgumentException("line start does not match given tokenization start");
        }
        int nbTokens = 0;
        int posMatch = 0; // current position in line
        for (int p = currentLinePos; p < tokenization.size(); p++) {
            currentToken = tokenization.get(p);
            posMatch = line.indexOf(currentToken, posMatch);
            if (posMatch == -1)
                break;
            nbTokens++;
        }
        return nbTokens;
    }

    /**
     * Ensure that special XML characters are correctly encoded.
     */
    public static String trimEncodedCharaters(String string) {
        return string.replaceAll("&amp\\s+;", "&amp;")
                .replaceAll("&quot\\s+;|&amp;quot\\s*;", "&quot;")
                .replaceAll("&lt\\s+;|&amp;lt\\s*;", "&lt;")
                .replaceAll("&gt\\s+;|&amp;gt\\s*;", "&gt;")
                .replaceAll("&apos\\s+;|&amp;apos\\s*;", "&apos;");
    }

    public static boolean filterLine(String line) {
        boolean filter = false;
        if (StringUtils.isEmpty(line)) {
            filter = true;
        } else if (line.contains("@IMAGE") || line.contains("@PAGE")) {
            filter = true;
        }
        return filter;
    }

    /**
     * The equivalent of String.replaceAll() for StringBuilder
     */
    public static StringBuilder replaceAll(StringBuilder sb, String regex, String replacement) {
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(sb);
        int start = 0;
        while (m.find(start)) {
            sb.replace(m.start(), m.end(), replacement);
            start = m.start() + replacement.length();
        }
        return sb;
    }

    /**
     * Return the prefix of a string.
     */
    public static String prefix(String s, int count) {
        if (s == null) {
            return null;
        }

        if (s.length() < count) {
            return s;
        }

        return s.substring(0, count);
    }

    /**
     * Return the suffix of a string.
     */
    public static String suffix(String s, int count) {
        if (s == null) {
            return null;
        }

        if (s.length() < count) {
            return s;
        }

        return s.substring(s.length() - count);
    }

    public static String JSONEncode(String json) {
        // we assume all json string will be bounded by double quotes
        return json.replaceAll("\"", "\\\"").replaceAll("\n", "\\\n");
    }

    public static String strrep(char c, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(c);
        }
        return builder.toString();
    }

    public static int getOccCount(String term, String string) {
        return StringUtils.countMatches(term, string);
    }

    /**
     * Test for the current string contains at least one digit.
     *
     * @param tok the string to be processed.
     * @return true if contains a digit
     */
    public static boolean test_digit(String tok) {
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
     * Useful for recognising an acronym candidate: check if a text is only
     * composed of upper case, dot and digit characters
     */
    public static boolean isAllUpperCaseOrDigitOrDot(String text) {
        for (int i = 0; i < text.length(); i++) {
            final char charAt = text.charAt(i);
            if (!Character.isUpperCase(charAt) && !Character.isDigit(charAt) && charAt != '.') {
                return false;
            }
        }
        return true;
    }

    /**
     * Remove indicated leading and trailing characters from a string
     **/
    public static String removeLeadingAndTrailingChars(String text, String leadingChars, String trailingChars) {
        text = StringUtils.stripStart(text, leadingChars);
        text = StringUtils.stripEnd(text, trailingChars);
        return text;
    }

    /**
     * Remove indicated leading and trailing characters from a string represented as a list of LayoutToken.
     * Indicated leading and trailing characters must be matching exactly the layout token text content.
     **/
    public static List<LayoutToken> removeLeadingAndTrailingCharsLayoutTokens(
            List<LayoutToken> tokens,
            String leadingChars,
            String trailingChars) {
        if (tokens == null)
            return tokens;
        if (tokens.size() == 0)
            return tokens;

        int start = 0;
        for (int i = 0; i < tokens.size(); i++) {
            LayoutToken token = tokens.get(i);
            if (token.getText() == null || token.getText().length() == 0) {
                start++;
                continue;
            } else if (token.getText().length() > 1) {
                break;
            } else if (leadingChars.contains(token.getText())) {
                start++;
            } else
                break;
        }

        int end = tokens.size();
        for (int i = end; i > 0; i--) {
            LayoutToken token = tokens.get(i - 1);
            if (token.getText() == null || token.getText().length() == 0) {
                end--;
                continue;
            } else if (token.getText().length() > 1) {
                break;
            } else if (trailingChars.contains(token.getText())) {
                end--;
            } else
                break;
        }

        if (start == end || end < start) {
            // we return an empty list
            return new ArrayList<LayoutToken>();
        }

        return tokens.subList(start, end);
    }

    /**
     * Remove ad-hoc list of stopwords for extracted field
     **/
    public static String removeFieldStopwords(String text) {
        List<String> tokens = GrobidAnalyzer.getInstance().tokenize(text);
        List<String> filteredTokens = new ArrayList<>();
        for (String token : tokens) {
            if (!stopwords.contains(token)) {
                filteredTokens.add(token);
            }
        }

        String finalText = String.join("", filteredTokens);
        finalText = finalText.replace("'s", " ");
        finalText = finalText.replace(",", " ");
        finalText = finalText.replace(".", " ");
        finalText = finalText.replaceAll("( )+", " ");
        return finalText;
    }

    /**
     * Detect in a string possible trailing acronyms, introduced in parenthesis after a full name.
     * We can typically use it on the affiliation full name, but it can also be applied to longer
     * texts.
     *
     * Return a Map with an acronym position and the corresponding full name position
     **/
    public static Map<OffsetPosition, OffsetPosition> acronymCandidates(List<LayoutToken> tokens) {
        Map<OffsetPosition, OffsetPosition> acronyms = null;

        boolean openParenthesis = false;
        int posParenthesis = -1;
        int i = 0;
        LayoutToken acronym = null;
        for (LayoutToken token : tokens) {
            if (token.getText() == null) {
                i++;
                continue;
            }
            if (token.getText().equals("(")) {
                openParenthesis = true;
                posParenthesis = i;
                acronym = null;
            } else if (token.getText().equals(")")) {
                openParenthesis = false;
            } else if (openParenthesis) {
                if (TextUtilities.isAllUpperCaseOrDigitOrDot(token.getText())) {
                    acronym = token;
                } else {
                    acronym = null;
                }
            }

            if ((acronym != null) && (!openParenthesis)) {
                // check if this possible acronym matches an immediately preceding term
                int j = posParenthesis;
                int k = acronym.getText().length();
                boolean stop = false;
                while ((k > 0) && (!stop)) {
                    k--;
                    char c = acronym.getText().toLowerCase().charAt(k);
                    while ((j > 0) && (!stop)) {
                        j--;
                        if (tokens.get(j) != null) {
                            String tok = tokens.get(j).getText();
                            if (tok.trim().length() == 0 || delimiters.contains(tok))
                                continue;
                            boolean numericMatch = false;
                            if ((tok.length() > 1) && StringUtils.isNumeric(tok)) {
                                // when the token is all digit, it often appears in full as such in the
                                // acronym (e.g. GDF15)
                                String acronymCurrentPrefix = acronym.getText().substring(0, k + 1);
                                //System.out.println("acronymCurrentPrefix: " + acronymCurrentPrefix);
                                if (acronymCurrentPrefix.endsWith(tok)) {
                                    // there is a full number match
                                    k = k - tok.length() + 1;
                                    numericMatch = true;
                                    //System.out.println("numericMatch is: " + numericMatch);
                                }
                            }

                            if ((tok.toLowerCase().charAt(0) == c) || numericMatch) {
                                if (k == 0) {
                                    if (acronyms == null)
                                        acronyms = new HashMap<>();
                                    List<LayoutToken> baseTokens = new ArrayList<>();
                                    StringBuilder builder = new StringBuilder();
                                    for (int l = j; l < posParenthesis; l++) {
                                        builder.append(tokens.get(l));
                                        baseTokens.add(tokens.get(l));
                                    }

                                    OffsetPosition acronymPosition = new OffsetPosition();
                                    acronymPosition.start = acronym.getOffset();
                                    acronymPosition.end = acronym.getOffset() + acronym.getText().length();

                                    OffsetPosition basePosition = new OffsetPosition();
                                    basePosition.start = tokens.get(j).getOffset();
                                    basePosition.end = tokens.get(j).getOffset() + acronym.getText().length();

                                    acronyms.put(acronymPosition, basePosition);
                                    stop = true;
                                } else
                                    break;
                            } else {
                                stop = true;
                            }
                        }
                    }
                }
                acronym = null;
                posParenthesis = -1;
            }
            i++;
        }
        return acronyms;
    }

    /**
     * Detect in a short string field a possible trailing acronyms, introduced in parenthesis after a full name.
     * We can typically use it on the affiliation full name.
     *
     * Return the token offset positions of the acronym and the corresponding full name, null otherwise
     **/
    public static org.apache.commons.lang3.tuple.Pair<OffsetPosition, OffsetPosition> fieldAcronymCandidate(
            List<LayoutToken> tokens) {
        if (tokens == null || tokens.size() == 0)
            return null;

        boolean openParenthesis = false;
        int posParenthesis = -1;
        int i = 0;
        LayoutToken acronym = null;
        int acronymStartIndex = 0;
        OffsetPosition acronymPosition = null;
        OffsetPosition basePosition = null;
        for (LayoutToken token : tokens) {
            if (token.getText() == null) {
                i++;
                continue;
            }
            if (token.getText().equals("(")) {
                openParenthesis = true;
                posParenthesis = i;
                acronym = null;
            } else if (token.getText().equals(")")) {
                openParenthesis = false;
            } else if (openParenthesis) {
                if (TextUtilities.isAllUpperCaseOrDigitOrDot(token.getText())) {
                    acronym = token;
                    acronymStartIndex = i;
                } else {
                    acronym = null;
                }
            }

            if ((acronym != null) && (!openParenthesis)) {
                acronymPosition = new OffsetPosition();
                acronymPosition.start = acronymStartIndex;
                acronymPosition.end = acronymStartIndex + 1;

                int j = posParenthesis;
                boolean stop = false;
                while ((j > 0) && (!stop)) {
                    j--;
                    String tok = tokens.get(j).getText();
                    if (tok.trim().length() == 0 || delimiters.contains(tok))
                        continue;
                    stop = true;
                }

                basePosition = new OffsetPosition();
                basePosition.start = 0;
                basePosition.end = j + 1;
            }

            i++;
        }

        if (acronymPosition != null && basePosition != null)
            return org.apache.commons.lang3.tuple.Pair.of(acronymPosition, basePosition);
        else
            return null;
    }

    public static List<OffsetPosition> matchTokenAndString(
            List<LayoutToken> layoutTokens,
            String text,
            List<OffsetPosition> positions) {
        List<OffsetPosition> newPositions = new ArrayList<>();
        StringBuilder accumulator = new StringBuilder();
        int pos = 0;
        int textPositionOfToken = 0;

        for (OffsetPosition position : positions) {
            List<LayoutToken> annotationTokens = layoutTokens.subList(position.start, position.end);
            boolean first = true;
            accumulator = new StringBuilder();
            for (int i = 0; i < annotationTokens.size(); i++) {
                LayoutToken token = annotationTokens.get(i);
                if (StringUtils.isEmpty(token.getText()))
                    continue;
                textPositionOfToken = text.indexOf(token.getText(), pos);
                if (textPositionOfToken != -1) {
                    //We update pos only at the first token of the annotation positions
                    if (first) {
                        pos = textPositionOfToken;
                        first = false;
                    }
                    accumulator.append(token);
                } else {
                    if (SentenceUtilities.toSkipToken(token.getText())) {
                        continue;
                    }
                    if (StringUtils.isNotEmpty(accumulator)) {
                        int accumulatorTextLength = accumulator.toString().length();
                        int start = text.indexOf(accumulator.toString(), pos);
                        int end = start + accumulatorTextLength;
                        newPositions.add(new OffsetPosition(start, end));
                        pos = end;
                        break;
                    }
                    pos = textPositionOfToken;
                }
            }
            if (StringUtils.isNotEmpty(accumulator)) {
                int annotationTextLength = accumulator.toString().length();
                int start = text.indexOf(accumulator.toString(), pos);
                int end = start + annotationTextLength;
                newPositions.add(new OffsetPosition(start, end));
                pos = end;
                accumulator = new StringBuilder();
            }

        }
        if (StringUtils.isNotEmpty(accumulator)) {
            int start = text.indexOf(accumulator.toString(), pos);
            newPositions.add(new OffsetPosition(start, start + accumulator.toString().length()));
        }

        return newPositions;
    }
}
