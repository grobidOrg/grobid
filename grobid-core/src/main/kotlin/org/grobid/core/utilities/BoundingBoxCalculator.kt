package org.grobid.core.utilities

import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import org.grobid.core.layout.*
import java.util.*
import kotlin.math.abs

/**
 * Utilities to calculate bounding boxes from coordinates
 */
object BoundingBoxCalculator {
    private const val EPS_X = 15.0
    private const val EPS_Y = 4.0

    @JvmOverloads
    @JvmStatic
    fun calculateOneBox(tokens: Iterable<LayoutToken>?, ignoreDifferentPageTokens: Boolean = false): BoundingBox? {
        if (tokens == null) {
            return null
        }

        var b: BoundingBox? = null
        for (t in tokens) {
            if (LayoutTokensUtil.noCoords(t)) {
                continue
            }
            if (b == null) {
                b = BoundingBox.fromLayoutToken(t)
            } else {
                if (ignoreDifferentPageTokens) {
                    b = b.boundBoxExcludingAnotherPage(BoundingBox.fromLayoutToken(t))
                } else {
                    b = b.boundBox(BoundingBox.fromLayoutToken(t))
                }
            }
        }
        return b
    }

    @JvmStatic
    fun calculate(tokens: MutableList<LayoutToken?>?): MutableList<BoundingBox?> {
        var tokens = tokens
        val result: MutableList<BoundingBox?> = Lists.newArrayList<BoundingBox?>()
        if (tokens != null) {
            tokens = Lists.newArrayList(
                Iterables.filter(tokens) { layoutToken ->
                    layoutToken != null &&
                        !(abs(layoutToken.getWidth()) <= Double.MIN_VALUE || abs(layoutToken.getHeight()) <= Double.MIN_VALUE)
                }
            )

            if (tokens == null || tokens.isEmpty()) {
                return result
            }

            val firstBox = BoundingBox.fromLayoutToken(tokens.get(0))
            result.add(firstBox)
            var lastBox = firstBox
            for (i in 1 until tokens.size) {
                val b = BoundingBox.fromLayoutToken(tokens.get(i))
                if (abs(b.getWidth()) <= Double.Companion.MIN_VALUE || abs(b.getHeight()) <= Double.Companion.MIN_VALUE) {
                    continue
                }

                if (near(lastBox, b)) {
                    result.set(result.size - 1, result.get(result.size - 1)!!.boundBox(b))
                } else {
                    result.add(b)
                }
                lastBox = b
            }
        }

        return result
    }

    //same page, Y is more or less the same, b2 follows b1 on X, and b2 close to the end of b1
    @JvmStatic
    fun near(b1: BoundingBox, b2: BoundingBox): Boolean {
        return b1.getPage() == b2.getPage() && abs(b1.getY() - b2.getY()) < EPS_Y && abs(b1.getY2() - b2.getY2()) < EPS_Y && b2.getX() - b1.getX2() < EPS_X && b2.getX() >= b1.getX()
    }

    @JvmStatic
    fun mergeBoundingBoxesIfTouching(graphicObjects: MutableList<GraphicObject?>?): ArrayList<GraphicObject?> {
        if (graphicObjects == null || graphicObjects.isEmpty()) {
            return ArrayList<GraphicObject?>()
        }
        // 0. Remove all bounding boxes that are contained in others
        val filtered: MutableList<GraphicObject?> = ArrayList<GraphicObject?>(graphicObjects)
        for (i in filtered.indices) {
            val a = filtered.get(i)
            if (a == null) continue
            val boxA = a.getBoundingBox()
            for (j in filtered.indices) {
                if (i == j) continue
                val b = filtered.get(j)
                if (b == null) continue
                val boxB = b.getBoundingBox()
                if (boxB.contains(boxA)) {
                    filtered.set(i, null)
                    break
                }
            }
        }
        filtered.removeIf { obj: GraphicObject? -> Objects.isNull(obj) }

        // 1. Collect all bounding boxes that are touching (intersect or share edge)
        val groups: MutableList<MutableList<GraphicObject>> = ArrayList<MutableList<GraphicObject>>()
        val used = BooleanArray(filtered.size)
        for (i in filtered.indices) {
            if (used[i]) continue
            val group: MutableList<GraphicObject> = ArrayList<GraphicObject>()
            group.add(filtered.get(i)!!)
            used[i] = true
            var changed: Boolean
            do {
                changed = false
                for (j in filtered.indices) {
                    if (used[j]) continue
                    for (g in group) {
                        if (touchingOrIntersecting(g.getBoundingBox(), filtered.get(j)!!.getBoundingBox())) {
                            group.add(filtered.get(j)!!)
                            used[j] = true
                            changed = true
                            break
                        }
                    }
                }
            } while (changed)
            groups.add(group)
        }

        // 2. For each group, sort by Y, then X
        for (group in groups) {
            if (group.size > 1) {
                group.sortWith(
                    Comparator.comparingDouble<GraphicObject> { it.boundingBox.y }
                        .thenComparingDouble { it.boundingBox.x }
                )
            }
        }

        // 3. For each group, merge touching bounding boxes
        val result: MutableList<GraphicObject?> = ArrayList<GraphicObject?>()
        for (group in groups) {
            if (group.size == 1) {
                result.add(group.get(0))
                continue
            }

            // Merge touching bounding boxes in the group using a horizontal-leading sweep
            val newGroup: MutableList<GraphicObject> = ArrayList<GraphicObject>()
            var current = group[0]
            for (i in 1 until group.size) {
                val next = group[i]
                val doMerge =
                    touchingOrIntersectingWithPartial(current.boundingBox, next.boundingBox, 0.85)

                if (doMerge) {
                    val mergedBox = current.boundingBox.boundBox(next.boundingBox)
                    current = GraphicObject(mergedBox, GraphicObjectType.VECTOR_BOX)
                } else {
                    newGroup.add(current)
                    current = next
                }
            }
            newGroup.add(current)

            // Merge touching boxes in the group using a vertical-leading sweep
            current = newGroup[0]
            for (i in 1 until newGroup.size) {
                val next = newGroup[i]
                val doMerge =
                    touchingOrIntersectingWithPartial(current.boundingBox, next.boundingBox, 0.85)

                if (doMerge) {
                    val mergedBox = current.boundingBox.boundBox(next.boundingBox)
                    current = GraphicObject(mergedBox, GraphicObjectType.VECTOR_BOX)
                } else {
                    result.add(current)
                    current = next
                }
            }
            //Add the last merged box
            result.add(current)
        }

        return Lists.newArrayList(result)
    }

    @JvmStatic
    fun isValidGraphicObject(go: GraphicObject, page: Page): Boolean {
        if (go.getWidth() * go.getHeight() < 1000) {
            return false
        }

        if (go.getWidth() < 50) {
            return false
        }

        if (go.getHeight() < 50) {
            return false
        }

        val mainArea = page.getMainArea()

        if (go.getBoundingBox().calculateOutsideRatio(mainArea) > VectorGraphicBoxCalculator.MAX_OUTSIDE_RATIO
            && go.getWidth() * go.getHeight() < VectorGraphicBoxCalculator.MINIMUM_BITMAP_AREA
        ) {
            return false
        }

        return true
    }

    @JvmStatic
    fun touchingOrIntersecting(a: BoundingBox, b: BoundingBox): Boolean {
        if (a.intersect(b)) return true
        // Check if they share an edge (touching)
        val xTouch = (abs(a.getX2() - b.getX()) < 1 || abs(b.getX2() - a.getX()) < 1)
        val yOverlap = (a.getY() < b.getY2() && a.getY2() > b.getY())
        val yTouch = (abs(a.getY2() - b.getY()) < 1 || abs(b.getY2() - a.getY()) < 1)
        val xOverlap = (a.getX() < b.getX2() && a.getX2() > b.getX())
        return (xTouch && yOverlap) || (yTouch && xOverlap)
    }

    @JvmStatic
    fun touchingOrIntersectingWithPartial(a: BoundingBox, b: BoundingBox, alignmentPercentage: Double): Boolean {
        // Vertical edge touch (left/right), check Y overlap and percentage
        val xTouch = abs(a.getX2() - b.getX()) < 1 || abs(b.getX2() - a.getX()) < 1 ||
            (a.getX() < b.getX2() && a.getX2() > b.getX())
        if (xTouch) {
            val overlapY = minOf(a.getY2(), b.getY2()) - maxOf(a.getY(), b.getY())
            val minHeight = minOf(a.getHeight(), b.getHeight())
            val maxHeight = maxOf(a.getHeight(), b.getHeight())

            // Both checks: minimum edge has good overlap and sizes aren't too different
            if (overlapY > 0 && overlapY / minHeight >= alignmentPercentage &&
                minHeight / maxHeight >= alignmentPercentage
            ) { // Prevent L-shapes with large differences
                return true
            }
        }

        // Horizontal edge touch (top/bottom), check X overlap and percentage
        val yTouch = abs(a.getY2() - b.getY()) < 1 || abs(b.getY2() - a.getY()) < 1 ||
            (a.getY() < b.getY2() && a.getY2() > b.getY())
        if (yTouch) {
            val overlapX = minOf(a.getX2(), b.getX2()) - maxOf(a.getX(), b.getX())
            val minWidth = minOf(a.getWidth(), b.getWidth())
            val maxWidth = maxOf(a.getWidth(), b.getWidth())

            // Both checks: minimum edge has good overlap and sizes aren't too different
            if (overlapX > 0 && overlapX / minWidth >= alignmentPercentage &&
                minWidth / maxWidth >= alignmentPercentage
            ) { // Prevent L-shapes with large differences
                return true
            }
        }

        return false
    }
}