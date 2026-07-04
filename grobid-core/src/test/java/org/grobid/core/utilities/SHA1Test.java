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

import org.junit.Assert;
import org.junit.Test;

public class SHA1Test {

    @Test
    public void testgetSHA1() {
        Assert.assertEquals(
                "Hashed value is not the expected one",
                "9d4e1e23bd5b727046a9e3b4b7db57bd8d6ee684",
                SHA1.getSHA1("pass"));
    }

}
