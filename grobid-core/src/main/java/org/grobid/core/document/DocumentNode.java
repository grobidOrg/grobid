package org.grobid.core.document;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.grobid.core.layout.BoundingBox;

/**
 * Class corresponding to a node of the structure of a hierarchically organized document (i.e. for a table
 * of content).
 *
 */
public class DocumentNode {
    private Integer id = null;

    // Gorn address for tree structure
    private String address = null;
    // real numbering of the section, if any

    private String realNumber = null;
    // normalized numbering of the section, if any

    private String normalizedNumber = null;
    // the string attached to this document level, e.g. section title

    private String label = null;
    // list of child document nodes

    private List<DocumentNode> children = null;
    // offset relatively to the document tokenization (so token offset, NOT character offset)

    public int startToken = -1;
    public int endToken = -1;
    // coordinates of the string attached to this document level, typically where an index link
    // action point in the document

    private BoundingBox boundingBox = null;
    // parent document node, if null it is a root node

    private DocumentNode father = null;
    public DocumentNode() {
    }

    public DocumentNode(String label, String address) {
        this.label = label;
        this.address = address;
    }

    public String getRealNumber() {
        return realNumber;
    }

    public void setRealNumber(String number) {
        realNumber = number;
    }

    public String getNormalizedNumber() {
        return normalizedNumber;
    }

    public void setNormalizedNumber(String number) {
        normalizedNumber = number;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String theAddress) {
        address = theAddress;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String theLabel) {
        label = theLabel;
    }

    public List<DocumentNode> getChildren() {
        return children;
    }

    public void setChildren(List<DocumentNode> nodes) {
        children = nodes;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBox box) {
        boundingBox = box;
    }

    public DocumentNode getFather() {
        return father;
    }

    public void setFather(DocumentNode parent) {
        father = parent;
    }

    public void addChild(DocumentNode child) {
        if (this.children == null) {
            this.children = new ArrayList<DocumentNode>();
        }
        String addr = null;
        if (this.address != null) {
            if (this.address.equals("0")) {
                addr = "" + (this.children.size() + 1);
            } else {
                addr = this.address + (this.children.size() + 1);
            }
        }
        child.address = addr;
        child.father = this;
        if (child.endToken > this.endToken) {
            this.endToken = child.endToken;
        }

        this.children.add(child);
    }

    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .append("address", address)
                .append("label", label)
                .append("startToken", startToken)
                .append("endToken", endToken)
                .toString();
    }

    public String toString(int tab) {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(" ").append(address).append(" ").append(label).append(" ").append(startToken).append(" ").append(endToken).append("\n");

        if (children != null) {
            for (DocumentNode node : children) {
                for (int n = 0; n < tab + 1; n++) {
                    sb.append("\t");
                }
                sb.append(node.toString(tab + 1));
            }
        }
        return sb.toString();
    }

    public DocumentNode clone() {
        DocumentNode result = new DocumentNode();
        result.address = this.address;
        result.realNumber = this.realNumber;
        result.label = this.label;
        result.startToken = this.startToken;
        result.endToken = this.endToken;
        return result;
    }

    public DocumentNode getSpanningNode(int position) {
        if ((startToken <= position) && (endToken >= position)) {
            if (children != null) {
                for (DocumentNode node : children) {
                    if ((node.startToken <= position) && (node.endToken >= position)) {
                        return node.getSpanningNode(position);
                    }
                }
                return this;
            } else {
                return this;
            }
        } else {
            return null;
        }
    }


    /*public DocumentNode nextSlibing() {
         if ( (children != null) && (children.size() > 0) ) {
             return children.get(0);
         }
         else if (father == null) {
             return null;
         }
         else {
             for (DocumentNode node : father.children) {

             }
         }
     }*/
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}

