package org.grobid.core.utilities.glutton;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.crossref.CrossrefDeserializer;
import org.grobid.core.utilities.crossref.CrossrefRequestListener;
import org.grobid.core.utilities.crossref.CrossrefRequestListener.Response;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * Glutton request
 *
 */
public class GluttonRequest<T extends Object> extends Observable {

    protected String BASE_PATH = "/service/lookup";
    protected static final List<String> identifiers = Arrays.asList("doi", "DOI", "pmid", "PMID", "pmcid", "PMCID", "pmc", "PMC");

    /**
     * Query parameters, cannot be null, ex: ?atitle=[title]&firstAuthor=[first_author_lastname]
     * Identifier are also delivered as parameter, with the name of the identifier
     */
    public Map<String, String> params;

    /**
     * JSON response deserializer, ex: WorkDeserializer to convert metadata to BiblioItem, it's similar 
     * to CrossRef, but possibly enriched with some additional metadata (e.g. PubMed)
     */
    protected CrossrefDeserializer<T> deserializer;
    
    protected ArrayList<CrossrefRequestListener<T>> listeners;
    
    public GluttonRequest(String model, Map<String, String> params, CrossrefDeserializer<T> deserializer) {
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
            CrossrefRequestListener.Response<T> message = new CrossrefRequestListener.Response<T>();
            message.setException(new Exception("Empty list of parameter, cannot build request to glutton service"), this.toString());
            notifyListeners(message);
            return;
        }
        CloseableHttpClient httpclient = null;
        
        // Get the configured timeout in milliseconds
        int timeout = GrobidProperties.getGluttonConsolidationTimeout() * 1000; // Convert to milliseconds
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .setConnectionRequestTimeout(timeout)
            .build();
            
        if (GrobidProperties.getProxyHost() != null) {
            HttpHost proxy = new HttpHost(GrobidProperties.getProxyHost(), GrobidProperties.getProxyPort());
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            httpclient = HttpClients.custom()
                .setRoutePlanner(routePlanner)
                .setDefaultRequestConfig(requestConfig)
                .build();
        } else {
            httpclient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();   
        }

        try {
            String url = GrobidProperties.getInstance().getGluttonUrl();
            if (url == null) {
                throw new Exception("Invalid url for glutton service");
            }
            
            URIBuilder uriBuilder = new URIBuilder(url + BASE_PATH);

            // check if we have a strong identifier directly supported by Glutton: DOI, PMID, PMCID
            // more probably in the future
            if (params.get("DOI") != null || params.get("doi") != null) {
                String doi = params.get("DOI");
                if (doi == null)
                    doi = params.get("doi");
                uriBuilder.setParameter("doi", doi);
            } 
            if (params.get("HALID") != null || params.get("halId") != null) {
                String doi = params.get("HALID");
                if (doi == null)
                    doi = params.get("halId");
                uriBuilder.setParameter("halId", doi);
            } 
            if (params.get("PMID") != null || params.get("pmid") != null) {
                String pmid = params.get("PMID");
                if (pmid == null)
                    pmid = params.get("pmid");
                uriBuilder.setParameter("pmid", pmid);
            } 
            if (params.get("PMCID") != null || params.get("pmcid") != null || params.get("pmc") != null || params.get("PMC") != null) {
                String pmcid = params.get("PMCID");
                if (pmcid == null)
                    pmcid = params.get("pmcid");
                if (pmcid == null)
                    pmcid = params.get("PMC");
                if (pmcid == null)
                    pmcid = params.get("pmc");
                uriBuilder.setParameter("pmc", pmcid);
            } 
            {
                for (Entry<String, String> cursor : params.entrySet()) {
                    if (!identifiers.contains(cursor.getKey())) 
                        uriBuilder.setParameter(mapFromCrossref(cursor.getKey()), cursor.getValue());
                }
            }

            //System.out.println(uriBuilder.toString());

            HttpGet httpget = new HttpGet(uriBuilder.build());

            ResponseHandler<Void> responseHandler = response -> {

                Response<T> message = new Response<T>();

                message.status = response.getStatusLine().getStatusCode();

                /*Header limitIntervalHeader = response.getFirstHeader("X-Rate-Limit-Interval");
                Header limitLimitHeader = response.getFirstHeader("X-Rate-Limit-Limit");
                if (limitIntervalHeader != null && limitLimitHeader != null)
                    message.setTimeLimit(limitIntervalHeader.getValue(), limitLimitHeader.getValue());
                */
                if (message.status == 503) {
                    throw new GrobidResourceException();
                } else if (message.status < 200 || message.status >= 300) {
                    message.errorMessage = response.getStatusLine().getReasonPhrase();
                } else {

                    HttpEntity entity = response.getEntity();

                    if (entity != null) {
                        String body = EntityUtils.toString(entity);
                        message.results = deserializer.parse(body);
                    }
                }

                notifyListeners(message);

                return null;
            };
            
            httpclient.execute(httpget, responseHandler);
            
        } catch (GrobidResourceException gre) {
            try {
                httpclient.close();
            } catch (IOException e) { 
                // to log
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ie) {
                // to log
            }
            execute();
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

    /**
     * Mapping CrossRef API field arguments to the ones of glutton, to ensure compatibility
     */
    private String mapFromCrossref(String field) {
        if (field.equals("query.bibliographic"))
            return "biblio";
 
        if (field.equals("query.title")) {
            return "atitle";
        }

        if (field.equals("query.author")) {
            return "firstAuthor";
        }

        if (field.equals("query.container-title")) {
            return "jtitle";
        }
        
        return field;
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
