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
            // client.checkLimits();

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
        /*
         * if (!response.hasError()) client.updateLimits(response.limitIterations, response.interval);
         */
    }

    @Override
    public void onSuccess(List<T> results) {
    }

    @Override
    public void onError(int status, String message, Exception exception) {
    }

}
