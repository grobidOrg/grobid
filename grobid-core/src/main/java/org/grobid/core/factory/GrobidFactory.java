package org.grobid.core.factory;

import org.grobid.core.engines.Engine;

/**
 * Factory to get engine instances.
 */
public class GrobidFactory extends AbstractEngineFactory {

    /**
     * The instance of GrobidFactory.
     */
    private static GrobidFactory factory = null;

    /**
     * Constructor.
     */
    protected GrobidFactory() {
        init();
    }

    /**
     * Return a new instance of GrobidFactory if it doesn't exist, the existing instance else.
     *
     * @return GrobidFactory
     */
    public static GrobidFactory getInstance() {
        if (factory == null) {
            factory = newInstance();
        }
        return factory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Engine getEngine() {
        return super.getEngine(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Engine getEngine(boolean preload) {
        return super.getEngine(preload);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Engine createEngine() {
        return createEngine(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Engine createEngine(boolean preload) {
        return super.createEngine(preload);
    }

    /**
     * Creates a new instance of GrobidFactory.
     *
     * @return GrobidFactory
     */
    protected static GrobidFactory newInstance() {
        return new GrobidFactory();
    }

    /**
     * Resets this class and all its static fields. For instance sets the current object to null.
     */
    public static void reset() {
        factory = null;
    }

}
