package org.grobid.core.engines.counters;

public class ReferenceMarkerMatcherCounters {

    public static final Countable MATCHED_REF_MARKERS = new Countable() {
        @Override
        public String getName() {
            return "MATCHED_REF_MARKERS";
        }
    };

    public static final Countable UNMATCHED_REF_MARKERS = new Countable() {
        @Override
        public String getName() {
            return "UNMATCHED_REF_MARKERS";
        }
    };

    public static final Countable NO_CANDIDATES = new Countable() {
        @Override
        public String getName() {
            return "NO_CANDIDATES";
        }
    };

    public static final Countable MANY_CANDIDATES = new Countable() {
        @Override
        public String getName() {
            return "MANY_CANDIDATES";
        }
    };

    public static final Countable STYLE_AUTHORS = new Countable() {
        @Override
        public String getName() {
            return "STYLE_AUTHORS";
        }
    };

    public static final Countable STYLE_NUMBERED = new Countable() {
        @Override
        public String getName() {
            return "STYLE_NUMBERED";
        }
    };

    public static final Countable MATCHED_REF_MARKERS_AFTER_POST_FILTERING = new Countable() {
        @Override
        public String getName() {
            return "MATCHED_REF_MARKERS_AFTER_POST_FILTERING";
        }
    };

    public static final Countable MANY_CANDIDATES_AFTER_POST_FILTERING = new Countable() {
        @Override
        public String getName() {
            return "MANY_CANDIDATES_AFTER_POST_FILTERING";
        }
    };

    public static final Countable NO_CANDIDATES_AFTER_POST_FILTERING = new Countable() {
        @Override
        public String getName() {
            return "NO_CANDIDATES_AFTER_POST_FILTERING";
        }
    };

    public static final Countable STYLE_OTHER = new Countable() {
        @Override
        public String getName() {
            return "STYLE_OTHER";
        }
    };

    public static final Countable INPUT_REF_STRINGS_CNT = new Countable() {
        @Override
        public String getName() {
            return "INPUT_REF_STRINGS_CNT";
        }
    };
}
