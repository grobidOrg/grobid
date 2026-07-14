package org.grobid.service.main;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.AbstractModule;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.forms.MultiPartBundle;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.servlet.jakarta.exporter.MetricsServlet;
import jakarta.servlet.ServletRegistration;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jetty.server.handler.CrossOriginHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.GuiceBundle;

import org.grobid.service.GrobidServiceConfiguration;
import org.grobid.service.metrics.ApplicationMetrics;
import org.grobid.service.metrics.OtlpMetricsReporter;
import org.grobid.service.modules.GrobidServiceModule;
import org.grobid.service.resources.HealthResource;

public final class GrobidServiceApplication extends Application<GrobidServiceConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidServiceApplication.class);
    private static final String[] DEFAULT_CONF_LOCATIONS = {"grobid-home/config/grobid.yaml"};
    private static final String RESOURCES = "/api";

    // Retained so run() can pull Guice singletons (e.g. the shared ApplicationMetrics) from the injector.
    private GuiceBundle guiceBundle;

    // ========== Application ==========

    @Override
    public String getName() {
        return "grobid-service";
    }

    @Override
    public void initialize(Bootstrap<GrobidServiceConfiguration> bootstrap) {
        guiceBundle = GuiceBundle.builder()
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
        // Bridge the application's Dropwizard metrics into the Prometheus registry and also
        // export JVM/process metrics (heap, GC, threads, CPU). The Prometheus exposition format
        // is served at /metrics/prometheus on the admin connector so that a Prometheus server can
        // scrape it (and Grafana can then dashboard/alert on it). Previously this endpoint was
        // wired to Dropwizard's JSON MetricsServlet, so it served the wrong format with mismatched
        // metric names (issue #920).
        //
        // GrobidDropwizardExports makes the bridged metrics idiomatic for Prometheus so that
        // "promtool check metrics" stays clean: it snake_cases the camelCase Java metric names,
        // drops Dropwizard's JVM gauge sets (redundant with the hotspot DefaultExports below and
        // carrying promtool-rejected _count suffixes), and renames Jersey's "total" timers off the
        // counter-reserved _total suffix.
        new GrobidDropwizardExports(environment.metrics()).register();
        DefaultExports.initialize();
        ServletRegistration.Dynamic registration = environment.admin().addServlet("Prometheus", new MetricsServlet());
        registration.addMapping("/metrics/prometheus");

        // Shared holder for the application/business metrics (documents, latency, errors, in-flight,
        // size). Recorded once into the default Prometheus registry, which serves both delivery
        // paths: the Prometheus servlet above and the OTLP push below.
        ApplicationMetrics applicationMetrics = guiceBundle.getInjector().getInstance(ApplicationMetrics.class);

        // Push-based metrics export over OTLP (disabled by default; see grobid.otlp in grobid.yaml).
        // Bridges the default Prometheus registry to an OTLP exporter, so the pushed series are the
        // same ones the scrape endpoint serves. Managed by the Dropwizard lifecycle for clean shutdown.
        environment.lifecycle().manage(new OtlpMetricsReporter(configuration.getGrobid().getOtlp()));

        environment.jersey().setUrlPattern(RESOURCES + "/*");

        // Instruments every request (count/latency/size/in-flight + upload counters) for both metrics
        // paths and writes a structured access log carrying client provenance.
        environment.jersey().register(new GrobidMetricsFilter(applicationMetrics));

        // Enable CORS via Jetty's CrossOriginHandler (replaces the removed-for-deprecation
        // org.eclipse.jetty.ee10.servlets.CrossOriginFilter). The handler is inserted above the
        // Dropwizard application context, so it applies to all served paths.
        CrossOriginHandler cors = new CrossOriginHandler();
        cors.setAllowedOriginPatterns(toSet(configuration.getGrobid().getCorsAllowedOrigins()));
        cors.setAllowedMethods(toSet(configuration.getGrobid().getCorsAllowedMethods()));
        cors.setAllowedHeaders(toSet(configuration.getGrobid().getCorsAllowedHeaders()));
        // GROBID is a stateless API with no cookies/auth; "*" origins + credentials is insecure and
        // rejected by browsers, so credentials are explicitly disabled.
        cors.setAllowCredentials(false);
        environment.getApplicationContext().insertHandler(cors);

        //Error handling
        //        environment.jersey().register(new GrobidExceptionMapper());
        //        environment.jersey().register(new GrobidServiceExceptionMapper());
        //        environment.jersey().register(new WebApplicationExceptionMapper());
    }

    /**
     * Splits a comma-separated configuration value (e.g. "OPTIONS,GET,POST") into a set of
     * trimmed, non-empty entries, preserving order, as expected by {@link CrossOriginHandler}.
     */
    private static Set<String> toSet(String csv) {
        if (csv == null) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
