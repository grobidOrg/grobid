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

import com.google.common.base.Joiner;

import org.grobid.core.GrobidModel;
import org.grobid.core.jni.DeLFTModel;

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
