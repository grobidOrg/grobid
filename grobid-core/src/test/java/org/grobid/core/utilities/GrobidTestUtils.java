package org.grobid.core.utilities;

import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class GrobidTestUtils {

    public static String getWapitiResult(List<String> features, List<Triple<String, Integer, Integer>> labels) {
        return getWapitiResult(features, labels, " ");
    }

    /**
     * Utility method to generate a hypothetical result from wapiti. Useful for testing the extraction of the sequence
     * labeling.
     *
     * @param labels
     * label maps. A list of Triples, containing label (left), start_index (middle) and end_index exclusive (right)
     * @return a string containing the resulting features + labels returned by wapiti
     */
    public static String getWapitiResult(
            List<String> features,
            List<Triple<String, Integer, Integer>> labels,
            String separator) {

        List<String> labeled = new ArrayList<>();
        int idx = 0;

        for (Triple<String, Integer, Integer> label : labels) {

            if (idx < label.getMiddle()) {
                for (int i = idx; i < label.getMiddle(); i++) {
                    labeled.add("<other>");
                    idx++;
                }
            }

            for (int i = label.getMiddle(); i < label.getRight(); i++) {
                labeled.add(label.getLeft());
                idx++;
            }
        }

        if (idx < features.size()) {
            for (int i = idx; i < features.size(); i++) {
                labeled.add("<other>");
                idx++;
            }
        }

        assertThat(features, hasSize(labeled.size()));

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < features.size(); i++) {
            if (features.get(i) == null || features.get(i).startsWith(" ")) {
                continue;
            }
            sb.append(features.get(i)).append(separator).append(labeled.get(i)).append("\n");
        }

        return sb.toString();
    }
}
