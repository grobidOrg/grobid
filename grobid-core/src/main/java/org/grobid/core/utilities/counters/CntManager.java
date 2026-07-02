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
package org.grobid.core.utilities.counters;

import java.io.Serializable;
import java.util.Map;

import org.grobid.core.engines.counters.Countable;

public interface CntManager extends Serializable {
    void i(Countable e);

    void i(Countable e, long val);

    void i(String group, String name);

    void i(String group, String name, long val);

    long cnt(Countable e);

    long cnt(String group, String name);

    Counter getCounter(Countable e);

    Counter getCounter(String group, String name);

    Map<String, Long> getCounters(Class<? extends Countable> enumClass);

    Map<String, Long> getCounters(String group);

    Map<String, Map<String, Long>> getAllCounters();

    Map<String, Long> flattenAllCounters(String separator);

    void addMetric(String name, CntsMetric cntsMetric);

    void removeMetric(String name);
}
