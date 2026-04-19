package org.grobid.core.utilities.counters.impl;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.grobid.core.engines.counters.Countable;
import org.grobid.core.utilities.counters.CntManager;
import org.grobid.core.utilities.counters.CntsMetric;
import org.grobid.core.utilities.counters.Counter;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class CntManagerImpl implements CntManager {
    private static final long serialVersionUID = 2305126306757162275L;

    private ConcurrentMap<String, ConcurrentMap<String, Counter>> classCounters = new ConcurrentHashMap<>();
    private ConcurrentMap<String, ConcurrentMap<String, Counter>> strCnts = new ConcurrentHashMap<>();
    transient private ConcurrentMap<String, CntsMetric> metrics = null;

    private void checkGroupName(String groupName) {
        if (classCounters.containsKey(groupName)) {
            throw new IllegalStateException("Group name " + groupName + " coincides with the enum type counter name");
        }
    }

    private void checkClass(String class1) {
        if (strCnts.containsKey(class1)) {
            throw new IllegalStateException("Enum class name " + class1 + " coincides with the string type counter name");
        }
    }

    @Override
    public void i(Countable e) {
        i(e, 1);
    }

    @Override
    public void i(Countable e, long val) {
        final String groupName = getCounterEnclosingName(e);
        checkClass(groupName);

        classCounters.putIfAbsent(groupName, new ConcurrentHashMap<String, Counter>());
        ConcurrentMap<String, Counter> cntMap = classCounters.get(groupName);

        cntMap.putIfAbsent(e.getName(), new CounterImpl());
        Counter cnt = cntMap.get(e.getName());
        cnt.i(val);
    }

    @Override
    public void i(String group, String name) {
        i(group, name, 1);
    }

    @Override
    public void i(String group, String name, long val) {
        checkGroupName(group);

        strCnts.putIfAbsent(group, new ConcurrentHashMap<String, Counter>());
        ConcurrentMap<String, Counter> cntMap = strCnts.get(group);

        cntMap.putIfAbsent(name, new CounterImpl());
        Counter cnt = cntMap.get(name);

        cnt.i(val);
    }

    @Override
    public long cnt(Countable e) {
        Map<String, Counter> cntMap = classCounters.get(getCounterEnclosingName(e));
        if (cntMap == null) {
            return 0;
        }
        Counter cnt = cntMap.get(e.getName());
        return cnt == null ? 0 : cnt.cnt();
    }

    @Override
    public long cnt(String group, String name) {
        Map<String, Counter> cntMap = strCnts.get(group);
        if (cntMap == null) {
            return 0;
        }
        Counter cnt = cntMap.get(name);
        return cnt == null ? 0 : cnt.cnt();
    }

    @Override
    public Counter getCounter(Countable e) {
        checkClass(e.getName());
        classCounters.putIfAbsent(e.getName(), new ConcurrentHashMap<String, Counter>());

        ConcurrentMap<String, Counter> cntMap = classCounters.get(e.getClass().getName());
        cntMap.putIfAbsent(e.getName(), new CounterImpl());
        return cntMap.get(e.getName());
    }

    @Override
    public Counter getCounter(String group, String name) {
        checkGroupName(group);
        strCnts.putIfAbsent(group, new ConcurrentHashMap<String, Counter>());
        ConcurrentMap<String, Counter> cntMap = strCnts.get(group);
        cntMap.putIfAbsent(name, new CounterImpl());

        return cntMap.get(name);
    }

    @Override
    public Map<String, Long> getCounters(Class<? extends Countable> countableClass) {
        Map<String, Long> toReturn = new ConcurrentHashMap<>();
        final ConcurrentMap<String, Counter> stringCounterConcurrentMap = classCounters.get(countableClass.getName());
        for (String key : stringCounterConcurrentMap.keySet()) {
            toReturn.put(key, stringCounterConcurrentMap.get(key).cnt());
        }
        return toReturn;
    }

    @Override
    public Map<String, Long> getCounters(String group) {
        Map<String, Long> toReturn = new ConcurrentHashMap<>();
        if (strCnts.containsKey(group)) {
            for (Map.Entry<String, Counter> e : strCnts.get(group).entrySet()) {
                toReturn.put(e.getKey(), e.getValue().cnt());
            }
        }
        return toReturn;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public Map<String, Map<String, Long>> getAllCounters() {
        Map<String, Map<String, Long>> map = new ConcurrentHashMap<>();
        for (String e : classCounters.keySet()) {
            try {
                map.put(e, getCounters((Class<? extends Countable>) Class.forName(e)));
            } catch (ClassNotFoundException e1) {
                throw new IllegalStateException(e1);
            }
        }

        for (String e : strCnts.keySet()) {
            map.put(e, getCounters(e));
        }

        return map;
    }

    @Override
    public Map<String, Long> flattenAllCounters(String separator) {
        Map<String, Long> map = new HashMap<>();
        for (Map.Entry<String, Map<String, Long>> group : getAllCounters().entrySet()) {
            for (Map.Entry<String, Long> e : group.getValue().entrySet()) {
                map.put(group.getKey() + separator + e.getKey(), e.getValue());
            }
        }
        return map;
    }

    @Override
    public synchronized void addMetric(String name, CntsMetric cntsMetric) {
        if (metrics == null) {
            metrics = new ConcurrentHashMap<>();
        }
        metrics.put(name, cntsMetric);
    }

    @Override
    public synchronized void removeMetric(String name) {
        if (metrics == null) {
            metrics = new ConcurrentHashMap<>();
        }
        metrics.remove(name);
    }

    @Override
    public synchronized String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("counters", getAllCounters())
                .append("metrics", materializeMetrics())
                .toString();
    }

    private Map<String, String> materializeMetrics() {
        if (metrics == null || metrics.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, CntsMetric> e : metrics.entrySet()) {
            out.put(e.getKey(), e.getValue().getMetricString(this));
        }
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CntManagerImpl)) return false;
        CntManagerImpl that = (CntManagerImpl) o;
        return new EqualsBuilder()
                .append(classCounters, that.classCounters)
                .append(strCnts, that.strCnts)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(classCounters)
                .append(strCnts)
                .toHashCode();
    }

    protected String getCounterEnclosingName(Countable e) {
        if (e.getClass() != null && e.getClass().getEnclosingClass() != null) {
            return e.getClass().getEnclosingClass().getName();
        } else {
            return e.getClass().getName();
        }
    }
}
