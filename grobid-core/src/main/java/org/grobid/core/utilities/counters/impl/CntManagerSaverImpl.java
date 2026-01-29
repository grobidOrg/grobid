package org.grobid.core.utilities.counters.impl;

import org.grobid.core.utilities.counters.CntManager;
import org.grobid.core.utilities.counters.CntManagerSaver;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class CntManagerSaverImpl implements CntManagerSaver {
    public static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public CntManager deserialize(InputStream is) throws IOException {
        ObjectInputStream in = new ObjectInputStream(is);
        try {
            return (CntManager) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot deserialize counter because: " + e.getMessage(), e);
        }

    }

    @Override
    public void serialize(CntManager cntManager, OutputStream os) throws IOException {
        ObjectOutput out = new ObjectOutputStream(os);
        out.writeObject(cntManager);
    }
}
