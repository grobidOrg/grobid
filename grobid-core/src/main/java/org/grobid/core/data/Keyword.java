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
package org.grobid.core.data;

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
