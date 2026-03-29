package org.grobid.service.util;

public class BibTexMediaType {
    public static final String MEDIA_TYPE = "application/x-bibtex";

    // Lower server-side quality so XML is preferred when no Accept header is sent
    public static final String MEDIA_TYPE_QS = MEDIA_TYPE + ";qs=0.5";
}
