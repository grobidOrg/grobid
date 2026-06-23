package org.grobid.core.lexicon;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.*;
import java.util.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.Pair;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;

/**
 * Class for fast matching of word sequences over text stream.
 *
 */
public final class FastMatcher {
    private Map terms = null;

    public FastMatcher() {
        if (terms == null) {
            terms = new HashMap();
        }
    }

    public FastMatcher(File file) {
        if (!file.exists()) {
            throw new GrobidResourceException("Cannot add term to matcher, because file '"
                    +
                    file.getAbsolutePath()
                    + "' does not exist.");
        }
        if (!file.canRead()) {
            throw new GrobidResourceException("Cannot add terms to matcher, because cannot read file '"
                    +
                    file.getAbsolutePath()
                    + "'.");
        }
        try {
            loadTerms(file, GrobidAnalyzer.getInstance(), false);
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid FastMatcher.", e);
        }
    }

    public FastMatcher(File file, org.grobid.core.analyzers.Analyzer analyzer) {
        if (!file.exists()) {
            throw new GrobidResourceException("Cannot add term to matcher, because file '"
                    +
                    file.getAbsolutePath()
                    + "' does not exist.");
        }
        if (!file.canRead()) {
            throw new GrobidResourceException("Cannot add terms to matcher, because cannot read file '"
                    +
                    file.getAbsolutePath()
                    + "'.");
        }
        try {
            loadTerms(file, analyzer, false);
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid FastMatcher.", e);
        }
    }

    public FastMatcher(File file, org.grobid.core.analyzers.Analyzer analyzer, boolean caseSensitive) {
        if (!file.exists()) {
            throw new GrobidResourceException("Cannot add term to matcher, because file '"
                    +
                    file.getAbsolutePath()
                    + "' does not exist.");
        }
        if (!file.canRead()) {
            throw new GrobidResourceException("Cannot add terms to matcher, because cannot read file '"
                    +
                    file.getAbsolutePath()
                    + "'.");
        }
        try {
            loadTerms(file, analyzer, caseSensitive);
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid FastMatcher.", e);
        }
    }

    public FastMatcher(InputStream is) {
        try {
            loadTerms(is, GrobidAnalyzer.getInstance(), false);
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid FastMatcher.", e);
        }
    }

    public FastMatcher(InputStream is, org.grobid.core.analyzers.Analyzer analyzer) {
        try {
            loadTerms(is, analyzer, false);
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid FastMatcher.", e);
        }
    }

    public FastMatcher(InputStream is, org.grobid.core.analyzers.Analyzer analyzer, boolean caseSensitive) {
        try {
            loadTerms(is, analyzer, caseSensitive);
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid FastMatcher.", e);
        }
    }

    /**
     * Load a set of terms to the fast matcher from a file listing terms one per line
     */
    public int loadTerms(File file) throws IOException {
        InputStream fileIn = new FileInputStream(file);
        return loadTerms(fileIn, GrobidAnalyzer.getInstance(), false);
    }

    /**
     * Load a set of terms to the fast matcher from a file listing terms one per line
     */
    public int loadTerms(File file, boolean caseSensitive) throws IOException {
        InputStream fileIn = new FileInputStream(file);
        return loadTerms(fileIn, GrobidAnalyzer.getInstance(), caseSensitive);
    }

    /**
     * Load a set of terms to the fast matcher from a file listing terms one per line
     */
    public int loadTerms(File file, org.grobid.core.analyzers.Analyzer analyzer, boolean caseSensitive)
            throws IOException {
        InputStream fileIn = new FileInputStream(file);
        return loadTerms(fileIn, analyzer, caseSensitive);
    }

    /**
     * Load a set of term to the fast matcher from an input stream
     */
    public int loadTerms(InputStream is, org.grobid.core.analyzers.Analyzer analyzer, boolean caseSensitive)
            throws IOException {
        if (terms == null) {
            terms = new HashMap();
        }
        int nbTerms = 0;
        try (InputStreamReader reader = new InputStreamReader(is, UTF_8);
                BufferedReader bufReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufReader.readLine()) != null) {
                if (line.length() == 0)
                    continue;
                line = UnicodeUtil.normaliseText(line);
                line = StringUtils.normalizeSpace(line);
                if (!caseSensitive)
                    line = line.toLowerCase();
                nbTerms += loadTerm(line, analyzer, true);
            }
        } finally {
            // Close the input stream if it's not already closed
            if (is != null) {
                is.close();
            }
        }
        return nbTerms;
    }

    /**
     * Load a term to the fast matcher, by default the standard delimiters will be ignored
     */
    public int loadTerm(String term, org.grobid.core.analyzers.Analyzer analyzer) {
        return loadTerm(term, analyzer, true);
    }

    /**
     * Load a term to the fast matcher, by default the loading will be case sensitive
     */
    public int loadTerm(String term, org.grobid.core.analyzers.Analyzer analyzer, boolean ignoreDelimiters) {
        return loadTerm(term, analyzer, ignoreDelimiters, true);
    }

    /**
     * Load a term to the fast matcher
     */
    public int loadTerm(
            String term,
            org.grobid.core.analyzers.Analyzer analyzer,
            boolean ignoreDelimiters,
            boolean caseSensitive) {
        int nbTerms = 0;
        if (isBlank(term))
            return 0;
        Map t = terms;
        List<String> tokens = analyzer.tokenize(term, new Language("en", 1.0));
        for (String token : tokens) {
            if (token.length() == 0) {
                continue;
            }
            if (token.equals(" ") || token.equals("\n")) {
                continue;
            }
            if (ignoreDelimiters && (delimiters.indexOf(token) != -1)) {
                continue;
            }
            if (!caseSensitive) {
                token = token.toLowerCase();
            }
            Map t2 = (Map) t.get(token);
            if (t2 == null) {
                t2 = new HashMap();
                t.put(token, t2);
            }
            t = t2;
        }
        // end of the term
        if (t != terms) {
            Map t2 = (Map) t.get("#");
            if (t2 == null) {
                t2 = new HashMap();
                t.put("#", t2);
            }
            nbTerms++;
            t = terms;
        }
        return nbTerms;
    }

    private static String delimiters = TextUtilities.delimiters;

    /**
     * Identify terms in a piece of text and gives corresponding token positions.
     * All the matches are returned.
     *
     * @param text: the text to be processed
     * @return the list of offset positions of the matches, an empty list if no match have been found
     */
    public List<OffsetPosition> matchToken(String text) {
        return matchToken(text, false);
    }

    /**
     * Identify terms in a piece of text and gives corresponding token positions.
     * All the matches are returned.
     *
     * @param text: the text to be processed
     * @param caseSensitive: ensure case sensitive matching or not
     * @return the list of offset positions of the matches, an empty list if no match have been found
     */
    public List<OffsetPosition> matchToken(String text, boolean caseSensitive) {
        List<OffsetPosition> results = new ArrayList<OffsetPosition>();
        List<Integer> startPos = new ArrayList<Integer>();
        List<Integer> lastNonSeparatorPos = new ArrayList<Integer>();
        List<Map> t = new ArrayList<Map>();
        int currentPos = 0;
        StringTokenizer st = new StringTokenizer(text, delimiters, true);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.equals(" ") || token.equals("\n")) {
                continue;
            }
            if (delimiters.indexOf(token) != -1) {
                currentPos++;
                continue;
            }

            if (!caseSensitive)
                token = token.toLowerCase();

            // we try to complete opened matching
            int i = 0;
            List<Map> new_t = new ArrayList<Map>();
            List<Integer> new_startPos = new ArrayList<Integer>();
            List<Integer> new_lastNonSeparatorPos = new ArrayList<Integer>();
            // continuation of current opened matching
            for (Map tt : t) {
                Map t2 = (Map) tt.get(token);
                if (t2 != null) {
                    new_t.add(t2);
                    new_startPos.add(startPos.get(i));
                    new_lastNonSeparatorPos.add(currentPos);
                }
                //else
                {
                    t2 = (Map) tt.get("#");
                    if (t2 != null) {
                        // end of the current term, matching successful
                        OffsetPosition ofp = new OffsetPosition();
                        ofp.start = startPos.get(i).intValue();
                        ofp.end = lastNonSeparatorPos.get(i).intValue();
                        results.add(ofp);
                    }
                }
                i++;
            }

            // we start new matching starting at the current token
            Map t2 = (Map) terms.get(token);
            if (t2 != null) {
                new_t.add(t2);
                new_startPos.add(Integer.valueOf(currentPos));
                new_lastNonSeparatorPos.add(currentPos);
            }

            t = new_t;
            startPos = new_startPos;
            lastNonSeparatorPos = new_lastNonSeparatorPos;
            currentPos++;
        }

        // test if the end of the string correspond to the end of a term
        int i = 0;
        if (t != null) {
            for (Map tt : t) {
                Map t2 = (Map) tt.get("#");
                if (t2 != null) {
                    // end of the current term, matching successful
                    OffsetPosition ofp = new OffsetPosition();
                    ofp.start = startPos.get(i).intValue();
                    ofp.end = lastNonSeparatorPos.get(i).intValue();
                    results.add(ofp);
                }
                i++;
            }
        }

        return results;
    }

    /**
     * Identify terms in a piece of text and gives corresponding token positions.
     * All the matches are returned. Here the input text is already tokenized.
     *
     * @param tokens: the text to be processed
     * @return the list of offset positions of the matches, an empty list if no match have been found
     */
    /*public List<OffsetPosition> matcher(List<String> tokens) {
        StringBuilder text = new StringBuilder();
        for (String token : tokens) {
            text.append(processToken(token));
        }
        return matcher(text.toString());
    }*/

    /**
     * Identify terms in a piece of text and gives corresponding token positions.
     * All the matches are returned. Here the input is a list of LayoutToken object.
     *
     * @param tokens the text to be processed as a list of LayoutToken objects
     * @return the list of offset positions of the matches, an empty list if no match have been found
     */
    public List<OffsetPosition> matchLayoutToken(List<LayoutToken> tokens) {
        return matchLayoutToken(tokens, true, false);
    }

    /**
     * Identify terms in a piece of text and gives corresponding token positions.
     * All the matches are returned. Here the input is a list of LayoutToken object.
     *
     * @param tokens the text to be processed as a list of LayoutToken objects
     * @param ignoreDelimiters if true, ignore the delimiters in the matching process
     * @param caseSensitive: ensure case sensitive matching or not
     * @return the list of offset positions of the matches, an empty list if no match have been found
     */
    public List<OffsetPosition> matchLayoutToken(
            List<LayoutToken> tokens,
            boolean ignoreDelimiters,
            boolean caseSensitive) {
        if (CollectionUtils.isEmpty(tokens)) {
            return new ArrayList<OffsetPosition>();
        }

        List<OffsetPosition> results = new ArrayList<>();
        List<Integer> startPosition = new ArrayList<>();
        List<Integer> lastNonSeparatorPos = new ArrayList<>();
        List<Map> currentMatches = new ArrayList<>();
        int currentPos = 0;
        for (LayoutToken token : tokens) {
            if (token.getText().equals(" ") || token.getText().equals("\n")) {
                currentPos++;
                continue;
            }

            if (ignoreDelimiters && (delimiters.indexOf(token.getText()) != -1)) {
                currentPos++;
                continue;
            }

            String tokenText = UnicodeUtil.normaliseText(token.getText());
            if (!caseSensitive)
                tokenText = tokenText.toLowerCase();

            // we try to complete opened matching
            int i = 0;
            List<Map> matchesTreeList = new ArrayList<>();
            List<Integer> matchesPosition = new ArrayList<>();
            List<Integer> new_lastNonSeparatorPos = new ArrayList<>();

            // we check whether the current token matches as continuation of a previous match.
            for (Map currentMatch : currentMatches) {
                Map childMatches = (Map) currentMatch.get(tokenText);
                if (childMatches != null) {
                    matchesTreeList.add(childMatches);
                    matchesPosition.add(startPosition.get(i));
                    new_lastNonSeparatorPos.add(currentPos);
                }

                //check if the token itself is present, I add the match in the list of results
                childMatches = (Map) currentMatch.get("#");
                if (childMatches != null) {
                    // end of the current term, matching successful
                    OffsetPosition ofp = new OffsetPosition(startPosition.get(i), lastNonSeparatorPos.get(i));
                    results.add(ofp);
                }

                i++;
            }

            // we start new matching starting at the current token
            Map match = (Map) terms.get(tokenText);
            if (match != null) {
                matchesTreeList.add(match);
                matchesPosition.add(currentPos);
                new_lastNonSeparatorPos.add(currentPos);
            }

            currentMatches = matchesTreeList;
            startPosition = matchesPosition;
            lastNonSeparatorPos = new_lastNonSeparatorPos;
            currentPos++;
        }

        // test if the end of the string correspond to the end of a term
        int i = 0;
        if (currentMatches != null) {
            for (Map tt : currentMatches) {
                Map t2 = (Map) tt.get("#");
                if (t2 != null) {
                    // end of the current term, matching successful
                    OffsetPosition ofp = new OffsetPosition(startPosition.get(i), lastNonSeparatorPos.get(i));
                    results.add(ofp);
                }
                i++;
            }
        }

        return results;
    }

    /**
     *
     * Gives the character positions within a text where matches occur.
     * <p>
     * By iterating over the OffsetPosition and applying substring, we get all the matches.
     * <p>
     * All the matches are returned.
     *
     * @param text: the text to be processed
     * @return the list of offset positions of the matches referred to the input string, an empty
     * list if no match have been found
     */
    public List<OffsetPosition> matchCharacter(String text) {
        return matchCharacter(text, false);
    }

    /**
     *
     * Gives the character positions within a text where matches occur.
     * <p>
     * By iterating over the OffsetPosition and applying substring, we get all the matches.
     * <p>
     * All the matches are returned.
     *
     * @param text: the text to be processed
     * @param caseSensitive: ensure case sensitive matching or not
     * @return the list of offset positions of the matches referred to the input string, an empty
     * list if no match have been found
     */
    public List<OffsetPosition> matchCharacter(String text, boolean caseSensitive) {
        List<OffsetPosition> results = new ArrayList<>();
        List<Integer> startPosition = new ArrayList<>();
        List<Integer> lastNonSeparatorPos = new ArrayList<>();
        List<Map> currentMatches = new ArrayList<>();
        int currentPos = 0;
        StringTokenizer st = new StringTokenizer(text, delimiters, true);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.equals(" ")) {
                currentPos++;
                continue;
            }
            if (delimiters.indexOf(token) != -1) {
                currentPos++;
                continue;
            }
            if (!caseSensitive)
                token = token.toLowerCase();

            // we try to complete opened matching
            int i = 0;
            List<Map> matchesTreeList = new ArrayList<>();
            List<Integer> matchesPosition = new ArrayList<>();
            List<Integer> new_lastNonSeparatorPos = new ArrayList<>();

            // we check whether the current token matches as continuation of a previous match.
            for (Map currentMatch : currentMatches) {
                Map childMatches = (Map) currentMatch.get(token);
                if (childMatches != null) {
                    matchesTreeList.add(childMatches);
                    matchesPosition.add(startPosition.get(i));
                    new_lastNonSeparatorPos.add(currentPos + token.length());
                }

                //check if the token itself is present, I add the match in the list of results
                childMatches = (Map) currentMatch.get("#");
                if (childMatches != null) {
                    // end of the current term, matching successful
                    OffsetPosition ofp = new OffsetPosition(startPosition.get(i), lastNonSeparatorPos.get(i));
                    results.add(ofp);
                }

                i++;
            }

            //TODO: e.g. The Bronx matches 'The Bronx' and 'Bronx' is this correct?

            // we start new matching starting at the current token
            Map match = (Map) terms.get(token);
            if (match != null) {
                matchesTreeList.add(match);
                matchesPosition.add(currentPos);
                new_lastNonSeparatorPos.add(currentPos + token.length());
            }

            currentMatches = matchesTreeList;
            startPosition = matchesPosition;
            lastNonSeparatorPos = new_lastNonSeparatorPos;
            currentPos += token.length();
        }

        // test if the end of the string correspond to the end of a term
        int i = 0;
        if (currentMatches != null) {
            for (Map tt : currentMatches) {
                Map t2 = (Map) tt.get("#");
                if (t2 != null) {
                    // end of the current term, matching successful
                    OffsetPosition ofp = new OffsetPosition(startPosition.get(i), lastNonSeparatorPos.get(i));
                    results.add(ofp);
                }
                i++;
            }
        }

        return results;
    }

    /**
     *
     * Gives the character positions within a tokenized text where matches occur.
     * <p>
     * All the matches are returned.
     *
     * @param tokens the text to be processed as a list of LayoutToken objects
     * @return the list of offset positions of the matches referred to the input string, an empty
     * list if no match have been found
     */
    public List<OffsetPosition> matchCharacterLayoutToken(List<LayoutToken> tokens) {
        return matchCharacterLayoutToken(tokens, false);
    }

    /**
     *
     * Gives the character positions within a tokenized text where matches occur.
     * <p>
     * All the matches are returned.
     *
     * @param tokens the text to be processed as a list of LayoutToken objects
     * @param caseSensitive ensure case sensitive matching or not
     * @return the list of offset positions of the matches referred to the input string, an empty
     * list if no match have been found
     */
    public List<OffsetPosition> matchCharacterLayoutToken(List<LayoutToken> tokens, boolean caseSensitive) {
        List<OffsetPosition> results = new ArrayList<>();
        List<Integer> startPosition = new ArrayList<>();
        List<Integer> lastNonSeparatorPos = new ArrayList<>();
        List<Map> currentMatches = new ArrayList<>();

        int currentPos = 0;

        for (LayoutToken token : tokens) {
            if (token.getText().equals(" ")) {
                currentPos++;
                continue;
            }
            if (delimiters.indexOf(token.getText()) != -1) {
                currentPos++;
                continue;
            }
            String tokenString = token.getText();
            if (!caseSensitive)
                tokenString = tokenString.toLowerCase();

            // we try to complete opened matching
            int i = 0;
            List<Map> matchesTreeList = new ArrayList<>();
            List<Integer> matchesPosition = new ArrayList<>();
            List<Integer> new_lastNonSeparatorPos = new ArrayList<>();

            // we check whether the current token matches as continuation of a previous match.
            for (Map currentMatch : currentMatches) {
                Map childMatches = (Map) currentMatch.get(tokenString);
                if (childMatches != null) {
                    matchesTreeList.add(childMatches);
                    matchesPosition.add(startPosition.get(i));
                    new_lastNonSeparatorPos.add(currentPos);
                }

                //check if the token itself is present, I add the match in the list of results
                childMatches = (Map) currentMatch.get("#");
                if (childMatches != null) {
                    // end of the current term, matching successful
                    OffsetPosition ofp = new OffsetPosition(startPosition.get(i), lastNonSeparatorPos.get(i));
                    results.add(ofp);
                }

                i++;
            }

            // we start new matching starting at the current token
            Map match = (Map) terms.get(tokenString);
            if (match != null) {
                matchesTreeList.add(match);
                matchesPosition.add(currentPos);
                new_lastNonSeparatorPos.add(currentPos);
            }

            currentMatches = matchesTreeList;
            startPosition = matchesPosition;
            lastNonSeparatorPos = new_lastNonSeparatorPos;
            currentPos++;
        }

        // test if the end of the string correspond to the end of a term
        int i = 0;
        if (currentMatches != null) {
            for (Map tt : currentMatches) {
                Map t2 = (Map) tt.get("#");
                if (t2 != null) {
                    // end of the current term, matching successful
                    OffsetPosition ofp = new OffsetPosition(startPosition.get(i), lastNonSeparatorPos.get(i));
                    results.add(ofp);
                }
                i++;
            }
        }

        return results;
    }

    /**
     * Identify terms in a piece of text and gives corresponding token positions.
     * All the matches are returned. This case correspond to text from a trainer,
     * where the text is already tokenized with some labeled that can be ignored.
     *
     * @param tokens: the text to be processed
     * @return the list of offset positions of the matches, an empty list if no match have been found
     */
    public List<OffsetPosition> matcherPairs(List<Pair<String, String>> tokens) {
        return matcherPairs(tokens, false);
    }

    /**
     * Identify terms in a piece of text and gives corresponding token positions.
     * All the matches are returned. This case correspond to text from a trainer,
     * where the text is already tokenized with some labeled that can be ignored.
     *
     * @param tokens: the text to be processed
     * @param caseSensitive: ensure case sensitive matching or not
     * @return the list of offset positions of the matches, an empty list if no match have been found
     */
    public List<OffsetPosition> matcherPairs(List<Pair<String, String>> tokens, boolean caseSensitive) {
        StringBuilder text = new StringBuilder();
        for (Pair<String, String> tokenP : tokens) {
            String token = tokenP.getA();
            text.append(processToken(token));
        }
        return matchToken(text.toString(), caseSensitive);
    }

    /**
     * Process token, if different than @newline
     */
    protected String processToken(String token) {
        if (!token.trim().equals("@newline")) {
            int ind = token.indexOf(" ");
            if (ind == -1)
                ind = token.indexOf("\t");
            if (ind == -1)
                return " " + token;
            else
                return " " + token.substring(0, ind);
        }
        return "";
    }
}
