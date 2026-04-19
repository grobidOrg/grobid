package org.grobid.core.document

import org.grobid.core.layout.Block
import org.grobid.core.layout.BoundingBox
import org.grobid.core.layout.LayoutToken
import java.util.stream.Collectors

object DocumentUtilities {

    private fun calculateMedian(numbers: List<Double>): Double {
        if (numbers.isEmpty()) {
            throw IllegalArgumentException("The list cannot be empty")
        }

        val sortedNumbers = numbers.sorted()
        val size = sortedNumbers.size

        return if (size % 2 == 1) {
            sortedNumbers[size / 2]
        } else {
            val middle1 = sortedNumbers[size / 2 - 1]
            val middle2 = sortedNumbers[size / 2]
            (middle1 + middle2) / 2
        }
    }

    @JvmStatic
    fun getMedianBlockVerticalDistance(document: Document): Double {
        // Median distance between blocks
        if (document.getBlocks() == null || document.getBlocks().isEmpty()) {
            return 10.0 // Default value for empty documents
        }

        var previousBlock: Block? = null
        val distances: MutableList<Double> = ArrayList()
        val filteredBlocks = document.getBlocks().stream()
            .filter { b: Block -> b.boundingBox != null }
            .sorted { b1: Block, b2: Block ->
                if (b1.page.number != b2.page.number) {
                    b1.page.number.compareTo(b2.page.number)
                } else {
                    (b1.boundingBox!!.y).compareTo(b2.boundingBox!!.y)
                }
            }
            .collect(Collectors.toList())

        for (block in filteredBlocks) {
            if (
                previousBlock == null
                || previousBlock.page.number != block.page.number
            ) {
                previousBlock = block
                continue
            }

            val distance = previousBlock.boundingBox.verticalDistanceTo(block.boundingBox)
            if (distance > 0) {
                distances.add(distance)
            }
            previousBlock = block
        }

        return calculateMedian(distances)
    }

    @JvmStatic
    fun getMedianLineVerticalDistance(
        document: Document
    ): Double {
        if (document.getBlocks() == null || document.getBlocks().isEmpty()) {
            return 2.0 // Default value for empty documents
        }

        val filteredTokensSortedVertically = document.getBlocks().stream()
            .filter { b: Block -> b.boundingBox != null }
            .flatMap { b: Block -> b.tokens.stream() }
            .collect(Collectors.groupingBy { l: LayoutToken -> Pair(l.page, l.y) })
            .values
            .stream()
            .map { tokens -> tokens[0] }
            .filter { l: LayoutToken -> l.x >= 0 && l.y >= 0 }
            .sorted { l1: LayoutToken, l2: LayoutToken ->
                if (l1.page != l2.page) {
                    l1.page.compareTo(l2.page)
                } else {
                    l1.y.compareTo(l2.y)
                }
            }
            .collect(Collectors.toList())

        var previousToken: LayoutToken? = null
        val distances: MutableList<Double> = ArrayList()
        for (token in filteredTokensSortedVertically) {
            if (
                previousToken == null
                || previousToken.page != token.page
            ) {
                previousToken = token
                continue
            }

            val distance = BoundingBox
                .fromLayoutToken(previousToken)
                .verticalDistanceTo(BoundingBox.fromLayoutToken(token))

            if (distance > 0) {
                distances.add(distance)
            }
            previousToken = token
        }

        return calculateMedian(distances)
    }

    /**
     * Calculate adaptive block merging thresholds based on document layout statistics
     */
    @JvmStatic
    fun calculateAdaptiveThresholds(document: Document): AdaptiveThresholds {
        val medianBlockDistance = getMedianBlockVerticalDistance(document)
        val medianLineDistance = getMedianLineVerticalDistance(document)
        val layoutInfo = analyzeDocumentLayout(document)

        // Base threshold for standard block merging (conservative)
        val standardThreshold = Math.max(5.0, medianBlockDistance * 1.2)

        // Multi-column threshold: allows larger horizontal gaps when vertically aligned
        val multiColumnThreshold = if (layoutInfo.hasMultipleColumns) {
            calculateMultiColumnThreshold(document, layoutInfo)
        } else {
            standardThreshold * 1.3 // Slightly more permissive even for single column
        }

        // Vertical alignment tolerance - how close blocks need to be vertically
        val verticalTolerance = Math.max(1.0, medianLineDistance * 0.8)

        return AdaptiveThresholds(
            standardThreshold = standardThreshold,
            multiColumnThreshold = multiColumnThreshold,
            verticalTolerance = verticalTolerance,
            layoutInfo = layoutInfo
        )
    }

    /**
     * Analyze document layout to understand column structure and spacing patterns
     */
    private fun analyzeDocumentLayout(document: Document): LayoutInfo {
        if (document.blocks == null || document.blocks.isEmpty()) {
            return LayoutInfo(false, 1, true, emptyList())
        }

        val blocksByPage = document.blocks.groupBy { it.page.number }
        val pageAnalyses = blocksByPage.map { (pageNum, blocks) ->
            analyzePageLayout(pageNum, blocks.filter { it.boundingBox != null })
        }

        val hasMultipleColumns = pageAnalyses.any { it.estimatedColumns > 1 }
        val mostCommonColumns = pageAnalyses
            .groupBy { it.estimatedColumns }
            .maxByOrNull { it.value.size }?.key ?: 1

        return LayoutInfo(
            hasMultipleColumns = hasMultipleColumns,
            estimatedColumns = mostCommonColumns,
            isConsistentColumns = pageAnalyses.all { it.estimatedColumns == mostCommonColumns },
            pageAnalyses = pageAnalyses
        )
    }

    /**
     * Analyze a single page to understand its column structure
     */
    private fun analyzePageLayout(pageNumber: Int, blocks: List<Block>): PageLayoutAnalysis {
        if (blocks.isEmpty()) {
            return PageLayoutAnalysis(pageNumber, 1, emptyList())
        }

        // Sort blocks by their x position to analyze horizontal distribution
        val sortedByX = blocks.sortedBy { it.boundingBox!!.x }

        // Find column gaps by looking for significant horizontal spacing
        val xPositions = sortedByX.map { it.boundingBox!!.x + it.boundingBox!!.width / 2.0 }
        val horizontalGaps = xPositions.zipWithNext { a, b -> b - a }

        // Use median gap as baseline, look for gaps that are significantly larger
        val medianGap = horizontalGaps.median()
        val columnGaps = horizontalGaps
            .mapIndexed { index, gap -> index to gap }
            .filter { (_, gap) -> gap > medianGap * 1.5 }
            .map { (index, _) -> xPositions[index] }

        val estimatedColumns = columnGaps.size + 1

        return PageLayoutAnalysis(
            pageNumber = pageNumber,
            estimatedColumns = estimatedColumns,
            columnSeparators = columnGaps
        )
    }

    /**
     * Calculate appropriate threshold for multi-column block merging
     */
    private fun calculateMultiColumnThreshold(document: Document, layoutInfo: LayoutInfo): Double {
        if (document.blocks == null || document.blocks.isEmpty()) {
            return 18.0 // Default fallback
        }

        val blocks = document.blocks.filter { it.boundingBox != null }

        if (blocks.size < 10) {
            // Not enough data for statistical analysis, use conservative default
            return 18.0
        }

        // Find blocks that are vertically aligned (same line) and calculate their horizontal distances
        val verticallyAlignedPairs = findVerticallyAlignedBlockPairs(blocks)

        if (verticallyAlignedPairs.isEmpty()) {
            // No vertically aligned blocks found, use standard logic
            return 15.0
        }

        val horizontalDistances = verticallyAlignedPairs.map { (block1, block2) ->
            Math.abs(block1.boundingBox!!.x - block2.boundingBox!!.x)
        }

        // Use 75th percentile as threshold to accommodate most column distances
        val sortedDistances = horizontalDistances.sorted()
        val index = (sortedDistances.size * 0.75).toInt().coerceAtMost(sortedDistances.size - 1)
        val recommendedThreshold = sortedDistances[index]

        // Add some tolerance and cap at reasonable values
        return Math.max(15.0, Math.min(50.0, recommendedThreshold * 1.2))
    }

    /**
     * Find pairs of blocks that are vertically aligned (on the same line)
     */
    private fun findVerticallyAlignedBlockPairs(blocks: List<Block>): List<Pair<Block, Block>> {
        if (blocks.isEmpty()) {
            return emptyList()
        }

        val blocksByPage = blocks.groupBy { it.page.number }
        val alignedPairs = mutableListOf<Pair<Block, Block>>()

        for ((_, pageBlocks) in blocksByPage) {
            val validPageBlocks = pageBlocks.filter { it.boundingBox != null }
            if (validPageBlocks.size < 2) continue

            val sortedByY = validPageBlocks.sortedBy { it.boundingBox!!.y }

            // Use a simpler approach - don't create temporary document
            // Just use a fixed tolerance for vertical alignment
            val verticalTolerance = 3.0 // 3 points tolerance for being on same line

            for (i in 0 until sortedByY.size - 1) {
                val block1 = sortedByY[i]
                val block2 = sortedByY[i + 1]

                if (block1.boundingBox != null && block2.boundingBox != null) {
                    val verticalDistance = Math.abs(block1.boundingBox!!.verticalDistanceTo(block2.boundingBox))

                    // Consider blocks aligned if they're within tolerance
                    if (verticalDistance <= verticalTolerance) {
                        alignedPairs.add(Pair(block1, block2))
                    }
                }
            }
        }

        return alignedPairs
    }
}

/**
 * Adaptive thresholds calculated based on document layout statistics
 */
data class AdaptiveThresholds(
    val standardThreshold: Double,
    val multiColumnThreshold: Double,
    val verticalTolerance: Double,
    val layoutInfo: LayoutInfo
)

/**
 * Information about document layout structure
 */
data class LayoutInfo(
    val hasMultipleColumns: Boolean,
    val estimatedColumns: Int,
    val isConsistentColumns: Boolean,
    val pageAnalyses: List<PageLayoutAnalysis>
)

/**
 * Layout analysis for a single page
 */
data class PageLayoutAnalysis(
    val pageNumber: Int,
    val estimatedColumns: Int,
    val columnSeparators: List<Double>
)

/**
 * Extension function to calculate median of double list
 */
private fun List<Double>.median(): Double {
    if (isEmpty()) return 0.0
    val sorted = sorted()
    val size = size
    return if (size % 2 == 1) {
        sorted[size / 2]
    } else {
        (sorted[size / 2 - 1] + sorted[size / 2]) / 2
    }
}