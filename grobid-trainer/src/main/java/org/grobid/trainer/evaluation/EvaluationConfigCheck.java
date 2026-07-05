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
package org.grobid.trainer.evaluation;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.grobid.core.engines.tagging.GrobidCRFEngine;
import org.grobid.core.utilities.Consolidation.GrobidConsolidationService;
import org.grobid.core.utilities.GrobidProperties;

/**
 * Pre-flight validation of the GROBID configuration used by the end-to-end
 * evaluation ({@code jatsEval}/{@code teiEval}, driven by {@link EndToEndEvaluation}).
 * <p>
 * The evaluation tasks load the default {@code grobid-home/config/grobid.yaml}
 * through the {@link GrobidProperties} singleton, so this checker reads the exact
 * same configuration the evaluation will use. It fails fast (exit code 1) so that a
 * misconfigured run is caught before spending hours processing the gold corpora.
 * <p>
 * It verifies that:
 * <ul>
 *   <li>consolidation is set to biblio-glutton and its URL is reachable, and</li>
 *   <li>the models for which Deep Learning beats CRF are set to {@code engine: "delft"}.</li>
 * </ul>
 * The required-DeLFT model set below is the one recommended in
 * {@code doc/Deep-Learning-models.md}.
 */
public class EvaluationConfigCheck {

    /**
     * Models for which {@code doc/Deep-Learning-models.md} recommends DeLFT over CRF
     * for benchmarking (DeLFT yields a significantly better score).
     */
    private static final String[] REQUIRED_DELFT_MODELS = {
            "citation",
            "affiliation-address",
            "reference-segmenter",
            "header",
            "funding-acknowledgement"
    };

    /**
     * biblio-glutton liveness endpoint, appended to the configured glutton base URL.
     * biblio-glutton has no {@code /service/isalive} route; {@code /service/data} is a
     * no-parameter endpoint that always answers 200 and returns index statistics, so it
     * doubles as a health probe that confirms the databases are loaded.
     */
    private static final String GLUTTON_HEALTH_PATH = "/service/data";

    private static final int GLUTTON_PING_TIMEOUT_MS = 5000;

    /**
     * @param args optionally {@code "warn"} to only report problems without failing
     *             (exit code stays 0); any other/no argument means hard-fail on problems.
     */
    public static void main(String[] args) {
        boolean warnOnly = args.length > 0 && "warn".equalsIgnoreCase(args[0]);

        GrobidProperties.getInstance();
        System.out.println(">>>>>>>> GROBID_HOME=" + GrobidProperties.getGrobidHome());

        List<String> problems = new ArrayList<>();

        checkConsolidation(problems);
        checkDelftModels(problems);

        System.out.println();
        if (problems.isEmpty()) {
            System.out.println(
                    "[OK] grobid.yaml is correctly configured for end-to-end evaluation "
                            + "(biblio-glutton reachable, DeLFT models enabled).");
            System.exit(0);
        }

        System.err.println("The GROBID configuration is NOT ready for end-to-end evaluation:");
        for (String problem : problems) {
            System.err.println("  - " + problem);
        }
        System.err.println();
        System.err.println(
                "Fix grobid-home/config/grobid.yaml (or copy the ready-made "
                        + "grobid-home/config/grobid-evaluation.yaml preset over it).");
        System.err.println(
                "See doc/End-to-end-evaluation.md, doc/Consolidation.md and "
                        + "doc/Deep-Learning-models.md.");

        if (warnOnly) {
            System.err.println();
            System.err.println("Continuing anyway (warn-only mode).");
            System.exit(0);
        }
        System.exit(1);
    }

    /**
     * Verify that consolidation is set to biblio-glutton, that a glutton URL is
     * configured, and that the glutton service actually answers its health endpoint.
     */
    private static void checkConsolidation(List<String> problems) {
        GrobidConsolidationService service = GrobidProperties.getConsolidationService();
        System.out.println("Consolidation service: " + (service == null ? "(none)" : service.getExt()));
        if (service != GrobidConsolidationService.GLUTTON) {
            problems.add(
                    "consolidation.service is '"
                            + (service == null ? "(none)" : service.getExt())
                            + "', expected 'glutton' (biblio-glutton) for evaluation.");
            return;
        }

        String gluttonUrl = GrobidProperties.getGluttonUrl();
        System.out.println("Glutton URL: " + (gluttonUrl == null ? "(none)" : gluttonUrl));
        if (StringUtils.isBlank(gluttonUrl)) {
            problems.add("consolidation.glutton.url is not set.");
            return;
        }

        String isAliveUrl = StringUtils.removeEnd(gluttonUrl, "/") + GLUTTON_HEALTH_PATH;
        if (!isReachable(isAliveUrl)) {
            problems.add(
                    "biblio-glutton is not reachable at "
                            + isAliveUrl
                            + " (server down, wrong URL, or network issue).");
        } else {
            System.out.println("Glutton reachable: yes (" + isAliveUrl + ")");
        }
    }

    /**
     * Verify that each doc-recommended model resolves to the DeLFT engine. This relies
     * on {@link GrobidProperties#getGrobidEngine(String)}, which already applies the
     * flavor prefix-fallback, so it reflects the engine the evaluation will actually use.
     */
    private static void checkDelftModels(List<String> problems) {
        System.out.println();
        System.out.println("Model engines (must be 'delft'):");
        for (String model : REQUIRED_DELFT_MODELS) {
            GrobidCRFEngine engine;
            try {
                engine = GrobidProperties.getGrobidEngine(model);
            } catch (Exception e) {
                problems.add("model '" + model + "': cannot resolve engine (" + e.getMessage() + ").");
                continue;
            }
            System.out.println("  " + model + " -> " + engine.getExt());
            if (engine != GrobidCRFEngine.DELFT) {
                problems.add(
                        "model '"
                                + model
                                + "' uses engine '"
                                + engine.getExt()
                                + "', expected 'delft'.");
            }
        }
    }

    /** GET the given URL and return true if the server answers with a 2xx status. */
    private static boolean isReachable(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(GLUTTON_PING_TIMEOUT_MS);
            connection.setReadTimeout(GLUTTON_PING_TIMEOUT_MS);
            int status = connection.getResponseCode();
            return status >= 200 && status < 300;
        } catch (IOException e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
