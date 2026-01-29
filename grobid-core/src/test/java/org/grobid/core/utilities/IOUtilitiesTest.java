package org.grobid.core.utilities;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import static org.junit.Assert.*;

public class IOUtilitiesTest {

    @Test
    public void testwriteInFileANDreadFile() throws IOException {
        File file = File.createTempFile("temp", "test");
        IOUtilities.writeInFile(file.getAbsolutePath(), getString());
        assertEquals("Not expected value", getString(), IOUtilities.readFile(file.getAbsolutePath()));
    }

    private static String getString() {
        return "1 \" ' A \n \t \r test\n\\n \n M";
    }
}
