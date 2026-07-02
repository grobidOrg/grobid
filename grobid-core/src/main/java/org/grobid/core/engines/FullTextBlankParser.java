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
package org.grobid.core.engines;

import java.io.*;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.GrobidModels;
import org.grobid.core.document.*;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.*;

public class FullTextBlankParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(FullTextBlankParser.class);

    protected File tmpPath = null;

    // default bins for relative position
    private static final int NBBINS_POSITION = 12;

    // default bins for inter-block spacing
    private static final int NBBINS_SPACE = 5;

    // default bins for block character density
    private static final int NBBINS_DENSITY = 5;

    // projection scale for line length
    private static final int LINESCALE = 10;

    protected EngineParsers parsers;

    public FullTextBlankParser(EngineParsers parsers) {
        super(GrobidModels.FULLTEXT);
        tmpPath = GrobidProperties.getTempPath();
    }

    public Document process(
            File inputPdf,
            GrobidAnalysisConfig config) throws Exception {
        DocumentSource documentSource = DocumentSource.fromPdf(
                inputPdf,
                config.getStartPage(),
                config.getEndPage(),
                config.getPdfAssetPath() != null,
                true,
                false);
        return process(documentSource, config);
    }

    public Document process(
            File inputPdf,
            String md5Str,
            GrobidAnalysisConfig config) throws Exception {
        DocumentSource documentSource = DocumentSource.fromPdf(
                inputPdf,
                config.getStartPage(),
                config.getEndPage(),
                config.getPdfAssetPath() != null,
                true,
                false);
        documentSource.setMD5(md5Str);
        return process(documentSource, config);
    }

    /**
     * Machine-learning recognition of the complete full text structures.
     *
     * @param documentSource input
     * @param config config
     * @return the document object with built TEI
     */
    public Document process(
            DocumentSource documentSource,
            GrobidAnalysisConfig config) {
        if (tmpPath == null) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path is null.");
        }
        if (!tmpPath.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path '"
                    +
                    tmpPath.getAbsolutePath()
                    + "' does not exists.");
        }
        try {
            Document doc = new Document(documentSource);
            doc.addTokenizedDocument(config);

            if (doc.getBlocks() == null) {
                throw new Exception("PDF parsing resulted in empty content");
            }
            doc.produceStatistics();

            List<LayoutToken> tokenizations = doc.getTokenizations();

            // also write the raw text as seen before segmentation
            StringBuffer rawtxt = new StringBuffer();
            for (LayoutToken txtline : tokenizations) {
                rawtxt.append(TextUtilities.HTMLEncode(txtline.getText()));
            }

            String fulltext = rawtxt.toString();
            TEIFormatter formatter = new TEIFormatter(doc, null);
            StringBuilder tei = formatter.toTEIHeader(
                    null,
                    null,
                    null,
                    null,
                    null,
                    config);

            tei.append("\t\t<body>\n");
            tei.append("\t\t\t<div>\n");
            tei.append("\t\t\t\t<p>\n");
            tei.append(fulltext);
            tei.append("\t\t\t\t</p>\n");
            tei.append("\t\t\t</div>\n");
            tei.append("\t\t</body>\n");
            tei.append("\t</text>\n");
            tei.append("</TEI>\n");

            doc.setTei(tei.toString());
            return doc;
        } catch (GrobidException e) {
            throw e;
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
