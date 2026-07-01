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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import com.google.common.collect.Lists;
import org.junit.Ignore;
import org.junit.Test;

import org.grobid.core.test.EngineTest;

@Ignore
public class UnicodeUtilTest extends EngineTest {

    @Test
    public void testNormaliseToken() throws Exception {
        String test = "´\rÓÑÔÙØØØ";
        String result = UnicodeUtil.normaliseText(test);
        assertThat("´\nÓÑÔÙØØØ", is(result));

        ArrayList<String> tokens = Lists.newArrayList(
                "½ºº´\r",
                "½ºº´\n",
                "½ºº´\t",
                "½ºº´\f",
                "½ºº´ ",
                "½ºº´\f\n",
                "½ºº´\r\t");
        for (String token : tokens) {
            assertEquals("½ºº´", UnicodeUtil.normaliseText(token.replace(" ", "").replace("\n", "")));
        }
    }

}
