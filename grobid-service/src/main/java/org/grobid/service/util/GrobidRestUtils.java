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
package org.grobid.service.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrobidRestUtils {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(GrobidRestUtils.class);

    // type of PDF annotation for visualization purposes
    public enum Annotation {
        CITATION, BLOCK, FIGURE
    }

    /**
     * Check whether the result is null or empty.
     */
    public static boolean isResultNullOrEmpty(String result) {
        return StringUtils.isBlank(result);
    }

    public static Annotation getAnnotationFor(int type) {
        GrobidRestUtils.Annotation annotType = null;
        if (type == 0)
            annotType = GrobidRestUtils.Annotation.CITATION;
        else if (type == 1)
            annotType = GrobidRestUtils.Annotation.BLOCK;
        else if (type == 2)
            annotType = GrobidRestUtils.Annotation.FIGURE;

        return annotType;
    }

}
