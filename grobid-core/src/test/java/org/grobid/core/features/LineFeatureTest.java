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
import static org.junit.Assert.assertThat;

import org.junit.Test;

/** The whole-line features of the segmentation model: one value per line, without separator. */
public class LineFeatureTest {

    @Test
    public void lineShadow_keepsTheWordsAndShadowsTheDigits() {
        assertThat(FeatureFactory.lineShadow("3"), is("X"));
        assertThat(FeatureFactory.lineShadow("12"), is("XX"));
        assertThat(FeatureFactory.lineShadow("Page 3 of 12"), is("page_X_of_XX"));
        assertThat(FeatureFactory.lineShadow("CC-BY 4.0 International license"), is("cc-by_X.X_international_license"));
        assertThat(FeatureFactory.lineShadow("2.3 Materials and methods"), is("X.X_materials_and_methods"));
        assertThat(FeatureFactory.lineShadow("MATERIALS AND METHODS"), is("materials_and_methods"));
        assertThat(FeatureFactory.lineShadow("Table 1"), is("table_X"));
        assertThat(FeatureFactory.lineShadow("J. Biol. Chem. 2006; 281:8518-27"), is("j._biol._chem._XXXX;_XXX:XXXX-XX"));
    }

    @Test
    public void lineShape_keepsTheLayoutNotTheWords() {
        assertThat(FeatureFactory.lineShape("3"), is("d"));
        assertThat(FeatureFactory.lineShape("12"), is("d"));
        assertThat(FeatureFactory.lineShape("Page 3 of 12"), is("Xx_d_x_d"));
        assertThat(FeatureFactory.lineShape("1 Introduction"), is("d_Xx"));
        assertThat(FeatureFactory.lineShape("MATERIALS AND METHODS"), is("X_X_X"));
        assertThat(FeatureFactory.lineShape("Figure 3. Distribution of the samples"), is("Xx_d._Xx_x_x_x"));
        assertThat(FeatureFactory.lineShape("1 Microsoft Research, Los Angeles, California, USA"), is("d_Xx_Xx,_Xx_Xx,_Xx,_X"));
        assertThat(FeatureFactory.lineShape("[12] A. Smith and B. Jones, Nature 521, 436 (2015)."), is("[d]_X._Xx_x_X._Xx,_Xx_d,_d_(d)."));
        assertThat(FeatureFactory.lineShape("Received: 12 March 2019 / Accepted: 3 June 2019"), is("Xx:_d_Xx_d_/_Xx:_d_Xx_d"));
    }

    @Test
    public void whitespace_isCollapsedToOneUnderscore_andBlankLinesGetNoline() {
        assertThat(FeatureFactory.lineShadow("  Germany  "), is("germany"));
        assertThat(FeatureFactory.lineShadow("Los Angeles,\t California"), is("los_angeles,_california"));
        assertThat(FeatureFactory.lineShape("Los Angeles,\t California"), is("Xx_Xx,_Xx"));
        assertThat(FeatureFactory.lineShadow("   "), is("NOLINE"));
        assertThat(FeatureFactory.lineShape("   "), is("NOLINE"));
        assertThat(FeatureFactory.lineShadow(null), is("NOLINE"));
        assertThat(FeatureFactory.lineShape(null), is("NOLINE"));
    }
}
