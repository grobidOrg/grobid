package org.grobid.core.layout;

import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Represents a bounding box to identify area in the original PDF
 */
public class BoundingBox implements Comparable {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoundingBox.class);
    private int page;
    private double x, y, width, height;

    private double x2, y2;

    private BoundingBox(int page, double x, double y, double width, double height) {
        this.page = page;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.x2 = x + width;
        this.y2 = y + height;
    }

    public static BoundingBox fromTwoPoints(int page, double x1, double y1, double x2, double y2) {
        if (x1 > x2 || y1 > y2) {
            throw new IllegalArgumentException("Invalid points provided: (" + x1 + ";" + y1 + ")-(" + x2 + ";" + y2 + ")");
        }
        return new BoundingBox(page, x1, y1, x2 - x1, y2 - y1);
    }

    public static BoundingBox fromString(String coords) {
        try {
            String[] split = coords.split(",");

            Long pageNum = Long.valueOf(split[0], 10);

            float x = Float.parseFloat(split[1]);
            float y = Float.parseFloat(split[2]);
            float w = Float.parseFloat(split[3]);
            float h = Float.parseFloat(split[4]);

            return new BoundingBox(pageNum.intValue(), x, y, w, h);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static BoundingBox fromPointAndDimensions(int page, double x, double y, double width, double height) {
        return new BoundingBox(page, x, y, width, height);
    }

    public static BoundingBox fromLayoutToken(LayoutToken tok) {
        return BoundingBox.fromPointAndDimensions(tok.getPage(), tok.getX(), tok.getY(), tok.getWidth(), tok.getHeight());
    }

    public boolean intersect(BoundingBox b) {
        double ax1 = this.x;
        double ax2 = this.x2;
        double ay1 = this.y;
        double ay2 = this.y2;

        double bx1 = b.x;
        double bx2 = b.x2;
        double by1 = b.y;
        double by2 = b.y2;


        if (ax2 < bx1) return false;
        else if (ax1 > bx2) return false;
        else if (ay2 < by1) return false;
        else if (ay1 > by2) return false;
        else
            return true;
    }

    public int getPage() {
        return page;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getX2() {
        return x2;
    }

    public double getY2() {
        return y2;
    }

    public BoundingBox boundBox(BoundingBox o) {
        if (this.page != o.page) {
            throw new IllegalStateException("Cannot compute a bounding box for different pages");
        }
        return fromTwoPoints(o.page, Math.min(this.x, o.x), Math.min(this.y, o.y), Math.max(this.x2, o.x2), Math.max(this.y2, o.y2));
    }

    public BoundingBox boundBoxExcludingAnotherPage(BoundingBox o) {
        if (this.page != o.page) {
            LOGGER.debug("Cannot compute a bounding box for different pages: " + this + " and " + o + "; skipping");
            return this;
        }
        return fromTwoPoints(o.page, Math.min(this.x, o.x), Math.min(this.y, o.y), Math.max(this.x2, o.x2), Math.max(this.y2, o.y2));
    }


    public boolean contains(BoundingBox b) {
        return x <= b.x && y <= b.y && x2 >= b.x2 && y2 >= b.y2;
    }

    private double dist(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x2 - x1) * (x2 - 1) + (y2 - y1) * (y2 - y1));
    }

    public double verticalDistanceTo(BoundingBox to) {
        //the current box is completely "bottomer"
        boolean bottom = to.y2 < y;
        boolean top = y2 < to.y;

        if (bottom) {
            return y - to.y2;
        } else if (top) {
            return to.y - y2;
        }

        return 0;
    }

    public double area() {
        return width * height;
    }

    public double distanceTo(BoundingBox to) {
        if (this.page != to.page) {
            return 1000 * Math.abs(this.page - to.page);
        }

        //the current box is completely "lefter"

        boolean left = x2 < to.x;
        boolean right = to.x2 < x;
        boolean bottom = to.y2 < y;
        boolean top = y2 < to.y;
        if (top && left) {
            return dist(x2, y2, to.x, y);
        } else if (left && bottom) {
            return dist(x2, y, to.x, to.y2);
        } else if (bottom && right) {
            return dist(x, y, to.x2, to.y2);
        } else if (right && top) {
            return dist(x, y2, to.x2, to.y);
        } else if (left) {
            return to.x - x2;
        } else if (right) {
            return x - to.x2;
        } else if (bottom) {
            return y - to.y2;
        } else if (top) {
            return to.y - y2;
        } else {
            return 0;
        }
    }

    public BoundingBox boundingBoxIntersection(BoundingBox b) {
        if (!this.intersect(b))
            return null;

        double ax1 = this.x;
        double ax2 = this.x2;
        double ay1 = this.y;
        double ay2 = this.y2;

        double bx1 = b.x;
        double bx2 = b.x2;
        double by1 = b.y;
        double by2 = b.y2;

        double ix1 = 0.0;
        if (ax1 > bx1)
            ix1 = ax1;
        else
            ix1 = bx1;

        double iy1 = 0.0;
        if (ay1 > by1)
            iy1 = ay1;
        else
            iy1 = by1;

        double ix2 = 0.0;
        if (ax2 > bx2)
            ix2 = bx2;
        else
            ix2 = ax2;

        double iy2 = 0.0;
        if (ay2 > by2)
            iy2 = by2;
        else
            iy2 = ay2;

        return fromTwoPoints(page, ix1, iy1, ix2, iy2);
    }

    @Override
    public String toString() {
        return String.format("%d,%.2f,%.2f,%.2f,%.2f", page, x, y, width, height);
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("\"p\":").append(page).append(", ");
        builder.append("\"x\":").append(x).append(", ");
        builder.append("\"y\":").append(y).append(", ");
        builder.append("\"w\":").append(width).append(", ");
        builder.append("\"h\":").append(height);
        return builder.toString();
    }

    public void writeJsonProps(JsonGenerator gen) throws IOException {
        gen.writeNumberField("p", page);
        gen.writeNumberField("x", x);
        gen.writeNumberField("y", y);
        gen.writeNumberField("w", width);
        gen.writeNumberField("h", height);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoundingBox)) return false;

        BoundingBox that = (BoundingBox) o;

        if (getPage() != that.getPage()) return false;
        if (Double.compare(that.getX(), getX()) != 0) return false;
        if (Double.compare(that.getY(), getY()) != 0) return false;
        if (Double.compare(that.getWidth(), getWidth()) != 0) return false;
        return Double.compare(that.getHeight(), getHeight()) == 0;
    }

    @Override
    public int compareTo(Object otherBox) {
        if (this.equals(otherBox))
            return 0;

        if (!(otherBox instanceof BoundingBox))
            return -1;

        BoundingBox that = (BoundingBox) otherBox;

        // the rest of position comparison is using the barycenter of the boxes
        double thisCenterX = x + (width / 2);
        double thisCenterY = y + (height / 2);
        double otherCenterX = that.x + (that.width / 2);
        double otherCenterY = that.y + (that.height / 2);
        if (Double.compare(thisCenterY, otherCenterY) == 0)
            return Double.compare(thisCenterX, otherCenterX);
        else
            return Double.compare(thisCenterY, otherCenterY);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = getPage();
        temp = Double.doubleToLongBits(getX());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(getY());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(getWidth());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(getHeight());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    /**
     * Calculates the Intersection over Union (IoU) between this bounding box and another.
     *
     * @param other The other bounding box to compare with
     * @return The IoU ratio (0.0 if no intersection, approaching 1.0 as overlap increases)
     */
    public double calculateOverlapRatio(BoundingBox other) {
        // Check if boxes are on different pages or don't intersect
        if (this.page != other.page || !this.intersect(other)) {
            return 0.0;
        }

        // Get the intersection box
        BoundingBox intersection = this.boundingBoxIntersection(other);

        // Calculate areas
        double intersectionArea = intersection.area();
        double thisArea = this.area();
        double otherArea = other.area();

        // Calculate union area: A + B - (A ∩ B)
        double unionArea = thisArea + otherArea - intersectionArea;

        // Return Intersection over Union
        return Math.round(intersectionArea / unionArea * 100) / 100.0;

    }

    /**
     * Calculates the ratio of this bounding box that extends outside the reference area.
     *
     * @param referenceArea The reference area (e.g., page dimensions)
     * @return The ratio of the box area that falls outside the reference area (0.0 if fully contained, 1.0 if fully outside)
     */
    public double calculateOutsideRatio(BoundingBox referenceArea) {
        // Check if boxes are on different pages
        if (this.page != referenceArea.page) {
            return 1.0; // Completely outside if on different pages
        }

        // If the box is fully contained within the reference area, return 0
        if (referenceArea.contains(this)) {
            return 0.0;
        }

        // If the box doesn't intersect with the reference area at all, it's completely outside
        if (!this.intersect(referenceArea)) {
            return 1.0;
        }

        // Calculate the intersection area (the part that's inside)
        BoundingBox intersection = this.boundingBoxIntersection(referenceArea);
        double intersectionArea = intersection.area();

        // Calculate the total area of this box
        double thisArea = this.area();

        // The ratio of the area that's outside is (total area - intersection area) / total area
        double outsideRatio = (thisArea - intersectionArea) / thisArea;

        // Round to 2 decimal places
        return Math.round(outsideRatio * 100) / 100.0;
    }
}
