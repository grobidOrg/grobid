package org.grobid.core.factory;

import java.util.NoSuchElementException;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.grobid.core.engines.Engine;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.GrobidProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrobidPoolingFactory extends AbstractEngineFactory implements
		PoolableObjectFactory<Engine> {

	/**
	 * A pool which contains objects of type Engine for the conversion.
	 */
	private static volatile GenericObjectPool<Engine> grobidEnginePool = null;
	private static volatile Object grobidEnginePoolControl = new Object();
	private static final Logger LOGGER = LoggerFactory
			.getLogger(GrobidPoolingFactory.class);

	private static volatile Boolean preload = false;

	/**
	 * Constructor.
	 */
	protected GrobidPoolingFactory() {
		//fullInit();
		init();
	}

	/**
	 * Creates a pool for {@link Engine} objects. So a number of objects is
	 * always available and ready to start immediatly.
	 * 
	 * @return GenericObjectPool
	 */
	protected static GenericObjectPool<Engine> newPoolInstance() {
		if (grobidEnginePool == null) {
			// initialize grobidEnginePool
			LOGGER.debug("synchronized newPoolInstance");
			synchronized (grobidEnginePoolControl) {
				if (grobidEnginePool == null) {
					grobidEnginePool = new GenericObjectPool<>(GrobidPoolingFactory.newInstance());
					//grobidEnginePool.setFactory(GrobidPoolingFactory.newInstance());
					grobidEnginePool
							.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
					grobidEnginePool.setMaxWait(GrobidProperties.getPoolMaxWait());
					grobidEnginePool.setMaxActive(GrobidProperties.getMaxConcurrency());
					grobidEnginePool.setTestWhileIdle(false);
					grobidEnginePool.setLifo(false);
					grobidEnginePool.setTimeBetweenEvictionRunsMillis(2000);
					grobidEnginePool.setMaxIdle(0);
				}
			}
		}
		return grobidEnginePool;
	}

	/**
	 * Obtains an instance from this pool.<br>
	 * 
	 * By contract, clients must call {@link GrobidPoolingFactory#returnEngine}
	 * when they finish to use the engine.
	 */
	public static synchronized Engine getEngineFromPool(boolean preloadModels) {
		preload = preloadModels;
		if (grobidEnginePool == null) {
			grobidEnginePool = newPoolInstance();
		}
		Engine engine = null;
		try {
			engine = grobidEnginePool.borrowObject();
		} catch (NoSuchElementException nseExp) {
			throw new NoSuchElementException();
		} catch (Exception exp) {
			throw new GrobidException("An error occurred while getting an engine from the engine pool", exp);
		}
		LOGGER.info("Number of Engines in pool active/max: "
				+ grobidEnginePool.getNumActive() + "/"
				+ grobidEnginePool.getMaxActive());
		return engine;
	}

	/**
	 * By contract, engine must have been obtained using
	 * {@link GrobidPoolingFactory#getEngineFromPool}.<br>
	 */
	public static void returnEngine(Engine engine) {
		try {
			//engine.close();
			if (grobidEnginePool == null) 
				LOGGER.error("grobidEnginePool is null !");
			grobidEnginePool.returnObject(engine);
		} catch (Exception exp) {
			throw new GrobidException(
					"An error occurred while returning an engine from the engine pool", exp);
		}
	}

	/**
	 * Creates and returns an instance of GROBIDFactory. The init() method will
	 * be called.
	 * 
	 * @return
	 */
	protected static GrobidPoolingFactory newInstance() {
		return new GrobidPoolingFactory();
	}
	
	@Override
	public void activateObject(Engine arg0) throws Exception {
	}

	@Override
	public void destroyObject(Engine engine) throws Exception {
	}


	@Override
	public Engine makeObject() throws Exception {
		return (createEngine(this.preload));
	}
	
	@Override
	public void passivateObject(Engine arg0) throws Exception {
	}
	
	@Override
	public boolean validateObject(Engine arg0) {
		return false;
	}

	/**
	 * Returns whether the engine pool has been initialized.
	 */
	public static boolean isPoolInitialized() {
		return grobidEnginePool != null;
	}

	/**
	 * Returns the number of currently borrowed (active) engines, or -1 if pool is not initialized.
	 */
	public static int getActiveEngineCount() {
		GenericObjectPool<Engine> pool = grobidEnginePool;
		return pool != null ? pool.getNumActive() : -1;
	}

	/**
	 * Returns the number of idle engines in the pool, or -1 if pool is not initialized.
	 */
	public static int getIdleEngineCount() {
		GenericObjectPool<Engine> pool = grobidEnginePool;
		return pool != null ? pool.getNumIdle() : -1;
	}

	/**
	 * Returns the configured maximum number of active engines, or -1 if pool is not initialized.
	 */
	public static int getMaxActiveEngineCount() {
		GenericObjectPool<Engine> pool = grobidEnginePool;
		return pool != null ? pool.getMaxActive() : -1;
	}

}
