package org.grobid.core.process;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class ProcessRunnerTest {

    @Test
    public void sumRssKb_shouldSumOneValuePerLine() {
        // Typical `ps -o rss= -p <pids>` output: one RSS value (KB) per line, leading spaces.
        String psOutput = "  1024\n  2048\n  512\n";

        assertThat(ProcessRunner.sumRssKb(psOutput), is(3584L));
    }

    @Test
    public void sumRssKb_shouldIgnoreBlankAndNonNumericLines() {
        // `-o rss=` suppresses the header, but be defensive against a stray header / blank lines.
        String psOutput = "RSS\n\n  4096  \n   \nnot-a-number\n100\n";

        assertThat(ProcessRunner.sumRssKb(psOutput), is(4196L));
    }

    @Test
    public void sumRssKb_shouldReturnZeroForNullOrEmpty() {
        assertThat(ProcessRunner.sumRssKb(null), is(0L));
        assertThat(ProcessRunner.sumRssKb(""), is(0L));
        assertThat(ProcessRunner.sumRssKb("   \n  \n"), is(0L));
    }

    @Test
    public void readRssKb_shouldReturnZeroForNoPids() {
        assertThat(ProcessRunner.readRssKb(null), is(0L));
        assertThat(ProcessRunner.readRssKb(Collections.emptyList()), is(0L));
    }

    @Test
    public void readRssKb_shouldReportPositiveRssForTheCurrentJvm() {
        // The current JVM process certainly has a resident set; ps must return a positive total.
        long ownPid = ProcessHandle.current().pid();

        long rss = ProcessRunner.readRssKb(Arrays.asList(ownPid));

        assertThat(rss > 0L, is(true));
    }

    @Test
    public void currentRssKb_shouldBePositiveWhileChildAliveAndZeroAfterKill() throws Exception {
        // 'sleep' is universally available on the Unix CI/dev environments this code targets.
        ProcessRunner worker = new ProcessRunner(Arrays.asList("sleep", "5"), "rss-test", true);
        worker.start();
        try {
            Thread.sleep(400); // let the child be scheduled
            long rssAlive = worker.currentRssKb();
            assertThat("a live child must report positive RSS", rssAlive > 0L, is(true));

            worker.killProcess();
            Thread.sleep(400);
            assertThat("a killed child reports 0 RSS", worker.currentRssKb(), is(0L));
        } finally {
            worker.killProcess();
            worker.interrupt();
        }
    }
}
