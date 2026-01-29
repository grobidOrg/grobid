package org.grobid.core.utilities.counters.impl;

import org.grobid.core.utilities.counters.Counter;

class NoOpCounterImpl implements Counter {
    private static final long serialVersionUID = -6891249458789932892L;

    @Override
    public void i() {

    }

    @Override
    public void i(long val) {
    }

    @Override
    public long cnt() {
        return 0;
    }

    @Override
    public void set(long val) {
    }
}
