package org.grobid.core.utilities.crossref;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;
import org.grobid.core.utilities.GrobidProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;

/**
 * GET crossref request
 * @see <a href="https://github.com/CrossRef/rest-api-doc/blob/master/rest_api.md">Crossref API Documentation</a>
 *
 */
public class CrossrefRequest<T extends Object> extends Observable {

	protected static final String BASE_URL = "https://api.crossref.org";

	/**
	 * Model key in crossref, ex: "works", "journals"..
	 * @see <a href="https://github.com/CrossRef/rest-api-doc/blob/master/rest_api.md">Crossref API Documentation</a>
	 */
	public String model;

	/**
	 * Model identifier in crossref, can be null, ex: doi for a work
	 * @see <a href="https://github.com/CrossRef/rest-api-doc/blob/master/rest_api.md">Crossref API Documentation</a>
	 */
	//public String id;

	/**
	 * Query parameters, cannot be null, ex: ?query.bibliographic=[title]&query.author=[author]
	 * @see <a href="https://github.com/CrossRef/rest-api-doc/blob/master/rest_api.md">Crossref API Documentation</a>
	 */
	public Map<String, String> params;

	/**
	 * JSON response deserializer, ex: WorkDeserializer to convert Work to BiblioItem
	 */
	protected CrossrefDeserializer<T> deserializer;

	protected ArrayList<CrossrefRequestListener<T>> listeners;

	public CrossrefRequest(String model, Map<String, String> params, CrossrefDeserializer<T> deserializer) {
		this.model = model;
		//this.id = id;
		this.params = params;
		this.deserializer = deserializer;
		this.listeners = new ArrayList<CrossrefRequestListener<T>>();
	}

	/**
	 * Add listener to catch response when request is executed.
	 */
	public void addListener(CrossrefRequestListener<T> listener) {
		this.listeners.add(listener);
	}

	/**
	 * Notify all connected listeners
	 */
	protected void notifyListeners(CrossrefRequestListener.Response<T> message) {
		for (CrossrefRequestListener<T> listener : listeners)
			listener.notify(message);
	}

	/**
	 * Execute request, handle response by sending to listeners a CrossrefRequestListener.Response
	 */
	public void execute() {
		if (params == null) {
            // this should not happen
            CrossrefRequestListener.Response<T> message = new CrossrefRequestListener.Response<>();
            message.setException(new Exception("Empty list of parameter, cannot build request to the consolidation service"), this.toString());
            notifyListeners(message);
            return;
        }

        CloseableHttpClient httpclient = null;
        int timeout = GrobidProperties.getCrossrefConsolidationTimeout() * 1000; // Convert to milliseconds
        RequestConfig requestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .build();

        if (GrobidProperties.getProxyHost() != null) {
            HttpHost proxy = new HttpHost(GrobidProperties.getProxyHost(), GrobidProperties.getProxyPort());
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);

            httpclient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setRoutePlanner(routePlanner)
                .build();
        } else {
            httpclient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
        }

		try {
			URIBuilder uriBuilder = new URIBuilder(BASE_URL);

			String path = model;

			if (params.get("query.title") != null) {
            	params.put("query.bibliographic", params.get("query.title"));
            	params.remove("query.title");
            }

			if (params.get("DOI") != null || params.get("doi") != null) {
                String doi = params.get("DOI");
                if (doi == null)
                    doi = params.get("doi");
                //uriBuilder.setParameter("doi", doi);
                path += "/"+doi;
                uriBuilder.setPath(path);
            } else {
            	uriBuilder.setPath(path);
				for (Entry<String, String> cursor : params.entrySet())
					if (!cursor.getKey().equals("doi") && !cursor.getKey().equals("DOI") &&
						!cursor.getKey().equals("firstPage") && !cursor.getKey().equals("volume"))
						uriBuilder.setParameter(cursor.getKey(), cursor.getValue());
            }

			// "mailto" parameter to be used in the crossref query and in User-Agent
     		//  header, as recommended by CrossRef REST API documentation, e.g. &mailto=GroovyBib@example.org
            if (GrobidProperties.getCrossrefMailto() != null) {
	            uriBuilder.setParameter("mailto", GrobidProperties.getCrossrefMailto());
	        }

            // set recommended User-Agent header
            HttpGet httpget = new HttpGet(uriBuilder.build());

            if (GrobidProperties.getCrossrefMailto() != null) {
            	httpget.setHeader("User-Agent",
            		"GROBID/" + GrobidProperties.getVersion() + " (https://github.com/kermitt2/grobid; mailto:" + GrobidProperties.getCrossrefMailto() + ")");
			} else {
				httpget.setHeader("User-Agent", "GROBID/" + GrobidProperties.getVersion() + " (https://github.com/kermitt2/grobid)");
			}

			// set the authorization token for the Metadata Plus service if available
			// skip if token has been disabled (validation failure or 401 at runtime)
			if (GrobidProperties.getCrossrefToken() != null && !CrossrefClient.getInstance().isTokenDisabled()) {
            	httpget.setHeader("Crossref-Plus-API-Token", "Bearer " + GrobidProperties.getCrossrefToken());
			}

            ResponseHandler<Void> responseHandler = new ResponseHandler<Void>() {

				@Override
				public Void handleResponse(HttpResponse response) throws ClientProtocolException, IOException {

					CrossrefRequestListener.Response<T> message = new CrossrefRequestListener.Response<T>();

					message.status = response.getStatusLine().getStatusCode();

					// note: header field names are case insensitive
					Header limitIntervalHeader = response.getFirstHeader("X-Rate-Limit-Interval");
					Header limitLimitHeader = response.getFirstHeader("X-Rate-Limit-Limit");
					if (limitIntervalHeader != null && limitLimitHeader != null) {
						message.setTimeLimit(limitIntervalHeader.getValue(), limitLimitHeader.getValue());
					}

					// read concurrency limit and API pool headers
					Header concurrencyLimitHeader = response.getFirstHeader("x-concurrency-limit");
					if (concurrencyLimitHeader != null) {
						try {
							message.concurrencyLimit = Integer.parseInt(concurrencyLimitHeader.getValue().trim());
						} catch (NumberFormatException e) {
							// ignore unparseable header
						}
					}
					Header apiPoolHeader = response.getFirstHeader("x-api-pool");
					if (apiPoolHeader != null) {
						message.apiPool = apiPoolHeader.getValue().trim();
					}

					if (message.status == 429) {
						// rate limit exceeded - set error and notify without trying to parse body
						message.errorMessage = "Rate limit exceeded (HTTP 429)";
						notifyListeners(message);
						return null;
					}

					if (message.status < 200 || message.status >= 300) {
						message.errorMessage = response.getStatusLine().getReasonPhrase();
						notifyListeners(message);
					}

					HttpEntity entity = response.getEntity();

					if (entity != null) {
						String body = EntityUtils.toString(entity);
						if (body != null && body.equals("Resource not found.")) {
							// this used to be a json object too in the past I think
							message.results = null;
						} else
							message.results = deserializer.parse(body);
					}

					notifyListeners(message);

					return null;
				}

            };

            httpclient.execute(httpget, responseHandler);

		} catch (Exception e) {
			CrossrefRequestListener.Response<T> message = new CrossrefRequestListener.Response<T>();
			message.setException(e, this.toString());
			notifyListeners(message);
        } finally {
            try {
				httpclient.close();
			} catch (IOException e) {
				CrossrefRequestListener.Response<T> message = new CrossrefRequestListener.Response<T>();
				message.setException(e, this.toString());
				notifyListeners(message);

			}
        }
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(" (");
		if (params != null) {
			for (Entry<String, String> cursor : params.entrySet()) {
				sb.append(",").append(cursor.getKey()).append("=").append(cursor.getValue());
			}
		}
		sb.append(")");
		return sb.toString();
	}
}
