package org.grobid.service.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Created by lfoppiano on 30/11/16.
 */
public class GrobidRestUtilsTest {
    @Test
    public void getAnnotationFor_validType_shouldWork() throws Exception {
        assertThat(GrobidRestUtils.getAnnotationFor(1), is(GrobidRestUtils.Annotation.BLOCK));
        assertThat(GrobidRestUtils.getAnnotationFor(2), is(GrobidRestUtils.Annotation.FIGURE));
        assertThat(GrobidRestUtils.getAnnotationFor(0), is(GrobidRestUtils.Annotation.CITATION));
    }

    @Test
    public void getAnnotationFor_invalidType_shouldWork() throws Exception {
        assertNull(GrobidRestUtils.getAnnotationFor(-1));
        assertNull(GrobidRestUtils.getAnnotationFor(4));
        assertNull(GrobidRestUtils.getAnnotationFor(3));
    }

}
