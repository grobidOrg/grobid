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
package org.grobid.core.document;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import org.grobid.core.layout.BoundingBox;
import org.grobid.core.utilities.TextUtilities;

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
        sb.append(id)
                .append(" ")
                .append(address)
                .append(" ")
                .append(label)
                .append(" ")
                .append(startToken)
                .append(" ")
                .append(endToken)
                .append("\n");

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

    // similarity threshold above which an outline label is considered to match a section head
    private static final double NODE_MATCH_THRESHOLD = 0.90;

    /**
     * Normalize an outline label or a section head for soft comparison. Outline labels extracted
     * from the PDF bookmarks carry non-breaking spaces, soft hyphens, '|' number separators and
     * multi-line indentation runs, none of which are meaningful for matching.
     */
    private static String normalizeForMatching(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value
                .replace('\u00A0', ' ')  // non-breaking space
                .replace("\u00AD", "")   // soft hyphen
                .replace('|', ' ');
        return StringUtils.normalizeSpace(normalized);
    }

    /**
     * Given the label of a potential node (typically a section head), find it in the hierarchy
     * rooted at {@code rootNode} using soft string matching and return its depth.
     *
     * @param rootNode the node to search from
     * @param label    the section head text to look up
     * @param depth    the depth of {@code rootNode} in the hierarchy (0 for the outline root)
     * @return the depth of the first matching node, or -1 if no node matches
     */
    public static int findNodeDepth(DocumentNode rootNode, String label, int depth) {
        DocumentNode node = findNode(rootNode, label);
        if (node == null) {
            return -1;
        }
        int d = depth;
        DocumentNode cursor = node;
        while (cursor != rootNode && cursor.getFather() != null) {
            d++;
            cursor = cursor.getFather();
        }
        return d;
    }

    /**
     * Find the first node in the hierarchy rooted at {@code rootNode} whose label softly matches
     * {@code label} (depth-first). Returns the matching node itself, or null if none matches.
     */
    public static DocumentNode findNode(DocumentNode rootNode, String label) {
        if (rootNode == null || label == null) {
            return null;
        }
        String normalizedLabel = normalizeForMatching(label);
        String nodeLabel = normalizeForMatching(rootNode.getLabel());
        if (StringUtils.isNotBlank(nodeLabel)) {
            double score = TextUtilities.getRatcliffObershelpSimilarity(nodeLabel, normalizedLabel, false);
            if (score >= NODE_MATCH_THRESHOLD) {
                return rootNode;
            }
        }
        if (rootNode.getChildren() != null) {
            for (DocumentNode child : rootNode.getChildren()) {
                DocumentNode found = findNode(child, label);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Whether {@code node} is a strict descendant of {@code ancestor} in the outline tree, walking
     * the father chain. Used to decide whether a sub-head belongs under the currently open section:
     * if its outline parent heading was missed by the sequence labeller, the sub-head is not a
     * descendant of the open section and must start a new div rather than fold into it.
     */
    public static boolean isDescendantOf(DocumentNode node, DocumentNode ancestor) {
        if (node == null || ancestor == null) {
            return false;
        }
        DocumentNode cursor = node.getFather();
        while (cursor != null) {
            if (cursor == ancestor) {
                return true;
            }
            cursor = cursor.getFather();
        }
        return false;
    }
}
