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
package org.grobid.core.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;

import org.grobid.core.data.BibDataSet;

public class TestReferencesParser extends EngineTest {

    public static final String TEST_RESOURCES_PATH = "./src/test/resources/test";

    //@Test
    public void testReferences() throws Exception {
        String testPath = TEST_RESOURCES_PATH;

        String pdfPath = testPath + File.separator + "Wang-paperAVE2008.pdf";
        List<BibDataSet> resRefs = engine.processReferences(new File(pdfPath), 1);

        assertNotNull(resRefs);
        assertThat(resRefs.size(), is(12));
    }

}
