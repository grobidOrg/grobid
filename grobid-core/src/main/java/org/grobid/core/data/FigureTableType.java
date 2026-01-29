package org.grobid.core.data;

public enum FigureTableType {
    FIGURE("figure"), TABLE("table");

    private final String value;

    FigureTableType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
