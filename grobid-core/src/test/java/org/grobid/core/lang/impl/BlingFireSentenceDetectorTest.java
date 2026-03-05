package org.grobid.core.lang.impl;

import org.grobid.core.utilities.OffsetPosition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class BlingFireSentenceDetectorTest {

    private final BlingFireSentenceDetector detector = new BlingFireSentenceDetector();

    @Test
    public void testDetect_singleSentence() {
        String text = "This is a single sentence.";
        List<OffsetPosition> result = detector.detect(text);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).start, is(0));
        assertThat(result.get(0).end, is(text.length()));
    }

    @Test
    public void testDetect_multipleSentences() {
        String text = "First sentence. Second sentence. Third sentence.";
        List<OffsetPosition> result = detector.detect(text);

        assertThat(result, hasSize(3));
        assertThat(text.substring(result.get(0).start, result.get(0).end), is("First sentence."));
        assertThat(text.substring(result.get(1).start, result.get(1).end), is("Second sentence."));
        assertThat(text.substring(result.get(2).start, result.get(2).end), is("Third sentence."));
    }

    @Test
    public void testDetect_emptyText() {
        List<OffsetPosition> result = detector.detect("");
        assertThat(result, is(empty()));
    }

    @Test
    public void testDetect_nullText() {
        List<OffsetPosition> result = detector.detect(null);
        assertThat(result, is(empty()));
    }

    @Test
    public void testDetect_sentenceWithAbbreviations() {
        String text = "Dr. Smith went to Washington. He arrived at 3 p.m. yesterday.";
        List<OffsetPosition> result = detector.detect(text);

        // BlingFire should handle abbreviations, expecting 2 sentences
        assertThat(result, hasSize(2));
    }

    @Test
    public void testDetect_longScientificText_noOutOfBounds() {
        String text = "Lung cancer is the most common cause of cancer--related death worldwide, " +
            "estimated to be responsible for nearly one in five cancer--related deaths (1). " +
            "Epigenetic changes have been implicated during the early stages of carcinogenesis and clonal expansion (2,3) " +
            "and several DNA methylation changes have been recently identified in relation to lung cancer risk (4--6). " +
            "Interestingly, many of these identified methylation changes have also been associated with smoking, " +
            "the most well--established risk factor for lung cancer. Therefore, DNA methylation may serve as a mediator " +
            "of the influence of smoking on lung cancer, as previously reported (4); as an independent risk factor; " +
            "or alternatively as a non--causal biomarker. Given the plasticity of epigenetic markers in response to " +
            "modifiable exposures, any DNA methylation changes that are causally linked to lung cancer are potentially " +
            "appealing targets for intervention (7,8). However, these epigenetic markers are sensitive to reverse causation, " +
            "being affected by cancer processes (8), and are also prone to confounding, for example by socio--economic and " +
            "lifestyle factors (9,10). Epigenome--wide association studies (EWAS) for lung cancer have recently been conducted, " +
            "with genome--wide DNA methylation measured using the Illumina Infinium\u00ae Human Methylation 450 BeadChip on DNA " +
            "extracted from pre--diagnostic, peripheral blood samples (4,5). The prospective design of these studies minimizes " +
            "the potential impact of reverse causation, although it is possible that latent cancer undiagnosed at the time of " +
            "blood draw impacted peripheral methylation changes in these individuals. One site that has been identified is " +
            "cg05575921, within the aryl hydrocarbon receptor (AHRR) gene, which has been consistently replicated in relation " +
            "to both smoking (11) and lung cancer (4,5,12). Functional evidence suggests that this region could be causally " +
            "involved in lung cancer (13). However, the observed association between methylation and lung cancer might simply " +
            "reflect separate effects of smoking on lung cancer and DNA methylation, i.e. the association may be a result of " +
            "confounding (14), including residual confounding after adjustment for self--reported smoking behaviour (15,16). " +
            "One alternative approach to assess whether an association between exposure and disease reflects causation is the " +
            "use of Mendelian randomization (MR) (17,18). MR uses genetic variants robustly associated with modifiable factors " +
            "as instruments to infer causality between the modifiable factor and disease outcome, overcoming most of unmeasured " +
            "or residual confounding and reverse causation. MR may be adapted to the setting of DNA methylation (19--21) with " +
            "the use of methylation quantitative trait loci (mQTLs), which are genetic variants that strongly correlate with " +
            "the methylation state of nearby CpG sites (22). The degree of association of these mQTLs with lung cancer may be " +
            "used to shed light upon the potential causal role of DNA methylation on lung cancer incidence. In this study, we " +
            "performed a meta--analysis of four lung cancer EWAS nested within prospective cohorts to identify CpG sites at " +
            "which differential methylation is associated with lung cancer risk and applied Mendelian randomization to investigate " +
            "whether the observed DNA methylation changes are causally linked to lung cancer.";

        List<OffsetPosition> result = detector.detect(text);

        assertThat(result, is(not(empty())));
        // Verify no offset exceeds text length
        for (OffsetPosition pos : result) {
            assertThat(pos.start, greaterThanOrEqualTo(0));
            assertThat(pos.end, lessThanOrEqualTo(text.length()));
        }
        // Verify we can safely substring every sentence
        for (OffsetPosition pos : result) {
            String sentence = text.substring(pos.start, pos.end);
            assertThat(sentence.isEmpty(), is(false));
        }
    }
}
