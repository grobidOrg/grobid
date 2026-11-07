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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.grobid.core.exceptions.GrobidException;

public class WapitiModelCompactorTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    // 2 labels; observations: a (unigram, features 0-1), b (bigram, 2-5), c (unigram, 6-7),
    // d (both, 8-9 then 10-13), é (unigram, 14-15); only b and d have active features
    private static final String MODEL = "#mdl#2#3\n"
            + "#rdr#1/1/0\n"
            + "11:u00:%x[0,0],\n"
            + "#qrk#2\n"
            + "1:A,\n"
            + "1:B,\n"
            + "#qrk#5\n"
            + "3:u:a,\n"
            + "1:b,\n"
            + "3:u:c,\n"
            + "3:*:d,\n"
            + "4:u:é,\n"
            + "2=0x1.8p-1\n"
            + "9=-0x1p+0\n"
            + "12=0x1.2p-3\n";

    private static final String COMPACTED = "#mdl#2#3\n"
            + "#rdr#1/1/0\n"
            + "11:u00:%x[0,0],\n"
            + "#qrk#2\n"
            + "1:A,\n"
            + "1:B,\n"
            + "#qrk#2\n"
            + "1:b,\n"
            + "3:*:d,\n"
            + "0=0x1.8p-1\n"
            + "5=-0x1p+0\n"
            + "8=0x1.2p-3\n";

    private File write(String name, String content) throws Exception {
        File file = new File(folder.getRoot(), name);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void testCompact_dropsUnusedObservationsAndRenumbersFeatures() throws Exception {
        File input = write("model.extended.wapiti", MODEL);
        File output = new File(folder.getRoot(), "model.wapiti");

        WapitiModelCompactor.compact(input, output);

        assertThat(read(output), is(COMPACTED));
        assertThat(read(input), is(MODEL));
        assertThat(new File(folder.getRoot(), "model.wapiti.compacting").exists(), is(false));
    }

    @Test
    public void testCompact_alreadyCompactedModel_isUnchanged() throws Exception {
        File input = write("model.extended.wapiti", COMPACTED);
        File output = new File(folder.getRoot(), "model.wapiti");

        WapitiModelCompactor.compact(input, output);

        assertThat(read(output), is(COMPACTED));
    }

    @Test
    public void testCompact_featureOutOfRange_shouldFail() throws Exception {
        File input = write("model.extended.wapiti", MODEL + "16=0x1p+0\n");
        File output = new File(folder.getRoot(), "model.wapiti");

        assertThrows(GrobidException.class, () -> WapitiModelCompactor.compact(input, output));
        assertThat(output.exists(), is(false));
    }

    @Test
    public void testCompact_truncatedModel_shouldFail() throws Exception {
        File input = write("model.extended.wapiti", "#mdl#2#3\n#rdr#1/1/0\n11:u00:%x[0,0],\n#qrk#2\n1:A,\n");
        File output = new File(folder.getRoot(), "model.wapiti");

        assertThrows(GrobidException.class, () -> WapitiModelCompactor.compact(input, output));
        assertThat(output.exists(), is(false));
    }

    @Test
    public void testExtendedModelPath() {
        assertThat(
                WapitiModelCompactor.extendedModelPath(new File("/models/header/model.wapiti.new")).getPath(),
                is("/models/header/model.extended.wapiti"));
        assertThat(
                WapitiModelCompactor.extendedModelPath(new File("/models/header/model.wapiti")).getPath(),
                is("/models/header/model.extended.wapiti"));
        assertThat(
                WapitiModelCompactor.extendedModelPath(new File("/tmp/header_nfold_0_abc.wapiti")).getPath(),
                is("/tmp/header_nfold_0_abc.extended.wapiti"));
        assertThat(
                WapitiModelCompactor.extendedModelPath(new File("/tmp/custom-model")).getPath(),
                is("/tmp/custom-model.extended"));
    }
}
