package org.grobid.core.document

import org.grobid.core.engines.config.GrobidAnalysisConfig
import org.grobid.core.factory.AbstractEngineFactory
import org.grobid.core.layout.Block
import org.grobid.core.layout.Page
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.greaterThanOrEqualTo
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test


class DocumentUtilitiesTest {
    @BeforeTest
    @Throws(java.lang.Exception::class)
    fun setInitialContext() {
        AbstractEngineFactory.init()
    }

    @Test
    @Throws(Exception::class)
    fun testGetMedianDistanceForBlocks1() {
        val input = File(this.javaClass.getResource("sampleDocument.1.pdf")?.toURI())
        val doc = DocumentSource.fromPdf(input)

        val document = Document(doc)
        document.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance())

        val medianBlockDistance = DocumentUtilities.getMedianBlockVerticalDistance(document)
        println("Median block distance: $medianBlockDistance")
        assertThat(medianBlockDistance, `is`(greaterThan(0.0)))
        doc.close(true, true, true)
    }

    @Test
    @Throws(Exception::class)
    fun testGetMedianDistanceForBlocks2() {
        val input = File(this.javaClass.getResource("sampleDocument.2.pdf")?.toURI())
        val doc = DocumentSource.fromPdf(input)

        val document = Document(doc)
        document.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance())

        val medianBlockDistance = DocumentUtilities.getMedianBlockVerticalDistance(document)
        println("Median block distance: $medianBlockDistance")
        assertThat(medianBlockDistance, `is`(greaterThan(0.0)))
        doc.close(true, true, true)
    }

    @Test
    @Throws(Exception::class)
    fun testGetMedianDistanceForLines1() {
        val input = File(this.javaClass.getResource("sampleDocument.1.pdf")?.toURI())
        val doc = DocumentSource.fromPdf(input)

        val document = Document(doc)
        document.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance())

        val medianBlockDistance = DocumentUtilities.getMedianLineVerticalDistance(document)
        println("Median vertical line distance: $medianBlockDistance")
        assertThat(medianBlockDistance, `is`(greaterThan(0.0)))
        doc.close(true, true, true)
    }

    @Test
    @Throws(Exception::class)
    fun testGetMedianDistanceForLines2() {
        val input = File(this.javaClass.getResource("sampleDocument.2.pdf")?.toURI())
        val doc = DocumentSource.fromPdf(input)

        val document = Document(doc)
        document.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance())

        val medianBlockDistance = DocumentUtilities.getMedianLineVerticalDistance(document)
        println("Median vertical line distance: $medianBlockDistance")
        assertThat(medianBlockDistance, `is`(greaterThan(0.0)))
        doc.close(true, true, true)
    }

    @Test
    @Throws(Exception::class)
    fun testCalculateAdaptiveThresholds1() {
        val input = File(this.javaClass.getResource("sampleDocument.1.pdf")?.toURI())
        val doc = DocumentSource.fromPdf(input)

        val document = Document(doc)
        document.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance())

        val adaptiveThresholds = DocumentUtilities.calculateAdaptiveThresholds(document)
        println("Adaptive thresholds for document 1:")
        println("  Standard threshold: ${adaptiveThresholds.standardThreshold}")
        println("  Multi-column threshold: ${adaptiveThresholds.multiColumnThreshold}")
        println("  Vertical tolerance: ${adaptiveThresholds.verticalTolerance}")
        println("  Has multiple columns: ${adaptiveThresholds.layoutInfo.hasMultipleColumns}")
        println("  Estimated columns: ${adaptiveThresholds.layoutInfo.estimatedColumns}")

        assertThat(adaptiveThresholds.standardThreshold, `is`(greaterThan(0.0)))
        assertThat(adaptiveThresholds.multiColumnThreshold, `is`(greaterThan(0.0)))
        assertThat(adaptiveThresholds.verticalTolerance, `is`(greaterThan(0.0)))
        assertThat(adaptiveThresholds.multiColumnThreshold, `is`(greaterThanOrEqualTo(adaptiveThresholds.standardThreshold)))
        doc.close(true, true, true)
    }

    @Test
    @Throws(Exception::class)
    fun testCalculateAdaptiveThresholds2() {
        val input = File(this.javaClass.getResource("sampleDocument.2.pdf")?.toURI())
        val doc = DocumentSource.fromPdf(input)

        val document = Document(doc)
        document.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance())

        val adaptiveThresholds = DocumentUtilities.calculateAdaptiveThresholds(document)
        println("Adaptive thresholds for document 2:")
        println("  Standard threshold: ${adaptiveThresholds.standardThreshold}")
        println("  Multi-column threshold: ${adaptiveThresholds.multiColumnThreshold}")
        println("  Vertical tolerance: ${adaptiveThresholds.verticalTolerance}")
        println("  Has multiple columns: ${adaptiveThresholds.layoutInfo.hasMultipleColumns}")
        println("  Estimated columns: ${adaptiveThresholds.layoutInfo.estimatedColumns}")

        assertThat(adaptiveThresholds.standardThreshold, `is`(greaterThan(0.0)))
        assertThat(adaptiveThresholds.multiColumnThreshold, `is`(greaterThan(0.0)))
        assertThat(adaptiveThresholds.verticalTolerance, `is`(greaterThan(0.0)))
        assertThat(adaptiveThresholds.multiColumnThreshold, `is`(greaterThanOrEqualTo(adaptiveThresholds.standardThreshold)))
        doc.close(true, true, true)
    }

    @Test
    @Throws(Exception::class)
    fun testAdaptiveThresholdsWithEmptyDocument() {
        val document = Document()

        // Should not throw exception even with empty document
        val adaptiveThresholds = DocumentUtilities.calculateAdaptiveThresholds(document)

        println("Adaptive thresholds for empty document:")
        println("  Standard threshold: ${adaptiveThresholds.standardThreshold}")
        println("  Multi-column threshold: ${adaptiveThresholds.multiColumnThreshold}")
        println("  Vertical tolerance: ${adaptiveThresholds.verticalTolerance}")

        assertThat(adaptiveThresholds.standardThreshold, `is`(greaterThanOrEqualTo(5.0)))
        assertThat(adaptiveThresholds.multiColumnThreshold, `is`(greaterThanOrEqualTo(5.0)))
        assertThat(adaptiveThresholds.verticalTolerance, `is`(greaterThanOrEqualTo(1.0)))
        assertThat(adaptiveThresholds.layoutInfo.hasMultipleColumns, `is`(false))
        assertThat(adaptiveThresholds.layoutInfo.estimatedColumns, `is`(1))
    }
}