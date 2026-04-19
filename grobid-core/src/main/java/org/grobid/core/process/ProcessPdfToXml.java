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
            // ── What ProcessBuilder is ──────────────────────────────────────────
            // ProcessBuilder is Java's way of launching an external program (a
            // "child process") from inside a Java application. Here it launches
            // pdfalto — the tool that converts a PDF's visual layout into XML
            // tokens that GROBID's machine-learning models can read.
            //
            // You give ProcessBuilder a list of strings: the first string is the
            // program to run (e.g., "pdfalto.exe"), and the rest are its arguments
            // (e.g., the input PDF path, the output XML path, flags like
            // "-fullFontName"). ProcessBuilder then starts that program, and Java
            // can read its output and check whether it succeeded or failed.
            // ─────────────────────────────────────────────────────────────────────
            builder = new ProcessBuilder(cmd);

            // ── WINDOWS SUPPORT: inject DLL directory into PATH ─────────────────
            // Why this is needed:
            //   When a program starts on Windows, the operating system needs to
            //   find all its DLLs (Dynamic Link Libraries). DLLs are files
            //   containing shared code that a program depends on at runtime — think
            //   of them as toolboxes the program reaches into while it runs. On
            //   Linux, the equivalent files are called ".so" (shared object) files,
            //   and Linux finds them using a built-in search system called "ld.so"
            //   that reads configuration files and standard directories like
            //   /usr/lib. Windows has no equivalent automatic system — instead, it
            //   searches the PATH environment variable (the same list of folders
            //   your command prompt searches when you type a command name).
            //
            //   pdfalto.exe was compiled with MinGW (a toolchain that lets you
            //   build Linux-style C++ code on Windows). MinGW programs need several
            //   runtime DLLs (like libstdc++-6.dll, libgcc_s_seh-1.dll, etc.) to
            //   be findable at launch time. These DLLs are placed in the same
            //   folder as pdfalto.exe.
            //
            // What this block does:
            //   1. Checks if we are running on Windows.
            //   2. Finds the folder where pdfalto.exe lives (its "parent" folder).
            //   3. Adds that folder to the front of the PATH environment variable
            //      for the child process only (it does not change PATH globally).
            //   This ensures Windows can find pdfalto's DLLs when it starts.
            //
            // Why ";" (semicolon):
            //   On Windows, folders in PATH are separated by semicolons.
            //   On Linux, they are separated by colons.
            // ─────────────────────────────────────────────────────────────────────
            if (org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS && cmd.size() > 0) {
                java.io.File exeFile = new java.io.File(cmd.get(0));
                if (exeFile.exists()) {
                    String dllDir = exeFile.getParent();
                    java.util.Map<String, String> env = builder.environment();
                    String path = env.getOrDefault("PATH", env.getOrDefault("Path", ""));
                    if (!path.contains(dllDir)) {
                        env.put("PATH", dllDir + ";" + path);
                    }
                }
            }
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
                    LOGGER.error("pdfalto process finished with error code: "
                            + exit + ". " + cmd);
                    LOGGER.error("pdfalto return message: \n" + message);
                }
            }
        }
        return exit;
    }

}
