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
package org.grobid.trainer.evaluation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.grobid.trainer.evaluation.utilities.FieldSpecification;

public class EndToEndEvaluationTest {

    @Test
    public void testRemoveFieldsFromEvaluation_shouldRemove() throws Exception {

        List<FieldSpecification> fieldSpecification = new ArrayList<>();
        FieldSpecification field1 = new FieldSpecification();
        field1.fieldName = "bao";
        fieldSpecification.add(field1);

        FieldSpecification field2 = new FieldSpecification();
        field2.fieldName = "miao";
        fieldSpecification.add(field2);

        List<String> labelSpecification = new ArrayList<>();
        labelSpecification.add("bao");
        labelSpecification.add("miao");

        EndToEndEvaluation.removeFieldsFromEvaluation(Arrays.asList("bao"), fieldSpecification, labelSpecification);

        assertThat(fieldSpecification, hasSize(1));
        assertThat(labelSpecification, hasSize(1));

        assertThat(fieldSpecification.get(0).fieldName, is("miao"));
        assertThat(labelSpecification.get(0), is("miao"));
    }

    @Test
    public void testRemoveFieldsFromEvaluation() throws Exception {

        List<FieldSpecification> fieldSpecification = new ArrayList<>();
        FieldSpecification field1 = new FieldSpecification();
        field1.fieldName = "bao";
        fieldSpecification.add(field1);

        FieldSpecification field2 = new FieldSpecification();
        field2.fieldName = "miao";
        fieldSpecification.add(field2);

        List<String> labelSpecification = new ArrayList<>();
        labelSpecification.add("bao");
        labelSpecification.add("miao");

        EndToEndEvaluation.removeFieldsFromEvaluation(Arrays.asList("zao"), fieldSpecification, labelSpecification);

        assertThat(fieldSpecification, hasSize(2));
        assertThat(labelSpecification, hasSize(2));

        assertThat(fieldSpecification.get(0).fieldName, is("bao"));
        assertThat(labelSpecification.get(0), is("bao"));

        assertThat(fieldSpecification.get(1).fieldName, is("miao"));
        assertThat(labelSpecification.get(1), is("miao"));
    }

    @Test
    public void testRemoveFieldsFromEvaluationEmpty_ShouldNotFail() throws Exception {
        List<FieldSpecification> fieldSpecification = new ArrayList<>();
        List<String> labelSpecification = new ArrayList<>();

        EndToEndEvaluation.removeFieldsFromEvaluation(Arrays.asList("bao"), fieldSpecification, labelSpecification);

        assertThat(fieldSpecification, hasSize(0));
        assertThat(labelSpecification, hasSize(0));
    }

}
