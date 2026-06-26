package org.grobid.core.document;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class DocumentSourceTest {

    @Test
    public void wrapWithUlimit_shouldExecArgumentsDirectlyWithoutShellInterpolation() {
        List<String> cmd = Arrays.asList("pdfalto", "-noLineNumbers", "/tmp/in.pdf", "/tmp/out.xml");

        List<String> wrapped = DocumentSource.wrapWithUlimit(cmd, 6291456L);

        // bash -c '<script>' <argv0> <original cmd...>
        assertThat(wrapped.get(0), is("bash"));
        assertThat(wrapped.get(1), is("-c"));
        assertThat(wrapped.get(2), is("ulimit -Sv 6291456 && exec \"$@\""));
        assertThat(wrapped.subList(4, wrapped.size()), is(cmd));
    }

    @Test
    public void wrapWithUlimit_shouldKeepMaliciousFileNameAsSingleArgument() {
        // Crafted file name from GHSA-mgxf-7mg7-qpmf: a single quote followed by shell syntax.
        String maliciousPath = "/data/poc';printf${IFS}pwned>pwned.txt;#'.pdf";
        List<String> cmd = Arrays.asList("pdfalto", maliciousPath, "/tmp/out.xml");

        List<String> wrapped = DocumentSource.wrapWithUlimit(cmd, 6291456L);

        // The malicious path must survive as one untouched argv element: it is passed to the
        // program verbatim, never spliced into the shell script string.
        assertThat(wrapped, hasItem(maliciousPath));

        // The script the shell actually parses must not contain the file name at all.
        String script = wrapped.get(2);
        assertThat(script.contains(maliciousPath), is(false));
        assertThat(script.contains("printf"), is(false));

        // Sanity: only the fixed ulimit/exec scaffolding precedes the original command.
        assertThat(
                wrapped,
                contains(
                        "bash",
                        "-c",
                        "ulimit -Sv 6291456 && exec \"$@\"",
                        "pdfalto",
                        "pdfalto",
                        maliciousPath,
                        "/tmp/out.xml"));
        assertThat(script, not(containsString("'")));
    }
}
