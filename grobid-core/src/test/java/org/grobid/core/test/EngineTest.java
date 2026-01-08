package org.grobid.core.test;

import org.grobid.core.engines.Engine;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.utilities.TestEngineUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class EngineTest {
    protected static Engine engine;

    @BeforeClass
    public static void setUpClass() throws Exception {
        TestEngineUtils.initGrobidForceWapiti();
        engine = GrobidFactory.getInstance().getEngine();
    }

    @AfterClass
    public static void closeResources() throws Exception {
        engine.close();
    }
}
