package org.grobid.service.data;

public class ServiceInfo {

    private String version;

    private String revision;

    public ServiceInfo() {
    }

    public ServiceInfo(String version) {
        this.version = version;
    }

    public ServiceInfo(String version, String revision) {
        this(version);
        this.revision = revision;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }
}
