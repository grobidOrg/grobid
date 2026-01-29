package org.grobid.core.utilities;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append("start", start)
                .append("end", end)
                .toString();
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

        return new EqualsBuilder().append(start, that.start).append(end, that.end).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(start).append(end).toHashCode();
    }
}
