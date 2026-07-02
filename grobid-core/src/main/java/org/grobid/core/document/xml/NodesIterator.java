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
package org.grobid.core.document.xml;

import java.util.Iterator;
import java.util.NoSuchElementException;

import nu.xom.Node;
import nu.xom.Nodes;

public class NodesIterator implements Iterable<Node>, Iterator<Node> {
    private Nodes nodes;
    private int cur;

    public NodesIterator(Nodes nodes) {
        this.nodes = nodes;
        cur = 0;
    }

    @Override
    public Iterator<Node> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return cur < nodes.size();
    }

    @Override
    public Node next() {
        if (cur == nodes.size()) {
            throw new NoSuchElementException();
        }

        return nodes.get(cur++);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
