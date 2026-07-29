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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.utilities.GrobidProperties;

/**
 * Environment shared by the external processes GROBID launches (currently pdfalto).
 */
class ProcessEnvironment {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessEnvironment.class);

    private ProcessEnvironment() {
    }

    /**
     * Point the child process at GROBID's own temp directory.
     * <p>
     * Since 0.6.1, pdfalto streams large page DOMs to a scratch file to bound peak
     * memory, and picks its location from {@code $TMPDIR}, falling back to
     * {@code /tmp}. In containers {@code /tmp} is usually tmpfs, i.e. RAM: spilling
     * there leaves peak memory unchanged and risks ENOSPC, which is exactly the case
     * streaming exists to avoid. GROBID already has a configured on-disk temp
     * directory ({@code grobid.temp}), so hand pdfalto that instead of letting it
     * guess.
     */
    static void setTempDirectory(ProcessBuilder builder) {
        try {
            File tempPath = GrobidProperties.getTempPath();
            if (tempPath != null && tempPath.isDirectory()) {
                builder.environment().put("TMPDIR", tempPath.getAbsolutePath());
            }
        } catch (Exception e) {
            // Never let this stop the conversion: without TMPDIR pdfalto falls back to
            // /tmp, which still works, it just may not bound memory as effectively.
            LOGGER.warn("Could not set TMPDIR for the pdfalto process, falling back to its default", e);
        }
    }
}
