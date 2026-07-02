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

import java.util.*;

public class Row extends LinePart {

    private List<Cell> cells = new ArrayList<>();

    public void add(Cell cell) {
        cells.add(cell);

        setTop(cell);
        setBottom(cell);
        setLeft(cell);
        setRight(cell);
    }

    private void setTop(Cell cell) {
        double cellTop = cell.getTop();

        if (top == GROBID_TOKEN_DEFAULT_DOUBLE || top > cellTop) {
            top = cellTop;
        }
    }

    private void setBottom(Cell cell) {
        double cellBottom = cell.getBottom();

        if (bottom == GROBID_TOKEN_DEFAULT_DOUBLE || bottom < cellBottom) {
            bottom = cellBottom;
        }
    }

    private void setLeft(Cell cell) {
        double cellLeft = cell.getLeft();

        if (left == GROBID_TOKEN_DEFAULT_DOUBLE || left > cellLeft) {
            left = cellLeft;
        }
    }

    private void setRight(Cell cell) {
        double cellRight = cell.getRight();

        if (right == GROBID_TOKEN_DEFAULT_DOUBLE || right < cellRight) {
            right = cellRight;
        }
    }

    public List<Cell> getContent() {
        return this.cells;
    }

    @Override
    public boolean isEmpty() {
        return this.getContent().size() == 0;
    }

    /**
     *
     * @param lines Lines, detected by the algorithm, see Line::extractLines
     * @return rows containing cells; doesn't include empty cells
     */
    public static List<Row> extractRows(List<Line> lines) {
        List<Row> rows = new ArrayList<>();
        for (Line line : lines) {
            if (line.getText().isEmpty())
                continue;
            List<LinePart> lineContent = line.getContent();
            Row row = new Row();
            Cell currentCell = null;
            int i = lineContent.size() - 1;
            while (!lineContent.isEmpty() && i >= 0) {
                LinePart linePart = lineContent.get(i);
                if (currentCell == null) {
                    currentCell = new Cell();
                    row.add(currentCell);
                    currentCell.add(linePart);
                    lineContent.remove(i);
                    i--;
                    continue;
                }

                if (currentCell.linePartInBorders(linePart)) {
                    currentCell.add(linePart);
                    lineContent.remove(i);
                    i = lineContent.size() - 1; // return to the first item and recheck borders
                    continue;
                }

                if (i == 0) {
                    currentCell = null;
                    i = lineContent.size() - 1;
                } else {
                    i--;
                }
            }
            row.getContent().sort(Comparator.comparingDouble(LinePart::getLeft));
            rows.add(row);
        }
        return rows;
    }

    /**
     *
     * @param rows extracted rows
     * @param columnCount the maximum number of columns in the table
     * Identifies and inserts empty cells into the table based on the left and right margins of the content inside columns.
     *
     */
    public static void insertEmptyCells(List<Row> rows, int columnCount) {
        int columnNumber = 0;
        while (columnNumber < columnCount) {
            double currentLeftMost = Cell.GROBID_TOKEN_DEFAULT_DOUBLE;
            double nextColumnLeftMost = Cell.GROBID_TOKEN_DEFAULT_DOUBLE;

            for (Row row : rows) {
                List<Cell> cells = row.getContent();
                if (columnNumber > cells.size() - 1)
                    continue;
                Cell cell = cells.get(columnNumber);
                if (currentLeftMost == Cell.GROBID_TOKEN_DEFAULT_DOUBLE || currentLeftMost > cell.getLeft()) {
                    currentLeftMost = cell.getLeft();
                }

                if ((columnNumber + 1) < cells.size()) {
                    Cell nextColumnCell = cells.get(columnNumber + 1);
                    if (nextColumnLeftMost == Cell.GROBID_TOKEN_DEFAULT_DOUBLE
                            || nextColumnLeftMost > nextColumnCell.getLeft()) {
                        nextColumnLeftMost = nextColumnCell.getLeft();
                    }
                }
            }

            double currentRightMost = Cell.GROBID_TOKEN_DEFAULT_DOUBLE;
            for (Row row : rows) {
                List<Cell> cells = row.getContent();
                if (columnNumber > cells.size() - 1)
                    continue;
                Cell cell = cells.get(columnNumber);
                if (nextColumnLeftMost != Cell.GROBID_TOKEN_DEFAULT_DOUBLE) {
                    if (cell.getRight() < nextColumnLeftMost && (currentRightMost < cell.getRight())) {
                        currentRightMost = cell.getRight();
                    }
                }
            }

            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                List<Cell> cells = row.getContent();
                if (columnNumber > cells.size() - 1) {
                    // insert empty cell for premature ended rows
                    Cell newCell = new Cell();
                    newCell.setLeft(currentLeftMost);
                    newCell.setRight(currentRightMost);
                    newCell.setPositionRow(i);
                    newCell.setPositionColumn(columnNumber);
                    row.add(newCell);
                    continue;
                }
                Cell cell = cells.get(columnNumber);
                if (cell.getRight() <= currentRightMost || currentRightMost == Cell.GROBID_TOKEN_DEFAULT_DOUBLE) {
                    cell.setPositionRow(i);
                    cell.setPositionColumn(columnNumber);
                } else if (cell.getLeft() > currentRightMost) {
                    // empty cell
                    Cell newCell = new Cell();
                    newCell.setRight(cell.getRight());
                    newCell.setLeft(currentLeftMost);
                    newCell.setPositionRow(i);
                    newCell.setPositionColumn(columnNumber);
                    row.getContent().add(columnNumber, newCell);
                } else {
                    Cell newCell = new Cell();
                    newCell.setRight(cell.getRight());
                    newCell.setLeft(nextColumnLeftMost);
                    newCell.setPositionRow(i);
                    newCell.setPositionColumn(columnNumber + 1);
                    newCell.setMerged(true);
                    row.getContent().add(columnNumber + 1, newCell);

                    // current cell spans on several columns
                    int z = columnNumber;
                    while (z >= 0) {
                        Cell colspanCell = cells.get(z);
                        // find the cell that spans on several rows, it's the first non-empty cell.
                        if (!colspanCell.isEmpty()) {
                            colspanCell.setColspan(colspanCell.getColspan() + 1);
                            if (colspanCell.getPositionRow() == -1) {
                                colspanCell.setPositionRow(z);
                            }

                            if (colspanCell.getPositionColumn() == -1) {
                                colspanCell.setPositionColumn(columnNumber);
                            }
                            break;
                        }
                        z--;
                    }
                }
            }

            columnNumber++;
        }
    }

    public static int columnCount(List<Row> rows) {
        int columnCount = 0;
        for (Row row : rows) {
            int cellNumber = row.getContent().size();
            if (cellNumber > columnCount) {
                columnCount = cellNumber;
            }
        }

        return columnCount;
    }

    public static void mergeMulticolumnCells(List<Row> rows) {
        for (Row row : rows) {
            List<Cell> cells = row.getContent();
            for (int i = cells.size() - 1; i >= 0; i--) {
                Cell cell = cells.get(i);
                if (cell.isMerged()) {
                    row.getContent().remove(i);
                }
            }
        }
    }
}
