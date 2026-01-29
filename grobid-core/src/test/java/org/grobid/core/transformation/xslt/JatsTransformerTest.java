package org.grobid.core.transformation.xslt;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class JatsTransformerTest {
    JATSTransformer target;

    @Before
    public void setUp() throws Exception {
        target = new JATSTransformer();
    }

    @Test
    @Ignore("Not ready yet")
    public void testTransform_teiHeader() throws Exception {
        String teiInput = IOUtils
                .toString(this.getClass().getResourceAsStream("/xslt/sample1.tei.header.xml"), "UTF-8");
        String output = target.transform(teiInput);
        System.out.println(output);
    }

    @Test
    @Ignore("Not ready yet")
    public void testTransform_teiFulltext() throws Exception {
        String teiInput = IOUtils
                .toString(this.getClass().getResourceAsStream("/xslt/sample2.tei.fulltext.xml"), "UTF-8");
        String output = target.transform(teiInput);
        System.out.println(output);
    }

}
