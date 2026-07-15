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
package org.grobid.core.jni;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * These tests exercise the executor and shutdown logic only, no Python
 * interpreter is created so no native JEP installation is required.
 * The shutdown test must run last as it terminates the singleton's executor.
 */
@TestMethodOrder(OrderAnnotation.class)
public class JEPThreadPoolClassifierTest {

    @Test
    @Order(1)
    public void testWorkerThread_shouldBeNamedDaemon() throws Exception {
        AtomicReference<Thread> worker = new AtomicReference<>();
        JEPThreadPoolClassifier.getInstance().run(() -> worker.set(Thread.currentThread()));

        assertThat(worker.get().isDaemon(), is(true));
        assertThat(worker.get().getName(), is("jep-classifier-worker"));
    }

    @Test
    @Order(2)
    public void testRun_shouldPropagateTaskException() {
        assertThrows(IllegalStateException.class, () -> JEPThreadPoolClassifier.getInstance().run(() -> {
            throw new IllegalStateException("boom");
        }));
    }

    @Test
    @Order(3)
    public void testShutdown_shouldCompleteQuicklyAndBeIdempotent() {
        long start = System.currentTimeMillis();
        JEPThreadPoolClassifier.getInstance().shutdown();
        JEPThreadPoolClassifier.getInstance().shutdown();
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed, is(lessThan(5000L)));
    }
}
