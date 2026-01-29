package org.grobid.core.engines.citations;

import java.util.List;
import org.grobid.core.document.Document;

public interface ReferenceSegmenter {
    List<LabeledReferenceResult> extract(String referenceBlock);

    List<LabeledReferenceResult> extract(Document document);
}
