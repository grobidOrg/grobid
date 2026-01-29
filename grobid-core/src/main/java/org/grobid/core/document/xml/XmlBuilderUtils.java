package org.grobid.core.document.xml;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Text;
import nu.xom.Node;
import nu.xom.ParsingException;
import nu.xom.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.List;

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
        // return rootElement;
    }

    public static String toXml(Element element) {
        // OutputStream os = new ByteOutputStream();
        // try {
        // Serializer serializer = new Serializer(os, "UTF-8");
        // serializer.setIndent(4);
        // serializer.write(new Document(element));
        // } catch (IOException e) {
        // throw new RuntimeException("Cannot serialize "e);
        // }
        // return os.toString();
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
        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in == null || ("".equals(in)))
            return "";
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i);
            if ((current == 0x9) || (current == 0xA) || (current == 0xD) || ((current >= 0x20) && (current <= 0xD7FF))
                    || ((current >= 0xE000) && (current <= 0xFFFD)) || ((current >= 0x10000) && (current <= 0x10FFFF)))
                out.append(current);
        }
        return out.toString();
    }
}
