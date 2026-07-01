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
package org.grobid.core.analyzers;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.nlp.textboundaries.ReTokenizer;
import org.grobid.nlp.textboundaries.ReTokenizerFactory;

/**
 * An Analyzer able to dispatch text to be tokenized to the adequate analyzer given a specified language.
 *
 * The language might be preliminary set by the language recognizer or manually if it is already
 * known by the context of usage of the text.
 *
 */
public class GrobidAnalyzer implements Analyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidAnalyzer.class);

    private static volatile GrobidAnalyzer instance;

    private ReTokenizer jaAnalyzer = null;
    private ReTokenizer krAnalyzer = null;
    private ReTokenizer zhAnalyzer = null;

    public static GrobidAnalyzer getInstance() {
        if (instance == null) {
            //double check idiom
            synchronized (GrobidAnalyzer.class) {
                if (instance == null) {
                    getNewInstance();
                }
            }
        }
        return instance;
    }

    /**
     * Creates a new instance.
     */
    private static synchronized void getNewInstance() {
        LOGGER.debug("Get new instance of GrobidAnalyzer");
        instance = new GrobidAnalyzer();
    }

    /**
     * Hidden constructor
     */
    private GrobidAnalyzer() {
        try {
            krAnalyzer = ReTokenizerFactory.create("ko_g");
        } catch (Exception e) {
            LOGGER.error("Invalid kr tokenizer", e);
        }
    }

    public String getName() {
        return "GrobidAnalyzer";
    }

    /**
     * Tokenizer entry point
     */
    public List<String> tokenize(String text) {
        return tokenize(text, null);
    }

    public List<String> tokenize(String text, Language lang) {
        List<String> result = new ArrayList<String>();
        if ((text == null) || (text.length() == 0)) {
            return result;
        }
        try {
            //if (lang != null)
            //System.out.println("---> tokenize: " + text + " // " + lang.getLang());

            if ((lang == null) || (lang.getLang() == null)) {
                // default Indo-European languages
                result = GrobidDefaultAnalyzer.getInstance().tokenize(text);
            } else if (lang.isJapaneses()) {
                // Japanese analyser
                if (jaAnalyzer == null)
                    jaAnalyzer = ReTokenizerFactory.create("ja_g");
                result = jaAnalyzer.tokensAsList(text);
            } else if (lang.isChinese()) {
                // Chinese analyser
                if (zhAnalyzer == null)
                    zhAnalyzer = ReTokenizerFactory.create("zh_g");
                result = zhAnalyzer.tokensAsList(text);
            } else if (lang.isKorean()) {
                // Korean analyser
                /*if (krAnalyzer == null)
                	krAnalyzer = ReTokenizerFactory.create("ko_g");*/
                result = krAnalyzer.tokensAsList(text);
            } else if (lang.isArabic()) {
                // Arabic analyser
                result = GrobidDefaultAnalyzer.getInstance().tokenize(text);
                int p = 0;
                for (String token : result) {
                    // string being immutable in Java, I think we can't do better that this:
                    StringBuilder newToken = new StringBuilder();
                    for (int i = 0; i < token.length(); i++) {
                        newToken.append(ArabicChars.arabicCharacters(token.charAt(i)));
                    }
                    result.set(p, newToken.toString());
                    p++;
                }
            } else {
                // default Indo-European languages
                result = GrobidDefaultAnalyzer.getInstance().tokenize(text);
            }
        } catch (Exception e) {
            LOGGER.error("Invalid tokenizer", e);
        }
        return result;
    }

    /**
     * Re-tokenizer entry point to be applied to text already tokenized in the PDF representation
     */
    public List<String> retokenize(List<String> textTokenized) {
        return retokenize(textTokenized, null);
    }

    public List<String> retokenize(List<String> textTokenized, Language lang) {
        List<String> result = null;
        if ((textTokenized == null) || (textTokenized.size() == 0)) {
            return new ArrayList<String>();
        }
        try {
            if ((lang == null) || (lang.getLang() == null)) {
                // default Indo-European languages
                result = GrobidDefaultAnalyzer.getInstance().retokenize(textTokenized);
            } else if (lang.isJapaneses()) {
                // Japanese analyser
                if (jaAnalyzer == null)
                    jaAnalyzer = ReTokenizerFactory.create("ja_g");
                for (String chunk : textTokenized) {
                    List<String> localResult = jaAnalyzer.tokensAsList(chunk);
                    result.addAll(localResult);
                }
            } else if (lang.isChinese()) {
                // Chinese analyser
                if (zhAnalyzer == null)
                    zhAnalyzer = ReTokenizerFactory.create("zh_g");
                for (String chunk : textTokenized) {
                    List<String> localResult = zhAnalyzer.tokensAsList(chunk);
                    result.addAll(localResult);
                }
            } else if (lang.isKorean()) {
                // Korean analyser
                /*if (krAnalyzer == null)
                	krAnalyzer = ReTokenizerFactory.create("ko_g");*/
                for (String chunk : textTokenized) {
                    List<String> localResult = krAnalyzer.tokensAsList(chunk);
                    result.addAll(localResult);
                }
            } else if (lang.isArabic()) {
                // Arabic analyser
                for (String token : textTokenized) {
                    StringBuilder newToken = new StringBuilder();
                    for (int i = 0; i < token.length(); i++) {
                        newToken.append(ArabicChars.arabicCharacters(token.charAt(i)));
                    }
                    result.add(newToken.toString());
                }
            } else {
                // default Indo-European languages
                result = GrobidDefaultAnalyzer.getInstance().retokenize(textTokenized);
            }
        } catch (Exception e) {
            LOGGER.error("Invalid tokenizer", e);
        }
        return result;
    }

    public List<LayoutToken> tokenizeWithLayoutToken(String text) {
        return tokenizeWithLayoutToken(text, null);
    }

    public List<LayoutToken> retokenizeFromLayoutToken(List<LayoutToken> tokens) {
        return GrobidDefaultAnalyzer.getInstance().retokenizeFromLayoutToken(tokens);
    }

    public List<LayoutToken> tokenizeWithLayoutToken(String text, Language lang) {
        text = UnicodeUtil.normaliseText(text);
        List<String> tokens = tokenize(text, lang);
        return LayoutTokensUtil.getLayoutTokensForTokenizedText(tokens);
    }

    public List<String> retokenizeSubdigits(List<String> chunks) {
        return GrobidDefaultAnalyzer.getInstance().retokenizeSubdigits(chunks);
    }

    public List<LayoutToken> retokenizeSubdigitsWithLayoutToken(List<String> chunks) {
        return GrobidDefaultAnalyzer.getInstance().retokenizeSubdigitsWithLayoutToken(chunks);
    }

    public List<LayoutToken> retokenizeSubdigitsFromLayoutToken(List<LayoutToken> tokens) {
        return GrobidDefaultAnalyzer.getInstance().retokenizeSubdigitsFromLayoutToken(tokens);
    }
}
