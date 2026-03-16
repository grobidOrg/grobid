package org.grobid.core.utilities;

import com.google.common.base.Function;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class Triple<A, B, C> {

    private final A a;
    private final B b;
    private final C c;

    public Triple(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public Function getAFunction = new Function<Triple<A, B, C>, A>() {
        @Override
        public A apply(Triple<A, B, C> input) {
            return input.getA();
        }
    };

    public Function getBFunction = new Function<Triple<A, B, C>, B>() {
        @Override
        public B apply(Triple<A, B, C> input) {
            return input.getB();
        }
    };

    public Function getCFunction = new Function<Triple<A, B, C>, C>() {
        @Override
        public C apply(Triple<A, B, C> input) {
            return input.getC();
        }
    };

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("a", a)
                .append("b", b)
                .append("c", c)
                .toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(a)
                .append(b)
                .append(c)
                .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Triple)) return false;
        Triple<?, ?, ?> that = (Triple<?, ?, ?>) o;
        return new EqualsBuilder()
                .append(a, that.a)
                .append(b, that.b)
                .append(c, that.c)
                .isEquals();
    }

	public A getA() {
		return a;
	}

	public B getB() {
		return b;
	}
	
	public C getC() {
		return c;
	}

}
