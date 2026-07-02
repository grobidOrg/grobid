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

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class IOUtilitiesTest {

    @Test
    public void testwriteInFileANDreadFile() throws IOException {
        File file = File.createTempFile("temp", "test");
        IOUtilities.writeInFile(file.getAbsolutePath(), getString());
        assertEquals("Not expected value", getString(), IOUtilities.readFile(file.getAbsolutePath()));
    }

    private static String getString() {
        return "1 \" ' A \n \t \r test\n\\n \n M";
    }
}
