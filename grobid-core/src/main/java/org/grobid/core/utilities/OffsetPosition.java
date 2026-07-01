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
package org.grobid.core.utilities;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class OffsetPosition implements Comparable<OffsetPosition> {
    public int start = -1;
    public int end = -1;

    public OffsetPosition() {
    }

    public OffsetPosition(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public boolean overlaps(OffsetPosition pos) {
        return !((end <= pos.start) || (start >= pos.end));
    }

    public String toString() {
        return "" + start + "\t" + end;
    }

    @Override
    public int compareTo(OffsetPosition pos) {
        if (pos.start < start)
            return 1;
        else if (pos.start == start) {
            if (pos.end < end)
                return 1;
            else if (pos.end == end)
                return 0;
            else
                return -1;
        } else
            return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        OffsetPosition that = (OffsetPosition) o;

        return new EqualsBuilder()
                .append(start, that.start)
                .append(end, that.end)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(start)
                .append(end)
                .toHashCode();
    }
}
