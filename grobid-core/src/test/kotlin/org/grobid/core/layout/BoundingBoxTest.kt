package org.grobid.core.layout

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BoundingBoxTest {

    @Test
    fun testCalculateOverlapRatio_NoOverlap() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 10.0, 10.0)
        val box2 = BoundingBox.fromPointAndDimensions(1, 20.0, 20.0, 10.0, 10.0)
        assertEquals(0.0, box1.calculateOverlapRatio(box2))
    }

    @Test
    fun testCalculateOverlapRatio_PartialOverlap() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 5.0, 5.0, 10.0, 10.0)
        val box2 = BoundingBox.fromPointAndDimensions(1, 10.0, 10.0, 10.0, 10.0)
        // Intersection is 5x5=25, Union is 2*100-25=175, Ratio is 25/175=0.1429
        assertEquals(0.14, box1.calculateOverlapRatio(box2), 0.001)
    }

    @Test
    fun testCalculateOverlapRatio_PartialOverlap2() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 5.0, 5.0, 10.0, 10.0)
        val box2 = BoundingBox.fromPointAndDimensions(1, 5.0, 10.0, 10.0, 10.0)
        // Intersection is 5x10=50, Union is 2*100-50=150, Ratio is 50/155=0.33
        assertEquals(0.33, box1.calculateOverlapRatio(box2))
    }

    @Test
    fun testCalculateOverlapRatio_CompleteOverlap() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 15.0, 15.0, 5.0, 5.0)
        val box2 = BoundingBox.fromPointAndDimensions(1, 15.0, 15.0, 5.0, 5.0)
        assertEquals(1.0, box1.calculateOverlapRatio(box2))
    }

    @Test
    fun testCalculateOverlapRatio_DifferentPages() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 10.0, 10.0)
        val box2 = BoundingBox.fromPointAndDimensions(2, 0.0, 0.0, 10.0, 10.0)
        assertEquals(0.0, box1.calculateOverlapRatio(box2))
    }

    @Test
    fun testCalculateOverlapRatio_OneBoxContainsOther() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 20.0, 20.0)
        val box2 = BoundingBox.fromPointAndDimensions(1, 5.0, 5.0, 10.0, 10.0)
        // Intersection is 10x10=100, Union is 400+100-100=400, Ratio is 100/400=0.25
        assertEquals(0.25, box1.calculateOverlapRatio(box2))
    }

    @Test
    fun testCalculateOverlapRatio_TouchingBoxes() {
        val box1 = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 10.0, 10.0)
        val box2 = BoundingBox.fromPointAndDimensions(1, 10.0, 0.0, 10.0, 10.0)
        assertEquals(0.0, box1.calculateOverlapRatio(box2))
    }

    @Test
    fun testCalculateOverlapRatio_example1() {
        val box1 = BoundingBox.fromPointAndDimensions(5, 55.44, 67.06, 486.01, 111.09)
        val box2 = BoundingBox.fromPointAndDimensions(5, 54.00, 68.0, 490.0, 650.0)
        assertEquals(0.17, box1.calculateOverlapRatio(box2))
    }

    @Test
    fun testCalculateOutsideRatio_CompletelyInside() {
        val referenceArea = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 100.0, 100.0)
        val box = BoundingBox.fromPointAndDimensions(1, 25.0, 25.0, 50.0, 50.0)
        assertEquals(0.0, box.calculateOutsideRatio(referenceArea))
    }

    @Test
    fun testCalculateOutsideRatio_CompletelyOutside() {
        val referenceArea = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 100.0, 100.0)
        val box = BoundingBox.fromPointAndDimensions(1, 150.0, 150.0, 50.0, 50.0)
        assertEquals(1.0, box.calculateOutsideRatio(referenceArea))
    }

    @Test
    fun testCalculateOutsideRatio_PartiallyOutside() {
        val referenceArea = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 100.0, 100.0)
        val box = BoundingBox.fromPointAndDimensions(1, 75.0, 75.0, 50.0, 50.0)
        // Box is 25% inside, 75% outside
        assertEquals(0.75, box.calculateOutsideRatio(referenceArea))
    }

    @Test
    fun testCalculateOutsideRatio_DifferentPages() {
        val referenceArea = BoundingBox.fromPointAndDimensions(1, 0.0, 0.0, 100.0, 100.0)
        val box = BoundingBox.fromPointAndDimensions(2, 25.0, 25.0, 50.0, 50.0)
        assertEquals(1.0, box.calculateOutsideRatio(referenceArea))
    }

    @Test
    fun testCalculateOutsideRatio_Figure1Reference_boxSlightlingOutside() {
        val referenceArea = BoundingBox.fromPointAndDimensions(5,54.00,68.00,490.00,650.00)

        val boxInside = BoundingBox.fromPointAndDimensions(5,55.44,67.06,486.01,111.09)
        assertEquals(0.01, boxInside.calculateOutsideRatio(referenceArea))
    }
}