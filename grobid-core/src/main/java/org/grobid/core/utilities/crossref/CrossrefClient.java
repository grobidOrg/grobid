package org.grobid.core.utilities.crossref;

import org.grobid.core.utilities.GrobidProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Request pool to get data from api.crossref.org without exceeding limits
 * supporting multi-thread.
 *
 * Note: the provided interval for the query rate returned by CrossRef appeared to be not reliable,
 * so we have to use the rate limit (X-Rate-Limit-Interval) as a global parallel query limit, without
 * interval consideration.
 * See https://github.com/kermitt2/grobid/pull/725
 *
 */
public class CrossrefClient implements Closeable {
	public static final Logger LOGGER = LoggerFactory.getLogger(CrossrefClient.class);

	protected static volatile CrossrefClient instance;

	protected volatile ExecutorService executorService;

	protected int maxPoolSize = 1;
	protected int configuredPoolSize = 1;
	protected static boolean limitAuto = true;

	// exponential backoff state for rate limiting
	protected volatile long backoffMs = 0;
	protected volatile int backoffCount = 0;
	private static final long MAX_BACKOFF_MS = 60_000;

	// this list is used to maintain a list of Futures that were submitted,
	// that we can use to check if the requests are completed
	protected volatile Map<Long, List<Future<?>>> futures = new HashMap<>();

	public static CrossrefClient getInstance() {
        if (instance == null) {
			getNewInstance();
		}
        return instance;
    }

    /**
     * Creates a new instance.
     */
	private static synchronized void getNewInstance() {
		LOGGER.debug("Get new instance of CrossrefClient");
		instance = new CrossrefClient();
	}

    /**
     * Hidden constructor
     */
    protected CrossrefClient() {
    	// note: by default timeout with newCachedThreadPool is set to 60s, which might be too much for crossref usage,
    	// hanging grobid significantly, so we might want to use rather a custom instance of ThreadPoolExecutor and set
    	// the timeout differently
		this.executorService = new ThreadPoolExecutor(
            0, Integer.MAX_VALUE,
            5L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            r -> {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            }
        );
		this.futures = new HashMap<>();

		// set initial pool size based on API tier
		int initialPoolSize = determineInitialPoolSize();
		this.configuredPoolSize = initialPoolSize;
		setLimits(initialPoolSize, 1000);
	}

	/**
	 * Determine initial pool size based on CrossRef API tier.
	 * - Plus tier (has token): 50 (high initial, will be tuned by response headers)
	 * - Polite tier (has mailto): 3
	 * - Public tier (neither): 1
	 */
	private static int determineInitialPoolSize() {
		try {
			String token = GrobidProperties.getCrossrefToken();
			if (token != null) {
				LOGGER.info("CrossRef API tier: Plus (token set) - initial concurrency: 50");
				return 50;
			}
			String mailto = GrobidProperties.getCrossrefMailto();
			if (mailto != null) {
				LOGGER.info("CrossRef API tier: Polite (mailto set) - initial concurrency: 3");
				return 3;
			}
			LOGGER.info("CrossRef API tier: Public (no mailto, no token) - initial concurrency: 1");
			return 1;
		} catch (Exception e) {
			// GrobidProperties may not be initialized yet
			return 1;
		}
	}

	public static void printLog(CrossrefRequest<?> request, String message) {
		LOGGER.debug((request != null ? request+": " : "")+message);
	}

	public void setLimits(int iterations, int interval) {
		this.setMaxPoolSize(iterations);
		// interval is not usable anymore, we need to wait termination of threads independently from any time interval
	}

	public void updateLimits(int iterations, int interval) {
		if (this.limitAuto) {
			this.setLimits(iterations, interval);
			// note: interval not used anymore
		}
	}

	/**
	 * Update concurrency limit from response header.
	 */
	public void updateConcurrencyLimit(int concurrencyLimit) {
		if (concurrencyLimit > 0 && this.limitAuto && backoffMs == 0) {
			this.configuredPoolSize = concurrencyLimit;
			this.setMaxPoolSize(concurrencyLimit);
			LOGGER.debug("Updated concurrency limit from response header: " + concurrencyLimit);
		}
	}

	/**
	 * Trigger exponential backoff after receiving a 429 response.
	 * Starts at 1s, doubles each subsequent call, caps at 60s.
	 * During backoff, pool size is reduced to 1 to serialize requests.
	 */
	public void triggerBackoff() {
		backoffCount++;
		if (backoffMs == 0) {
			backoffMs = 1000;
		} else {
			backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
		}
		this.setMaxPoolSize(1);
		LOGGER.warn("Rate limited (429). Backing off for " + backoffMs + "ms (attempt " + backoffCount + ")");
	}

	/**
	 * Reset backoff state after a successful response.
	 * Restores pool size to the configured value.
	 */
	public void resetBackoff() {
		if (backoffMs > 0) {
			backoffMs = 0;
			backoffCount = 0;
			this.setMaxPoolSize(configuredPoolSize);
			LOGGER.info("Backoff reset. Restored pool size to " + configuredPoolSize);
		}
	}

	/**
	 * Push a request in pool to be executed as soon as possible, then wait a response through the listener.
	 * API Documentation : https://github.com/CrossRef/rest-api-doc/blob/master/rest_api.md
	 */
	public <T extends Object> void pushRequest(CrossrefRequest<T> request, CrossrefRequestListener<T> listener,
		long threadId) {
		if (listener != null)
			request.addListener(listener);
		synchronized(this) {
			// if in backoff, wait before submitting
			if (backoffMs > 0) {
				try {
					LOGGER.debug("Backoff active, sleeping for " + backoffMs + "ms before submitting request");
					Thread.sleep(backoffMs);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

			// we limit the number of active threads to the crossref api dynamic limit returned in the response header
			while(((ThreadPoolExecutor)executorService).getActiveCount() >= this.getMaxPoolSize()) {
				try {
					TimeUnit.MICROSECONDS.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			Future<?> f = executorService.submit(new CrossrefRequestTask<T>(this, request));
			List<Future<?>> localFutures = this.futures.get(threadId);
			if (localFutures == null)
				localFutures = new ArrayList<>();
			localFutures.add(f);
			this.futures.put(threadId, localFutures);
			LOGGER.debug("Add request to thread " + threadId +
					"; active threads count is now " + ((ThreadPoolExecutor) executorService).getActiveCount()
			);
		}
	}

	/**
	 * Push a request in pool to be executed soon as possible, then wait a response through the listener.
	 * @see <a href="https://github.com/CrossRef/rest-api-doc/blob/master/rest_api.md">Crossref API Documentation</a>
	 *
	 * @param params		query parameters, can be null, ex: ?query.title=[title]&query.author=[author]
	 * @param deserializer	json response deserializer, ex: WorkDeserializer to convert Work to BiblioItem
	 * @param threadId		the java identifier of the thread providing the request (e.g. via Thread.currentThread().getId())
	 * @param listener		catch response from request
	 */
	public <T extends Object> void pushRequest(String model, Map<String, String> params, CrossrefDeserializer<T> deserializer,
			long threadId, CrossrefRequestListener<T> listener) {
		CrossrefRequest<T> request = new CrossrefRequest<>(model, params, deserializer);
		synchronized(this) {
			this.pushRequest(request, listener, threadId);
		}
	}

	/**
	 * Wait for all request from a specific thread to be completed
	 */
	public void finish(long threadId) {
		synchronized(this.futures) {
			try {
				List<Future<?>> threadFutures = this.futures.get(threadId);
				if (threadFutures != null) {
					for(Future<?> future : threadFutures) {
						future.get();
						// get will block until the future is done
					}
					this.futures.remove(threadId);
				}
			} catch (InterruptedException ie) {
			 	// Preserve interrupt status
			 	Thread.currentThread().interrupt();
			} catch (ExecutionException ee) {
				LOGGER.error("CrossRef request execution fails");
			}
		}
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	@Override
	public void close() throws IOException {
		executorService.shutdown();
	}
}
