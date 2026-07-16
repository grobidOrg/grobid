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
package org.grobid.core.document

import org.junit.Test
import kotlin.test.assertEquals

class DocumentNodeTest {
    @Test
    fun testFindNodeDepth() {
        // Build a simple tree:
        // root
        // ├── child1
        // │   └── grandchild1
        // └── child2
        val root = DocumentNode("1| Introduction", "0")
        val child1 = DocumentNode("2| Crystal structure", null)
        val child2 = DocumentNode("child2", null)
        val grandchild1 = DocumentNode("grandchild1", null)
        root.addChild(child1)
        root.addChild(child2)
        child1.addChild(grandchild1)

        // Exact match
        assertEquals(0, DocumentNode.findNodeDepth(root, "1| Introduction", 0))
        assertEquals(1, DocumentNode.findNodeDepth(root, "2| Crystal structure", 0))
        assertEquals(1, DocumentNode.findNodeDepth(root, "child2", 0))
        assertEquals(2, DocumentNode.findNodeDepth(root, "grandchild1", 0))

        // Soft match (case-insensitive, partial, etc.)
        assertEquals(1, DocumentNode.findNodeDepth(root, "2| Crystal structure", 0))
        assertEquals(1, DocumentNode.findNodeDepth(root, "Crystal structure", 0))
        assertEquals(
            -1,
            DocumentNode.findNodeDepth(
                root,
                "2.3 | Crystal structure determination of\n" +
                    "4a, 5a, 5b, 6a, and 6b",
                0,
            ),
        )
        assertEquals(2, DocumentNode.findNodeDepth(root, "grandchild", 0))

        // Not found
        assertEquals(-1, DocumentNode.findNodeDepth(root, "nonexistent", 0))
    }

    @Test
    fun testFindNodeDepth_normalizesOutlineArtifacts() {
        // Outline labels from pdfalto carry non-breaking spaces, soft hyphens and '|' number
        // separators; the section head produced from LayoutTokens has none of these.
        val root = DocumentNode("root", "0")
        val nbspNode = DocumentNode("4.1 Extraction\u00A0and\u00A0isolation", null)
        val softHyphenNode = DocumentNode("2.2.1|Synthesis of tert-\u00ADButoxy acetic acid", null)
        root.addChild(nbspNode)
        root.addChild(softHyphenNode)

        assertEquals(1, DocumentNode.findNodeDepth(root, "4.1 Extraction and isolation", 0))
        assertEquals(1, DocumentNode.findNodeDepth(root, "2.2.1 Synthesis of tert-Butoxy acetic acid", 0))
    }
}
