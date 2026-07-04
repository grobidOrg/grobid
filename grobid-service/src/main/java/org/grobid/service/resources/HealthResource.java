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
package org.grobid.service.resources;

import java.util.Map;

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.grobid.core.engines.tagging.TaggerFactory;
import org.grobid.core.factory.GrobidPoolingFactory;
import org.grobid.service.GrobidRestService;
import org.grobid.service.GrobidServiceConfiguration;

@Path("health")
@Singleton
@Produces("application/json;charset=UTF-8")
public class HealthResource extends HealthCheck {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    private GrobidServiceConfiguration configuration;

    @Inject
    public HealthResource(GrobidServiceConfiguration configuration) {
        this.configuration = configuration;
    }

    @GET
    public Response alive() {
        boolean serviceInitialized = GrobidRestService.isInitialized();
        boolean poolInitialized = GrobidPoolingFactory.isPoolInitialized();
        boolean grobidHomeConfigured = configuration.getGrobid().getGrobidHome() != null;
        boolean hasModelFailures = TaggerFactory.hasFailures();
        boolean ready = serviceInitialized && poolInitialized && grobidHomeConfigured && !hasModelFailures;

        ObjectNode json = mapper.createObjectNode();
        json.put("initialized", serviceInitialized);
        json.put("ready", ready);

        // Pool info
        ObjectNode poolNode = mapper.createObjectNode();
        poolNode.put("initialized", poolInitialized);
        poolNode.put("active", GrobidPoolingFactory.getActiveEngineCount());
        poolNode.put("idle", GrobidPoolingFactory.getIdleEngineCount());
        poolNode.put("maxActive", GrobidPoolingFactory.getMaxActiveEngineCount());
        json.set("pool", poolNode);

        // Model info
        ObjectNode modelsNode = mapper.createObjectNode();
        Map<String, String> loadedModels = TaggerFactory.getLoadedModels();
        ObjectNode loadedNode = mapper.createObjectNode();
        for (Map.Entry<String, String> entry : loadedModels.entrySet()) {
            loadedNode.put(entry.getKey(), entry.getValue());
        }
        modelsNode.set("loaded", loadedNode);

        Map<String, String> failedModels = TaggerFactory.getFailedModels();
        ObjectNode failedNode = mapper.createObjectNode();
        for (Map.Entry<String, String> entry : failedModels.entrySet()) {
            failedNode.put(entry.getKey(), entry.getValue());
        }
        modelsNode.set("failed", failedNode);

        modelsNode.put("totalLoaded", loadedModels.size());
        modelsNode.put("totalFailed", failedModels.size());
        json.set("models", modelsNode);

        json.put("grobidHomeConfigured", grobidHomeConfigured);

        String initError = GrobidRestService.getInitializationError();
        if (initError != null) {
            json.put("initializationError", initError);
        }

        int status = ready ? 200 : 503;
        return Response.status(status).entity(json.toString()).type(MediaType.APPLICATION_JSON).build();
    }

    @Override
    protected Result check() throws Exception {
        boolean serviceInitialized = GrobidRestService.isInitialized();
        boolean poolInitialized = GrobidPoolingFactory.isPoolInitialized();
        boolean grobidHomeConfigured = configuration.getGrobid().getGrobidHome() != null;

        if (!grobidHomeConfigured) {
            return Result.unhealthy("Grobid home is null in the configuration");
        }
        if (!serviceInitialized) {
            String error = GrobidRestService.getInitializationError();
            return Result.unhealthy("Service not initialized" + (error != null ? ": " + error : ""));
        }
        if (!poolInitialized) {
            return Result.unhealthy("Engine pool is not initialized");
        }
        if (TaggerFactory.hasFailures()) {
            Map<String, String> failed = TaggerFactory.getFailedModels();
            return Result.unhealthy("Models failed to load: " + String.join(", ", failed.keySet()));
        }

        return Result.healthy(
                "Pool: %d/%d active engines",
                GrobidPoolingFactory.getActiveEngineCount(),
                GrobidPoolingFactory.getMaxActiveEngineCount());
    }
}
