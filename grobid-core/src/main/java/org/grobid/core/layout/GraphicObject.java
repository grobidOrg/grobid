package org.grobid.core.layout;

import java.io.File;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Class for representing graphical objects occurring within a document.
 *
 */
public class GraphicObject {
    private String filePath = null;

    private GraphicObjectType type = GraphicObjectType.UNKNOWN;

    // position in the global tokenization
    private int startPosition = -1;
    private int endPosition = -1;

    private int blockNumber = -1;

    private BoundingBox boundingBox = null;

    private boolean mask = false;

    // in case of vector image, we don't have a boundingBox from pdfalto, simply the page information
    private int page = -1;
    
    public boolean used;

    public GraphicObject() {
    }

    public GraphicObject(BoundingBox boundingBox, GraphicObjectType type) {
        this.boundingBox = boundingBox;
        this.type = type;
    }

    /**
     * Return the full path of the file corresponding to the graphic object, useful
     * as internal implementation information only
     */
    public String getFilePath() {
        return this.filePath;
    }

    /**
     * Return an URI for the file corresponding to the graphic object, in practice a 
     * portable relative path usable for data exchange
     */
    public String getURI() {
        if (filePath == null) {
            return null;
        }
        int ind = filePath.lastIndexOf("/");
        if (ind != -1) {
            //int ind2 = filePath.substring(0, ind-1).lastIndexOf("/");
            //if (ind2 != -1)
            //    return filePath.substring(ind2+1, filePath.length());
            return filePath.substring(ind+1, filePath.length());
        }

        return new File(filePath).getName();
    }

    public GraphicObjectType getType() {
        return type;
    }

    public void setFilePath(String path) {
        this.filePath = path;
    }

    public void setType(GraphicObjectType type) {
        this.type = type;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }

    /*public int getBlockNumber() {
        return blockNumber;
    }*/

    public void setBlockNumber(int blockNumber) {
        this.blockNumber = blockNumber;
    }

    public double getX() {
        if (boundingBox != null)
            return boundingBox.getX();
        else 
            return 0.0;
    }

    public double getY() {
        if (boundingBox != null)
            return boundingBox.getY();
        else 
            return 0.0;
    }

    public double getWidth() {
        if (boundingBox != null)
            return boundingBox.getWidth();
        else 
            return 0.0;
    }

    public double getHeight() {
        if (boundingBox != null)
            return boundingBox.getHeight();
        else 
            return 0.0;
    }

    public int getPage() {
        if (boundingBox != null)
            return boundingBox.getPage();
        else 
            return page;
    }

    /*public void setX(double x1) {
        this.x = Math.abs(x1);
    }

    public void setY(double y1) {
        this.y = Math.abs(y1);
    }

    public void setWidth(double x2) {
        this.width = Math.abs(x2);
    }

    public void setHeight(double y2) {
        this.height = Math.abs(y2);
    }*/

    public void setPage(int page) {
        this.page = page;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBox box) {
        boundingBox = box;
    }

    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("type", type)
                .append("filePath", filePath)
                .append("startPosition", startPosition)
                .append("endPosition", endPosition)
                .append("boundingBox", boundingBox)
                .toString();
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public boolean isMask() {
        return mask;
    }

    public void setMask(boolean mask) {
        this.mask = mask;
    }
}