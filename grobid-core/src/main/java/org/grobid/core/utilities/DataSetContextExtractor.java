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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import org.apache.commons.io.IOUtils;

import org.grobid.core.data.DataSetContext;

/**
 * Extracting citation callout
 */
public class DataSetContextExtractor {
    public static final Pattern REF_PATTERN = Pattern.compile("<ref>(.*)</ref>", Pattern.DOTALL);
    public static final int CUT_DEFAULT_LENGTH = 50;

    static {
        InputStream is = DataSetContextExtractor.class.getResourceAsStream("/xq/get-citation-context-from-tei.xq");
        try {
            CONTEXT_EXTRACTION_BIB_XQ = IOUtils.toString(is, UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        IOUtils.closeQuietly(is);

        is = DataSetContextExtractor.class.getResourceAsStream("/xq/get-formula-context-from-tei.xq");
        try {
            CONTEXT_EXTRACTION_FORMULA_XQ = IOUtils.toString(is, UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        IOUtils.closeQuietly(is);

        is = DataSetContextExtractor.class.getResourceAsStream("/xq/get-figure-context-from-tei.xq");
        try {
            CONTEXT_EXTRACTION_FIGURE_XQ = IOUtils.toString(is, UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        IOUtils.closeQuietly(is);

        is = DataSetContextExtractor.class.getResourceAsStream("/xq/get-table-context-from-tei.xq");
        try {
            CONTEXT_EXTRACTION_TABLE_XQ = IOUtils.toString(is, UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        IOUtils.closeQuietly(is);
    }

    private static final String CONTEXT_EXTRACTION_BIB_XQ;
    private static final String CONTEXT_EXTRACTION_FORMULA_XQ;
    private static final String CONTEXT_EXTRACTION_FIGURE_XQ;
    private static final String CONTEXT_EXTRACTION_TABLE_XQ;

    static <K extends Comparable<? super K>, V> ListMultimap<K, V> multimap() {
        return MultimapBuilder.treeKeys().linkedListValues().build();
    }

    protected static String cutContextSimple(String cont) {
        Matcher m = REF_PATTERN.matcher(cont);
        if (m.find()) {
            String g = m.group(1);
            int index = m.start();

            return cont.substring(
                    Math.max(0, index - CUT_DEFAULT_LENGTH),
                    Math.min(
                            cont.length(),
                            index + g.length() + CUT_DEFAULT_LENGTH));
        } else {
            throw new IllegalStateException("Implementation error: no <ref> found in" + cont);
        }
    }

    public static Multimap<String, DataSetContext> getCitationReferences(String tei)
            throws XPathException, IOException {
        XQueryProcessor xQueryProcessor = new XQueryProcessor(tei);

        SequenceIterator it = xQueryProcessor.getSequenceIterator(CONTEXT_EXTRACTION_BIB_XQ);

        Item item;
        Multimap<String, DataSetContext> contexts = multimap();

        while ((item = it.next()) != null) {
            String val = item.getStringValue();
            String citationTeiId = it.next().getStringValue();
            String sectionName = it.next().getStringValue();
            double pos = Double.parseDouble(it.next().getStringValue());
            String coords = it.next().getStringValue();

            DataSetContext pcc = new DataSetContext();
            String context = cutContextSimple(val);

            pcc.setContext(extractContextSentence(context));
            pcc.setDocumentCoords(coords);
            pcc.setTeiId(citationTeiId);

            contexts.put(citationTeiId, pcc);
        }
        return contexts;
    }

    private static String extractContextSentence(String cont) {
        Matcher m = REF_PATTERN.matcher(cont);
        if (m.find()) {
            String g = m.group(1);
            return m.replaceAll(Matcher.quoteReplacement(g));
        } else {
            throw new IllegalStateException("Implementation error: no <ref> found in" + cont);
        }
    }

    public static Multimap<String, DataSetContext> getFormulaReferences(String tei) throws XPathException, IOException {
        XQueryProcessor xQueryProcessor = new XQueryProcessor(tei);

        SequenceIterator it = xQueryProcessor.getSequenceIterator(CONTEXT_EXTRACTION_FORMULA_XQ);

        Item item;
        Multimap<String, DataSetContext> contexts = multimap();

        while ((item = it.next()) != null) {
            String val = item.getStringValue();
            String formulaTeiId = it.next().getStringValue();
            String sectionName = it.next().getStringValue();
            double pos = Double.parseDouble(it.next().getStringValue());
            String coords = it.next().getStringValue();

            DataSetContext pcc = new DataSetContext();
            String context = cutContextSimple(val);

            pcc.setContext(extractContextSentence(context));
            pcc.setDocumentCoords(coords);
            pcc.setTeiId(formulaTeiId);

            contexts.put(formulaTeiId, pcc);
        }
        return contexts;
    }

    public static Multimap<String, DataSetContext> getFigureReferences(String tei) throws XPathException, IOException {
        XQueryProcessor xQueryProcessor = new XQueryProcessor(tei);

        SequenceIterator it = xQueryProcessor.getSequenceIterator(CONTEXT_EXTRACTION_FIGURE_XQ);

        Item item;
        Multimap<String, DataSetContext> contexts = multimap();

        while ((item = it.next()) != null) {
            String val = item.getStringValue();
            String figureTeiId = it.next().getStringValue();
            String sectionName = it.next().getStringValue();
            double pos = Double.parseDouble(it.next().getStringValue());
            String coords = it.next().getStringValue();

            DataSetContext pcc = new DataSetContext();
            String context = cutContextSimple(val);

            pcc.setContext(extractContextSentence(context));
            pcc.setDocumentCoords(coords);
            pcc.setTeiId(figureTeiId);

            contexts.put(figureTeiId, pcc);
        }
        return contexts;
    }

    public static Multimap<String, DataSetContext> getTableReferences(String tei) throws XPathException, IOException {
        XQueryProcessor xQueryProcessor = new XQueryProcessor(tei);

        SequenceIterator it = xQueryProcessor.getSequenceIterator(CONTEXT_EXTRACTION_TABLE_XQ);

        Item item;
        Multimap<String, DataSetContext> contexts = multimap();

        while ((item = it.next()) != null) {
            String val = item.getStringValue();
            String tableTeiId = it.next().getStringValue();
            String sectionName = it.next().getStringValue();
            double pos = Double.parseDouble(it.next().getStringValue());
            String coords = it.next().getStringValue();

            DataSetContext pcc = new DataSetContext();
            String context = cutContextSimple(val);

            pcc.setContext(extractContextSentence(context));
            pcc.setDocumentCoords(coords);
            pcc.setTeiId(tableTeiId);

            contexts.put(tableTeiId, pcc);
        }
        return contexts;
    }

}
