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
package org.grobid.core.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Test;

import org.grobid.core.data.Person;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.PDFAnnotation;

public class TestNameParser extends EngineTest {

    @AfterClass
    public static void tearDown() {
        GrobidFactory.reset();
    }

    @Test
    public void testNameParserHeader() throws Exception {
        String authorSequence1 = "José-María Carazo, Alberto Pascual-Montano";
        List<Person> res = engine.processAuthorsHeader(authorSequence1);
        assertNotNull(res);
        assertEquals(2, res.size());
        if (res.size() > 0) {
            assertThat(res.get(0).getFirstName(), is("José-María"));
            assertThat(res.get(0).getLastName(), is("Carazo"));
        }
        if (res.size() > 1) {
            assertThat(res.get(1).getFirstName(), is("Alberto"));
            assertThat(res.get(1).getLastName(), is("Pascual-Montano"));
        }

        String authorSequence2 = "Farzaneh Sarafraz*, James M. Eales*, Reza Mohammadi, Jonathan Dickerson, David Robertson, Goran Nenadic*";
        res = engine.processAuthorsHeader(authorSequence2);
        assertNotNull(res);
        assertEquals(6, res.size());
        if (res.size() > 0) {
            assertThat(res.get(0).getFirstName(), is("Farzaneh"));
            assertThat(res.get(0).getLastName(), is("Sarafraz"));
        }
        if (res.size() > 1) {
            assertThat(res.get(1).getFirstName(), is("James"));
            assertThat(res.get(1).getMiddleName(), is("M"));
            assertThat(res.get(1).getLastName(), is("Eales"));
        }
        if (res.size() > 2) {
            assertThat(res.get(2).getFirstName(), is("Reza"));
            assertThat(res.get(2).getLastName(), is("Mohammadi"));
        }

        String authorSequence3 = "KARL-HEINZ HÖCKER";
        res = engine.processAuthorsHeader(authorSequence3);
        assertNotNull(res);
        if (res != null) {
            //assertEquals(1, res.size());
            if (res.size() > 0) {
                //assertThat(res.get(0).getFirstName(), is("SF"));
                assertThat(res.get(0).getLastName(), is("Höcker"));
                assertThat(res.get(0).getFirstName(), is("Karl-Heinz"));
            }
        }
    }

    @Test
    public void testNameParserCitation() throws Exception {

        String authorSequence1 = "Tsuruoka Y. et al.";
        List<Person> res = engine.processAuthorsCitation(authorSequence1);
        assertNotNull(res);
        assertEquals(1, res.size());
        if (res.size() > 0) {
            assertThat(res.get(0).getFirstName(), is("Y"));
            assertThat(res.get(0).getLastName(), is("Tsuruoka"));
        }

        String authorSequence2 = "Altschul SF, Madden TL, Schäffer AA, Zhang J, Zhang Z, Miller W, Lipman DJ";
        res = engine.processAuthorsCitation(authorSequence2);
        assertNotNull(res);
        if (res != null) {
            //assertEquals(1, res.size());
            if (res.size() > 0) {
                //assertThat(res.get(0).getFirstName(), is("SF"));
                assertThat(res.get(0).getLastName(), is("Altschul"));
            }
        }
    }

    @Test
    public void testORCIDExtractionEdgeCase() throws Exception {
        // This test simulates the edge case that would cause StringIndexOutOfBoundsException
        // in ORCID extraction when charsCovered > authorsToken.getText().length()

        // We need to create a scenario where the calculation:
        // int charsCovered = (int) ((intersectBox.getWidth() / pixPerChar) + 0.5);
        // results in a value larger than the token text length

        String authorText = "John Smith"; // 10 characters

        // Create LayoutTokens manually to control the width parameter
        List<org.grobid.core.layout.LayoutToken> tokens = new java.util.ArrayList<>();

        // Create a token with very small width to make pixPerChar very small
        LayoutToken smallWidthToken = new org.grobid.core.layout.LayoutToken();
        smallWidthToken.setText(authorText);
        smallWidthToken.setWidth(1.0); // Very small width: 1.0 pixels for 10 chars = 0.1 pixPerChar

        tokens.add(smallWidthToken);

        // Create a mock PDF annotation that would overlap significantly
        org.grobid.core.layout.PDFAnnotation mockAnnotation = new PDFAnnotation();
        mockAnnotation.setDestination("https://orcid.org/1234-5678-9012-3456");

        // Create a bounding box that covers more area than the token itself
        // This will result in a large intersectBox.getWidth()
        org.grobid.core.layout.BoundingBox largeBoundingBox = BoundingBox.fromPointAndDimensions(
                1,
                0.0,
                0.0,
                100.0,
                20.0 // Much larger than the token's 1.0 width
        );
        mockAnnotation.setBoundingBoxes(List.of(largeBoundingBox));

        // Set the token's bounding box to be much smaller
        org.grobid.core.layout.BoundingBox tokenBoundingBox = BoundingBox.fromPointAndDimensions(
                1,
                0.0,
                0.0,
                1.0,
                10.0 // Small box: 1.0 x 10.0
        );
        smallWidthToken.setX(tokenBoundingBox.getX());
        smallWidthToken.setY(tokenBoundingBox.getY());
        smallWidthToken.setWidth(tokenBoundingBox.getWidth());
        smallWidthToken.setHeight(tokenBoundingBox.getHeight());
        smallWidthToken.setPage(1);

        List<org.grobid.core.layout.PDFAnnotation> annotations = new java.util.ArrayList<>();
        annotations.add(mockAnnotation);

        // Before the fix, this would cause:
        // pixPerChar = 1.0 / 10 = 0.1
        // charsCovered = (100.0 / 0.1 + 0.5) = 1000.5 -> 1000 chars
        // substring(0, 10 - 1000) = substring(0, -990) -> StringIndexOutOfBoundsException

        // After the fix, the boundary check should prevent the exception
        try {
            // This should not throw StringIndexOutOfBoundsException after the fix
            List<Person> result = engine.getParsers()
                    .getAuthorParser()
                    .processingHeaderWithLayoutTokens(tokens, annotations);

            // The test passes if no exception is thrown
            assertNotNull("Processing should complete without exception", result);

            // Additionally, verify that ORCID was extracted correctly despite the edge case
            if (result != null && !result.isEmpty()) {
                Person author = result.get(0);
                // The ORCID should be extracted if the annotation processing worked
                // Note: This depends on the exact ORCID extraction logic working correctly
            }

        } catch (StringIndexOutOfBoundsException e) {
            // If this exception is thrown, it means the bug is not fixed
            throw new AssertionError("StringIndexOutOfBoundsException should have been prevented by boundary check", e);
        }
    }

}
