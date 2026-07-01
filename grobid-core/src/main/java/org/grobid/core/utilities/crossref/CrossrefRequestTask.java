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
package org.grobid.core.utilities.crossref;

import java.util.List;

/**
 * Task to execute its request at the right time.
 *
 */
public class CrossrefRequestTask<T extends Object> extends CrossrefRequestListener<T> implements Runnable {

    protected CrossrefClient client;
    protected CrossrefRequest<T> request;

    public CrossrefRequestTask(CrossrefClient client, CrossrefRequest<T> request) {
        this.client = client;
        this.request = request;

        CrossrefClient.printLog(request, "New request in the pool");
    }

    @Override
    public void run() {
        try {
            CrossrefClient.printLog(request, ".. executing");

            request.addListener(this);
            request.execute();

        } catch (Exception e) {
            Response<T> message = new Response<>();
            message.setException(e, request.toString());
            request.notifyListeners(message);
        }
    }

    @Override
    public void onResponse(Response<T> response) {
        if (response.status == 429) {
            client.triggerBackoff();
        } else if (response.status == 401) {
            // token invalid or expired — disable it and downgrade to polite/public
            client.disableToken();
        } else if (!response.hasError()) {
            client.resetBackoff();
            // update rate limit from x-rate-limit-limit / x-rate-limit-interval headers
            client.updateRateLimit(response.limitIterations, response.interval);
            // update concurrency from x-concurrency-limit header
            if (response.concurrencyLimit > 0) {
                client.updateConcurrencyLimit(response.concurrencyLimit);
            }
        }
    }

    @Override
    public void onSuccess(List<T> results) {
    }

    @Override
    public void onError(int status, String message, Exception exception) {
    }

}
