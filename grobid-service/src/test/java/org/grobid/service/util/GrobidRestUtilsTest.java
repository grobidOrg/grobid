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
package org.grobid.service.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Created by lfoppiano on 30/11/16.
 */
public class GrobidRestUtilsTest {
    @Test
    public void getAnnotationFor_validType_shouldWork() throws Exception {
        assertThat(GrobidRestUtils.getAnnotationFor(1), is(GrobidRestUtils.Annotation.BLOCK));
        assertThat(GrobidRestUtils.getAnnotationFor(2), is(GrobidRestUtils.Annotation.FIGURE));
        assertThat(GrobidRestUtils.getAnnotationFor(0), is(GrobidRestUtils.Annotation.CITATION));
    }

    @Test
    public void getAnnotationFor_invalidType_shouldWork() throws Exception {
        assertNull(GrobidRestUtils.getAnnotationFor(-1));
        assertNull(GrobidRestUtils.getAnnotationFor(4));
        assertNull(GrobidRestUtils.getAnnotationFor(3));
    }

}
