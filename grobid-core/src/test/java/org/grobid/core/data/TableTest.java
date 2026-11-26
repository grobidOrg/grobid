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
package org.grobid.core.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class TableTest {

    @Test
    public void testIsCompleteForTEI_withHeaderAndCaption_shouldBeComplete() {
        Table table = new Table();
        table.appendHeader("Table 1");
        table.appendCaption("Some caption");

        assertThat(table.isCompleteForTEI(), is(true));
    }

    @Test
    public void testIsCompleteForTEI_withoutHeaderOrCaption_shouldNotBeComplete() {
        Table table = new Table();

        assertThat(table.isCompleteForTEI(), is(false));
    }

    @Test
    public void testIsCompleteForTEI_withHeaderOnly_shouldNotBeComplete() {
        Table table = new Table();
        table.appendHeader("Table 1");

        assertThat(table.isCompleteForTEI(), is(false));
    }

    @Test
    public void testIsCompleteForTEI_fromTypedArea_shouldBeCompleteWithoutHeaderOrCaption() {
        // A user-provided typed area carries only the table grid; it must survive TEI serialization
        // even though the sequence-labelling model did not produce a head/caption.
        Table table = new Table();
        table.setFromTypedArea(true);

        assertThat(table.isCompleteForTEI(), is(true));
    }
}
