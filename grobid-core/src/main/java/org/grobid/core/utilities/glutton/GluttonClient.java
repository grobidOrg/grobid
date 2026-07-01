/*
 * Copyright 2008-2026 GROBID contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grobid.core.utilities.glutton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.utilities.crossref.CrossrefClient;
import org.grobid.core.utilities.crossref.CrossrefDeserializer;
import org.grobid.core.utilities.crossref.CrossrefRequestListener;

/**
 * Client to the Glutton bibliographical service
 *
 */
public class GluttonClient extends CrossrefClient {
    public static final Logger LOGGER = LoggerFactory.getLogger(GluttonClient.class);

    private static volatile GluttonClient instance;

    //private volatile ExecutorService executorService;

    //private static boolean limitAuto = true;
    //private volatile TimedSemaphore timedSemaphore;

    // this list is used to maintain a list of Futures that were submitted,
    // that we can use to check if the requests are completed
    //private volatile Map<Long, List<Future<?>>> futures = new HashMap<>();

    public static GluttonClient getInstance() {
        if (instance == null) {
            getNewInstance();
        }
        return instance;
    }

    /**
     * Creates a new instance.
     */
    private static synchronized void getNewInstance() {
        LOGGER.debug("Get new instance of GluttonClient");
        instance = new GluttonClient();
    }

    /**
     * Hidden constructor
     */
    private GluttonClient() {
        super();
        /*this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
        this.timedSemaphore = null;
        this.futures = new HashMap<>();*/
        int nThreads = Runtime.getRuntime().availableProcessors();
        //int nThreads = (int) Math.ceil((double)Runtime.getRuntime().availableProcessors() / 2);
        LOGGER.debug("nThreads: " + nThreads);
        this.executorService = Executors.newFixedThreadPool(nThreads * 2);
        //setLimits(20, 1000); // default calls per second
    }

    public static void printLog(GluttonRequest<?> request, String message) {
        LOGGER.debug((request != null ? request + ": " : "") + message);
        //System.out.println((request != null ? request+": " : "")+message);
    }

    /**
     * Push a request in pool to be executed as soon as possible, then wait a response through the listener.
     */
    public <T extends Object> void pushRequest(
            GluttonRequest<T> request,
            CrossrefRequestListener<T> listener,
            long threadId) {
        if (listener != null)
            request.addListener(listener);
        synchronized (this) {
            Future<?> f = executorService.submit(new GluttonRequestTask<>(this, request));
            List<Future<?>> localFutures = this.futures.get(threadId);
            if (localFutures == null)
                localFutures = new ArrayList<>();
            localFutures.add(f);
            this.futures.put(threadId, localFutures);
            //System.out.println("add request to thread " + threadId + " / current total for the thread: " +  localFutures.size());
        }
    }

    /**
     * Push a request in pool to be executed soon as possible, then wait a response through the listener.
     *
     * @param params        query parameters, can be null, ex: ?query.title=[title]&query.author=[author]
     * @param deserializer  json response deserializer, ex: WorkDeserializer to convert Work to BiblioItem
     * @param threadId      the java identifier of the thread providing the request (e.g. via Thread.currentThread().getId())
     * @param listener      catch response from request
     */
    @Override
    public <T> void pushRequest(
            String model,
            Map<String, String> params,
            CrossrefDeserializer<T> deserializer,
            long threadId,
            CrossrefRequestListener<T> listener) {
        GluttonRequest<T> request = new GluttonRequest<>(model, params, deserializer);
        synchronized (this) {
            this.pushRequest(request, listener, threadId);
        }
    }
}
