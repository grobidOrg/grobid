package org.grobid.core.utilities;

import org.junit.Assert;
import org.junit.Test;

public class SHA1Test {

    @Test
    public void testgetSHA1() {
        Assert.assertEquals(
                "Hashed value is not the expected one",
                "9d4e1e23bd5b727046a9e3b4b7db57bd8d6ee684",
                SHA1.getSHA1("pass"));
    }

}
