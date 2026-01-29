package org.grobid.core.data;

import java.util.List;
import java.util.Arrays;

/**
 * Class for representing information related to copyrights owner and file license.
 */
public class CopyrightsLicense {

    // copyrights owner
    public enum CopyrightsOwner {
        PUBLISHER("publisher"), AUTHORS("authors"), UNDECIDED("undecided");

        private String name;

        private CopyrightsOwner(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static List<String> copyrightOwners = Arrays.asList("publisher", "authors", "undecided");

    // File-level licenses
    public enum License {
        CC0("CC-0"), CCBY("CC-BY"), CCBYNC("CC-BY-NC"), CCBYNCND("CC-BY-NC-ND"), CCBYSA("CC-BY-SA"),
        CCBYNCSA("CC-BY-NC-SA"), CCBYND("CC-BY-ND"), COPYRIGHT("strict-copyrights"), OTHER("other"),
        UNDECIDED("undecided");

        private String name;

        private License(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    };

    public static List<String> licenses = Arrays.asList(
            "CC-0",
            "CC-BY",
            "CC-BY-NC",
            "CC-BY-NC-ND",
            "CC-BY-SA",
            "CC-BY-NC-SA",
            "CC-BY-ND",
            "copyright",
            "other",
            "undecided");

    private CopyrightsOwner copyrightsOwner;
    private double copyrightsOwnerProb;
    private License license;
    private double licenseProb;

    public CopyrightsOwner getCopyrightsOwner() {
        return this.copyrightsOwner;
    }

    public void setCopyrightsOwner(CopyrightsOwner owner) {
        this.copyrightsOwner = owner;
    }

    public double getCopyrightsOwnerProb() {
        return this.copyrightsOwnerProb;
    }

    public void setCopyrightsOwnerProb(double prob) {
        this.copyrightsOwnerProb = prob;
    }

    public License getLicense() {
        return this.license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public double getLicenseProb() {
        return this.licenseProb;
    }

    public void setLicenseProb(double prob) {
        this.licenseProb = prob;
    }
}
