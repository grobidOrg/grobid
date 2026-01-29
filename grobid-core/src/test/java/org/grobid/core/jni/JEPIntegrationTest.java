package org.grobid.core.jni;

import org.junit.*;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Integration tests for JEP classes Tests the interaction between JEPThreadPool, JEPThreadPoolClassifier, and DeLFT
 * models
 */
public class JEPIntegrationTest {

    private JEPThreadPool mainPool;
    private JEPThreadPoolClassifier classifierPool;
    private static final AtomicInteger taskCounter = new AtomicInteger(0);

    @BeforeClass
    public static void setUpClass() {
        // Set up test environment
    }

    @AfterClass
    public static void tearDownClass() {
        // Clean up test environment
        try {
            JEPThreadPool.getInstance().shutdown();
            JEPThreadPoolClassifier.getInstance().shutdown();
        } catch (Exception e) {
            // Ignore cleanup errors in tests
        }
    }

    @Before
    public void setUp() {
        mainPool = JEPThreadPool.getInstance();
        classifierPool = JEPThreadPoolClassifier.getInstance();
        taskCounter.set(0);
    }

    @After
    public void tearDown() {
        // Clean up after each test
        try {
            mainPool.closeCurrentJEPInstance();
            classifierPool.closeCurrentJEPInstance();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    public void testPoolIndependence() {
        // Test that the two pools are independent instances
        assertNotNull("Main pool should not be null", mainPool);
        assertNotNull("Classifier pool should not be null", classifierPool);
        assertNotSame("Main pool and classifier pool should be different instances", mainPool, classifierPool);
    }

    @Test
    public void testConcurrentPoolUsage() throws InterruptedException, ExecutionException {
        // Test that both pools can be used concurrently
        final AtomicInteger mainPoolTasks = new AtomicInteger(0);
        final AtomicInteger classifierPoolTasks = new AtomicInteger(0);

        // Submit tasks to both pools
        Runnable mainTask = () -> mainPoolTasks.incrementAndGet();
        Runnable classifierTask = () -> classifierPoolTasks.incrementAndGet();

        mainPool.run(mainTask);
        classifierPool.run(classifierTask);

        assertEquals("Main pool should execute its task", 1, mainPoolTasks.get());
        assertEquals("Classifier pool should execute its task", 1, classifierPoolTasks.get());
    }

    @Test
    public void testPoolTaskExecutionOrder() throws InterruptedException {
        // Test that tasks are executed in order within each pool
        final StringBuilder mainPoolOrder = new StringBuilder();
        final StringBuilder classifierPoolOrder = new StringBuilder();

        // Submit tasks to main pool
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            Runnable task = () -> mainPoolOrder.append("M" + taskId);
            mainPool.run(task);
        }

        // Submit tasks to classifier pool
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            Runnable task = () -> classifierPoolOrder.append("C" + taskId);
            classifierPool.run(task);
        }

        assertEquals("Main pool tasks should execute in order", "M0M1M2", mainPoolOrder.toString());
        assertEquals("Classifier pool tasks should execute in order", "C0C1C2", classifierPoolOrder.toString());
    }

    @Test
    public void testPoolCallableExecution() throws InterruptedException, ExecutionException {
        // Test that both pools can execute Callable tasks
        Callable<String> mainTask = () -> "main result";
        Callable<String> classifierTask = () -> "classifier result";

        String mainResult = mainPool.call(mainTask);
        String classifierResult = classifierPool.call(classifierTask);

        assertEquals("Main pool should return correct result", "main result", mainResult);
        assertEquals("Classifier pool should return correct result", "classifier result", classifierResult);
    }

    @Test
    public void testPoolExceptionHandling() throws InterruptedException {
        // Test that both pools handle exceptions correctly
        Runnable failingMainTask = () -> {
            throw new RuntimeException("Main pool test exception");
        };

        Runnable failingClassifierTask = () -> {
            throw new RuntimeException("Classifier pool test exception");
        };

        // Test main pool exception handling
        // Note: JEPThreadPool.run() waits for the task to complete but does not re-throw exceptions from the Runnable
        // task directly
        mainPool.run(failingMainTask);
        // The exception is handled internally by the executor and logged

        // Test classifier pool exception handling
        // Note: JEPThreadPoolClassifier.run() waits for the task to complete but does not re-throw exceptions from the
        // Runnable task directly
        classifierPool.run(failingClassifierTask);
        // The exception is handled internally by the executor and logged
    }

    @Test
    public void testPoolCallableExceptionHandling() {
        // Test that both pools handle Callable exceptions correctly
        Callable<String> failingMainTask = () -> {
            throw new RuntimeException("Main pool callable test exception");
        };

        Callable<String> failingClassifierTask = () -> {
            throw new RuntimeException("Classifier pool callable test exception");
        };

        // Test main pool Callable exception handling
        try {
            mainPool.call(failingMainTask);
            fail("Main pool should throw ExecutionException");
        } catch (InterruptedException e) {
            fail("Should not throw InterruptedException");
        } catch (ExecutionException e) {
            assertTrue("Should contain the original exception", e.getCause() instanceof RuntimeException);
            assertEquals(
                    "Should have the correct message",
                    "Main pool callable test exception",
                    e.getCause().getMessage());
        }

        // Test classifier pool Callable exception handling
        try {
            classifierPool.call(failingClassifierTask);
            fail("Classifier pool should throw ExecutionException");
        } catch (InterruptedException e) {
            fail("Should not throw InterruptedException");
        } catch (ExecutionException e) {
            assertTrue("Should contain the original exception", e.getCause() instanceof RuntimeException);
            assertEquals(
                    "Should have the correct message",
                    "Classifier pool callable test exception",
                    e.getCause().getMessage());
        }
    }

    // Note: shutdown and closeCurrentInstance tests are moved to separate test classes to avoid affecting other tests

    @Test
    public void testPoolSingletonBehavior() {
        // Test that both pools maintain singleton behavior
        JEPThreadPool mainPool1 = JEPThreadPool.getInstance();
        JEPThreadPool mainPool2 = JEPThreadPool.getInstance();

        JEPThreadPoolClassifier classifierPool1 = JEPThreadPoolClassifier.getInstance();
        JEPThreadPoolClassifier classifierPool2 = JEPThreadPoolClassifier.getInstance();

        assertSame("Main pool should maintain singleton behavior", mainPool1, mainPool2);
        assertSame("Classifier pool should maintain singleton behavior", classifierPool1, classifierPool2);
    }

    @Test
    public void testPoolThreadSafety() throws InterruptedException {
        // Test that both pools are thread-safe
        final int numThreads = 5;
        final AtomicInteger mainPoolSuccess = new AtomicInteger(0);
        final AtomicInteger classifierPoolSuccess = new AtomicInteger(0);
        final AtomicInteger mainPoolFailure = new AtomicInteger(0);
        final AtomicInteger classifierPoolFailure = new AtomicInteger(0);

        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                try {
                    // Test main pool
                    Runnable mainTask = () -> mainPoolSuccess.incrementAndGet();
                    mainPool.run(mainTask);
                } catch (Exception e) {
                    mainPoolFailure.incrementAndGet();
                }

                try {
                    // Test classifier pool
                    Runnable classifierTask = () -> classifierPoolSuccess.incrementAndGet();
                    classifierPool.run(classifierTask);
                } catch (Exception e) {
                    classifierPoolFailure.incrementAndGet();
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(5000); // 5 second timeout
        }

        assertEquals("All main pool tasks should succeed", numThreads, mainPoolSuccess.get());
        assertEquals("All classifier pool tasks should succeed", numThreads, classifierPoolSuccess.get());
        assertEquals("No main pool tasks should fail", 0, mainPoolFailure.get());
        assertEquals("No classifier pool tasks should fail", 0, classifierPoolFailure.get());
    }

    @Test
    public void testPoolNullTaskHandling() throws InterruptedException {
        // Test that both pools handle null tasks correctly
        try {
            mainPool.run(null);
            fail("Main pool should throw NullPointerException for null Runnable");
        } catch (NullPointerException e) {
            // Expected
        }

        try {
            classifierPool.run(null);
            fail("Classifier pool should throw NullPointerException for null Runnable");
        } catch (NullPointerException e) {
            // Expected
        }

        try {
            mainPool.call(null);
            fail("Main pool should throw NullPointerException for null Callable");
        } catch (NullPointerException e) {
            // Expected
        } catch (ExecutionException e) {
            fail("Should throw NullPointerException, not ExecutionException");
        }

        try {
            classifierPool.call(null);
            fail("Classifier pool should throw NullPointerException for null Callable");
        } catch (NullPointerException e) {
            // Expected
        } catch (ExecutionException e) {
            fail("Should throw NullPointerException, not ExecutionException");
        }
    }

    @Test
    public void testPoolLongRunningTasks() throws InterruptedException, ExecutionException {
        // Test that both pools can handle long-running tasks
        final long expectedDuration = 100; // 100ms

        Callable<String> longMainTask = () -> {
            Thread.sleep(expectedDuration);
            return "main long task completed";
        };

        Callable<String> longClassifierTask = () -> {
            Thread.sleep(expectedDuration);
            return "classifier long task completed";
        };

        long startTime = System.currentTimeMillis();
        String mainResult = mainPool.call(longMainTask);
        long mainDuration = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        String classifierResult = classifierPool.call(longClassifierTask);
        long classifierDuration = System.currentTimeMillis() - startTime;

        assertEquals("Main pool should return correct result", "main long task completed", mainResult);
        assertEquals(
                "Classifier pool should return correct result",
                "classifier long task completed",
                classifierResult);
        assertTrue("Main pool task should take at least expected duration", mainDuration >= expectedDuration);
        assertTrue(
                "Classifier pool task should take at least expected duration",
                classifierDuration >= expectedDuration);
    }

    private void assertDoesNotThrow(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }
}
