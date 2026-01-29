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
