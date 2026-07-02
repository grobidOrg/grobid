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
package org.grobid.core.data;

import org.grobid.core.utilities.OffsetPosition;

/**
 * Class for managing chemical entities.
 *
 */
public class ChemicalEntity {
    // attribute
    String rawName = null;
    String inchi = null;
    String smiles = null;

    OffsetPosition offsets = null;

    public ChemicalEntity() {
        offsets = new OffsetPosition();
    }

    public ChemicalEntity(String raw) {
        offsets = new OffsetPosition();
        this.rawName = raw;
    }

    public String getRawName() {
        return rawName;
    }

    public String getInchi() {
        return inchi;
    }

    public String getSmiles() {
        return smiles;
    }

    public void setRawName(String raw) {
        this.rawName = raw;
    }

    public void setInchi(String inchi) {
        this.inchi = inchi;
    }

    public void setSmiles(String smiles) {
        this.smiles = smiles;
    }

    public void setOffsetStart(int start) {
        offsets.start = start;
    }

    public int getOffsetStart() {
        return offsets.start;
    }

    public void setOffsetEnd(int end) {
        offsets.end = end;
    }

    public int getOffsetEnd() {
        return offsets.end;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(rawName + "\t" + inchi + "\t" + smiles + "\t" + offsets.toString());
        return buffer.toString();
    }

    // TODO: CML encoding
}
