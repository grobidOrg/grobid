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
package org.grobid.service.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.grobid.core.utilities.IOUtilities;

public class ZipUtils {

    public static InputStream decompressStream(InputStream input) throws Exception {
        PushbackInputStream pb = new PushbackInputStream(input, 2); //we need a pushbackstream to look ahead
        byte[] signature = new byte[2];
        try {
            pb.read(signature); //read the signature
            pb.unread(signature); //push back the signature to the stream
        } catch (Exception e) {

        }
        if (signature[0] == (byte) 0x1f && signature[1] == (byte) 0x8b) //check if matches standard gzip maguc number
            return new GZIPInputStream(pb);
        else
            return pb;
    }

    private static void copyInputStream(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[1024];
        int len;

        while ((len = in.read(buffer)) >= 0)
            out.write(buffer, 0, len);
    }

    public static final void main(String[] args) {
        Enumeration entries;

        if (args.length != 1) {
            System.err.println("Usage: Unzip zipfile");
            return;
        }

        String pPath = args[0];
        try (ZipFile zipFile = new ZipFile(pPath)) {
            entries = zipFile.entries();

            File tempDir = IOUtilities.newTempFile(
                    "GROBID",
                    Long.toString(System.nanoTime()));
            if (!(tempDir.delete())) {
                throw new IOException("Could not delete temp file: "
                        + tempDir.getAbsolutePath());
            }
            if (!(tempDir.mkdir())) {
                throw new IOException("Could not create temp directory: "
                        + tempDir.getAbsolutePath());
            }

            File canonicalTempDir = tempDir.getCanonicalFile();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();

                // Resolve and validate the output path before any filesystem
                // operation to prevent path traversal (Zip Slip).
                File outFile = new File(canonicalTempDir.getAbsolutePath() + File.separator + entry.getName())
                        .getCanonicalFile();
                if (!outFile.toPath().startsWith(canonicalTempDir.toPath())) {
                    throw new IOException("Bad zip entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    // Assume directories are stored parents first then
                    // children.
                    System.err.println(
                            "Extracting directory: "
                                    + entry.getName());
                    // This is not robust, just for demonstration purposes.
                    if (!outFile.mkdir() && !outFile.isDirectory()) {
                        throw new IOException("Could not create directory: " + outFile.getAbsolutePath());
                    }
                    continue;
                }

                System.err.println("Extracting file: " + entry.getName());

                try (InputStream in = zipFile.getInputStream(entry);
                        FileOutputStream fos = new FileOutputStream(outFile);
                        OutputStream out = new BufferedOutputStream(fos)) {
                    copyInputStream(in, out);
                }
            }
        } catch (IOException ioe) {
            System.err.println("Unhandled exception:");
            ioe.printStackTrace();
            return;
        }
    }

}
