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

import java.util.List;

import org.grobid.core.utilities.crossref.CrossrefRequestListener;
import org.grobid.core.utilities.crossref.CrossrefRequestListener.Response;

/**
 * Task to execute its request at the right time.
 *
 */
public class GluttonRequestTask<T extends Object> extends CrossrefRequestListener<T> implements Runnable {

    protected GluttonClient client;
    protected GluttonRequest<T> request;

    public GluttonRequestTask(GluttonClient client, GluttonRequest<T> request) {
        this.client = client;
        this.request = request;

        GluttonClient.printLog(request, "New request in the pool");
    }

    @Override
    public void run() {
        try {
            //client.checkLimits();

            GluttonClient.printLog(request, ".. executing");

            request.addListener(this);
            request.execute();

        } catch (Exception e) {
            Response<T> message = new Response<T>();
            message.setException(e, request.toString());
            request.notifyListeners(message);
        }
    }

    @Override
    public void onResponse(Response<T> response) {
        /*if (!response.hasError())
            client.updateLimits(response.limitIterations, response.interval);*/
    }

    @Override
    public void onSuccess(List<T> results) {
    }

    @Override
    public void onError(int status, String message, Exception exception) {
    }

}
