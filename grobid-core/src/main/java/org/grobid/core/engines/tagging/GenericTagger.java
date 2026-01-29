package org.grobid.core.engines.tagging;

import java.io.Closeable;

public interface GenericTagger extends Closeable {
    String label(Iterable<String> data);

    String label(String data);
}
