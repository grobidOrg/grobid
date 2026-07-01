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
package org.grobid.core.data.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.grobid.core.layout.LayoutToken;

public class Line extends LinePart {

    private List<LinePart> contentParts = new ArrayList<>();

    public void add(LinePart contentPart) {
        contentParts.add(contentPart);

        setTop(contentPart);
        setBottom(contentPart);
        setLeft(contentPart);
        setRight(contentPart);
    }

    private void setTop(LinePart contentPart) {
        double partTop = contentPart.getTop();

        if (top == GROBID_TOKEN_DEFAULT_DOUBLE || top > partTop) {
            top = partTop;
        }
    }

    private void setBottom(LinePart contentPart) {
        double partBottom = contentPart.getBottom();

        if (bottom == GROBID_TOKEN_DEFAULT_DOUBLE || bottom < partBottom) {
            bottom = partBottom;
        }
    }

    private void setLeft(LinePart contentPart) {
        double partLeft = contentPart.getLeft();

        if (left == GROBID_TOKEN_DEFAULT_DOUBLE || left > partLeft) {
            left = partLeft;
        }
    }

    private void setRight(LinePart contentPart) {
        double partRight = contentPart.getRight();

        if (right == GROBID_TOKEN_DEFAULT_DOUBLE || right < partRight) {
            right = partRight;
        }
    }

    public List<LinePart> getContent() {
        if (!this.contentParts.isEmpty())
            return this.contentParts;
        return null;
    }

    public boolean isEmpty() {
        return this.contentParts.size() == 0;
    }

    public boolean linePartInBorders(LinePart linePart) {
        if (this.contentParts.isEmpty())
            return true;

        // token is fully above the line or below, it doesn't overlap
        if ((this.getTop() > linePart.getBottom()) || this.getBottom() < linePart.getTop())
            return false;

        return true;
    }

    @Override
    public String getText() {
        StringBuilder stringBuilder = new StringBuilder();
        for (LinePart linePart : contentParts) {
            stringBuilder.append(linePart.getText());
        }

        return stringBuilder.toString();
    }

    public static List<LinePart> extractLineParts(List<LayoutToken> contentTokens) {
        List<LinePart> lineParts = new ArrayList<>();
        LinePart currentLinePart = null;
        for (int i = 0; i < contentTokens.size(); i++) {
            LayoutToken contentToken = contentTokens.get(i);
            if (i == 0) {
                currentLinePart = new LinePart();
                lineParts.add(currentLinePart);
            }

            if (!contentToken.getText().equals("\n")) {
                currentLinePart.add(contentToken);
            }

            if (contentToken.getText().equals("\n")) {
                LinePart newLinePart = new LinePart();
                lineParts.add(newLinePart);
                currentLinePart = newLinePart;
            }
        }
        return lineParts;
    }

    /*
     * Algorithm for extracting lines.
     * See algorithm 1: Burcu Yildiz, Katharina Kaiser, Silvia Miksch. pdf2table: A Method to Extract Table Information
     * from PDF Files.
     */
    public static List<Line> extractLines(List<LinePart> lineParts) {
        List<Line> lines = new ArrayList<>();
        Line currentLine = null;
        int i = lineParts.size() - 1;
        while (!lineParts.isEmpty() && i >= 0) {
            LinePart linePart = lineParts.get(i);
            if (linePart.getText().isEmpty()) {
                lineParts.remove(i);
                i--;
                continue;
            }

            if (currentLine == null) {
                currentLine = new Line();
                lines.add(currentLine);
                currentLine.add(linePart);
                lineParts.remove(i);
                i--;
                continue;
            }

            if (currentLine.linePartInBorders(linePart)) {
                currentLine.add(linePart);
                lineParts.remove(i);
                i = lineParts.size() - 1; // return to the first item and recheck borders
                continue;
            }

            if (i == 0) {
                currentLine = null;
                i = lineParts.size() - 1;
            } else {
                i--;
            }
        }

        lines.sort(Comparator.comparingDouble(Line::getTop)); // sorting by top position
        return lines;
    }
}
