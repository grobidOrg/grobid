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
package org.grobid.trainer.evaluation;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.grobid.core.GrobidModels;
import org.grobid.core.engines.tagging.GenericTagger;
import org.grobid.core.engines.tagging.TaggerFactory;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.trainer.AbstractTrainer;
import org.grobid.trainer.PatentParserTrainer;

/**
 * Evaluation of the extraction and parsing of the patent and NPL citations present in the patent
 * description.
 *
 */
public class PatentEvaluation {
    private String evaluationPath = null;

    private GenericTagger taggerPatent = null;
    private GenericTagger taggerNPL = null;
    private GenericTagger taggerAll = null;
    //where a test file would be put
    private String outputPath;

    public PatentEvaluation() {
        evaluationPath = AbstractTrainer.getEvalCorpusBasePath().getAbsolutePath();
        outputPath = GrobidProperties.getInstance().getTempPath().getAbsolutePath();
        //taggerNPL = TaggerFactory.getTagger(GrobidModels.PATENT_NPL);
        //taggerPatent = TaggerFactory.getTagger(GrobidModels.PATENT_PATENT);
        taggerAll = TaggerFactory.getTagger(GrobidModels.PATENT_CITATION);
    }

    /**
     * Evaluation of the patent and NPL parsers against an evaluation set in the normal training format
     * at token and instance level.
     *
     * @param type gives the model to be evaluated: 0 is the patent citation only model, 1 is the NPL
     *             citation only model and 2 is the combined patent+NPL citation model.
     * @return report
     */
    public String evaluate(int type) {
        // we need first to produce the evaluation files with features from the files in corpus format present
        // in the evaluation folder
        PatentParserTrainer ppt = new PatentParserTrainer();

        //noinspection NullableProblems
        //ppt.createDataSet("test", null, evaluationPath, outputPath);
        String setName;

        GenericTagger tagger;
        if (type == 0) {
            tagger = taggerPatent;
            setName = "patent";
        } else if (type == 1) {
            tagger = taggerNPL;
            setName = "npl";
        } else if (type == 2) {
            tagger = taggerAll;
            setName = "all";
        } else {
            throw new GrobidException("An exception occurred while evaluating Grobid. The parameter "
                    +
                    "type is undefined.");
        }

        return evaluate();
    }

    /**
     * Evaluation of the patent and NPL parsers against an evaluation set in the normal training format
     * at token and instance level.
     * @return report
     */
    public String evaluate() {
        // we need first to produce the evaluation files with features from the files in corpus format present
        // in the evaluation folder
        StringBuilder report = new StringBuilder();

        PatentParserTrainer ppt = new PatentParserTrainer();
        //noinspection NullableProblems
        ppt.createDataSet("test", evaluationPath, outputPath, 1);

        List<GenericTagger> taggers = new ArrayList<GenericTagger>();
        taggers.add(taggerAll);

        // note: there is no field for these models
        for (GenericTagger tagger : taggers) {

            // total tag
            int totalExpected = 0;
            int totalCorrect = 0;
            int totalSuggested = 0;
            // total instance
            int totalInstanceExpected = 0;
            int totalInstanceCorrect = 0;
            int totalInstanceSuggested = 0;

            // npl tag
            int totalNPLExpected = 0;
            int totalNPLCorrect = 0;
            int totalNPLSuggested = 0;
            // npl instance
            int totalInstanceNPLExpected = 0;
            int totalInstanceNPLCorrect = 0;
            int totalInstanceNPLSuggested = 0;

            // patent tag
            int totalPatentExpected = 0;
            int totalPatentCorrect = 0;
            int totalPatentSuggested = 0;
            // patent instance
            int totalInstancePatentExpected = 0;
            int totalInstancePatentCorrect = 0;
            int totalInstancePatentSuggested = 0;

            try {
                // read the evaluation file enriched with feature
                BufferedReader bufReader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(outputPath + "/all.test"), "UTF-8"));
                String line = null;
                ArrayList<String> patentBlocks = new ArrayList<String>();
                while ((line = bufReader.readLine()) != null) {
                    patentBlocks.add(line);
                }
                bufReader.close();

                //TODO: VZ_FIX
                //                String theResult = EvaluationUtilities.taggerRun(patentBlocks, tagger);
                String theResult = tagger.label(patentBlocks);
                //System.out.println(theResult);
                StringTokenizer stt = new StringTokenizer(theResult, "\n");
                //                line = null;
                String previousExpectedLabel = null;
                String previousSuggestedLabel = null;
                boolean instanceCorrect = true;
                while (stt.hasMoreTokens()) {
                    line = stt.nextToken();
                    StringTokenizer st = new StringTokenizer(line, "\t");
                    String expected = null; // expected tag
                    String actual = null;     // tag suggested by the model
                    String word = null;     // the token
                    boolean start = true;
                    boolean failure = false;
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        if (start) {
                            word = token.trim();
                            start = false;
                        }
                        expected = actual;
                        actual = token.trim();
                    }

                    // we can simply compare the two last line (expected, actual) for evaluation
                    // in this context an instance is a connected sequence the "non-other" tags in the expected tags
                    // As in (Peng & McCallum, 2006), we simply measure the accuracy at the instance level.

                    // tags
                    if ((expected != null) && (actual != null)) {
                        if (!expected.equals("<other>")) {
                            totalExpected++;
                            if (expected.endsWith("refPatent>"))
                                totalPatentExpected++;
                            else if (expected.endsWith("refNPL>"))
                                totalNPLExpected++;
                            else
                                report.append("WARNING bizarre suggested tag: " + expected + "\n");
                        }

                        if (!actual.equals("<other>")) {
                            totalSuggested++;
                            if (actual.endsWith("refPatent>"))
                                totalPatentSuggested++;
                            else if (actual.endsWith("refNPL>"))
                                totalNPLSuggested++;
                            else
                                report.append("WARNING bizarre suggested tag: " + actual + "\n");
                        }

                        if (actual.endsWith("refPatent>"))
                            actual = "refPatent";
                        else if (actual.endsWith("refNPL>")) {
                            actual = "refNPL";
                        }

                        if (expected.endsWith("refPatent>"))
                            expected = "refPatent";
                        else if (expected.endsWith("refNPL>"))
                            expected = "refNPL";

                        if (actual.equals("<other>"))
                            actual = "other";
                        if (expected.equals("<other>"))
                            expected = "other";

                        if (expected.equals(actual)) {
                            if (!actual.equals("other") && !expected.equals("other")) {
                                totalCorrect++;
                                if (expected.startsWith("refPatent"))
                                    totalPatentCorrect++;
                                else if (expected.startsWith("refNPL"))
                                    totalNPLCorrect++;
                            }
                        } else {
                            failure = true;
                        }

                        // expected instance
                        if (!expected.equals("other")) {
                            if ((previousExpectedLabel == null) || (!expected.equals(previousExpectedLabel))) {
                                // we are starting a new instance

                                // are we ending an instance?
                                if (previousExpectedLabel != null) {
                                    if (!previousExpectedLabel.equals("other")) {
                                        // we are ending an instance
                                        if (instanceCorrect) {
                                            if (previousExpectedLabel.startsWith("refPatent"))
                                                totalInstancePatentCorrect++;
                                            else if (previousExpectedLabel.startsWith("refNPL"))
                                                totalInstanceNPLCorrect++;
                                        }
                                    }
                                }

                                // new instance
                                totalInstanceExpected++;
                                if (expected.startsWith("refPatent"))
                                    totalInstancePatentExpected++;
                                else if (expected.startsWith("refNPL"))
                                    totalInstanceNPLExpected++;
                                instanceCorrect = true;
                            }

                        } else {
                            // are we ending an instance?
                            if (previousExpectedLabel != null) {
                                if (!previousExpectedLabel.equals("other")) {
                                    // we are ending an instance
                                    if (instanceCorrect) {
                                        totalInstanceCorrect++;
                                        if (previousExpectedLabel.startsWith("refPatent"))
                                            totalInstancePatentCorrect++;
                                        else if (previousExpectedLabel.startsWith("refNPL"))
                                            totalInstanceNPLCorrect++;
                                    }
                                    instanceCorrect = true;
                                }
                            }
                        }

                        if (failure) {
                            instanceCorrect = false;
                        }

                        previousExpectedLabel = expected;
                        previousSuggestedLabel = actual;
                    }
                }
            } catch (Exception e) {
                throw new GrobidException("An exception occurred while evaluating Grobid.", e);
            }

            double precision;
            double recall;
            double f;

            if (tagger == taggerNPL) {
                report.append("\n\n*********************************************\n");
                report.append("****** NPL reference extraction model *******\n");
                report.append("*********************************************\n");
            } else if (tagger == taggerPatent) {
                report.append("\n\n************************************************\n");
                report.append("****** patent reference extraction model *******\n");
                report.append("************************************************\n");
            } else if (tagger == taggerAll) {
                report.append("\n\n*************************************************************\n");
                report.append("****** combined NPL+patent reference extraction model *******\n");
                report.append("*************************************************************\n");
            }

            if (tagger == taggerAll) {
                report.append("\n======== GENERAL TAG EVALUATION ========\n");
                report.append("Total expected tags: ").append(totalExpected).append("\n");
                report.append("Total suggested tags: ").append(totalSuggested).append("\n");
                report.append("Total correct tags (Correct Positive): ").append(totalCorrect).append("\n");
                report.append("Total incorrect tags (False Positive + False Negative): ")
                        .append(Math.abs(totalSuggested - totalCorrect))
                        .append("\n");
                precision = (double) totalCorrect / totalSuggested;
                recall = (double) totalCorrect / totalExpected;
                f = 2 * precision * recall / (precision + recall);
                report.append("Precision\t= ").append(TextUtilities.formatTwoDecimals(precision * 100)).append("\n");
                report.append("Recall\t= ").append(TextUtilities.formatTwoDecimals(recall * 100)).append("\n");
                report.append("F-score\t= ").append(TextUtilities.formatTwoDecimals(f * 100)).append("\n");
            }

            if (tagger != taggerPatent) {
                report.append("\n======== TAG NPL EVALUATION ========\n");
                report.append("Total expected tags: ").append(totalNPLExpected).append("\n");
                report.append("Total suggested tags: ").append(totalNPLSuggested).append("\n");
                report.append("Total correct tags (Correct Positive): ").append(totalNPLCorrect).append("\n");
                report.append("Total incorrect tags (False Positive + False Negative): ")
                        .append(Math.abs(totalNPLSuggested - totalNPLCorrect))
                        .append("\n");
                precision = (double) totalNPLCorrect / totalNPLSuggested;
                recall = (double) totalNPLCorrect / totalNPLExpected;
                f = 2 * precision * recall / (precision + recall);
                report.append("Precision\t= ").append(TextUtilities.formatTwoDecimals(precision * 100)).append("\n");
                report.append("Recall\t= ").append(TextUtilities.formatTwoDecimals(recall * 100)).append("\n");
                report.append("F-score\t= ").append(TextUtilities.formatTwoDecimals(f * 100)).append("\n");
            }

            if (tagger != taggerNPL) {
                report.append("\n======== TAG PATENT EVALUATION ========\n");
                report.append("Total expected tags: ").append(totalPatentExpected).append("\n");
                report.append("Total suggested tags: ").append(totalPatentSuggested).append("\n");
                report.append("Total correct tags (Correct Positive): ").append(totalPatentCorrect).append("\n");
                report.append("Total incorrect tags (False Positive + False Negative): ")
                        .append(Math.abs(totalPatentSuggested - totalPatentCorrect))
                        .append("\n");
                precision = (double) totalPatentCorrect / totalPatentSuggested;
                recall = (double) totalPatentCorrect / totalPatentExpected;
                f = 2 * precision * recall / (precision + recall);
                report.append("Precision\t= ").append(TextUtilities.formatTwoDecimals(precision * 100)).append("\n");
                report.append("Recall\t= ").append(TextUtilities.formatTwoDecimals(recall * 100)).append("\n");
                report.append("F-score\t= ").append(TextUtilities.formatTwoDecimals(f * 100)).append("\n");
            }

            if (tagger == taggerAll) {
                report.append("\n======== GENERAL INSTANCE EVALUATION ========\n");
                report.append("Total expected instances: ").append(totalInstanceExpected).append("\n");
                report.append("Total correct instances: ").append(totalInstanceCorrect).append("\n");
                recall = (double) totalInstanceCorrect / totalInstanceExpected;
                report.append("Instance Accuracy = ")
                        .append(TextUtilities.formatTwoDecimals(recall * 100))
                        .append("\n");
            }

            if (tagger != taggerPatent) {
                report.append("\n======== INSTANCE NPL EVALUATION ========\n");
                report.append("Total expected instances: ").append(totalInstanceNPLExpected).append("\n");
                report.append("Total correct instances: ").append(totalInstanceNPLCorrect).append("\n");
                recall = (double) totalInstanceNPLCorrect / totalInstanceNPLExpected;
                report.append("Instance accuracy = ")
                        .append(TextUtilities.formatTwoDecimals(recall * 100))
                        .append("\n");
            }

            if (tagger != taggerNPL) {
                report.append("\n======== INSTANCE PATENT EVALUATION ========\n");
                report.append("Total expected instances: ").append(totalInstancePatentExpected).append("\n");
                report.append("Total correct instances: ").append(totalInstancePatentCorrect).append("\n");
                recall = (double) totalInstancePatentCorrect / totalInstancePatentExpected;
                report.append("Instance accuracy = ")
                        .append(TextUtilities.formatTwoDecimals(recall * 100))
                        .append("\n\n");
            }
        }
        return report.toString();
    }

    /**
     * Evaluation of the extraction against the gold corpus for patent reference resolution (non XML format, realized in 2010).
     * Use in particular for a comparison with Ddoc and ACE.
     * @param path file path
     */
    public void evaluateGold(File path) {
        try {
            TreeMap<String, ArrayList<String>> rfap_reference = new TreeMap<String, ArrayList<String>>();
            TreeMap<String, ArrayList<String>> rf_reference = new TreeMap<String, ArrayList<String>>();
            TreeMap<String, ArrayList<String>> rfap_ace = new TreeMap<String, ArrayList<String>>();
            TreeMap<String, ArrayList<String>> rf_ace = new TreeMap<String, ArrayList<String>>();
            TreeMap<String, ArrayList<String>> rfap_Ddoc = new TreeMap<String, ArrayList<String>>();
            TreeMap<String, ArrayList<String>> rf_Ddoc = new TreeMap<String, ArrayList<String>>();
            TreeMap<String, ArrayList<String>> rfap = new TreeMap<String, ArrayList<String>>();
            TreeMap<String, ArrayList<String>> rf = new TreeMap<String, ArrayList<String>>();

            // we parse the log file for getting reference data and ACE/Ddoc results
            String dossierName = null;
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(evaluationPath + "/gold/REF_20100426.txt"), "UTF8"));
            String s;
            //            boolean rf_part = false;
            ArrayList<String> resap_reference = null;
            ArrayList<String> res_reference = null;

            while ((s = br.readLine()) != null) {
                if (s.length() == 0)
                    continue;

                if (s.startsWith("RFAP:")) {
                    resap_reference = new ArrayList<String>();
                    s = s.substring(5, s.length());
                    String[] pats = s.split(" ");
                    for (String pat : pats) {
                        if (pat != null) {
                            if (pat.length() > 0) {
                                if (!resap_reference.contains(pat))
                                    resap_reference.add(pat);
                            }
                        }
                    }
                } else if (s.startsWith("RF:")) {
                    res_reference = new ArrayList<String>();
                    s = s.substring(3, s.length());
                    String[] pats = s.split(" ");
                    for (String pat : pats) {
                        if (pat != null) {
                            if (pat.length() > 0) {
                                if (!res_reference.contains(pat))
                                    res_reference.add(pat);
                            }
                        }
                    }
                } else {
                    if (dossierName != null) {
                        rfap_reference.put(dossierName, resap_reference);
                        rf_reference.put(dossierName, res_reference);
                    }

                    dossierName = s.trim();
                    dossierName = dossierName.replace(".txt", "");
                }
            }
            rfap_reference.put(dossierName, resap_reference);
            rf_reference.put(dossierName, res_reference);
            br.close();

            // we parse the log file for getting ACE results
            br = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(evaluationPath + "/ACE_20100426.txt"), "UTF8"));
            //            rf_part = false;
            ArrayList<String> resap_ace = null;
            ArrayList<String> res_ace = null;

            dossierName = null;
            while ((s = br.readLine()) != null) {
                if (s.length() == 0)
                    continue;

                if (s.startsWith("RFAP:")) {
                    resap_ace = new ArrayList<String>();
                    s = s.substring(5, s.length());
                    String[] pats = s.split(" ");
                    for (String pat : pats) {
                        if (pat != null) {
                            if (pat.length() > 0) {
                                if (!resap_ace.contains(pat))
                                    resap_ace.add(pat);
                            }
                        }
                    }
                } else if (s.startsWith("RF:")) {
                    res_ace = new ArrayList<String>();
                    s = s.substring(3, s.length());
                    String[] pats = s.split(" ");
                    for (String pat : pats) {
                        if (pat != null) {
                            if (pat.length() > 0) {
                                if (!res_ace.contains(pat))
                                    res_ace.add(pat);
                            }
                        }
                    }
                } else {
                    if (dossierName != null) {
                        rfap_ace.put(dossierName, resap_ace);
                        rf_ace.put(dossierName, res_ace);
                    }

                    dossierName = s.trim();
                    dossierName = dossierName.replace(".txt", "");
                }
            }
            rfap_ace.put(dossierName, resap_ace);
            rf_ace.put(dossierName, res_ace);
            br.close();

            // we parse the log file for Ddoc results
            br = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(evaluationPath + "/Ddoc_20100426.txt"), "UTF8"));
            ArrayList<String> resap_Ddoc = null;
            ArrayList<String> res_Ddoc = null;

            dossierName = null;
            while ((s = br.readLine()) != null) {
                if (s.length() == 0)
                    continue;

                if (s.startsWith("RFAP:")) {
                    resap_Ddoc = new ArrayList<String>();
                    s = s.substring(5, s.length());
                    String[] pats = s.split(" ");
                    for (String pat : pats) {
                        if (pat != null) {
                            if (pat.length() > 0) {
                                if (!resap_Ddoc.contains(pat))
                                    resap_Ddoc.add(pat);
                            }
                        }
                    }
                } else if (s.startsWith("RF:")) {
                    res_Ddoc = new ArrayList<String>();
                    s = s.substring(3, s.length());
                    String[] pats = s.split(" ");
                    for (String pat : pats) {
                        if (pat != null) {
                            if (pat.length() > 0) {
                                if (!res_Ddoc.contains(pat))
                                    res_Ddoc.add(pat);
                            }
                        }
                    }
                } else {
                    if (dossierName != null) {
                        rfap_Ddoc.put(dossierName, resap_Ddoc);
                        rf_Ddoc.put(dossierName, res_Ddoc);
                    }

                    dossierName = s.trim();
                    dossierName = dossierName.replace(".txt", "");
                }
            }
            rfap_Ddoc.put(dossierName, resap_Ddoc);
            rf_Ddoc.put(dossierName, res_Ddoc);
            br.close();

            /*while((s = br.readLine()) != null) {
                   s = s.substring(1, s.length());

                   if (s.trim().length() == 0) continue;

                   if (s.startsWith("EP") & (dossierName == null)) {
                       StringTokenizer st = new StringTokenizer(s, " ");
                       dossierName = st.nextToken().trim();
                       //dossierName = "EP20"+dossierName.substring(2,4)+"0"+dossierName.substring(4,dossierName.length());
                       //System.out.println(dossierName);
                   }
                   else if (s.startsWith("RFAP")) {
                       rf_part = false;
                       resap_reference = new ArrayList<String>();
                       resap_ace = new ArrayList<String>();
                       resap_Ddoc = new ArrayList<String>();
                   }
                   else if (s.startsWith("RF")) {
                       rf_part = true;
                       res_reference = new ArrayList<String>();
                       res_ace = new ArrayList<String>();
                       res_Ddoc = new ArrayList<String>();
                   }
                   else if (s.startsWith("_______")) {
                       rfap_reference.put(dossierName, resap_reference);
                       rf_reference.put(dossierName, res_reference);
                       rfap_ace.put(dossierName, resap_ace);
                       rf_ace.put(dossierName, res_ace);
                       rfap_Ddoc.put(dossierName, resap_Ddoc);
                       rf_Ddoc.put(dossierName, res_Ddoc);
                       dossierName = null;
                   }
                   else {
                       StringTokenizer st = new StringTokenizer(s, "|");
                       if (rf_part) {
                           String tok1 = st.nextToken().trim();
                           String tok2 = st.nextToken().trim();
                           String tok3 = st.nextToken().trim();

                           if (tok1.length() > 0) {
                               if (!res_reference.contains(tok1))
                                   res_reference.add(tok1);
                           }
                           if (tok2.length() > 0) {
                               if (!res_ace.contains(tok2))
                                   res_ace.add(tok2);
                           }
                           if (tok3.length() > 0) {
                               if (!res_Ddoc.contains(tok3))
                                   res_Ddoc.add(tok3);
                           }
                       }
                       else {
                           String tok1 = st.nextToken().trim();
                           if (!st.hasMoreTokens())
                               System.out.println("WARNING: " + s);
                           String tok2 = st.nextToken().trim();
                           if (!st.hasMoreTokens())
                               System.out.println("WARNING: " + s);
                           String tok3 = st.nextToken().trim();

                           if (tok1.length() > 0) {
                               if (!resap_reference.contains(tok1))
                                   resap_reference.add(tok1);
                           }
                           if (tok2.length() > 0) {
                               if (!resap_ace.contains(tok2))
                                   resap_ace.add(tok2);
                           }
                           if (tok3.length() > 0) {
                               if (!resap_Ddoc.contains(tok3))
                                   resap_Ddoc.add(tok3);
                           }
                       }
                   }
               }
               br.close();
               */

            // we parse our own results
            BufferedReader br2 = new BufferedReader(
                    new InputStreamReader(new FileInputStream(path.getParent() + "/report.txt"), "UTF8"));
            dossierName = null;
            ArrayList<String> resap = null;
            ArrayList<String> res = null;
            while ((s = br2.readLine()) != null) {
                if (s.length() == 0)
                    continue;

                if (s.startsWith("RFAP:")) {
                    resap = new ArrayList<String>();
                    s = s.substring(5, s.length());
                    String[] pats = s.split(" ");
                    for (String pat : pats) {
                        if (pat != null) {
                            if (pat.length() > 0) {
                                if (!resap.contains(pat))
                                    resap.add(pat);
                            }
                        }
                    }
                } else if (s.startsWith("RF:")) {
                    res = new ArrayList<String>();
                    s = s.substring(3, s.length());
                    String[] pats = s.split(" ");
                    for (String pat : pats) {
                        if (pat != null) {
                            if (pat.length() > 0) {
                                if (!res.contains(pat))
                                    res.add(pat);
                            }
                        }
                    }
                } else {
                    if (dossierName != null) {
                        rfap.put(dossierName, resap);
                        rf.put(dossierName, res);
                    }

                    dossierName = s.trim();
                    dossierName = dossierName.replace(".txt", "");
                }
            }
            rfap.put(dossierName, resap);
            rf.put(dossierName, res);
            br2.close();

            // all the set are initiated, we compute the metrics

            // reference
            int count_rfap_reference = 0;
            for (Map.Entry<String, ArrayList<String>> entry : rfap_reference.entrySet()) {
                //                dossierName = entry.getKey();
                ArrayList<String> liste = entry.getValue();
                count_rfap_reference += liste.size();
            }
            int count_rf_reference = 0;
            int nbDossier = 0;
            for (Map.Entry<String, ArrayList<String>> entry : rf_reference.entrySet()) {
                //                dossierName = entry.getKey();
                ArrayList<String> liste = entry.getValue();
                count_rf_reference += liste.size();
                nbDossier++;
            }
            System.out.println(
                    "Ref. data: "
                            + count_rfap_reference
                            + " serials and "
                            + count_rf_reference
                            + " publications, total: "
                            + (count_rfap_reference + count_rf_reference)
                            + " in "
                            + nbDossier
                            + " dossiers");

            // ace
            int count_rfap_ace = 0;
            int count_rfap_ace_correct = 0;
            for (Map.Entry<String, ArrayList<String>> entry : rfap_ace.entrySet()) {
                dossierName = entry.getKey();
                ArrayList<String> referenceListe = rfap_reference.get(dossierName);
                ArrayList<String> liste = entry.getValue();
                count_rfap_ace += liste.size();
                for (String pat : liste) {
                    if (referenceListe.contains(pat)) {
                        count_rfap_ace_correct++;
                    }
                }
            }
            int count_rf_ace = 0;
            int count_rf_ace_correct = 0;
            nbDossier = 0;
            for (Map.Entry<String, ArrayList<String>> entry : rf_ace.entrySet()) {
                dossierName = entry.getKey();
                ArrayList<String> referenceListe = rf_reference.get(dossierName);
                ArrayList<String> liste = entry.getValue();
                count_rf_ace += liste.size();
                for (String pat : liste) {
                    if (referenceListe.contains(pat)) {
                        count_rf_ace_correct++;
                    }
                }
                nbDossier++;
            }
            System.out.println(
                    "ACE data: "
                            + count_rfap_ace
                            + " ("
                            + count_rfap_ace_correct
                            + " correct) serials and "
                            + count_rf_ace
                            + " ("
                            + count_rf_ace_correct
                            + " correct) publications, total: "
                            + (count_rfap_ace + count_rf_ace)
                            + " in "
                            + nbDossier
                            + " dossiers");

            // Ddoc
            int count_rfap_Ddoc = 0;
            int count_rfap_Ddoc_correct = 0;
            for (Map.Entry<String, ArrayList<String>> entry : rfap_Ddoc.entrySet()) {
                dossierName = entry.getKey();
                ArrayList<String> referenceListe = rfap_reference.get(dossierName);
                ArrayList<String> liste = entry.getValue();
                count_rfap_Ddoc += liste.size();
                for (String pat : liste) {
                    if (referenceListe.contains(pat)) {
                        count_rfap_Ddoc_correct++;
                    }
                }
            }
            int count_rf_Ddoc = 0;
            int count_rf_Ddoc_correct = 0;
            nbDossier = 0;
            for (Map.Entry<String, ArrayList<String>> entry : rf_Ddoc.entrySet()) {
                dossierName = entry.getKey();
                ArrayList<String> referenceListe = rf_reference.get(dossierName);
                ArrayList<String> liste = entry.getValue();
                count_rf_Ddoc += liste.size();
                for (String pat : liste) {
                    if (referenceListe.contains(pat)) {
                        count_rf_Ddoc_correct++;
                    }
                }
                nbDossier++;
            }
            System.out.println(
                    "Ddoc data: "
                            + count_rfap_Ddoc
                            + " ("
                            + count_rfap_Ddoc_correct
                            + " correct) serials and "
                            + count_rf_Ddoc
                            + " ("
                            + count_rf_Ddoc_correct
                            + " correct) publications, total: "
                            + (count_rfap_Ddoc + count_rf_Ddoc)
                            + " in "
                            + nbDossier
                            + " dossiers");

            // GROBID
            int count_rfap = 0;
            int count_rfap_correct = 0;
            for (Map.Entry<String, ArrayList<String>> entry : rfap.entrySet()) {
                //System.out.println("key is " + entry.getKey() + " and value is " + entry.getValue());
                dossierName = entry.getKey();
                ArrayList<String> referenceListe = rfap_reference.get(dossierName);
                if (referenceListe != null) {
                    ArrayList<String> liste = entry.getValue();
                    count_rfap += liste.size();
                    for (String pat : liste) {
                        if (referenceListe.contains(pat)) {
                            count_rfap_correct++;
                        }
                    }
                }
            }
            int count_rf = 0;
            int count_rf_correct = 0;
            nbDossier = 0;
            for (Map.Entry<String, ArrayList<String>> entry : rf.entrySet()) {
                dossierName = entry.getKey();
                ArrayList<String> referenceListe = rf_reference.get(dossierName);
                if (referenceListe != null) {
                    ArrayList<String> liste = entry.getValue();
                    count_rf += liste.size();
                    for (String pat : liste) {
                        if (referenceListe.contains(pat)) {
                            count_rf_correct++;
                        }
                    }
                    nbDossier++;
                } else
                    System.out.println(
                            "WARNING! file "
                                    + dossierName
                                    + " in GROBID's results but not in reference results");
            }
            System.out.println(
                    "GROBID data: "
                            + count_rfap
                            + " ("
                            + count_rfap_correct
                            + " correct) serials and "
                            + count_rf
                            + " ("
                            + count_rf_correct
                            + " correct) publications, total: "
                            + (count_rfap + count_rf)
                            + " in "
                            + nbDossier
                            + " dossiers");

            // creating sharing Ddoc and Grobid by intersection
            int count_rfap_DdocIGROBID = 0;
            int count_rfap_DdocIGROBID_correct = 0;
            for (Map.Entry<String, ArrayList<String>> entry : rfap_Ddoc.entrySet()) {
                dossierName = entry.getKey();
                ArrayList<String> referenceListe = rfap_reference.get(dossierName);
                ArrayList<String> liste = entry.getValue();
                ArrayList<String> listeGrobid = rfap.get(dossierName);
                if (listeGrobid == null) {
                    System.out.println(
                            "WARNING! file "
                                    + dossierName
                                    + " in Ddoc results but not in GROBID's one");
                } else {
                    ArrayList<String> liste2 = new ArrayList<String>();
                    for (String toto : liste) {
                        if (listeGrobid.contains(toto))
                            liste2.add(toto);
                    }
                    count_rfap_DdocIGROBID += liste2.size();
                    for (String pat : liste2) {
                        if (referenceListe.contains(pat)) {
                            count_rfap_DdocIGROBID_correct++;
                        }
                    }
                }
            }
            int count_rf_DdocIGROBID = 0;
            int count_rf_DdocIGROBID_correct = 0;
            nbDossier = 0;
            for (Map.Entry<String, ArrayList<String>> entry : rf_Ddoc.entrySet()) {
                dossierName = entry.getKey();
                ArrayList<String> referenceListe = rf_reference.get(dossierName);
                ArrayList<String> liste = entry.getValue();
                ArrayList<String> listeGrobid = rf.get(dossierName);
                if (listeGrobid == null) {
                    System.out.println(
                            "WARNING! file "
                                    + dossierName
                                    + " in Ddoc results but not in GROBID's one");
                } else {
                    ArrayList<String> liste2 = new ArrayList<String>();
                    for (String toto : liste) {
                        if (listeGrobid.contains(toto))
                            liste2.add(toto);
                    }
                    count_rf_DdocIGROBID += liste2.size();
                    for (String pat : liste2) {
                        if (referenceListe.contains(pat)) {
                            count_rf_DdocIGROBID_correct++;
                        }
                    }
                    nbDossier++;
                }
            }
            System.out.println(
                    "Ddoc+GROBID data: "
                            + count_rfap_DdocIGROBID
                            + " ("
                            + count_rfap_DdocIGROBID_correct
                            + " correct) serials and "
                            + count_rf_DdocIGROBID
                            + " ("
                            + count_rf_DdocIGROBID_correct
                            + " correct) publications, total: "
                            + (count_rfap_DdocIGROBID + count_rf_DdocIGROBID)
                            + " in "
                            + nbDossier
                            + " dossiers");

            // creating sharing Ddoc and Grobid by union
            int count_rfap_DdocUGROBID = 0;
            int count_rfap_DdocUGROBID_correct = 0;
            for (Map.Entry<String, ArrayList<String>> entry : rfap_Ddoc.entrySet()) {
                dossierName = entry.getKey();
                ArrayList<String> referenceListe = rfap_reference.get(dossierName);
                ArrayList<String> liste = entry.getValue();
                ArrayList<String> listeGrobid = rfap.get(dossierName);
                if (listeGrobid == null) {
                    System.out.println(
                            "WARNING! file "
                                    + dossierName
                                    + " in Ddoc results but not in GROBID's one");
                } else {
                    for (String toto : listeGrobid) {
                        if (!liste.contains(toto))
                            liste.add(toto);
                    }
                    count_rfap_DdocUGROBID += liste.size();
                    for (String pat : liste) {
                        if (referenceListe.contains(pat)) {
                            count_rfap_DdocUGROBID_correct++;
                        }
                    }
                }
            }
            int count_rf_DdocUGROBID = 0;
            int count_rf_DdocUGROBID_correct = 0;
            nbDossier = 0;
            for (Map.Entry<String, ArrayList<String>> entry : rf_Ddoc.entrySet()) {
                dossierName = entry.getKey();
                ArrayList<String> referenceListe = rf_reference.get(dossierName);
                ArrayList<String> liste = entry.getValue();
                ArrayList<String> listeGrobid = rf.get(dossierName);
                if (listeGrobid == null) {
                    System.out.println(
                            "WARNING! file "
                                    + dossierName
                                    + " in Ddoc results but not in GROBID's one");
                } else {
                    for (String toto : listeGrobid) {
                        if (!liste.contains(toto))
                            liste.add(toto);
                    }
                    count_rf_DdocUGROBID += liste.size();
                    for (String pat : liste) {
                        if (referenceListe.contains(pat)) {
                            count_rf_DdocUGROBID_correct++;
                        }
                    }
                    nbDossier++;
                }
            }
            System.out.println(
                    "Ddoc|GROBID data: "
                            + count_rfap_DdocUGROBID
                            + " ("
                            + count_rfap_DdocUGROBID_correct
                            + " correct) serials and "
                            + count_rf_DdocUGROBID
                            + " ("
                            + count_rf_DdocUGROBID_correct
                            + " correct) publications, total: "
                            + (count_rfap_DdocUGROBID + count_rf_DdocUGROBID)
                            + " in "
                            + nbDossier
                            + " dossiers");

            // ACE
            double ace_rfap_precision = (double) count_rfap_ace_correct / count_rfap_ace;
            double ace_rfap_recall = (double) count_rfap_ace_correct / count_rfap_reference;
            double ace_rfap_f = (2 * ace_rfap_precision * ace_rfap_recall)
                    / (ace_rfap_precision + ace_rfap_recall);

            double ace_rf_precision = (double) count_rf_ace_correct / count_rf_ace;
            double ace_rf_recall = (double) count_rf_ace_correct / count_rf_reference;
            double ace_rf_f = (2 * ace_rf_precision * ace_rf_recall)
                    / (ace_rf_precision + ace_rf_recall);

            double ace_rfall_precision = (double) (count_rfap_ace_correct + count_rf_ace_correct)
                    / (count_rfap_ace + count_rf_ace);
            double ace_rfall_recall = (double) (count_rfap_ace_correct + count_rf_ace_correct)
                    / (count_rfap_reference + count_rf_reference);
            double ace_rfall_f = (2 * ace_rfall_precision * ace_rfall_recall)
                    / (ace_rfall_precision + ace_rfall_recall);

            // Ddoc
            double Ddoc_rfap_precision = (double) count_rfap_Ddoc_correct / count_rfap_Ddoc;
            double Ddoc_rfap_recall = (double) count_rfap_Ddoc_correct / count_rfap_reference;
            double Ddoc_rfap_f = (2 * Ddoc_rfap_precision * Ddoc_rfap_recall)
                    / (Ddoc_rfap_precision + Ddoc_rfap_recall);

            double Ddoc_rf_precision = (double) count_rf_Ddoc_correct / count_rf_Ddoc;
            double Ddoc_rf_recall = (double) count_rf_Ddoc_correct / count_rf_reference;
            double Ddoc_rf_f = (2 * Ddoc_rf_precision * Ddoc_rf_recall)
                    / (Ddoc_rf_precision + Ddoc_rf_recall);

            double Ddoc_rfall_precision = (double) (count_rfap_Ddoc_correct + count_rf_Ddoc_correct)
                    / (count_rfap_Ddoc + count_rf_Ddoc);
            double Ddoc_rfall_recall = (double) (count_rfap_Ddoc_correct + count_rf_Ddoc_correct)
                    / (count_rfap_reference + count_rf_reference);
            double Ddoc_rfall_f = (2 * Ddoc_rfall_precision * Ddoc_rfall_recall)
                    / (Ddoc_rfall_precision + Ddoc_rfall_recall);

            // GROBID
            double grobid_rfap_precision = (double) count_rfap_correct / count_rfap;
            double grobid_rfap_recall = (double) count_rfap_correct / count_rfap_reference;
            double grobid_rfap_f = (2 * grobid_rfap_precision * grobid_rfap_recall)
                    / (grobid_rfap_precision + grobid_rfap_recall);

            double grobid_rf_precision = (double) count_rf_correct / count_rf;
            double grobid_rf_recall = (double) count_rf_correct / count_rf_reference;
            double grobid_rf_f = (2 * grobid_rf_precision * grobid_rf_recall)
                    / (grobid_rf_precision + grobid_rf_recall);

            double grobid_rfall_precision = (double) (count_rfap_correct + count_rf_correct)
                    / (count_rf + count_rfap);
            double grobid_rfall_recall = (double) (count_rfap_correct + count_rf_correct)
                    / (count_rfap_reference + count_rf_reference);
            double grobid_rfall_f = (2 * grobid_rfall_precision * grobid_rfall_recall)
                    / (grobid_rfall_precision + grobid_rfall_recall);

            // Ddoc ? GROBID
            double DdocIGROBID_rfap_precision = (double) count_rfap_DdocIGROBID_correct / count_rfap_DdocIGROBID;
            double DdocIGROBID_rfap_recall = (double) count_rfap_DdocIGROBID_correct / count_rfap_reference;
            double DdocIGROBID_rfap_f = (2 * DdocIGROBID_rfap_precision * DdocIGROBID_rfap_recall)
                    / (DdocIGROBID_rfap_precision + DdocIGROBID_rfap_recall);

            double DdocIGROBID_rf_precision = (double) count_rf_DdocIGROBID_correct / count_rf_DdocIGROBID;
            double DdocIGROBID_rf_recall = (double) count_rf_DdocIGROBID_correct / count_rf_reference;
            double DdocIGROBID_rf_f = (2 * DdocIGROBID_rf_precision * DdocIGROBID_rf_recall)
                    / (DdocIGROBID_rf_precision + DdocIGROBID_rf_recall);

            double DdocIGROBID_rfall_precision = (double) (count_rfap_DdocIGROBID_correct
                    + count_rf_DdocIGROBID_correct)
                    / (count_rfap_DdocIGROBID + count_rf_DdocIGROBID);
            double DdocIGROBID_rfall_recall = (double) (count_rfap_DdocIGROBID_correct + count_rf_DdocIGROBID_correct)
                    / (count_rfap_reference + count_rf_reference);
            double DdocIGROBID_rfall_f = (2 * DdocIGROBID_rfall_precision * DdocIGROBID_rfall_recall)
                    / (DdocIGROBID_rfall_precision + DdocIGROBID_rfall_recall);

            // Ddoc U GROBID
            double DdocUGROBID_rfap_precision = (double) count_rfap_DdocUGROBID_correct / count_rfap_DdocUGROBID;
            double DdocUGROBID_rfap_recall = (double) count_rfap_DdocUGROBID_correct / count_rfap_reference;
            double DdocUGROBID_rfap_f = (2 * DdocUGROBID_rfap_precision * DdocUGROBID_rfap_recall)
                    / (DdocUGROBID_rfap_precision + DdocUGROBID_rfap_recall);

            double DdocUGROBID_rf_precision = (double) count_rf_DdocUGROBID_correct / count_rf_DdocUGROBID;
            double DdocUGROBID_rf_recall = (double) count_rf_DdocUGROBID_correct / count_rf_reference;
            double DdocUGROBID_rf_f = (2 * DdocUGROBID_rf_precision * DdocUGROBID_rf_recall)
                    / (DdocUGROBID_rf_precision + DdocUGROBID_rf_recall);

            double DdocUGROBID_rfall_precision = (double) (count_rfap_DdocUGROBID_correct
                    + count_rf_DdocUGROBID_correct)
                    / (count_rfap_DdocUGROBID + count_rf_DdocUGROBID);
            double DdocUGROBID_rfall_recall = (double) (count_rfap_DdocUGROBID_correct + count_rf_DdocUGROBID_correct)
                    / (count_rfap_reference + count_rf_reference);
            double DdocUGROBID_rfall_f = (2 * DdocUGROBID_rfall_precision * DdocUGROBID_rfall_recall)
                    / (DdocUGROBID_rfall_precision + DdocUGROBID_rfall_recall);

            // print the report
            System.out.println("___________________________________________________________");
            System.out.println("RFAP: ");
            System.out.println("\t\tPrecision\tRecall\t\tF-score");
            System.out.println(
                    "ACE\t\t"
                            + TextUtilities.formatTwoDecimals(ace_rfap_precision * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(ace_rfap_recall * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(ace_rfap_f * 100));
            System.out.println(
                    "Ddoc\t"
                            + TextUtilities.formatTwoDecimals(Ddoc_rfap_precision * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(Ddoc_rfap_recall * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(Ddoc_rfap_f * 100));
            System.out.println(
                    "GROBID\t"
                            + TextUtilities.formatTwoDecimals(grobid_rfap_precision * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(grobid_rfap_recall * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(grobid_rfap_f * 100));
            System.out.println(
                    "Ddoc+GROBID\t"
                            + TextUtilities.formatTwoDecimals(DdocIGROBID_rfap_precision * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(DdocIGROBID_rfap_recall * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(DdocIGROBID_rfap_f * 100));
            System.out.println(
                    "Ddoc|GROBID\t"
                            + TextUtilities.formatTwoDecimals(DdocUGROBID_rfap_precision * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(DdocUGROBID_rfap_recall * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(DdocUGROBID_rfap_f * 100));

            System.out.println("\n___________________________________________________________");
            System.out.println("RF: ");
            System.out.println("\t\tPrecision\tRecall\t\tF-score");
            System.out.println(
                    "ACE\t\t"
                            + TextUtilities.formatTwoDecimals(ace_rf_precision * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(ace_rf_recall * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(ace_rf_f * 100));
            System.out.println(
                    "Ddoc\t"
                            + TextUtilities.formatTwoDecimals(Ddoc_rf_precision * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(Ddoc_rf_recall * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(Ddoc_rf_f * 100));
            System.out.println(
                    "GROBID\t"
                            + TextUtilities.formatTwoDecimals(grobid_rf_precision * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(grobid_rf_recall * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(grobid_rf_f * 100));
            System.out.println(
                    "Ddoc+GROBID\t"
                            + TextUtilities.formatTwoDecimals(DdocIGROBID_rf_precision * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(DdocIGROBID_rf_recall * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(DdocIGROBID_rf_f * 100));
            System.out.println(
                    "Ddoc|GROBID\t"
                            + TextUtilities.formatTwoDecimals(DdocUGROBID_rf_precision * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(DdocUGROBID_rf_recall * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(DdocUGROBID_rf_f * 100));

            System.out.println("\n___________________________________________________________");
            System.out.println("All: ");
            System.out.println("\t\tPrecision\tRecall\t\tF-score");
            System.out.println(
                    "ACE\t\t"
                            + TextUtilities.formatTwoDecimals(ace_rfall_precision * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(ace_rfall_recall * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(ace_rfall_f * 100));
            System.out.println(
                    "Ddoc\t"
                            + TextUtilities.formatTwoDecimals(Ddoc_rfall_precision * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(Ddoc_rfall_recall * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(Ddoc_rfall_f * 100));
            System.out.println(
                    "GROBID\t"
                            + TextUtilities.formatTwoDecimals(grobid_rfall_precision * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(grobid_rfall_recall * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(grobid_rfall_f * 100));
            System.out.println(
                    "Ddoc+GROBID\t"
                            + TextUtilities.formatTwoDecimals(DdocIGROBID_rfall_precision * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(DdocIGROBID_rfall_recall * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(DdocIGROBID_rfall_f * 100));
            System.out.println(
                    "Ddod|GROBID\t"
                            + TextUtilities.formatTwoDecimals(DdocUGROBID_rfall_precision * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(DdocUGROBID_rfall_recall * 100)
                            + "\t\t"
                            + TextUtilities.formatTwoDecimals(DdocUGROBID_rfall_f * 100));

            // write Ddoc and reference results
            File fileOut = new File(path.getParent() + "/reference.txt");
            OutputStream os = new FileOutputStream(fileOut, false);
            Writer referenceWriter = new OutputStreamWriter(os, "UTF-8");

            //Collection.reverse(rf_reference);
            //rf_reference = new TreeMap<String, ArrayList<String>>(Collections.reverseOrder());

            System.out.println("Reference data in " + path.getParent() + "/reference.txt");
            for (Map.Entry<String, ArrayList<String>> entry : rf_reference.entrySet()) {
                dossierName = entry.getKey();
                referenceWriter.write(dossierName + ".txt\n");
                ArrayList<String> referenceListe1 = rfap_reference.get(dossierName);
                ArrayList<String> liste = entry.getValue();
                referenceWriter.write("RFAP: ");
                for (String toto : referenceListe1) {
                    referenceWriter.write(toto + " ");
                }
                referenceWriter.write("\nRF: ");
                for (String toto : liste) {
                    referenceWriter.write(toto + " ");
                }
                referenceWriter.write("\n");
            }
            referenceWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("An exception occurred while evaluating Grobid.", e);
        }
    }

}
