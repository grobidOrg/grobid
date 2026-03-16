package org.grobid.core.document;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Class representing a pointer within a PDF document, basically a block index and then a token index within a block (not global token index)
 */
public class DocumentPointer implements Comparable<DocumentPointer>{
    public static final DocumentPointer START_DOCUMENT_POINTER = new DocumentPointer(0, 0 , 0);


    private final int blockPtr;
    private final int tokenBlockPos;
    private final int tokenDocPos;

    public DocumentPointer(int blockPtr, int tokenDocPos, int tokenBlockPos) {
        Preconditions.checkArgument(tokenDocPos >= tokenBlockPos);
        Preconditions.checkArgument(tokenBlockPos >= 0);
        this.tokenDocPos = tokenDocPos;
        this.tokenBlockPos = tokenBlockPos;
        this.blockPtr = blockPtr;
    }

    public DocumentPointer(Document doc, int blockIndex, int tokenDocPos) {
        this(blockIndex, tokenDocPos, tokenDocPos - doc.getBlocks().get(blockIndex).getStartToken());
    }

    @Override
    public int compareTo(DocumentPointer o) {
        return Ints.compare(tokenDocPos, o.tokenDocPos);
    }

    public int getBlockPtr() {
        return blockPtr;
    }

    public int getTokenBlockPos() {
        return tokenBlockPos;
    }

    public int getTokenDocPos() {
        return tokenDocPos;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("blockPtr", blockPtr)
                .append("tokenBlockPos", tokenBlockPos)
                .append("tokenDocPos", tokenDocPos)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentPointer)) return false;
        DocumentPointer that = (DocumentPointer) o;
        return new EqualsBuilder()
                .append(blockPtr, that.blockPtr)
                .append(tokenBlockPos, that.tokenBlockPos)
                .append(tokenDocPos, that.tokenDocPos)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(blockPtr)
                .append(tokenBlockPos)
                .append(tokenDocPos)
                .toHashCode();
    }
}
