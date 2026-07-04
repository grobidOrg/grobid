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

public class FigureCounters {
    public static final Countable TOO_MANY_FIGURES_PER_PAGE = new Countable() {
        @Override
        public String getName() {
            return "TOO_MANY_FIGURES_PER_PAGE";
        }
    };
    public static final Countable STANDALONE_FIGURES = new Countable() {
        @Override
        public String getName() {
            return "STANDALONE_FIGURES";
        }
    };

    public static final Countable SKIPPED_BAD_STANDALONE_FIGURES = new Countable() {
        @Override
        public String getName() {
            return "SKIPPED_BAD_STANDALONE_FIGURES";
        }
    };

    public static final Countable SKIPPED_SMALL_STANDALONE_FIGURES = new Countable() {
        @Override
        public String getName() {
            return "SKIPPED_SMALL_STANDALONE_FIGURES";
        }
    };
    public static final Countable SKIPPED_BIG_STANDALONE_FIGURES = new Countable() {
        @Override
        public String getName() {
            return "SKIPPED_BIG_STANDALONE_FIGURES";
        }
    };

    public static final Countable ASSIGNED_GRAPHICS_TO_FIGURES = new Countable() {
        @Override
        public String getName() {
            return "ASSIGNED_GRAPHICS_TO_FIGURES";
        }
    };

    public static final Countable SKIPPED_DUE_TO_MISMATCH_OF_CAPTIONS_AND_VECTOR_AND_BITMAP_GRAPHICS = new Countable() {
        @Override
        public String getName() {
            return "SKIPPED_DUE_TO_MISMATCH_OF_CAPTIONS_AND_VECTOR_AND_BITMAP_GRAPHICS";
        }
    };
}
