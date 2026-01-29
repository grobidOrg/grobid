package org.grobid.core;

import org.grobid.core.utilities.GrobidConfig;
import org.grobid.core.utilities.GrobidProperties;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

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
        // assertThat(splittedPath[splittedPath.length - 1], is("model.wapiti"));
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
        // assertThat(splittedPath[splittedPath.length - 1], is("model.wapiti"));
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
        // assertThat(tokenizePath[tokenizePath.length - 1], is("model.wapiti"));
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
