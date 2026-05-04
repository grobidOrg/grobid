package org.grobid.core.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import org.apache.commons.lang3.StringUtils;

import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;

/**
 * Dealing with layout tokens
 */
public class LayoutTokensUtil {

    public static final Function<LayoutToken, String> TO_TEXT_FUNCTION = new Function<LayoutToken, String>() {
        @Override
        public String apply(LayoutToken layoutToken) {
            return layoutToken.t();
        }
    };

    public static List<LayoutToken> enrichWithNewLineInfo(List<LayoutToken> toks) {
        PeekingIterator<LayoutToken> tokens = Iterators.peekingIterator(toks.iterator());
        while (tokens.hasNext()) {
            LayoutToken curToken = tokens.next();
            if (tokens.hasNext() && tokens.peek().getText().equals("\n")) {
                curToken.setNewLineAfter(true);
            }
            if (curToken.getText().equals("\n")) {
                curToken.setText(" ");
            }
        }
        return toks;
    }

    public static String normalizeText(String text) {
        //return TextUtilities.dehyphenize(text).replace("\n", " ").replaceAll("[ ]{2,}", " ");
        return StringUtils.normalizeSpace(text.replace("\n", " "));
    }

    public static String normalizeText(List<LayoutToken> tokens) {
        //return TextUtilities.dehyphenize(toText(tokens)).replace("\n", " ").replaceAll("[ ]{2,}", " ");
        return StringUtils.normalizeSpace(toText(tokens).replace("\n", " "));
    }

    public static String normalizeDehyphenizeText(List<LayoutToken> tokens) {
        return StringUtils
                .normalizeSpace(LayoutTokensUtil.toText(LayoutTokensUtil.dehyphenize(tokens)).replace("\n", " "));
    }

    public static String toText(List<LayoutToken> tokens) {
        return Joiner.on("").join(Iterables.transform(tokens, TO_TEXT_FUNCTION));
    }

    public static boolean noCoords(LayoutToken t) {
        return t.getPage() == -1 || t.getWidth() <= 0;
    }

    public static boolean spaceyToken(String tok) {
        /*return (tok.equals(" ")
                || tok.equals("\u00A0")
                || tok.equals("\n"));*/
        // all space characters are normalised into simple space character
        return tok.equals(" ");
    }

    public static boolean newLineToken(String tok) {
        //return (tok.equals("\n") || tok.equals("\r") || tok.equals("\n\r"));
        // all new line characters are normalised into simple \n character
        return tok.equals("\n");
    }

    /*public static String removeSpecialVariables(String tok) {
        if (tok.equals("@BULLET")) {
            tok = "•";
        }
        return tok;
    }*/

    public static boolean containsToken(List<LayoutToken> toks, String text) {
        for (LayoutToken t : toks) {
            if (text.equals(t.t())) {
                return true;
            }
        }
        return false;
    }

    public static int tokenPos(List<LayoutToken> toks, String text) {
        int cnt = 0;
        for (LayoutToken t : toks) {
            if (text.equals(t.t())) {
                return cnt;
            }
            cnt++;
        }
        return -1;
    }

    public static int tokenPos(List<LayoutToken> toks, Pattern p) {
        int cnt = 0;
        for (LayoutToken t : toks) {
            if (p.matcher(t.t()).matches()) {
                return cnt;
            }
            cnt++;
        }
        return -1;
    }

    //    public static List<List<LayoutToken>> split(List<LayoutToken> toks, Pattern p) {
    //        return split(toks, p, false);
    //    }

    public static List<List<LayoutToken>> split(List<LayoutToken> toks, Pattern p, boolean preserveSeparator) {
        return split(toks, p, preserveSeparator, true);
    }

    public static List<List<LayoutToken>> split(
            List<LayoutToken> toks,
            Pattern p,
            boolean preserveSeparator,
            boolean preserveLeftOvers) {
        List<List<LayoutToken>> split = new ArrayList<>();
        List<LayoutToken> curToks = new ArrayList<>();
        for (LayoutToken tok : toks) {
            if (p.matcher(tok.t()).matches()) {
                if (preserveSeparator) {
                    curToks.add(tok);
                }
                split.add(curToks);
                curToks = new ArrayList<>();
            } else {
                curToks.add(tok);
            }
        }
        if (preserveLeftOvers) {
            if (!curToks.isEmpty()) {
                split.add(curToks);
            }
        }
        return split;
    }

    public static boolean tooFarAwayVertically(List<BoundingBox> boxes, double distance) {
        if (boxes == null) {
            return false;
        }
        for (int i = 0; i < boxes.size() - 1; i++) {
            if (boxes.get(i).verticalDistanceTo(boxes.get(i + 1)) > distance) {
                return true;
            }
        }
        return false;
    }

    public static String getCoordsString(List<LayoutToken> toks) {
        List<BoundingBox> res = BoundingBoxCalculator.calculate(toks);
        return Joiner.on(";").join(res);
    }

    public static String getCoordsStringForOneBox(List<LayoutToken> toks) {
        BoundingBox res = BoundingBoxCalculator.calculateOneBox(toks, true);
        if (res == null) {
            return null;
        }
        return res.toString();
    }

    public static List<LayoutToken> dehyphenize(List<LayoutToken> tokens) {
        List<LayoutToken> output = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            LayoutToken currentToken = tokens.get(i);
            //the current token is dash (and is neither subscript nor superscript) checking what's around
            if (currentToken.getText().equals("-") && !(currentToken.isSubscript() || currentToken.isSuperscript())) {
                if (doesRequireDehypenisation(tokens, i)) {
                    //Cleanup eventual additional spaces before the hyphen that have been already written to the output
                    int z = output.size() - 1;
                    while (z >= 0 && output.get(z).getText().equals(" ")) {
                        String tokenString = output.get(z).getText();

                        if (tokenString.equals(" ")) {
                            output.remove(z);
                        }
                        z--;
                    }

                    List<Integer> breakLines = new ArrayList<>();
                    List<Integer> spaces = new ArrayList<>();

                    int j = i + 1;
                    while (j < tokens.size() && tokens.get(j).getText().equals(" ")
                            || tokens.get(j).getText().equals("\n")) {
                        String tokenString = tokens.get(j).getText();

                        if (tokenString.equals("\n")) {
                            breakLines.add(j);
                        }
                        if (tokenString.equals(" ")) {
                            spaces.add(j);
                        }
                        j++;
                    }
                    i += breakLines.size() + spaces.size();
                } else {
                    output.add(currentToken);

                    List<Integer> breakLines = new ArrayList<>();
                    List<Integer> spaces = new ArrayList<>();

                    int j = i + 1;
                    while (j < tokens.size() && tokens.get(j).getText().equals("\n")) {
                        String tokenString = tokens.get(j).getText();

                        if (tokenString.equals("\n")) {
                            breakLines.add(j);
                        }
                        j++;
                    }
                    i += breakLines.size() + spaces.size();

                }
            } else {
                output.add(currentToken);
            }
        }
        return output;
    }

    /**
     * Check if the current token (place i), or the hyphen, needs to be removed or not.
     * <p>
     * It will check the tokens before and after. It will get to the next "non space" tokens and verify
     * that it's a plain word. If it's not it's keeping the hyphen.
     * <p>
     * TODO: What to do in case of a punctuation is found?
     */
    protected static boolean doesRequireDehypenisation(List<LayoutToken> tokens, int i) {
        boolean forward = false;
        boolean backward = false;

        int j = i + 1;
        int breakLine = 0;
        int spacesAfter = 0;

        double coordinateY = tokens.get(i).getY();

        while (j < tokens.size() && (tokens.get(j).getText().equals(" ") || tokens.get(j).getText().equals("\n"))) {
            if (tokens.get(j).getText().equals("\n")) {
                breakLine++;
            } else if (tokens.get(j).getText().equals(" ")) {
                spacesAfter++;
            } else if (tokens.get(j).getY() > coordinateY) {
                breakLine++;
            }
            j++;
        }

        if (breakLine == 0) {
            // check if there is a break-line using coordinates, if not, no dehypenisation
            if (j < tokens.size() && tokens.get(j).getY() == coordinateY) {
                return false;
            }
        }

        //tokens.stream().collect(groupingBy(LayoutToken::getY)).keySet()

        if (j < tokens.size()) {
            forward = StringUtils.isAllLowerCase(tokens.get(j).getText());
            if (forward) {
                //If nothing before the hyphen, but it looks like a forward hypenisation, let's trust it
                if (i < 1) {
                    return forward;
                }

                //I check if the coordinates have changed, this means there is a newline
                if (tokens.get(j).getY() > coordinateY) {
                    return forward;
                }

                // Check backward
                int z = i - 1;
                while (z > 0 && (tokens.get(z).getText().equals(" ") || tokens.get(z).getText().equals("\n"))) {
                    z--;
                }

                if (StringUtils.isAlpha(tokens.get(z).getText())) {
                    if (tokens.get(z).getY() < coordinateY) {
                        backward = true;
                    } else if (coordinateY == -1 && breakLine > 0) {
                        backward = true;
                    }
                }
            }
        }

        return backward;
    }

    public static List<LayoutToken> subListByOffset(List<LayoutToken> token, int startIncluded) {
        return subListByOffset(token, startIncluded, Integer.MAX_VALUE);
    }

    public static List<LayoutToken> subListByOffset(List<LayoutToken> token, int startIncluded, int endExcluded) {
        return token
                .stream()
                .filter(t -> t.getOffset() >= startIncluded && t.getOffset() < endExcluded)
                .collect(Collectors.toList());
    }

    public static List<LayoutToken> getLayoutTokensForTokenizedText(List<String> tokens) {
        List<LayoutToken> result = new ArrayList<>();
        int pos = 0;
        for (int i = 0; i < tokens.size(); i++) {
            String tok = tokens.get(i);
            LayoutToken layoutToken = new LayoutToken();
            layoutToken.setText(tok);
            layoutToken.setOffset(pos);
            result.add(layoutToken);
            pos += tok.length();
            if (i < tokens.size() - 1 && tokens.get(i + 1).equals("\n")) {
                layoutToken.setNewLineAfter(true);
            }
        }

        return result;
    }

}
