package org.grobid.core.engines.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Test;

import org.grobid.core.GrobidModel;

public class DebugCaptureContextTest {

    @After
    public void tearDown() {
        // never leak the thread-local across tests sharing this executor thread
        DebugCaptureContext.clear();
    }

    private static GrobidModel stubModel(final String name) {
        return new GrobidModel() {
            @Override
            public String getFolderName() {
                return name;
            }

            @Override
            public String getModelPath() {
                return name;
            }

            @Override
            public String getModelName() {
                return name;
            }

            @Override
            public String getTemplateName() {
                return name + ".template";
            }
        };
    }

    @Test
    public void inactiveByDefault() {
        assertThat(DebugCaptureContext.isActive(), is(false));
    }

    @Test
    public void activateThenClearTogglesActive() {
        DebugCaptureContext.activate();
        assertThat(DebugCaptureContext.isActive(), is(true));

        DebugCaptureContext.clear();
        assertThat(DebugCaptureContext.isActive(), is(false));
    }

    @Test
    public void warnIfActiveIsNoOpWhenInactive() {
        // must not throw, even with a null model
        DebugCaptureContext.warnIfActive(stubModel("date"), "Test.path()");
        DebugCaptureContext.warnIfActive(null, "Test.path()");
        assertThat(DebugCaptureContext.isActive(), is(false));
    }

    @Test
    public void warnIfActiveDoesNotThrowWhenActive() {
        DebugCaptureContext.activate();
        DebugCaptureContext.warnIfActive(stubModel("date"), "Test.path()");
        DebugCaptureContext.warnIfActive(null, "Test.path()");
        // warning is a side effect only; state is unchanged
        assertThat(DebugCaptureContext.isActive(), is(true));
    }

    @Test
    public void activeFlagIsThreadScoped() throws Exception {
        DebugCaptureContext.activate();
        final AtomicBoolean childSawActive = new AtomicBoolean(true);

        Thread child = new Thread(() -> childSawActive.set(DebugCaptureContext.isActive()));
        child.start();
        child.join();

        // the child thread has its own thread-local and must not inherit "active"
        assertThat(childSawActive.get(), is(false));
        assertThat(DebugCaptureContext.isActive(), is(true));
    }
}
