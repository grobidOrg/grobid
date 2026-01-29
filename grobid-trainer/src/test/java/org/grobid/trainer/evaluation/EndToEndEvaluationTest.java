package org.grobid.trainer.evaluation;

import org.grobid.trainer.evaluation.utilities.FieldSpecification;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

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
