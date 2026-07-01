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
package org.grobid.core.engines.counters;

public class CitationParserCounters {
    public static final Countable SEGMENTED_REFERENCES = new Countable() {
        @Override
        public String getName() {
            return "SEGMENTED_REFERENCES";
        }
    };
    public static final Countable NULL_SEGMENTED_REFERENCES_LIST = new Countable() {
        @Override
        public String getName() {
            return "NULL_SEGMENTED_REFERENCES_LIST";
        }
    };
    public static final Countable EMPTY_REFERENCES_BLOCKS = new Countable() {
        @Override
        public String getName() {
            return "EMPTY_REFERENCES_BLOCKS";
        }
    };
    public static final Countable NOT_EMPTY_REFERENCES_BLOCKS = new Countable() {
        @Override
        public String getName() {
            return "NOT_EMPTY_REFERENCES_BLOCKS";
        }
    };
}
