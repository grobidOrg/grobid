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

    @Test
    public void createTraining_digitStartFileName_producesValidXmlIds() throws Exception {
        File source = new File("src/test/resources/sample2/sample.pdf");
        File outDir = Files.createTempDirectory("training-xmlid-test").toFile();
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

        FileUtils.deleteDirectory(outDir);
    }
}
