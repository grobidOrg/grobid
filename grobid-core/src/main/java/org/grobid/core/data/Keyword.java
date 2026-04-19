package org.grobid.core.data;

import java.util.ArrayList;
import java.util.List;

import org.grobid.core.utilities.TextUtilities;

/**
 * Class for representing a keyword extracted from a publication.
 *
 */
public class Keyword {
    private String keyword = null;
    private String type = null;

 	public Keyword(String key) {
 		keyword = key;
 	}

 	public Keyword(String key, String typ) {
 		keyword = key;
		type = typ;
 	}

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String key) {
        keyword = key;
    }

    public String getType() {
        return type;
    }

    public void setType(String typ) {
        type = typ;
    }

    public boolean notNull() {
        if (keyword == null)
            return false;
        else
            return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (keyword != null) {
            sb.append(keyword).append(" ");
        }
        if (type != null) {
            sb.append(" (type:").append(type).append(")");
        }
        return sb.toString().trim();
    }

    public String toTEI() {
        if (keyword == null) {
            return null;
        }
        String res = "<term>" + TextUtilities.HTMLEncode(keyword) + "</term>";
        return res;
    }

}
