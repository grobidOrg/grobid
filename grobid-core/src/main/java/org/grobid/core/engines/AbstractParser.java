package org.grobid.core.engines;

import org.grobid.core.GrobidModel;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.engines.tagging.*;
import org.grobid.core.utilities.counters.CntManager;
import org.grobid.core.utilities.counters.impl.CntManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public abstract class AbstractParser implements GenericTagger, Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractParser.class);
    private GenericTagger genericTagger;
    protected GrobidAnalyzer analyzer = GrobidAnalyzer.getInstance();

    protected CntManager cntManager = CntManagerFactory.getNoOpCntManager();

    protected AbstractParser(GrobidModel model) {
        this(model, CntManagerFactory.getNoOpCntManager());
    }

    protected AbstractParser(GrobidModel model, CntManager cntManager) {
        this.cntManager = cntManager;
        genericTagger = TaggerFactory.getTagger(model);
    }

    protected AbstractParser(GrobidModel model, CntManager cntManager, GrobidCRFEngine engine) {
        this.cntManager = cntManager;
        genericTagger = TaggerFactory.getTagger(model, engine);
    }

    protected AbstractParser(GrobidModel model, CntManager cntManager, GrobidCRFEngine engine, String architecture) {
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

    @Override
    public void close() throws IOException {
        try {
            genericTagger.close();
        } catch (Exception e) {
            LOGGER.warn("Cannot close the parser: " + e.getMessage());
            // no op
        }
    }
}
