package org.grobid.service.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrobidRestUtils {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(GrobidRestUtils.class);

    // type of PDF annotation for visualization purposes
    public enum Annotation {
        CITATION, BLOCK, TABLE, FIGURE
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
        else if (type == 3)
            annotType = GrobidRestUtils.Annotation.TABLE;

        return annotType;
    }

}
