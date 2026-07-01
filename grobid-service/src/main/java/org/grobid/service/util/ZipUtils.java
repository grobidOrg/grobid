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

    public static final void copyInputStream(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[1024];
        int len;

        while ((len = in.read(buffer)) >= 0)
            out.write(buffer, 0, len);

        in.close();
        out.close();
    }

    public static final void main(String[] args) {
        Enumeration entries;
        ZipFile zipFile;

        if (args.length != 1) {
            System.err.println("Usage: Unzip zipfile");
            return;
        }

        String pPath = args[0];
        try {
            zipFile = new ZipFile(pPath);

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

            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();

                if (entry.isDirectory()) {
                    // Assume directories are stored parents first then
                    // children.
                    System.err.println(
                            "Extracting directory: "
                                    + entry.getName());
                    // This is not robust, just for demonstration purposes.

                    File dir = new File(tempDir.getAbsolutePath() + File.separator + entry.getName())
                            .getCanonicalFile();
                    if (!dir.toPath().startsWith(tempDir.toPath())) {
                        throw new IOException("Bad zip entry: " + entry.getName());
                    }
                    dir.mkdir();
                    continue;
                }

                System.err.println("Extracting file: " + entry.getName());

                copyInputStream(
                        zipFile.getInputStream(entry),
                        new BufferedOutputStream(new FileOutputStream(new File(tempDir
                                .getAbsolutePath()
                                + File.separator
                                + entry.getName()).getCanonicalFile())));
                File outFile = new File(tempDir.getAbsolutePath() + File.separator + entry.getName())
                        .getCanonicalFile();
                if (!outFile.toPath().startsWith(tempDir.toPath())) {
                    throw new IOException("Bad zip entry: " + entry.getName());
                }
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(outFile)));
            }

            zipFile.close();
        } catch (IOException ioe) {
            System.err.println("Unhandled exception:");
            ioe.printStackTrace();
            return;
        }
    }

}
