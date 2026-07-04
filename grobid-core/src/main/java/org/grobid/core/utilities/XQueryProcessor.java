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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Properties;

import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.trans.XPathException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.xml.sax.InputSource;

/**
 * Running XQuery queries
 */
public class XQueryProcessor {
    private final StaticQueryContext sqc;
    private final DynamicQueryContext dqc;

    public static String getQueryFromResources(String name) throws IOException {
        return IOUtils.toString(XQueryProcessor.class.getResourceAsStream("/xq/" + name), UTF_8);
    }

    public XQueryProcessor(File xmFile) throws XPathException, IOException {
        this(FileUtils.readFileToString(xmFile, UTF_8));
    }

    public XQueryProcessor(String xmlContent) throws XPathException {
        Configuration c = new Configuration();

        sqc = new StaticQueryContext(c);
        dqc = new DynamicQueryContext(c);

        InputStream is = new ByteArrayInputStream(xmlContent.getBytes(UTF_8));
        InputSource XMLSource = new InputSource(is);

        SAXSource SAXs = new SAXSource(XMLSource);
        DocumentInfo DI = sqc.buildDocument(SAXs);
        dqc.setContextItem(DI);
    }

    public SequenceIterator getSequenceIterator(String query) throws XPathException {
        Properties props = new Properties();

        final XQueryExpression exp = sqc.compileQuery(query);
        StringWriter stringWriter = new StringWriter();
        exp.run(dqc, new StreamResult(stringWriter), props);
        return exp.iterator(dqc);
    }

}
