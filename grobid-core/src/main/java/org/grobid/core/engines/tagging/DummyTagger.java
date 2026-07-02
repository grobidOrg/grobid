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
package org.grobid.core.engines.tagging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;

import org.grobid.core.GrobidModel;
import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;

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
