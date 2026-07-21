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
package org.grobid.core.layout;

/**
 * Represents a typed area in a PDF document for specialized processing.
 * This includes areas containing figures, tables, or content to be ignored.
 */
public class TypedArea {
    private int page;           // page number (1-based, following PDF convention)
    private double x;           // x-coordinate of upper-left corner
    private double y;           // y-coordinate of upper-left corner
    private double width;       // width of the area
    private double height;      // height of the area
    private AreaType type;      // type: figure, table, ignore, paratext

    public TypedArea() {
    }

    public TypedArea(int page, double x, double y, double width, double height, AreaType type) {
        this.page = page;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
    }

    /**
     * Legacy constructor for backward compatibility.
     * @deprecated Use {@link #TypedArea(int, double, double, double, double, AreaType)} instead.
     */
    @Deprecated
    public TypedArea(int page, double x, double y, double width, double height, String name) {
        this.page = page;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        // Convert string name to AreaType for backward compatibility
        this.type = name != null ? AreaType.fromString(name.toLowerCase()) : AreaType.PARATEXT;
    }

    /**
     * Creates an TypedArea from a coordinate string in the format: "page,x,y,width,height,type"
     */
    public static TypedArea fromCoordinates(String coordString) {
        String[] parts = coordString.split(",");
        if (parts.length < 6) {
            throw new IllegalArgumentException(
                    "Invalid coordinate string format. Expected: page,x,y,width,height,type");
        }

        try {
            int page = Integer.parseInt(parts[0].trim());
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double width = Double.parseDouble(parts[3].trim());
            double height = Double.parseDouble(parts[4].trim());
            AreaType type = AreaType.fromString(parts[5].trim());

            return new TypedArea(page, x, y, width, height, type);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric values in coordinate string: " + coordString, e);
        }
    }

    /**
     * Checks if a LayoutToken falls within or intersects with this typed area.
     *
     * @param token the LayoutToken to check
     * @return true if the token intersects with this typed area
     */
    public boolean contains(LayoutToken token) {
        if (token.getPage() != this.page) {
            return false;
        }

        double tokenLeft = token.getX();
        double tokenRight = token.getX() + token.getWidth();
        double tokenTop = token.getY();
        double tokenBottom = token.getY() + token.getHeight();

        double areaLeft = this.x;
        double areaRight = this.x + this.width;
        double areaTop = this.y;
        double areaBottom = this.y + this.height;

        // Check for intersection: two rectangles intersect if their projections overlap on both axes
        return !(tokenRight < areaLeft ||
                tokenLeft > areaRight ||
                tokenBottom < areaTop ||
                tokenTop > areaBottom);
    }

    /**
     * Checks if a bounding box (e.g. a graphic object) intersects with this typed area.
     *
     * @param box the bounding box to check
     * @return true if the box is on the same page and intersects this typed area
     */
    public boolean intersects(BoundingBox box) {
        if (box == null || box.getPage() != this.page) {
            return false;
        }

        double areaRight = this.x + this.width;
        double areaBottom = this.y + this.height;

        return !(box.getX2() < this.x ||
                box.getX() > areaRight ||
                box.getY2() < this.y ||
                box.getY() > areaBottom);
    }

    /**
     * Creates an TypedArea from a coordinate string in the format: "page,x,y,width,height,name"
     */
    public static TypedArea fromString(String coordString) {
        String[] parts = coordString.split(",");
        if (parts.length < 5) {
            throw new IllegalArgumentException(
                    "Invalid coordinate string format. Expected: page,x,y,width,height[,name]");
        }

        try {
            int page = Integer.parseInt(parts[0].trim());
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double width = Double.parseDouble(parts[3].trim());
            double height = Double.parseDouble(parts[4].trim());
            String name = parts.length > 5 ? parts[5].trim() : "";

            return new TypedArea(page, x, y, width, height, name);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric values in coordinate string: " + coordString, e);
        }
    }

    // Getters and setters
    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public AreaType getType() {
        return type;
    }

    public void setType(AreaType type) {
        this.type = type;
    }

    /**
     * Legacy getter for backward compatibility.
     * @deprecated Use {@link #getType()} instead.
     */
    @Deprecated
    public String getName() {
        return type != null ? type.getValue() : null;
    }

    /**
     * Legacy setter for backward compatibility.
     * @deprecated Use {@link #setType(AreaType)} instead.
     */
    @Deprecated
    public void setName(String name) {
        this.type = name != null ? AreaType.fromString(name.toLowerCase()) : AreaType.PARATEXT;
    }

    @Override
    public String toString() {
        return String.format(
                "TypedArea{page=%d, x=%.2f, y=%.2f, width=%.2f, height=%.2f, type='%s'}",
                page,
                x,
                y,
                width,
                height,
                type != null ? type.getValue() : "null");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        TypedArea that = (TypedArea) obj;
        return page == that.page &&
                Double.compare(that.x, x) == 0 &&
                Double.compare(that.y, y) == 0 &&
                Double.compare(that.width, width) == 0 &&
                Double.compare(that.height, height) == 0 &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = page;
        temp = Double.doubleToLongBits(x);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(width);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(height);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
