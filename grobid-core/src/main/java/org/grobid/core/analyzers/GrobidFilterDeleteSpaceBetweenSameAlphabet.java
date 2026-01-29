/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Bruno Pouliquen @ WIPO
 */

package org.grobid.core.analyzers;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

public final class GrobidFilterDeleteSpaceBetweenSameAlphabet extends TokenFilter {
    private CharTermAttribute termAttr;
    private TypeAttribute typeAttr;
    private PositionIncrementAttribute posAttr;
    private OffsetAttribute offsetAttr;

    private String previousBuffer;
    private int previousBufferLength = 0;
    private String previousType = null;
    private int previousStartOffset = 0;
    private int previousEndOffset = 0;
    private int previousPosIncr = 0;

    public GrobidFilterDeleteSpaceBetweenSameAlphabet(TokenStream input) {
        super(input);
        termAttr = (CharTermAttribute) addAttribute(CharTermAttribute.class);
        typeAttr = (TypeAttribute) addAttribute(TypeAttribute.class);
        offsetAttr = (OffsetAttribute) addAttribute(OffsetAttribute.class);;
        posAttr = (PositionIncrementAttribute) addAttribute(PositionIncrementAttribute.class);
        previousBuffer = null;
    }

    public boolean incrementToken() throws IOException {

        if (previousBuffer != null) {
            termAttr.setEmpty().append(previousBuffer);
            typeAttr.setType(previousType);
            offsetAttr.setOffset(previousStartOffset, previousEndOffset);
            posAttr.setPositionIncrement(previousPosIncr);
            previousBuffer = null;
            return true;
        }

        if (!input.incrementToken()) { // #B
            return false; // #C
        }
        char[] buffer = termAttr.buffer();
        if (!isLatinChar(buffer[0]))
            return true;
        if (isDigit(buffer[0])) {
            // if (!isNumeral(previousBuffer)) return true;
        }

        previousBuffer = termAttr.toString();
        previousBufferLength = termAttr.length();
        previousType = typeAttr.type();
        previousStartOffset = offsetAttr.startOffset();
        previousEndOffset = offsetAttr.endOffset();
        previousPosIncr = posAttr.getPositionIncrement();

        boolean cont = true;
        String currentBuffer = null;
        int currentBufferLength = 0;
        String currentType = null;
        int currentStartOffset = -1;
        int currentEndOffset = -1;
        int currentPosIncr = 0;
        while (cont && input.incrementToken()) {
            currentBuffer = termAttr.toString();
            currentBufferLength = termAttr.length();
            currentType = typeAttr.type();
            currentStartOffset = offsetAttr.startOffset();
            currentEndOffset = offsetAttr.endOffset();
            currentPosIncr = posAttr.getPositionIncrement();

            // Series of conditions to concatenate tokens:
            if ((buffer[0] == '.' && isNumeral(previousBuffer) // 0 . => 0.
            ) || (isNumeral(currentBuffer) && isNumeral(previousBuffer) // 1 2 => 12
            ) || (previousBuffer.endsWith(".") && isNumeral(previousBuffer) && isNumeral(currentBuffer) // 0. 1 => 0.1
            ) || (currentStartOffset >= previousEndOffset && isLatinChar(buffer[0]) && currentType.equals(previousType)
                    && (!(isNumeral(previousBuffer) && !isNumeral(currentBuffer))

                    ) && (!(isNumeral(currentBuffer) && !isNumeral(previousBuffer)))
            // a b => ab
            )

            ) {
                // current token has the same alphabet, we concatenate them
                String n = previousBuffer + currentBuffer;
                previousBuffer = n;
                currentBuffer = null;
                previousEndOffset = currentEndOffset;
            } else {
                cont = false;
                break;
            }
        }

        termAttr.setEmpty().append(previousBuffer);
        typeAttr.setType(previousType);
        offsetAttr.setOffset(previousStartOffset, previousEndOffset);
        posAttr.setPositionIncrement(previousPosIncr);
        previousBuffer = null;

        if (currentBuffer != null) {
            previousBuffer = currentBuffer;
            previousBufferLength = currentBufferLength;
            previousType = currentType;
            previousStartOffset = currentStartOffset;
            previousEndOffset = currentEndOffset;
            previousPosIncr = currentPosIncr;
        }
        return true;
    }

    private boolean isDigit(char c) {
        return ((c >= '0' && c <= '9') || (c >= 0xFF10 && c <= 0xFF19));
    }

    private boolean isLatinChar(char c) {
        return ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || (c >= 0xFF10 && c <= 0xFF19) || (c >= 0xFF01 && c <= 0xFF5E));

    }

    private boolean isNumeral(String s) {
        return (s != null && !s.isEmpty() && isDigit(s.charAt(0)));
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        previousBuffer = null;
        previousBufferLength = 0;
        previousType = null;
        previousStartOffset = 0;
        previousEndOffset = 0;
        previousPosIncr = 0;
    }
}
