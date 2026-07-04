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

import static org.grobid.core.utilities.Utilities.convertStringOffsetToTokenOffset;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.layout.LayoutToken;

public class UtilitiesTest {

    @Test
    public void testStringToBooleanTrue() {
        assertEquals(
                "stringToBoolean value does not match expected result",
                true,
                Utilities.stringToBoolean("true"));
    }

    @Test
    public void testStringToBooleanTrue2() {
        assertEquals(
                "stringToBoolean value does not match expected result",
                true,
                Utilities.stringToBoolean(" TruE "));
    }

    @Test
    public void testStringToBooleanFalse() {
        assertEquals(
                "stringToBoolean value does not match expected result",
                false,
                Utilities.stringToBoolean("false"));
    }

    @Test
    public void testStringToBooleanFalse2() {
        assertEquals(
                "stringToBoolean value does not match expected result",
                false,
                Utilities.stringToBoolean(" fAlSe "));
    }

    @Test
    public void testStringToBooleanFalse3() {
        assertEquals(
                "stringToBoolean value does not match expected result",
                false,
                Utilities.stringToBoolean(" non boolean value"));
    }

    @Test
    public void testStringToBooleanBlank() {
        assertEquals(
                "stringToBoolean value does not match expected result",
                false,
                Utilities.stringToBoolean(""));
    }

    @Test
    public void testStringToBooleanBlank2() {
        assertEquals(
                "stringToBoolean value does not match expected result",
                false,
                Utilities.stringToBoolean(null));
    }

    @Test
    public void testMergePositions1() throws IOException {
        List<OffsetPosition> position1 = new ArrayList<OffsetPosition>();
        List<OffsetPosition> position2 = new ArrayList<OffsetPosition>();
        position1.add(new OffsetPosition(0, 2));
        position2.add(new OffsetPosition(3, 5));

        List<OffsetPosition> positions = Utilities.mergePositions(position1, position2);
        assertEquals(positions.size(), 2);
        assertEquals(positions.get(0).start, 0);
        assertEquals(positions.get(0).end, 2);
        assertEquals(positions.get(1).start, 3);
        assertEquals(positions.get(1).end, 5);
    }

    @Test
    public void testMergePositions2() throws IOException {
        List<OffsetPosition> position1 = new ArrayList<OffsetPosition>();
        List<OffsetPosition> position2 = new ArrayList<OffsetPosition>();
        position1.add(new OffsetPosition(0, 2));
        position1.add(new OffsetPosition(4, 5));
        position2.add(new OffsetPosition(3, 4));
        position2.add(new OffsetPosition(8, 10));

        List<OffsetPosition> positions = Utilities.mergePositions(position1, position2);
        assertEquals(positions.size(), 3);
        assertEquals(positions.get(0).start, 0);
        assertEquals(positions.get(0).end, 2);
        assertEquals(positions.get(1).start, 3);
        assertEquals(positions.get(1).end, 5);
        assertEquals(positions.get(2).start, 8);
        assertEquals(positions.get(2).end, 10);
    }

    @Test
    public void testMergePositionsOverlap() throws IOException {
        List<OffsetPosition> position1 = new ArrayList<OffsetPosition>();
        List<OffsetPosition> position2 = new ArrayList<OffsetPosition>();
        position1.add(new OffsetPosition(0, 3));
        position1.add(new OffsetPosition(5, 6));
        position1.add(new OffsetPosition(8, 9));
        position2.add(new OffsetPosition(1, 2));
        position2.add(new OffsetPosition(3, 6));
        position2.add(new OffsetPosition(7, 10));

        List<OffsetPosition> positions = Utilities.mergePositions(position1, position2);
        assertEquals(positions.size(), 2);
        assertEquals(positions.get(0).start, 0);
        assertEquals(positions.get(0).end, 6);
        assertEquals(positions.get(1).start, 7);
        assertEquals(positions.get(1).end, 10);
    }

    @Test
    public void testConvertStringOffsetToTokenOffset() throws Exception {
        String input = "This is a token.";
        List<LayoutToken> layoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        OffsetPosition stringPosition = new OffsetPosition(5, 9);
        List<OffsetPosition> tokenOffsets = convertStringOffsetToTokenOffset(
                Arrays.asList(stringPosition),
                layoutTokens);

        assertThat(tokenOffsets, hasSize(1));
        OffsetPosition position = tokenOffsets.get(0);
        assertThat(LayoutTokensUtil.toText(layoutTokens.subList(position.start, position.end + 1)), is("is a"));
    }
}
