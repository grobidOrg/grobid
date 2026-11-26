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
package org.grobid.trainer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Stream;

import org.grobid.core.exceptions.GrobidException;

/**
 * Removes from a Wapiti model the observations that no feature with a non-zero weight refers to,
 * the equivalent of Wapiti's --compact option applied to an already saved model. The input and the
 * output can be the same file.
 *
 * The compacted model labels exactly the same as the original one (the dropped observations never
 * contribute to a score), loads faster and is much smaller, the observation strings being the bulk
 * of a model saved without --compact.
 *
 * Model layout (as written by Wapiti): a "#mdl#type#nact" line, a "#rdr#npats/ntoks/autouni" line
 * followed by the npats patterns, a "#qrk#Y" line followed by the Y labels, a "#qrk#O" line followed
 * by the O observations ("len:string," where the string starts with u, b or * for unigram, bigram or
 * both), then one "index=weight" line per active feature. An observation owns Y features if it is a
 * unigram, then Y*Y if it is a bigram, in observation order. Lines are handled as raw bytes so that
 * everything that is kept is copied unchanged.
 */
public class WapitiModelCompactor {

    private static final String MODEL_FILE_NAME = "model.wapiti";

    private WapitiModelCompactor() {
    }

    /**
     * Compact in place the given Wapiti model files, and every model.wapiti found under the given
     * directories. Used when building the Docker images: the models are kept uncompacted in the
     * repository. Fails if no model is found.
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: WapitiModelCompactor <model file or directory>...");
        }
        List<Path> models = new ArrayList<>();
        for (String arg : args) {
            Path path = Paths.get(arg);
            if (Files.isDirectory(path)) {
                try (Stream<Path> paths = Files.walk(path)) {
                    paths.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().equals(MODEL_FILE_NAME))
                            .sorted()
                            .forEach(models::add);
                }
            } else if (Files.isRegularFile(path)) {
                models.add(path);
            } else {
                throw new GrobidException("No such Wapiti model or directory: " + path.toAbsolutePath());
            }
        }
        if (models.isEmpty()) {
            throw new GrobidException("No " + MODEL_FILE_NAME + " found under " + String.join(", ", args));
        }
        for (Path model : models) {
            compact(model.toFile(), model.toFile());
        }
    }

    /**
     * Write to output the compacted version of the Wapiti model in input. The output is first written
     * to a temporary file in the same directory, then moved in place.
     */
    public static void compact(File input, File output) {
        File tmp = new File(output.getAbsolutePath() + ".compacting");
        try {
            // first pass: kind of each observation, start of its features and the active features
            long[] starts;
            byte[] kinds;
            int[] featureObs;
            long[] featureRel;
            byte[][] featureValues;
            int nbFeatures = 0;
            BitSet used = new BitSet();
            int nbLabels;
            int nbObs;
            try (LineReader reader = new LineReader(input)) {
                reader.expectPrefix("#mdl#");
                String rdr = reader.expectPrefix("#rdr#");
                int nbPatterns = parseInt(rdr.substring(5).split("/")[0], reader);
                for (int i = 0; i < nbPatterns; i++) {
                    reader.next();
                }
                nbLabels = parseInt(reader.expectPrefix("#qrk#").substring(5), reader);
                for (int i = 0; i < nbLabels; i++) {
                    reader.next();
                }
                nbObs = parseInt(reader.expectPrefix("#qrk#").substring(5), reader);
                starts = new long[nbObs];
                kinds = new byte[nbObs];
                long size = 0;
                for (int o = 0; o < nbObs; o++) {
                    byte[] line = reader.next();
                    int colon = indexOf(line, (byte) ':');
                    if (colon < 0 || colon + 1 >= line.length) {
                        throw reader.error("malformed observation");
                    }
                    byte type = line[colon + 1];
                    byte kind = (byte) ((type == 'u' || type == '*' ? 1 : 0) | (type == 'b' || type == '*' ? 2 : 0));
                    if (kind == 0) {
                        throw reader.error("observation of unknown type");
                    }
                    kinds[o] = kind;
                    starts[o] = size;
                    size += unigramSize(kind, nbLabels) + bigramSize(kind, nbLabels);
                }

                featureObs = new int[1024];
                featureRel = new long[1024];
                featureValues = new byte[1024][];
                byte[] line;
                while ((line = reader.nextOrNull()) != null) {
                    if (new String(line, StandardCharsets.US_ASCII).trim().isEmpty()) {
                        continue;
                    }
                    int eq = indexOf(line, (byte) '=');
                    if (eq < 0) {
                        throw reader.error("malformed feature");
                    }
                    long index;
                    try {
                        index = Long.parseLong(new String(line, 0, eq, StandardCharsets.US_ASCII));
                    } catch (NumberFormatException e) {
                        throw reader.error("malformed feature index");
                    }
                    if (index < 0 || index >= size) {
                        throw reader.error("feature index out of range");
                    }
                    int pos = Arrays.binarySearch(starts, index);
                    int o = pos >= 0 ? pos : -pos - 2;
                    if (nbFeatures == featureObs.length) {
                        int capacity = featureObs.length * 2;
                        featureObs = Arrays.copyOf(featureObs, capacity);
                        featureRel = Arrays.copyOf(featureRel, capacity);
                        featureValues = Arrays.copyOf(featureValues, capacity);
                    }
                    featureObs[nbFeatures] = o;
                    featureRel[nbFeatures] = index - starts[o];
                    featureValues[nbFeatures] = Arrays.copyOfRange(line, eq + 1, line.length);
                    nbFeatures++;
                    used.set(o);
                }
            }

            // new start of the features of each kept observation (the layout inside an observation is unchanged)
            long size = 0;
            for (int o = 0; o < nbObs; o++) {
                if (used.get(o)) {
                    starts[o] = size;
                    size += unigramSize(kinds[o], nbLabels) + bigramSize(kinds[o], nbLabels);
                }
            }

            // second pass: copy the header, the patterns, the labels and the kept observations, then the features
            try (LineReader reader = new LineReader(input);
                    OutputStream out = new BufferedOutputStream(Files.newOutputStream(tmp.toPath()), 1 << 20)) {
                writeLine(out, reader.next());
                byte[] rdr = reader.next();
                writeLine(out, rdr);
                int nbPatterns = Integer.parseInt(
                        new String(rdr, StandardCharsets.UTF_8)
                                .substring(5)
                                .split("/")[0]);
                for (int i = 0; i < nbPatterns + 1 + nbLabels; i++) {
                    writeLine(out, reader.next());
                }
                reader.next();
                writeLine(out, ("#qrk#" + used.cardinality()).getBytes(StandardCharsets.US_ASCII));
                for (int o = 0; o < nbObs; o++) {
                    byte[] line = reader.next();
                    if (used.get(o)) {
                        writeLine(out, line);
                    }
                }
                for (int f = 0; f < nbFeatures; f++) {
                    out.write(Long.toString(starts[featureObs[f]] + featureRel[f]).getBytes(StandardCharsets.US_ASCII));
                    out.write('=');
                    writeLine(out, featureValues[f]);
                }
            }
            Files.move(tmp.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println(
                    "Compacted Wapiti model "
                            + input.getAbsolutePath()
                            + " into "
                            + output.getAbsolutePath()
                            + ": observations "
                            + nbObs
                            + " -> "
                            + used.cardinality()
                            + ", active features "
                            + nbFeatures);
        } catch (IOException e) {
            throw new GrobidException("Cannot compact the Wapiti model " + input.getAbsolutePath(), e);
        } finally {
            tmp.delete();
        }
    }

    private static long unigramSize(byte kind, int nbLabels) {
        return (kind & 1) != 0 ? nbLabels : 0;
    }

    private static long bigramSize(byte kind, int nbLabels) {
        return (kind & 2) != 0 ? (long) nbLabels * nbLabels : 0;
    }

    private static int parseInt(String value, LineReader reader) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw reader.error("malformed count");
        }
    }

    private static int indexOf(byte[] line, byte b) {
        for (int i = 0; i < line.length; i++) {
            if (line[i] == b) {
                return i;
            }
        }
        return -1;
    }

    private static void writeLine(OutputStream out, byte[] line) throws IOException {
        out.write(line);
        out.write('\n');
    }

    /** Reads lines split on \n only (no \r handling, no decoding), as raw bytes. */
    private static class LineReader implements AutoCloseable {
        private final File file;
        private final InputStream in;
        private final byte[] chunk = new byte[1 << 20];
        private int chunkPos = 0;
        private int chunkLen = 0;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(256);
        private long lineNumber = 0;

        LineReader(File file) throws IOException {
            this.file = file;
            this.in = Files.newInputStream(file.toPath());
        }

        byte[] nextOrNull() throws IOException {
            buffer.reset();
            boolean read = false;
            while (true) {
                if (chunkPos == chunkLen) {
                    chunkLen = in.read(chunk);
                    chunkPos = 0;
                    if (chunkLen <= 0) {
                        chunkLen = 0;
                        if (!read) {
                            return null;
                        }
                        break;
                    }
                }
                read = true;
                int end = chunkPos;
                while (end < chunkLen && chunk[end] != '\n') {
                    end++;
                }
                buffer.write(chunk, chunkPos, end - chunkPos);
                if (end < chunkLen) {
                    chunkPos = end + 1;
                    break;
                }
                chunkPos = end;
            }
            lineNumber++;
            return buffer.toByteArray();
        }

        byte[] next() throws IOException {
            byte[] line = nextOrNull();
            if (line == null) {
                throw error("unexpected end of file");
            }
            return line;
        }

        String expectPrefix(String prefix) throws IOException {
            String line = new String(next(), StandardCharsets.UTF_8);
            if (!line.startsWith(prefix)) {
                throw error("expected a line starting with " + prefix);
            }
            return line;
        }

        GrobidException error(String message) {
            return new GrobidException(
                    "Cannot compact the Wapiti model "
                            + file.getAbsolutePath()
                            + ": "
                            + message
                            + " at line "
                            + lineNumber);
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }
}
