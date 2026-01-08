package org.grobid.core.engines.tagging.delft;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-line demonstrator for DeLFT ONNX inference.
 * 
 * Usage:
 * java -jar delft-onnx.jar --model <model_dir> --embeddings <lmdb_path>
 * --embedding-size 300 --input "text to annotate"
 * 
 * For feature models, use --test-features to run with sample features.
 */
public class DeLFTDemo {

    public static void main(String[] args) {
        String modelDir = null;
        String embeddingsPath = null;
        int embeddingSize = 300;
        int maxSeqLength = 512;
        String inputText = null;
        String inputFile = null;

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--model":
                    modelDir = args[++i];
                    break;
                case "--embeddings":
                    embeddingsPath = args[++i];
                    break;
                case "--embedding-size":
                    embeddingSize = Integer.parseInt(args[++i]);
                    break;
                case "--max-seq-length":
                    maxSeqLength = Integer.parseInt(args[++i]);
                    break;
                case "--input":
                    inputText = args[++i];
                    break;
                case "--input-file":
                    inputFile = args[++i];
                    break;
                case "--help":
                    printUsage();
                    return;
            }
        }

        // If no direct input but input file provided, read from file
        List<String> inputTexts = new ArrayList<>();
        if (inputText != null) {
            inputTexts.add(inputText);
        } else if (inputFile != null) {
            try {
                inputTexts = readInputFile(inputFile);
                if (inputTexts.isEmpty()) {
                    System.err.println("Error: Input file is empty: " + inputFile);
                    System.exit(1);
                }
            } catch (IOException e) {
                System.err.println("Error reading input file: " + e.getMessage());
                System.exit(1);
            }
        }

        // Validate
        if (modelDir == null || embeddingsPath == null || inputTexts.isEmpty()) {
            System.err.println("Error: Missing required arguments");
            printUsage();
            System.exit(1);
        }

        try {
            // Load model
            System.err.println("Loading model from: " + modelDir);
            DeLFTOnnxModel model = new DeLFTOnnxModel(
                    Paths.get(modelDir),
                    Paths.get(embeddingsPath),
                    embeddingSize,
                    maxSeqLength);

            // Print model info
            System.err.println("Model has features: " + model.hasFeatures());
            if (model.hasFeatures()) {
                System.err.println("Number of features: " + model.getNumFeatures());
            }

            // Run inference for each input
            System.err.println("Processing " + inputTexts.size() + " input(s)");

            System.out.println("["); // Start JSON array
            for (int i = 0; i < inputTexts.size(); i++) {
                String text = inputTexts.get(i);
                System.err.println("Processing: " + text);
                DeLFTOnnxModel.AnnotationResult result;

                if (model.hasFeatures()) {
                    // Feature model: tokenize and generate sample features
                    String[] tokens = text.split("\\s+");
                    String[][] features = createSampleDateFeatures(tokens);
                    System.err.println("Generated sample features for " + tokens.length + " tokens");
                    result = model.annotateTokens(tokens, features);
                } else {
                    result = model.annotate(text);
                }

                // Print result as JSON
                System.out.print(result.toJson());
                if (i < inputTexts.size() - 1) {
                    System.out.println(",");
                } else {
                    System.out.println();
                }
            }
            System.out.println("]"); // End JSON array

            // Clean up
            model.close();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Create sample features for date model testing.
     * 
     * Features for date model include:
     * - Line position (LINESTART, LINEIN, LINEEND)
     * - Capitalization (ALLCAP, INITCAP, NOCAPS)
     * - Digit pattern (ALLDIGIT, CONTAINSDIGITS, NODIGIT)
     * - Boolean flags (0/1)
     * - Punctuation (COMMA, DOT, HYPHEN, NOPUNCT, etc.)
     */
    private static String[][] createSampleDateFeatures(String[] tokens) {
        int numFeatures = 7; // Date model has 7 features
        String[][] features = new String[tokens.length][numFeatures];

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];

            // Feature 0: Line position
            if (i == 0) {
                features[i][0] = "LINESTART";
            } else if (i == tokens.length - 1) {
                features[i][0] = "LINEEND";
            } else {
                features[i][0] = "LINEIN";
            }

            // Feature 1: Capitalization
            if (token.equals(token.toUpperCase()) && token.matches(".*[A-Z].*")) {
                features[i][1] = "ALLCAP";
            } else if (Character.isUpperCase(token.charAt(0))) {
                features[i][1] = "INITCAP";
            } else {
                features[i][1] = "NOCAPS";
            }

            // Feature 2: Digit pattern
            if (token.matches("\\d+")) {
                features[i][2] = "ALLDIGIT";
            } else if (token.matches(".*\\d.*")) {
                features[i][2] = "CONTAINSDIGITS";
            } else {
                features[i][2] = "NODIGIT";
            }

            // Feature 3-5: Boolean flags (default to 0)
            features[i][3] = "0";
            features[i][4] = "0";
            features[i][5] = "0";

            // Feature 6: Punctuation
            if (token.equals(",")) {
                features[i][6] = "COMMA";
            } else if (token.equals(".")) {
                features[i][6] = "DOT";
            } else if (token.equals("-")) {
                features[i][6] = "HYPHEN";
            } else if (token.matches("[()\\[\\]]")) {
                features[i][6] = token.matches("[\\(\\[]") ? "OPENBRACKET" : "ENDBRACKET";
            } else if (token.matches("\\p{Punct}")) {
                features[i][6] = "PUNCT";
            } else {
                features[i][6] = "NOPUNCT";
            }
        }

        return features;
    }

    /**
     * Read input text from a file as a single sequence.
     * All lines are concatenated with spaces.
     */
    private static List<String> readInputFile(String filePath) throws IOException {
        List<String> texts = new ArrayList<>();
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    if (content.length() > 0) {
                        content.append(" ");
                    }
                    content.append(line);
                }
            }
        }
        if (content.length() > 0) {
            texts.add(content.toString());
        }
        return texts;
    }

    private static void printUsage() {
        System.out.println("DeLFT ONNX Inference Demo");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar delft-onnx.jar [options]");
        System.out.println();
        System.out.println("Required Options:");
        System.out.println("  --model <path>          Path to exported model directory");
        System.out.println("  --embeddings <path>     Path to LMDB embeddings database");
        System.out.println("  --input <text>          Text to annotate (or use --input-file)");
        System.out.println("  --input-file <path>     File with input texts (one per line)");
        System.out.println();
        System.out.println("Optional:");
        System.out.println("  --embedding-size <n>    Embedding dimension (default: 300)");
        System.out.println("  --max-seq-length <n>    Max sequence length (default: 512)");
        System.out.println("  --help                  Show this help");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  java -jar delft-onnx.jar \\");
        System.out.println("    --model ./exported_models/date-features \\");
        System.out.println("    --embeddings ./data/db/glove-840B \\");
        System.out.println("    --input \"December 25, 2024\"");
        System.out.println();
        System.out.println("  Or with input file:");
        System.out.println("  java -jar delft-onnx.jar \\");
        System.out.println("    --model ./exported_models/header-BidLSTM_CRF \\");
        System.out.println("    --embeddings ./data/db/glove-840B \\");
        System.out.println("    --input-file ./header_samples.txt");
        System.out.println();
        System.out.println("Note: For feature models (BidLSTM_CRF_FEATURES), sample features are");
        System.out.println("      auto-generated for demo purposes.");
    }
}
