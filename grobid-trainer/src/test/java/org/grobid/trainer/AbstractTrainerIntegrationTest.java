package org.grobid.trainer;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.GrobidModels;
import org.grobid.core.utilities.GrobidProperties;

public class AbstractTrainerIntegrationTest {

    private AbstractTrainer target;

    @BeforeClass
    public static void init() {
        GrobidProperties.getInstance();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        //        LibraryLoader.load();
    }

    @Before
    public void setUp() throws Exception {
        target = new AbstractTrainer(GrobidModels.DUMMY) {

            @Override
            public int createCRFPPData(
                    File corpusPath,
                    File outputTrainingFile,
                    File outputEvalFile,
                    double splitRatio) {
                // the file for writing the training data
                //                if (outputTrainingFile != null) {
                //                    try (OutputStream os = new FileOutputStream(outputTrainingFile)) {
                //                        try (Writer writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                //
                //                            for (int i = 0; i < 100; i++) {
                //                                double random = Math.random();
                //                                writer.write("blablabla" + random);
                //                                writer.write("\n");
                //                                if (i % 10 == 0) {
                //                                    writer.write("\n");
                //                                }
                //                            }
                //                        }
                //                    } catch (IOException e) {
                //                        e.printStackTrace();
                //                    }
                //                }

                return 100;
            }
        };
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testLoad_shouldWork() throws Exception {
        Path path = Paths.get("src/test/resources/sample.wapiti.output.date.txt");
        List<String> expected = Arrays.asList(
                "Available available A Av Ava Avai e le ble able LINESTART INITCAP NODIGIT 0 0 0 NOPUNCT I-<other>\n"
                        +
                        "online online o on onl onli e ne ine line LINEIN NOCAPS NODIGIT 0 0 0 NOPUNCT <other>\n"
                        +
                        "18 18 1 18 18 18 8 18 18 18 LINEIN NOCAPS ALLDIGIT 0 0 0 NOPUNCT I-<day>\n"
                        +
                        "January january J Ja Jan Janu y ry ary uary LINEIN INITCAP NODIGIT 0 0 1 NOPUNCT I-<month>\n"
                        +
                        "2010 2010 2 20 201 2010 0 10 010 2010 LINEEND NOCAPS ALLDIGIT 0 1 0 NOPUNCT I-<year>",
                "June june J Ju Jun June e ne une June LINESTART INITCAP NODIGIT 0 0 1 NOPUNCT I-<month>\n"
                        +
                        "16 16 1 16 16 16 6 16 16 16 LINEIN NOCAPS ALLDIGIT 0 0 0 NOPUNCT I-<day>\n"
                        +
                        ", , , , , , , , , , LINEIN ALLCAP NODIGIT 1 0 0 COMMA I-<other>\n"
                        +
                        "2008 2008 2 20 200 2008 8 08 008 2008 LINEEND NOCAPS ALLDIGIT 0 1 0 NOPUNCT I-<year>",
                "November november N No Nov Nove r er ber mber LINESTART INITCAP NODIGIT 0 0 1 NOPUNCT I-<month>\n"
                        +
                        "4 4 4 4 4 4 4 4 4 4 LINEIN NOCAPS ALLDIGIT 1 0 0 NOPUNCT I-<day>\n"
                        +
                        ", , , , , , , , , , LINEIN ALLCAP NODIGIT 1 0 0 COMMA I-<other>\n"
                        +
                        "2009 2009 2 20 200 2009 9 09 009 2009 LINEEND NOCAPS ALLDIGIT 0 1 0 NOPUNCT I-<year>",
                "Published published P Pu Pub Publ d ed hed shed LINESTART INITCAP NODIGIT 0 0 0 NOPUNCT I-<other>\n"
                        +
                        "18 18 1 18 18 18 8 18 18 18 LINEIN NOCAPS ALLDIGIT 0 0 0 NOPUNCT I-<day>\n"
                        +
                        "May may M Ma May May y ay May May LINEIN INITCAP NODIGIT 0 0 1 NOPUNCT I-<month>\n"
                        +
                        "2011 2011 2 20 201 2011 1 11 011 2011 LINEEND NOCAPS ALLDIGIT 0 1 0 NOPUNCT I-<year>");

        List<String> loadedTrainingData = target.load(path);

        assertThat(loadedTrainingData, is(expected));
    }

    @Test
    public void testSplitNFold_n3_shouldWork() throws Exception {
        List<String> dummyTrainingData = new ArrayList<>();
        dummyTrainingData.add(dummyExampleGeneration("1", 3));
        dummyTrainingData.add(dummyExampleGeneration("2", 4));
        dummyTrainingData.add(dummyExampleGeneration("3", 2));
        dummyTrainingData.add(dummyExampleGeneration("4", 6));
        dummyTrainingData.add(dummyExampleGeneration("5", 6));
        dummyTrainingData.add(dummyExampleGeneration("6", 2));
        dummyTrainingData.add(dummyExampleGeneration("7", 2));
        dummyTrainingData.add(dummyExampleGeneration("8", 3));
        dummyTrainingData.add(dummyExampleGeneration("9", 3));
        dummyTrainingData.add(dummyExampleGeneration("10", 3));

        List<ImmutablePair<String, String>> splitMapping = target.splitNFold(dummyTrainingData, 3);
        assertThat(splitMapping, hasSize(3));

        assertThat(splitMapping.get(0).getLeft(), endsWith("train"));
        assertThat(splitMapping.get(0).getRight(), endsWith("test"));

        //Fold 1
        List<String> fold1Training = target.load(Paths.get(splitMapping.get(0).getLeft()));
        List<String> fold1Evaluation = target.load(Paths.get(splitMapping.get(0).getRight()));

        System.out.println(Arrays.toString(fold1Training.toArray()));
        System.out.println(Arrays.toString(fold1Evaluation.toArray()));

        assertThat(fold1Training, hasSize(7));
        assertThat(fold1Evaluation, hasSize(3));

        //Fold 2
        List<String> fold2Training = target.load(Paths.get(splitMapping.get(1).getLeft()));
        List<String> fold2Evaluation = target.load(Paths.get(splitMapping.get(1).getRight()));

        System.out.println(Arrays.toString(fold2Training.toArray()));
        System.out.println(Arrays.toString(fold2Evaluation.toArray()));

        assertThat(fold2Training, hasSize(7));
        assertThat(fold2Evaluation, hasSize(3));

        //Fold 3
        List<String> fold3Training = target.load(Paths.get(splitMapping.get(2).getLeft()));
        List<String> fold3Evaluation = target.load(Paths.get(splitMapping.get(2).getRight()));

        System.out.println(Arrays.toString(fold3Training.toArray()));
        System.out.println(Arrays.toString(fold3Evaluation.toArray()));

        assertThat(fold3Training, hasSize(6));
        assertThat(fold3Evaluation, hasSize(4));

        // Cleanup
        splitMapping.stream().forEach(f -> {
            try {
                Files.delete(Paths.get(f.getRight()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        splitMapping.stream().forEach(f -> {
            try {
                Files.delete(Paths.get(f.getLeft()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSplitNFold_n10_shouldThrowException() throws Exception {
        List<String> dummyTrainingData = new ArrayList<>();
        dummyTrainingData.add(dummyExampleGeneration("1", 3));
        dummyTrainingData.add(dummyExampleGeneration("2", 4));
        dummyTrainingData.add(dummyExampleGeneration("3", 2));
        dummyTrainingData.add(dummyExampleGeneration("4", 6));

        List<ImmutablePair<String, String>> splitMapping = target.splitNFold(dummyTrainingData, 10);

    }

    private String dummyExampleGeneration(String exampleId, int total) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < total; i++) {
            sb.append("line " + i + " example " + exampleId).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    @Test
    public void testLoadAndShuffle_shouldWork() throws Exception {
        Path path = Paths.get("src/test/resources/sample.wapiti.output.date.txt");
        List<String> orderedTrainingData = Arrays.asList(
                "Available available A Av Ava Avai e le ble able LINESTART INITCAP NODIGIT 0 0 0 NOPUNCT I-<other>\n"
                        +
                        "online online o on onl onli e ne ine line LINEIN NOCAPS NODIGIT 0 0 0 NOPUNCT <other>\n"
                        +
                        "18 18 1 18 18 18 8 18 18 18 LINEIN NOCAPS ALLDIGIT 0 0 0 NOPUNCT I-<day>\n"
                        +
                        "January january J Ja Jan Janu y ry ary uary LINEIN INITCAP NODIGIT 0 0 1 NOPUNCT I-<month>\n"
                        +
                        "2010 2010 2 20 201 2010 0 10 010 2010 LINEEND NOCAPS ALLDIGIT 0 1 0 NOPUNCT I-<year>",
                "June june J Ju Jun June e ne une June LINESTART INITCAP NODIGIT 0 0 1 NOPUNCT I-<month>\n"
                        +
                        "16 16 1 16 16 16 6 16 16 16 LINEIN NOCAPS ALLDIGIT 0 0 0 NOPUNCT I-<day>\n"
                        +
                        ", , , , , , , , , , LINEIN ALLCAP NODIGIT 1 0 0 COMMA I-<other>\n"
                        +
                        "2008 2008 2 20 200 2008 8 08 008 2008 LINEEND NOCAPS ALLDIGIT 0 1 0 NOPUNCT I-<year>",
                "November november N No Nov Nove r er ber mber LINESTART INITCAP NODIGIT 0 0 1 NOPUNCT I-<month>\n"
                        +
                        "4 4 4 4 4 4 4 4 4 4 LINEIN NOCAPS ALLDIGIT 1 0 0 NOPUNCT I-<day>\n"
                        +
                        ", , , , , , , , , , LINEIN ALLCAP NODIGIT 1 0 0 COMMA I-<other>\n"
                        +
                        "2009 2009 2 20 200 2009 9 09 009 2009 LINEEND NOCAPS ALLDIGIT 0 1 0 NOPUNCT I-<year>",
                "Published published P Pu Pub Publ d ed hed shed LINESTART INITCAP NODIGIT 0 0 0 NOPUNCT I-<other>\n"
                        +
                        "18 18 1 18 18 18 8 18 18 18 LINEIN NOCAPS ALLDIGIT 0 0 0 NOPUNCT I-<day>\n"
                        +
                        "May may M Ma May May y ay May May LINEIN INITCAP NODIGIT 0 0 1 NOPUNCT I-<month>\n"
                        +
                        "2011 2011 2 20 201 2011 1 11 011 2011 LINEEND NOCAPS ALLDIGIT 0 1 0 NOPUNCT I-<year>");

        List<String> shuffledTrainingData = target.loadAndShuffle(path);

        assertThat(shuffledTrainingData, hasSize(orderedTrainingData.size()));
        assertThat(shuffledTrainingData, is(not(orderedTrainingData)));
    }

}
