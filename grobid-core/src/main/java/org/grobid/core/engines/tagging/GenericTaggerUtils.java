package org.grobid.core.engines.tagging;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.utilities.Triple;
import org.grobid.analyzers.grobidkr.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

public class GenericTaggerUtils {

    // Deprecated, please use the constants from TaggingLabels
    @Deprecated
    public static final String START_ENTITY_LABEL_PREFIX = "I-";
    @Deprecated
    public static final String START_ENTITY_LABEL_PREFIX_ALTERNATIVE = "B-";
    @Deprecated
    public static final String START_ENTITY_LABEL_PREFIX_ALTERNATIVE_2 = "E-";

    public static final Pattern SEPARATOR_PATTERN = Pattern.compile("[\t ]");

    /**
     * @param labeledResult
     * labeled result from a tagger
     * @return a list of pairs - first element in a pair is a token itself, the second is a label (e.g. <footnote> or
     * I-<footnote>) Note an empty line in the result will be transformed to a 'null' pointer of a pair
     */
    public static List<Pair<String, String>> getTokensAndLabels(String labeledResult) {
        return processLabeledResult(labeledResult, splits -> Pair.of(splits.get(0), splits.get(splits.size() - 1)));
    }

    /**
     * @param labeledResult
     * labeled result from a tagger
     * @return a list of triples - first element in a pair is a token itself, the second is a label (e.g. <footnote> or
     * I-<footnote>) and the third element is a string with the features Note an empty line in the result will be
     * transformed to a 'null' pointer of a pair
     */
    public static List<Triple<String, String, String>> getTokensWithLabelsAndFeatures(
            String labeledResult,
            final boolean addFeatureString) {
        Function<List<String>, Triple<String, String, String>> fromSplits = splits -> {
            String featureString = addFeatureString ? Joiner.on("\t").join(splits.subList(0, splits.size() - 1)) : null;
            return new Triple<>(splits.get(0), splits.get(splits.size() - 1), featureString);
        };

        return processLabeledResult(labeledResult, fromSplits);
    }

    private static <T> List<T> processLabeledResult(String labeledResult, Function<List<String>, T> fromSplits) {
        String[] lines = labeledResult.split("\n");
        List<T> res = new ArrayList<>(lines.length);
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                res.add(null);
                continue;
            }
            List<String> splits = Splitter.on(SEPARATOR_PATTERN).splitToList(line);
            res.add(fromSplits.apply(splits));
        }
        return res;
    }

    public static String getPlainIOBLabel(String label) {
        return isBeginningOfIOBEntity(label) ? StringUtil.substring(label, 2) : label;
    }

    public static boolean isBeginningOfIOBEntity(String label) {
        return StringUtil.startsWith(label, TaggingLabels.IOB_START_ENTITY_LABEL_PREFIX)
                || StringUtil.startsWith(label, TaggingLabels.ENAMEX_START_ENTITY_LABEL_PREFIX);
    }

    // I-<citation> --> <citation>
    // <citation> --> <citation>
    public static String getPlainLabel(String label) {
        return isBeginningOfEntity(label) ? StringUtil.substring(label, 2) : label;
    }

    public static boolean isBeginningOfEntity(String label) {
        return StringUtils.startsWith(label, TaggingLabels.GROBID_START_ENTITY_LABEL_PREFIX)
                || StringUtil.startsWith(label, TaggingLabels.ENAMEX_START_ENTITY_LABEL_PREFIX);
    }
}
