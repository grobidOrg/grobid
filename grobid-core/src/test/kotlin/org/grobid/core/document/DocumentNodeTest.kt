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
        assertEquals(-1, DocumentNode.findNodeDepth(root, "2.3 | Crystal structure determination of\n" +
            "4a, 5a, 5b, 6a, and 6b", 0))
        assertEquals(2, DocumentNode.findNodeDepth(root, "grandchild", 0))

        // Not found
        assertEquals(-1, DocumentNode.findNodeDepth(root, "nonexistent", 0))
    }
}