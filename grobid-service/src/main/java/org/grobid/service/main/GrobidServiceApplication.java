package org.grobid.service.main;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;

import com.google.inject.AbstractModule;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.metrics.servlets.MetricsServlet;
import io.prometheus.client.dropwizard.DropwizardExports;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletRegistration;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jetty.ee10.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.GuiceBundle;

import org.grobid.service.GrobidServiceConfiguration;
import org.grobid.service.modules.GrobidServiceModule;
import org.grobid.service.resources.HealthResource;

public final class GrobidServiceApplication extends Application<GrobidServiceConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidServiceApplication.class);
    private static final String[] DEFAULT_CONF_LOCATIONS = {"grobid-home/config/grobid.yaml"};
    private static final String RESOURCES = "/api";

    // ========== Application ==========

    @Override
    public String getName() {
        return "grobid-service";
    }

    @Override
    public void initialize(Bootstrap<GrobidServiceConfiguration> bootstrap) {
        GuiceBundle guiceBundle = GuiceBundle.builder()
                .modules(getGuiceModules())
                .build();
        bootstrap.addBundle(guiceBundle);

        /*bootstrap.addBundle(GuiceBundle.builder()
                .enableAutoConfig(getClass().getPackage().getName())
                .build());*/

        bootstrap.addBundle(new MultiPartBundle());
        bootstrap.addBundle(new AssetsBundle("/web", "/", "index.html", "grobidAssets"));
    }

    private AbstractModule getGuiceModules() {
        return new GrobidServiceModule();
    }

    @Override
    public void run(GrobidServiceConfiguration configuration, Environment environment) {
        environment.healthChecks().register("health-check", new HealthResource(configuration));

        LOGGER.info("Service config={}", configuration);
        new DropwizardExports(environment.metrics()).register();
        ServletRegistration.Dynamic registration = environment.admin().addServlet("Prometheus", new MetricsServlet());
        registration.addMapping("/metrics/prometheus");
        environment.jersey().setUrlPattern(RESOURCES + "/*");

        String allowedOrigins = configuration.getGrobid().getCorsAllowedOrigins();
        String allowedMethods = configuration.getGrobid().getCorsAllowedMethods();
        String allowedHeaders = configuration.getGrobid().getCorsAllowedHeaders();

        // Enable CORS headers
        final FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

        // Configure CORS parameters
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, allowedOrigins);
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, allowedMethods);
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, allowedHeaders);

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, RESOURCES + "/*");

        //Error handling
        //        environment.jersey().register(new GrobidExceptionMapper());
        //        environment.jersey().register(new GrobidServiceExceptionMapper());
        //        environment.jersey().register(new WebApplicationExceptionMapper());
    }

    // ========== static ==========
    public static void main(String... args) throws Exception {
        if (ArrayUtils.getLength(args) < 2) {
            //LOGGER.warn("Expected 2 argument: [0]-server, [1]-<path to config yaml file>");

            String foundConf = null;
            for (String p : DEFAULT_CONF_LOCATIONS) {
                File confLocation = new File(p).getAbsoluteFile();
                if (confLocation.exists()) {
                    foundConf = confLocation.getAbsolutePath();
                    //LOGGER.info("Found conf path: {}", foundConf);
                    break;
                }
            }

            if (foundConf != null) {
                //LOGGER.info("Running with default arguments: \"server\" \"{}\"", foundConf);
                args = new String[]{"server", foundConf};
            } else {
                throw new RuntimeException(
                        "No explicit config provided and cannot find in one of the default locations: "
                                + Arrays.toString(DEFAULT_CONF_LOCATIONS));
            }
        }

        //LOGGER.info("Configuration file: {}", new File(args[1]).getAbsolutePath());
        new GrobidServiceApplication().run(args);
    }
}
