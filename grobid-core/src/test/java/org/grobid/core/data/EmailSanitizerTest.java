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
package org.grobid.core.data;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.grobid.core.data.util.EmailSanitizer;

public class EmailSanitizerTest {
    EmailSanitizer sanitizer = new EmailSanitizer();

    @Test
    public void testEmailSanitizer() {
        a("test@gmail.com", "+++test@gmail.com");
        a(l("abc@gmail.com", "z.jang@gmail.com"), "abc/z.jang@gmail.com");
    }

    private void a(String expected, String actual) {
        assertEquals(l(expected), sanitizer.splitAndClean(l(actual)));
    }

    private void a(List<String> expected, String actual) {
        assertEquals(expected, sanitizer.splitAndClean(l(actual)));
    }

    private static <T> List<T> l(T... els) {
        return Arrays.asList(els);
    }

}
