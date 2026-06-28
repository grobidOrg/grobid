package org.grobid.core.jni;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.base.Throwables;
import fr.limsi.wapiti.SWIGTYPE_p_mdl_t;
import fr.limsi.wapiti.Wapiti;

import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidExceptionStatus;

public class WapitiWrapper {
    public static String label(SWIGTYPE_p_mdl_t model, String data) {
        if (data.trim().isEmpty()) {
            System.err.println(
                    "Empty data is provided to Wapiti tagger: " + Throwables.getStackTraceAsString(new Throwable()));
            return "";
        }

        String result = Wapiti.labelFromModel(model, data);
        if (result == null) {
            throw new GrobidException(
                    "Wapiti tagging failed (null data returned) - Possibly mismatch between grobid-home and grobid-core",
                    GrobidExceptionStatus.TAGGING_ERROR);
        }
        return result;
    }

    public static SWIGTYPE_p_mdl_t getModel(File model) {
        return getModel(model, false);
    }

    public static SWIGTYPE_p_mdl_t getModel(File model, boolean checkLabels) {
        if (!model.getAbsolutePath().contains(" ")) {
            return Wapiti.loadModel("label " + (checkLabels ? "--check" : "") + " -m " + model.getAbsolutePath());
        }
        // The Wapiti JNI argument parser splits on whitespace, so paths with spaces
        // must be accessed via a temporary symlink with a space-free path.
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("grobid-wapiti");
            Path link = createSafeSymlink(model.toPath(), tempDir);
            // Wapiti.loadModel() reads the model file fully into memory before
            // returning, so the symlink (and temp dir) can be safely deleted once
            // the call returns.
            return Wapiti.loadModel("label " + (checkLabels ? "--check" : "") + " -m " + link.toAbsolutePath());
        } catch (IOException e) {
            throw new GrobidException("Failed to create safe path for model: " + model.getAbsolutePath(), e);
        } finally {
            deleteTempDir(tempDir);
        }
    }

    /**
     * Creates a symbolic link inside {@code tempDir} whose name contains no spaces,
     * pointing to the absolute path of {@code target}. If a link with the safe name
     * already exists, a numeric suffix is appended to avoid collisions.
     */
    static Path createSafeSymlink(Path target, Path tempDir) throws IOException {
        String name = target.getFileName().toString();
        String safeName = name.replace(" ", "_");
        int dotIdx = safeName.lastIndexOf('.');
        String base = dotIdx >= 0 ? safeName.substring(0, dotIdx) : safeName;
        String ext = dotIdx >= 0 ? safeName.substring(dotIdx) : "";

        Path link = tempDir.resolve(safeName);
        int counter = 1;
        while (Files.exists(link)) {
            link = tempDir.resolve(base + "_" + counter++ + ext);
        }
        Files.createSymbolicLink(link, target.toAbsolutePath());
        return link;
    }

    /**
     * Deletes all contents inside {@code tempDir} (including nested structures) and then the
     * directory itself, suppressing any {@link IOException}.
     */
    static void deleteTempDir(Path tempDir) {
        if (tempDir == null) {
            return;
        }
        try {
            Files.walk(tempDir)
                 .sorted(java.util.Comparator.reverseOrder())
                 .forEach(path -> {
                     try {
                         Files.deleteIfExists(path);
                     } catch (IOException e) {
                         // best-effort cleanup; ignore
                     }
                 });
        } catch (IOException e) {
            // best-effort cleanup; ignore
        }
    }

}
