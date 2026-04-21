package org.grobid.core.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for representing and exchanging a document block. A block is defined here relatively to
 * some properties in the document layout.
 */
public class Block {
    private String text = null;
    private BoundingBox boundingBox = null;
    /*private double y = 0.0;
    private double x = 0.0;
    private double width = 0.0;
    private double height = 0.0;*/
    private String font = null;
    private boolean bold = false;
    private boolean italic = false;
    private String colorFont = null;
    public double fontSize = 0.0;

    public List<LayoutToken> tokens = null;

    // start position of the block in the original tokenization
    private int startToken = -1;
    // end position of the block in the original tokenization
    private int endToken = -1;

    // the page in the document where the block is located
    private Page page = null;

    public enum Type {DEFAULT, BULLET, FIGURE, TABLE, REFERENCE}

    private Type type;

    public Block() {
    }

    public void addToken(LayoutToken lt) {
        if (tokens == null) {
            tokens = new ArrayList<LayoutToken>();
        }
        tokens.add(lt);
    }

    public List<LayoutToken> getTokens() {
        return tokens;
    }

    public void resetTokens() {
        tokens = null;
    }

    public void setType(Type t) {
        type = t;
    }

    public Type getType() {
        return type;
    }

    public String getText() {
        if (text != null && text.trim().startsWith("@"))
            return text.trim();
        else if (tokens == null)
            return null;
        else if (text != null)
            return text;
        else {
            StringBuilder localText = new StringBuilder();
            for(LayoutToken token : tokens) {
                localText.append(token.getText());
            }
            text = localText.toString();
            return text;
        }
    }

    public int getNbTokens() {
        if (tokens == null)
            return 0;
        else
            return tokens.size();
    }

    public void setFont(String f) {
        font = f;
    }

    public String getFont() {
        return font;
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

    public boolean getBold() {
        return bold;
    }

    public boolean getItalic() {
        return italic;
    }

    public void setFontSize(double d) {
        fontSize = d;
    }

    public double getFontSize() {
        return fontSize;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBox box) {
        boundingBox = box;
    }

    /*public void setX(double d) {
        x = Math.abs(d);
    }*/

    public double getX() {
        if (boundingBox != null)
            return boundingBox.getX();
        else
            return 0.0;
    }

    /*public void setY(double d) {
        y = Math.abs(d);
    }*/

    public double getY() {
        if (boundingBox != null)
            return boundingBox.getY();
        else
            return 0.0;
    }

    /*public void setHeight(double d) {
        height = Math.abs(d);
    }*/

    public double getHeight() {
        if (boundingBox != null)
            return boundingBox.getHeight();
        else
            return 0.0;
    }

    /*public void setWidth(double d) {
        width = Math.abs(d);
    }*/

    public double getWidth() {
        if (boundingBox != null)
            return boundingBox.getWidth();
        else
            return 0.0;
    }

    public int getStartToken() {
        return startToken;
    }

    public int getEndToken() {
        if (endToken == -1) {
            if (tokens == null || tokens.size() == 0) {
                return getStartToken();
            } else {
                return getStartToken() + tokens.size();
            }
        } else
            return endToken;
    }

    public void setStartToken(int start) {
        startToken = start;
    }

    public void setEndToken(int end) {
        endToken = end;
    }

    public Page getPage() {
        return page;
    }

    public int getPageNumber() {
        if (page != null) {
            return page.getNumber();
        } else {
            return -1;
        }
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public boolean isNull() {
        if ( (tokens == null) && (startToken == -1) && (endToken == -1) && (type == null) ) {
            return true;
        }
        else
            return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Block{");
        sb.append(", startToken=").append(startToken);
        sb.append(", endToken=").append(endToken);
        sb.append(", type=").append(type);
        if (boundingBox != null) {
            sb.append(", boundingBox=").append(boundingBox.toString()).append('}');
        } else {
            sb.append('}');
        }
        return sb.toString();
    }
}
