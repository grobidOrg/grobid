package org.grobid.core.utilities.counters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface CntManagerSaver {
    CntManager deserialize(InputStream is) throws IOException;

    void serialize(CntManager cntManager, OutputStream os) throws IOException;
}
