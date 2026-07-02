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
package org.grobid.core.process;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;

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

    // since we are limiting by ulimit, pdftoxml is actually a child process, therefore Process.destroy() won't work
    // killing harshly with pkill
    public void killProcess() {
        if (process != null) {
            try {
                Long pid = getPidOfProcess(process);
                if (pid != null) {
                    LOGGER.info("Killing pdf to xml process with PID " + pid + " and its children");
                    Runtime.getRuntime().exec(new String[]{"pkill", "-9", "-P", String.valueOf(pid)}).waitFor();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    //WARNING
    public static Long getPidOfProcess(Process p) {
        Long pid = null;

        try {
            if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                pid = f.getLong(p);
                f.setAccessible(false);
            }
        } catch (Exception e) {
            pid = null;
        }
        return pid;
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
