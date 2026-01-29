package org.grobid.core.document.xml;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class NodeChildrenIterator implements Iterable<Node>, Iterator<Node> {
    private static class BlacklistElementNamePredicate implements Predicate<Node> {
        private Set<String> blacklistElements = new HashSet<>();

        public BlacklistElementNamePredicate(String... elementNames) {
            this.blacklistElements.addAll(Arrays.asList(elementNames));
        }

        @Override
        public boolean apply(Node input) {
            return (input instanceof Text && !blacklistElements.contains("text"))
                    || (input instanceof Element && !blacklistElements.contains(((Element) input).getLocalName()));
        }
    }

    private Node node;
    private int cur;

    private NodeChildrenIterator(Node node) {
        this.node = node;
        cur = 0;
    }

    @Override
    public Iterator<Node> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return node != null && cur < node.getChildCount();
    }

    @Override
    public Node next() {
        if (node == null || cur == node.getChildCount()) {
            throw new NoSuchElementException();
        }

        return node.getChild(cur++);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public static Iterable<Node> get(Node parent) {
        return new NodeChildrenIterator(parent);
    }

    public static Iterable<Node> get(Node parent, String... blacklistedNodes) {
        return Iterables.filter(get(parent), new BlacklistElementNamePredicate(blacklistedNodes));
    }

}
