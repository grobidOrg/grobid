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
package org.grobid.core.utilities;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class IOUtilitiesTest {

    @Test
    public void testwriteInFileANDreadFile() throws IOException {
        File file = File.createTempFile("temp", "test");
        IOUtilities.writeInFile(file.getAbsolutePath(), getString());
        assertEquals("Not expected value", getString(), IOUtilities.readFile(file.getAbsolutePath()));
    }

    @Test
    public void testWriteInputFileToTargetFile() throws IOException {
        File targetDir = Files.createTempDirectory("writeInputFile-test").toFile();
        File target = new File(targetDir, "002634.full.pdf");

        File written = IOUtilities.writeInputFile(
                new ByteArrayInputStream(getString().getBytes(StandardCharsets.UTF_8)),
                target);

        assertNotNull(written);
        assertEquals(target.getAbsolutePath(), written.getAbsolutePath());
        assertEquals("002634.full.pdf", written.getName());
        assertEquals(getString(), new String(Files.readAllBytes(written.toPath()), StandardCharsets.UTF_8));

        written.delete();
        targetDir.delete();
    }

    private static String getString() {
        return "1 \" ' A \n \t \r test\n\\n \n M";
    }
}
