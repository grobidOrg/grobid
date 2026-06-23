package org.grobid.core.visualization;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Multimap;
import net.sf.saxon.trans.XPathException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.DataSetContext;
import org.grobid.core.data.Equation;
import org.grobid.core.data.Figure;
import org.grobid.core.data.Person;
import org.grobid.core.data.Table;
import org.grobid.core.document.Document;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.Page;
import org.grobid.core.utilities.DataSetContextExtractor;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.Pair;

/**
 *  Utilities for visualizing citation markers and biblographical references, either directly
 *  in the PDF using the PDF annotation layer or as annotations in JSON for supporting
 *  web based rendering (e.g. with PDF.js) and interactive HTML layer.
 *  See the web console/demo for actual examples of usage.
 */
public class CitationsVisualizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CitationsVisualizer.class);

    private static final JsonFactory jFactory = new JsonFactory();

    /**
     *  Augment a PDF with bibliographical annotation, for bib. ref. and bib markers.
     *  The PDF annotation layer is used with "GoTo" and "URI" action links.
     *  The annotations of the bibliographical references can be associated to an URL in order
     *  to have clickable references directly in the PDF.
     *  The Apache PDFBox library is used.
     *
     *  @param document PDDocument object resulting from the PDF parsing with PDFBox
     *  @param teiDoc the Document object resulting from the full document structuring
     *  @param resolvedBibRefUrl the list of URL to be added to the bibliographical reference
     *  annotations, if null the bib. ref. annotations are not associated to external URL.
     */
    public static PDDocument annotatePdfWithCitations(
            PDDocument document,
            Document teiDoc,
            List<String> resolvedBibRefUrl) throws IOException, XPathException {
        String tei = teiDoc.getTei();
        //System.out.println(tei);
        int totalBib = 0;
        int totalMarkers1 = 0;
        int totalMarkers2 = 0;
        Multimap<String, DataSetContext> contexts = DataSetContextExtractor.getCitationReferences(tei);
        Map<String, Pair<Integer, Integer>> dictionary = new HashMap<>();
        int indexBib = 0;
        for (BibDataSet cit : teiDoc.getBibDataSets()) {
            String teiId = cit.getResBib().getTeiId();
            totalBib++;
            String theUrl = null;
            if ((resolvedBibRefUrl != null) &&
                    (resolvedBibRefUrl.size() > indexBib) &&
                    (resolvedBibRefUrl.get(indexBib) != null))
                theUrl = resolvedBibRefUrl.get(indexBib);
            else {
                // by default we put the existing url, doi or arXiv link
                BiblioItem biblio = cit.getResBib();
                if (!StringUtils.isEmpty(biblio.getDOI())) {
                    theUrl = "https://dx.doi.org/" + biblio.getDOI();
                } else if (!StringUtils.isEmpty(biblio.getArXivId())) {
                    theUrl = "https://arxiv.org/abs/" + biblio.getArXivId();
                } else if (!StringUtils.isEmpty(biblio.getWeb())) {
                    theUrl = biblio.getWeb();
                }
            }
            if (cit.getResBib().getCoordinates() != null) {
                for (BoundingBox b : cit.getResBib().getCoordinates()) {
                    annotatePage(document, b.toString(), teiId, theUrl, 1.5f, false, dictionary);
                }
            }
            //annotating reference markers
            for (DataSetContext c : contexts.get(teiId)) {
                //System.out.println(c.getContext());
                String mrect = c.getDocumentCoords();
                if ((mrect != null) && (mrect.trim().length() > 0)) {
                    for (String coords : mrect.split(";")) {
                        if (coords.trim().length() == 0)
                            continue;
                        annotatePage(document, coords, teiId, null, 1.0f, true, dictionary);
                        totalMarkers1++;
                    }
                }
            }
            indexBib++;
        }
        for (DataSetContext c : contexts.get("")) {
            String mrect = c.getDocumentCoords();
            if ((mrect != null) && (mrect.trim().length() > 0)) {
                for (String coords : mrect.split(";")) {
                    if (coords.trim().length() == 0)
                        continue;
                    annotatePage(document, coords, null, null, 1.0f, true, dictionary);
                    totalMarkers2++;
                }
            }
        }

        if (teiDoc.getResHeader() != null && teiDoc.getResHeader().getFullAuthors() != null) {
            for (Person p : teiDoc.getResHeader().getFullAuthors()) {
                if (p.getLayoutTokens() != null) {
                    String coordsString = LayoutTokensUtil.getCoordsString(p.getLayoutTokens());
                    for (String coords : coordsString.split(";")) {
                        annotatePage(
                                document,
                                coords,
                                "123",
                                null,
                                //                                p.getLastName() == null ? 1 : p.getLastName().hashCode(),
                                1.0f,
                                true,
                                dictionary);
                    }
                }
            }
        }

        LOGGER.debug("totalBib: " + totalBib);
        LOGGER.debug("totalMarkers1: " + totalMarkers1);
        LOGGER.debug("totalMarkers2: " + totalMarkers2);
        return document;
    }

    private static void annotatePage(
            PDDocument document,
            String coords,
            String teiId,
            String uri,
            float lineWidth,
            boolean isMarker,
            Map<String, Pair<Integer, Integer>> dictionary) throws IOException {
        //System.out.println("Annotating for coordinates: " + coords);
        /*long seed = 0L;
        if (teiId != null)
        	seed = teiId.hashCode();*/
        if (StringUtils.isEmpty(coords)) {
            return;
        }
        String[] split = coords.split(",");

        Long pageNum = Long.valueOf(split[0], 10) - 1;

        if (pageNum < 0 || pageNum >= document.getNumberOfPages()) {
            LOGGER.warn(
                    "Annotation skipped: page index "
                            + (pageNum + 1)
                            + " is out of range (document reports "
                            + document.getNumberOfPages()
                            + " page(s)).");
            return;
        }
        PDPage page = document.getDocumentCatalog().getPages().get(pageNum.intValue());

        PDRectangle mediaBox = page.getCropBox();
        if (mediaBox == null) {
            mediaBox = page.getMediaBox();
            // this will look for the main media box of the page up in the PDF element hierarchy
            if (mediaBox == null) {
                // last hope
                mediaBox = page.getArtBox();
                if (mediaBox == null) {
                    // we tried our best given PDFBox
                    LOGGER.warn("Media box for page " + pageNum.intValue() + " not found.");
                    return;
                }
            }
        }

        float height = mediaBox.getHeight();
        float lowerX = mediaBox.getLowerLeftX();
        float lowerY = mediaBox.getLowerLeftY();

        float x = Float.parseFloat(split[1]);
        float y = Float.parseFloat(split[2]);
        float w = Float.parseFloat(split[3]);
        float h = Float.parseFloat(split[4]);

        float annX = x + lowerX;
        float annY = (height - (y + h)) + lowerY;
        float annRightX = x + w + lowerX;
        float annTopY = height - y + lowerY;

        PDRectangle rect = new PDRectangle();

        rect.setLowerLeftX(annX);
        rect.setLowerLeftY(annY);
        rect.setUpperRightX(annRightX);
        rect.setUpperRightY(annTopY);

        PDBorderStyleDictionary borderULine = new PDBorderStyleDictionary();
        borderULine.setStyle(PDBorderStyleDictionary.STYLE_BEVELED);
        // so that a border is not visible at all
        borderULine.setWidth(0);

        PDAnnotationLink txtLink = new PDAnnotationLink();
        txtLink.setBorderStyle(borderULine);

        //white rectangle border color (ideally, should be transparent)
        COSArray white = new COSArray();
        white.setFloatArray(new float[]{1f, 1f, 1f});
        txtLink.setColor(new PDColor(white, PDDeviceRGB.INSTANCE));
        txtLink.setReadOnly(true);
        txtLink.setHighlightMode(PDAnnotationLink.HIGHLIGHT_MODE_PUSH);

        if (isMarker && (teiId != null)) {
            Pair<Integer, Integer> thePlace = dictionary.get(teiId);
            if (thePlace != null) {
                PDPageFitWidthDestination destination = new PDPageFitWidthDestination();
                PDPage pdpage = document.getPage(thePlace.getA());
                destination.setPage(pdpage);
                //destination.setPageNumber(thePlace.getA());
                destination.setTop(thePlace.getB());
                PDActionGoTo action = new PDActionGoTo();
                action.setDestination(destination);
                txtLink.setAction(action);
            }
        } else {
            if (teiId != null) {
                // register the object in the dictionary
                if (dictionary.get(teiId) == null) {
                    Pair<Integer, Integer> thePlace = new Pair<>(pageNum.intValue(), Math.round(annTopY + h));
                    dictionary.put(teiId, thePlace);
                }
            }
            if (uri != null) {
                PDActionURI action = new PDActionURI();
                if (uri.endsWith("fulltext/original"))
                    uri = uri.replace("fulltext/original", "fulltext/pdf");
                action.setURI(uri);
                txtLink.setAction(action);
            } else
                return;
        }
        txtLink.setRectangle(rect);

        // adding link to the reference
        page.getAnnotations().add(txtLink);

        //draw a line
        PDBorderStyleDictionary borderThick = new PDBorderStyleDictionary();
        borderThick.setWidth(1);  // 12th inch

        // adding line to the reference
        PDPageContentStream stream = new PDPageContentStream(document, page, true, false, true);
        //Random r = new Random(seed + 1);

        //        stream.setStrokingColor(85, 177, 245);
        //stream.setStrokingColor(r.nextInt(255), r.nextInt(255), r.nextInt(255));
        stream.setStrokingColor(0, 0, 255);
        if (isMarker || (uri != null))
            stream.setLineWidth(lineWidth);
        else
            stream.setLineWidth(0f);
        stream.drawLine(annX, annY, annRightX, annY);
        stream.close();
    }

    public static String getJsonAnnotations(Document teiDoc, List<String> resolvedBibRefUrl)
            throws IOException, XPathException {
        return getJsonAnnotations(teiDoc, resolvedBibRefUrl, false);
    }

    /**
     *  Produce JSON annotations with PDF coordinates for web based PDF rendering. Annotations
     *  are given for bib. ref. and bib markers separately, together with the dimension of the
     *  different pages for client resizing.
     *  The annotations of the bibliographical references can be associated to an URL in
     *  order to support "clickable" bib. ref. annotations.
     *
     *  @param teiDoc the Document object resulting from the full document structuring
     *  @param resolvedBibRefUrl the list of URL to be added to the bibliographical reference
     *                           annotations, if null the bib. ref. annotations are not associated
     *                           to external URL.
     *  @param addFiguresTables if true, also annotate figure and table areas, plus the callout
     *                          to figures and tables
     *
     */
    public static String getJsonAnnotations(Document teiDoc, List<String> resolvedBibRefUrl, boolean addFiguresTables)
            throws IOException, XPathException {
        StringWriter refW = new StringWriter();
        JsonGenerator jsonRef = jFactory.createGenerator(refW);
        //jsonRef.useDefaultPrettyPrinter();
        jsonRef.writeStartObject();

        // page height and width
        List<Page> pages = teiDoc.getPages();
        int pageNumber = 1;
        jsonRef.writeArrayFieldStart("pages");
        for (Page page : pages) {
            jsonRef.writeStartObject();
            jsonRef.writeNumberField("page_height", page.getHeight());
            jsonRef.writeNumberField("page_width", page.getWidth());
            jsonRef.writeEndObject();
            pageNumber++;
        }
        jsonRef.writeEndArray();

        StringWriter markW = new StringWriter();
        JsonGenerator jsonMark = jFactory.createGenerator(markW);
        jsonMark.writeStartArray();

        int totalMarkers1 = 0;
        int totalMarkers2 = 0;
        int totalBib = 0;

        jsonRef.writeArrayFieldStart("refBibs");
        String tei = teiDoc.getTei();
        Multimap<String, DataSetContext> contexts = DataSetContextExtractor.getCitationReferences(tei);
        int bibIndex = 0;
        for (BibDataSet cit : teiDoc.getBibDataSets()) {
            String teiId = cit.getResBib().getTeiId();
            totalBib++;
            jsonRef.writeStartObject();
            jsonRef.writeStringField("id", teiId);
            // url if any - they are passed via the resolvedBibRefUrl vector provided as argument
            if ((resolvedBibRefUrl != null) &&
                    (resolvedBibRefUrl.size() > bibIndex) &&
                    (resolvedBibRefUrl.get(bibIndex) != null)) {
                jsonRef.writeStringField("url", resolvedBibRefUrl.get(bibIndex));
            } else {
                // by default we put the existing url, doi or arXiv link
                BiblioItem biblio = cit.getResBib();
                String theUrl = null;

                if (!StringUtils.isEmpty(biblio.getOAURL())) {
                    theUrl = biblio.getOAURL();
                } else if (!StringUtils.isEmpty(biblio.getDOI())) {
                    theUrl = "https://dx.doi.org/" + biblio.getDOI();
                } else if (!StringUtils.isEmpty(biblio.getArXivId())) {
                    theUrl = "https://arxiv.org/abs/" + biblio.getArXivId();
                } else if (!StringUtils.isEmpty(biblio.getWeb())) {
                    theUrl = biblio.getWeb();
                }
                if (theUrl != null)
                    jsonRef.writeStringField("url", theUrl);
            }
            jsonRef.writeArrayFieldStart("pos");
            if (cit.getResBib().getCoordinates() != null) {
                for (BoundingBox b : cit.getResBib().getCoordinates()) {
                    // reference string
                    jsonRef.writeStartObject();
                    b.writeJsonProps(jsonRef);
                    jsonRef.writeEndObject();
                }
            }
            jsonRef.writeEndArray(); // pos
            jsonRef.writeEndObject(); // refBibs element
            // reference markers for this reference
            for (DataSetContext c : contexts.get(teiId)) {
                //System.out.println(c.getContext());
                String mrect = c.getDocumentCoords();
                if ((mrect != null) && (mrect.trim().length() > 0)) {
                    for (String coords : mrect.split(";")) {
                        if ((coords == null) || (coords.length() == 0))
                            continue;
                        //annotatePage(document, coords, teiId.hashCode(), 1.0f);
                        jsonMark.writeStartObject();
                        jsonMark.writeStringField("id", teiId);
                        BoundingBox b2 = BoundingBox.fromString(coords);
                        b2.writeJsonProps(jsonMark);
                        jsonMark.writeEndObject();
                        totalMarkers1++;
                    }
                }
            }
            bibIndex++;
        }
        jsonRef.writeEndArray(); // refBibs

        // remaining reference markers which have not been solved with an actual full
        // bibliographical reference object
        for (DataSetContext c : contexts.get("")) {
            String mrect = c.getDocumentCoords();
            if ((mrect != null) && (mrect.trim().length() > 0)) {
                for (String coords : mrect.split(";")) {
                    if (coords.trim().length() == 0)
                        continue;
                    //annotatePage(document, coords, 0, 1.0f);
                    BoundingBox b = BoundingBox.fromString(coords);
                    jsonMark.writeStartObject();
                    b.writeJsonProps(jsonMark);
                    jsonMark.writeEndObject();
                    totalMarkers2++;
                }
            }
        }
        jsonMark.writeEndArray();
        jsonMark.close();

        LOGGER.debug("totalBib: " + totalBib);
        LOGGER.debug("totalBibMarkers1: " + totalMarkers1);
        LOGGER.debug("totalBibMarkers2: " + totalMarkers2);

        jsonRef.writeFieldName("refMarkers");
        jsonRef.writeRawValue(markW.toString());

        // for the same price, we add the formulas
        markW = new StringWriter();
        jsonMark = jFactory.createGenerator(markW);
        jsonMark.writeStartArray();

        totalMarkers1 = 0;
        totalMarkers2 = 0;
        int totalFormulas = 0;

        jsonRef.writeArrayFieldStart("formulas");
        contexts = DataSetContextExtractor.getFormulaReferences(tei);
        if (CollectionUtils.isNotEmpty(teiDoc.getEquations())) {
            for (Equation formula : teiDoc.getEquations()) {
                String teiId = formula.getTeiId();
                totalFormulas++;
                jsonRef.writeStartObject();
                jsonRef.writeStringField("id", teiId);

                jsonRef.writeArrayFieldStart("pos");
                if (formula.getCoordinates() != null) {
                    for (BoundingBox b : formula.getCoordinates()) {
                        // reference string
                        jsonRef.writeStartObject();
                        b.writeJsonProps(jsonRef);
                        jsonRef.writeEndObject();
                    }
                }
                jsonRef.writeEndArray(); // pos
                jsonRef.writeEndObject(); // formula element

                // reference markers for this formula
                for (DataSetContext c : contexts.get(teiId)) {
                    //System.out.println(c.getContext());
                    String mrect = c.getDocumentCoords();
                    if ((mrect != null) && (mrect.trim().length() > 0)) {
                        for (String coords : mrect.split(";")) {
                            if ((coords == null) || (coords.length() == 0))
                                continue;
                            //annotatePage(document, coords, teiId.hashCode(), 1.0f);
                            jsonMark.writeStartObject();
                            jsonMark.writeStringField("id", teiId);
                            BoundingBox b2 = BoundingBox.fromString(coords);
                            b2.writeJsonProps(jsonMark);
                            jsonMark.writeEndObject();
                            totalMarkers1++;
                        }
                    }
                }
            }
        }
        jsonRef.writeEndArray(); // formulas

        // remaining formula markers which have not been solved with an actual full
        // formula object
        for (DataSetContext c : contexts.get("")) {
            String mrect = c.getDocumentCoords();
            if ((mrect != null) && (mrect.trim().length() > 0)) {
                for (String coords : mrect.split(";")) {
                    if (coords.trim().length() == 0)
                        continue;
                    //annotatePage(document, coords, 0, 1.0f);
                    BoundingBox b = BoundingBox.fromString(coords);
                    jsonMark.writeStartObject();
                    b.writeJsonProps(jsonMark);
                    jsonMark.writeEndObject();
                    totalMarkers2++;
                }
            }
        }
        jsonMark.writeEndArray();
        jsonMark.close();

        jsonRef.writeFieldName("formulaMarkers");
        jsonRef.writeRawValue(markW.toString());

        LOGGER.debug("totalFormulas: " + totalBib);
        LOGGER.debug("totalFormulaMarkers1: " + totalMarkers1);
        LOGGER.debug("totalFormulaMarkers2: " + totalMarkers2);

        // if requested, for the same price, we add the figures+tables
        if (addFiguresTables) {
            markW = new StringWriter();
            jsonMark = jFactory.createGenerator(markW);
            jsonMark.writeStartArray();

            totalMarkers1 = 0;
            totalMarkers2 = 0;
            int totalFigures = 0;

            jsonRef.writeArrayFieldStart("figures");
            contexts = DataSetContextExtractor.getFigureReferences(tei);
            for (Figure figure : teiDoc.getFigures()) {
                String teiId = figure.getTeiId();
                totalFigures++;
                jsonRef.writeStartObject();
                jsonRef.writeStringField("id", teiId);

                jsonRef.writeArrayFieldStart("pos");
                if (figure.getCoordinates() != null) {
                    for (BoundingBox b : figure.getCoordinates()) {
                        // reference string
                        jsonRef.writeStartObject();
                        b.writeJsonProps(jsonRef);
                        jsonRef.writeEndObject();
                    }
                }
                jsonRef.writeEndArray(); // pos
                jsonRef.writeEndObject(); // figure element

                // reference markers for this figure
                for (DataSetContext c : contexts.get(teiId)) {
                    //System.out.println(c.getContext());
                    String mrect = c.getDocumentCoords();
                    if ((mrect != null) && (mrect.trim().length() > 0)) {
                        for (String coords : mrect.split(";")) {
                            if ((coords == null) || (coords.length() == 0))
                                continue;
                            //annotatePage(document, coords, teiId.hashCode(), 1.0f);
                            jsonMark.writeStartObject();
                            jsonMark.writeStringField("id", teiId);
                            BoundingBox b2 = BoundingBox.fromString(coords);
                            b2.writeJsonProps(jsonMark);
                            jsonMark.writeEndObject();
                            totalMarkers1++;
                        }
                    }
                }
            }
            jsonRef.writeEndArray(); // figures

            // remaining reference markers which have not been solved with an actual
            // figure object
            for (DataSetContext c : contexts.get("")) {
                String mrect = c.getDocumentCoords();
                if ((mrect != null) && (mrect.trim().length() > 0)) {
                    for (String coords : mrect.split(";")) {
                        if (coords.trim().length() == 0)
                            continue;
                        //annotatePage(document, coords, 0, 1.0f);
                        BoundingBox b = BoundingBox.fromString(coords);
                        jsonMark.writeStartObject();
                        b.writeJsonProps(jsonMark);
                        jsonMark.writeEndObject();
                        totalMarkers2++;
                    }
                }
            }
            jsonMark.writeEndArray();
            jsonMark.close();

            jsonRef.writeFieldName("figureMarkers");
            jsonRef.writeRawValue(markW.toString());

            LOGGER.debug("totalFigures: " + totalBib);
            LOGGER.debug("totalFigureMarkers1: " + totalMarkers1);
            LOGGER.debug("totalFigureMarkers2: " + totalMarkers2);

            // same for tables
            markW = new StringWriter();
            jsonMark = jFactory.createGenerator(markW);
            jsonMark.writeStartArray();

            totalMarkers1 = 0;
            totalMarkers2 = 0;
            int totalTables = 0;

            jsonRef.writeArrayFieldStart("tables");
            contexts = DataSetContextExtractor.getTableReferences(tei);
            for (Table table : teiDoc.getTables()) {
                String teiId = table.getTeiId();
                totalTables++;
                jsonRef.writeStartObject();
                jsonRef.writeStringField("id", teiId);

                jsonRef.writeArrayFieldStart("pos");
                if (table.getCoordinates() != null) {
                    for (BoundingBox b : table.getCoordinates()) {
                        // reference string
                        jsonRef.writeStartObject();
                        b.writeJsonProps(jsonRef);
                        jsonRef.writeEndObject();
                    }
                }
                jsonRef.writeEndArray(); // pos
                jsonRef.writeEndObject(); // table element

                // reference markers for this table
                for (DataSetContext c : contexts.get(teiId)) {
                    //System.out.println(c.getContext());
                    String mrect = c.getDocumentCoords();
                    if ((mrect != null) && (mrect.trim().length() > 0)) {
                        for (String coords : mrect.split(";")) {
                            if ((coords == null) || (coords.length() == 0))
                                continue;
                            //annotatePage(document, coords, teiId.hashCode(), 1.0f);
                            jsonMark.writeStartObject();
                            jsonMark.writeStringField("id", teiId);
                            BoundingBox b2 = BoundingBox.fromString(coords);
                            b2.writeJsonProps(jsonMark);
                            jsonMark.writeEndObject();
                            totalMarkers1++;
                        }
                    }
                }
            }
            jsonRef.writeEndArray(); // tables

            // remaining reference markers which have not been solved with an actual full
            // table object
            for (DataSetContext c : contexts.get("")) {
                String mrect = c.getDocumentCoords();
                if ((mrect != null) && (mrect.trim().length() > 0)) {
                    for (String coords : mrect.split(";")) {
                        if (coords.trim().length() == 0)
                            continue;
                        //annotatePage(document, coords, 0, 1.0f);
                        BoundingBox b = BoundingBox.fromString(coords);
                        jsonMark.writeStartObject();
                        b.writeJsonProps(jsonMark);
                        jsonMark.writeEndObject();
                        totalMarkers2++;
                    }
                }
            }
            jsonMark.writeEndArray();
            jsonMark.close();

            jsonRef.writeFieldName("tableMarkers");
            jsonRef.writeRawValue(markW.toString());

            LOGGER.debug("totalTables: " + totalBib);
            LOGGER.debug("totalTableMarkers1: " + totalMarkers1);
            LOGGER.debug("totalTableMarkers2: " + totalMarkers2);
        }

        jsonRef.writeEndObject();
        jsonRef.close();

        return refW.toString();
    }

    /*
     * A variant where annotations are provided page per page
     */
    /*public static String getJsonAnnotationsPerPage(Document teiDoc, List<String> resolvedBibRefUrl) throws IOException, XPathException {
        StringBuilder jsonRef = new StringBuilder();
        jsonRef.append("{\"pages\" : [");

        int totalMarkers1 = 0;
        int totalMarkers2 = 0;
        int totalBib = 0;

        List<Page> pages = teiDoc.getPages();
        int pageNumber = 1;
        for(Page page : pages) {
            if (pageNumber > 1)
                jsonRef.append(", ");

            // page height and width
            jsonRef.append("{\"page_height\":" + page.getHeight());
            jsonRef.append(", \"page_width\":" + page.getWidth());

            boolean refBibOutput = false;
            StringBuilder jsonMark = new StringBuilder();
            boolean refMarkOutput = false;

            String tei = teiDoc.getTei();
            Multimap<String, DataSetContext> contexts = DataSetContextExtractor.getCitationReferences(tei);

            boolean beginMark = true;
            boolean begin = true;
            for (BibDataSet cit : teiDoc.getBibDataSets()) {
                String teiId = cit.getResBib().getTeiId();
                boolean idOutput = false;

                boolean begin2 = true;
                if (cit.getResBib().getCoordinates() != null) {
                    for (BoundingBox b : cit.getResBib().getCoordinates()) {
                        if (b.getPage() == pageNumber) {
                            if (!refBibOutput) {
                                jsonRef.append(", \"refBibs\": [ ");
                                refBibOutput = true;
                            }
                            if (!idOutput) {
                                if (begin)
                                    begin = false;
                                else
                                    jsonRef.append(", ");
                                jsonRef.append("{\"id\":\"").append(teiId).append("\", ");
                                jsonRef.append("\"pos\":[");
                                idOutput = true;
                            }

                            // reference string
                            if (begin2)
                                begin2 = false;
                            else
                                jsonRef.append(", ");

                            jsonRef.append("{").append(b.toJson()).append("}");
                            totalBib++;
                        }
                        //annotatePage(document, b.toString(), teiId.hashCode(), contexts.containsKey(teiId) ? 1.5f : 0.5f);
                    }
                }

                // reference markers for this reference
                for (DataSetContext c : contexts.get(teiId)) {
                    //System.out.println(c.getContext());
                    String mrect = c.getDocumentCoords();
                    if ( (mrect != null) && (mrect.trim().length()>0) ) {
                        for (String coords : mrect.split(";")) {
                            if (coords.trim().length() == 0)
                                continue;
                            //annotatePage(document, coords, teiId.hashCode(), 1.0f);
                            BoundingBox b2 = BoundingBox.fromString(coords);
                            if (b2.getPage() == pageNumber) {
                                if (!refMarkOutput) {
                                    jsonMark.append(", \"refMarkers\": [");
                                    refMarkOutput = true;
                                } else
                                    jsonMark.append(", ");
                                jsonMark.append("{ \"id\":\"").append(teiId).append("\", ");
                                jsonMark.append(b2.toJson()).append(" }");
                                totalMarkers1++;
                            }
                        }
                    }
                }
                 if (idOutput) {
                    jsonRef.append("] }");
                }
            }

            for (DataSetContext c : contexts.get("")) {
                String mrect = c.getDocumentCoords();
                if ( (mrect != null) && (mrect.trim().length()>0) ) {
                    for (String coords : mrect.split(";")) {
                        if (coords.trim().length() == 0)
                            continue;
                        BoundingBox b = BoundingBox.fromString(coords);
                        if (b.getPage() == pageNumber) {
                            if (!refMarkOutput) {
                                jsonMark.append(", \"refMarkers\": [");
                                refMarkOutput = true;
                            } else
                                jsonMark.append(", ");
                            jsonMark.append("{").append(b.toJson()).append("}");
                            totalMarkers2++;
                        }
                    }
                }
            }
            pageNumber++;
            if (refBibOutput) {
                jsonRef.append("]");

            }
            if (refMarkOutput) {
                jsonRef.append(jsonMark.toString()).append("]");
            }
            jsonRef.append("}");
        }

        LOGGER.debug("totalBib: " + totalBib);
        LOGGER.debug("totalMarkers1: " + totalMarkers1);
        LOGGER.debug("totalMarkers2: " + totalMarkers2);

        jsonRef.append("]}");
        return jsonRef.toString();
    }*/

}
