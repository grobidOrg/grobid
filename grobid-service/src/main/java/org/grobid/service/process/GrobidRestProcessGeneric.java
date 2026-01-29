package org.grobid.service.process;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

@Singleton
public class GrobidRestProcessGeneric {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidRestProcessGeneric.class);

    @Inject
    public GrobidRestProcessGeneric() {

    }

    /**
     * Returns a string containing true, if the service is alive.
     *
     * @return a response object containing the string true if service is alive.
     */
    public String isAlive() {
        LOGGER.debug("called isAlive()...");

        String retVal = null;
        try {
            retVal = Boolean.valueOf(true).toString();
        } catch (Exception e) {
            LOGGER.error("GROBID Service is not alive, because of: ", e);
            retVal = Boolean.valueOf(false).toString();
        }
        return retVal;
    }

    /**
     * Returns the description of how to use the grobid-service in a human readable way (html).
     *
     * @return a response object containing a html description
     */
    public Response getDescription_html(UriInfo uriInfo) {
        Response response = null;

        LOGGER.debug("called getDescription_html()...");

        String htmlCode = "<h4>grobid-service documentation</h4>"
                + "This service provides a RESTful interface for using the grobid system. grobid extracts data from pdf files. For more information see: "
                + "<a href=\"http://grobid.readthedocs.org/\">http://grobid.readthedocs.org/</a>";

        response = Response.status(Status.OK).entity(htmlCode).type(MediaType.TEXT_HTML).build();

        return response;
    }
}
