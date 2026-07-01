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
package org.grobid.core.data;

import nu.xom.Element;

import org.grobid.core.utilities.OffsetPosition;

/**
 * This class represent an annotation in an XML node.
 * The annotation is composed by two information: the XML Element node and the offset position
 */
public class AnnotatedXMLElement {

    private OffsetPosition offsetPosition;
    private Element annotationNode;

    public AnnotatedXMLElement(Element annotationNode, OffsetPosition offsetPosition) {
        this.annotationNode = annotationNode;
        this.offsetPosition = offsetPosition;
    }

    public OffsetPosition getOffsetPosition() {
        return offsetPosition;
    }

    public void setOffsetPosition(OffsetPosition offsetPosition) {
        this.offsetPosition = offsetPosition;
    }

    public Element getAnnotationNode() {
        return annotationNode;
    }

    public void setAnnotationNode(Element annotationNode) {
        this.annotationNode = annotationNode;
    }
}
