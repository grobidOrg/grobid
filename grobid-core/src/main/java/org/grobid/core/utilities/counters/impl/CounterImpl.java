package org.grobid.core.utilities.counters.impl;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.grobid.core.utilities.counters.Counter;

import java.util.concurrent.atomic.AtomicLong;

class CounterImpl implements Counter {
    private static final long serialVersionUID = 4764636620333386314L;
    private volatile AtomicLong cnt = new AtomicLong(0);

    public CounterImpl() {
    }

    public CounterImpl(long cnt) {
        this.cnt.set(cnt);
    }

    @Override
    public void i() {
        cnt.incrementAndGet();
    }

    @Override
    public void i(long val) {
        cnt.addAndGet(val);
    }

    @Override
    public long cnt() {
        return cnt.longValue();
    }

    @Override
    public void set(long val) {
        cnt.set(val);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CounterImpl)) return false;
        CounterImpl counter = (CounterImpl) o;
        return new EqualsBuilder()
                .append(cnt.get(), counter.cnt.get())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(cnt.get())
                .toHashCode();
    }
}
