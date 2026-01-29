package org.grobid.core.data;

import nu.xom.Element;
import org.grobid.core.utilities.OffsetPosition;

/**
 * This class represent an annotation in an XML node. The annotation is composed by two information: the XML Element
 * node and the offset position
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
