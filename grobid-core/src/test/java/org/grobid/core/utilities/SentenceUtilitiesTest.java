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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.grobid.core.lang.SentenceDetector;
import org.grobid.core.lang.SentenceDetectorFactory;

public class SentenceUtilitiesTest {

    SentenceDetectorFactory sentenceDetectorFactoryMock;
    SentenceDetector sentenceDetectorMock;
    SentenceUtilities target;

    @Before
    public void setUp() {
        GrobidProperties.getInstance();
        GrobidConfig.ModelParameters modelParameters = new GrobidConfig.ModelParameters();
        modelParameters.name = "bao";
        GrobidProperties.addModel(modelParameters);

        sentenceDetectorFactoryMock = mock(SentenceDetectorFactory.class);
        sentenceDetectorMock = mock(SentenceDetector.class);
        target = SentenceUtilities.getInstance();
        // Inject the mocked factory into the singleton (replaces PowerMock's Whitebox.setInternalState)
        GrobidTestUtils.setField(target, "sdf", sentenceDetectorFactoryMock);
    }

    @Test
    public void testNullText() throws Exception {
        String text = null;
        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(text);
        assertNull(theSentences);
    }

    @Test
    public void testEmptyText() throws Exception {
        String text = "";
        when(sentenceDetectorFactoryMock.getInstance()).thenReturn(sentenceDetectorMock);
        when(sentenceDetectorMock.detect(text)).thenReturn(new ArrayList<>());

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(text);

        verify(sentenceDetectorMock).detect(text);
        assertThat(theSentences.size(), is(0));
    }

    @Test
    public void testOneSentenceText() throws Exception {
        String text = "Bla bla bla.";
        when(sentenceDetectorFactoryMock.getInstance()).thenReturn(sentenceDetectorMock);
        when(sentenceDetectorMock.detect(text)).thenReturn(Arrays.asList(new OffsetPosition(0, 12)));

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(text);

        verify(sentenceDetectorMock).detect(text);
        assertThat(theSentences.size(), is(1));
    }

    @Test
    public void testTwoSentencesText() throws Exception {
        String text = "Bla bla bla. Bli bli bli.";
        when(sentenceDetectorFactoryMock.getInstance()).thenReturn(sentenceDetectorMock);
        when(sentenceDetectorMock.detect(text))
                .thenReturn(Arrays.asList(new OffsetPosition(0, 12), new OffsetPosition(13, 24)));

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(text);

        verify(sentenceDetectorMock).detect(text);
        assertThat(theSentences.size(), is(2));
    }

    @Test
    public void testTwoSentencesTextWithUselessForbidden() throws Exception {
        String text = "Bla bla bla. Bli bli bli.";
        List<OffsetPosition> forbidden = new ArrayList<>();
        forbidden.add(new OffsetPosition(2, 8));

        when(sentenceDetectorFactoryMock.getInstance()).thenReturn(sentenceDetectorMock);
        when(sentenceDetectorMock.detect(text, null))
                .thenReturn(Arrays.asList(new OffsetPosition(0, 12), new OffsetPosition(13, 24)));

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(text, forbidden);

        verify(sentenceDetectorMock).detect(text, null);
        assertThat(theSentences.size(), is(2));
    }

    @Test
    public void testTwoSentencesTextWithUsefullForbidden() throws Exception {
        String text = "Bla bla bla. Bli bli bli.";
        List<OffsetPosition> forbidden = new ArrayList<>();
        forbidden.add(new OffsetPosition(2, 8));
        forbidden.add(new OffsetPosition(9, 15));

        when(sentenceDetectorFactoryMock.getInstance()).thenReturn(sentenceDetectorMock);
        when(sentenceDetectorMock.detect(text, null))
                .thenReturn(Arrays.asList(new OffsetPosition(0, 12), new OffsetPosition(13, 24)));

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(text, forbidden);

        verify(sentenceDetectorMock).detect(text, null);
        assertThat(theSentences.size(), is(1));
    }

    @Test
    public void testCorrectSegmentation_shouldNotCancelSegmentation() throws Exception {
        String paragraph = "This is a sentence. [3] Another sentence.";

        List<String> refs = Arrays.asList("[3]");
        List<String> sentences = Arrays.asList("This is a sentence.", "Another sentence.");

        List<OffsetPosition> refSpans = getPositions(paragraph, refs);
        List<OffsetPosition> sentenceSpans = getPositions(paragraph, sentences);

        when(sentenceDetectorFactoryMock.getInstance()).thenReturn(sentenceDetectorMock);
        when(sentenceDetectorMock.detect(paragraph, null)).thenReturn(sentenceSpans);

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(paragraph, refSpans);

        verify(sentenceDetectorMock).detect(paragraph, null);
        assertThat(theSentences.size(), is(2));
    }

    @Test
    public void testCorrectSegmentation_shouldNotCancelSegmentation2() throws Exception {
        String paragraph = "This is a sentence [3] and the continuing sentence.";

        List<String> refs = Arrays.asList("[3]");
        List<String> sentences = Arrays.asList("This is a sentence", "and the continuing sentence.");

        List<OffsetPosition> refSpans = getPositions(paragraph, refs);
        List<OffsetPosition> sentenceSpans = getPositions(paragraph, sentences);

        when(sentenceDetectorFactoryMock.getInstance()).thenReturn(sentenceDetectorMock);
        when(sentenceDetectorMock.detect(paragraph, null)).thenReturn(sentenceSpans);

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(paragraph, refSpans);

        verify(sentenceDetectorMock).detect(paragraph, null);
        assertThat(theSentences.size(), is(2));
    }

    @Test
    public void testCorrectSegmentation_shouldCancelWrongSegmentation() throws Exception {
        String paragraph = "(Foppiano and al. 2021) explains what he's thinking.";

        List<String> refs = Arrays.asList("(Foppiano and al. 2021)");
        List<String> sentences = Arrays.asList("(Foppiano and al.", "2021) explains what he's thinking.");

        List<OffsetPosition> refSpans = getPositions(paragraph, refs);
        List<OffsetPosition> sentenceSpans = getPositions(paragraph, sentences);

        when(sentenceDetectorFactoryMock.getInstance()).thenReturn(sentenceDetectorMock);
        when(sentenceDetectorMock.detect(paragraph, null)).thenReturn(sentenceSpans);

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(paragraph, refSpans);

        verify(sentenceDetectorMock).detect(paragraph, null);
        assertThat(theSentences.size(), is(1));
    }

    @Test
    public void testCorrectSegmentation_shouldCancelWrongSegmentation2() throws Exception {
        String paragraph = "What we claim corresponds with what (Foppiano and al. 2021) explains what he's thinking.";

        List<String> refs = Arrays.asList("(Foppiano and al. 2021)");
        List<String> sentences = Arrays
                .asList("What we claim corresponds with what (Foppiano and al.", "2021) explains what he's thinking.");

        List<OffsetPosition> refSpans = getPositions(paragraph, refs);
        List<OffsetPosition> sentenceSpans = getPositions(paragraph, sentences);

        when(sentenceDetectorFactoryMock.getInstance()).thenReturn(sentenceDetectorMock);
        when(sentenceDetectorMock.detect(paragraph, null)).thenReturn(sentenceSpans);

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(paragraph, refSpans);

        verify(sentenceDetectorMock).detect(paragraph, null);
        assertThat(theSentences.size(), is(1));
    }

    private List<OffsetPosition> getPositions(String paragraph, List<String> refs) {
        List<OffsetPosition> positions = new ArrayList<>();
        int previousRefEnd = 0;
        for (String ref : refs) {
            int startRef = paragraph.indexOf(ref, previousRefEnd);
            int endRef = startRef + ref.length();

            positions.add(new OffsetPosition(startRef, endRef));
            previousRefEnd = endRef;
        }

        return positions;
    }
}
