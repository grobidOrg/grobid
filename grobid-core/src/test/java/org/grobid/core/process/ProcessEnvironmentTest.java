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
package org.grobid.core.process;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.utilities.GrobidProperties;

public class ProcessEnvironmentTest {

    @BeforeClass
    public static void setInitialContext() throws Exception {
        GrobidProperties.getInstance();
    }

    @Test
    public void testSetTempDirectory_pointsPdfaltoAtGrobidTempPath() {
        ProcessBuilder builder = new ProcessBuilder("true");
        ProcessEnvironment.setTempDirectory(builder);

        // pdfalto reads $TMPDIR to place its page-streaming scratch file; left unset it
        // falls back to /tmp, which is tmpfs in a container and defeats the streaming.
        String tmpdir = builder.environment().get("TMPDIR");
        assertThat(tmpdir, is(notNullValue()));
        assertThat(new File(tmpdir), is(GrobidProperties.getTempPath().getAbsoluteFile()));
        assertThat(new File(tmpdir).isDirectory(), is(true));
    }
}
