package org.grobid.core.process;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ProcessPdfToXml {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPdfToXml.class);

    /**
     * Process the conversion.
     */
    public static Integer process(List<String> cmd) {
        Process process = null;
        ProcessBuilder builder = null;
        Integer exit = null;
        String message = "error message cannot be retrieved";
        try {
            builder = new ProcessBuilder(cmd);
            builder.redirectErrorStream(true);
            process = builder.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = null;
            String previousOutput = null;
            while (null != (output = br.readLine())) {
                // writing the pdfalto stderr in the GROBID logs as warning
                if (!output.equals(previousOutput)) {
                    LOGGER.warn("pdfalto stderr: " + output);
                    previousOutput = output;
                }
            }
            exit = process.waitFor();
            message = IOUtils.toString(process.getErrorStream(), UTF_8);

        } catch (InterruptedException ignore) {
            // Process needs to be destroyed -- it's done in the finally block
            LOGGER.warn("pdfalto process is about to be killed.");
        } catch (IOException ioExp) {
            LOGGER.error("IOException while launching the command {} : {}", cmd, ioExp.getMessage());
        } finally {
            if (process != null) {
                IOUtils.closeQuietly(process.getInputStream(), process.getOutputStream(), process.getErrorStream());

                process.destroy();

                if (exit == null || exit != 0) {
                    LOGGER.error("pdfalto process finished with error code: " + exit + ". " + cmd);
                    LOGGER.error("pdfalto return message: \n" + message);
                }
            }
        }
        return exit;
    }

}
