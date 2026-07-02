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

import java.util.*;

/**
 * Class for representing a page.
 *
 */
public class Page {
    private List<Block> blocks = null;
    private double width = 0.0;
    private double height = 0.0;
    private int number = -1;
    private int pageLengthChar = 0;
    private BoundingBox mainArea;

    public Page(int nb) {
        number = nb;
    }

    public boolean isEven() {
        return number % 2 == 0;
    }

    public void addBlock(Block b) {
        if (blocks == null) {
            blocks = new ArrayList<>();
        }
        blocks.add(b);
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setHeight(double d) {
        height = Math.abs(d);
    }

    public double getHeight() {
        return height;
    }

    public void setWidth(double d) {
        width = Math.abs(d);
    }

    public double getWidth() {
        return width;
    }

    public void setPageLengthChar(int length) {
        pageLengthChar = length;
    }

    public int getPageLengthChar() {
        return pageLengthChar;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public BoundingBox getMainArea() {
        return mainArea;
    }

    public void setMainArea(BoundingBox mainArea) {
        this.mainArea = mainArea;
    }
}
