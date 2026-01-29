package org.grobid.service;

import com.google.inject.Singleton;
import io.dropwizard.core.Configuration;

@Singleton
public class GrobidServiceConfiguration extends Configuration {

    private GrobidServicePropConfiguration grobid;

    public GrobidServicePropConfiguration getGrobid() {
        return grobid;
    }

    public void setGrobid(GrobidServicePropConfiguration grobid) {
        this.grobid = grobid;
    }
}
