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
package org.grobid.core.utilities;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.exceptions.GrobidPropertyException;

public class DelftModelDirectoryTest {

    @BeforeClass
    public static void setInitialContext() throws Exception {
        GrobidProperties.getInstance();
    }

    private static GrobidConfig.ModelParameters delftModel(String name, String suffix) {
        GrobidConfig.ModelParameters modelParameters = new GrobidConfig.ModelParameters();
        modelParameters.name = name;
        modelParameters.engine = "delft";
        modelParameters.delft = new GrobidConfig.DelftModelParameters();
        modelParameters.delft.architecture = "BidLSTM_CRF_FEATURES";
        modelParameters.delft.suffix = suffix;
        return modelParameters;
    }

    @Test
    public void testSuffix_shippedModel() {
        assertThat(GrobidProperties.getDelftSuffix("header"), is("glove_840B"));
    }

    @Test
    public void testSuffix_flavor_shouldInheritFromBase() {
        assertThat(GrobidProperties.getDelftSuffix("header-article-light"), is("glove_840B"));
    }

    @Test
    public void testSuffix_notSet_shouldGiveDirectoryWithoutSuffix() {
        GrobidProperties.addModel(delftModel("suffix-test-null", null));
        GrobidProperties.addModel(delftModel("suffix-test-empty", ""));

        assertThat(GrobidProperties.getDelftSuffix("suffix-test-null"), is(nullValue()));
        assertThat(
                GrobidProperties.getDelftModelDirectoryName("suffix-test-null", "BidLSTM_CRF_FEATURES"),
                is("suffix-test-null-BidLSTM_CRF_FEATURES"));
        assertThat(
                GrobidProperties.getDelftModelDirectoryName("suffix-test-empty", "BidLSTM_CRF_FEATURES"),
                is("suffix-test-empty-BidLSTM_CRF_FEATURES"));
    }

    @Test
    public void testSuffix_set_shouldBeAppended() {
        GrobidProperties.addModel(delftModel("suffix-test-set", "no_embeddings"));

        assertThat(
                GrobidProperties.getDelftModelDirectoryName("suffix-test-set", "BidLSTM_CRF_FEATURES"),
                is("suffix-test-set-BidLSTM_CRF_FEATURES-no_embeddings"));
    }

    @Test(expected = GrobidPropertyException.class)
    public void testSuffix_invalid_shouldThrowException() {
        GrobidProperties.addModel(delftModel("suffix-test-invalid", "glove'); import os; ('"));

        GrobidProperties.getDelftSuffix("suffix-test-invalid");
    }

    @Test
    public void testModelDirectory_shippedModel_shouldUseSuffixedDirectory() {
        File modelDir = GrobidProperties.getDelftModelDirectory("header", "BidLSTM_CRF_FEATURES");

        assertThat(modelDir, is(notNullValue()));
        assertThat(modelDir.getName(), is("header-BidLSTM_CRF_FEATURES-glove_840B"));
    }

    @Test
    public void testModelDirectory_missing_shouldBeNull() {
        assertThat(GrobidProperties.getDelftModelDirectory("header", "BidLSTM_ChainCRF_FEATURES"), is(nullValue()));
        assertThat(
                GrobidProperties.getDelftModelDirectory("header-article-light", "BidLSTM_CRF_FEATURES"),
                is(nullValue()));
    }
}
