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
package org.grobid.core.utilities.counters.impl;

import java.util.Map;

import org.grobid.core.engines.counters.Countable;
import org.grobid.core.utilities.counters.CntManager;
import org.grobid.core.utilities.counters.CntsMetric;
import org.grobid.core.utilities.counters.Counter;

class NoOpCntManagerImpl implements CntManager {
    @Override
    public void i(Countable e) {

    }

    @Override
    public void i(Countable e, long val) {

    }

    @Override
    public void i(String group, String name) {

    }

    @Override
    public void i(String group, String name, long val) {

    }

    @Override
    public long cnt(Countable e) {
        return 0;
    }

    @Override
    public long cnt(String group, String name) {
        return 0;
    }

    @Override
    public Counter getCounter(Countable e) {
        return null;
    }

    @Override
    public Counter getCounter(String group, String name) {
        return null;
    }

    @Override
    public Map<String, Long> getCounters(Class<? extends Countable> enumClass) {
        return null;
    }

    @Override
    public Map<String, Long> getCounters(String group) {
        return null;
    }

    @Override
    public Map<String, Map<String, Long>> getAllCounters() {
        return null;
    }

    @Override
    public Map<String, Long> flattenAllCounters(String separator) {
        return null;
    }

    @Override
    public void addMetric(String name, CntsMetric cntsMetric) {

    }

    @Override
    public void removeMetric(String name) {

    }
}
