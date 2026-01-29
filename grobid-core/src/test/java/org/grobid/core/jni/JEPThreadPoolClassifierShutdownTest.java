package org.grobid.core.jni;

import org.junit.*;

import static org.junit.Assert.*;

/**
 * Separate test class for JEPThreadPoolClassifier shutdown tests This avoids affecting other tests that depend on the
 * singleton
 */
public class JEPThreadPoolClassifierShutdownTest {

    @Test
    public void testShutdown() {
        JEPThreadPoolClassifier threadPool = JEPThreadPoolClassifier.getInstance();
        try {
            threadPool.shutdown();
        } catch (Exception e) {
            fail("shutdown should not throw exceptions: " + e.getMessage());
        }
    }

    @Test
    public void testCloseCurrentJEPInstance() {
        JEPThreadPoolClassifier threadPool = JEPThreadPoolClassifier.getInstance();
        try {
            threadPool.closeCurrentJEPInstance();
        } catch (Exception e) {
            fail("closeCurrentJEPInstance should not throw exceptions: " + e.getMessage());
        }
    }
}
