package org.grobid.core.layout;

/**
 * Enumeration of supported area types for typed area processing.
 */
public enum AreaType {
    FIGURE("figure"),
    TABLE("table"),
    IGNORE("ignore"),
    PARATEXT("paratext");

    private final String value;

    AreaType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AreaType fromString(String value) {
        for (AreaType type : AreaType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown area type: " + value);
    }
}
