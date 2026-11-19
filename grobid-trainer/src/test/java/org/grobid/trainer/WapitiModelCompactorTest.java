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
        File input = write("full.wapiti", MODEL);
        File output = new File(folder.getRoot(), "model.wapiti");

        WapitiModelCompactor.compact(input, output);

        assertThat(read(output), is(COMPACTED));
        assertThat(read(input), is(MODEL));
        assertThat(new File(folder.getRoot(), "model.wapiti.compacting").exists(), is(false));
    }

    @Test
    public void testCompact_alreadyCompactedModel_isUnchanged() throws Exception {
        File input = write("full.wapiti", COMPACTED);
        File output = new File(folder.getRoot(), "model.wapiti");

        WapitiModelCompactor.compact(input, output);

        assertThat(read(output), is(COMPACTED));
    }

    @Test
    public void testCompact_featureOutOfRange_shouldFail() throws Exception {
        File input = write("full.wapiti", MODEL + "16=0x1p+0\n");
        File output = new File(folder.getRoot(), "model.wapiti");

        assertThrows(GrobidException.class, () -> WapitiModelCompactor.compact(input, output));
        assertThat(output.exists(), is(false));
    }

    @Test
    public void testCompact_truncatedModel_shouldFail() throws Exception {
        File input = write("full.wapiti", "#mdl#2#3\n#rdr#1/1/0\n11:u00:%x[0,0],\n#qrk#2\n1:A,\n");
        File output = new File(folder.getRoot(), "model.wapiti");

        assertThrows(GrobidException.class, () -> WapitiModelCompactor.compact(input, output));
        assertThat(output.exists(), is(false));
    }

    @Test
    public void testCompact_inPlace() throws Exception {
        File model = write("model.wapiti", MODEL);

        WapitiModelCompactor.compact(model, model);

        assertThat(read(model), is(COMPACTED));
    }

    @Test
    public void testMain_compactsEveryModelUnderTheDirectories() throws Exception {
        File header = new File(folder.getRoot(), "models/header");
        File date = new File(folder.getRoot(), "models/date");
        header.mkdirs();
        date.mkdirs();
        Files.write(new File(header, "model.wapiti").toPath(), MODEL.getBytes(StandardCharsets.UTF_8));
        Files.write(new File(date, "model.wapiti").toPath(), MODEL.getBytes(StandardCharsets.UTF_8));
        Files.write(new File(date, "other.wapiti").toPath(), MODEL.getBytes(StandardCharsets.UTF_8));

        WapitiModelCompactor.main(new String[]{new File(folder.getRoot(), "models").getPath()});

        assertThat(read(new File(header, "model.wapiti")), is(COMPACTED));
        assertThat(read(new File(date, "model.wapiti")), is(COMPACTED));
        assertThat(read(new File(date, "other.wapiti")), is(MODEL));
    }

    @Test
    public void testMain_noModelFound_shouldFail() throws Exception {
        File empty = folder.newFolder("empty");

        assertThrows(GrobidException.class, () -> WapitiModelCompactor.main(new String[]{empty.getPath()}));
    }
}
