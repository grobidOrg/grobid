package org.grobid.core.engines.tagging.delft;

import ai.onnxruntime.OrtException;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.features.FeaturesVectorHeader;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.GrobidProperties;
import org.junit.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assume.assumeTrue;

/**
 * Integration test for Header ONNX model.
 * 
 * This test verifies that the ONNX-based header model can be loaded and
 * run inference correctly. It helps diagnose issues with:
 * - ONNX Runtime native library loading
 * - LMDB embeddings compatibility
 * - CRF decoder functionality
 * - End-to-end model inference
 * 
 * Prerequisites:
 * - ONNX header model at grobid-home/models/header-BidLSTM_CRF_FEATURES.onnx/
 * - Embeddings preloaded using: python3
 * grobid-home/scripts/preload_embeddings.py --embedding glove-840B
 */
public class HeaderOnnxIntegrationTest {

    private static final String ARCHITECTURE = "BidLSTM_CRF_FEATURES";

    private static Path modelPath;
    private static Path embeddingsPath;
    private DeLFTOnnxModel model;

    @BeforeClass
    public static void setUpClass() {
        // Initialize GROBID properties
        GrobidProperties.getInstance();

        // Get model path
        String modelName = GrobidModels.HEADER.getModelName();
        String grobidHome = GrobidProperties.getGrobidHome().getAbsolutePath();
        modelPath = Path.of(grobidHome, "models", modelName + "-" + ARCHITECTURE + ".onnx");

        // Get embeddings path
        String delftPath = GrobidProperties.getDeLFTFilePath();
        embeddingsPath = Path.of(delftPath, "data", "db", "glove-840B");
    }

    @Before
    public void setUp() throws IOException, OrtException {
        // Skip test if model is not available
        assumeTrue("ONNX model not found at " + modelPath +
                ". Please ensure the ONNX header model is installed.",
                Files.exists(modelPath) && Files.isDirectory(modelPath));

        // Skip test if embeddings are not available
        assumeTrue("Embeddings not found at " + embeddingsPath +
                ". Please run: python3 grobid-home/scripts/preload_embeddings.py --embedding glove-840B",
                Files.exists(embeddingsPath) && Files.isDirectory(embeddingsPath));

        // Load model
        model = new DeLFTOnnxModel(modelPath);
    }

    @After
    public void tearDown() {
        if (model != null) {
            model.close();
        }
    }

    @Test
    public void testModelCanBeLoaded() {
        assertThat(model, is(notNullValue()));
        assertThat("Model should have features", model.hasFeatures(), is(true));
        assertThat("Model should have > 0 features", model.getNumFeatures(), greaterThan(0));
    }

    @Test
    public void testMaxSequenceLength() {
        int maxSeqLength = model.getMaxSeqLength();
        assertThat("Max sequence length should be positive", maxSeqLength, greaterThan(0));
    }

    @Test
    public void testAnnotateSimpleHeader() throws OrtException {
        String input = "Deep Learning for Natural Language Processing John Smith MIT";

        List<LayoutToken> allTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<LayoutToken> filtered = allTokens.stream()
            .filter( token -> StringUtils.isNotBlank(token.getText()))
            .collect(Collectors.toList());

        String[] words = new String[filtered.size()];
        String[][] features = new String[filtered.size()][model.getNumFeatures()];
        for (int i = 0; i < filtered.size(); i++) {
            words[i] = filtered.get(i).getText();
            FeaturesVectorHeader featuresVectorHeader = FeaturesVectorHeader.fromLayoutToken(filtered.get(i));
            features[i] = featuresVectorHeader.printVector().split("\n");
        }

        DeLFTOnnxModel.AnnotationResult result = model.annotateTokens(words, features);

        assertThat(result, is(notNullValue()));
        assertThat(result.getTokens(), is(notNullValue()));
        assertThat(result.getLabels(), is(notNullValue()));
        assertThat("Tokens and labels should have same length",
                result.getTokens().length, is(result.getLabels().length));
        assertThat(result.getLabels().length, greaterThan(0));

        long otherLabel = Arrays.stream(result.getLabels()).filter(v -> v.equalsIgnoreCase("<other>")).count();

        assertThat(otherLabel, lessThan((long)result.getLabels().length));
    }

    @Test
    public void testLabelGrobidInput() {
        // Create a simple GROBID-formatted input with features
        // Format: token\tfeature1\tfeature2\t...
        StringBuilder input = new StringBuilder();

        // Simulate header features (token + 22 features based on config.json)
        // Features indices 9-30 = 22 features
        String[] tokens = { "Deep", "Learning", "for", "NLP" };
        for (String token : tokens) {
            input.append(token);
            // Add dummy features (GROBID uses various binary and categorical features)
            for (int i = 0; i < 22; i++) {
                input.append("\t").append("NOFEAT");
            }
            input.append("\n");
        }

        String result = model.labelGrobidInput(input.toString());

        assertThat(result, is(notNullValue()));
        assertThat("Result should not be empty", result.length(), greaterThan(0));

        // Verify that each line has the original content plus a label
        String[] resultLines = result.trim().split("\n");
        assertThat("Result should have same number of lines as input",
                resultLines.length, is(tokens.length));

        for (String line : resultLines) {
            String[] parts = line.split("\t");
            // Should have: token + 22 features + 1 label = 24 parts
            assertThat("Each line should have at least 24 parts (token + features + label)",
                    parts.length, greaterThan(23));
        }
    }

    @Test
    public void testAnnotateTokensWithFeatures() throws OrtException {
        // Test token annotation with explicit features
        String[] tokens = { "Analysis", "of", "Cancer", "Genomes" };
        // 22 features per token
        String[][] features = new String[tokens.length][22];
        for (int i = 0; i < tokens.length; i++) {
            for (int j = 0; j < 22; j++) {
                features[i][j] = "NOFEAT";
            }
        }

        DeLFTOnnxModel.AnnotationResult result = model.annotateTokens(tokens, features);

        assertThat(result, is(notNullValue()));
        assertThat(result.getLabels().length, is(tokens.length));

        assertThat(result.getLabels().length, greaterThan(0));

        long otherLabel = Arrays.stream(result.getLabels()).filter(v -> v.equalsIgnoreCase("<other>")).count();

        assertThat(otherLabel, lessThan((long)result.getLabels().length));
    }

    @Test
    public void testLabelMultipleSequences() {
        // Test with multiple sequences separated by empty lines
        StringBuilder input = new StringBuilder();

        // First sequence
        input.append(FeaturesVectorHeader.fromLayoutToken(new LayoutToken("Title")).printVector());
        input.append(FeaturesVectorHeader.fromLayoutToken(new LayoutToken("Text")).printVector());
        input.append("\n"); // Empty line = sequence separator

        // Second sequence
        input.append(FeaturesVectorHeader.fromLayoutToken(new LayoutToken("Author")).printVector());
        input.append(FeaturesVectorHeader.fromLayoutToken(new LayoutToken("Name")).printVector());

        String result = model.labelGrobidInput(input.toString());

        assertThat(result, is(notNullValue()));
        // Result should contain labeled tokens
        assertThat("Result should contain labeled output", result.length(), greaterThan(0));
    }
}
