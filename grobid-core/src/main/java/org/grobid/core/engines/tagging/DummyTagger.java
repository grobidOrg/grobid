package org.grobid.core.engines.tagging;

import com.google.common.base.Joiner;
import org.grobid.core.GrobidModel;
import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This tagger just return one label <dummy>
 */
public class DummyTagger implements GenericTagger {

    public static final String DUMMY_LABEL = "<dummy>";

    public DummyTagger(GrobidModel model) {
        if (!model.equals(GrobidModels.DUMMY)) {
            throw new GrobidException(
                    "Cannot use a non-dummy model with the dummy tagger. All dummies or no dummies. ");
        }
    }

    @Override
    public String label(Iterable<String> data) {
        final List<String> output = new ArrayList<>();
        data.forEach(d -> output.add(d + "\t" + DUMMY_LABEL));
        return Joiner.on('\n').join(output);
    }

    @Override
    public String label(String data) {
        return "<dummy>";
    }

    @Override
    public void close() throws IOException {

    }
}
