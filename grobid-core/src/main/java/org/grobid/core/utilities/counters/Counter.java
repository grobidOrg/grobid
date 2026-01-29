package org.grobid.core.utilities.counters;

import java.io.Serializable;

public interface Counter extends Serializable {
    void i();

    void i(long val);

    long cnt();

    void set(long val);
}
