package org.grobid.core.data;

import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.KeyGen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Class for representing a funding/grant.
 */
public class Funding {
    private Funder funder = null;

    // this is an identifier for identifying and referencing the funding inside the full document
    private String identifier = null;

    // program or call
    private String programFullName = null;
    private List<LayoutToken> programFullNameLayoutTokens = new ArrayList<>();

    private String programAbbreviatedName = null;
    private List<LayoutToken> programAbbreviatedNameLayoutTokens = new ArrayList<>();

    private String grantNumber  = null;
    private List<LayoutToken> grantNumberLayoutTokens = new ArrayList<>();

    private String grantName = null;
    private List<LayoutToken> grantNameLayoutTokens = new ArrayList<>();

    private String projectFullName = null;
    private List<LayoutToken> projectFullNameLayoutTokens = new ArrayList<>();

    private String projectAbbreviatedName = null;
    private List<LayoutToken> projectAbbreviatedNameLayoutTokens = new ArrayList<>();

    private String url = null;
    private List<LayoutToken> urlLayoutTokens = new ArrayList<>();

    private List<LayoutToken> layoutTokens = new ArrayList<>();

    private Date start = null;
    private Date end = null;

    public Funder getFunder() {
        return this.funder;
    }

    public void setFunder(Funder funder) {
        this.funder = funder;
    }

    public String getProgramFullName() {
        return this.programFullName;
    }

    public void setProgramFullName(String programFullName) {
        this.programFullName = programFullName;
    }

    public void appendProgramFullNameLayoutTokens(List<LayoutToken> layoutTokens) {
        this.programFullNameLayoutTokens.addAll(layoutTokens);
    }

    public List<LayoutToken> getProgramFullNameLayoutTokens() {
        return this.programFullNameLayoutTokens;
    }

    public String getProgramAbbreviatedName() {
        return this.programAbbreviatedName;
    }

    public void setProgramAbbreviatedName(String programAbbreviatedName) {
        this.programAbbreviatedName = programAbbreviatedName;
    }

    public void appendProgramAbbreviatedNameLayoutTokens(List<LayoutToken> layoutTokens) {
        this.programAbbreviatedNameLayoutTokens.addAll(layoutTokens);
    }

    public List<LayoutToken> getProgramAbbreviatedNameLayoutTokens() {
        return this.programAbbreviatedNameLayoutTokens;
    }

    public String getGrantNumber() {
        return this.grantNumber;
    }

    public void setGrantNumber(String grantNumber) {
        if (grantNumber != null && grantNumber.startsWith("n˚"))
            grantNumber = grantNumber.replace("n˚", "");
        this.grantNumber = grantNumber;
    }

    public void appendGrantNumberLayoutTokens(List<LayoutToken> layoutTokens) {
        this.grantNumberLayoutTokens.addAll(layoutTokens);
    }

    public List<LayoutToken> getGrantNumberLayoutTokens() {
        return this.grantNumberLayoutTokens;
    }

    public String getGrantName() {
        return this.grantName;
    }

    public void setGrantName(String grantName) {
        this.grantName = grantName;
    }

    public void appendGrantNameLayoutTokens(List<LayoutToken> layoutTokens) {
        this.grantNameLayoutTokens.addAll(layoutTokens);
    }

    public List<LayoutToken> getGrantNameLayoutTokens() {
        return this.grantNameLayoutTokens;
    }

    public String getProjectFullName() {
        return this.projectFullName;
    }

    public void setProjectFullName(String project) {
        this.projectFullName = project;
    }

    public void appendProjectFullNameLayoutTokens(List<LayoutToken> layoutTokens) {
        this.projectFullNameLayoutTokens.addAll(layoutTokens);
    }

    public List<LayoutToken> getProjectFullNameLayoutTokens() {
        return this.projectFullNameLayoutTokens;
    }

    public String projectAbbreviatedName() {
        return this.projectAbbreviatedName;
    }

    public void setProjectAbbreviatedName(String project) {
        this.projectAbbreviatedName = project;
    }

    public void appendProjectAbbreviatedNameLayoutTokens(List<LayoutToken> layoutTokens) {
        this.projectAbbreviatedNameLayoutTokens.addAll(layoutTokens);
    }

    public List<LayoutToken> getProjectAbbreviatedNameLayoutTokens() {
        return this.projectAbbreviatedNameLayoutTokens;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIdentifier() {
        if (this.identifier == null) {
            String localId = KeyGen.getKey().substring(0, 7);
            this.identifier = "_" + localId;
        }

        return this.identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void appendUrlLayoutTokens(List<LayoutToken> layoutTokens) {
        this.urlLayoutTokens.addAll(layoutTokens);
    }

    public List<LayoutToken> getUrlLayoutTokens() {
        return this.urlLayoutTokens;
    }

    public List<LayoutToken> getLayoutTokens() {
        return this.layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }

    public void addLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens.addAll(layoutTokens);
    }

    public boolean isValid() {
        if (funder != null ||
            grantNumber != null ||
            grantName != null ||
            projectFullName != null ||
            projectAbbreviatedName != null ||
            programFullName != null ||
            programAbbreviatedName != null ||
            url != null)
            return true;
        else
            return false;
    }

    public boolean isNonEmptyFunding() {
        if (grantNumber != null ||
            grantName != null ||
            projectFullName != null ||
            projectAbbreviatedName != null ||
            programFullName != null ||
            programAbbreviatedName != null ||
            url != null)
            return true;
        else
            return false;
    }


    /**
     * For the given funder instance, try to define the acronym, either as part of the current
     * full name, or as prefix in the grant number for some well-known funders.
     **/
    public void inferAcronyms() {
        if (this.funder == null || funder.getFullNameLayoutTokens() == null)
            return;
        //System.out.println(LayoutTokensUtil.toText(funder.getFullNameLayoutTokens()));

        // check if full name contains acronym
        Pair<OffsetPosition, OffsetPosition> acronymCandidate = TextUtilities.fieldAcronymCandidate(funder.getFullNameLayoutTokens());
        if (acronymCandidate != null) {
            OffsetPosition acronymPosition = acronymCandidate.getLeft();
            OffsetPosition basePosition = acronymCandidate.getRight();

            //System.out.println(LayoutTokensUtil.toText(funder.getFullNameLayoutTokens().subList(acronymPosition.start, acronymPosition.end)));
            //System.out.println(LayoutTokensUtil.toText(funder.getFullNameLayoutTokens().subList(basePosition.start, basePosition.end)));

            // post validate acronym candidate: we need matching with base component
            // get first letter profile for the tokens
            StringBuilder profileBase = new StringBuilder();
            for(LayoutToken token : funder.getFullNameLayoutTokens()) {
                if (token.getText() == null || token.getText().length() ==0)
                    continue;
                profileBase.append(token.getText().charAt(0));
            }
            String acronymString = LayoutTokensUtil.toText(funder.getFullNameLayoutTokens().subList(acronymPosition.start, acronymPosition.end));
            String profileBaseString = profileBase.toString();
            boolean validAcronym = true;
            int profilePosIndex = 0;
            for (int i=0; i<acronymString.length(); i++) {
                char theChar = acronymString.charAt(i);
                int posMatch = profileBaseString.indexOf(""+theChar, profilePosIndex);
                if (posMatch == -1) {
                    validAcronym = false;
                    break;
                } else {
                    profilePosIndex = posMatch;
                }
            }

            if (validAcronym) {
                this.funder.setAbbreviatedName(acronymString);
                this.funder.setAbbreviatedNameLayoutTokens(funder.getFullNameLayoutTokens().subList(acronymPosition.start, acronymPosition.end));

                this.funder.setFullName(LayoutTokensUtil.toText(funder.getFullNameLayoutTokens().subList(basePosition.start, basePosition.end)));
                this.funder.setFullNameLayoutTokens(funder.getFullNameLayoutTokens().subList(basePosition.start, basePosition.end));
            }
        }

        // check the grant number prefix
        if (funder.getAbbreviatedName() == null && grantNumber != null) {
            for (Map.Entry<String,String> entry : Funder.prefixFounders.entrySet()) {
                if (grantNumber.startsWith(entry.getKey()+"-")) {
                    this.funder.setAbbreviatedName(entry.getKey());
                    this.funder.setAbbreviatedNameLayoutTokens(null);
                    this.funder.setFullName(entry.getValue());
                    this.funder.setFullNameLayoutTokens(null);
                    break;
                }
            }
        }

        // check if full name is an acronym
        if (funder.getAbbreviatedName() == null && funder.getFullName() != null) {
            for (Map.Entry<String,String> entry : Funder.prefixFounders.entrySet()) {
                if (funder.getFullName().equals(entry.getKey())) {
                    this.funder.setAbbreviatedName(entry.getKey());
                    this.funder.setAbbreviatedNameLayoutTokens(this.funder.getFullNameLayoutTokens());
                    this.funder.setFullName(entry.getValue());
                    this.funder.setFullNameLayoutTokens(null);
                    break;
                }
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (funder != null) {
            sb.append("funder: ").append(funder.toString()).append("\n");
        }
        if (grantName != null) {
            sb.append("grant name: ").append(grantName).append("\n");
        }
        if (grantNumber != null) {
            sb.append("grant number: ").append(grantNumber).append("\n");
        }
        if (projectFullName != null) {
            sb.append("project name: ").append(projectFullName).append("\n");
        }
        if (projectAbbreviatedName != null) {
            sb.append("project abbreviated name: ").append(projectAbbreviatedName).append("\n");
        }
        if (programFullName != null) {
            sb.append("program name: ").append(programFullName).append("\n");
        }
        if (programAbbreviatedName != null) {
            sb.append("program abbreviated name: ").append(programAbbreviatedName).append("\n");
        }
        if (url != null) {
            sb.append("url: ").append(url).append("\n");
        }
        return sb.toString();
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        boolean start = false;
        json.append("{\n");
        if (funder != null) {
            json.append(funder.toJson());
            start = true;
        }
        if (grantNumber != null) {
            if (start)
                json.append(",\n");
            json.append("\"grantNumber\": \"");
            json.append(grantNumber+ "\"");
            start = true;
        }
        // to be completed...

        json.append("\n}");
        return json.toString();
    }

    public String toTEI() {
        return toTEI(0);
    }

    public String toTEI(int nbIndent) {
        StringBuilder tei = new StringBuilder();

        String localType = "funding";
        if (projectFullName != null || projectAbbreviatedName != null)
            localType = "funded-project";

        if (this.identifier == null) {
            String localId = KeyGen.getKey().substring(0, 7);
            this.identifier = "_" + localId;
        }

        for(int i=0; i<nbIndent; i++)
            tei.append("\t");
        tei.append("<org type=\""+localType+"\" xml:id=\""+this.identifier+"\">\n");

        if (grantNumber != null) {
            for(int i=0; i<nbIndent+1; i++)
                tei.append("\t");
            tei.append("<idno type=\"grant-number\">"+TextUtilities.HTMLEncode(grantNumber)+"</idno>\n");
        }

        if (grantName != null) {
            for(int i=0; i<nbIndent+1; i++)
                tei.append("\t");
            tei.append("<orgName type=\"grant-name\">"+TextUtilities.HTMLEncode(grantName)+"</orgName>\n");
        }

        if (projectFullName != null) {
            for(int i=0; i<nbIndent+1; i++)
                tei.append("\t");
            tei.append("<orgName type=\"project\" subtype=\"full\">"+TextUtilities.HTMLEncode(projectFullName)+"</orgName>\n");
        }
        if (projectAbbreviatedName != null) {
            for(int i=0; i<nbIndent+1; i++)
                tei.append("\t");
            tei.append("<orgName type=\"project\" subtype=\"abbreviated\">"+TextUtilities.HTMLEncode(projectAbbreviatedName)+"</orgName>\n");
        }
        if (programFullName != null) {
            for(int i=0; i<nbIndent+1; i++)
                tei.append("\t");
            tei.append("<orgName type=\"program\" subtype=\"full\">"+TextUtilities.HTMLEncode(programFullName)+"</orgName>\n");
        }
        if (programAbbreviatedName != null) {
            for(int i=0; i<nbIndent+1; i++)
                tei.append("\t");
            tei.append("<orgName type=\"program\" subtype=\"abbreviated\">"+TextUtilities.HTMLEncode(programAbbreviatedName)+"</orgName>\n");
        }
        if (url != null) {
            for(int i=0; i<nbIndent+1; i++)
                tei.append("\t");
            tei.append("<ptr target=\"").append(TextUtilities.HTMLEncode(url)).append("\" />\n");
        }
        if (start != null) {
            String dateString = start.toTEI();
            dateString = dateString.replace("<date ", "<date type=\"start\" ");
            for(int i=0; i<nbIndent+1; i++)
                tei.append("\t");
            tei.append(dateString);
        }
        if (end != null) {
            String dateString = end.toTEI();
            dateString = dateString.replace("<date ", "<date type=\"end\" ");
            for(int i=0; i<nbIndent+1; i++)
                tei.append("\t");
            tei.append(dateString);
        }

        for(int i=0; i<nbIndent; i++)
            tei.append("\t");
        tei.append("</org>\n");

        return tei.toString();
    }
}
