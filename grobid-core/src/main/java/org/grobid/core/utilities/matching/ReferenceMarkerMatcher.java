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
package org.grobid.core.utilities.matching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.engines.counters.ReferenceMarkerMatcherCounters;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.Pair;
import org.grobid.core.utilities.counters.CntManager;

/**
 * Matching reference markers to extracted citations
 */
public class ReferenceMarkerMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceMarkerMatcher.class);

    public static final Pattern YEAR_PATTERN = Pattern.compile("[12][0-9]{3}[a-d]?");
    public static final Pattern YEAR_PATTERN_WITH_LOOK_AROUND = Pattern.compile("(?<!\\d)[12][0-9]{3}(?!\\d)[a-d]?");
    //public static final Pattern AUTHOR_NAME_PATTERN = Pattern.compile("[A-Z][A-Za-z]+");
    public static final Pattern AUTHOR_NAME_PATTERN = Pattern.compile("[A-Z][\\p{L}]+");
    //public static final Pattern NUMBERED_CITATION_PATTERN = Pattern.compile(" *[\\(\\[]? *(?:\\d+[-–]\\d+,|\\d+, *)*[ ]*(?:\\d+[-–]\\d+|\\d+)[\\)\\]]? *");
    public static final Pattern NUMBERED_CITATION_PATTERN = Pattern
            .compile("[\\(\\[]?\\s*(?:\\d+[-−–]\\d+,|\\d+,[ ]*)*[ ]*(?:\\d+[-–]\\d+|\\d+)\\s*[\\)\\]]?");
    public static final Pattern AUTHOR_SEPARATOR_PATTERN = Pattern.compile(";");
    public static final ClassicAnalyzer ANALYZER = new ClassicAnalyzer(Version.LUCENE_45);
    public static final int MAX_RANGE = 20;
    public static final Pattern NUMBERED_CITATIONS_SPLIT_PATTERN = Pattern.compile("[,;]");
    public static final Pattern AND_WORD_PATTERN = Pattern.compile("(and)|&");
    public static final Pattern FIGURE_TABLES_REF_SEPARATORS = Pattern.compile("(and)|(&)|(,)");
    public static final Pattern DASH_PATTERN = Pattern.compile("[–−-]");

    public class MatchResult {
        private String text;
        private List<LayoutToken> tokens;
        private BibDataSet bibDataSet;

        public MatchResult(String text, List<LayoutToken> tokens, BibDataSet bibDataSet) {
            this.text = text;
            this.tokens = tokens;
            this.bibDataSet = bibDataSet;
        }

        public String getText() {
            return text;
        }

        public List<LayoutToken> getTokens() {
            return tokens;
        }

        public BibDataSet getBibDataSet() {
            return bibDataSet;
        }
    }

    public static final Function<String, Object> IDENTITY = new Function<String, Object>() {
        @Override
        public Object apply(String s) {
            return s;
        }
    };
    private final LuceneIndexMatcher<BibDataSet, String> authorMatcher;
    private final LuceneIndexMatcher<BibDataSet, String> labelMatcher;
    private CntManager cntManager;
    private Set<String> allLabels = null;
    private Set<String> allFirstAuthors = null;

    public ReferenceMarkerMatcher(List<BibDataSet> bds, CntManager cntManager)
            throws EntityMatcherException {
        allLabels = new HashSet<String>();
        allFirstAuthors = new HashSet<String>();
        if ((bds != null) && (bds.size() > 0)) {
            for (BibDataSet bibDataSet : bds) {
                allLabels.add(bibDataSet.getRefSymbol());
                //System.out.println(bibDataSet.getRefSymbol());
                String authorString = bibDataSet.getResBib().getFirstAuthorSurname();
                if ((authorString != null) && (authorString.length() > 0))
                    allFirstAuthors.add(authorString);
            }
        }

        this.cntManager = cntManager;
        authorMatcher = new LuceneIndexMatcher<>(
                new Function<BibDataSet, Object>() {
                    @Override
                    public Object apply(BibDataSet bibDataSet) {
                        String authorString = bibDataSet.getResBib().getAuthors() + " et al";
                        if (bibDataSet.getResBib().getPublicationDate() != null) {
                            authorString += " " + bibDataSet.getResBib().getPublicationDate();
                        }
                        //                        System.out.println("Indexing: " + authorString);
                        return authorString;
                    }
                },
                IDENTITY);

        authorMatcher.setMustMatchPercentage(1.0);
        if (bds != null)
            authorMatcher.load(bds);
        labelMatcher = new LuceneIndexMatcher<>(
                new Function<BibDataSet, Object>() {
                    @Override
                    public Object apply(BibDataSet bibDataSet) {
                        return bibDataSet.getRefSymbol();
                    }
                },
                IDENTITY);

        labelMatcher.setMustMatchPercentage(1.0);
        if (bds != null)
            labelMatcher.load(bds);
    }

    public List<MatchResult> match(List<LayoutToken> refTokens) throws EntityMatcherException {
        cntManager.i(ReferenceMarkerMatcherCounters.INPUT_REF_STRINGS_CNT);
        String text = LayoutTokensUtil
                .toText(LayoutTokensUtil.dehyphenize(LayoutTokensUtil.enrichWithNewLineInfo(refTokens)));

        if (isAuthorCitationStyle(text)) {
            cntManager.i(ReferenceMarkerMatcherCounters.STYLE_AUTHORS);
            //System.out.println("STYLE_AUTHORS: " + text);
            return matchAuthorCitation(text, refTokens);
        } else if (isNumberedCitationReference(text)) {
            cntManager.i(ReferenceMarkerMatcherCounters.STYLE_NUMBERED);
            //System.out.println("STYLE_NUMBERED: " + text);
            return matchNumberedCitation(text, refTokens);
        } else {
            cntManager.i(ReferenceMarkerMatcherCounters.STYLE_OTHER);
            //System.out.println("STYLE_OTHER: " + text);
            //            LOGGER.info("Other style: " + text);
            return Collections.singletonList(new MatchResult(text, refTokens, null));
        }
    }

    /*public boolean isAuthorCitationStyle(String text) {
        return ( YEAR_PATTERN.matcher(text.trim()).find() ||
                 NUMBERED_CITATION_PATTERN.matcher(text.trim()).find() )
            && AUTHOR_NAME_PATTERN.matcher(text.trim()).find();
    }*/

    public boolean isAuthorCitationStyle(String text) {
        return YEAR_PATTERN.matcher(text.trim()).find() && AUTHOR_NAME_PATTERN.matcher(text.trim()).find();
    }

    // relaxed number matching
    /*public static boolean isNumberedCitationReference(String t) {
        return NUMBERED_CITATION_PATTERN.matcher(t.trim()).find();
    }*/

    // number matching for number alone or in combination with author for cases "Naze et al. [5]"
    public boolean isNumberedCitationReference(String t) {
        return NUMBERED_CITATION_PATTERN.matcher(t.trim()).matches() ||
                (NUMBERED_CITATION_PATTERN.matcher(t.trim()).find() && AUTHOR_NAME_PATTERN.matcher(t.trim()).find());
    }

    // string number matching
    /*public static boolean isNumberedCitationReference(String t) {
        return NUMBERED_CITATION_PATTERN.matcher(t.trim()).matches();
    }*/

    private List<MatchResult> matchNumberedCitation(String input, List<LayoutToken> refTokens)
            throws EntityMatcherException {
        List<Pair<String, List<LayoutToken>>> labels = getNumberedLabels(refTokens, true);
        List<MatchResult> results = new ArrayList<>();
        for (Pair<String, List<LayoutToken>> label : labels) {
            String text = label.a;
            List<LayoutToken> labelToks = label.b;
            List<BibDataSet> matches = labelMatcher.match(text);
            if (matches.size() == 1) {
                cntManager.i(ReferenceMarkerMatcherCounters.MATCHED_REF_MARKERS);
                //                System.out.println("MATCHED: " + text + "\n" + matches.get(0).getRefSymbol() + "\n" + matches.get(0).getRawBib());

                //                System.out.println("-----------");
                results.add(new MatchResult(text, labelToks, matches.get(0)));
            } else {
                cntManager.i(ReferenceMarkerMatcherCounters.UNMATCHED_REF_MARKERS);
                if (matches.size() != 0) {
                    cntManager.i(ReferenceMarkerMatcherCounters.MANY_CANDIDATES);
                    //                    LOGGER.info("MANY CANDIDATES: " + input + "\n" + text + "\n");
                    for (BibDataSet bds : matches) {
                        //                        LOGGER.info("  " + bds.getRawBib());
                    }

                    //                    LOGGER.info("----------");
                } else {
                    cntManager.i(ReferenceMarkerMatcherCounters.NO_CANDIDATES);
                    //                    LOGGER.info("NO CANDIDATES: " + text + "\n" + text);
                    //                    LOGGER.info("++++++++++++");
                }
                results.add(new MatchResult(text, labelToks, null));
            }
        }
        return results;
    }

    public static List<Pair<String, List<LayoutToken>>> getNumberedLabels(
            List<LayoutToken> layoutTokens,
            boolean addWrappingSymbol) {
        List<List<LayoutToken>> split = LayoutTokensUtil.split(layoutTokens, NUMBERED_CITATIONS_SPLIT_PATTERN, true);
        List<Pair<String, List<LayoutToken>>> res = new ArrayList<>();
        // return [ ] or () depending on (1 - 2) or [3-5])
        Pair<Character, Character> wrappingSymbols = getWrappingSymbols(split.get(0));
        for (List<LayoutToken> s : split) {
            int minusPos = LayoutTokensUtil.tokenPos(s, DASH_PATTERN);
            if (minusPos < 0) {
                res.add(new Pair<>(LayoutTokensUtil.toText(s), s));
            } else {
                try {
                    LayoutToken minusTok = s.get(minusPos);
                    List<LayoutToken> leftNumberToks = s.subList(0, minusPos);
                    List<LayoutToken> rightNumberToks = s.subList(minusPos + 1, s.size());

                    Integer a;
                    Integer b;

                    a = Integer.valueOf(
                            LuceneUtil.tokenizeString(ANALYZER, LayoutTokensUtil.toText(leftNumberToks)).get(0),
                            10);
                    b = Integer.valueOf(
                            LuceneUtil.tokenizeString(ANALYZER, LayoutTokensUtil.toText(rightNumberToks)).get(0),
                            10);

                    if (a < b && b - a < MAX_RANGE) {
                        for (int i = a; i <= b; i++) {
                            List<LayoutToken> tokPtr;
                            if (i == a) {
                                tokPtr = leftNumberToks;
                            } else if (i == b) {
                                tokPtr = rightNumberToks;
                            } else {
                                tokPtr = Collections.singletonList(minusTok);
                            }

                            if (addWrappingSymbol)
                                res.add(new Pair<>(wrappingSymbols.a + String.valueOf(i) + wrappingSymbols.b, tokPtr));
                            else
                                res.add(new Pair<>(String.valueOf(i), tokPtr));
                        }
                    }
                } catch (Exception e) {
                    LOGGER.debug("Cannot parse citation reference range: " + s);
                }

            }
        }
        return res;
    }

    private static Pair<Character, Character> getWrappingSymbols(List<LayoutToken> layoutTokens) {
        for (LayoutToken t : layoutTokens) {
            if (LayoutTokensUtil.spaceyToken(t.t()) || LayoutTokensUtil.newLineToken(t.t())) {
                continue;
            }
            if (t.t().equals("(")) {
                return new Pair<>('(', ')');
            } else {
                return new Pair<>('[', ']');
            }
        }

        return new Pair<>('[', ']');
    }

    private List<MatchResult> matchAuthorCitation(String text, List<LayoutToken> refTokens)
            throws EntityMatcherException {
        List<Pair<String, List<LayoutToken>>> split = splitAuthors(refTokens);
        List<MatchResult> results = new ArrayList<>();

        for (Pair<String, List<LayoutToken>> si : split) {
            String c = si.a;
            List<LayoutToken> splitItem = si.b;

            List<BibDataSet> matches = authorMatcher.match(c);
            if (matches.size() == 1) {
                cntManager.i(ReferenceMarkerMatcherCounters.MATCHED_REF_MARKERS);
                //System.out.println("MATCHED: " + text + "\n" + c + "\n" + matches.get(0).getRawBib());
                results.add(new MatchResult(c, splitItem, matches.get(0)));
            } else {
                if (matches.size() != 0) {
                    cntManager.i(ReferenceMarkerMatcherCounters.MANY_CANDIDATES);
                    List<BibDataSet> filtered = postFilterMatches(c, matches);
                    if (filtered.size() == 1) {
                        results.add(new MatchResult(c, splitItem, filtered.get(0)));
                        cntManager.i(ReferenceMarkerMatcherCounters.MATCHED_REF_MARKERS);
                        cntManager.i(ReferenceMarkerMatcherCounters.MATCHED_REF_MARKERS_AFTER_POST_FILTERING);
                    } else {
                        cntManager.i(ReferenceMarkerMatcherCounters.UNMATCHED_REF_MARKERS);
                        results.add(new MatchResult(c, splitItem, null));
                        if (filtered.size() == 0) {
                            cntManager.i(ReferenceMarkerMatcherCounters.NO_CANDIDATES_AFTER_POST_FILTERING);
                        } else {
                            cntManager.i(ReferenceMarkerMatcherCounters.MANY_CANDIDATES_AFTER_POST_FILTERING);
                            //LOGGER.info("SEVERAL MATCHED REF CANDIDATES: " + text + "\n-----\n" + c + "\n");
                            /*for (BibDataSet bds : matches) {
                                LOGGER.info("+++++");
                                LOGGER.info("  " + bds.getRawBib());
                            }*/
                        }
                    }
                } else {
                    results.add(new MatchResult(c, splitItem, null));
                    cntManager.i(ReferenceMarkerMatcherCounters.NO_CANDIDATES);
                    //LOGGER.info("NO MATCHED REF CANDIDATES: " + text + "\n" + c);
                    //LOGGER.info("++++++++++++");
                }
            }
        }

        return results;
    }

    // splitting into individual citation references strings like in:
    // Kuwajima et al., 1985; Creighton, 1990; Ptitsyn et al., 1990;
    private static List<Pair<String, List<LayoutToken>>> splitAuthors(List<LayoutToken> toks) {
        List<List<LayoutToken>> split = LayoutTokensUtil.split(toks, AUTHOR_SEPARATOR_PATTERN, true);
        List<Pair<String, List<LayoutToken>>> result = new ArrayList<>();

        for (List<LayoutToken> splitTokens : split) {
            //cases like: Khechinashvili et al. (1973) and Privalov (1979)
            String text = LayoutTokensUtil.toText(splitTokens);
            int matchCount = matchCount(text, YEAR_PATTERN_WITH_LOOK_AROUND);
            if (matchCount == 2 && text.contains(" and ")) {
                for (List<LayoutToken> ys : LayoutTokensUtil.split(splitTokens, AND_WORD_PATTERN, true)) {
                    result.add(new Pair<>(LayoutTokensUtil.toText(LayoutTokensUtil.dehyphenize(ys)), ys));
                }
            } else if (matchCount > 1) {
                List<List<LayoutToken>> yearSplit = LayoutTokensUtil.split(splitTokens, YEAR_PATTERN, true, false);
                List<List<LayoutToken>> yearSplitWithLeftOver = LayoutTokensUtil
                        .split(splitTokens, YEAR_PATTERN, true, true);
                // do we have a leftover to be added?
                List<LayoutToken> leftover = null;
                if (yearSplit.size() < yearSplitWithLeftOver.size()) {
                    leftover = yearSplitWithLeftOver.get(yearSplitWithLeftOver.size() - 1);
                }
                if (yearSplit.isEmpty()) {
                    result.add(
                            new Pair<>(LayoutTokensUtil.toText(LayoutTokensUtil.dehyphenize(splitTokens)),
                                    splitTokens));
                } else {
                    if (matchCount(splitTokens, AUTHOR_NAME_PATTERN) == 1) {
                        // cases like Grafton et al. 1995, 1998;
                        // the idea is that we produce as many labels as we have year.
                        //E.g. "Grafton et al. 1995, 1998;" will become two pairs:
                        // 1) ("Grafton et al. 1995", tokens_of("Grafton et al. 1995"))
                        // 2) ("Grafton et al. 1998", tokens_of("1998"))
                        // this method will allow to mark two citations in a non-overlapping manner

                        List<LayoutToken> firstYearSplitItem;
                        firstYearSplitItem = yearSplit.get(0);
                        result.add(
                                new Pair<>(LayoutTokensUtil.toText(LayoutTokensUtil.dehyphenize(firstYearSplitItem)),
                                        firstYearSplitItem));

                        List<LayoutToken> excludedYearToks = firstYearSplitItem
                                .subList(0, firstYearSplitItem.size() - 1);
                        String authorName = LayoutTokensUtil.toText(LayoutTokensUtil.dehyphenize(excludedYearToks));

                        for (int i = 1; i < yearSplit.size(); i++) {
                            List<LayoutToken> toksI = yearSplit.get(i);
                            if (i == yearSplit.size() - 1 && leftover != null) {
                                List<LayoutToken> lastSegmentTokens = toksI.subList(toksI.size() - 1, toksI.size());
                                lastSegmentTokens.addAll(leftover);
                                result.add(
                                        new Pair<>(
                                                authorName
                                                        + " "
                                                        + LayoutTokensUtil.toText(LayoutTokensUtil.dehyphenize(toksI))
                                                        + LayoutTokensUtil.toText(leftover),
                                                lastSegmentTokens));
                            } else {
                                result.add(
                                        new Pair<>(
                                                authorName
                                                        + " "
                                                        + LayoutTokensUtil.toText(LayoutTokensUtil.dehyphenize(toksI)),
                                                toksI.subList(toksI.size() - 1, toksI.size())));
                            }
                        }
                    } else {
                        // case when two authors still appear
                        for (int k = 0; k < yearSplit.size(); k++) {
                            List<LayoutToken> item = yearSplit.get(k);
                            if (k == yearSplit.size() - 1 && leftover != null) {
                                List<LayoutToken> lastSegmentTokens = item;
                                lastSegmentTokens.addAll(leftover);
                                result.add(
                                        new Pair<>(
                                                LayoutTokensUtil
                                                        .toText(LayoutTokensUtil.dehyphenize(lastSegmentTokens)),
                                                lastSegmentTokens));
                            } else
                                result.add(
                                        new Pair<>(LayoutTokensUtil.toText(LayoutTokensUtil.dehyphenize(item)), item));
                        }
                    }
                }
            } else {
                result.add(new Pair<>(LayoutTokensUtil.toText(LayoutTokensUtil.dehyphenize(splitTokens)), splitTokens));
            }
        }
        return result;
    }

    private static int matchCount(String s, Pattern p) {
        Matcher m = p.matcher(s);
        int cnt = 0;
        while (m.find()) {
            cnt++;
        }
        return cnt;
    }

    private static int matchCount(List<LayoutToken> toks, Pattern p) {
        return matchCount(LayoutTokensUtil.toText(toks), p);
    }

    //if we match more than 1 citation based on name, then we leave only those citations that have author name first
    private List<BibDataSet> postFilterMatches(String c, List<BibDataSet> matches) {
        if (c.toLowerCase().contains("et al") || c.toLowerCase().contains(" and ")) {
            String[] sp = c.trim().split(" ");
            //callouts often include parentheses as seen in https://grobid.readthedocs.io/en/latest/training/fulltext/
            final String author = sp[0].replaceAll("[\\(\\[]", "").toLowerCase();
            ArrayList<BibDataSet> bibDataSets = Lists
                    .newArrayList(Iterables.filter(matches, new Predicate<BibDataSet>() {
                        @Override
                        public boolean apply(BibDataSet bibDataSet) {
                            // first author last name formatted raw bib
                            return bibDataSet.getRawBib().trim().toLowerCase().startsWith(author);
                        }
                    }));

            if (bibDataSets.size() == 1) {
                return bibDataSets;
            }

            bibDataSets = Lists.newArrayList(Iterables.filter(matches, new Predicate<BibDataSet>() {
                @Override
                public boolean apply(BibDataSet bibDataSet) {
                    BiblioItem resBib = bibDataSet.getResBib();
                    if (resBib == null)
                        return false;
                    String firstAuthorLastName = resBib.getFirstAuthorSurname();
                    if (firstAuthorLastName == null)
                        return false;
                    firstAuthorLastName = firstAuthorLastName.toLowerCase();
                    // first author forename last name formatted raw bib
                    return firstAuthorLastName.equals(author);
                }
            }));

            if (bibDataSets.size() <= 1) {
                return bibDataSets;
            }

            //cases like c = "Smith et al, 2015" and Bds = <"Smith, Hoffmann, 2015", "Smith, 2015"> -- should prefer first one
            return Lists.newArrayList(Iterables.filter(bibDataSets, new Predicate<BibDataSet>() {
                @Override
                public boolean apply(BibDataSet bibDataSet) {
                    return (bibDataSet.getResBib().getFullAuthors() != null
                            && bibDataSet.getResBib().getFullAuthors().size() > 1);
                }
            }));
        } else {
            //cases like c = "Smith, 2015" and Bds = <"Smith, Hoffmann, 2015", "Smith, 2015"> -- should prefer second one
            return Lists.newArrayList(Iterables.filter(matches, new Predicate<BibDataSet>() {
                @Override
                public boolean apply(BibDataSet bibDataSet) {
                    return bibDataSet.getResBib().getFullAuthors() != null
                            && bibDataSet.getResBib().getFullAuthors().size() == 1;
                }
            }));
        }
    }

    /**
     * Return true if the text is a known label from the bibliographical reference list
     */
    public boolean isKnownLabel(String text) {
        if ((allLabels != null) && (allLabels.contains(text.trim())))
            return true;
        else
            return false;
    }

    /**
     * Return true if the text is a known first author from the bibliographical reference list
     */
    public boolean isKnownFirstAuthor(String text) {
        if ((allFirstAuthors != null) && (allFirstAuthors.contains(text.trim())))
            return true;
        else
            return false;
    }

}
