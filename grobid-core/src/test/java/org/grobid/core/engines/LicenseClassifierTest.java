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
package org.grobid.core.engines;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import org.grobid.core.data.CopyrightsLicense;

public class LicenseClassifierTest {

    @Test
    public void testExtractResults_shouldExtractPublishers_undecided() {
        String copyrightOwnerAsJson = "{\n"
                +
                "    \"classifications\": [\n"
                +
                "        {\n"
                +
                "            \"authors\": 0.0022506017703562975,\n"
                +
                "            \"publisher\": 0.9961154460906982,\n"
                +
                "            \"text\": \"© 2015 IOP Publishing Ltd\",\n"
                +
                "            \"undecided\": 0.009332857094705105\n"
                +
                "        }\n"
                +
                "    ],\n"
                +
                "    \"date\": \"2024-02-01T15:22:17.353931\",\n"
                +
                "    \"model\": \"copyright_gru\",\n"
                +
                "    \"software\": \"DeLFT\"\n"
                +
                "}";

        String licencesAsJson = "{\n"
                +
                "    \"classifications\": [\n"
                +
                "        {\n"
                +
                "            \"CC-0\": 5.753641289629741e-06,\n"
                +
                "            \"CC-BY\": 0.002589514711871743,\n"
                +
                "            \"CC-BY-NC\": 0.0008843864197842777,\n"
                +
                "            \"CC-BY-NC-ND\": 0.00015740084927529097,\n"
                +
                "            \"CC-BY-NC-SA\": 0.002522438997402787,\n"
                +
                "            \"CC-BY-ND\": 0.00047874293522909284,\n"
                +
                "            \"CC-BY-SA\": 0.0004411475674714893,\n"
                +
                "            \"copyright\": 0.004834720399230719,\n"
                +
                "            \"other\": 3.192744770785794e-05,\n"
                +
                "            \"text\": \"© 2015 IOP Publishing Ltd\",\n"
                +
                "            \"undecided\": 0.9963551759719849\n"
                +
                "        }\n"
                +
                "    ],\n"
                +
                "    \"date\": \"2024-02-01T15:22:31.070649\",\n"
                +
                "    \"model\": \"license_gru\",\n"
                +
                "    \"software\": \"DeLFT\"\n"
                +
                "}";

        List<CopyrightsLicense> copyrightsLicenses = LicenseClassifier
                .extractResults(copyrightOwnerAsJson, licencesAsJson);

        assertThat(copyrightsLicenses, hasSize(1));
        assertThat(copyrightsLicenses.get(0).getLicense(), is(CopyrightsLicense.License.UNDECIDED));
        assertThat(copyrightsLicenses.get(0).getLicenseProb(), is(0.9963551759719849));
        assertThat(copyrightsLicenses.get(0).getCopyrightsOwner(), is(CopyrightsLicense.CopyrightsOwner.PUBLISHER));
        assertThat(copyrightsLicenses.get(0).getCopyrightsOwnerProb(), is(0.9961154460906982));

    }

    @Test
    public void testExtractResults_shouldReturnAuthors_ccby() {
        String copyrightOwnerAsJson = "{\n"
                +
                "    \"classifications\": [\n"
                +
                "        {\n"
                +
                "            \"authors\": 0.9663094878196716,\n"
                +
                "            \"publisher\": 0.033012233674526215,\n"
                +
                "            \"text\": \"© 2020 The Authors. Published by Elsevier Ltd. This is an open access article under the CC BY license (http://creativecommons.org/licenses/BY/4.0/). T\",\n"
                +
                "            \"undecided\": 0.005560279358178377\n"
                +
                "        }\n"
                +
                "    ],\n"
                +
                "    \"date\": \"2024-02-01T09:45:49.755983\",\n"
                +
                "    \"model\": \"copyright_gru\",\n"
                +
                "    \"software\": \"DeLFT\"\n"
                +
                "}";

        String licencesAsJson = "{\n"
                +
                "    \"classifications\": [\n"
                +
                "        {\n"
                +
                "            \"CC-0\": 2.471400932790857e-07,\n"
                +
                "            \"CC-BY\": 0.9981574416160583,\n"
                +
                "            \"CC-BY-NC\": 0.0009365379810333252,\n"
                +
                "            \"CC-BY-NC-ND\": 0.0003149482945445925,\n"
                +
                "            \"CC-BY-NC-SA\": 1.9512295693857595e-05,\n"
                +
                "            \"CC-BY-ND\": 0.00010157905489904806,\n"
                +
                "            \"CC-BY-SA\": 0.0007704910240136087,\n"
                +
                "            \"copyright\": 0.0026725931093096733,\n"
                +
                "            \"other\": 0.003816531505435705,\n"
                +
                "            \"text\": \"© 2020 The Authors. Published by Elsevier Ltd. This is an open access article under the CC BY license (http://creativecommons.org/licenses/BY/4.0/). T\",\n"
                +
                "            \"undecided\": 0.0006339686224237084\n"
                +
                "        }\n"
                +
                "    ],\n"
                +
                "    \"date\": \"2024-02-01T09:46:03.644485\",\n"
                +
                "    \"model\": \"license_gru\",\n"
                +
                "    \"software\": \"DeLFT\"\n"
                +
                "}";
        List<CopyrightsLicense> copyrightsLicenses = LicenseClassifier
                .extractResults(copyrightOwnerAsJson, licencesAsJson);

        assertThat(copyrightsLicenses, hasSize(1));
        assertThat(copyrightsLicenses.get(0).getLicense(), is(CopyrightsLicense.License.CCBY));
        assertThat(copyrightsLicenses.get(0).getLicenseProb(), is(0.9981574416160583));
        assertThat(copyrightsLicenses.get(0).getCopyrightsOwner(), is(CopyrightsLicense.CopyrightsOwner.AUTHORS));
        assertThat(copyrightsLicenses.get(0).getCopyrightsOwnerProb(), is(0.9663094878196716));

    }

    @Test
    public void testExtractResults_shouldReturnUndecided_copyright() {
        String copyrightOwnerAsJson = "{\n"
                +
                "    \"classifications\": [\n"
                +
                "        {\n"
                +
                "            \"authors\": 0.5,\n"
                +
                "            \"publisher\": 0.5,\n"
                +
                "            \"text\": \"© 2020 The Authors. Published by Elsevier Ltd. This is an open access article under the CC BY license (http://creativecommons.org/licenses/BY/4.0/). T\",\n"
                +
                "            \"undecided\": 0.005560279358178377\n"
                +
                "        }\n"
                +
                "    ],\n"
                +
                "    \"date\": \"2024-02-01T09:45:49.755983\",\n"
                +
                "    \"model\": \"copyright_gru\",\n"
                +
                "    \"software\": \"DeLFT\"\n"
                +
                "}";

        String licencesAsJson = "{\n"
                +
                "    \"classifications\": [\n"
                +
                "        {\n"
                +
                "            \"CC-0\": 2.471400932790857e-07,\n"
                +
                "            \"CC-BY\": 0.5,\n"
                +
                "            \"CC-BY-NC\": 0.5,\n"
                +
                "            \"CC-BY-NC-ND\": 0.0003149482945445925,\n"
                +
                "            \"CC-BY-NC-SA\": 1.9512295693857595e-05,\n"
                +
                "            \"CC-BY-ND\": 0.00010157905489904806,\n"
                +
                "            \"CC-BY-SA\": 0.0007704910240136087,\n"
                +
                "            \"copyright\": 0.0026725931093096733,\n"
                +
                "            \"other\": 0.003816531505435705,\n"
                +
                "            \"text\": \"© 2020 The Authors. Published by Elsevier Ltd. This is an open access article under the CC BY license (http://creativecommons.org/licenses/BY/4.0/). T\",\n"
                +
                "            \"undecided\": 0.0006339686224237084\n"
                +
                "        }\n"
                +
                "    ],\n"
                +
                "    \"date\": \"2024-02-01T09:46:03.644485\",\n"
                +
                "    \"model\": \"license_gru\",\n"
                +
                "    \"software\": \"DeLFT\"\n"
                +
                "}";
        List<CopyrightsLicense> copyrightsLicenses = LicenseClassifier
                .extractResults(copyrightOwnerAsJson, licencesAsJson);

        assertThat(copyrightsLicenses, hasSize(1));
        assertThat(copyrightsLicenses.get(0).getLicense(), is(CopyrightsLicense.License.UNDECIDED));
        assertThat(copyrightsLicenses.get(0).getLicenseProb(), is(0.0006339686224237084));
        assertThat(copyrightsLicenses.get(0).getCopyrightsOwner(), is(CopyrightsLicense.CopyrightsOwner.UNDECIDED));
        assertThat(copyrightsLicenses.get(0).getCopyrightsOwnerProb(), is(0.005560279358178377));
    }

}
