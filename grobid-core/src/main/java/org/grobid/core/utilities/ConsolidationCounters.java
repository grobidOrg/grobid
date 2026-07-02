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

import org.grobid.core.engines.counters.*;

/**
 * Counters for keeping track of consolidation activity and results
 *
 */
public class ConsolidationCounters {
    public static final Countable CONSOLIDATION = new Countable() {
        @Override
        public String getName() {
            return "CONSOLIDATION";
        }
    };
    public static final Countable CONSOLIDATION_SUCCESS = new Countable() {
        @Override
        public String getName() {
            return "CONSOLIDATION_SUCCESS";
        }
    };
    public static final Countable CONSOLIDATION_PER_DOI = new Countable() {
        @Override
        public String getName() {
            return "CONSOLIDATION_PER_DOI";
        }
    };
    public static final Countable CONSOLIDATION_PER_DOI_SUCCESS = new Countable() {
        @Override
        public String getName() {
            return "CONSOLIDATION_PER_DOI_SUCCESS";
        }
    };
    public static final Countable TOTAL_BIB_REF = new Countable() {
        @Override
        public String getName() {
            return "TOTAL_BIB_REF";
        }
    };
}
