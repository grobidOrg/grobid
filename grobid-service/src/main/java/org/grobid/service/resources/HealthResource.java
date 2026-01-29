package org.grobid.service.resources;

import com.codahale.metrics.health.HealthCheck;
import org.grobid.service.GrobidServiceConfiguration;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("health")
@Singleton
@Produces("application/json;charset=UTF-8")
public class HealthResource extends HealthCheck {

    @Inject
    private GrobidServiceConfiguration configuration;

    @Inject
    public HealthResource(GrobidServiceConfiguration configuration) {
        this.configuration = configuration;
    }

    @GET
    public Response alive() {
        return Response.ok().build();
    }

    @Override
    protected Result check() throws Exception {
        return configuration.getGrobid().getGrobidHome() != null ? Result.healthy()
                : Result.unhealthy("Grobid home is null in the configuration");
    }
}
