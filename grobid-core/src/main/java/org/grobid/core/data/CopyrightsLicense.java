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
package org.grobid.core.data;

import java.util.Arrays;
import java.util.List;

/**
 * Class for representing information related to copyrights owner and file license.
 */
public class CopyrightsLicense {

    // copyrights owner
    public enum CopyrightsOwner {
        PUBLISHER("publisher"),
        AUTHORS("authors"),
        UNDECIDED("undecided");

        private String name;

        private CopyrightsOwner(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    };

    public static List<String> copyrightOwners = Arrays.asList("publisher", "authors", "undecided");

    // File-level licenses
    public enum License {
        CC0("CC-0"),
        CCBY("CC-BY"),
        CCBYNC("CC-BY-NC"),
        CCBYNCND("CC-BY-NC-ND"),
        CCBYSA("CC-BY-SA"),
        CCBYNCSA("CC-BY-NC-SA"),
        CCBYND("CC-BY-ND"),
        COPYRIGHT("strict-copyrights"),
        OTHER("other"),
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
