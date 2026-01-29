package org.grobid.core.document;

import org.grobid.core.layout.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for additional information for patent document.
 *
 */
public class PatentDocument extends Document {

    private int beginBlockPAReport = -1;

    static public Pattern searchReport = Pattern.compile(
            "((international|interna(\\s)+Η(\\s)+onal)(\\s)+(search)(\\s)+(report))|"
                    + "((internationaler)(\\s)+(recherchenberich))|"
                    + "(I(\\s)+N(\\s)+T(\\s)+E(\\s)+R(\\s)+N(\\s)+A(\\s)+T(\\s)+I(\\s)+O(\\s)+N(\\s)+A(\\s)+L(\\s)+S(\\s)+E(\\s)+A(\\s)+R(\\s)+C(\\s)+H)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    static public Pattern FamilyMembers = Pattern
            .compile("(patent)(\\s)+(famil(v|y))(\\s)+(members)?", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    public PatentDocument(DocumentSource documentSource) {
        super(documentSource);
    }

    public int getBeginBlockPAReport() {
        return beginBlockPAReport;
    }

    public void setBeginBlockPAReport(int begin) {
        beginBlockPAReport = begin;
    }

    /**
     * Return all blocks corresponding to the prior art report of a WO patent publication
     */
    public String getWOPriorArtBlocks() {
        System.out.println("getWOPriorArtBlocks");
        StringBuilder accumulated = new StringBuilder();
        int i = 0;
        boolean PAReport = false;
        boolean newPage = false;
        if (getBlocks() != null) {
            for (Block block : getBlocks()) {
                String content = block.getText();
                if (content != null) {
                    content = content.trim();
                    // System.out.println(content);
                    if (newPage & (!PAReport)) {
                        // System.out.println("new page");
                        Matcher m = PatentDocument.searchReport.matcher(content);

                        if (m.find()) {
                            PAReport = true;
                            beginBlockPAReport = i;
                        }
                    }

                    /*
                     * if (PAReport) { Matcher m = FamilyMembers.matcher(content);
                     *
                     * if (m.find()) { PAReport = false; } }
                     */

                    newPage = content.startsWith("@PAGE");

                    if (PAReport)
                        accumulated.append(content).append("\n");
                }
                i++;
            }
        }
        System.out.println(accumulated.toString());
        return accumulated.toString();
    }

}
