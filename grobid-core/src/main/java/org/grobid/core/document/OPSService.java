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
package org.grobid.core.document;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import org.grobid.core.sax.TextSaxParser;

/**
 *
 *  Usage of the EPO OPS service for online interactive version of patent document retrieval.
 *  We use a POST request since the EPO wsdl file result in a terrible mess with WSDL2Java.
 *
 *  There is now however a new REST interface that should be used instead of the SOAP one.
 *
 *  Service "fair use" implies no more than 6 request per minutes and up to 20 query per
 *  batch SOAP envelope.
 *
 */

public class OPSService {

    public OPSService() {
    }

    static String OPS_HOST = "ops.epo.org";
    static int OPS_PORT = 80;

    public String stripNonValidXMLCharacters(String in) {
        StringBuffer out = new StringBuffer();
        char current;

        if (in == null || ("".equals(in))) {
            return "";
        }
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i);
            if ((current == 0x9) ||
                    (current == 0xA) ||
                    (current == 0xD) ||
                    ((current >= 0x20) && (current <= 0xD7FF)) ||
                    ((current >= 0xE000) && (current <= 0xFFFD)) ||
                    ((current >= 0x10000) && (current <= 0x10FFFF)))
                out.append(current);
        }
        return out.toString();
    }

    /**
     *  Access to full text for a given patent publication number
     */
    public String descriptionRetrieval(String patentNumber) throws IOException,
            ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        try {
            // header
            String envelope = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
            envelope += "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ops=\"http://ops.epo.org\" xmlns:exc=\"http://www.epo.org/exchange\">\n";
            envelope += "<soapenv:Header/>\n";
            envelope += "<soapenv:Body>\n";
            envelope += "<ops:description-retrieval format=\"text-only\" format-version=\"1.0\">\n";

            // body
            envelope += "<exc:publication-reference data-format=\"epodoc\">\n";
            envelope += "<exc:document-id>\n";
            envelope += "<exc:doc-number>" + patentNumber + "</exc:doc-number>\n";
            envelope += "</exc:document-id>\n";
            envelope += "</exc:publication-reference>\n";

            envelope += "</ops:description-retrieval>\n";
            envelope += "</soapenv:Body>\n";
            envelope += "</soapenv:Envelope>\n";

            //Create socket
            InetAddress addr = InetAddress.getByName(OPS_HOST);
            Socket sock = new Socket(addr, OPS_PORT);

            //Send header
            String path = "/soap-services/description-retrieval";
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), "UTF-8"));
            wr.write("POST " + path + " HTTP/1.0\r\n");
            wr.write("Host: " + OPS_HOST + "\r\n");
            wr.write("SOAPAction: description-retrieval" + "\r\n");
            wr.write("Content-Length: " + envelope.length() + "\r\n");
            wr.write("Content-Type: text/xml; charset=\"utf-8\"\r\n");
            wr.write("\r\n");

            //Send data
            wr.write(envelope);
            wr.flush();

            // Response
            BufferedReader rd = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line = null;
            boolean toRead = false;
            while ((line = rd.readLine()) != null) {
                if (line.startsWith("<?xml"))
                    toRead = true;
                if (toRead) {
                    line = stripNonValidXMLCharacters(line);
                    sb.append(line);
                }
            }

            TextSaxParser sax = new TextSaxParser();

            // get a factory
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setValidating(false);
            spf.setFeature("http://xml.org/sax/features/namespaces", false);
            spf.setFeature("http://xml.org/sax/features/validation", false);
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            //get a new instance of parser
            XMLReader reader = spf.newSAXParser().getXMLReader();
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            reader.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId) {
                    return new InputSource(
                            new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes()));
                }
            });
            reader.setContentHandler(sax);
            InputSource input = new InputSource(new StringReader(sb.toString()));
            input.setEncoding("UTF-8");
            reader.parse(input);

            String res = sax.getText();
            if (res != null)
                return res;
            else
                return null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

} // end of class
