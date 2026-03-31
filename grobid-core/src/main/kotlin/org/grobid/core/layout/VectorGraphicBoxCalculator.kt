package org.grobid.core.layout

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import org.apache.batik.anim.dom.SAXSVGDocumentFactory
import org.apache.batik.bridge.BridgeContext
import org.apache.batik.bridge.DocumentLoader
import org.apache.batik.bridge.UserAgent
import org.apache.batik.bridge.UserAgentAdapter
import org.apache.batik.parser.AWTPathProducer
import org.apache.batik.parser.PathParser
import org.apache.batik.util.XMLResourceDescriptor
import org.apache.commons.lang3.StringUtils
import org.grobid.core.document.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.svg.SVGDocument
import org.w3c.dom.svg.SVGElement
import org.w3c.dom.svg.SVGLocatable
import java.io.File
import java.io.IOException
import java.util.*
import java.util.stream.Collectors

/**
 * Working with vector graphics
 */
object VectorGraphicBoxCalculator {
    private val LOGGER: Logger = LoggerFactory.getLogger(VectorGraphicBoxCalculator::class.java)

    const val MINIMUM_VECTOR_BOX_AREA: Int = 3000
    const val MINIMUM_BITMAP_AREA: Int = 10000
    val VEC_GRAPHICS_FILE_SIZE_LIMIT: Int = 100 * 1024 * 1024
    const val MAX_OUTSIDE_RATIO = 0.10

    @JvmStatic
    @Throws(IOException::class)
    fun calculate(document: Document): Multimap<Int?, GraphicObject?> {
        val blockMultimap = document.getBlocksPerPage()
        val result: Multimap<Int?, GraphicObject?> = LinkedHashMultimap.create<Int?, GraphicObject?>()
        val finalListOfBoxes: MutableList<BoundingBox?> = ArrayList<BoundingBox?>()

        // init BATIK stuff
        val docFactory = SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName())
        val ua: UserAgent = UserAgentAdapter()
        val loader = DocumentLoader(ua)
        val ctx = BridgeContext(ua, loader)
        ctx.setDynamicState(BridgeContext.DYNAMIC)

        for (pageNum in 1..document.getPages().size) {
            val mainPageArea = document.getPage(pageNum).mainArea

            //String q = XQueryProcessor.getQueryFromResources("vector-coords.xq");
            val vecFile =
                File(document.getDocumentSource().xmlFile.absolutePath + "_data", "image-$pageNum.svg")
            if (vecFile.exists()) {
                if (vecFile.length() > VEC_GRAPHICS_FILE_SIZE_LIMIT) {
                    LOGGER.warn("The vector file " + vecFile + " is too large to be processed, size: " + vecFile.length())
                    continue
                }

                //XQueryProcessor pr = new XQueryProcessor(vecFile);
                val doc = docFactory.createSVGDocument(vecFile.path)
                val boxesFromClips: MutableList<BoundingBox?> = extractBoxesFromClips(doc, pageNum)

                val boxesFromGroups = extractBoxesFromGroupElements(doc, pageNum)

                val allBoxes = ArrayList<BoundingBox?>()
                allBoxes.addAll(boxesFromClips)
                allBoxes.addAll(boxesFromGroups)

                val boxesSortedBySize: MutableList<BoundingBox> = allBoxes.stream()
                    .distinct()
                    .sorted { b1: BoundingBox?, b2: BoundingBox? -> b2!!.area().compareTo(b1!!.area()) }
                    .collect(Collectors.toList())

                val boxesToKeep: MutableList<BoundingBox?> = ArrayList<BoundingBox?>()
                for (box in boxesSortedBySize) {
                    // Remove boxes that
                    // - are contained in other boxes or
                    // - that are outside the main area
                    if (
                        boxesSortedBySize.stream()
                            .noneMatch {
                                b: BoundingBox? -> b !== box && b!!.contains(box)
                            } && (mainPageArea.contains(box) || box.calculateOutsideRatio(mainPageArea) <= MAX_OUTSIDE_RATIO)
                        && box.area() >= MINIMUM_VECTOR_BOX_AREA
                    ) {
                        boxesToKeep.add(box)
                    }
                }

                finalListOfBoxes.addAll(boxesToKeep)
            }
        }

        // Transformation of bounding boxes to GraphicObjects
        finalListOfBoxes.stream()
            .map { b: BoundingBox? -> GraphicObject(b, GraphicObjectType.VECTOR_BOX) }
            .forEach { go: GraphicObject? -> result.put(go!!.boundingBox.page, go) }

        return result
    }

    /**
     * Determines if an SVG element is transparent and should be ignored.
     *
     * @param element The SVG element to check
     * @return true if the element is determined to be transparent
     */
    private fun isTransparentSVG(element: SVGElement): Boolean {
        // Check direct fill-opacity attribute on the element
        if (element.hasAttribute("fill-opacity") &&
            element.getAttribute("fill-opacity").toDoubleOrNull() == 0.0
        ) {
            return true
        }

        // Check style attribute for fill-opacity
        val styleValue = element.getAttribute("style")
        if (StringUtils.isBlank(styleValue)) {
            return false
        }

        var hasOpacityAttribute = false
        var opacityValue = 1.0
        var fillOpacityValue = 1.0
        var hasFillNone = false
        var hasStroke = false

        for (attributePair in styleValue.split(";").filter { it.isNotEmpty() }) {
            val cleanPair = attributePair.trim()
            val separatorIndex = cleanPair.indexOf(":")

            if (separatorIndex != -1) {
                val key = cleanPair.substring(0, separatorIndex).trim()
                val value = cleanPair.substring(separatorIndex + 1).trim()

                when (key) {
                    "opacity" -> {
                        hasOpacityAttribute = true
                        opacityValue = value.toDoubleOrNull() ?: 1.0
                    }

                    "fill-opacity" -> {
                        fillOpacityValue = value.toDoubleOrNull() ?: 1.0
                    }

                    "fill" -> {
                        hasFillNone = value == "none"
                    }

                    "stroke" -> {
                        hasStroke = value != "none" //&& !value.equals("#000000", ignoreCase = true)
                    }
                }
            }
        }

        // Element is effectively transparent if:
        // - Has zero opacity
        // - Has zero fill-opacity and no visible stroke
        // - Has fill:none and no visible stroke
        return (hasOpacityAttribute && opacityValue < 0.01) ||
            (fillOpacityValue < 0.01 && !hasStroke) ||
            (hasFillNone && !hasStroke)
    }

    private fun extractBoxesFromClips(
        doc: SVGDocument?,
        pageNum: Int
    ): MutableList<BoundingBox?> {
        //GVTBuilder builder = new GVTBuilder();
        // GraphicsNode rootGN = builder.build(ctx, doc);
        val boxes: MutableList<BoundingBox?> = ArrayList<BoundingBox?>()

        // Get clipPath elements
        val clipPaths = doc!!.getElementsByTagNameNS("http://www.w3.org/2000/svg", "clipPath")
        for (i in 0 until clipPaths.length) {
            val clipPathElement = clipPaths.item(i) as SVGElement

            // Analyze child elements inside the clipPath
            val childElements = clipPathElement.childNodes
            var aggregatedBox: BoundingBox? = null

            // Process each child element
            for (j in 0 until childElements.getLength()) {
                if (childElements.item(j) is SVGElement) {
                    val child = childElements.item(j) as SVGElement

                    // For path elements, get path data and calculate bounds
                    if (child.getTagName() == "path") {
                        // Skip transparent paths
                        if (isTransparentSVG(child)) {
                            continue
                        }

                        val pathData = child.getAttribute("d")
                        // Use Batik's PathParser to parse path data
                        val pathBox = calculatePathBounds(pathData, pageNum)

                        // Merge with aggregated box
                        if (aggregatedBox == null) {
                            aggregatedBox = pathBox
                        } else if (pathBox != null) {
                            aggregatedBox = aggregatedBox.boundBox(pathBox)
                        }
                    } else if (child is SVGLocatable) {
                        try {
                            val locatable = child as SVGLocatable
                            val rect = locatable.getBBox()
                            if (rect != null) {
                                val elementBox = BoundingBox.fromPointAndDimensions(
                                    pageNum,
                                    rect.getX().toDouble(),
                                    rect.getY().toDouble(),
                                    rect.getWidth().toDouble(),
                                    rect.getHeight().toDouble()
                                )

                                if (aggregatedBox == null) {
                                    aggregatedBox = elementBox
                                } else {
                                    aggregatedBox = aggregatedBox.boundBox(elementBox)
                                }
                            }
                        } catch (e: Exception) {
                            // Some elements might not support getBBox
                            LOGGER.debug("Could not get bounding box for element: " + child.getTagName(), e)
                        }
                    }
                }
            }

            // Add the resulting aggregated box if valid
            if (aggregatedBox != null && aggregatedBox.area() > 0) {
                boxes.add(aggregatedBox)
            }
        }
        return boxes
    }

    //                 !!!!!!
// TBD: try to get simply the clip box instead of recomputing all the SVG area (because it can take ages in rare cases!)
    //                 note: getBBox on the clipBox is producing nothing because no rendering of clipBox of course, we need to find another
    //                 way to get the BB of clipPath
    //                 !!!!!!
    /**
     * Extracts bounding boxes from SVG group elements when no boxes were detected from clipPath elements.
     *
     * @param doc The SVG document
     * @param pageNum The current page number
     * @return List of valid bounding boxes extracted from group elements
     */
    private fun extractBoxesFromGroupElements(
        doc: SVGDocument,
        pageNum: Int
    ): MutableList<BoundingBox?> {
        val boxes: MutableList<BoundingBox?> = ArrayList<BoundingBox?>()

        val nodeList = doc.getElementsByTagNameNS("http://www.w3.org/2000/svg", "g")
        if (nodeList.length == 0) {
            return boxes
        }

        // Check if all groups are white cache elements
        var allDummyCache = true
        for (i in 0 until nodeList.length) {
            val item = nodeList.item(i) as SVGElement
            if (!isDummyCacheSVG(item)) {
                allDummyCache = false
                break
            }
        }

        // Skip if all groups are just white cache elements
        if (allDummyCache) {
            LOGGER.debug("Page {}: SVG document contains only white cache elements, skipping", pageNum)
            return boxes
        }

        // Process each group element
        for (i in 0 until nodeList.getLength()) {
            try {
                val item = nodeList.item(i) as SVGElement?

                // Skip transparent elements
                if (item == null) {
                    continue
                }

                if (item != null && isTransparentSVG(item)) {
                    LOGGER.debug("Skipping transparent group element: {}", item!!.getAttribute("id"))
                    continue
                }

                // Skip elements with specific problematic attributes
                if (item.hasAttribute("evenodd") == true) {
                    LOGGER.debug(
                        "Skipping element with invalid evenodd attribute: {}",
                        item.getAttribute("id")
                    )
                    continue
                }


                val locatable = item as SVGLocatable
                var box: BoundingBox? = null

                // First try to get the bounding box directly
                val rect = locatable.getBBox()
                if (rect != null) {
                    box = BoundingBox.fromPointAndDimensions(
                        pageNum,
                        rect.getX().toDouble(),
                        rect.getY().toDouble(),
                        rect.getWidth().toDouble(),
                        rect.getHeight().toDouble()
                    )
                } else {
                    // If getBBox() returns null, try to get bounding box from child path elements
                    val paths = item.getElementsByTagNameNS("http://www.w3.org/2000/svg", "path")
                    if (paths.length > 0) {
                        // Combine all path-bounding boxes in this group
                        for (j in 0 until paths.length) {
                            val pathElement = paths.item(j) as SVGElement
                            val pathData = pathElement.getAttribute("d")
                            val pathBox = calculatePathBounds(pathData, pageNum)

                            if (pathBox != null) {
                                box = if (box == null) pathBox else box.boundBox(pathBox)
                            }
                        }
                        LOGGER.debug("Created bounding box from path data for group: {}", item.getAttribute("id"))
                    }
                }

                // Preliminary filtering
                if (box == null || box.area() == 0.0) {
                    continue
                }

                boxes.add(box)
            } catch (e: Exception) {
                LOGGER.debug("Error processing SVG group element: {}", e.message)
            }
        }

        // Filter out boxes that are just lines (height or width < 1)
        return boxes.stream()
            .filter { box: BoundingBox? -> box!!.getHeight() >= 1 && box.getWidth() >= 1 }
            .collect(Collectors.toList())
    }

    /**
     * Calculates the bounding box for a given SVG path data.
     *
     * @param pathData The SVG path data string
     * @param pageNum The current page number
     * @return BoundingBox if the path is valid, null otherwise
     */
    private fun calculatePathBounds(pathData: String?, pageNum: Int): BoundingBox? {
        if (pathData == null || pathData.trim { it <= ' ' }.isEmpty()) {
            return null
        }

        try {
            // Create a path producer to convert SVG path to a Java2D shape
            val pathProducer = AWTPathProducer()

            val pathParser = PathParser()
            pathParser.setPathHandler(pathProducer)
            pathParser.parse(pathData)

            val shape = pathProducer.getShape()
            val bounds = shape.getBounds2D()

            if (bounds.getWidth() > 0 && bounds.getHeight() > 0) {
                return BoundingBox.fromPointAndDimensions(
                    pageNum,
                    bounds.getX(),
                    bounds.getY(),
                    bounds.getWidth(),
                    bounds.getHeight()
                )
            }
        } catch (e: Exception) {
            LOGGER.debug("Could not parse SVG path data: {}", pathData, e)
        }

        return null
    }

    /**
     * Determines if an SVG element is a "dummy cache" element.
     *
     *
     * Dummy cache elements are typically white filling areas used for aesthetics
     * rather than actual graphics content. These elements usually have a style
     * attribute with "fill: #FFFFFF" or equivalent white color values.
     *
     * @param element The SVG element to check
     * @return true if the element is determined to be a dummy cache element
     */
    fun isDummyCacheSVG(element: SVGElement): Boolean {
        val styleValue = element.getAttribute("style")
        if (StringUtils.isBlank(styleValue)) {
            return false
        }

        for (attributePair in styleValue.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            var attributePair = attributePair
            attributePair = attributePair.trim { it <= ' ' }
            val separatorIndex = attributePair.indexOf(":")

            if (separatorIndex != -1) {
                val name = attributePair.substring(0, separatorIndex).trim { it <= ' ' }.lowercase(Locale.getDefault())
                val value =
                    attributePair.substring(separatorIndex + 1).trim { it <= ' ' }.lowercase(Locale.getDefault())

                if ("fill" == name &&
                    ("#ffffff" == value || "#fff" == value || "white" == value)
                ) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Merge bounding boxes in case of intersection
     */
    @JvmStatic
    fun mergeBoxes(boxes: MutableList<BoundingBox?>?): MutableList<BoundingBox?> {
        if (boxes == null || boxes.isEmpty()) {
            return ArrayList<BoundingBox?>()
        }

        // Create a copy to avoid modifying the input list
        val workingCopy: MutableList<BoundingBox?> = ArrayList<BoundingBox?>(boxes)

        var mergeOccurred: Boolean
        do {
            mergeOccurred = false
            for (i in workingCopy.indices) {
                val current = workingCopy.get(i)
                if (current == null) continue

                for (j in i + 1 until workingCopy.size) {
                    val other = workingCopy.get(j)
                    if (other == null) continue

                    if (current.intersect(other)) {
                        // Merge the boxes
                        workingCopy.set(i, current.boundBox(other))
                        workingCopy.set(j, null)
                        mergeOccurred = true
                    }
                }
            }
        } while (mergeOccurred)

        // Filter out null values and return
        return workingCopy.stream()
            .filter { obj: BoundingBox? -> Objects.nonNull(obj) }
            .collect(Collectors.toList())
    }

    /**
     * Merge bounding boxes in case of close proximity defined by a max proximity distance
     */
    @JvmStatic
    fun glueBoxes(boxes: MutableList<BoundingBox?>?, maxProximityDistance: kotlin.Double): MutableList<BoundingBox?> {
        if (boxes == null || boxes.isEmpty()) {
            return ArrayList<BoundingBox?>()
        }

        // Create a working copy to avoid modifying the input list
        val workingCopy: MutableList<BoundingBox?> = ArrayList<BoundingBox?>(boxes)

        var mergeOccurred: Boolean
        do {
            mergeOccurred = false
            for (i in workingCopy.indices) {
                val current = workingCopy.get(i)
                if (current == null) continue

                for (j in i + 1 until workingCopy.size) {
                    val other = workingCopy.get(j)
                    if (other == null) continue

                    if (current.distanceTo(other) < maxProximityDistance) {
                        // Merge boxes that are within the proximity distance
                        workingCopy.set(i, current.boundBox(other))
                        workingCopy.set(j, null)
                        mergeOccurred = true
                    }
                }
            }
        } while (mergeOccurred)

        // Filter out null values and boxes that are too small
        return workingCopy.stream()
            .filter { box: BoundingBox? -> box != null && box.getHeight() >= 5 && box.getWidth() >= 5 }
            .collect(Collectors.toList())
    }
}
