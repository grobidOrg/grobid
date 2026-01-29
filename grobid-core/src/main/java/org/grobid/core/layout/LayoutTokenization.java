package org.grobid.core.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for representing a tokenization of document section where tokens include layout attributes. Once built, it is
 * possible to iterate through the string tokens only ignoring the layout information or through the layout token
 * objects.
 *
 */
public class LayoutTokenization {
    // private List<LayoutToken> layoutTokenization = null;
    private List<LayoutToken> tokenization = null; // this should ultimately be removed

    public LayoutTokenization() {
        // layoutTokenization = layoutTokens;
        tokenization = new ArrayList<LayoutToken>();
    }

    public LayoutTokenization(List<LayoutToken> tokens) {
        // layoutTokenization = layoutTokens;
        tokenization = tokens;
    }

    // public List<LayoutToken> getLayoutTokens() {
    // return layoutTokenization;
    // }

    public List<LayoutToken> getTokenization() {
        return tokenization;
    }

    // public void addLayoutToken(LayoutToken token) {
    // if (layoutTokenization == null)
    // layoutTokenization = new ArrayList<LayoutToken>();
    // else
    // layoutTokenization.add(token);
    // }

    // public void setLayoutTokens(List<LayoutToken> layoutTokens) {
    // this.layoutTokenization = layoutTokens;
    // }

    public void addToken(LayoutToken token) {
        if (tokenization == null)
            tokenization = new ArrayList<LayoutToken>();
        else
            tokenization.add(token);
    }

    public void addTokens(List<LayoutToken> tokens) {
        if (tokenization == null)
            tokenization = new ArrayList<LayoutToken>();
        else {
            tokenization.addAll(tokens);
        }
    }

    public void setTokenization(List<LayoutToken> tokens) {
        tokenization = tokens;
    }

    public int size() {
        if (tokenization == null)
            return 0;
        else
            return tokenization.size();
    }
}
