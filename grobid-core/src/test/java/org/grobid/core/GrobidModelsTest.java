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
package org.grobid.core;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.utilities.GrobidConfig;
import org.grobid.core.utilities.GrobidProperties;

public class GrobidModelsTest {

    @BeforeClass
    public static void setInitialContext() throws Exception {
        GrobidProperties.getInstance();
    }

    @Test
    public void testGrobidModelsEnum_StandardModel_affiliation() throws Exception {
        GrobidModel model = GrobidModels.AFFILIATION_ADDRESS;

        assertThat(model.getFolderName(), is("affiliation-address"));
        assertThat(model.getModelName(), is("affiliation-address"));
        assertThat(model.getTemplateName(), is("affiliation-address.template"));
        String[] splittedPath = model.getModelPath().split("[/\\\\]");
        //assertThat(splittedPath[splittedPath.length - 1], is("model.wapiti"));
        assertThat(splittedPath[splittedPath.length - 2], is("affiliation-address"));
        assertThat(splittedPath[splittedPath.length - 3], is("models"));
    }

    @Test
    public void testGrobidModelsEnum_StandardModel_name() throws Exception {
        GrobidModel model = GrobidModels.HEADER;

        assertThat(model.getFolderName(), is("header"));
        assertThat(model.getModelName(), is("header"));
        assertThat(model.getTemplateName(), is("header.template"));
        String[] splittedPath = model.getModelPath().split("[/\\\\]");
        //assertThat(splittedPath[splittedPath.length - 1], is("model.wapiti"));
        assertThat(splittedPath[splittedPath.length - 2], is("header"));
        assertThat(splittedPath[splittedPath.length - 4], is("grobid-home"));
    }

    @Test
    public void testGrobidModelsEnum_CustomModel_shouldBeConfiguredBeforeHand() throws Exception {
        GrobidConfig.ModelParameters modelParameters = new GrobidConfig.ModelParameters();
        modelParameters.name = "myDreamModel";
        modelParameters.engine = "wapiti";
        GrobidProperties.addModel(modelParameters);

        GrobidModel model = GrobidModels.modelFor("myDreamModel");

        assertThat(model.getFolderName(), is("myDreamModel"));
        assertThat(model.getModelName(), is("myDreamModel"));
        assertThat(model.getTemplateName(), is("myDreamModel.template"));

        String[] tokenizePath = model.getModelPath().split("[/\\\\]");
        //assertThat(tokenizePath[tokenizePath.length - 1], is("model.wapiti"));
        assertThat(tokenizePath[tokenizePath.length - 2], is("myDreamModel"));
        assertThat(tokenizePath[tokenizePath.length - 3], is("models"));
        assertThat(tokenizePath[tokenizePath.length - 4], is("grobid-home"));

        GrobidModel model2 = GrobidModels.modelFor("AnotherDreamModel");
        assertThat(model2.equals(model), is(false));

        GrobidModel model3 = GrobidModels.modelFor("myDreamModel");
        assertThat(model3.equals(model), is(true));
    }

    @Test
    public void testGrobidFlavor_getFlavorFromName() throws Exception {
        assertThat(GrobidModels.Flavor.fromLabel("ietf"), is(nullValue()));
        assertThat(GrobidModels.Flavor.fromLabel("sdo/ietf"), is(GrobidModels.Flavor.IETF));

        assertThat(GrobidModels.Flavor.fromLabel("3gpp"), is(nullValue()));
        assertThat(GrobidModels.Flavor.fromLabel("sdo/3gpp"), is(GrobidModels.Flavor._3GPP));
    }

    @Test
    public void testGrobidFlavor_missing_shouldFallbackToStandardModel() throws Exception {
        GrobidModel modelFlavor = GrobidModels.getModelFlavor(GrobidModels.DATE, GrobidModels.Flavor.IETF);
        assertThat(modelFlavor.getFolderName(), is("date"));
        assertThat(modelFlavor.getModelPath(), not(containsString("ietf")));
        assertThat(modelFlavor.getModelPath(), endsWith("date/model.wapiti"));
    }
}
