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

import java.util.*;

/**
 * Class for representing a classification.
 *
 */
public class Classification {
    private String classificationScheme = null;

    private List<String> classes = null;
    private String rawString = null;

    public String getClassificationScheme() {
        return classificationScheme;
    }

    public void setClassificationScheme(String s) {
        classificationScheme = s;
    }

    public List<String> getClasses() {
        return classes;
    }

    public void setClasses(List<String> c) {
        classes = c;
    }

    public String getRawString() {
        return rawString;
    }

    public void setRawString(String s) {
        rawString = s;
    }
}
