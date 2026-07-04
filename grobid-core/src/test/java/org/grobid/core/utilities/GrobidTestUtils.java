/*
 * Copyright 2008-2026 GROBID contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grobid.core.utilities;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;

public class GrobidTestUtils {

    /**
     * Set a private instance field via reflection (test-only helper replacing
     * PowerMock's Whitebox.setInternalState). Walks up the class hierarchy so
     * inherited fields are also reachable.
     */
    public static void setField(Object target, String fieldName, Object value) {
        setField(target.getClass(), target, fieldName, value);
    }

    /**
     * Set a private static field via reflection (test-only helper replacing
     * PowerMock's Whitebox.setInternalState).
     */
    public static void setStaticField(Class<?> clazz, String fieldName, Object value) {
        setField(clazz, null, fieldName, value);
    }

    private static void setField(Class<?> clazz, Object target, String fieldName, Object value) {
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                // try the superclass
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unable to set field '" + fieldName + "' on " + clazz, e);
            }
        }
        throw new RuntimeException("No field '" + fieldName + "' found on " + clazz);
    }

    public static String getWapitiResult(List<String> features, List<Triple<String, Integer, Integer>> labels) {
        return getWapitiResult(features, labels, " ");
    }

    /**
     * Utility method to generate a hypothetical result from wapiti.
     * Useful for testing the extraction of the sequence labeling.
     *
     * @param labels label maps. A list of Triples, containing label (left), start_index (middle) and end_index exclusive (right)
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
