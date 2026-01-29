package org.grobid.core.utilities

import org.apache.commons.lang3.StringUtils
import org.grobid.core.engines.label.TaggingLabels

object LabelUtils {
    /**
     * Post-process text labeled by the fulltext model on chunks that are known to be text (no table, or figure)
     * It converts table and figure labels to paragraph labels.
     */
    @JvmStatic
    fun postProcessFullTextLabeledText(fulltextLabeledText: String): String {
        val result = StringBuilder()

        val lines = fulltextLabeledText
            .split("\n".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()
        var previousLabel: String? = null

        for (i in lines.indices) {
            val line = lines[i]
            if (StringUtils.isBlank(line)) continue

            val pieces = line.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val label = pieces[pieces.size - 1]
            if (label == "I-" + TaggingLabels.FIGURE.label || label == "I-" + TaggingLabels.TABLE.label) {
                if (previousLabel == null || !previousLabel.endsWith(TaggingLabels.PARAGRAPH.label)) {
                    pieces[pieces.size - 1] = "I-" + TaggingLabels.PARAGRAPH.label
                } else {
                    pieces[pieces.size - 1] = TaggingLabels.PARAGRAPH.label
                }
            } else if (label == TaggingLabels.FIGURE.label || label == TaggingLabels.TABLE.label) {
                pieces[pieces.size - 1] = TaggingLabels.PARAGRAPH.label
            }
            result.append(pieces.joinToString("\t"))
            previousLabel = label
            result.append("\n")
        }

        return result.toString()
    }

    /**
     * This method correct the fulltext sequence when the model has predicted several unlikely
     * start sequences of table or figures.
     * For example: I-<figure> followed by another I-<figure> (or table) </figure></figure>
     **/
    @JvmStatic
    fun postProcessFulltextFixInvalidTableOrFigure(fulltextLabeledText: String): String {
        val result = StringBuilder()

        val lines = fulltextLabeledText
            .split("\n".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()

        var previousLabel: String? = null
        for (i in lines.indices) {
            val line = lines[i]
            if (StringUtils.isBlank(line)) continue

            val pieces = line
                .split("\t".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()

            val label = pieces[pieces.size - 1]
            if (label == "I-" + TaggingLabels.FIGURE.label) {
                if (StringUtils.equals(previousLabel, "I-" + TaggingLabels.FIGURE.label)) {
                    pieces[pieces.size - 1] = TaggingLabels.FIGURE.label
                }
            } else if (label == "I-" + TaggingLabels.TABLE.label) {
                if (StringUtils.equals(previousLabel, "I-" + TaggingLabels.TABLE.label)) {
                    pieces[pieces.size - 1] = TaggingLabels.TABLE.label
                }
            }

            result.append(pieces.joinToString("\t"))
            previousLabel = label
            result.append("\n")
        }

        return result.toString()
    }
}
