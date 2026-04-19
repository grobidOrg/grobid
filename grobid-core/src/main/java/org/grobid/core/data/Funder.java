package org.grobid.core.data;

import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lang.Language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

/**
 * Class for representing a funding organization.
 * Optionally the funder is identified by its DOI at CrossRef funder registry.
 */

public class Funder {
    // prefered full name
    private String fullName = null;
    private List<LayoutToken> fullNameLayoutTokens = new ArrayList<>();

    // full names by languages
    private Map<Language, List<String>> fullNameByLanguage = new HashMap<>();

    private String abbreviatedName = null;
    private List<LayoutToken> abbreviatedNameLayoutTokens = new ArrayList<>();

    // abbreviated names by languages
    private Map<Language, List<String>> abbreviatedNameByLanguage = new HashMap<>();

    private String doi = null;

    // country or regional area (e.g. EU)
    private String country = null;
    private String countryCode = null;
    private String address = null;
    private String region = null;

    private Date startActiveDate = null;
    private Date endActiveDate = null;
    private Funder preceededBy = null;
    private Funder followedBy = null;

    private String url = null;

    private String crossrefFunderType = null;

    private List<LayoutToken> layoutTokens = new ArrayList<>();

    static public Funder EMPTY = new Funder("unknown");

    public static Map<String, String> prefixFounders;
    static {
        prefixFounders = new TreeMap<>();
        prefixFounders.put("ANR", "Agence Nationale de la Recherche");
        prefixFounders.put("NSF", "National Science Foundation");
        prefixFounders.put("NIH", "National Institutes of Health");
        prefixFounders.put("ERC", "European Research Council");
        //Japanese government
        prefixFounders.put("MEXT", "Ministry of Education, Culture, Sports, Science and Technology");
    }

    public Funder() {
    }

    public Funder(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return this.fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setFullNameLayoutTokens(List<LayoutToken> layoutTokens) {
        this.fullNameLayoutTokens = layoutTokens;
    }

    public void appendFullNameLayoutTokens(List<LayoutToken> layoutTokens) {
        this.fullNameLayoutTokens.addAll(layoutTokens);
    }

    public List<LayoutToken> getFullNameLayoutTokens() {
        return this.fullNameLayoutTokens;
    }

    public String getAbbreviatedName() {
        return this.abbreviatedName;
    }

    public void setAbbreviatedName(String abbreviatedName) {
        this.abbreviatedName = abbreviatedName;
    }

    public void setAbbreviatedNameLayoutTokens(List<LayoutToken> layoutTokens) {
        this.abbreviatedNameLayoutTokens = layoutTokens;
    }

    public void appendAbbreviatedNameLayoutTokens(List<LayoutToken> layoutTokens) {
        this.abbreviatedNameLayoutTokens.addAll(layoutTokens);
    }

    public List<LayoutToken> getAbbreviatedNameLayoutTokens() {
        return this.abbreviatedNameLayoutTokens;
    }

    public String getDoi() {
        return this.doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getCountry() {
        return this.country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Date getStartActiveDate() {
        return this.startActiveDate;
    }

    public void setStartActiveDate(Date startActiveDate) {
        this.startActiveDate = startActiveDate;
    }

    public Date getEndActiveDate() {
        return this.endActiveDate;
    }

    public void setEndActiveDate(Date endActiveDate) {
        this.endActiveDate = endActiveDate;
    }

    public Funder getPreceededBy() {
        return this.preceededBy;
    }

    public void setPreceededBy(Funder preceededBy) {
        this.preceededBy = preceededBy;
    }

    public Funder getFollowedBy() {
        return this.followedBy;
    }

    public void setFollowedBy(Funder followedBy) {
        this.followedBy = followedBy;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (fullName != null) {
            sb.append(fullName);
        }
        if (abbreviatedName != null) {
            sb.append(abbreviatedName);
        }
        return sb.toString();
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        boolean start = false;
        json.append("{\n");
        if (fullName != null) {
            json.append("\t\"fullName\": \"");
            json.append(this.fullName + "\"");
            start = true;
        }
        if (abbreviatedName != null) {
            if (start)
                json.append(",\n");
            json.append("\t\"abbreviatedName\": \"");
            json.append(this.abbreviatedName + "\"");
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

        for(int i=0; i<nbIndent; i++)
            tei.append("\t");
        tei.append("<funder>\n");

        if (fullName != null) {
            for(int i=0; i<nbIndent+1; i++)
                tei.append("\t");
            tei.append("<orgName type=\"full\">"+TextUtilities.HTMLEncode(fullName)+"</orgName>\n");
        }
        if (abbreviatedName != null) {
            for(int i=0; i<nbIndent+1; i++)
                tei.append("\t");
            tei.append("<orgName type=\"abbreviated\">"+TextUtilities.HTMLEncode(abbreviatedName)+"</orgName>\n");
        }
        if (doi != null) {
            for(int i=0; i<nbIndent+1; i++)
                tei.append("\t");
            tei.append("<idno type=\"DOI\" subtype=\"crossref\">"+TextUtilities.HTMLEncode(doi)+"</idno>\n");
        }

        for(int i=0; i<nbIndent; i++)
            tei.append("\t");
        tei.append("</funder>\n");

        return tei.toString();
    }
}
