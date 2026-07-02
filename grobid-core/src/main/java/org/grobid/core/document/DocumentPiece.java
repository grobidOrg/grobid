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
package org.grobid.core.document;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class DocumentPiece implements Comparable<DocumentPiece> {
    //for easier access make them final, but public
    private final DocumentPointer a;
    private final DocumentPointer b;

    public DocumentPiece(DocumentPointer a, DocumentPointer b) {
        if (a.compareTo(b) > 0) {
            throw new IllegalArgumentException("Invalid document piece: " + a + "-" + b);
        }
        this.a = a;
        this.b = b;
    }

    public DocumentPointer getLeft() {
        return a;
    }

    public DocumentPointer getRight() {
        return b;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("left", a)
                .append("right", b)
                .toString();
    }

    @Override
    public int compareTo(DocumentPiece o) {
        return a.compareTo(o.a);
    }
}
