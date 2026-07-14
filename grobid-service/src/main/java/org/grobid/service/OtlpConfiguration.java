package org.grobid.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for push-based metrics export over OTLP (OpenTelemetry Protocol).
 *
 * <p>This is the <em>push</em> counterpart to the pull-based Prometheus scrape endpoint at
 * {@code /metrics/prometheus}: instead of waiting to be scraped, the service periodically
 * pushes metrics to an OTLP receiver (an OpenTelemetry Collector, Grafana Alloy/Agent, or a
 * hosted backend such as Grafana Cloud). Disabled by default — turn it on under
 * {@code grobid.otlp} in {@code grobid.yaml}.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OtlpConfiguration {

    /** Master switch. When false, no OpenTelemetry SDK is created and nothing is pushed. */
    @JsonProperty
    private boolean enabled = false;

    /**
     * OTLP receiver endpoint. Use the port that matches {@link #protocol}:
     * {@code http://host:4318} for {@code http/protobuf}, {@code http://host:4317} for {@code grpc}.
     */
    @JsonProperty
    private String endpoint = "http://localhost:4318";

    /** Wire protocol: {@code http/protobuf} (firewall-friendly) or {@code grpc}. */
    @JsonProperty
    private String protocol = "http/protobuf";

    /** How often a batch of metrics is pushed, in seconds. */
    @JsonProperty
    private int intervalSeconds = 60;

    /**
     * Per-export timeout, in seconds, before the exporter gives up on an unreachable/slow receiver.
     * Bounds how long a single push may block the exporter thread each interval. Defaults to the
     * OTLP exporter's own default of 10s.
     */
    @JsonProperty
    private int timeoutSeconds = 10;

    /** Value of the {@code service.name} resource attribute — how this instance shows up in dashboards. */
    @JsonProperty
    private String serviceName = "grobid-service";

    /**
     * Extra headers sent on every OTLP request — typically authentication for a hosted backend
     * (e.g. {@code Authorization: Basic <base64 instanceID:token>} for Grafana Cloud).
     */
    @JsonProperty
    private Map<String, String> headers = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public void setHeaders(Map<String, String> headers) {
        // Defensive copy: never alias the caller's map, and keep the field non-null.
        this.headers = headers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
    }
}
