package org.grobid.core.data.table;

public class Cell extends Line {

    private int positionRow = -1;
    private int positionColumn = -1;
    private int colspan = 1;
    private boolean merged = false;

    public boolean linePartInBorders(LinePart linePart) {
        if (this.getContent().isEmpty())
            return true;

        if ((this.getLeft() > linePart.getRight()) || this.getRight() < linePart.getLeft())
            return false;

        return true;
    }

    public int getColspan() {
        return this.colspan;
    }

    public void setColspan(int colspan) {
        this.colspan = colspan;
    }

    public int getPositionRow() {
        return positionRow;
    }

    public void setPositionRow(int positionRow) {
        this.positionRow = positionRow;
    }

    public int getPositionColumn() {
        return positionColumn;
    }

    public void setPositionColumn(int positionColumn) {
        this.positionColumn = positionColumn;
    }

    public void setRight(double rightpos) {
        this.right = rightpos;
    }

    public void setLeft(double leftpos) {
        this.left = leftpos;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }

    public boolean isMerged() {
        return this.merged;
    }
}
