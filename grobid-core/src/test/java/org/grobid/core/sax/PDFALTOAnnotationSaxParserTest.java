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
package org.grobid.core.sax;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Before;
import org.junit.Test;

import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.layout.PDFAnnotation;

public class PDFALTOAnnotationSaxParserTest {
    SAXParserFactory spf = SAXParserFactory.newInstance();

    PDFALTOAnnotationSaxHandler target;
    DocumentSource mockDocumentSource;
    Document document;

    @Before
    public void setUp() throws Exception {

        mockDocumentSource = createMock(DocumentSource.class);

        document = Document.createFromText("");
        target = new PDFALTOAnnotationSaxHandler(document, new ArrayList<PDFAnnotation>());
    }

    @Test
    public void testParsing_pdf2XMLAnnotations_ShouldWork() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("pdfalto.xml_annot.xml");

        SAXParser p = spf.newSAXParser();
        p.parse(is, target);

        List<PDFAnnotation> pdfAnnotations = target.getPDFAnnotations();
        //		System.out.println(pdfAnnotations.size());
        assertTrue(pdfAnnotations.size() > 0);
        assertThat(pdfAnnotations, hasSize(520));

    }

}
