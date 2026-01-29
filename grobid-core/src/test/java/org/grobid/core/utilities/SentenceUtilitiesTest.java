package org.grobid.core.utilities;

import org.grobid.core.lang.SentenceDetector;
import org.grobid.core.lang.SentenceDetectorFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.*;
import org.junit.Ignore;

// Patrice @Luca this class is failing to run with JDK 1.17 and maybe lower versions (not tried), possibly security reasons,
// and I am not able to understand why with the complexity introduced by powermock in initialization.
// Could we move back to something simpler and readable maybe?

@Ignore
@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.grobid.core.lang.SentenceDetectorFactory")
@PrepareForTest({SentenceUtilities.class})
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

        sentenceDetectorFactoryMock = createMock(SentenceDetectorFactory.class);
        sentenceDetectorMock = createMock(SentenceDetector.class);
        target = SentenceUtilities.getInstance();
        Whitebox.setInternalState(target, sentenceDetectorFactoryMock);
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
        expect(sentenceDetectorFactoryMock.getInstance()).andReturn(sentenceDetectorMock);
        expect(sentenceDetectorMock.detect(text)).andReturn(new ArrayList<>());
        replay(sentenceDetectorFactoryMock, sentenceDetectorMock);
        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(text);

        verify(sentenceDetectorFactoryMock, sentenceDetectorMock);
        assertThat(theSentences.size(), is(0));
    }

    @Test
    public void testOneSentenceText() throws Exception {
        String text = "Bla bla bla.";
        expect(sentenceDetectorFactoryMock.getInstance()).andReturn(sentenceDetectorMock);
        expect(sentenceDetectorMock.detect(text)).andReturn(Arrays.asList(new OffsetPosition(0, 12)));
        replay(sentenceDetectorFactoryMock, sentenceDetectorMock);

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(text);
        verify(sentenceDetectorFactoryMock, sentenceDetectorMock);
        assertThat(theSentences.size(), is(1));
    }

    @Test
    public void testTwoSentencesText() throws Exception {
        String text = "Bla bla bla. Bli bli bli.";
        expect(sentenceDetectorFactoryMock.getInstance()).andReturn(sentenceDetectorMock);
        expect(sentenceDetectorMock.detect(text))
                .andReturn(Arrays.asList(new OffsetPosition(0, 12), new OffsetPosition(13, 24)));
        replay(sentenceDetectorFactoryMock, sentenceDetectorMock);

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(text);
        verify(sentenceDetectorFactoryMock, sentenceDetectorMock);
        assertThat(theSentences.size(), is(2));
    }

    @Test
    public void testTwoSentencesTextWithUselessForbidden() throws Exception {
        String text = "Bla bla bla. Bli bli bli.";
        List<OffsetPosition> forbidden = new ArrayList<>();
        forbidden.add(new OffsetPosition(2, 8));

        expect(sentenceDetectorFactoryMock.getInstance()).andReturn(sentenceDetectorMock);
        expect(sentenceDetectorMock.detect(text, null))
                .andReturn(Arrays.asList(new OffsetPosition(0, 12), new OffsetPosition(13, 24)));
        replay(sentenceDetectorFactoryMock, sentenceDetectorMock);

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(text, forbidden);
        verify(sentenceDetectorFactoryMock, sentenceDetectorMock);
        assertThat(theSentences.size(), is(2));
    }

    @Test
    public void testTwoSentencesTextWithUsefullForbidden() throws Exception {
        String text = "Bla bla bla. Bli bli bli.";
        List<OffsetPosition> forbidden = new ArrayList<>();
        forbidden.add(new OffsetPosition(2, 8));
        forbidden.add(new OffsetPosition(9, 15));

        expect(sentenceDetectorFactoryMock.getInstance()).andReturn(sentenceDetectorMock);
        expect(sentenceDetectorMock.detect(text, null))
                .andReturn(Arrays.asList(new OffsetPosition(0, 12), new OffsetPosition(13, 24)));
        replay(sentenceDetectorFactoryMock, sentenceDetectorMock);

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(text, forbidden);

        verify(sentenceDetectorFactoryMock, sentenceDetectorMock);
        assertThat(theSentences.size(), is(1));
    }

    @Test
    public void testCorrectSegmentation_shouldNotCancelSegmentation() throws Exception {
        String paragraph = "This is a sentence. [3] Another sentence.";

        List<String> refs = Arrays.asList("[3]");
        List<String> sentences = Arrays.asList("This is a sentence.", "Another sentence.");

        List<OffsetPosition> refSpans = getPositions(paragraph, refs);
        List<OffsetPosition> sentenceSpans = getPositions(paragraph, sentences);

        expect(sentenceDetectorFactoryMock.getInstance()).andReturn(sentenceDetectorMock);
        expect(sentenceDetectorMock.detect(paragraph, null)).andReturn(sentenceSpans);
        replay(sentenceDetectorFactoryMock, sentenceDetectorMock);

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(paragraph, refSpans);
        verify(sentenceDetectorFactoryMock, sentenceDetectorMock);
        assertThat(theSentences.size(), is(2));
    }

    @Test
    public void testCorrectSegmentation_shouldNotCancelSegmentation2() throws Exception {
        String paragraph = "This is a sentence [3] and the continuing sentence.";

        List<String> refs = Arrays.asList("[3]");
        List<String> sentences = Arrays.asList("This is a sentence", "and the continuing sentence.");

        List<OffsetPosition> refSpans = getPositions(paragraph, refs);
        List<OffsetPosition> sentenceSpans = getPositions(paragraph, sentences);

        expect(sentenceDetectorFactoryMock.getInstance()).andReturn(sentenceDetectorMock);
        expect(sentenceDetectorMock.detect(paragraph, null)).andReturn(sentenceSpans);
        replay(sentenceDetectorFactoryMock, sentenceDetectorMock);

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(paragraph, refSpans);
        verify(sentenceDetectorFactoryMock, sentenceDetectorMock);
        assertThat(theSentences.size(), is(2));
    }

    @Test
    public void testCorrectSegmentation_shouldCancelWrongSegmentation() throws Exception {
        String paragraph = "(Foppiano and al. 2021) explains what he's thinking.";

        List<String> refs = Arrays.asList("(Foppiano and al. 2021)");
        List<String> sentences = Arrays.asList("(Foppiano and al.", "2021) explains what he's thinking.");

        List<OffsetPosition> refSpans = getPositions(paragraph, refs);
        List<OffsetPosition> sentenceSpans = getPositions(paragraph, sentences);

        expect(sentenceDetectorFactoryMock.getInstance()).andReturn(sentenceDetectorMock);
        expect(sentenceDetectorMock.detect(paragraph, null)).andReturn(sentenceSpans);
        replay(sentenceDetectorFactoryMock, sentenceDetectorMock);

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(paragraph, refSpans);
        verify(sentenceDetectorFactoryMock, sentenceDetectorMock);
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

        expect(sentenceDetectorFactoryMock.getInstance()).andReturn(sentenceDetectorMock);
        expect(sentenceDetectorMock.detect(paragraph, null)).andReturn(sentenceSpans);
        replay(sentenceDetectorFactoryMock, sentenceDetectorMock);

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(paragraph, refSpans);
        verify(sentenceDetectorFactoryMock, sentenceDetectorMock);
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
