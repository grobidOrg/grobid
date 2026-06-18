package org.grobid.core.engines;

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.GrobidModel;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.engines.config.DebugCaptureContext;
import org.grobid.core.engines.config.DebugLabelingCollector;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.tagging.*;
import org.grobid.core.utilities.counters.CntManager;
import org.grobid.core.utilities.counters.impl.CntManagerFactory;

public abstract class AbstractParser implements GenericTagger, Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractParser.class);
    private GenericTagger genericTagger;
    protected final GrobidModel model;
    protected GrobidAnalyzer analyzer = GrobidAnalyzer.getInstance();

    protected CntManager cntManager = CntManagerFactory.getNoOpCntManager();

    protected AbstractParser(GrobidModel model) {
        this(model, CntManagerFactory.getNoOpCntManager());
    }

    protected AbstractParser(GrobidModel model, CntManager cntManager) {
        this.model = model;
        this.cntManager = cntManager;
        genericTagger = TaggerFactory.getTagger(model);
    }

    protected AbstractParser(GrobidModel model, CntManager cntManager, GrobidCRFEngine engine) {
        this.model = model;
        this.cntManager = cntManager;
        genericTagger = TaggerFactory.getTagger(model, engine);
    }

    protected AbstractParser(GrobidModel model, CntManager cntManager, GrobidCRFEngine engine, String architecture) {
        this.model = model;
        this.cntManager = cntManager;
        genericTagger = TaggerFactory.getTagger(model, engine, architecture);
    }

    @Override
    public String label(Iterable<String> data) {
        return genericTagger.label(data);
    }

    @Override
    public String label(String data) {
        return genericTagger.label(data);
    }

    /**
     * Run sequence labelling and, when {@code config} carries a debug collector,
     * record the raw output under this parser's model name. Returns the unparsed
     * tagger output exactly as {@link #label(String)} would.
     */
    protected String labelAndCapture(String data, GrobidAnalysisConfig config) {
        String result = label(data);
        captureIfEnabled(config, result);
        return result;
    }

    protected String labelAndCapture(Iterable<String> data, GrobidAnalysisConfig config) {
        String result = label(data);
        captureIfEnabled(config, result);
        return result;
    }

    /**
     * Warn (only when a debug capture is active on this thread) that this parser
     * was reached through a no-config overload, so its output will be missing from
     * the debug dump. Call from the {@code -> (…, null)} delegators.
     */
    protected void warnIfDebugUncaptured(String callPath) {
        DebugCaptureContext.warnIfActive(model, callPath);
    }

    private void captureIfEnabled(GrobidAnalysisConfig config, String result) {
        if (config == null) {
            return;
        }
        DebugLabelingCollector collector = config.getDebugLabelingCollector();
        if (collector != null) {
            collector.record(model, result);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            genericTagger.close();
        } catch (Exception e) {
            LOGGER.warn("Cannot close the parser: " + e.getMessage());
            //no op
        }
    }
}
