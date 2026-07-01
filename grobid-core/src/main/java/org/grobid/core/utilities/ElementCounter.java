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

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ElementCounter<T> implements Serializable {

    private static final long serialVersionUID = 7859247056683063608L;
    private Map<T, Integer> cnts = new LinkedHashMap<>();

    public ElementCounter() {
    }

    public ElementCounter(Map<T, Integer> cnts) {
        this.cnts = cnts;
    }

    public void i(T obj) {
        i(obj, 1);
    }

    public void i(T obj, int val) {
        if (cnts.containsKey(obj)) {
            cnts.put(obj, cnts.get(obj) + val);
        } else {
            cnts.put(obj, val);
        }
    }

    public int cnt(T obj) {
        if (cnts.containsKey(obj)) {
            return cnts.get(obj);
        } else {
            return 0;
        }
    }

    public Map<T, Integer> getCnts() {
        return cnts;
    }

    //Jackson
    public void setCountItems(List<ElementCounterItem<T>> items) {
        for (ElementCounterItem<T> i : items) {
            cnts.put(i.getItem(), i.getCnt());
        }
    }

    public List<Map.Entry<T, Integer>> getSortedCounts() {
        List<Map.Entry<T, Integer>> list = Lists.newArrayList(cnts.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<T, Integer>>() {
            @Override
            public int compare(Map.Entry<T, Integer> o1, Map.Entry<T, Integer> o2) {
                return -o1.getValue().compareTo(o2.getValue());
            }
        });
        return list;
    }

    public int size() {
        return cnts.size();
    }

    public List<ElementCounterItem<T>> getCountItems() {
        return Lists.newArrayList(
                Iterables.transform(getCnts().entrySet(), new Function<Map.Entry<T, Integer>, ElementCounterItem<T>>() {
                    @Override
                    public ElementCounterItem<T> apply(Map.Entry<T, Integer> input) {
                        return new ElementCounterItem<T>(input.getKey(), input.getValue());
                    }
                }));
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("cnts", cnts)
                .toString();
    }
}
