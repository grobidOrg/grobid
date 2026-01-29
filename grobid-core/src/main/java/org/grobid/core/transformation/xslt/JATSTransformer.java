package org.grobid.core.transformation.xslt;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;

public class JATSTransformer {

    public final static String xsltPath = "/xslt/grobid-jats.xsl";

    private static Transformer t;

    static {
        net.sf.saxon.TransformerFactoryImpl tf = new net.sf.saxon.TransformerFactoryImpl();
        tf.setURIResolver(new URIResolver() {
            @Override
            public Source resolve(String href, String base) throws TransformerException {
                return new StreamSource(this.getClass().getResource("/xslt/" + href).getPath());
            }
        });

        InputStream is = JATSTransformer.class.getResourceAsStream(xsltPath);

        try {
            t = tf.newTransformer(new StreamSource(is));
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public String transform(String input)
            throws TransformerException, ParserConfigurationException, SAXException, XMLStreamException {
        return transform(input, t);
    }

    protected String transform(String input, Transformer t)
            throws TransformerException, ParserConfigurationException, SAXException, XMLStreamException {
        StringWriter w = new StringWriter();
        Result r = new StreamResult(w);
        StreamSource xmlSource = new StreamSource(new ByteArrayInputStream(input.getBytes(Charset.forName("UTF-8"))));

        t.transform(xmlSource, r);
        return w.toString();
    }

    public String stripNamespaces(String input) {
        return input.replaceAll("xmlns:(pdm|xsl|ext|exch)=\"[^\"]+\"", "");
    }
}
