package org.grobid.core.data;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.utilities.TextUtilities;

/**
 * Class for managing patent bibliographical references.
 *
 */
public class PatentItem implements Comparable<PatentItem> {
    // attribute
    private String authority = null;
    private String number_epodoc = null;
	private String number_wysiwyg = null;
    private String kindCode = null;

    // patent type when applicable
    private Boolean application = false;
    private Boolean provisional = false;
    private Boolean reissued = false;
    private Boolean plant = false;
    private Boolean design = false;
	private Boolean utility = false;
	
    // scores
    private double conf = 1.0;
    //private String confidence = null;

    // position in document
    private int offset_begin = 0;
    private int offset_end = 0;

    // position in raw string (in case of factorised numbers)
    private int offset_raw = 0;

    // context of occurrence of the reference
    private String context = null;

	// coordinates in the orignal layout (for PDF)
	private List<BoundingBox> coordinates = null;

    public String getAuthority() {
        return authority;
    }

    public String getNumberEpoDoc() {
        return number_epodoc;
    }

    public String getNumberWysiwyg() {
        return number_wysiwyg;
    }

    public String getKindCode() {
        return kindCode;
    }

    public Boolean getApplication() {
        return application;
    }

    public Boolean getProvisional() {
        return provisional;
    }

    public Boolean getReissued() {
        return reissued;
    }

    public Boolean getPlant() {
        return plant;
    }

    public Boolean getDesign() {
        return design;
    }

	public Boolean getUtility() {
        return design;
    }

    public double getConf() {
        return conf;
    }

    /*public String getConfidence() {
        return confidence;
    }*/

    public int getOffsetBegin() {
        return offset_begin;
    }

    public int getOffsetEnd() {
        return offset_end;
    }

    public int getOffsetRaw() {
        return offset_raw;
    }

    /**
     * Context of occurrence of the reference
     */
    public String getContext() {
        return context;
    }

    public void setContext(String cont) {
        context = cont;
    }

    public void setOffsetBegin(int ofs) {
        offset_begin = ofs;
    }

    public void setOffsetEnd(int ofs) {
        offset_end = ofs;
    }

    public void setOffsetRaw(int ofs) {
        offset_raw = ofs;
    }

    public void setKindCode(String kc) {
        kindCode = kc;
    }

    public void setNumberEpoDoc(String num) {
        number_epodoc = num;
    }

    public void setNumberWysiwyg(String num) {
        number_wysiwyg = num;
    }

    public void setAuthority(String s) {
        authority = s;
    }

    public void setApplication(boolean b) {
        application = b;
    }

    public void setProvisional(boolean b) {
        provisional = b;
    }

    public void setReissued(boolean b) {
        reissued = b;
    }

    public void setPlant(boolean b) {
        plant = b;
    }

    public void setDesign(boolean b) {
        design = b;
    }

	public void setUtility(boolean b) {
        utility = b;
    }

	public void setConf(double val) {
		conf = val;
	}

    public int compareTo(PatentItem another) {
        return number_wysiwyg.compareTo(another.getNumberWysiwyg());
    }

    private final static String espacenet = "http://v3.espacenet.com/publicationDetails/biblio?DB=EPODOC";
    private final static String espacenet2 = "http://v3.espacenet.com/searchResults?DB=EPODOC";

    private final static String epoline = "https://register.epoline.org/espacenet/application?number=";

    private final static String epoline2 = "https://register.epoline.org/espacenet/simpleSearch?index[0]=publication&value[0]=";
    private final static String epoline3 = "&index[1]=&value[1]=&index[2]=&value[2]=&searchMode=simple&recent=";

    public String getEspacenetURL() {
        String res = null;
        if (provisional) {
            res = espacenet2 + "&PR=" + authority + number_epodoc + "P";
        } else if (application) {
            res = espacenet2 + "&AP=" + authority + number_epodoc;
        } else {
            res = espacenet + "&CC=" + authority + "&NR=" + number_epodoc;
        }
        return res;
    }

    public String getEpolineURL() {
        String res = null;
        if (application) {
            res = epoline + authority + number_epodoc;
        } else {
            // we need the application number corresponding to the publication
            res = epoline2 + authority + number_epodoc + epoline3;
        }
        return res;
    }

    public String getType() {
        if (application)
            return "application";
        if (provisional)
            return "provisional";
        if (reissued)
            return "reissued";
        if (plant)
            return "plant";
        if (design)
            return "design";
        // default
        return "publication";
    }

    public void setType(String type) {
        if (type.equals("publication"))
            return;
        if (type.equals("application")) {
            application = true;
        } else if (type.equals("provisional")) {
            provisional = true;
        } else if (type.equals("reissued")) {
            reissued = true;
        } else if (type.equals("plant")) {
            plant = true;
        } else if (type.equals("design")) {
            design = true;
        }
    }
	
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("authority", authority)
                .append("number_wysiwyg", number_wysiwyg)
                .append("number_epodoc", number_epodoc)
                .append("kindCode", kindCode)
                .append("application", application)
                .append("provisional", provisional)
                .append("reissued", reissued)
                .append("plant", plant)
                .append("design", design)
                .append("conf", conf)
                .append("offset_begin", offset_begin)
                .append("offset_end", offset_end)
                .append("offset_raw", offset_raw)
                .append("context", context)
                .toString();
    }
    
	public String toTEI() {
		return toTEI(null, false, null);
	}
	
	public String toTEI(boolean withPtr, String ptrVal) {
		return toTEI(null, withPtr, ptrVal);
	}
	
	public String toTEI(String date) {
		return toTEI(date, false, null);
	}
	
	public String toTEI(String date, boolean withPtr, String ptrVal) {
		/* TEI for patent bilbiographical data is as follow (After the TEI guideline update of October 2012):
		<biblStruct type="patent¦utilityModel¦designPatent¦plant" status="application¦publication">
		<monogr>
		<authority>
		<orgName type="national¦regional">[name of patent office]<orgName> (mandatory)
		</authority>
		<idno type="docNumber">[patent document number]</idno> (mandatory)
		<imprint> (optional)
		<classCode scheme="kindCode">[kind code]</classCode> (optional)
		<date>[date]</date> (optional)
		</imprint>
		</monogr>
		</biblStruct>
		*/
		StringBuffer biblStruct = new StringBuffer();
		
		// type of patent
		biblStruct.append("<biblStruct type=\"");
		if (design) {
			biblStruct.append("designPatent");
		}
		else if (plant) {
			biblStruct.append("plant");
		}
		else if (utility) {
			biblStruct.append("utilityModel");
		}
		else {
			biblStruct.append("patent");
		}
		
		// status
		biblStruct.append("\" status=\"");				
		if (application) {
			biblStruct.append("application");
		}
		else if (provisional) {
			biblStruct.append("provisional");
		}
		else if (reissued) {
			biblStruct.append("reissued");
		}
		else {
			biblStruct.append("publication");
		}
		biblStruct.append("\">");
		
		biblStruct.append("<monogr><authority><orgName type=\"");
		if (authority.equals("EP") || authority.equals("WO") || authority.equals("XN") 
			|| authority.equals("XN") || authority.equals("GC") || authority.equals("EA") ) { 
			// XN is the Nordic Patent Institute
			// OA is the African Intellectual Property Organization (OAPI)
			// GC is the Gulf Cooperation Council 
			// EA Eurasian Patent Organization
			biblStruct.append("regional");
		}
		else {
			biblStruct.append("national");
		}
		biblStruct.append("\">"+TextUtilities.HTMLEncode(authority)+"</orgName></authority>");
		biblStruct.append("<idno type=\"docNumber\" subtype=\"epodoc\">"+TextUtilities.HTMLEncode(number_epodoc)+"</idno>");
		biblStruct.append("<idno type=\"docNumber\" subtype=\"original\">"+TextUtilities.HTMLEncode(number_wysiwyg)+"</idno>");
		
		if ((kindCode != null) || (date != null)) {
			biblStruct.append("<imprint>");
			if (kindCode != null) {
				biblStruct.append("<classCode scheme=\"kindCode\">"+TextUtilities.HTMLEncode(kindCode)+"</classCode>");
			}
			if (date != null) {
				biblStruct.append("<date>"+TextUtilities.HTMLEncode(date)+"</date>");
			}
			biblStruct.append("</imprint>");
		}
		
		if (withPtr) {
			biblStruct.append("<ptr target=\"#string-range('" + ptrVal + "',"+
				offset_begin +","+ 
				(offset_end - offset_begin + 1) +")\"></ptr>");
		}
		
		if (conf != 0.0) {
			biblStruct.append("<certainty degree=\"" + conf +"\" />");
		}
		biblStruct.append("</monogr>");

		biblStruct.append("</biblStruct>");
		
		return biblStruct.toString();
	}
	
	public String toJson(String date, boolean withCoordinates) {
		StringBuilder json = new StringBuilder();
		json.append("{");
		json.append("\"type\": ");
		if (design) {
			json.append("\"designPatent\"");
		}
		else if (plant) {
			json.append("\"plant\"");
		}
		else if (utility) {
			json.append("\"utilityModel\"");
		}
		else {
			json.append("\"patent\"");
		}
		
		json.append(", \"status\": ");				
		if (application) {
			json.append("\"application\"");
		}
		else if (provisional) {
			json.append("\"provisional\"");
		}
		else if (reissued) {
			json.append("\"reissued\"");
		}
		else {
			json.append("\"publication\"");
		}
		
		json.append(", \"authority\": { \"name\": \"").append(authority).append("\", \"type\": \"");
		if (authority.equals("EP") || authority.equals("WO") || authority.equals("XN") 
			|| authority.equals("XN") || authority.equals("GC") || authority.equals("EA") ) { 
			// XN is the Nordic Patent Institute
			// OA is the African Intellectual Property Organization (OAPI)
			// GC is the Gulf Cooperation Council 
			// EA Eurasian Patent Organization
			json.append("regional");
		}
		else {
			json.append("national");
		}
		json.append("\"}");
		
		json.append(", \"number\": {"); 
		if (number_wysiwyg != null) {
			json.append("\"original\" : \"").append(number_wysiwyg).append("\"");
			if (number_epodoc != null)
				json.append(", ");
		}
		if (number_epodoc != null)
			json.append("\"epodoc\" : \"").append(number_epodoc).append("\"");
		json.append("}"); 

		if (kindCode != null) {
			json.append(", \"kindCode\" : \"").append(kindCode).append("\"");
		}

		if (date != null) {
			json.append(", \"date\" : \"").append(date).append("\"");
		}

		if ( withCoordinates && (coordinates != null) && (coordinates.size() > 0) ) {
			json.append(", \"pos\": [");
			boolean first = true;
			for (BoundingBox b : coordinates) {
				if (first)
					first = false;
				else
					json.append(",");
				json.append("{").append(b.toJson()).append("}");
			}
			json.append("]");
		}

		if ( (offset_begin != -1) && (offset_end != -1)) {
			json.append(", \"offset\": {");
			json.append("\"begin\" : ").append(offset_begin).append(", ");
			json.append("\"end\" : ").append(offset_end);
			json.append("}");
		}
		
		String url1 = getEspacenetURL();
		String url2 = null;
		
		if (authority.equals("EP"))
			url2= getEpolineURL();

		if (  (url1 != null) || (url2 != null) ) {
			json.append(", \"url\": {");
			if (url1 != null) {
				json.append("\"espacenet\" : \"").append(url1).append("\"");
				if (url2 != null)
					json.append(", ");
			}
			if (url2 != null)
				json.append("\"epoline\" : \"").append(url2).append("\"");
			json.append("}");
		}

		json.append("}");
		return json.toString();
	}
 
    public void setCoordinates(List<BoundingBox> coordinates) {
        this.coordinates = coordinates;
    }

    public List<BoundingBox> getCoordinates() {
        return coordinates;
    }
}