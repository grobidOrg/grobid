package org.grobid.core.data.table;

import org.grobid.core.layout.LayoutToken;

import java.util.ArrayList;
import java.util.List;

public class LinePart {

    public static final double GROBID_TOKEN_DEFAULT_DOUBLE = -1.0;
    private List<LayoutToken> contentTokens = new ArrayList<>();

    double top = GROBID_TOKEN_DEFAULT_DOUBLE;
    double bottom = GROBID_TOKEN_DEFAULT_DOUBLE;
    double left = GROBID_TOKEN_DEFAULT_DOUBLE;
    double right = GROBID_TOKEN_DEFAULT_DOUBLE;

    public void add(LayoutToken contentToken) {
        contentTokens.add(contentToken);
        setTop(contentToken);
        setBottom(contentToken);
        setLeft(contentToken);
        setRight(contentToken);
    }

    private void setTop(LayoutToken contentToken) {
        double tokenY = contentToken.getY();
        if (tokenY == GROBID_TOKEN_DEFAULT_DOUBLE)
            return;

        if (top == GROBID_TOKEN_DEFAULT_DOUBLE) {
            top = tokenY;
            return;
        }

        if (tokenY < top) {
            top = tokenY;
        }
    }

    private void setBottom(LayoutToken contentToken) {
        double tokenY = contentToken.getY();
        double tokenHeight = contentToken.getHeight();

        if (tokenY == GROBID_TOKEN_DEFAULT_DOUBLE || tokenHeight == GROBID_TOKEN_DEFAULT_DOUBLE)
            return;

        double tokenBottom = Double.sum(tokenY, tokenHeight);

        if (bottom == GROBID_TOKEN_DEFAULT_DOUBLE) {
            bottom = tokenBottom;
            return;
        }

        if (tokenBottom > bottom) {
            bottom = tokenBottom;
        }
    }

    private void setLeft(LayoutToken contentToken) {
        double tokenX = contentToken.getX();
        if (tokenX == GROBID_TOKEN_DEFAULT_DOUBLE)
            return;

        if (left == GROBID_TOKEN_DEFAULT_DOUBLE) {
            left = tokenX;
            return;
        }

        if (tokenX < left) {
            left = tokenX;
        }
    }

    private void setRight(LayoutToken contentToken) {
        double tokenX = contentToken.getX();
        double tokenWidth = contentToken.getWidth();

        if (tokenX == GROBID_TOKEN_DEFAULT_DOUBLE || tokenWidth == GROBID_TOKEN_DEFAULT_DOUBLE)
            return;

        double tokenRight = Double.sum(tokenX, tokenWidth);

        if (right == GROBID_TOKEN_DEFAULT_DOUBLE) {
            right = tokenRight;
            return;
        }

        if (tokenRight > right) {
            right = tokenRight;
        }
    }

    public double getTop() {
        return top;
    }

    public double getBottom() {
        return bottom;
    }

    public double getLeft() {
        return left;
    }

    public double getRight() {
        return right;
    }

    public String getText() {
        StringBuilder stringBuilder = new StringBuilder();
        for (LayoutToken token : contentTokens) {
            stringBuilder.append(token.getText());
        }

        return stringBuilder.toString();
    }

    public boolean isEmpty() {
        return this.contentTokens.size() == 0;
    }
}
