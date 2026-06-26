package org.grobid.core.process;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessRunner extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessRunner.class);

    private List<String> cmd;
    private Integer exit;
    private Process process;

    public String getErrorStreamContents() {
        return errorStreamContents;
    }

    private String errorStreamContents;

    private boolean useStreamGobbler;
    StreamGobbler sgIn;
    StreamGobbler sgErr;

    public ProcessRunner(List<String> cmd, String name, boolean useStreamGobbler) {
        super(name);
        this.cmd = cmd;
        this.useStreamGobbler = useStreamGobbler;
    }

    // Under the bash ulimit wrapper (Linux), pdfalto is a grand-child process, so Process.destroy()
    // on the bash process is not enough: we forcibly kill the whole descendant tree as well.
    public void killProcess() {
        if (process != null) {
            try {
                LOGGER.info("Killing pdf to xml process with PID " + process.pid() + " and its descendants");
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Long getPidOfProcess(Process p) {
        try {
            return p.pid();
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    /**
     * Resident memory (RSS) currently used by the managed process and all of its descendants,
     * in kilobytes. Used to enforce a memory ceiling on platforms where the shell {@code ulimit}
     * cannot (notably macOS, where {@code ulimit -Sv} caps virtual address space and is unusable).
     *
     * @return total RSS in KB, or 0 if it cannot be determined (no process, or {@code ps} unavailable).
     */
    public long currentRssKb() {
        if (process == null || !process.isAlive()) {
            return 0L;
        }
        List<Long> pids = new ArrayList<>();
        pids.add(process.pid());
        process.descendants().forEach(h -> pids.add(h.pid()));
        return readRssKb(pids);
    }

    /**
     * Read the summed RSS (KB) of the given pids via {@code ps -o rss= -p <pids>}.
     * Works on macOS (BSD ps) and Linux. Returns 0 on any error.
     */
    protected static long readRssKb(List<Long> pids) {
        if (pids == null || pids.isEmpty()) {
            return 0L;
        }
        String pidList = pids.stream().map(String::valueOf).collect(Collectors.joining(","));
        try {
            Process ps = new ProcessBuilder("ps", "-o", "rss=", "-p", pidList)
                    .redirectErrorStream(false)
                    .start();
            String output = IOUtils.toString(ps.getInputStream(), StandardCharsets.UTF_8);
            ps.waitFor();
            return sumRssKb(output);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Sum the RSS values (one per line, in KB) printed by {@code ps -o rss=}. Blank/non-numeric
     * lines are ignored, so the helper is safe to unit-test with raw {@code ps} output.
     */
    protected static long sumRssKb(String psOutput) {
        if (psOutput == null) {
            return 0L;
        }
        long total = 0L;
        for (String line : psOutput.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                total += Long.parseLong(trimmed);
            } catch (NumberFormatException ignore) {
                // header or unexpected line, skip
            }
        }
        return total;
    }

    public void run() {
        process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(cmd);
            process = builder.start();

            if (useStreamGobbler) {
                sgIn = new StreamGobbler(process.getInputStream());
                sgErr = new StreamGobbler(process.getErrorStream());
            }

            exit = process.waitFor();
        } catch (InterruptedException ignore) {
            //Process needs to be destroyed -- it's done in the finally block
        } catch (IOException e) {
            LOGGER.error("IOException while launching the command {} : {}", cmd.toString(), e.getMessage());
        } finally {
            if (process != null) {
                IOUtils.closeQuietly(process.getInputStream());
                IOUtils.closeQuietly(process.getOutputStream());
                try {
                    errorStreamContents = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    LOGGER.error("Error retrieving error stream from process: ", e);
                }
                IOUtils.closeQuietly(process.getErrorStream());

                process.destroy();

            }

            if (useStreamGobbler) {
                try {
                    if (sgIn != null) {
                        sgIn.close();
                    }
                } catch (IOException e) {
                    LOGGER.error("IOException while closing the stream gobbler: {}", e);
                }

                try {
                    if (sgErr != null) {
                        sgErr.close();
                    }
                } catch (IOException e) {
                    LOGGER.error("IOException while closing the stream gobbler: {}", e);
                }
            }
        }

    }

    public Integer getExitStatus() {
        return exit;
    }
}
