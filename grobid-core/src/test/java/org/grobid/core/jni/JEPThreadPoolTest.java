package org.grobid.core.jni;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Unit tests for JEPThreadPool Tests singleton behavior, thread management, and task execution
 */
public class JEPThreadPoolTest {

    private JEPThreadPool threadPool;
    private static final AtomicInteger taskCounter = new AtomicInteger(0);

    @AfterClass
    public static void tearDownClass() {
        JEPThreadPool.getInstance().shutdown();
    }

    @Before
    public void setUp() {
        threadPool = JEPThreadPool.getInstance();
        taskCounter.set(0);
    }

    @After
    public void tearDown() {
        threadPool.closeCurrentJEPInstance();
    }

    @Test
    public void testSingletonBehavior() {
        // Test that getInstance() always returns the same instance
        JEPThreadPool instance1 = JEPThreadPool.getInstance();
        JEPThreadPool instance2 = JEPThreadPool.getInstance();

        assertNotNull("Instance should not be null", instance1);
        assertSame("Should return the same instance", instance1, instance2);
    }

    @Test
    public void testRunTask() throws InterruptedException {
        final AtomicBoolean taskExecuted = new AtomicBoolean(false);

        Runnable task = () -> {
            taskExecuted.set(true);
            taskCounter.incrementAndGet();
        };

        threadPool.run(task);

        assertTrue("Task should have been executed", taskExecuted.get());
        assertEquals("Task counter should be incremented", 1, taskCounter.get());
    }

    @Test
    public void testCallTask() throws InterruptedException, ExecutionException {
        final String expectedResult = "test result";

        Callable<String> task = () -> {
            taskCounter.incrementAndGet();
            return expectedResult;
        };

        String result = threadPool.call(task);

        assertEquals("Should return expected result", expectedResult, result);
        assertEquals("Task counter should be incremented", 1, taskCounter.get());
    }

    @Test
    public void testMultipleTasks() throws InterruptedException, ExecutionException {
        final int numTasks = 5;
        final AtomicInteger completedTasks = new AtomicInteger(0);

        // Run multiple tasks
        for (int i = 0; i < numTasks; i++) {
            final int taskId = i;
            Runnable task = () -> {
                completedTasks.incrementAndGet();
                taskCounter.incrementAndGet();
            };
            threadPool.run(task);
        }

        assertEquals("All tasks should be completed", numTasks, completedTasks.get());
        assertEquals("Task counter should reflect all tasks", numTasks, taskCounter.get());
    }

    @Test
    public void testCallableWithException() {
        Callable<String> failingTask = () -> {
            throw new RuntimeException("Test exception");
        };

        try {
            threadPool.call(failingTask);
            fail("Should throw ExecutionException");
        } catch (InterruptedException e) {
            fail("Should not throw InterruptedException");
        } catch (ExecutionException e) {
            assertTrue("Should contain the original exception", e.getCause() instanceof RuntimeException);
            assertEquals("Should have the correct message", "Test exception", e.getCause().getMessage());
        }
    }

    @Test
    public void testRunnableWithException() throws InterruptedException {
        Runnable failingTask = () -> {
            throw new RuntimeException("Test exception");
        };

        try {
            threadPool.run(failingTask);
            // The run method doesn't re-throw exceptions from the task
            // This is expected behavior - the exception is logged but not propagated
        } catch (Exception e) {
            // If an exception is thrown, it should be a RuntimeException
            assertTrue("Should be RuntimeException", e instanceof RuntimeException);
            assertEquals("Should have the correct message", "Test exception", e.getMessage());
        }
    }

    @Test
    public void testCloseCurrentJEPInstance() {
        // This test verifies that closeCurrentJEPInstance doesn't throw exceptions
        // In a real environment, this would close the JEP instance for the current thread
        try {
            threadPool.closeCurrentJEPInstance();
        } catch (Exception e) {
            fail("closeCurrentJEPInstance should not throw exceptions: " + e.getMessage());
        }
    }

    // Note: shutdown test is moved to a separate test class to avoid affecting other tests

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        final int numThreads = 3;
        final AtomicInteger successfulTasks = new AtomicInteger(0);
        final AtomicInteger failedTasks = new AtomicInteger(0);

        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                try {
                    Runnable task = () -> {
                        successfulTasks.incrementAndGet();
                        taskCounter.incrementAndGet();
                    };
                    threadPool.run(task);
                } catch (Exception e) {
                    failedTasks.incrementAndGet();
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

        assertEquals("All tasks should succeed", numThreads, successfulTasks.get());
        assertEquals("No tasks should fail", 0, failedTasks.get());
        assertEquals("Task counter should reflect all tasks", numThreads, taskCounter.get());
    }

    @Test
    public void testTaskExecutionOrder() throws InterruptedException {
        final StringBuilder executionOrder = new StringBuilder();

        // Submit tasks in order
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            Runnable task = () -> {
                executionOrder.append(taskId);
                taskCounter.incrementAndGet();
            };
            threadPool.run(task);
        }

        assertEquals("Tasks should execute in order", "012", executionOrder.toString());
        assertEquals("Task counter should reflect all tasks", 3, taskCounter.get());
    }

    @Test
    public void testLongRunningTask() throws InterruptedException, ExecutionException {
        final long startTime = System.currentTimeMillis();
        final long expectedDuration = 100; // 100ms

        Callable<String> longTask = () -> {
            Thread.sleep(expectedDuration);
            taskCounter.incrementAndGet();
            return "completed";
        };

        String result = threadPool.call(longTask);
        long actualDuration = System.currentTimeMillis() - startTime;

        assertEquals("Should return expected result", "completed", result);
        assertTrue("Task should take at least the expected duration", actualDuration >= expectedDuration);
        assertEquals("Task counter should be incremented", 1, taskCounter.get());
    }

    @Test
    public void testNullTask() throws InterruptedException {
        // Test with null Runnable
        try {
            threadPool.run(null);
            fail("Should throw NullPointerException for null Runnable");
        } catch (NullPointerException e) {
            // Expected
        }

        // Test with null Callable
        try {
            threadPool.call(null);
            fail("Should throw NullPointerException for null Callable");
        } catch (NullPointerException e) {
            // Expected
        } catch (ExecutionException e) {
            fail("Should throw NullPointerException, not ExecutionException");
        }
    }

    @Test
    public void testInterruptedTask() throws InterruptedException {
        final AtomicBoolean taskStarted = new AtomicBoolean(false);
        final AtomicBoolean taskInterrupted = new AtomicBoolean(false);

        Runnable longTask = () -> {
            taskStarted.set(true);
            try {
                Thread.sleep(1000); // Sleep for 1 second
            } catch (InterruptedException e) {
                taskInterrupted.set(true);
                Thread.currentThread().interrupt();
            }
        };

        // Start the task in a separate thread
        Thread taskThread = new Thread(() -> {
            try {
                threadPool.run(longTask);
            } catch (InterruptedException e) {
                taskInterrupted.set(true);
            }
        });

        taskThread.start();

        // Wait for task to start
        Thread.sleep(100);
        assertTrue("Task should have started", taskStarted.get());

        // Interrupt the task
        taskThread.interrupt();
        taskThread.join(2000);

        assertTrue("Task should have been interrupted", taskInterrupted.get());
    }
}
