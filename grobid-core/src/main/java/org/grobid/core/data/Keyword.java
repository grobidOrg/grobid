package org.grobid.core.data;

import org.grobid.core.utilities.TextUtilities;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append("keyword", keyword)
                .append("type", type)
                .toString();
    }

    public String toTEI() {
        if (keyword == null) {
            return null;
        }
        String res = "<term>" + TextUtilities.HTMLEncode(keyword) + "</term>";
        return res;
    }

}
