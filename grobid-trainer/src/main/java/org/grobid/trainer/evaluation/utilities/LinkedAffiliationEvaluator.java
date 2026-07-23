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
package org.grobid.trainer.evaluation.utilities;

import static org.grobid.trainer.evaluation.EndToEndEvaluation.minLevenshteinDistance;
import static org.grobid.trainer.evaluation.EndToEndEvaluation.minRatcliffObershelpSimilarity;
import static org.grobid.trainer.evaluation.utilities.EvaluationNormalization.basicNormalization;
import static org.grobid.trainer.evaluation.utilities.EvaluationNormalization.removeFullPunct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

import com.rockymadden.stringmetric.similarity.RatcliffObershelpMetric;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import scala.Option;

import org.grobid.core.utilities.TextUtilities;
import org.grobid.trainer.evaluation.Stats;

/**
 * Per-author linked-affiliation accuracy metric.
 *
 * <p>The standard header fields concatenate all affiliation text across all authors into a
 * single string before comparing, so they measure affiliation <i>extraction</i>, not the
 * author&#8596;affiliation <i>linking</i>. This metric instead pairs each gold author with a
 * grobid author (by normalised surname, forename initial as a tie-break) and compares the
 * affiliations that are actually attached to each of them. Results are accumulated under the
 * label {@code affiliation_linked} on the four matching variants (strict / soft / Levenshtein /
 * Ratcliff-Obershelp), so they appear automatically in the existing field-level tables.</p>
 *
 * <p>Scope note: only authors whose gold affiliation link is explicit (JATS {@code xref/@rid}
 * or a nested {@code aff}, or a nested {@code affiliation} in pub2TEI gold) are scored. Gold
 * documents that encode affiliations purely positionally are out of scope (such authors are
 * skipped, not counted as missed). Collaboration "authors" are skipped.</p>
 */
public class LinkedAffiliationEvaluator implements CustomFieldEvaluator {

    /** label under which the per-author linked-affiliation metric is reported */
    public static final String AFFILIATION_LINKED_LABEL = "affiliation_linked";

    /** minimum length for a substring-containment affiliation match (avoids trivial hits) */
    private static final int AFFILIATION_CONTAINMENT_FLOOR = 5;

    /** Footnote appended to the header section of the report when this field is evaluated. */
    public static final String REPORT_NOTE = "\nNote: the \"affiliation_linked\" field above is a "
            + "linking-aware metric (each author is paired with its gold counterpart and "
            + "their attached affiliations compared). Its support column reports the number "
            + "of articles the metric is computed from (those with at least one explicit "
            + "gold affiliation link), while precision/recall/F1 are measured over the "
            + "individual author-affiliation links.\n"
            + "Only authors whose gold affiliation link is explicit are scored; "
            + "affiliations encoded purely positionally in the gold (no xref/@rid and no "
            + "nested aff) are out of scope, not counted as misses.\n"
            + "Ground truth: single-affiliation papers (exactly one <aff>) have been "
            + "completed by linking every author to that sole affiliation (~1,649 authors "
            + "across PMC, bioRxiv and PLOS). Still to be done: multi-affiliation papers "
            + "that encode the author-to-affiliation mapping only positionally, which "
            + "require the PDF superscripts to disambiguate.\n";

    /**
     * @return the number of gold authors actually scored (those with an explicit, resolvable
     *     affiliation link). A return value {@code > 0} means this article contributes to the
     *     affiliation_linked metric; the caller uses it to count contributing articles.
     */
    @Override
    public int evaluate(
            Document gold,
            Document tei,
            String inputType,
            XPath xp,
            Stats strictStats,
            Stats softStats,
            Stats levenshteinStats,
            Stats ratcliffObershelpStats) throws Exception {

        List<AuthorAff> grobidAuthors = extractGrobidAuthors(tei, xp);
        List<AuthorAff> goldAuthors = inputType.equals("nlm")
                ? extractNlmAuthors(gold, xp)
                : extractGrobidAuthors(gold, xp);

        Stats[] allStats = {strictStats, softStats, levenshteinStats, ratcliffObershelpStats};

        boolean[] consumed = new boolean[grobidAuthors.size()];
        int scoredAuthors = 0;
        for (AuthorAff goldAuthor : goldAuthors) {
            if (goldAuthor.surnameNorm.isEmpty()) {
                // collaboration or otherwise unnamed contributor: out of scope
                continue;
            }
            if (goldAuthor.affs.isEmpty()) {
                // no explicit gold affiliation link for this author: out of scope (not a miss)
                continue;
            }

            int matchIdx = findMatchingGrobidAuthor(goldAuthor, grobidAuthors, consumed);
            List<Aff> grobidAffs;
            if (matchIdx >= 0) {
                consumed[matchIdx] = true;
                grobidAffs = grobidAuthors.get(matchIdx).affs;
            } else {
                // unmatched gold author: every expected affiliation is a false negative
                grobidAffs = Collections.emptyList();
            }

            for (int level = 0; level < allStats.length; level++) {
                scoreAuthorAffiliations(goldAuthor.affs, grobidAffs, level, allStats[level]);
            }
            scoredAuthors++;
        }

        // grobid authors that were never paired but still carry affiliation text:
        // these affiliations were linked to an author that does not align with any gold author
        for (int i = 0; i < grobidAuthors.size(); i++) {
            if (consumed[i]) {
                continue;
            }
            int nbAffs = grobidAuthors.get(i).affs.size();
            for (Stats stats : allStats) {
                for (int k = 0; k < nbAffs; k++) {
                    stats.incrementFalsePositive(AFFILIATION_LINKED_LABEL);
                }
            }
        }

        return scoredAuthors;
    }

    /**
     * Greedy 1:1 matching of a single author's gold affiliations against the grobid ones, for a
     * given matching variant. Each gold affiliation is one expected unit; a matched grobid
     * affiliation is a true positive (observed), an unmatched gold affiliation a false negative,
     * and any leftover grobid affiliation a false positive.
     */
    private void scoreAuthorAffiliations(List<Aff> goldAffs, List<Aff> grobidAffs, int level, Stats stats) {
        boolean[] used = new boolean[grobidAffs.size()];
        for (Aff goldAff : goldAffs) {
            stats.incrementExpected(AFFILIATION_LINKED_LABEL);
            int match = -1;
            for (int k = 0; k < grobidAffs.size(); k++) {
                if (used[k]) {
                    continue;
                }
                if (affiliationMatches(goldAff, grobidAffs.get(k), level)) {
                    match = k;
                    break;
                }
            }
            if (match >= 0) {
                used[match] = true;
                stats.incrementObserved(AFFILIATION_LINKED_LABEL);
            } else {
                stats.incrementFalseNegative(AFFILIATION_LINKED_LABEL);
            }
        }
        for (int k = 0; k < grobidAffs.size(); k++) {
            if (!used[k]) {
                stats.incrementFalsePositive(AFFILIATION_LINKED_LABEL);
            }
        }
    }

    /**
     * Whether a gold and a grobid affiliation match at the given variant.
     * level: 0 = strict, 1 = soft, 2 = Levenshtein, 3 = Ratcliff/Obershelp.
     * For the non-strict variants a substring-containment fallback is allowed, because gold JATS
     * affiliation text is usually a superset of grobid's structured orgName (it bundles
     * city/country/zip that grobid splits into the address).
     */
    private boolean affiliationMatches(Aff gold, Aff grobid, int level) {
        if (level == 0) {
            return gold.strictNorm.length() > 0 && gold.strictNorm.equals(grobid.strictNorm);
        }

        boolean contained = containment(gold.softNorm, grobid.orgNorm)
                || containment(gold.orgNorm, grobid.softNorm);

        if (level == 1) {
            return (gold.softNorm.length() > 0 && gold.softNorm.equals(grobid.softNorm)) || contained;
        }

        if (level == 2) {
            if (gold.strictNorm.length() > 0 && grobid.strictNorm.length() > 0) {
                int distance = TextUtilities.getLevenshteinDistance(gold.strictNorm, grobid.strictNorm);
                int bigger = Math.max(gold.strictNorm.length(), grobid.strictNorm.length());
                double pct = (double) (bigger - distance) / bigger;
                if (pct >= minLevenshteinDistance) {
                    return true;
                }
            }
            return contained;
        }

        // Ratcliff/Obershelp
        if (gold.strictNorm.length() > 0 && grobid.strictNorm.length() > 0) {
            Option<Object> similarityObject = RatcliffObershelpMetric.compare(gold.strictNorm, grobid.strictNorm);
            if ((similarityObject != null) && (similarityObject.get() != null)
                    && ((Double) similarityObject.get() >= minRatcliffObershelpSimilarity)) {
                return true;
            }
        }
        return contained;
    }

    private static boolean containment(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() < AFFILIATION_CONTAINMENT_FLOOR || b.length() < AFFILIATION_CONTAINMENT_FLOOR) {
            return false;
        }
        return a.contains(b) || b.contains(a);
    }

    /**
     * Greedy author pairing: the first not-yet-consumed grobid author with the same normalised
     * surname; a matching forename initial is preferred when several share the surname.
     */
    private int findMatchingGrobidAuthor(AuthorAff gold, List<AuthorAff> grobidAuthors, boolean[] consumed) {
        int firstSurnameMatch = -1;
        for (int i = 0; i < grobidAuthors.size(); i++) {
            if (consumed[i]) {
                continue;
            }
            AuthorAff candidate = grobidAuthors.get(i);
            if (candidate.surnameNorm.isEmpty() || !candidate.surnameNorm.equals(gold.surnameNorm)) {
                continue;
            }
            if (firstSurnameMatch < 0) {
                firstSurnameMatch = i;
            }
            if (!gold.forenameInitial.isEmpty() && gold.forenameInitial.equals(candidate.forenameInitial)) {
                return i;
            }
        }
        return firstSurnameMatch;
    }

    /**
     * Extract authors and their nested affiliations from a grobid (or pub2TEI gold) TEI document.
     */
    private List<AuthorAff> extractGrobidAuthors(Document doc, XPath xp) throws Exception {
        List<AuthorAff> result = new ArrayList<>();
        NodeList authorNodes = (NodeList) xp.compile("//sourceDesc/biblStruct/analytic/author")
                .evaluate(doc.getDocumentElement(), XPathConstants.NODESET);
        for (int i = 0; i < authorNodes.getLength(); i++) {
            Node authorNode = authorNodes.item(i);
            AuthorAff record = new AuthorAff();
            record.surnameNorm = normalizeName(getTextContent(xp, authorNode, "persName/surname/text()"));
            String forename = getTextContent(xp, authorNode, "persName/forename[@type=\"first\"]/text()");
            if (forename.isEmpty()) {
                forename = getTextContent(xp, authorNode, "persName/forename/text()");
            }
            record.forenameInitial = initial(forename);

            NodeList affNodes = (NodeList) xp.compile("affiliation").evaluate(authorNode, XPathConstants.NODESET);
            for (int j = 0; j < affNodes.getLength(); j++) {
                Node affNode = affNodes.item(j);
                // skip collaboration-as-affiliation
                NodeList collab = (NodeList) xp.compile("orgName[@type=\"collaboration\"]")
                        .evaluate(affNode, XPathConstants.NODESET);
                if (collab.getLength() > 0) {
                    continue;
                }
                String orgName = getTextContent(xp, affNode, "orgName/text()");
                String address = getTextContent(xp, affNode, "address//text()");
                String full = (orgName + " " + address).trim();
                if (!full.isEmpty()) {
                    record.affs.add(makeAff(orgName, full));
                }
            }
            result.add(record);
        }
        return result;
    }

    /**
     * Extract authors and their linked affiliations from an NLM/JATS gold document.
     * The author&#8594;affiliation link follows {@code contrib/xref[@ref-type="aff"]/@rid} to the
     * {@code aff} with the matching id (under {@code contrib-group} or {@code article-meta}), with a
     * fallback to an {@code aff} nested directly inside the {@code contrib}.
     */
    private List<AuthorAff> extractNlmAuthors(Document gold, XPath xp) throws Exception {
        // index every affiliation by its id (label child excluded)
        Map<String, Aff> affById = new HashMap<>();
        NodeList affNodes = (NodeList) xp.compile("//article-meta//aff")
                .evaluate(gold.getDocumentElement(), XPathConstants.NODESET);
        for (int i = 0; i < affNodes.getLength(); i++) {
            Node affNode = affNodes.item(i);
            String id = getTextContent(xp, affNode, "@id");
            if (id.isEmpty()) {
                continue;
            }
            String text = getTextContent(xp, affNode, ".//text()[not(parent::label)]");
            affById.put(id, makeAff(text, text));
        }

        List<AuthorAff> result = new ArrayList<>();
        NodeList contribs = (NodeList) xp.compile(
                "/article/front/article-meta/contrib-group/contrib[@contrib-type=\"author\"]")
                .evaluate(gold.getDocumentElement(), XPathConstants.NODESET);
        for (int i = 0; i < contribs.getLength(); i++) {
            Node contrib = contribs.item(i);
            AuthorAff record = new AuthorAff();
            String surname = getTextContent(xp, contrib, "name/surname/text()");
            if (surname.isEmpty()) {
                surname = getTextContent(xp, contrib, "string-name/surname/text()");
            }
            record.surnameNorm = normalizeName(surname);
            record.forenameInitial = initial(getTextContent(xp, contrib, "name/given-names/text()"));

            NodeList rids = (NodeList) xp.compile("xref[@ref-type=\"aff\"]/@rid")
                    .evaluate(contrib, XPathConstants.NODESET);
            if (rids.getLength() > 0) {
                for (int r = 0; r < rids.getLength(); r++) {
                    for (String rid : rids.item(r).getNodeValue().trim().split("\\s+")) {
                        Aff aff = affById.get(rid);
                        if (aff != null) {
                            record.affs.add(aff);
                        }
                    }
                }
            } else {
                // fallback: affiliation nested directly inside the contrib
                NodeList nested = (NodeList) xp.compile(".//aff").evaluate(contrib, XPathConstants.NODESET);
                for (int n = 0; n < nested.getLength(); n++) {
                    String text = getTextContent(xp, nested.item(n), ".//text()[not(parent::label)]");
                    if (!text.trim().isEmpty()) {
                        record.affs.add(makeAff(text, text));
                    }
                }
            }
            result.add(record);
        }
        return result;
    }

    /** Concatenated value of every node selected by the relative path, space-separated. */
    private String getTextContent(XPath xp, Node context, String path) throws Exception {
        NodeList nodes = (NodeList) xp.compile(path).evaluate(context, XPathConstants.NODESET);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nodes.getLength(); i++) {
            String value = nodes.item(i).getNodeValue();
            if (value != null) {
                sb.append(value).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static String normalizeName(String name) {
        return removeFullPunct(basicNormalization(name == null ? "" : name));
    }

    private static String initial(String forename) {
        String normalized = normalizeName(forename);
        return normalized.isEmpty() ? "" : normalized.substring(0, 1);
    }

    private static Aff makeAff(String orgNameText, String fullText) {
        Aff aff = new Aff();
        aff.strictNorm = basicNormalization(fullText);
        aff.softNorm = removeFullPunct(fullText);
        aff.orgNorm = removeFullPunct(orgNameText);
        return aff;
    }

    /** An author with the affiliations linked to them, for linking-aware evaluation. */
    private static class AuthorAff {
        String surnameNorm = "";
        String forenameInitial = "";
        List<Aff> affs = new ArrayList<>();
    }

    /** A single affiliation reduced to the normalisations used by the matching variants. */
    private static class Aff {
        String strictNorm = "";
        String softNorm = "";
        String orgNorm = "";
    }
}
