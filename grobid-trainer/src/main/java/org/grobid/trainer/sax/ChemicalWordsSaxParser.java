package org.grobid.trainer.sax;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.grobid.core.exceptions.GrobidException;

/**
 * This SAX parser process the list of words of a document and produce the labeled data
 * based on stand off annotations.
 *
 * @author Patrice Lopez
 */
public class ChemicalWordsSaxParser extends DefaultHandler {

    private List<String> chemicalAnnotations = null;
    private List<String> chemicalAnnotationsStarts = null;
    private List<String> chemicalFormulas = null;
    private List<String> chemicalFormulasStarts = null;
    private List<String> chemicalSubstances = null;
    private List<String> chemicalSubstancesStarts = null;
    private List<String> chemicalClassNames = null;
    private List<String> chemicalClassNamesStarts = null;
    private List<String> chemicalLigand = null;
    private List<String> chemicalLigandStarts = null;
    private List<String> labeledResult = null;

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private String localID = null;

    public void setChemicalAnnotations(List<String> annotations, List<String> annotationsStarts) {
        chemicalAnnotations = annotations;
        chemicalAnnotationsStarts = annotationsStarts;
    }

    public void setChemicalFormulas(List<String> formulas, List<String> formulasStarts) {
        chemicalFormulas = formulas;
        chemicalFormulasStarts = formulasStarts;
    }

    public void setChemicalSubstances(List<String> substances, List<String> substancesStarts) {
        chemicalSubstances = substances;
        chemicalSubstancesStarts = substancesStarts;
    }

    public void setChemicalClassNames(List<String> classNames, List<String> classNamesStarts) {
        chemicalClassNames = classNames;
        chemicalClassNamesStarts = classNamesStarts;
    }

    public void setChemicalLigand(List<String> ligand, List<String> ligandStarts) {
        chemicalLigand = ligand;
        chemicalLigandStarts = ligandStarts;
    }

    public List<String> getLabeledResult() {
        return labeledResult;
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        return accumulator.toString().trim();
    }

    public void endElement(
            java.lang.String uri,
            java.lang.String localName,
            java.lang.String qName) throws SAXException {
        try {
            if (qName.equals("word")) {
                String word = getText();
                // we determine the label of the word based on localID
                String label = null;
                if (chemicalAnnotationsStarts.contains(localID) ||
                        chemicalFormulasStarts.contains(localID) ||
                        chemicalSubstancesStarts.contains(localID) ||
                        chemicalClassNamesStarts.contains(localID) ||
                        chemicalLigandStarts.contains(localID)) {
                    label = "I-<chemName>";
                } else if (chemicalAnnotations.contains(localID) ||
                        chemicalFormulas.contains(localID) ||
                        chemicalSubstances.contains(localID) ||
                        chemicalClassNames.contains(localID) ||
                        chemicalLigand.contains(localID)) {
                    label = "<chemName>";
                } else {
                    label = "<other>";
                }

                labeledResult.add(word + "\t" + label);
            }
            accumulator.setLength(0);
        } catch (Exception e) {
            //		    e.printStackTrace();
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts) throws SAXException {
        try {
            if (qName.equals("nite:root")) {
                labeledResult = new ArrayList<String>();
            } else if (qName.equals("word")) {
                int length = atts.getLength();

                // Process each attribute
                for (int i = 0; i < length; i++) {
                    // Get names and values for each attribute
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);

                    if ((name != null) && (value != null)) {
                        if (name.equals("nite:id")) {
                            localID = value;
                        }
                    }
                }

            }
        } catch (Exception e) {
            //		    e.printStackTrace();
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

}
