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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.grobid.core.GrobidModels;
import org.grobid.core.GrobidModels.Flavor;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.sax.TEISegmentationArticleLightRefSaxParser;
import org.grobid.trainer.sax.TEISegmentationArticleLightSaxParser;
import org.grobid.trainer.sax.TEISegmentationSaxParser;

public class SegmentationTrainer extends AbstractTrainer {

    private final GrobidModels.Flavor flavor;

    public SegmentationTrainer() {
        super(GrobidModels.SEGMENTATION);
        flavor = null;
    }

    public SegmentationTrainer(GrobidModels.Flavor modelFlavor) {
        super(GrobidModels.getModelFlavor(GrobidModels.SEGMENTATION, modelFlavor));
        flavor = modelFlavor;
    }

    @Override
    public int createCRFPPData(File corpusPath, File outputFile) {
        return addFeaturesSegmentation(
                corpusPath.getAbsolutePath() + "/tei",
                corpusPath.getAbsolutePath() + "/raw",
                outputFile,
                null,
                1.0);
    }

    /**
     * Add the selected features for the segmentation model
     *
     * @param corpusDir          path where corpus files are located
     * @param trainingOutputPath path where to store the temporary training data
     * @param evalOutputPath     path where to store the temporary evaluation data
     * @param splitRatio         ratio to consider for separating training and evaluation data, e.g. 0.8 for 80%
     * @return the total number of used corpus items
     */
    @Override
    public int createCRFPPData(
            final File corpusDir,
            final File trainingOutputPath,
            final File evalOutputPath,
            double splitRatio) {
        return addFeaturesSegmentation(
                corpusDir.getAbsolutePath() + "/tei",
                corpusDir.getAbsolutePath() + "/raw",
                trainingOutputPath,
                evalOutputPath,
                splitRatio);
    }

    /**
     * Add the selected features for the segmentation model
     *
     * @param sourceTEIPathLabel path to corpus TEI files
     * @param sourceRawPathLabel path to corpus raw files
     * @param trainingOutputPath path where to store the temporary training data
     * @param evalOutputPath     path where to store the temporary evaluation data
     * @param splitRatio         ratio to consider for separating training and evaluation data, e.g. 0.8 for 80%
     * @return number of examples
     */
    public int addFeaturesSegmentation(
            String sourceTEIPathLabel,
            String sourceRawPathLabel,
            final File trainingOutputPath,
            final File evalOutputPath,
            double splitRatio) {
        int totalExamples = 0;
        try {
            System.out.println("sourceTEIPathLabel: " + sourceTEIPathLabel);
            System.out.println("sourceRawPathLabel: " + sourceRawPathLabel);
            System.out.println("trainingOutputPath: " + trainingOutputPath);
            System.out.println("evalOutputPath: " + evalOutputPath);

            // we need first to generate the labeled files from the TEI annotated files
            File input = new File(sourceTEIPathLabel);
            // we process all tei files in the output directory
            File[] refFiles = input.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".tei.xml") || name.endsWith(".tei");
                }
            });

            if (refFiles == null) {
                return 0;
            }

            System.out.println(refFiles.length + " tei files");

            // the file for writing the training data
            OutputStream os2 = null;
            Writer writer2 = null;
            if (trainingOutputPath != null) {
                os2 = new FileOutputStream(trainingOutputPath);
                writer2 = new OutputStreamWriter(os2, StandardCharsets.UTF_8);
            }

            // the file for writing the evaluation data
            OutputStream os3 = null;
            Writer writer3 = null;
            if (evalOutputPath != null) {
                os3 = new FileOutputStream(evalOutputPath);
                writer3 = new OutputStreamWriter(os3, StandardCharsets.UTF_8);
            }

            // get a factory for SAX parser
            SAXParserFactory spf = SAXParserFactory.newInstance();

            AlignmentStats stats = new AlignmentStats();

            for (File tf : refFiles) {
                String name = tf.getName();
                LOGGER.info("Processing: " + name);

                TEISegmentationSaxParser parser;
                if (flavor == Flavor.ARTICLE_LIGHT) {
                    parser = new TEISegmentationArticleLightSaxParser();
                } else if (flavor == Flavor.ARTICLE_LIGHT_WITH_REFERENCES) {
                    parser = new TEISegmentationArticleLightRefSaxParser();
                } else {
                    parser = new TEISegmentationSaxParser();
                }

                //get a new instance of parser
                SAXParser p = spf.newSAXParser();
                p.parse(tf, parser);

                List<String> labeled = parser.getLabeledResult();

                // we can now add the features
                // we open the featured file
                try {
                    File theRawFile = new File(sourceRawPathLabel + File.separator + name.replace(".tei.xml", ""));
                    if (!theRawFile.exists()) {
                        LOGGER.error("The raw file does not exist: " + theRawFile.getPath());
                        stats.missingRaw.add(name);
                        continue;
                    }

                    // removing the @newline
                    /*List<String> newLabeled = new ArrayList<String>();
                    for(String label : labeled) {
                        if (!label.startsWith("@newline"))
                            newLabeled.add(label);
                    }
                    labeled = newLabeled;*/

                    /*StringBuilder temp = new StringBuilder();
                    for(String label : labeled) {
                        temp.append(label);
                    }
                    FileUtils.writeStringToFile(new File("/tmp/expected-"+name+".txt"), temp.toString());*/

                    int q = 0;
                    BufferedReader bis = new BufferedReader(
                            new InputStreamReader(new FileInputStream(theRawFile), StandardCharsets.UTF_8));
                    StringBuilder segmentation = new StringBuilder();
                    String line = null;
                    int l = 0;
                    String previousTag = null;
                    int nbInvalid = 0;
                    DocAlignment doc = new DocAlignment(name, labeled.size());
                    while ((line = bis.readLine()) != null) {
                        l++;
                        LineOutcome outcome = LineOutcome.END_OF_TEI;
                        int ii = line.indexOf(' ');
                        String token = null;
                        if (ii != -1) {
                            token = line.substring(0, ii);
                            // unicode normalisation of the token - it should not be necessary if the training data
                            // has been generated by a recent version of grobid
                            token = UnicodeUtil.normaliseTextAndRemoveSpaces(token);
                        }
                        // we get the label in the labelled data file for the same token
                        for (int pp = q; pp < labeled.size(); pp++) {
                            String localLine = labeled.get(pp);
                            StringTokenizer st = new StringTokenizer(localLine, " \t");
                            if (st.hasMoreTokens()) {
                                String localToken = st.nextToken();
                                // unicode normalisation of the token - it should not be necessary if the training data
                                // has been generated by a recent version of grobid
                                localToken = UnicodeUtil.normaliseTextAndRemoveSpaces(localToken);
                                if (localToken.equals(token)) {
                                    String tag = st.nextToken();
                                    segmentation.append(line).append(" ").append(tag);
                                    previousTag = tag;
                                    doc.teiLinesSkipped += pp - q;
                                    q = pp + 1;
                                    nbInvalid = 0;
                                    outcome = LineOutcome.MATCHED;
                                    //pp = q + 10;
                                    break;
                                }
                            }
                            if (pp - q > 5) {
                                //LOGGER.warn(name + " / Segmentation trainer: TEI and raw file unsynchronized at raw line " + l + " : " + localLine);
                                nbInvalid++;
                                // let's reuse the latest tag
                                if (previousTag != null) {
                                    segmentation.append(line).append(" ").append(previousTag);
                                    outcome = LineOutcome.PREVIOUS_LABEL;
                                } else {
                                    outcome = LineOutcome.NO_LABEL;
                                }
                                break;
                            }
                        }
                        doc.count(outcome, line.trim().isEmpty());
                        if (nbInvalid > 20) {
                            // too many consecutive synchronization issues
                            doc.abandonedAtLine = l;
                            break;
                        }
                    }
                    if (doc.abandonedAtLine > 0) {
                        while (bis.readLine() != null) {
                            doc.notRead++;
                        }
                    }
                    bis.close();
                    doc.teiLinesNotReached = labeled.size() - q;
                    doc.discarded = nbInvalid >= 10;
                    stats.add(doc);
                    LOGGER.info(doc.toString());

                    if (nbInvalid < 10) {
                        totalExamples++;
                        if ((writer2 == null) && (writer3 != null))
                            writer3.write(segmentation.toString() + "\n");
                        if ((writer2 != null) && (writer3 == null))
                            writer2.write(segmentation.toString() + "\n");
                        else {
                            if (Math.random() <= splitRatio)
                                writer2.write(segmentation.toString() + "\n");
                            else
                                writer3.write(segmentation.toString() + "\n");
                        }
                    } else {
                        LOGGER.error(
                                "{} / too many synchronization issues, file not used in training data and to be fixed!",
                                name);
                    }
                } catch (Exception e) {
                    LOGGER.error("Fail to open or process raw file", e);
                    stats.failed.add(name);
                }
            }

            System.out.println(stats.summary());

            if (writer2 != null) {
                writer2.close();
                if (os2 != null) {
                    os2.close();
                }
            }

            if (writer3 != null) {
                writer3.close();
                if (os3 != null) {
                    os3.close();
                }
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return totalExamples;
    }

    /**
     * What happened to a raw line when aligning it with the TEI labels
     */
    enum LineOutcome {
        /** its first token was found in the next TEI lines: it gets the TEI label */
        MATCHED,
        /** not found in the next TEI lines: it gets the label of the previous line */
        PREVIOUS_LABEL,
        /** not found in the next TEI lines and no previous label: not written */
        NO_LABEL,
        /** not found before the end of the TEI lines: not written */
        END_OF_TEI
    }

    /**
     * Alignment of the raw lines (features) of one document with its TEI lines (labels)
     */
    static class DocAlignment {
        final String name;
        final int teiLines;
        int rawLines = 0;
        int matched = 0;
        int previousLabel = 0;
        int noLabel = 0;
        int endOfTei = 0;
        // raw lines after the line where the alignment was abandoned (too many consecutive unsynchronized lines)
        int notRead = 0;
        int abandonedAtLine = 0;
        int blankLines = 0;
        int blankWritten = 0;
        // TEI lines jumped over to find a match, and TEI lines after the last match
        int teiLinesSkipped = 0;
        int teiLinesNotReached = 0;
        boolean discarded = false;

        DocAlignment(String name, int teiLines) {
            this.name = name;
            this.teiLines = teiLines;
        }

        void count(LineOutcome outcome, boolean blank) {
            if (blank) {
                // blank lines (e.g. at the end of the raw file) carry no features: not counted as raw lines,
                // but those written with the previous label are reported
                blankLines++;
                if (outcome == LineOutcome.PREVIOUS_LABEL) blankWritten++;
                return;
            }
            rawLines++;
            switch (outcome) {
                case MATCHED: matched++; break;
                case PREVIOUS_LABEL: previousLabel++; break;
                case NO_LABEL: noLabel++; break;
                case END_OF_TEI: endOfTei++; break;
            }
        }

        int totalRawLines() {
            return rawLines + notRead;
        }

        int written() {
            return discarded ? 0 : matched + previousLabel;
        }

        boolean fullyAligned() {
            return !discarded && matched == totalRawLines();
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT,
                "%s / %s: raw lines %d, written %d, aligned %d (labelled from the TEI %d, previous label %d), " +
                    "not aligned %d (no previous label %d, end of the TEI %d, not read after abandoning at line %d: %d), " +
                    "TEI lines %d, skipped %d, not reached %d, blank raw lines %d (written %d)",
                name, discarded ? "DISCARDED" : fullyAligned() ? "aligned" : "partially aligned",
                totalRawLines(), written(), matched + previousLabel, matched, previousLabel,
                noLabel + endOfTei + notRead, noLabel, endOfTei, abandonedAtLine, notRead,
                teiLines, teiLinesSkipped, teiLinesNotReached, blankLines, blankWritten);
        }
    }

    /**
     * Alignment statistics over the whole corpus, printed once the training data is created
     */
    static class AlignmentStats {
        final List<DocAlignment> docs = new ArrayList<>();
        final List<String> missingRaw = new ArrayList<>();
        final List<String> failed = new ArrayList<>();

        void add(DocAlignment doc) {
            docs.add(doc);
        }

        private static String pct(long part, long total) {
            return String.format(Locale.ROOT, "%.2f%%", total == 0 ? 0.0 : 100.0 * part / total);
        }

        String summary() {
            long raw = 0, written = 0, matched = 0, previousLabel = 0, noLabel = 0, endOfTei = 0,
                discardedLines = 0, tei = 0, teiSkipped = 0, teiNotReached = 0, blank = 0, blankWritten = 0;
            int discarded = 0, abandoned = 0, aligned = 0;
            for (DocAlignment d : docs) {
                raw += d.totalRawLines();
                tei += d.teiLines;
                blank += d.blankLines;
                blankWritten += d.discarded ? 0 : d.blankWritten;
                if (d.discarded) {
                    discarded++;
                    if (d.abandonedAtLine > 0) abandoned++;
                    discardedLines += d.totalRawLines();
                    continue;
                }
                written += d.written();
                matched += d.matched;
                previousLabel += d.previousLabel;
                noLabel += d.noLabel;
                endOfTei += d.endOfTei;
                teiSkipped += d.teiLinesSkipped;
                teiNotReached += d.teiLinesNotReached;
                if (d.fullyAligned()) aligned++;
            }
            int used = docs.size() - discarded;

            StringBuilder sb = new StringBuilder();
            sb.append("\n===== Segmentation training data: alignment of the raw files (features) with the TEI files (labels) =====\n");
            sb.append(String.format(Locale.ROOT, "TEI files: %d, raw file missing: %d, failed: %d%n",
                docs.size() + missingRaw.size() + failed.size(), missingRaw.size(), failed.size()));
            sb.append(String.format(Locale.ROOT, "documents used: %d (fully aligned: %d, partially aligned: %d)%n", used, aligned, used - aligned));
            sb.append(String.format(Locale.ROOT, "documents discarded: %d (alignment abandoned after more than 20 consecutive unsynchronized lines: %d, " +
                "ending with 10 to 20 unsynchronized lines: %d)%n", discarded, abandoned, discarded - abandoned));
            sb.append(String.format(Locale.ROOT, "raw lines: %d%n", raw));
            sb.append(String.format(Locale.ROOT, "  written to the training data: %d (%s)%n", written, pct(written, raw)));
            sb.append(String.format(Locale.ROOT, "    labelled from the TEI:       %d (%s)%n", matched, pct(matched, raw)));
            sb.append(String.format(Locale.ROOT, "    given the previous label:    %d (%s)%n", previousLabel, pct(previousLabel, raw)));
            sb.append(String.format(Locale.ROOT, "  not written: %d (%s)%n", raw - written, pct(raw - written, raw)));
            sb.append(String.format(Locale.ROOT, "    no previous label:           %d (%s)%n", noLabel, pct(noLabel, raw)));
            sb.append(String.format(Locale.ROOT, "    end of the TEI reached:      %d (%s)%n", endOfTei, pct(endOfTei, raw)));
            sb.append(String.format(Locale.ROOT, "    in discarded documents:      %d (%s)%n", discardedLines, pct(discardedLines, raw)));
            sb.append(String.format(Locale.ROOT, "TEI lines: %d, in used documents skipped: %d, not reached: %d%n", tei, teiSkipped, teiNotReached));
            sb.append(String.format(Locale.ROOT, "blank raw lines (not counted above): %d, written with the previous label: %d%n", blank, blankWritten));

            if (!missingRaw.isEmpty()) {
                sb.append("raw file missing: ").append(String.join(", ", missingRaw)).append("\n");
            }
            if (!failed.isEmpty()) {
                sb.append("failed: ").append(String.join(", ", failed)).append("\n");
            }
            List<DocAlignment> lossy = new ArrayList<>();
            for (DocAlignment d : docs) {
                if (!d.fullyAligned()) lossy.add(d);
            }
            lossy.sort(Comparator.comparingDouble((DocAlignment d) -> (double) d.written() / Math.max(d.totalRawLines(), 1))
                .thenComparing(d -> d.name));
            int top = Math.min(lossy.size(), 30);
            if (top > 0) {
                sb.append(String.format(Locale.ROOT, "documents with the lowest share of raw lines written (%d of %d not fully aligned):%n", top, lossy.size()));
                for (int i = 0; i < top; i++) {
                    DocAlignment d = lossy.get(i);
                    sb.append(String.format(Locale.ROOT, "  %-70s %s written %d / %d raw lines (%s), previous label %d%n",
                        d.name, d.discarded ? "DISCARDED" : "         ", d.written(), d.totalRawLines(),
                        pct(d.written(), d.totalRawLines()), d.previousLabel));
                }
            }
            sb.append("=====\n");
            return sb.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        AbstractTrainer.trainAndEvaluate(args, SegmentationTrainer::new, SegmentationTrainer::new);
    }
}
