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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class DataSetContextExtractorTest {

    @Test
    public void testRefEscapes() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/test/tei-escape.xml");
        String tei = IOUtils.toString(is, StandardCharsets.UTF_8);
        is.close();
        DataSetContextExtractor.getCitationReferences(tei);
    }

}
