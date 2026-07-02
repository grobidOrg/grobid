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
package org.grobid.core.document.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.Text;

public class XmlBuilderUtils {
    public static final String TEI_NS = "http://www.tei-c.org/ns/1.0";
    public static final Function<Element, String> TO_XML_FUNCTION = new Function<Element, String>() {
        @Override
        public String apply(Element element) {
            return toXml(element);
        }
    };
    public static final String XML_NS = "http://www.w3.org/XML/1998/namespace";

    public static Element fromString(String xml) {
        Builder parser = new Builder();
        Document doc;
        try {
            doc = parser.build(new StringReader(xml));
        } catch (ParsingException | IOException e) {
            throw new RuntimeException(e);
        }
        Element rootElement = doc.getRootElement();
        return (Element) rootElement.copy();
        //        return rootElement;
    }

    public static String toXml(Element element) {
        //        OutputStream os = new ByteOutputStream();
        //        try {
        //            Serializer serializer = new Serializer(os, "UTF-8");
        //            serializer.setIndent(4);
        //            serializer.write(new Document(element));
        //        } catch (IOException e) {
        //            throw new RuntimeException("Cannot serialize "e);
        //        }
        //        return os.toString();
        return element.toXML();
    }

    public static String toPrettyXml(Element element) {
        OutputStream os = new ByteArrayOutputStream();
        try {
            Serializer serializer = new Serializer(os, "UTF-8");
            serializer.setIndent(4);
            serializer.write(new Document(element));
        } catch (IOException e) {
            throw new RuntimeException("Cannot serialize document", e);
        }
        return os.toString();
    }

    public static String toXml(List<Element> elements) {
        return Joiner.on("\n").join(Iterables.transform(elements, TO_XML_FUNCTION));
    }

    public static Element teiElement(String name) {
        return new Element(name, TEI_NS);
    }

    public static void addCoords(Element el, String coords) {
        if (coords != null) {
            el.addAttribute(new Attribute("coords", coords));
        }
    }

    public static void addXmlId(Element el, String id) {
        el.addAttribute(new Attribute("xml:id", XML_NS, id));
    }

    public static Node textNode(String text) {
        return new Text(text);
    }

    public static Element teiElement(String name, String content) {
        Element element = new Element(name, TEI_NS);
        element.appendChild(content);
        return element;
    }

    public static void main(String[] args) throws ParsingException, IOException {
        Element e = fromString("<div><a>Test</a></div>");
        System.out.println(toXml(e));

    }

    public static String stripNonValidXMLCharacters(String in) {
        if (in == null || ("".equals(in)))
            return "";
        StringBuilder out = new StringBuilder(in.length());
        // Iterate by Unicode code point, not by char: supplementary characters (e.g. mathematical-italic
        // letters, U+1D400+) are stored as a UTF-16 surrogate pair, and each surrogate (0xD800-0xDFFF)
        // falls outside every valid range when tested individually. A char-wise filter therefore strips
        // them, and the original `current >= 0x10000` test was dead code because a `char` never exceeds
        // 0xFFFF. codePointAt() yields the combined scalar value so valid astral characters are preserved,
        // while genuinely invalid lone surrogates are still dropped.
        for (int i = 0; i < in.length();) {
            int current = in.codePointAt(i);
            if ((current == 0x9) ||
                    (current == 0xA) ||
                    (current == 0xD) ||
                    ((current >= 0x20) && (current <= 0xD7FF)) ||
                    ((current >= 0xE000) && (current <= 0xFFFD)) ||
                    ((current >= 0x10000) && (current <= 0x10FFFF)))
                out.appendCodePoint(current);
            i += Character.charCount(current);
        }
        return out.toString();
    }
}
