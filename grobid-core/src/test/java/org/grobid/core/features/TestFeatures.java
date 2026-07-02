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
package org.grobid.core.features;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.GrobidProperties;

public class TestFeatures {
    FeatureFactory target;

    @BeforeClass
    public static void setInitialContext() throws Exception {
        GrobidProperties.getInstance();
        Lexicon.getInstance();
    }

    @AfterClass
    public static void destroyInitialContext() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        target = FeatureFactory.getInstance();
    }

    @Test
    public void testDiscretizeLinearScale_10Nbins_100total_30val() {

        int val = 30;
        int total = 100;
        int nbBins = 10;
        //        System.out.println("linear " + nbBins);
        assertThat(target.linearScaling(0, total, nbBins), is(0));
        assertThat(target.linearScaling(10, total, nbBins), is(1));
        assertThat(target.linearScaling(20, total, nbBins), is(2));
        assertThat(target.linearScaling(50, total, nbBins), is(5));
        assertThat(target.linearScaling(70, total, nbBins), is(7));
        assertThat(target.linearScaling(100, total, nbBins), is(10));

        assertEquals("Discretized value is not the expected one", 3, target.linearScaling(val, total, nbBins));

    }

    @Test
    public void testDiscretizeLinearScale_7Nbins_100total_30val() {
        int val = 30;
        int total = 100;
        int nbBins = 7;

        //        System.out.println("linear " + nbBins);
        assertThat(target.linearScaling(10, total, nbBins), is(0));
        assertThat(target.linearScaling(20, total, nbBins), is(1));
        assertThat(target.linearScaling(50, total, nbBins), is(3));
        assertThat(target.linearScaling(70, total, nbBins), is(4));

        assertEquals("Discretized value is not the expected one", 2, target.linearScaling(val, total, nbBins));
    }

    @Test
    public void testDiscretizeLinearScale_10Nbins_1total_03val() {
        double valD = 0.3;
        double totalD = 1.0;
        int nbBins = 10;

        //        System.out.println("linear (double) " + nbBins);
        assertThat(target.linearScaling(0.1, totalD, nbBins), is(1));
        assertThat(target.linearScaling(0.2, totalD, nbBins), is(2));
        assertThat(target.linearScaling(0.5, totalD, nbBins), is(5));
        assertThat(target.linearScaling(0.7, totalD, nbBins), is(7));

        assertEquals("Discretized value is not the expected one", 3, target.linearScaling(valD, totalD, nbBins));
        nbBins = 8;
        assertEquals("Discretized value is not the expected one", 2, target.linearScaling(valD, totalD, nbBins));
    }

    @Test
    public void testDiscretizeLogScale_12Nbins_1total_03val() {
        double valD = 0.3;
        double totalD = 1.0;
        int nbBins = 12;

        //        System.out.println("log (double) " + nbBins);
        assertThat(target.logScaling(0.0, totalD, nbBins), is(0));
        assertThat(target.logScaling(0.1, totalD, nbBins), is(1));
        assertThat(target.logScaling(0.2, totalD, nbBins), is(3));
        assertThat(target.logScaling(0.5, totalD, nbBins), is(7));
        assertThat(target.logScaling(0.7, totalD, nbBins), is(9));
        assertThat(target.logScaling(1.0, totalD, nbBins), is(12));

        assertEquals("Discretized value is not the expected one", 4, target.logScaling(valD, totalD, nbBins));

    }

    @Test
    public void testDiscretizeLogScale_8Nbins_1total_03val() {
        double valD = 0.3;
        double totalD = 1.0;
        int nbBins = 8;

        //        System.out.println("log (double) " + nbBins);
        assertThat(target.logScaling(0.0, totalD, nbBins), is(0));
        assertThat(target.logScaling(0.1, totalD, nbBins), is(1));
        assertThat(target.logScaling(0.2, totalD, nbBins), is(2));
        assertThat(target.logScaling(0.5, totalD, nbBins), is(4));
        assertThat(target.logScaling(0.7, totalD, nbBins), is(6));
        assertThat(target.logScaling(1.0, totalD, nbBins), is(8));

        assertEquals("Discretized value is not the expected one", 3, target.logScaling(valD, totalD, nbBins));
    }

}
