/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grobid.core.analyzers;

import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.core.lang.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Default tokenizer adequate for all Indo-European languages.
 *
 */
public class GrobidDefaultAnalyzer implements Analyzer {

    private static volatile GrobidDefaultAnalyzer instance;

    public static GrobidDefaultAnalyzer getInstance() {
        if (instance == null) {
            //double check idiom
            // synchronized (instanceController) {
            if (instance == null)
                getNewInstance();
            // }
        }
        return instance;
    }

    /**
     * Creates a new instance.
     */
    private static synchronized void getNewInstance() {
        instance = new GrobidDefaultAnalyzer();
    }

    /**
     * Hidden constructor
     */
    private GrobidDefaultAnalyzer() {
    }

    public static final String delimiters = TextUtilities.delimiters;

    // the following regex is used to separate alphabetical and numerical character subsequences 
    // note: see about using \p{N} for unicode digits
    private static final String REGEX = "(?<=[\\p{L}])(?=\\d)|(?<=\\d)(?=\\D)";

    public String getName() {
        return "DefaultGrobidAnalyzer";
    }

    public List<String> tokenize(String text) {
        // as a default analyzer, language is not considered
        return tokenize(text, null);
    }

    public List<String> tokenize(String text, Language lang) {
        List<String> result = new ArrayList<>();
        text = UnicodeUtil.normaliseText(text);
        StringTokenizer st = new StringTokenizer(text, delimiters, true);
        while (st.hasMoreTokens()) {
            result.add(st.nextToken());
        }
        return result;
    }

    public List<String> retokenize(List<String> chunks) {
        StringTokenizer st = null;
        List<String> result = new ArrayList<>();
        for (String chunk : chunks) {
            chunk = UnicodeUtil.normaliseText(chunk);
            st = new StringTokenizer(chunk, delimiters, true);
            while (st.hasMoreTokens()) {
                result.add(st.nextToken());
            }
        }
        return result;
    }

    public List<LayoutToken> tokenizeWithLayoutToken(String text) {
        return tokenizeWithLayoutToken(text, null);
    }

    /**
     * Tokenize text returning list of LayoutTokens.
     */
    public List<LayoutToken> tokenizeWithLayoutToken(String text, Language language) {
        List<LayoutToken> result = new ArrayList<>();
        text = UnicodeUtil.normaliseText(text);
        List<String> tokens = tokenize(text, language);
        int pos = 0;
        for (int i = 0; i < tokens.size(); i++) {
            String tok = tokens.get(i);
            LayoutToken layoutToken = new LayoutToken();
            layoutToken.setText(tok);
            layoutToken.setOffset(pos);
            result.add(layoutToken);
            pos += tok.length();
            if (i < tokens.size() - 1 && tokens.get(i + 1).equals("\n")) {
                layoutToken.setNewLineAfter(true);
            }
        }

        return result;
    }

    /**
     * To tokenize an existing list of tokens. Only useful if input tokens have
     * been tokenized with a non-default Grobid tokenizer.  
     * Note: the coordinates of the subtokens are not recomputed here (at least for 
     * the moment). 
     * <p>
     * 1/74 -> "1", "/", "74"
     *
     */
    public List<LayoutToken> retokenizeFromLayoutToken(List<LayoutToken> tokens) {
        List<LayoutToken> result = new ArrayList<>();
        for(LayoutToken token : tokens) {
            if (token.getText() == null || token.getText().trim().length() == 0) {
                result.add(token);
            } else {
                String tokenText = token.getText();
                List<String> subtokens = tokenize(tokenText);
                int offset = token.getOffset();
                for (int i = 0; i < subtokens.size(); i++) {
                    LayoutToken layoutToken = new LayoutToken();
                    layoutToken.setText(subtokens.get(i));
                    layoutToken.setOffset(offset);

                    // coordinates - TODO: refine the width/X for the sub token
                    layoutToken.setX(token.getX());
                    layoutToken.setY(token.getY());
                    layoutToken.setHeight(token.getHeight());
                    layoutToken.setWidth(token.getWidth());
                    layoutToken.setPage(token.getPage());

                    offset += subtokens.get(i).length();
                    result.add(layoutToken);
                }
            }
        }
        return result;
    }

    /**
     * To tokenize mixture of alphabetical and numerical characters by separating 
     * separate alphabetical and numerical character subsequences. To be used
     * when relevant. 
     * <p>
     * 1m74 -> "1", "m", "74"
     *
     */
    public List<String> retokenizeSubdigits(List<String> chunks) {
        List<String> result = new ArrayList<>();
        for(String token : chunks) {
            // we split "letter" characters and digits
            String[] subtokens = token.split(REGEX);
            for (int i = 0; i < subtokens.length; i++) {
                result.add(subtokens[i]);
            }
        }
        return result;
    }

    /**
     * To tokenize mixture of alphabetical and numerical characters by separating separate alphabetical and numerical
     * character subsequences. To be used when relevant.
     *
     * <pre>
     * 1m74 ->  tokens.add(new LayoutToken("1"));
     * tokens.add(new LayoutToken("m"));
     * tokens.add(new LayoutToken("74"));
     * </pre>
     */
    public List<LayoutToken> retokenizeSubdigitsWithLayoutToken(List<String> chunks) {
        List<LayoutToken> result = new ArrayList<>();
        int offset = 0;
        for(String token : chunks) {
            // we split "letter" characters and digits
            String[] subtokens = token.split(REGEX);
            for (int i = 0; i < subtokens.length; i++) {
                LayoutToken layoutToken = new LayoutToken();
                layoutToken.setText(subtokens[i]);
                layoutToken.setOffset(offset);
                offset += subtokens[i].length();
                result.add(layoutToken);
            }
        }
        return result;
    }

    /**
     * To tokenize mixture of alphabetical and numerical characters by separating separate alphabetical and numerical
     * character subsequences. To be used when relevant. Input is a list of LayoutToken, but the coordinates of the
     * subtokens are however not recomputed here (at least for the moment).
     *
     * <pre>
     * 1m74 ->  tokens.add(new LayoutToken("1"));
     * tokens.add(new LayoutToken("m"));
     * tokens.add(new LayoutToken("74"));
     * </pre>
     */
    public List<LayoutToken> retokenizeSubdigitsFromLayoutToken(List<LayoutToken> tokens) {
        List<LayoutToken> result = new ArrayList<>();
        for(LayoutToken token : tokens) {
            // we split "letter" characters and digits
            if (token.getText() == null || token.getText().trim().length() == 0) {
                result.add(token);
            } else {
                String tokenText = token.getText();
                String[] subtokens = tokenText.split(REGEX);
                int offset = token.getOffset();
                for (int i = 0; i < subtokens.length; i++) {
                    LayoutToken layoutToken = new LayoutToken();
                    layoutToken.setText(subtokens[i]);
                    layoutToken.setOffset(offset);

                    // coordinates - TODO: refine the width/X for the sub token
                    layoutToken.setX(token.getX());
                    layoutToken.setY(token.getY());
                    layoutToken.setHeight(token.getHeight());
                    layoutToken.setWidth(token.getWidth());
                    layoutToken.setPage(token.getPage());

                    offset += subtokens[i].length();
                    result.add(layoutToken);
                }
            }
        }
        return result;
    }
}