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

/**
 * Class for managing passage of citations.
 *
 */
public class Passage {
    private int pageBegin = -1;
    private int pageEnd = -1;
    private int lineBegin = -1;
    private int lineEnd = -1;

    private String colBegin = null;
    private String colEnd = null;

    private String figure = null;
    private String table = null;

    private String rawPassage = null;

    public int getPageBegin() {
        return pageBegin;
    }

    public int getPageEnd() {
        return pageEnd;
    }

    public int getLineBegin() {
        return lineBegin;
    }

    public int getLineEnd() {
        return lineEnd;
    }

    public String getColBegin() {
        return colBegin;
    }

    public String getColEnd() {
        return colEnd;
    }

    public String getFigure() {
        return figure;
    }

    public String getTable() {
        return table;
    }

    public String getRawPassage() {
        return rawPassage;
    }

    public void setRawPassage(String s) {
        rawPassage = s;
    }
}
