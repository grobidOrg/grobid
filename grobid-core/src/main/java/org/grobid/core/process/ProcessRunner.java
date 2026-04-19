package org.grobid.core.process;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

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
			// ── What ProcessBuilder is ──────────────────────────────────────
			// ProcessBuilder is Java's built-in way to launch an external
			// program (a "child process"). Here, it is used to launch pdfalto —
			// the tool that converts a PDF's visual layout into XML tokens for
			// GROBID's machine-learning models. The "cmd" list contains the
			// program path followed by its command-line arguments. Java starts
			// the program, waits for it to finish, and reads its exit code.
			//
			// This class (ProcessRunner) wraps that launch inside a Java Thread
			// so GROBID can enforce a timeout: if pdfalto takes too long on a
			// single PDF, the calling code can kill the thread and move on.
			// ────────────────────────────────────────────────────────────────
			ProcessBuilder builder = new ProcessBuilder(cmd);

			// ── WINDOWS SUPPORT: inject DLL directory into PATH ────────────
			// Why this is needed:
			//   When Windows starts a program, it must find all DLLs (Dynamic
			//   Link Libraries) that program depends on. DLLs are shared code
			//   files — like toolboxes containing functions the program calls
			//   at runtime. On Linux, the equivalent ".so" (shared object)
			//   files are found automatically by a system called "ld.so" that
			//   reads /etc/ld.so.conf and standard directories like /usr/lib.
			//   Windows has no such automatic system — it searches the folders
			//   listed in the PATH environment variable instead.
			//
			//   pdfalto.exe was built with MinGW, a toolchain that compiles
			//   Linux-style C++ code on Windows. MinGW programs need runtime
			//   DLLs (like libstdc++-6.dll, libgcc_s_seh-1.dll, etc.) to be
			//   on PATH or in the same folder as the .exe. These DLLs are
			//   shipped alongside pdfalto.exe in GROBID's pdfalto directory.
			//
			// What this block does:
			//   1. Checks if we are running on Windows.
			//   2. Finds the folder containing pdfalto.exe (its parent folder).
			//   3. Prepends that folder to the PATH for the child process only
			//      (global PATH is not changed).
			//   This ensures Windows can locate pdfalto's DLLs at launch time.
			//
			// Why ";" (semicolon):
			//   PATH entries on Windows are separated by semicolons, not colons.
			//
			// The System.err.println lines below are diagnostic logs that print
			// to the console during startup — useful for debugging if pdfalto
			// fails to launch on a new Windows machine.
			// ────────────────────────────────────────────────────────────────
			if (org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS && cmd.size() > 0) {
				java.io.File exeFile = new java.io.File(cmd.get(0));
				// Diagnostic log for Windows pdfalto debugging
				System.err.println("[ProcessRunner] Windows exe: " + cmd.get(0) + " exists=" + exeFile.exists());
				System.err.println("[ProcessRunner] Full cmd: " + cmd);
				if (exeFile.exists()) {
					String dllDir = exeFile.getParent();
					java.util.Map<String, String> env = builder.environment();
					String path = env.getOrDefault("PATH", env.getOrDefault("Path", ""));
					if (!path.contains(dllDir)) {
						env.put("PATH", dllDir + ";" + path);
					}
					System.err.println("[ProcessRunner] PATH set: " + dllDir);
				}
			}
			process = builder.start();
			
            if (useStreamGobbler) {
                sgIn = new StreamGobbler(process.getInputStream());
                sgErr = new StreamGobbler(process.getErrorStream());
            }

            exit = process.waitFor();
        } 
		catch (InterruptedException ignore) {
            //Process needs to be destroyed -- it's done in the finally block
        } 
		catch (IOException e) {
            LOGGER.error("IOException while launching the command {} : {}", cmd.toString(), e.getMessage());
        } 
		finally {
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
                } 
				catch (IOException e) {
                    LOGGER.error("IOException while closing the stream gobbler: {}", e);
                }

                try {
                    if (sgErr != null) {
                        sgErr.close();
                    }
                } 
				catch (IOException e) {
                    LOGGER.error("IOException while closing the stream gobbler: {}", e);
                }
            }
        }

    }

    public Integer getExitStatus() {
        return exit;
    }
}
