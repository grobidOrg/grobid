package org.grobid.core.utilities

import org.grobid.core.layout.BoundingBox
import org.grobid.core.layout.GraphicObject
import org.grobid.core.layout.GraphicObjectType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoundingBoxCalculatorTest {
    @Test
    fun testMergeBoundingBoxesIfTouching_empty() {
        val result = BoundingBoxCalculator.mergeBoundingBoxesIfTouching(mutableListOf())
        assertTrue(result.isEmpty())
    }

    @Test
    fun testMergeBoundingBoxesIfTouching_noTouching() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 10.0, 10.0)
        val box2 = BoundingBox.fromPointAndDimensions(1, 20.0, 20.0, 10.0, 10.0)
        val go1 = GraphicObject(box1, GraphicObjectType.BITMAP)
        val go2 = GraphicObject(box2, GraphicObjectType.BITMAP)
        val result = BoundingBoxCalculator.mergeBoundingBoxesIfTouching(mutableListOf(go1, go2))
        assertEquals(2, result.size)
    }

    @Test
    fun testMergeBoundingBoxesIfTouching_touching() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 10.0, 10.0)
        val box2 = BoundingBox.fromPointAndDimensions(1, 10.0, 0.0, 10.0, 10.0) // Touches box1 on the right
        val go1 = GraphicObject(box1, GraphicObjectType.BITMAP)
        val go2 = GraphicObject(box2, GraphicObjectType.BITMAP)
        val result = BoundingBoxCalculator.mergeBoundingBoxesIfTouching(mutableListOf(go1, go2))
        assertEquals(1, result.size)
        val merged = result[0]!!.boundingBox
        assertEquals(0.0, merged.x, 0.01)
        assertEquals(0.0, merged.y, 0.01)
        assertEquals(20.0, merged.width, 0.01)
        assertEquals(10.0, merged.height, 0.01)
    }

    @Test
    fun testMergeBoundingBoxesIfTouching_contained() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 20.0, 20.0)
        val box2 = BoundingBox.fromPointAndDimensions(1, 5.0, 5.0, 5.0, 5.0) // Contained in box1
        val go1 = GraphicObject(box1, GraphicObjectType.BITMAP)
        val go2 = GraphicObject(box2, GraphicObjectType.BITMAP)
        val result = BoundingBoxCalculator.mergeBoundingBoxesIfTouching(mutableListOf(go1, go2))
        assertEquals(1, result.size)
        val merged = result[0]!!.boundingBox
        assertEquals(box1, merged)
    }

    @Test
    fun testMergeBoundingBoxesIfTouching_mergeX() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 10.0, 10.0)
        val box2 = BoundingBox.fromPointAndDimensions(1, 10.0, 0.0, 10.0, 10.0) // Touches box1
        val box3 = BoundingBox.fromPointAndDimensions(1, 20.0, 0.0, 10.0, 10.0) // Touches box2
        val go1 = GraphicObject(box1, GraphicObjectType.BITMAP)
        val go2 = GraphicObject(box2, GraphicObjectType.BITMAP)
        val go3 = GraphicObject(box3, GraphicObjectType.BITMAP)
        val result = BoundingBoxCalculator.mergeBoundingBoxesIfTouching(mutableListOf(go1, go2, go3))
        assertEquals(1, result.size)
        val merged1 = result[0]!!.boundingBox
        assertEquals(0.0, merged1.x, 0.01)
        assertEquals(0.0, merged1.y, 0.01)
        assertEquals(30.0, merged1.width, 0.01)
        assertEquals(10.0, merged1.height, 0.01)
    }

    @Test
    fun testMergeBoundingBoxesIfTouching_mergeX2() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 10.0, 10.0)
        val box2 = BoundingBox.fromPointAndDimensions(1, 8.0, 0.0, 10.0, 10.0) // Touches box1
        val box3 = BoundingBox.fromPointAndDimensions(1, 15.0, 0.0, 10.0, 10.0) // Touches box2
        val go1 = GraphicObject(box1, GraphicObjectType.BITMAP)
        val go2 = GraphicObject(box2, GraphicObjectType.BITMAP)
        val go3 = GraphicObject(box3, GraphicObjectType.BITMAP)
        val result = BoundingBoxCalculator.mergeBoundingBoxesIfTouching(mutableListOf(go1, go2, go3))
        assertEquals(1, result.size)
        val merged1 = result[0]!!.boundingBox
        assertEquals(0.0, merged1.x, 0.01)
        assertEquals(0.0, merged1.y, 0.01)
        assertEquals(25.0, merged1.width, 0.01)
        assertEquals(10.0, merged1.height, 0.01)
    }

    @Test
    fun testMergeBoundingBoxesIfTouching_mergeY() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 10.0, 10.0)
        val box2 = BoundingBox.fromPointAndDimensions(1, 0.0, 10.0, 10.0, 10.0) // Touches box1
        val box3 = BoundingBox.fromPointAndDimensions(1, 0.0, 20.0, 10.0, 10.0) // Touches box2
        val go1 = GraphicObject(box1, GraphicObjectType.BITMAP)
        val go2 = GraphicObject(box2, GraphicObjectType.BITMAP)
        val go3 = GraphicObject(box3, GraphicObjectType.BITMAP)
        val result = BoundingBoxCalculator.mergeBoundingBoxesIfTouching(mutableListOf(go1, go2, go3))
        assertEquals(1, result.size)
        val merged1 = result[0]!!.boundingBox
        assertEquals(0.0, merged1.x, 0.01)
        assertEquals(0.0, merged1.y, 0.01)
        assertEquals(10.0, merged1.width, 0.01)
        assertEquals(30.0, merged1.height, 0.01)
    }

    @Test
    fun testMergeBoundingBoxesIfTouching_shouldMergeThroughX() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 10.0, 10.0)
        val box2 = BoundingBox.fromPointAndDimensions(1, 10.0, 0.0, 10.0, 10.0) // Touches box1
        val box3 = BoundingBox.fromPointAndDimensions(1, 0.0, 10.0, 10.0, 10.0) // Touches box2
        val go1 = GraphicObject(box1, GraphicObjectType.BITMAP)
        val go2 = GraphicObject(box2, GraphicObjectType.BITMAP)
        val go3 = GraphicObject(box3, GraphicObjectType.BITMAP)
        val result = BoundingBoxCalculator.mergeBoundingBoxesIfTouching(mutableListOf(go1, go2, go3))
        assertEquals(2, result.size)
        val merged1 = result[0]!!.boundingBox
        assertEquals(0.0, merged1.x, 0.01)
        assertEquals(0.0, merged1.y, 0.01)
        assertEquals(20.0, merged1.width, 0.01)
        assertEquals(10.0, merged1.height, 0.01)

        val merged2 = result[1]!!.boundingBox
        assertEquals(0.0, merged2.x, 0.01)
        assertEquals(10.0, merged2.y, 0.01)
        assertEquals(10.0, merged2.width, 0.01)
        assertEquals(10.0, merged2.height, 0.01)
    }

    @Test
    fun testMergeBoundingBoxesIfTouching_shouldMergeThroughX_2() {
        val box1 = BoundingBox.fromString("1, 0.0, 0.0, 10.0, 10.0")
        val box2 = BoundingBox.fromPointAndDimensions(1, 10.0, 0.0, 10.0, 10.0) // Touches box1
        val box3 = BoundingBox.fromPointAndDimensions(1, 1.0, 11.0, 10.0, 10.0) // Touches box2
        val go1 = GraphicObject(box1, GraphicObjectType.BITMAP)
        val go2 = GraphicObject(box2, GraphicObjectType.BITMAP)
        val go3 = GraphicObject(box3, GraphicObjectType.BITMAP)
        val result = BoundingBoxCalculator.mergeBoundingBoxesIfTouching(mutableListOf(go1, go2, go3))
        assertEquals(2, result.size)
        val merged1 = result[0]!!.boundingBox
        assertEquals(0.0, merged1.x, 0.01)
        assertEquals(0.0, merged1.y, 0.01)
        assertEquals(20.0, merged1.width, 0.01)
        assertEquals(10.0, merged1.height, 0.01)

        val merged2 = result[1]!!.boundingBox
        assertEquals(1.0, merged2.x, 0.01)
        assertEquals(11.0, merged2.y, 0.01)
        assertEquals(10.0, merged2.width, 0.01)
        assertEquals(10.0, merged2.height, 0.01)
    }

    @Test
    fun testMergeBoundingBoxesIfTouching_shouldMergeThroughX_3() {
        val box1 = BoundingBox.fromString("1, 0.0, 0.0, 10.0, 10.0")
        val box2 = BoundingBox.fromString("1, 10.0, 1.1, 10.0, 10.0")
        val box3 = BoundingBox.fromString("1, 1.0, 11.0, 10.0, 10.0")
        val go1 = GraphicObject(box1, GraphicObjectType.BITMAP)
        val go2 = GraphicObject(box2, GraphicObjectType.BITMAP)
        val go3 = GraphicObject(box3, GraphicObjectType.BITMAP)
        val result = BoundingBoxCalculator.mergeBoundingBoxesIfTouching(mutableListOf(go1, go2, go3))
        assertEquals(2, result.size)
        val merged1 = result[0]!!.boundingBox
        assertEquals(0.0, merged1.x, 0.01)
        assertEquals(0.0, merged1.y, 0.01)
        assertEquals(20.0, merged1.width, 0.01)
        assertEquals(11.1, merged1.height, 0.01)

        val merged2 = result[1]!!.boundingBox
        assertEquals(1.0, merged2.x, 0.01)
        assertEquals(11.0, merged2.y, 0.01)
        assertEquals(10.0, merged2.width, 0.01)
        assertEquals(10.0, merged2.height, 0.01)
    }

    @Test
    fun testMergeBoundingBoxesIfTouching_partialOverlapping1_shouldMerge() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 100.0, 100.0)
        val box2 = BoundingBox.fromPointAndDimensions(1, 90.0, 10.1, 100.0, 100.0) // Touches box1
        val go1 = GraphicObject(box1, GraphicObjectType.BITMAP)
        val go2 = GraphicObject(box2, GraphicObjectType.BITMAP)
        val result = BoundingBoxCalculator.mergeBoundingBoxesIfTouching(mutableListOf(go1, go2))
        assertEquals(1, result.size)
    }

    @Test
    fun testMergeBoundingBoxesIfTouching_partialOverlapping2_shouldMerge() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 100.0, 100.0)
        val box2 = BoundingBox.fromPointAndDimensions(1, 90.0, 10.0, 100.0, 100.0) // Touches box1
        val go1 = GraphicObject(box1, GraphicObjectType.BITMAP)
        val go2 = GraphicObject(box2, GraphicObjectType.BITMAP)
        val result = BoundingBoxCalculator.mergeBoundingBoxesIfTouching(mutableListOf(go1, go2))
        assertEquals(1, result.size)

        val merged1 = result[0]!!.boundingBox
        assertEquals(0.0, merged1.x, 0.01)
        assertEquals(0.0, merged1.y, 0.01)
        assertEquals(190.0, merged1.width, 0.01)
        assertEquals(110.0, merged1.height, 0.01)
    }

    @Test
    fun testMergeBoundingBoxesIfTouching_partialOverlapping3_shouldMerge() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 100.0, 100.0)
        val box2 = BoundingBox.fromString("1,10,90,100,100")
        val go1 = GraphicObject(box1, GraphicObjectType.BITMAP)
        val go2 = GraphicObject(box2, GraphicObjectType.BITMAP)
        val result = BoundingBoxCalculator.mergeBoundingBoxesIfTouching(mutableListOf(go1, go2))
        assertEquals(1, result.size)

        val merged1 = result[0]!!.boundingBox
        assertEquals(0.0, merged1.x, 0.01)
        assertEquals(0.0, merged1.y, 0.01)
        assertEquals(110.0, merged1.width, 0.01)
        assertEquals(190.0, merged1.height, 0.01)
    }

    @Test
    fun testMergeBoundingBoxesIfTouching_partialOverlapping4_shouldMerge() {
        val box1 = BoundingBox.fromString("5,298.73,62.07,131.78,98.84")
        val box2 = BoundingBox.fromString("5,430.34,65.93,107.45,85.48")
        val box3 = BoundingBox.fromString("5,298.73,156.31,248.38,99.45")
        val go1 = GraphicObject(box1, GraphicObjectType.BITMAP)
        val go2 = GraphicObject(box2, GraphicObjectType.BITMAP)
        val go3 = GraphicObject(box3, GraphicObjectType.BITMAP)
        val result = BoundingBoxCalculator.mergeBoundingBoxesIfTouching(mutableListOf(go1, go2, go3))
        assertEquals(1, result.size)
    }


    @Test
    fun testMergeBoundingBoxesIfTouching_partialOverlapping5_shouldMerge() {
        val box1 = BoundingBox.fromString("7,301.43,67.06,116.92,99.40")
        val box2 = BoundingBox.fromString("7,420.22,70.37,119.27,96.07")
        val box3 = BoundingBox.fromString("7,169.40,67.06,120.32,90.26")
        val box4 = BoundingBox.fromString("7,55.44,158.30,233.44,90.26")
        val box5 = BoundingBox.fromString("7,51.71,69.82,116.87,87.50")
        val go1 = GraphicObject(box1, GraphicObjectType.BITMAP)
        val go2 = GraphicObject(box2, GraphicObjectType.BITMAP)
        val go3 = GraphicObject(box3, GraphicObjectType.BITMAP)
        val go4 = GraphicObject(box4, GraphicObjectType.BITMAP)
        val go5 = GraphicObject(box5, GraphicObjectType.BITMAP)
        val result = BoundingBoxCalculator.mergeBoundingBoxesIfTouching(mutableListOf(go1, go2, go3, go4, go5))
        assertEquals(2, result.size)
    }

    @Test
    fun testMergeBoundingBoxesIfTouching_partialOverlapping6_shouldMerge() {
        val box1 = BoundingBox.fromString("1,51.71,69.82,116.87,87.50")
        val box2 = BoundingBox.fromString("1,169.40,67.06,120.32,90.26")

        BoundingBoxCalculator.touchingOrIntersecting(box1, box2).also {
            assertTrue(it)
        }

    }
}

