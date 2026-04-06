package org.grobid.core.utilities.crossref;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
 * CrossRef enforces two types of limits depending on the API pool:
 * - Rate limit (x-rate-limit-limit / x-rate-limit-interval): max requests per time interval
 * - Concurrency limit (x-concurrency-limit): max simultaneous requests
 *
 * Plus tier: rate=150/sec, no concurrency limit
 * Polite tier: rate=10/sec, concurrency=3
 * Public tier: rate=5/sec, concurrency=1
 *
 * See https://www.crossref.org/documentation/retrieve-metadata/rest-api/access-and-authentication/
 *
 */
public class CrossrefClient implements Closeable {
	public static final Logger LOGGER = LoggerFactory.getLogger(CrossrefClient.class);

	protected static volatile CrossrefClient instance;

	protected volatile ExecutorService executorService;

	// Concurrency limit: max simultaneous requests (from x-concurrency-limit header)
	// Plus tier has no concurrency limit; Polite=3, Public=1
	protected volatile int maxPoolSize = 1;
	protected volatile int configuredPoolSize = 1;
	protected static boolean limitAuto = true;

	// Default rate limits per tier (requests per second)
	// See https://www.crossref.org/documentation/retrieve-metadata/rest-api/access-and-authentication/
	private static final int PLUS_RATE_LIMIT = 150;
	private static final int POLITE_RATE_LIMIT = 10;
	private static final int PUBLIC_RATE_LIMIT = 5;

	// Default concurrency limits per tier
	private static final int PLUS_CONCURRENCY = 50;  // no official limit, practical cap
	private static final int POLITE_CONCURRENCY = 3;
	private static final int PUBLIC_CONCURRENCY = 1;

	// exponential backoff with jitter for rate limiting (HTTP 429)
	// Uses "full jitter" strategy: sleep = random(0, min(cap, base * 2^attempt))
	protected volatile int backoffAttempt = 0;
	private static final long BACKOFF_BASE_MS = 1000;
	private static final long MAX_BACKOFF_MS = 60_000;

	// Minimum delay between submitting consecutive requests (milliseconds).
	// Computed from rate limit: intervalMs / rateLimit.
	// Can be overridden via grobid.yaml minRequestIntervalMs (>0 to override, <=0 for auto).
	private volatile long minRequestIntervalMs = 200;  // Public default: 1000/5
	private volatile long lastRequestTimeMs = 0;

	// when true, the API token is not sent in request headers (disabled after validation failure or 401)
	protected volatile boolean tokenDisabled = false;

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

		// set initial pool size and rate limit based on API tier
		initializeTierLimits();

		// validate Plus tier token at startup
		if (hasToken()) {
			validateApiToken();
		}
	}

	private boolean hasToken() {
		try {
			return GrobidProperties.getCrossrefToken() != null;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Initialize concurrency and rate limits based on CrossRef API tier.
	 * Plus: rate=150/sec, no concurrency limit (practical cap at 50)
	 * Polite: rate=10/sec, concurrency=3
	 * Public: rate=5/sec, concurrency=1
	 *
	 * If the user has set minRequestIntervalMs > 0 in config, that overrides the tier-based rate.
	 */
	private void initializeTierLimits() {
		int concurrency;
		long rateIntervalMs;

		try {
			if (hasToken()) {
				// Plus tier: start with Polite defaults until validation confirms Plus pool.
				// validateApiToken() will upgrade if the token is valid.
				concurrency = POLITE_CONCURRENCY;
				rateIntervalMs = 1000 / POLITE_RATE_LIMIT;  // 100ms
				LOGGER.info("CrossRef API tier: Plus (token set) - starting with Polite defaults " +
					"(concurrency: " + concurrency + ", rate: " + POLITE_RATE_LIMIT + " req/sec) " +
					"until token is validated");
			} else {
				String mailto = null;
				try {
					mailto = GrobidProperties.getCrossrefMailto();
				} catch (Exception e) {
					// ignore
				}
				if (mailto != null) {
					concurrency = POLITE_CONCURRENCY;
					rateIntervalMs = 1000 / POLITE_RATE_LIMIT;  // 100ms
					LOGGER.info("CrossRef API tier: Polite (mailto set) - concurrency: " + concurrency +
						", rate: " + POLITE_RATE_LIMIT + " req/sec");
				} else {
					concurrency = PUBLIC_CONCURRENCY;
					rateIntervalMs = 1000 / PUBLIC_RATE_LIMIT;  // 200ms
					LOGGER.info("CrossRef API tier: Public (no mailto, no token) - concurrency: " + concurrency +
						", rate: " + PUBLIC_RATE_LIMIT + " req/sec");
				}
			}
		} catch (Exception e) {
			concurrency = PUBLIC_CONCURRENCY;
			rateIntervalMs = 1000 / PUBLIC_RATE_LIMIT;
		}

		this.configuredPoolSize = concurrency;
		this.setMaxPoolSize(concurrency);

		// Allow config override for rate limit
		try {
			long configOverride = GrobidProperties.getCrossrefMinRequestInterval();
			if (configOverride > 0) {
				this.minRequestIntervalMs = configOverride;
				LOGGER.info("CrossRef rate limit overridden by config: " + configOverride + "ms between requests");
			} else {
				this.minRequestIntervalMs = rateIntervalMs;
			}
		} catch (Exception e) {
			this.minRequestIntervalMs = rateIntervalMs;
		}
	}

	/**
	 * Returns whether the API token has been disabled (due to validation failure or 401 response).
	 * When true, requests should not include the Crossref-Plus-API-Token header.
	 */
	public boolean isTokenDisabled() {
		return tokenDisabled;
	}

	/**
	 * Disable the API token and downgrade concurrency to polite or public tier.
	 * Called when the token is determined to be invalid (startup validation failure, HTTP 401).
	 * Subsequent requests will not include the Crossref-Plus-API-Token header.
	 */
	public void disableToken() {
		if (!tokenDisabled) {
			tokenDisabled = true;
			String mailto = null;
			try {
				mailto = GrobidProperties.getCrossrefMailto();
			} catch (Exception e) {
				// ignore
			}
			if (mailto != null) {
				this.configuredPoolSize = POLITE_CONCURRENCY;
				this.setMaxPoolSize(POLITE_CONCURRENCY);
				this.minRequestIntervalMs = 1000 / POLITE_RATE_LIMIT;
				LOGGER.warn("CrossRef API token disabled. Falling back to polite tier " +
					"(concurrency: " + POLITE_CONCURRENCY + ", rate: " + POLITE_RATE_LIMIT + " req/sec)");
			} else {
				this.configuredPoolSize = PUBLIC_CONCURRENCY;
				this.setMaxPoolSize(PUBLIC_CONCURRENCY);
				this.minRequestIntervalMs = 1000 / PUBLIC_RATE_LIMIT;
				LOGGER.warn("CrossRef API token disabled. Falling back to public tier " +
					"(concurrency: " + PUBLIC_CONCURRENCY + ", rate: " + PUBLIC_RATE_LIMIT + " req/sec)");
			}
		}
	}

	/**
	 * Validate the CrossRef API token by making a lightweight request at startup.
	 * On success: upgrade to Plus tier limits (rate=150/sec, no concurrency cap).
	 * On failure: stay at Polite defaults until response headers confirm actual tier.
	 */
	private void validateApiToken() {
		int validationTimeout = 5000; // 5 seconds
		RequestConfig requestConfig = RequestConfig.custom()
			.setCookieSpec(CookieSpecs.STANDARD)
			.setConnectTimeout(validationTimeout)
			.setSocketTimeout(validationTimeout)
			.setConnectionRequestTimeout(validationTimeout)
			.build();

		try (CloseableHttpClient httpclient = HttpClients.custom()
				.setDefaultRequestConfig(requestConfig)
				.build()) {

			HttpGet httpget = new HttpGet("https://api.crossref.org/works?rows=0");

			String token = GrobidProperties.getCrossrefToken();
			if (token != null) {
				httpget.setHeader("Crossref-Plus-API-Token", "Bearer " + token);
			}
			String mailto = GrobidProperties.getCrossrefMailto();
			if (mailto != null) {
				httpget.setHeader("User-Agent",
					"GROBID/" + GrobidProperties.getVersion() + " (https://github.com/kermitt2/grobid; mailto:" + mailto + ")");
			} else {
				httpget.setHeader("User-Agent", "GROBID/" + GrobidProperties.getVersion() + " (https://github.com/kermitt2/grobid)");
			}

			HttpResponse response = httpclient.execute(httpget);
			int status = response.getStatusLine().getStatusCode();

			if (status >= 200 && status < 300) {
				Header apiPoolHeader = response.getFirstHeader("x-api-pool");
				String apiPool = (apiPoolHeader != null) ? apiPoolHeader.getValue().trim() : null;

				if ("plus".equalsIgnoreCase(apiPool)) {
					applyPlusLimitsFromHeaders(response);
				} else {
					LOGGER.warn("CrossRef API token not recognized as Plus tier (pool: " + apiPool + ").");
					disableToken();
				}
			} else {
				LOGGER.warn("CrossRef API token validation failed (HTTP " + status + ").");
				disableToken();
			}
		} catch (Exception e) {
			LOGGER.warn("Could not validate CrossRef API token (service unreachable: " +
				e.getMessage() + "). Using Polite defaults until response headers confirm tier.");
		}
	}

	/**
	 * Apply Plus tier limits from validation response headers.
	 * Uses x-rate-limit-limit/interval for rate, x-concurrency-limit for concurrency (if present).
	 */
	private void applyPlusLimitsFromHeaders(HttpResponse response) {
		// Rate limit from headers
		Header rateLimitHeader = response.getFirstHeader("x-rate-limit-limit");
		Header rateIntervalHeader = response.getFirstHeader("x-rate-limit-interval");
		int rateLimit = PLUS_RATE_LIMIT;
		int rateIntervalMs = 1000;

		if (rateLimitHeader != null && rateIntervalHeader != null) {
			try {
				rateLimit = Integer.parseInt(rateLimitHeader.getValue().trim());
				rateIntervalMs = (int) org.joda.time.Duration.parse(
					"PT" + rateIntervalHeader.getValue().trim().toUpperCase()).getMillis();
			} catch (Exception e) {
				LOGGER.debug("Could not parse rate limit headers, using Plus defaults");
			}
		}

		// Only apply auto-computed rate if config doesn't override
		long configOverride = -1;
		try {
			configOverride = GrobidProperties.getCrossrefMinRequestInterval();
		} catch (Exception e) {
			// ignore
		}
		if (configOverride <= 0) {
			this.minRequestIntervalMs = rateIntervalMs / rateLimit;
		}

		// Concurrency from header (Plus has no official limit, use header or practical cap)
		Header concurrencyHeader = response.getFirstHeader("x-concurrency-limit");
		int concurrency = PLUS_CONCURRENCY;
		if (concurrencyHeader != null) {
			try {
				concurrency = Integer.parseInt(concurrencyHeader.getValue().trim());
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		this.configuredPoolSize = concurrency;
		this.setMaxPoolSize(concurrency);

		LOGGER.info("CrossRef API token validated. Pool: plus, rate: " + rateLimit +
			" req/" + rateIntervalMs + "ms (interval: " + this.minRequestIntervalMs +
			"ms), concurrency: " + concurrency);
	}

	public static void printLog(CrossrefRequest<?> request, String message) {
		LOGGER.debug((request != null ? request+": " : "")+message);
	}

	/**
	 * Update rate limit from x-rate-limit-limit and x-rate-limit-interval response headers.
	 * Computes minRequestIntervalMs = intervalMs / rateLimit.
	 * Ignored during active backoff.
	 *
	 * @param rateLimit the x-rate-limit-limit value (requests per interval)
	 * @param intervalMs the x-rate-limit-interval value in milliseconds
	 */
	public void updateRateLimit(int rateLimit, int intervalMs) {
		if (rateLimit > 0 && intervalMs > 0 && this.limitAuto && backoffAttempt == 0) {
			// Only update if config doesn't override
			long configOverride = -1;
			try {
				configOverride = GrobidProperties.getCrossrefMinRequestInterval();
			} catch (Exception e) {
				// ignore
			}
			if (configOverride <= 0) {
				long newInterval = intervalMs / rateLimit;
				if (newInterval != this.minRequestIntervalMs) {
					this.minRequestIntervalMs = newInterval;
					LOGGER.info("Updated rate limit from response headers: " + rateLimit +
						" req/" + intervalMs + "ms (interval: " + newInterval + "ms)");
				}
			}
		}
	}

	/**
	 * Update concurrency limit from x-concurrency-limit response header.
	 * Ignored during active backoff.
	 */
	public void updateConcurrencyLimit(int concurrencyLimit) {
		if (concurrencyLimit > 0 && this.limitAuto && backoffAttempt == 0) {
			this.configuredPoolSize = concurrencyLimit;
			this.setMaxPoolSize(concurrencyLimit);
			LOGGER.debug("Updated concurrency limit from response header: " + concurrencyLimit);
		}
	}

	/**
	 * Trigger exponential backoff after receiving a 429 response.
	 * During backoff, pool size is reduced to 1 to serialize requests.
	 */
	public void triggerBackoff() {
		backoffAttempt++;
		this.setMaxPoolSize(1);
		LOGGER.warn("Rate limited (429). Backoff attempt " + backoffAttempt +
			", next sleep up to " + computeBackoffCap() + "ms");
	}

	/**
	 * Compute the jittered backoff sleep duration using "full jitter" strategy.
	 * Returns a random value in [0, min(MAX_BACKOFF_MS, BACKOFF_BASE_MS * 2^attempt)].
	 * Each thread gets a different value, spreading retries across time and avoiding
	 * the thundering herd problem.
	 */
	long computeBackoffWithJitter() {
		long cap = computeBackoffCap();
		return ThreadLocalRandom.current().nextLong(cap + 1);
	}

	/**
	 * Compute the exponential backoff cap (before jitter).
	 */
	private long computeBackoffCap() {
		return Math.min(MAX_BACKOFF_MS, BACKOFF_BASE_MS * (1L << Math.min(backoffAttempt, 30)));
	}

	/**
	 * Reset backoff state after a successful response.
	 * Restores pool size to the configured value.
	 */
	public void resetBackoff() {
		if (backoffAttempt > 0) {
			backoffAttempt = 0;
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

		// Sleep OUTSIDE synchronized block so multiple threads can jitter-sleep in parallel
		if (backoffAttempt > 0) {
			long sleepMs = computeBackoffWithJitter();
			try {
				LOGGER.debug("Backoff active (attempt " + backoffAttempt + "), sleeping for " + sleepMs + "ms");
				Thread.sleep(sleepMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		synchronized(this) {
			// we limit the number of active threads to the crossref api dynamic limit returned in the response header
			while(((ThreadPoolExecutor)executorService).getActiveCount() >= this.getMaxPoolSize()) {
				try {
					TimeUnit.MICROSECONDS.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			// Enforce minimum inter-request delay to cap throughput
			long now = System.currentTimeMillis();
			long elapsed = now - lastRequestTimeMs;
			if (elapsed < minRequestIntervalMs) {
				try {
					Thread.sleep(minRequestIntervalMs - elapsed);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			lastRequestTimeMs = System.currentTimeMillis();

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
		this.pushRequest(request, listener, threadId);
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
