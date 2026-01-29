package org.grobid.core.engines.tagging;

import com.google.common.base.Joiner;
import org.grobid.core.GrobidModel;
import org.grobid.core.jni.DeLFTModel;

import java.io.IOException;

public class DeLFTTagger implements GenericTagger {

    private final DeLFTModel delftModel;

    public DeLFTTagger(GrobidModel model) {
        delftModel = new DeLFTModel(model, null);
    }

    public DeLFTTagger(GrobidModel model, String architecture) {
        delftModel = new DeLFTModel(model, architecture);
    }

    @Override
    public String label(Iterable<String> data) {
        return label(Joiner.on('\n').join(data));
    }

    @Override
    public String label(String data) {
        return delftModel.label(data);
    }

    @Override
    public void close() throws IOException {
        delftModel.close();
    }
}
