package org.grobid.core.layout;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.core.engines.label.TaggingLabel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for representing the layout information associated to a PDF object.
 *
 */
public class LayoutToken implements Comparable<LayoutToken>, Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String text = null;
    public double y = -1.0;
    public double x = -1.0;
    public double width = 0.0;
    public double height = 0.0;
    private String font = null;
    private boolean bold = false;
    private boolean italic = false;
    private String colorFont = null;
    public double fontSize = 0.0;
    private boolean rotation = false;
    private int page = -1;
    private boolean newLineAfter;
    private int blockPtr;
	private int offset = 0;
    private boolean subscript = false;
    private boolean superscript = false;
	
	/**
	 * All TaggingLabel accumulated for this token
	 */
	private ArrayList<TaggingLabel> labels = null;

    public LayoutToken() {
    }

    public LayoutToken(String text) {
        this.text = text;
    }

    public LayoutToken(LayoutToken token) {
        this.text = token.text;
        this.y = token.y;
        this.x = token.x;
        this.width = token.width;
        this.height = token.height;
        this.font = token.font;
        this.bold = token.bold;
        this.italic = token.italic;
        this.colorFont = token.colorFont;
        this.fontSize = token.fontSize;
        this.rotation = token.rotation;
        this.page = token.page;
        this.newLineAfter = token.newLineAfter;
        this.blockPtr = token.blockPtr;
        this.offset = token.offset;
        this.subscript = token.subscript;
        this.superscript = token.superscript;

        // deep copy of the TaggingLabel list
        if (token.labels != null) {
            this.labels = new ArrayList<TaggingLabel>();
            for(TaggingLabel l : token.labels) {
                this.labels.add(l);
            }
        }
    }
    
    public LayoutToken(String text, TaggingLabel label) {
    	this(text);
    	this.addLabel(label);
    }

    public void setFont(String f) {
        font = f;
    }

    public String getFont() {
        return font;
    }

    public void setText(String f) {
        //text = f.replaceAll("\n", "");
		text = f;
    }

    public void setRotation(boolean b) {
        rotation = b;
    }

    public boolean getRotation() {
        return rotation;
    }

    public String getText() {
        return text;
    }

    public String t() {
        return text;
    }

    public void setColorFont(String f) {
        colorFont = f;
    }

    public String getColorFont() {
        return colorFont;
    }

    public void setBold(boolean b) {
        bold = b;
    }

    public void setItalic(boolean i) {
        italic = i;
    }

    public boolean isBold() {
        return bold;
    }

    /** @use isBold() **/
    @Deprecated
    public boolean getBold() {
        return bold;
    }

    public boolean isItalic() {
        return italic;
    }

    /** @use isItalic() **/
    @Deprecated
    public boolean getItalic() {
        return italic;
    }

    public boolean isSubscript() {
        return subscript;
    }

    public void setSubscript(boolean script) {
        this.subscript = script;
    }

    public boolean isSuperscript() {
        return superscript;
    }

    public void setSuperscript(boolean script) {
        this.superscript = script;
    }

    public void setFontSize(double d) {
        fontSize = d;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setX(double d) {
        x = d;
    }

    public double getX() {
        return x;
    }

    public void setY(double d) {
        y = d;
    }

    public double getY() {
        return y;
    }

    public void setHeight(double d) {
        height = d;
    }

    public double getHeight() {
        return height;
    }

    public void setWidth(double d) {
        width = d;
    }

    public double getWidth() {
        return width;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public boolean isNewLineAfter() {
        return newLineAfter;
    }

    public void setNewLineAfter(boolean newLineAfter) {
        this.newLineAfter = newLineAfter;
    }

    public int getBlockPtr() {
        return blockPtr;
    }

    public void setBlockPtr(int blockPtr) {
        this.blockPtr = blockPtr;
    }

	public int getOffset() {
		return offset;
	}
	
	public void setOffset(int offset) {
		this.offset = offset;
	}
	
	/**
	 * Get all TaggingLabel accumulated for this token
	 */
	public List<TaggingLabel> getLabels() {
		if (this.labels == null)
			return new ArrayList<TaggingLabel>();
		return this.labels;
	}
	
	/**
	 * Check if a given TaggingLabel is associated with this token
	 */
	public boolean hasLabel(TaggingLabel label) {
		return this.labels.contains(label);
	}
	
	/**
	 * Add a TaggingLabel to this token
	 */
	public void addLabel(TaggingLabel label) {
		if (this.labels == null)
			this.labels = new ArrayList<TaggingLabel>();
		if (!hasLabel(label))
			this.labels.add(label);
	}

    @Override
    public String toString() {
        return text;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(text)
                .append(x)
                .append(y)
                .append(width)
                .append(height)
                .append(font)
                .append(bold)
                .append(italic)
                .append(colorFont)
                .append(fontSize)
                .append(rotation)
                .append(page)
                .toHashCode();
    }

    @Override
    public int compareTo(LayoutToken token2) {
        if (y != token2.y) {
            if (y < token2.y)
                return -1;
            else 
                return 1;
        }
        else if (x != token2.x) {
            if (x < token2.x)
                return -1;
            else 
                return 1;
        }
        else {
            double area1 = height*width;
            double area2 = token2.height*token2.width;

            return Double.compare(area1, area2);
        }
    }

}