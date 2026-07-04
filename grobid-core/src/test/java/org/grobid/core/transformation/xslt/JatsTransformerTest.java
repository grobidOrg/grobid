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
package org.grobid.core.transformation.xslt;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class JatsTransformerTest {
    JATSTransformer target;

    @Before
    public void setUp() throws Exception {
        target = new JATSTransformer();
    }

    @Test
    @Ignore("Not ready yet")
    public void testTransform_teiHeader() throws Exception {
        String teiInput = IOUtils
                .toString(this.getClass().getResourceAsStream("/xslt/sample1.tei.header.xml"), "UTF-8");
        String output = target.transform(teiInput);
        System.out.println(output);
    }

    @Test
    @Ignore("Not ready yet")
    public void testTransform_teiFulltext() throws Exception {
        String teiInput = IOUtils
                .toString(this.getClass().getResourceAsStream("/xslt/sample2.tei.fulltext.xml"), "UTF-8");
        String output = target.transform(teiInput);
        System.out.println(output);
    }

}
