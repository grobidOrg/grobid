package org.grobid.core.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for representing cluster of document layout elements, typically all blocks having the same font parameters.
 *
 */
public class Cluster {
    private List<Block> blocks = null;
    private List<Integer> blocks2 = null;
    public double y = 0.0;
    public double x = 0.0;
    public double width = 0.0;
    public double height = 0.0;
    private String font = null;
    private boolean bold = false;
    private boolean italic = false;
    private String colorFont = null;
    private double fontSize = 0.0;

    private int nbTokens = 0;

    public Cluster() {
    }

    public void addBlock(Block b) {
        if (blocks == null)
            blocks = new ArrayList<Block>();
        blocks.add(b);
    }

    public void addBlock2(Integer b) {
        if (blocks2 == null)
            blocks2 = new ArrayList<Integer>();
        blocks2.add(b);
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public List<Integer> getBlocks2() {
        return blocks2;
    }

    public void setFont(String f) {
        font = f;
    }

    public void setNbTokens(int t) {
        nbTokens = t;
    }

    public String getFont() {
        return font;
    }

    public int getNbBlocks() {
        if (blocks == null)
            return 0;
        else
            return blocks.size();
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

}
