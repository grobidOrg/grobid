package org.grobid.core.jni;

import org.junit.*;

import static org.junit.Assert.*;

/**
 * Separate test class for JEPThreadPool shutdown tests This avoids affecting other tests that depend on the singleton
 */
public class JEPThreadPoolShutdownTest {

    @Test
    public void testShutdown() {
        // This test verifies that shutdown doesn't throw exceptions
        // In a real environment, this would close all JEP instances and shutdown the executor
        JEPThreadPool threadPool = JEPThreadPool.getInstance();
        try {
            threadPool.shutdown();
        } catch (Exception e) {
            fail("shutdown should not throw exceptions: " + e.getMessage());
        }
    }

    @Test
    public void testCloseCurrentJEPInstance() {
        // This test verifies that closeCurrentJEPInstance doesn't throw exceptions
        // In a real environment, this would close the JEP instance for the current thread
        JEPThreadPool threadPool = JEPThreadPool.getInstance();
        try {
            threadPool.closeCurrentJEPInstance();
        } catch (Exception e) {
            fail("closeCurrentJEPInstance should not throw exceptions: " + e.getMessage());
        }
    }
}
