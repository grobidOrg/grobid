package org.grobid.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GrobidServicePropConfiguration {
    @NotEmpty
    @JsonProperty
    private String grobidHome;

    @NotEmpty
    @JsonProperty
    private String grobidServiceProperties;

    @JsonProperty
    private String grobidProperties;

    @JsonProperty
    private boolean modelPreload = false;

    @JsonProperty
    private String corsAllowedOrigins = "*";
    @JsonProperty
    private String corsAllowedMethods = "OPTIONS,GET,PUT,POST,DELETE,HEAD";
    @JsonProperty
    private String corsAllowedHeaders = "X-Requested-With,Content-Type,Accept,Origin";

    @JsonProperty
    private OtlpConfiguration otlp = new OtlpConfiguration();

    public String getGrobidHome() {
        return grobidHome;
    }

    public void setGrobidHome(String grobidHome) {
        this.grobidHome = grobidHome;
    }

    public String getGrobidServiceProperties() {
        return grobidServiceProperties;
    }

    public void setGrobidServiceProperties(String grobidServiceProperties) {
        this.grobidServiceProperties = grobidServiceProperties;
    }

    public String getGrobidProperties() {
        return grobidProperties;
    }

    public void setGrobidProperties(String grobidProperties) {
        this.grobidProperties = grobidProperties;
    }

    public boolean getModelPreload() {
        return modelPreload;
    }

    public void setModelPreload(boolean modelPreload) {
        this.modelPreload = modelPreload;
    }

    public String getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public String getCorsAllowedMethods() {
        return corsAllowedMethods;
    }

    public void setCorsAllowedMethods(String corsAllowedMethods) {
        this.corsAllowedMethods = corsAllowedMethods;
    }

    public String getCorsAllowedHeaders() {
        return corsAllowedHeaders;
    }

    public void setCorsAllowedHeaders(String corsAllowedHeaders) {
        this.corsAllowedHeaders = corsAllowedHeaders;
    }

    public OtlpConfiguration getOtlp() {
        return otlp;
    }

    public void setOtlp(OtlpConfiguration otlp) {
        this.otlp = otlp;
    }
}
