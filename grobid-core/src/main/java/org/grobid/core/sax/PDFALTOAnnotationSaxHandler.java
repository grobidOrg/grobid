package org.grobid.core.sax;

import org.grobid.core.document.Document;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.PDFAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 *  SAX parser for ALTO XML representation of the annotations present on PDF files 
 *  obtained via pdfalto. We only consider here link annotations, other type
 * of annotations (e.g. highlight) are ignored. 
 * 
 */
public class PDFALTOAnnotationSaxHandler extends DefaultHandler {
	public static final Logger LOGGER = LoggerFactory.getLogger(PDFALTOAnnotationSaxHandler.class);
	
	private StringBuilder accumulator = new StringBuilder(); // Accumulate parsed text
	private Document doc = null;
	private List<PDFAnnotation> annotations = null;
	private PDFAnnotation currentAnnotation = null;
	
	private List<Double> x_points = null;
	private List<Double> y_points = null;
	
	public PDFALTOAnnotationSaxHandler(Document doc, List<PDFAnnotation> annotations) {
		this.doc = doc;
		this.annotations = annotations;
	}
	
	public void characters(char[] ch, int start, int length) {
		accumulator.append(ch, start, length);
	}

	public String getText() {
		return accumulator.toString().trim();
	}
	
	public List<PDFAnnotation> getPDFAnnotations() {
		return annotations;
	}
	
	public void endElement(java.lang.String uri, java.lang.String localName,
			java.lang.String qName) throws SAXException {

		if (qName.equals("ANNOTATION")) {
			if (currentAnnotation != null) {
				annotations.add(currentAnnotation);
			}
			currentAnnotation = null;
		} else if (qName.equals("DEST") && (currentAnnotation != null)) {
			currentAnnotation.setDestination(getText());
		} else if (qName.equals("QUADRILATERAL") && (currentAnnotation != null)) {
			// create the bounding box
			double x = -1.0;
			double y = -1.0;
			double width = -1.0;
			double height = -1.0;

			double max = -1.0;
			double min = 1000.0;
			for(double val : x_points) {
				if (val < min)
					min = val;
				if (val > max)
					max = val;
			}
			x = min;
			width = max - min;
			max = -1.0;
			min = 1000.0;
			for(double val : y_points) {
				if (val < min)
					min = val;
				if (val > max)
					max = val;
			}
			y = min;
			height = max - min;
			BoundingBox box = BoundingBox
				.fromPointAndDimensions(currentAnnotation.getPageNumber(), x, y, width, height);
			currentAnnotation.addBoundingBox(box);
		}
	}
	
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		if (qName.equals("ANNOTATION")) {
			// we only consider annotation with attribute @subtype of value "Link" 
			int length = atts.getLength();

			// Process attributes
			for (int i = 0; i < length; i++) {
				// Get names and values for each attribute
				String name = atts.getQName(i);
				String value = atts.getValue(i);

				if ((name != null) && (value != null)) {
					if (name.equals("subtype")) {
						if (value.equals("Link")) {
							currentAnnotation = new PDFAnnotation();	
						}
					} else if (name.equals("pagenum")) {
						int page = -1;
						try {
							page = Integer.parseInt(value);
						} catch(Exception e) {
							LOGGER.error("The page number attribute for PDF annotation is not a valid integer: " + value);
						}
						if (page != -1)
							currentAnnotation.setPageNumber(page);
					} 
				}
			}
		} else if (qName.equals("ACTION") && (currentAnnotation != null)) {
			int length = atts.getLength();

			// Process attributes
			for (int i = 0; i < length; i++) {
				// Get names and values for each attribute
				String name = atts.getQName(i);
				String value = atts.getValue(i);

				if ((name != null) && (value != null)) {
					if (name.equals("type")) {
						if (value.equals("uri")) {
							currentAnnotation.setType(PDFAnnotation.Type.URI);
						} else if (value.equals("goto")) {
							currentAnnotation.setType(PDFAnnotation.Type.GOTO);
						} else if (value.equals("gotor")) {
                            currentAnnotation.setType(PDFAnnotation.Type.GOTOR);
                        } else {
							LOGGER.info("the link annotation type is not recognized: " + value);
							currentAnnotation.setType(PDFAnnotation.Type.UNKNOWN);
						}
					}
				}
			}
		} else if (qName.equals("POINT") && (currentAnnotation != null)) {
			int length = atts.getLength();

			// Process attributes
			for (int i = 0; i < length; i++) {
				// Get names and values for each attribute
				String name = atts.getQName(i);
				String value = atts.getValue(i);

				if ((name != null) && (value != null)) {
					if (name.equals("HPOS")) {
						double val = -1.0;
						try {
							val = Double.parseDouble(value);
						} catch(Exception e) {
							LOGGER.error("The value for x coordinate attribute is not a valid double: " + value);
						}
						if (val != -1.0)
							x_points.add(val);
					} else if (name.equals("VPOS")) {
						double val = -1.0;
						try {
							val = Double.parseDouble(value);
						} catch(Exception e) {
							LOGGER.error("The value for y coordinate attribute is not a valid double: " + value);
						}
						if (val != -1.0)
							y_points.add(val);
					}	
				}
			}
		} else if (qName.equals("QUADRILATERAL") && (currentAnnotation != null)) {
			x_points = new ArrayList<Double>();
			y_points = new ArrayList<Double>();
		} 
		accumulator.setLength(0);
	}
	
}