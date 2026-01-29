package org.grobid.core.utilities;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;

import java.util.List;

/**
 * Utilities to calculate bounding boxes from coordinates
 */
public class BoundingBoxCalculator {
    private static final double EPS_X = 15;
    private static final double EPS_Y = 4;

    public static BoundingBox calculateOneBox(Iterable<LayoutToken> tokens) {
        return calculateOneBox(tokens, false);
    }

    public static BoundingBox calculateOneBox(Iterable<LayoutToken> tokens, boolean ignoreDifferentPageTokens) {
        if (tokens == null) {
            return null;
        }

        BoundingBox b = null;
        for (LayoutToken t : tokens) {
            if (LayoutTokensUtil.noCoords(t)) {
                continue;
            }
            if (b == null) {
                b = BoundingBox.fromLayoutToken(t);
            } else {
                if (ignoreDifferentPageTokens) {
                    b = b.boundBoxExcludingAnotherPage(BoundingBox.fromLayoutToken(t));
                } else {
                    b = b.boundBox(BoundingBox.fromLayoutToken(t));
                }
            }
        }
        return b;
    }

    public static List<BoundingBox> calculate(List<LayoutToken> tokens) {
        List<BoundingBox> result = Lists.newArrayList();
        if (tokens != null) {
            tokens = Lists.newArrayList(Iterables.filter(tokens, new Predicate<LayoutToken>() {
                @Override
                public boolean apply(LayoutToken layoutToken) {
                    return !(Math.abs(layoutToken.getWidth()) <= Double.MIN_VALUE
                            || Math.abs(layoutToken.getHeight()) <= Double.MIN_VALUE);
                }
            }));
        }

        if (tokens == null || tokens.isEmpty()) {
            return result;
        }

        BoundingBox firstBox = BoundingBox.fromLayoutToken(tokens.get(0));
        result.add(firstBox);
        BoundingBox lastBox = firstBox;
        for (int i = 1; i < tokens.size(); i++) {
            BoundingBox b = BoundingBox.fromLayoutToken(tokens.get(i));
            if (Math.abs(b.getWidth()) <= Double.MIN_VALUE || Math.abs(b.getHeight()) <= Double.MIN_VALUE) {
                continue;
            }

            if (near(lastBox, b)) {
                result.set(result.size() - 1, result.get(result.size() - 1).boundBox(b));
            } else {
                result.add(b);
            }
            lastBox = b;
        }
        return result;
    }

    // same page, Y is more or less the same, b2 follows b1 on X, and b2 close to the end of b1
    private static boolean near(BoundingBox b1, BoundingBox b2) {
        return b1.getPage() == b2.getPage() && Math.abs(b1.getY() - b2.getY()) < EPS_Y
                && Math.abs(b1.getY2() - b2.getY2()) < EPS_Y && b2.getX() - b1.getX2() < EPS_X
                && b2.getX() >= b1.getX();
    }

}
