package org.grobid.core.utilities.matching;

import com.google.common.base.Joiner;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.Version;
import org.grobid.core.utilities.Pair;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class LuceneUtil {

    private LuceneUtil() {
    }

    /**
     * @return a StandardAnalyzer without stop-words
     */
    public static StandardAnalyzer createStandardAnalyzer() {
        return new StandardAnalyzer(Version.LUCENE_45);
    }

    public static String normalizeString(final Analyzer analyzer, final String in) {
        final List<String> tokens = tokenizeString(analyzer, in);
        return Joiner.on(' ').join(tokens);
    }

    public static String normalizeTokens(final Analyzer analyzer, final List<String> tokens) {
        return Joiner.on(' ').join(tokens);
    }

    /**
     * Convert a Reader to a List of Tokens.
     *
     * @param analyzer
     * the Analyzer to use
     * @param reader
     * the reader to feed to the Analyzer
     * @return a List of tokens
     * @throws java.io.IOException
     * lucene exceptions
     */
    private static List<String> readerToTokens(final Analyzer analyzer, final Reader reader) throws IOException {

        final List<String> coll = new ArrayList<String>();
        final TokenStream ts = analyzer.tokenStream("", reader);

        ts.reset();
        final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
        while (ts.incrementToken()) {
            final String val = String.valueOf(termAtt.buffer(), 0, termAtt.length());
            coll.add(val);
        }
        ts.end();
        ts.close();
        return coll;
    }

    public static List<String> tokenizeString(final Analyzer analyzer, final String in) {
        final Reader r = new StringReader(in);

        try {
            return readerToTokens(analyzer, r);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Pair<String, String>> tokenizeWithTokenTypes(final Analyzer analyzer, final String in) {
        final Reader r = new StringReader(in);
        final List<Pair<String, String>> coll = new ArrayList<Pair<String, String>>();
        try {

            final TokenStream ts = analyzer.tokenStream("", r);
            ts.reset();
            final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            final TypeAttribute typeAtt = ts.addAttribute(TypeAttribute.class);
            while (ts.incrementToken()) {
                final String val = String.valueOf(termAtt.buffer(), 0, termAtt.length());
                final String type = typeAtt.type();
                coll.add(new Pair<String, String>(val, type));
            }
            ts.end();
            ts.close();
        } catch (IOException e) {
            throw new RuntimeException("Error during tokenization", e);
        }
        return coll;
    }
}
