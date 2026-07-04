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
package org.grobid.core.data.util;

import java.util.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.data.Affiliation;
import org.grobid.core.data.Person;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;

/**
 * Assigns affiliations to authors using a priority-based strategy:
 * <ol>
 * <li>Distribution — trivial single-author or single-affiliation cases</li>
 * <li>Direct marker matching — matches structured Person.getMarkers() against
 * Affiliation.getMarker()</li>
 * <li>String-search marker matching — searches for affiliation markers in the
 * original author string (fallback for when name model misses markers)</li>
 * <li>Proximity matching — coordinate distance between layout tokens</li>
 * <li>Sequential fallback — last resort when no coordinates available</li>
 * </ol>
 */
public class AuthorAffiliationAssigner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorAffiliationAssigner.class);

    /**
     * Main entry point. Assigns affiliations to authors using the priority
     * strategy.
     *
     * @param authors         list of authors (modified in place via addAffiliation)
     * @param affiliations    list of affiliations (failAffiliation flag updated in
     *                        place)
     * @param originalAuthors the raw author string from the header (used for marker
     *                        matching)
     */
    public static void assign(List<Person> authors, List<Affiliation> affiliations, String originalAuthors) {
        if (CollectionUtils.isEmpty(authors) || CollectionUtils.isEmpty(affiliations)) {
            return;
        }

        int nbAuthors = authors.size();
        int nbAffiliations = affiliations.size();

        LOGGER.debug("Assigning affiliations: {} authors, {} affiliations", nbAuthors, nbAffiliations);

        // 1. Distribution: trivial cases
        if (nbAffiliations == 1) {
            // single affiliation → distribute to all authors
            Affiliation aff = affiliations.get(0);
            for (Person aut : authors) {
                aut.addAffiliation(aff);
            }
            aff.setFailAffiliation(false);
            LOGGER.debug(
                    "Distribution: single affiliation '{}' assigned to all {} authors",
                    aff.getRawAffiliationString(),
                    nbAuthors);
            return;
        }

        if (nbAuthors == 1 && nbAffiliations > 1) {
            // single author → assign all affiliations
            Person auth = authors.get(0);
            for (Affiliation aff : affiliations) {
                auth.addAffiliation(aff);
                aff.setFailAffiliation(false);
            }
            LOGGER.debug(
                    "Distribution: all {} affiliations assigned to single author '{}'",
                    nbAffiliations,
                    auth.getLastName());
            return;
        }

        // 2. Direct marker matching (Person.getMarkers() vs Affiliation.getMarker())
        assignByDirectMarkers(authors, affiliations);

        // 3. Marker matching (string-search approach — robust to name model errors)
        assignByMarkers(authors, affiliations, originalAuthors);

        // 4. Proximity matching (primary fallback for authors still without
        // affiliations)
        assignByProximity(authors, affiliations);

        // 5. Sequential fallback (last resort when no coordinates available)
        assignBySequence(authors, affiliations);

        // 6. Orphan rescue: assign any remaining unlinked affiliations to the
        //    nearest author by proximity (or sequentially if no coordinates).
        //    This prevents affiliations from being completely lost when the
        //    earlier strategies over-assigned some affiliations.
        rescueOrphanAffiliations(authors, affiliations);
    }

    /**
     * Pre-link tier (runs before {@link #assign}).
     * <p>
     * For headers without any markers — neither {@link Person#getMarkers()}
     * nor {@link Affiliation#getMarker()} — use the HEADER model's labelled
     * cluster stream to link an affiliation to the author whose name appears
     * in the immediately-preceding {@code <author>} cluster(s).
     * <p>
     * Two patterns are covered:
     * <ul>
     * <li><b>Per-author back blocks</b> (e.g. arXiv 1902.04360): each
     * affiliation block is preceded by exactly one author block.</li>
     * <li><b>Multi-author shared affiliation</b>:
     * {@code <author>A</author><author>B</author><author>C</author><affiliation>X</affiliation>}
     * — all three authors are linked to X by walking back through consecutive
     * {@code <author>} clusters.</li>
     * </ul>
     * <p>
     * The walk stops at the first non-{@code <author>} cluster (e.g. an
     * {@code <other>} footnote), keeping the heuristic conservative —
     * unmatched affiliations are left for the existing tiers in
     * {@link #assign} to resolve.
     * <p>
     * Pure no-op when any marker exists on any author or affiliation, or
     * when the cluster list is null/empty. This preserves existing behaviour
     * for marker-bearing headers.
     *
     * @param authors      list of authors (modified in place via addAffiliation)
     * @param affiliations list of affiliations (failAffiliation flag updated in
     *                     place)
     * @param clusters     HEADER model labelled cluster stream, in document
     *                     order; may be null/empty
     */
    public static void preLinkByPrecedingAuthorCluster(
            List<Person> authors,
            List<Affiliation> affiliations,
            List<TaggingTokenCluster> clusters) {
        if (CollectionUtils.isEmpty(authors)
                || CollectionUtils.isEmpty(affiliations)
                || CollectionUtils.isEmpty(clusters)) {
            return;
        }

        // Gate: any marker anywhere disables this tier.
        for (Person aut : authors) {
            if (CollectionUtils.isNotEmpty(aut.getMarkers())) {
                return;
            }
        }
        for (Affiliation aff : affiliations) {
            if (StringUtils.isNotBlank(aff.getMarker())) {
                return;
            }
        }

        // Reference-equality map: LayoutToken instances are passed through
        // AffiliationAddressParser unchanged (see AffiliationAddressParser
        // line 189–214), so identity lookup is correct.
        IdentityHashMap<LayoutToken, Affiliation> tokenToAff = new IdentityHashMap<>();
        for (Affiliation aff : affiliations) {
            List<LayoutToken> affTokens = aff.getLayoutTokens();
            if (affTokens == null) {
                continue;
            }
            for (LayoutToken tok : affTokens) {
                tokenToAff.putIfAbsent(tok, aff);
            }
        }

        for (int i = 0; i < clusters.size(); i++) {
            TaggingTokenCluster cluster = clusters.get(i);
            if (cluster == null || !TaggingLabels.HEADER_AFFILIATION.equals(cluster.getTaggingLabel())) {
                continue;
            }

            // One HEADER <affiliation> cluster may produce multiple Affiliation
            // objects (AffiliationAddressParser flattens then re-clusters), so
            // collect every distinct Affiliation whose tokens appear here.
            LinkedHashSet<Affiliation> targets = new LinkedHashSet<>();
            for (LayoutToken tok : cluster.concatTokens()) {
                Affiliation aff = tokenToAff.get(tok);
                if (aff != null) {
                    targets.add(aff);
                }
            }
            if (targets.isEmpty()) {
                continue;
            }

            // Walk back through consecutive HEADER_AUTHOR clusters. Each cluster
            // can contain multiple authors separated by "," / "and" — split the
            // tokens into name spans and match each independently so all authors
            // in a multi-author cluster get linked to the affiliation.
            for (int j = i - 1; j >= 0; j--) {
                TaggingTokenCluster prev = clusters.get(j);
                if (prev == null || !TaggingLabels.HEADER_AUTHOR.equals(prev.getTaggingLabel())) {
                    break;
                }
                for (List<LayoutToken> span : splitByNameSeparators(prev.concatTokens())) {
                    // matchPersonsByCluster returns multiple authors when the
                    // span covers several names without a separator (header
                    // model output for "M.D. Atkinson S.J. van Willigenburg"
                    // where neither comma nor "and" was tagged) — link the
                    // affiliation to all matched authors so co-authors don't
                    // silently lose their shared affiliation.
                    for (Person matched : matchPersonsByCluster(span, authors)) {
                        for (Affiliation aff : targets) {
                            List<Affiliation> existing = matched.getAffiliations();
                            if (existing == null || !existing.contains(aff)) {
                                matched.addAffiliation(aff);
                                aff.setFailAffiliation(false);
                                LOGGER.debug(
                                        "Pre-link by preceding <author>: '{}' linked to affiliation '{}'",
                                        matched.getLastName(),
                                        aff.getRawAffiliationString());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Split a sequence of cluster tokens into individual author-name spans by
     * "," and "and" separators. A cluster like "A, B, C and D" yields four
     * spans, each containing only the tokens for one author. Single-author
     * clusters return a single span.
     */
    static List<List<LayoutToken>> splitByNameSeparators(List<LayoutToken> tokens) {
        List<List<LayoutToken>> spans = new ArrayList<>();
        if (CollectionUtils.isEmpty(tokens)) {
            return spans;
        }
        List<LayoutToken> current = new ArrayList<>();
        for (LayoutToken tok : tokens) {
            String text = tok.getText();
            if (text == null) {
                continue;
            }
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (",".equals(trimmed) || "and".equalsIgnoreCase(trimmed)) {
                if (!current.isEmpty()) {
                    spans.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            current.add(tok);
        }
        if (!current.isEmpty()) {
            spans.add(current);
        }
        return spans;
    }

    /**
     * Match the author-cluster surface against the known authors list using
     * surname plus first-initial (consistent with {@link Person#deduplicate}).
     * Returns null when zero or more than one candidate matches — ambiguous
     * matches are left for downstream tiers.
     */
    static Person matchPersonByCluster(List<LayoutToken> clusterTokens, List<Person> authors) {
        List<Person> matches = matchPersonsByCluster(clusterTokens, authors);
        return matches.size() == 1 ? matches.get(0) : null;
    }

    /**
     * Multi-author variant of {@link #matchPersonByCluster}. When the
     * &lt;author&gt; cluster covers several names with no separator token (a
     * common header-model output for "M.D. Atkinson S.J. van Willigenburg"
     * where neither "and" nor "," was emitted), the cluster's tokens contain
     * multiple distinct surnames AND each candidate's first initial appears
     * as its own capitalised surface word — link the affiliation to each
     * such candidate so all the authors named in the cluster get covered.
     * Falls through to single-match semantics when only one candidate
     * matches by surname.
     */
    static List<Person> matchPersonsByCluster(List<LayoutToken> clusterTokens, List<Person> authors) {
        if (CollectionUtils.isEmpty(clusterTokens) || CollectionUtils.isEmpty(authors)) {
            return Collections.emptyList();
        }

        // Concatenate the span's raw token texts so hyphenated and multi-word
        // surnames (e.g. "Leyton-Brown", "Ojeda Valencia") survive intact.
        StringBuilder spanBuilder = new StringBuilder();
        for (LayoutToken tok : clusterTokens) {
            if (tok.getText() != null) {
                spanBuilder.append(tok.getText());
            }
        }
        String spanText = spanBuilder.toString().toLowerCase(Locale.ROOT);

        List<String> capWords = new ArrayList<>();
        for (LayoutToken tok : clusterTokens) {
            String text = tok.getText();
            if (StringUtils.isBlank(text)) {
                continue;
            }
            text = text.trim();
            if (text.isEmpty() || !Character.isLetter(text.charAt(0))) {
                continue;
            }
            // Capitalised surface words feed the first-initial fallback when
            // multiple authors share a surname.
            if (Character.isUpperCase(text.charAt(0))) {
                capWords.add(text);
            }
        }
        if (capWords.isEmpty()) {
            return Collections.emptyList();
        }
        String firstInitial = capWords.size() > 1 ? capWords.get(0).substring(0, 1) : null;

        // Primary match: Person.lastName appears in the span text. Strip
        // whitespace from both sides — spanText is built by concatenating
        // raw token texts (no inter-token spaces), so a surname like "van
        // Willigenburg" (with internal space) wouldn't otherwise match.
        String spanTextCompact = spanText.replaceAll("\\s+", "");
        List<Person> candidates = new ArrayList<>();
        for (Person aut : authors) {
            if (StringUtils.isBlank(aut.getLastName())) {
                continue;
            }
            String lnCompact = aut.getLastName().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
            if (spanTextCompact.contains(lnCompact)) {
                candidates.add(aut);
            }
        }
        if (candidates.size() <= 1) {
            return candidates;
        }

        // Multiple candidates: try to match each one's first initial against a
        // capitalised surface word in the span. Authors whose first initial is
        // present (and unique among candidates) are kept — the cluster covers
        // them all simultaneously, e.g. "M.D. Atkinson S.J. van Willigenburg".
        Set<String> capInitials = new HashSet<>();
        for (String word : capWords) {
            if (!word.isEmpty()) {
                capInitials.add(word.substring(0, 1).toUpperCase(Locale.ROOT));
            }
        }
        Map<String, List<Person>> byInitial = new HashMap<>();
        for (Person aut : candidates) {
            String fn = aut.getFirstName();
            if (StringUtils.isBlank(fn)) {
                continue;
            }
            String init = fn.substring(0, 1).toUpperCase(Locale.ROOT);
            byInitial.computeIfAbsent(init, k -> new ArrayList<>()).add(aut);
        }
        List<Person> matched = new ArrayList<>();
        for (Map.Entry<String, List<Person>> e : byInitial.entrySet()) {
            String init = e.getKey();
            List<Person> list = e.getValue();
            if (capInitials.contains(init) && list.size() == 1) {
                matched.add(list.get(0));
            }
        }
        if (!matched.isEmpty()) {
            return matched;
        }
        // Fallback: legacy single-author disambiguation by leading first
        // initial — preserves prior behaviour for spans that don't fit the
        // multi-author pattern.
        if (firstInitial != null) {
            List<Person> filtered = new ArrayList<>();
            for (Person aut : candidates) {
                if (StringUtils.isNotBlank(aut.getFirstName())
                        && aut.getFirstName().substring(0, 1).equalsIgnoreCase(firstInitial)) {
                    filtered.add(aut);
                }
            }
            if (filtered.size() == 1) {
                return filtered;
            }
        }
        return Collections.emptyList();
    }

    /**
     * Match authors to affiliations using structured markers extracted by the
     * name model ({@link Person#getMarkers()}) against {@link Affiliation#getMarker()}.
     * <p>
     * Multiple authors can share the same marker, and a single author can have
     * multiple markers. Markers are trimmed before comparison.
     */
    static void assignByDirectMarkers(List<Person> authors, List<Affiliation> affiliations) {
        // Build marker → affiliations index
        Map<String, List<Affiliation>> markerToAffs = new HashMap<>();
        for (Affiliation aff : affiliations) {
            if (StringUtils.isNotBlank(aff.getMarker())) {
                markerToAffs.computeIfAbsent(aff.getMarker().trim(), k -> new ArrayList<>()).add(aff);
            }
        }

        if (markerToAffs.isEmpty()) {
            LOGGER.debug("Direct marker matching: no affiliation markers, skipping");
            return;
        }

        boolean anyMatched = false;
        for (Person aut : authors) {
            if (CollectionUtils.isEmpty(aut.getMarkers())) {
                continue;
            }
            for (String marker : aut.getMarkers()) {
                if (StringUtils.isBlank(marker)) {
                    continue;
                }
                List<Affiliation> matched = markerToAffs.get(marker.trim());
                if (matched != null) {
                    for (Affiliation aff : matched) {
                        aut.addAffiliation(aff);
                        aff.setFailAffiliation(false);
                        anyMatched = true;
                        LOGGER.debug(
                                "Direct marker matching: author '{}' matched to affiliation '{}' via marker '{}'",
                                aut.getLastName(),
                                aff.getRawAffiliationString(),
                                marker);
                    }
                }
            }
        }

        if (!anyMatched) {
            LOGGER.debug("Direct marker matching: no person markers matched any affiliation markers");
        }
    }

    /**
     * Match authors to affiliations by searching for affiliation markers in the
     * original author string and finding the nearest author name by string
     * position.
     * <p>
     * This approach is robust to name model errors because it operates on the
     * raw concatenated author string rather than relying on structured marker
     * extraction from the name model.
     */
    static void assignByMarkers(List<Person> authors, List<Affiliation> affiliations, String originalAuthors) {
        if (StringUtils.isBlank(originalAuthors)) {
            LOGGER.debug("Marker matching: no originalAuthors string, skipping");
            return;
        }

        boolean hasMarker = false;
        for (Affiliation aff : affiliations) {
            if (aff.getMarker() != null) {
                hasMarker = true;
                break;
            }
        }

        if (!hasMarker) {
            LOGGER.debug("Marker matching: no affiliation markers found, skipping");
            return;
        }

        int indexAffiliation = 0;
        for (Affiliation aff : affiliations) {
            // circuit breaker
            if (indexAffiliation > 60)
                break;

            // skip affiliations already resolved by direct marker matching
            if (!aff.getFailAffiliation()) {
                indexAffiliation++;
                continue;
            }

            if (aff.getMarker() != null && aff.getMarker().length() > 0) {
                String marker = aff.getMarker();
                int from = 0;
                int ind = 0;
                ArrayList<Integer> winners = new ArrayList<>();
                while (ind != -1) {
                    ind = originalAuthors.indexOf(marker, from);

                    boolean bad = false;
                    if (ind != -1) {
                        // check for partial matches: single digit matching double digit,
                        // single special char matching double special char
                        if (marker.length() == 1) {
                            if (Character.isDigit(marker.charAt(0))) {
                                if (ind - 1 > 0) {
                                    if (Character.isDigit(originalAuthors.charAt(ind - 1))) {
                                        bad = true;
                                    }
                                }
                                if (ind + 1 < originalAuthors.length()) {
                                    if (Character.isDigit(originalAuthors.charAt(ind + 1))) {
                                        bad = true;
                                    }
                                }
                            } else if (Character.isLetter(marker.charAt(0))) {
                                if (ind - 1 > 0) {
                                    if (Character.isLetter(originalAuthors.charAt(ind - 1))) {
                                        bad = true;
                                    }
                                }
                                if (ind + 1 < originalAuthors.length()) {
                                    if (Character.isLetter(originalAuthors.charAt(ind + 1))) {
                                        bad = true;
                                    }
                                }
                            } else if (marker.charAt(0) == '*') {
                                if (ind - 1 > 0) {
                                    if (originalAuthors.charAt(ind - 1) == '*') {
                                        bad = true;
                                    }
                                }
                                if (ind + 1 < originalAuthors.length()) {
                                    if (originalAuthors.charAt(ind + 1) == '*') {
                                        bad = true;
                                    }
                                }
                            }
                        }
                        if (marker.length() == 2) {
                            // case with ** as marker
                            if ((marker.charAt(0) == '*') && (marker.charAt(1) == '*')) {
                                if (ind - 2 > 0) {
                                    if ((originalAuthors.charAt(ind - 1) == '*') &&
                                            (originalAuthors.charAt(ind - 2) == '*')) {
                                        bad = true;
                                    }
                                }
                                if (ind + 2 < originalAuthors.length()) {
                                    if ((originalAuthors.charAt(ind + 1) == '*') &&
                                            (originalAuthors.charAt(ind + 2) == '*')) {
                                        bad = true;
                                    }
                                }
                                if ((ind - 1 > 0) && (ind + 1 < originalAuthors.length())) {
                                    if ((originalAuthors.charAt(ind - 1) == '*') &&
                                            (originalAuthors.charAt(ind + 1) == '*')) {
                                        bad = true;
                                    }
                                }
                            }
                        }
                    }

                    if ((ind != -1) && !bad) {
                        // find the associated author name by proximity in string
                        String original = originalAuthors.toLowerCase();
                        int p = 0;
                        int best = -1;
                        int bestDistance = Integer.MAX_VALUE;
                        for (Person aut : authors) {
                            if (!winners.contains(Integer.valueOf(p))) {
                                String lastname = aut.getLastName();

                                if (lastname != null) {
                                    lastname = lastname.toLowerCase();
                                    int namePos = original.indexOf(lastname);
                                    if (namePos != -1) {
                                        int dist = Math.abs(ind - (namePos + lastname.length()));
                                        if (dist < bestDistance) {
                                            best = p;
                                            bestDistance = dist;
                                        }
                                    }
                                }
                            }
                            p++;
                        }

                        // associate this affiliation to the nearest author
                        if (best != -1) {
                            authors.get(best).addAffiliation(aff);
                            aff.setFailAffiliation(false);
                            winners.add(Integer.valueOf(best));
                            LOGGER.debug(
                                    "Marker matching: author '{}' matched to affiliation '{}' via marker '{}'",
                                    authors.get(best).getLastName(),
                                    aff.getRawAffiliationString(),
                                    marker);
                        }

                        from = ind + 1;
                    }
                    if ((ind != -1) && bad) {
                        from = ind + 1;
                        bad = false;
                    }

                    // circuit breaker
                    if (ind > originalAuthors.length() || ind > 1000)
                        break;
                }
            }
            indexAffiliation++;
        }
    }

    /**
     * Match remaining unmatched authors to affiliations using
     * coordinate proximity (distance between layout token centroids).
     * Multiple authors CAN share the same affiliation — each floating author
     * independently picks its nearest affiliation.
     */
    static void assignByProximity(List<Person> authors, List<Affiliation> affiliations) {
        List<Person> floatingAuthors = getFloatingAuthors(authors);

        if (floatingAuthors.isEmpty()) {
            LOGGER.debug("Proximity matching: no floating authors, skipping");
            return;
        }

        // compute centroids for floating authors (only those with layout tokens)
        Map<Person, double[]> authorCentroids = new LinkedHashMap<>();
        for (Person aut : floatingAuthors) {
            double[] centroid = computeCentroid(aut.getLayoutTokens());
            if (centroid != null) {
                authorCentroids.put(aut, centroid);
            }
        }

        // Only consider affiliations not yet claimed by an earlier tier.
        // When every affiliation has already been claimed (by preLink or marker
        // matching), a floating author's affiliation is genuinely missing from
        // the header (e.g. the HEADER model tagged it as <other>). Skip
        // assignment in that case — forcing the nearest claimed affiliation
        // onto an unrelated floating author is worse than leaving them empty.
        Map<Affiliation, double[]> affCentroids = new LinkedHashMap<>();
        for (Affiliation aff : affiliations) {
            if (!aff.getFailAffiliation()) {
                continue;
            }
            double[] centroid = computeCentroid(aff.getLayoutTokens());
            if (centroid != null) {
                affCentroids.put(aff, centroid);
            }
        }

        if (authorCentroids.isEmpty() || affCentroids.isEmpty()) {
            LOGGER.debug(
                    "Proximity matching: insufficient coordinates (authors={}, affs={}), skipping",
                    authorCentroids.size(),
                    affCentroids.size());
            return;
        }

        // Each floating author picks its nearest affiliation independently
        // from the candidate pool above (multiple authors can still share the
        // same affiliation — e.g. when none have been marker-matched).
        for (Map.Entry<Person, double[]> autEntry : authorCentroids.entrySet()) {
            Person aut = autEntry.getKey();
            double[] autCentroid = autEntry.getValue();

            Affiliation bestAff = null;
            double bestDist = Double.MAX_VALUE;

            for (Map.Entry<Affiliation, double[]> affEntry : affCentroids.entrySet()) {
                Affiliation aff = affEntry.getKey();
                double[] affCentroid = affEntry.getValue();
                double dist = distance(autCentroid, affCentroid);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestAff = aff;
                }
            }

            if (bestAff != null) {
                aut.addAffiliation(bestAff);
                bestAff.setFailAffiliation(false);
                LOGGER.debug(
                        "Proximity matching: author '{}' assigned to affiliation '{}' (distance={})",
                        aut.getLastName(),
                        bestAff.getRawAffiliationString(),
                        bestDist);
            }
        }
    }

    /**
     * Sequential fallback: last resort when no coordinates are available.
     * - If fewer remaining affiliations than authors → distribute all remaining
     * affiliations to all remaining authors
     * - Otherwise → 1:1 sequential assignment
     */
    static void assignBySequence(List<Person> authors, List<Affiliation> affiliations) {
        List<Person> floatingAuthors = getFloatingAuthors(authors);
        List<Affiliation> floatingAffiliations = getFloatingAffiliations(affiliations);

        if (floatingAuthors.isEmpty() || floatingAffiliations.isEmpty()) {
            return;
        }

        LOGGER.debug(
                "Sequential fallback: {} floating authors, {} floating affiliations",
                floatingAuthors.size(),
                floatingAffiliations.size());

        if (floatingAffiliations.size() < floatingAuthors.size()) {
            // Fewer affiliations than authors → distribute all to each author
            for (Person aut : floatingAuthors) {
                for (Affiliation aff : floatingAffiliations) {
                    aut.addAffiliation(aff);
                    aff.setFailAffiliation(false);
                }
            }
            LOGGER.debug(
                    "Sequential fallback: distributed {} affiliations to all {} floating authors",
                    floatingAffiliations.size(),
                    floatingAuthors.size());
        } else {
            // Equal or more affiliations than authors → 1:1 sequential
            int p = 0;
            for (Person aut : floatingAuthors) {
                if (p < floatingAffiliations.size()) {
                    aut.addAffiliation(floatingAffiliations.get(p));
                    floatingAffiliations.get(p).setFailAffiliation(false);
                    p++;
                }
            }
            LOGGER.debug("Sequential fallback: assigned {} affiliations 1:1 sequentially", p);
        }
    }

    /**
     * Compute the centroid (average X, average Y, page) of a list of layout
     * tokens.
     *
     * @return double array [x, y, page] or null if tokens are empty/null
     */
    static double[] computeCentroid(List<LayoutToken> tokens) {
        if (CollectionUtils.isEmpty(tokens)) {
            return null;
        }

        double sumX = 0, sumY = 0;
        int count = 0;
        int page = -1;

        for (LayoutToken token : tokens) {
            if (token.getY() > 0 || token.getX() > 0) {
                sumX += token.getX();
                sumY += token.getY();
                count++;
                if (page == -1) {
                    page = token.getPage();
                }
            }
        }

        if (count == 0) {
            return null;
        }

        return new double[]{sumX / count, sumY / count, page};
    }

    /**
     * Compute distance between two centroids.
     * If on different pages, adds a large penalty to prefer same-page matches.
     */
    static double distance(double[] centroid1, double[] centroid2) {
        double dx = centroid1[0] - centroid2[0];
        double dy = centroid1[1] - centroid2[1];
        double dist = Math.sqrt(dx * dx + dy * dy);

        // page penalty: different pages are much less likely to be related
        if ((int) centroid1[2] != (int) centroid2[2]) {
            dist += 10000.0;
        }

        return dist;
    }

    /**
     * Rescue orphan affiliations that remain unassigned after all primary strategies.
     * For each orphan affiliation, find the nearest author by coordinate proximity
     * and add the affiliation to that author. If no coordinates are available,
     * assign to the author with the fewest affiliations.
     */
    static void rescueOrphanAffiliations(List<Person> authors, List<Affiliation> affiliations) {
        List<Affiliation> orphans = getFloatingAffiliations(affiliations);
        if (orphans.isEmpty()) {
            return;
        }

        LOGGER.debug("Orphan rescue: {} affiliations still unassigned", orphans.size());

        for (Affiliation orphan : orphans) {
            double[] orphanCentroid = computeCentroid(orphan.getLayoutTokens());

            Person bestAuthor = null;

            if (orphanCentroid != null) {
                // Try proximity-based assignment
                double bestDist = Double.MAX_VALUE;
                for (Person aut : authors) {
                    double[] autCentroid = computeCentroid(aut.getLayoutTokens());
                    if (autCentroid != null) {
                        double dist = distance(autCentroid, orphanCentroid);
                        if (dist < bestDist) {
                            bestDist = dist;
                            bestAuthor = aut;
                        }
                    }
                }
            }

            if (bestAuthor == null) {
                // No coordinates — assign to author with fewest affiliations
                int minAffs = Integer.MAX_VALUE;
                for (Person aut : authors) {
                    int count = aut.getAffiliations() != null ? aut.getAffiliations().size() : 0;
                    if (count < minAffs) {
                        minAffs = count;
                        bestAuthor = aut;
                    }
                }
            }

            if (bestAuthor != null) {
                if (isContentDuplicate(orphan, bestAuthor.getAffiliations())) {
                    LOGGER.debug(
                            "Orphan rescue: skipping affiliation '{}' — content-equivalent to one already on '{}'",
                            orphan.getRawAffiliationString(),
                            bestAuthor.getLastName());
                    continue;
                }
                bestAuthor.addAffiliation(orphan);
                orphan.setFailAffiliation(false);
                LOGGER.debug(
                        "Orphan rescue: affiliation '{}' assigned to author '{}'",
                        orphan.getRawAffiliationString(),
                        bestAuthor.getLastName());
            }
        }
    }

    /**
     * True when {@code candidate}'s raw affiliation text is content-equivalent
     * to any affiliation in {@code existing} (after normalization, either string
     * is contained in the other). Used by {@link #rescueOrphanAffiliations} to
     * skip duplicates that arise when the HEADER model labels the
     * correspondence-line institution as {@code <affiliation>}, producing a
     * second {@link Affiliation} that mirrors the legitimate one.
     */
    static boolean isContentDuplicate(Affiliation candidate, List<Affiliation> existing) {
        if (CollectionUtils.isEmpty(existing) || candidate == null) {
            return false;
        }
        String c = normalizeForMatch(candidate.getRawAffiliationString());
        if (StringUtils.isBlank(c)) {
            return false;
        }
        for (Affiliation a : existing) {
            if (a == candidate) {
                continue;
            }
            String e = normalizeForMatch(a.getRawAffiliationString());
            if (StringUtils.isBlank(e)) {
                continue;
            }
            if (e.contains(c) || c.contains(e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normalize an affiliation raw string for substring-equivalence comparison:
     * lowercase, drop a leading marker label (digits or single letter followed
     * by space), drop "(...)" parentheticals (e.g. "(C2SM)"), and collapse
     * whitespace and punctuation to single spaces.
     */
    static String normalizeForMatch(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        String s = raw.toLowerCase(Locale.ROOT);
        s = s.replaceAll("\\([^)]*\\)", " ");
        s = s.replaceAll("^\\s*[\\d*†‡§¶a-z]{1,3}\\s+", "");
        s = s.replaceAll("[\\p{Punct}\\s]+", " ").trim();
        return s;
    }

    /**
     * Get authors that have no affiliations assigned yet.
     */
    private static List<Person> getFloatingAuthors(List<Person> authors) {
        List<Person> floating = new ArrayList<>();
        for (Person aut : authors) {
            if (CollectionUtils.isEmpty(aut.getAffiliations())) {
                floating.add(aut);
            }
        }
        return floating;
    }

    /**
     * Get affiliations that are still unassigned (failAffiliation == true).
     */
    private static List<Affiliation> getFloatingAffiliations(List<Affiliation> affiliations) {
        List<Affiliation> floating = new ArrayList<>();
        for (Affiliation aff : affiliations) {
            if (aff.getFailAffiliation()) {
                floating.add(aff);
            }
        }
        return floating;
    }
}
