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
package org.grobid.core.engines;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

import org.grobid.core.test.EngineTest;

/**
 * Smoke test for issue #356: training data generated from a PDF whose file name
 * starts with a digit and contains spaces must only carry valid NCName xml:id
 * values.
 */
public class TrainingXmlIdSmokeTest extends EngineTest {

    private static final Pattern XML_ID = Pattern.compile("xml:id=\"([^\"]*)\"");
    private static final Pattern NCNAME = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}._-]*");
    private static final Pattern FILE_DESC_XML_ID = Pattern.compile("<fileDesc xml:id=\"([^\"]*)\"");

    private File outDir;

    @After
    public void cleanUpOutputDirectory() throws Exception {
        if (outDir != null) {
            FileUtils.deleteDirectory(outDir);
        }
    }

    @Test
    public void createTraining_digitStartFileName_producesValidXmlIds() throws Exception {
        File source = new File("src/test/resources/sample2/sample.pdf");
        outDir = Files.createTempDirectory("training-xmlid-test").toFile();
        File pdf = new File(outDir, "123 sample report.pdf");
        FileUtils.copyFile(source, pdf);

        engine.createTraining(pdf, outDir.getAbsolutePath(), outDir.getAbsolutePath(), 0, null);

        File[] teiFiles = outDir.listFiles((dir, name) -> name.endsWith(".tei.xml"));
        assertThat("no training TEI files generated", teiFiles.length, greaterThan(0));

        StringBuilder errors = new StringBuilder();
        int checked = 0;
        for (File tei : teiFiles) {
            String content = FileUtils.readFileToString(tei, StandardCharsets.UTF_8);
            Matcher m = XML_ID.matcher(content);
            while (m.find()) {
                checked++;
                String id = m.group(1);
                if (!NCNAME.matcher(id).matches()) {
                    errors.append(tei.getName()).append(": invalid xml:id \"").append(id).append("\"\n");
                }
            }
        }
        System.out.println("Checked " + checked + " xml:id values across " + teiFiles.length + " TEI files");
        assertThat("no xml:id found in generated TEI files", checked, greaterThan(0));
        assertTrue(errors.toString(), errors.length() == 0);

        // the training file names carry the sanitized base name WITHOUT the leading
        // underscore, which is only prepended to the xml:id values to make them valid
        // NCNames; the trainers pair a TEI file with its raw feature file by stripping
        // the leading underscore from the xml:id
        String expectedBaseName = "123_sample_report";
        String expectedXmlId = "_123_sample_report";
        File[] trainingFiles = outDir.listFiles((dir, name) -> name.contains(".training"));
        assertThat("no training files generated", trainingFiles.length, greaterThan(0));
        for (File trainingFile : trainingFiles) {
            assertTrue(
                    "unexpected training file name: " + trainingFile.getName(),
                    trainingFile.getName().startsWith(expectedBaseName + ".training"));
        }

        // every fileDesc xml:id, whatever the model, must carry the document xml:id
        for (File tei : teiFiles) {
            String content = FileUtils.readFileToString(tei, StandardCharsets.UTF_8);
            Matcher m = FILE_DESC_XML_ID.matcher(content);
            while (m.find()) {
                assertTrue(
                        tei.getName()
                                + ": fileDesc xml:id \""
                                + m.group(1)
                                + "\" does not match the document xml:id",
                        m.group(1).equals(expectedXmlId));
            }
        }

        File headerTei = new File(outDir, expectedBaseName + ".training.header.tei.xml");
        if (headerTei.exists()) {
            String headerContent = FileUtils.readFileToString(headerTei, StandardCharsets.UTF_8);
            assertTrue(
                    "header TEI xml:id does not carry the expected NCName",
                    headerContent.contains("xml:id=\"" + expectedXmlId + "\""));
            assertTrue(
                    "raw header feature file missing for " + headerTei.getName(),
                    new File(outDir, expectedBaseName + ".training.header").exists());
        }
    }
}
