package org.grobid.service.process;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.grobid.core.engines.tagging.TaggerFactory;
import org.grobid.service.GrobidRestService;
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
     * Returns whether the service is alive and initialized.
     *
     * @return a response object containing "true"/"false" with HTTP 200/503
     */
    public Response isAlive() {
        LOGGER.debug("called isAlive()...");

        if (GrobidRestService.isInitialized() && !TaggerFactory.hasFailures()) {
            return Response.ok("true").type(MediaType.TEXT_PLAIN).build();
        } else {
            return Response.status(503).entity("false").type(MediaType.TEXT_PLAIN).build();
        }
    }

    /**
     * Returns the description of how to use the grobid-service in a human
     * readable way (html).
     *
     * @return a response object containing a html description
     */
    public Response getDescription_html(UriInfo uriInfo) {
        Response response = null;

        LOGGER.debug("called getDescription_html()...");

        String htmlCode = "<h4>grobid-service documentation</h4>" +
                "This service provides a RESTful interface for using the grobid system. grobid extracts data from pdf files. For more information see: " +
                "<a href=\"http://grobid.readthedocs.org/\">http://grobid.readthedocs.org/</a>";

        response = Response.status(Status.OK).entity(htmlCode)
                .type(MediaType.TEXT_HTML).build();

        return response;
    }
}
