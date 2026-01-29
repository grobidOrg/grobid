package org.grobid.core.data;

import org.grobid.core.data.util.EmailSanitizer;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

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
