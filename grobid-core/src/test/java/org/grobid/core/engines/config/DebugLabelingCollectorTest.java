package org.grobid.core.engines.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.grobid.core.GrobidModel;

public class DebugLabelingCollectorTest {

    private static GrobidModel stubModel(final String name) {
        return new GrobidModel() {
            @Override
            public String getFolderName() {
                return name;
            }

            @Override
            public String getModelPath() {
                return name;
            }

            @Override
            public String getModelName() {
                return name;
            }

            @Override
            public String getTemplateName() {
                return name + ".template";
            }
        };
    }

    @Test
    public void recordsEntriesUnderCanonicalModelName() {
        DebugLabelingCollector collector = new DebugLabelingCollector();
        collector.record(stubModel("header"), "tok\tfeat\tI-<title>\n");

        Map<String, List<String>> snapshot = collector.snapshot();
        assertThat(snapshot.size(), is(1));
        assertThat(snapshot.get("header").size(), is(1));
        assertThat(snapshot.get("header").get(0), is("tok\tfeat\tI-<title>\n"));
    }

    @Test
    public void preservesInsertionOrderAcrossKeys() {
        DebugLabelingCollector collector = new DebugLabelingCollector();
        collector.record(stubModel("segmentation"), "a");
        collector.record(stubModel("header"), "b");
        collector.record(stubModel("citation"), "c");

        List<String> keys = collector.modelKeys().stream().toList();
        assertThat(keys.get(0), is("segmentation"));
        assertThat(keys.get(1), is("header"));
        assertThat(keys.get(2), is("citation"));
    }

    @Test
    public void multipleRecordsUnderSameKeyBecomeAList() {
        DebugLabelingCollector collector = new DebugLabelingCollector();
        GrobidModel date = stubModel("date");
        collector.record(date, "first");
        collector.record(date, "second");
        collector.record(date, "third");

        List<String> dateEntries = collector.snapshot().get("date");
        assertThat(dateEntries.size(), is(3));
        assertThat(dateEntries.get(0), is("first"));
        assertThat(dateEntries.get(1), is("second"));
        assertThat(dateEntries.get(2), is("third"));
    }

    @Test
    public void nullInputsAreIgnored() {
        DebugLabelingCollector collector = new DebugLabelingCollector();
        collector.record(null, "ignored");
        collector.record(stubModel("header"), null);

        assertThat(collector.isEmpty(), is(true));
        assertThat(collector.snapshot().get("header"), is(nullValue()));
    }

    @Test
    public void snapshotIsImmutable() {
        DebugLabelingCollector collector = new DebugLabelingCollector();
        collector.record(stubModel("header"), "x");
        Map<String, List<String>> snapshot = collector.snapshot();

        try {
            snapshot.put("hacked", List.of("y"));
            throw new AssertionError("snapshot map should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void concurrentRecordingPreservesAllEntries() throws Exception {
        final int threads = 8;
        final int recordsPerThread = 250;
        final DebugLabelingCollector collector = new DebugLabelingCollector();
        final GrobidModel model = stubModel("citation");
        final CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                final int threadId = t;
                pool.submit(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int i = 0; i < recordsPerThread; i++) {
                        collector.record(model, "t" + threadId + "-" + i);
                    }
                });
            }
            start.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS), is(true));
        } finally {
            pool.shutdownNow();
        }

        assertThat(collector.snapshot().get("citation").size(), is(threads * recordsPerThread));
    }
}
