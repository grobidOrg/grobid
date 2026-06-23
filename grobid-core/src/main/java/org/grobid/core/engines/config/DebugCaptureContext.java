package org.grobid.core.engines.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.GrobidModel;

/**
 * Request-scoped marker telling parsers whether a debug-labelling capture is in
 * progress on the current thread. The REST layer calls {@link #activate()} before
 * running the engine for a {@code debugMode} request and {@link #clear()} in a
 * {@code finally} once it returns.
 *
 * <p>It exists so that {@link #warnIfActive(GrobidModel, String)} can flag the one
 * situation that otherwise fails silently: a parser reached through a no-config
 * overload while a {@link DebugLabelingCollector} is active. In that case the
 * model's raw output never gets recorded and its section simply vanishes from the
 * dump. The no-config overloads are the normal, high-frequency path for non-debug
 * requests and tests, so the warning is gated on this flag to stay silent there.
 *
 * <p>Scope is a {@link ThreadLocal} because GROBID serves one request per pooled
 * engine thread, mirroring the per-request lifecycle of {@link DebugLabelingCollector}.
 */
public final class DebugCaptureContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebugCaptureContext.class);

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private DebugCaptureContext() {
    }

    public static void activate() {
        ACTIVE.set(Boolean.TRUE);
    }

    public static void clear() {
        ACTIVE.remove();
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    /**
     * Log a warning when a no-config overload is reached while a debug capture is
     * active on this thread. No-op otherwise, so normal requests and tests stay
     * silent.
     *
     * @param model    the model whose output will be missing from the dump
     * @param callPath  human-readable identifier of the bypassed no-config method
     */
    public static void warnIfActive(GrobidModel model, String callPath) {
        if (isActive()) {
            LOGGER.warn(
                    "debugMode capture is active but {} was reached via the no-config path '{}'; "
                            + "its raw labelling output will be MISSING from the debug dump. Thread the "
                            + "request GrobidAnalysisConfig through this call.",
                    model != null ? model.getModelName() : "?",
                    callPath);
        }
    }
}
