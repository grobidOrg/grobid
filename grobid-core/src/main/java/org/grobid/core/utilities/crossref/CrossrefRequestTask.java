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
        if (!response.hasError())
            client.updateLimits(response.limitIterations, response.interval);
    }

    @Override
    public void onSuccess(List<T> results) {
    }

    @Override
    public void onError(int status, String message, Exception exception) {
    }

}
