package org.grobid.core.utilities;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.grobid.core.lang.Language;
import org.grobid.core.lang.SentenceDetector;
import org.grobid.core.lang.SentenceDetectorFactory;

/**
 * Tests for {@link SentenceUtilities#runSentenceDetection}.
 *
 * <p>Rewritten without PowerMock, which fails to initialise under JDK 17 (the reason the previous version
 * was {@code @Ignore}d). Instead of mocking statics, a hand-written fake {@link SentenceDetector} returns
 * canned sentence offsets and a fake {@link SentenceDetectorFactory} hands it out. The fake factory is
 * injected into the {@link SentenceUtilities} singleton's private {@code sdf} field by plain reflection
 * and the original factory is restored after each test, so no other test is affected by the singleton
 * mutation.
 */
public class SentenceUtilitiesTest {

    private SentenceDetectorFactory originalFactory;
    private FakeSentenceDetectorFactory fakeFactory;

    /** Fake detector: returns whatever offsets the test sets, regardless of input. */
    private static class FakeSentenceDetector implements SentenceDetector {
        List<OffsetPosition> toReturn = new ArrayList<>();

        @Override
        public List<OffsetPosition> detect(String text) {
            return toReturn;
        }

        @Override
        public List<OffsetPosition> detect(String text, Language lang) {
            return toReturn;
        }
    }

    private static class FakeSentenceDetectorFactory implements SentenceDetectorFactory {
        final FakeSentenceDetector detector = new FakeSentenceDetector();

        @Override
        public SentenceDetector getInstance() {
            return detector;
        }
    }

    private static Field sdfField() throws Exception {
        Field f = SentenceUtilities.class.getDeclaredField("sdf");
        f.setAccessible(true);
        return f;
    }

    @BeforeEach
    public void setUp() throws Exception {
        GrobidProperties.getInstance();
        SentenceUtilities target = SentenceUtilities.getInstance();
        Field f = sdfField();
        originalFactory = (SentenceDetectorFactory) f.get(target);
        fakeFactory = new FakeSentenceDetectorFactory();
        f.set(target, fakeFactory);
    }

    @AfterEach
    public void tearDown() throws Exception {
        // restore the real factory so the shared singleton is left untouched for other test classes
        sdfField().set(SentenceUtilities.getInstance(), originalFactory);
    }

    /** Set the sentence offsets the fake detector will return for the next call. */
    private void givenDetected(List<OffsetPosition> spans) {
        fakeFactory.detector.toReturn = spans;
    }

    @Test
    public void testNullText() {
        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(null);
        assertThat(theSentences, is(nullValue()));
    }

    @Test
    public void testEmptyText() {
        givenDetected(new ArrayList<>());
        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection("");
        assertThat(theSentences.size(), is(0));
    }

    @Test
    public void testOneSentenceText() {
        String text = "Bla bla bla.";
        givenDetected(Arrays.asList(new OffsetPosition(0, 12)));
        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(text);
        assertThat(theSentences.size(), is(1));
    }

    @Test
    public void testTwoSentencesText() {
        String text = "Bla bla bla. Bli bli bli.";
        givenDetected(Arrays.asList(new OffsetPosition(0, 12), new OffsetPosition(13, 25)));
        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(text);
        assertThat(theSentences.size(), is(2));
    }

    @Test
    public void testTwoSentencesTextWithUselessForbidden() {
        String text = "Bla bla bla. Bli bli bli.";
        List<OffsetPosition> forbidden = new ArrayList<>();
        forbidden.add(new OffsetPosition(2, 8));
        givenDetected(Arrays.asList(new OffsetPosition(0, 12), new OffsetPosition(13, 25)));

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(text, forbidden);
        assertThat(theSentences.size(), is(2));
    }

    @Test
    public void testTwoSentencesTextWithUsefullForbidden() {
        String text = "Bla bla bla. Bli bli bli.";
        List<OffsetPosition> forbidden = new ArrayList<>();
        forbidden.add(new OffsetPosition(2, 8));
        forbidden.add(new OffsetPosition(9, 15)); // straddles the boundary between the two sentences
        givenDetected(Arrays.asList(new OffsetPosition(0, 12), new OffsetPosition(13, 25)));

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(text, forbidden);
        assertThat(theSentences.size(), is(1));
    }

    @Test
    public void testCorrectSegmentation_shouldNotCancelSegmentation() {
        String paragraph = "This is a sentence. [3] Another sentence.";
        List<OffsetPosition> refSpans = getPositions(paragraph, Arrays.asList("[3]"));
        givenDetected(getPositions(paragraph, Arrays.asList("This is a sentence.", "Another sentence.")));

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(paragraph, refSpans);
        assertThat(theSentences.size(), is(2));
    }

    @Test
    public void testCorrectSegmentation_shouldNotCancelSegmentation2() {
        String paragraph = "This is a sentence [3] and the continuing sentence.";
        List<OffsetPosition> refSpans = getPositions(paragraph, Arrays.asList("[3]"));
        givenDetected(getPositions(paragraph, Arrays.asList("This is a sentence", "and the continuing sentence.")));

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(paragraph, refSpans);
        assertThat(theSentences.size(), is(2));
    }

    @Test
    public void testCorrectSegmentation_shouldCancelWrongSegmentation() {
        String paragraph = "(Foppiano and al. 2021) explains what he's thinking.";
        List<OffsetPosition> refSpans = getPositions(paragraph, Arrays.asList("(Foppiano and al. 2021)"));
        givenDetected(getPositions(paragraph, Arrays.asList("(Foppiano and al.", "2021) explains what he's thinking.")));

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(paragraph, refSpans);
        assertThat(theSentences.size(), is(1));
    }

    @Test
    public void testCorrectSegmentation_shouldCancelWrongSegmentation2() {
        String paragraph = "What we claim corresponds with what (Foppiano and al. 2021) explains what he's thinking.";
        List<OffsetPosition> refSpans = getPositions(paragraph, Arrays.asList("(Foppiano and al. 2021)"));
        givenDetected(getPositions(
                paragraph,
                Arrays.asList("What we claim corresponds with what (Foppiano and al.", "2021) explains what he's thinking.")));

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(paragraph, refSpans);
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
