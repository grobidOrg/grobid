package org.grobid.core.lexicon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.PDFAnnotation;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.OffsetPosition;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class LexiconTest {
    @Test
    public void testCharacterPositionsUrlPattern_URL_shouldReturnCorrectInterval() throws Exception {
        final String input = "This work was distributed on http:// github.com/myUsername/MyProject";
        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<OffsetPosition> offsetPositions = Lexicon.characterPositionsUrlPattern(tokenisedInput);

        assertThat(offsetPositions, hasSize(1));
        OffsetPosition FirstURL = offsetPositions.get(0);
        assertThat(input.substring(FirstURL.start, FirstURL.end), is("http:// github.com/myUsername/MyProject"));
    }

    @Test
    public void testTokenPositionsUrlPattern_URL_shouldReturnCorrectInterval() throws Exception {
        final String input = "This work was distributed on http:// github.com/myUsername/MyProject";
        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<OffsetPosition> offsetPositions = Lexicon.tokenPositionsUrlPattern(tokenisedInput);

        assertThat(offsetPositions, hasSize(1));
        OffsetPosition FirstURL = offsetPositions.get(0);
        //Note: The intervals returned by the method Utilities.convertStringOffsetToTokenOffset
        // consider the upper index to be included, while java consider the upper index to be excluded
        assertThat(LayoutTokensUtil.toText(tokenisedInput.subList(FirstURL.start, FirstURL.end + 1)), is("http:// github.com/myUsername/MyProject"));
    }

    @Test
    public void testCharacterPositionsUrlPattern_URLStartingWithWWW_shouldReturnCorrectInterval() throws Exception {
        final String input = "This work was distributed on www. github.com/myUsername/MyProject";
        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<OffsetPosition> offsetPositions = Lexicon.characterPositionsUrlPattern(tokenisedInput);

        assertThat(offsetPositions, hasSize(1));
        OffsetPosition FirstURL = offsetPositions.get(0);
        assertThat(input.substring(FirstURL.start, FirstURL.end), is("www. github.com/myUsername/MyProject"));
    }

    @Test
    public void testCharacterPositionsUrlPattern_URLStartingWithHTTPS_shouldReturnCorrectInterval() throws Exception {
        final String input = "This work was distributed on https:// www.github.com/myUsername/MyProject";
        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<OffsetPosition> offsetPositions = Lexicon.characterPositionsUrlPattern(tokenisedInput);

        assertThat(offsetPositions, hasSize(1));
        OffsetPosition FirstURL = offsetPositions.get(0);
        assertThat(input.substring(FirstURL.start, FirstURL.end), is("https:// www.github.com/myUsername/MyProject"));
    }

    /**
     * This test is to confirm the limitation of this method using the regex, where we prefer failing on some cases
     * rather than have a lot of false positive. This method will be anyway complemented with the annotated links in
     * the PDF (if available).
     */
    @Test
    public void testCharacterPositionsUrlPattern_URLTruncated_shouldReturnCorrectIntervalWithmissingPartOfURL() throws Exception {
        final String input = "This work was distributed on https://www. github.com/myUsername/MyProject";
        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<OffsetPosition> offsetPositions = Lexicon.characterPositionsUrlPattern(tokenisedInput);

        assertThat(offsetPositions, hasSize(1));
        OffsetPosition FirstURL = offsetPositions.get(0);
        assertThat(input.substring(FirstURL.start, FirstURL.end), is("https://www"));
    }

    @Test
    @Ignore("This test will fail, it can be used to test a real case when updating the regular expression")
    public void testCharacterPositionsUrlPattern_URL_shouldReturnCorrectInterval_2() throws Exception {
        final String input = "720 137409 The Government of Lao PDR 2005 Forestry Strategy to the year 2020 of the Lao PDR (available at: https://faolex.fao.org/ docs/pdf/lao144178.pdf)";
        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<OffsetPosition> offsetPositions = Lexicon.characterPositionsUrlPattern(tokenisedInput);

        assertThat(offsetPositions, hasSize(1));
        OffsetPosition url = offsetPositions.get(0);
        assertThat(input.substring(url.start, url.end), is("https://faolex.fao.org/ docs/pdf/lao144178.pdf"));
    }

    @Test
    public void testCharacterPositionsUrlPatternWithPDFAnnotations_URL_shouldReturnCorrectInterval() throws Exception {
        final String input = "1. 'internal status' indicates that their records should be \n" +
            "hidden in the interface. \n" +
            "2. In our previous work [1] we reported 77.03% F1-\n" +
            "score. There is a slight decrease in absolute scores \n" +
            "between DeLFT 0.2.8 and DeLFT 0.3.0. One cause \n" +
            "may be the use of different hyperparameters in \n" +
            "version 0.3.0 such as batch size and learning rate. \n" +
            "However, the most probable cause could be the \n" +
            "impact of using the Huggingface tokenizers \n" +
            "library which is suffering from quality issues \n" +
            "https://github.com/kermitt2/delft/issues/150. \n" +
            "\n" +
            "\n";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        LayoutToken lastToken = tokenisedInput.get(205);
        lastToken.setY(707.634);
        lastToken.setX(247.92652222222222);
        lastToken.setWidth(11.972533333333333);
        lastToken.setHeight(8.778);
        lastToken.setPage(10);

        //This is the actual text that is passed and is different from the layoutToken text.
        final String inputReal = "1. 'internal status' indicates that their records should be " +
            "hidden in the interface. " +
            "2. In our previous work [1] we reported 77.03% F1score. " +
            "There is a slight decrease in absolute scores between DeLFT 0.2.8 and DeLFT 0.3.0. " +
            "One cause may be the use of different hyperparameters in version 0.3.0 such as batch " +
            "size and learning rate. However, the most probable cause could be the impact of " +
            "using the Huggingface tokenizers library which is suffering from quality issues " +
            "https://github.com/kermitt2/delft/issues/150. ";

        PDFAnnotation annotation = new PDFAnnotation();
        annotation.setPageNumber(10);
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        boundingBoxes.add(BoundingBox.fromPointAndDimensions(10, 84.30, 706.68, 177.39, 9.52));
        annotation.setBoundingBoxes(boundingBoxes);
        annotation.setDestination("https://github.com/kermitt2/delft/issues/150");
        annotation.setType(PDFAnnotation.Type.URI);

        List<PDFAnnotation> pdfAnnotations = List.of(annotation);
        List<Pair<OffsetPosition, String>> positionsAndDestinations = Lexicon.characterPositionsUrlPatternWithPdfAnnotations(tokenisedInput, pdfAnnotations);

        assertThat(positionsAndDestinations, hasSize(1));
        OffsetPosition url = positionsAndDestinations.get(0).getLeft();
        assertThat(StringUtils.substring(input, url.start, url.end), is("https://github.com/kermitt2/delft/issues/150"));
        assertThat(positionsAndDestinations.get(0).getRight(), is("https://github.com/kermitt2/delft/issues/150"));
    }

    @Test
    public void testCharacterPositionsUrlPatternWithPDFAnnotations_URL_NoPDFAnnotationAvailable_shouldReturnCorrectInterval() throws Exception {
        final String input = "Data was analyzed using SPM8 software (http://www.fil.ion.ucl.ac.uk/spm). Images were \n" +
            "\n" +
            "spatially aligned to the first volume to correct for small movements; no run showed more than \n" +
            "\n" +
            "4mm displacement along the x, y or z dimension. Sinc interpolation minimized timing-errors \n" +
            "\n" +
            "between slices; functional images were coregistered to the anatomical image, normalized to the \n" +
            "\n" +
            "standard T1 Montreal Neurological Institute (MNI) template, and resliced at 4mm 3 resolution. \n" +
            "\n";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<PDFAnnotation> pdfAnnotations = new ArrayList<>();
        List<Pair<OffsetPosition, String>> positionsAndDestinations = Lexicon.characterPositionsUrlPatternWithPdfAnnotations(tokenisedInput, pdfAnnotations);

        assertThat(positionsAndDestinations, hasSize(1));
        OffsetPosition url = positionsAndDestinations.get(0).getLeft();
        assertThat(StringUtils.substring(input, url.start, url.end), is("http://www.fil.ion.ucl.ac.uk/spm"));
        assertThat(positionsAndDestinations.get(0).getRight(), is(nullValue()));
    }


    @Test
    public void testCharacterPositionsUrlPatternWithPDFAnnotations_URL_shouldReturnCorrectInterval2() throws Exception {
        final String input = "This work is available at https://github.com/lfoppiano/ \n" +
            "supercon2. The repository contains the code of the \n" +
            "SuperCon 2 interface, the curation workflow, and the \n" +
            "\n" +
            "Table 2. Data support, the number of entities for each label in \n" +
            "each of the datasets used for evaluating the ML models. The \n" +
            "base dataset is the original dataset described in [18], and the \n" +
            "curation dataset is automatically collected based on the data-\n" +
            "base corrections by the interface and manually corrected. \n" +
            "\n";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        LayoutToken lastTokenOfTheURL = tokenisedInput.get(19);
        lastTokenOfTheURL.setPage(9);
        lastTokenOfTheURL.setX(530.9363448275863);
        lastTokenOfTheURL.setY(538.153);
        lastTokenOfTheURL.setWidth(4.363655172413793);
        lastTokenOfTheURL.setHeight(9.702);

        PDFAnnotation annotation = new PDFAnnotation();
        annotation.setPageNumber(9);
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        boundingBoxes.add(BoundingBox.fromPointAndDimensions(9, 408.76, 537.11, 126.54, 10.49));
        annotation.setBoundingBoxes(boundingBoxes);
        annotation.setDestination("https://github.com/lfoppiano/supercon2");
        annotation.setType(PDFAnnotation.Type.URI);

        List<PDFAnnotation> pdfAnnotations = List.of(annotation);
        List<Pair<OffsetPosition, String>> positionsAndDestinations = Lexicon.characterPositionsUrlPatternWithPdfAnnotations(tokenisedInput, pdfAnnotations);

        assertThat(positionsAndDestinations, hasSize(1));
        OffsetPosition url = positionsAndDestinations.get(0).getLeft();
        assertThat(input.substring(url.start, url.end), is("https://github.com/lfoppiano/ \nsupercon2"));
        assertThat(positionsAndDestinations.get(0).getRight(), is("https://github.com/lfoppiano/supercon2"));
    }

    @Test
    public void testTokensPositionsUrlPatternWithPDFAnnotations_URL_shouldReturnCorrectInterval2() throws Exception {
        final String input = "This work is available at https://github.com/lfoppiano/ \n" +
            "supercon2. The repository contains the code of the \n" +
            "SuperCon 2 interface, the curation workflow, and the \n" +
            "\n" +
            "Table 2. Data support, the number of entities for each label in \n" +
            "each of the datasets used for evaluating the ML models. The \n" +
            "base dataset is the original dataset described in [18], and the \n" +
            "curation dataset is automatically collected based on the data-\n" +
            "base corrections by the interface and manually corrected. \n" +
            "\n";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        for (int i = 10; i < 23; i++) {
            LayoutToken token = tokenisedInput.get(i);
            token.setPage(9);
            token.setX(530.9363448275863);
            token.setY(538.153);
            token.setWidth(4.363655172413793);
            token.setHeight(9.702);
        }

        PDFAnnotation annotation = new PDFAnnotation();
        annotation.setPageNumber(9);
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        boundingBoxes.add(BoundingBox.fromPointAndDimensions(9, 408.76, 537.11, 126.54, 10.49));
        annotation.setBoundingBoxes(boundingBoxes);
        annotation.setDestination("https://github.com/lfoppiano/supercon2");
        annotation.setType(PDFAnnotation.Type.URI);

        List<PDFAnnotation> pdfAnnotations = List.of(annotation);
        List<Pair<OffsetPosition, String>> offsetPositions = Lexicon.tokenPositionUrlPatternWithPdfAnnotations(tokenisedInput, pdfAnnotations);

        assertThat(offsetPositions, hasSize(1));
        OffsetPosition url = offsetPositions.get(0).getLeft();
        // LF: we need a + 1 because the convention for the tokenPositionUrlPattern is inclusive, inclusive
        assertThat(LayoutTokensUtil.toText(tokenisedInput.subList(url.start, url.end + 1)), is("https://github.com/lfoppiano/ \nsupercon2"));
        assertThat(offsetPositions.get(0).getRight(), is("https://github.com/lfoppiano/supercon2"));
    }

    @Test
    public void testCharacterPositionsUrlPatternWithPDFAnnotations_URL_shouldReturnCorrectIntervalBasedOnText() throws Exception {
        final String input = "This work is available at https://github.com/lfoppiano/ \n" +
            "supercon2. The repository contains the code of the \n" +
            "SuperCon 2 interface, the curation workflow, and the \n" +
            "\n" +
            "Table 2. Data support, the number of entities for each label in \n" +
            "each of the datasets used for evaluating the ML models. The \n" +
            "base dataset is the original dataset described in [18], and the \n" +
            "curation dataset is automatically collected based on the data-\n" +
            "base corrections by the interface and manually corrected. \n" +
            "\n";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        for (int i = 10; i < 23; i++) {
            LayoutToken token = tokenisedInput.get(i);
            token.setPage(9);
            token.setX(530.9363448275863);
            token.setY(538.153);
            token.setWidth(4.363655172413793);
            token.setHeight(9.702);
        }
//        LayoutToken lastTokenOfTheURL = tokenisedInput.get(19);
//        lastTokenOfTheURL.setPage(9);
//        lastTokenOfTheURL.setX(530.9363448275863);
//        lastTokenOfTheURL.setY(538.153);
//        lastTokenOfTheURL.setWidth(4.363655172413793);
//        lastTokenOfTheURL.setHeight(9.702);

        PDFAnnotation annotation = new PDFAnnotation();
        annotation.setPageNumber(9);
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        boundingBoxes.add(BoundingBox.fromPointAndDimensions(9, 408.76, 537.11, 126.54, 10.49));
        annotation.setBoundingBoxes(boundingBoxes);
        annotation.setDestination("https://github.com/lfoppiano/supercon2");
        annotation.setType(PDFAnnotation.Type.URI);
        List<PDFAnnotation> pdfAnnotations = List.of(annotation);

        //This is the actual text that is passed and is different from the layoutToken text.
        final String inputReal = "This work is available at https://github.com/lfoppiano/ supercon2. The repository contains the code of the SuperCon 2 interface, the curation workflow, and the Table 2. Data support, the number of entities for each label in each of the datasets used for evaluating the ML models. The base dataset is the original dataset described in [18], and the curation dataset is automatically collected based on the database corrections by the interface and manually corrected. ";

        List<OffsetPosition> offsetPositions = Lexicon.characterPositionsUrlPatternWithPdfAnnotations(tokenisedInput, pdfAnnotations, inputReal);

        assertThat(offsetPositions, hasSize(1));
        OffsetPosition url = offsetPositions.get(0);
        assertThat(inputReal.substring(url.start, url.end), is("https://github.com/lfoppiano/ supercon2"));
    }

    @Test
    public void testCharacterPositionsUrlPatternWithPDFAnnotations2_URL_shouldReturnCorrectIntervalBasedOnText() throws Exception {
        final String input = "Table S1: Gene annotations from which exon-exon junctions were extracted and unioned to obtain \n" +
            "a list of annotated junctions. All tracks were taken from the UCSC Genome Browser [10] except for \n" +
            "GENCODE [2], which was downloaded from the GENCODE website http://www.gencodegenes. \n" +
            "org/releases/. Junction coordinates from hg38 annotations were lifted over to hg19 before the \n" +
            "union was performed. Of all gene annotations listed here, the Swedish Bioinformatics Institute \n" +
            "(SIB) genes has the most, with over 400,000 junctions for each of hg19 and hg38. \n";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        for (int i = 91; i < 105; i++) {
            LayoutToken token = tokenisedInput.get(i);
            token.setPage(2);
            token.setX(451.809);
            token.setY(391.933);
            token.setWidth(12.939499999999999);
            token.setHeight(6.279);
        }

        PDFAnnotation annotation = new PDFAnnotation();
        annotation.setPageNumber(2);
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        boundingBoxes.add(BoundingBox.fromPointAndDimensions(2, 450.81, 390.881, 79.61700000000002, 8.283000000000015));
        boundingBoxes.add(BoundingBox.fromPointAndDimensions(2, 303.174, 399.386, 38.622000000000014, 8.274000000000001));
        annotation.setBoundingBoxes(boundingBoxes);
        annotation.setDestination("http://www.gencodegenes.org/releases/");
        annotation.setType(PDFAnnotation.Type.URI);
        List<PDFAnnotation> pdfAnnotations = List.of(annotation);

        //This is the actual text that is passed and is different from the layoutToken text.
        final String inputReal = "Table S1: Gene annotations from which exon-exon junctions were extracted and unioned to obtain a list of annotated junctions. All tracks were taken from the UCSC Genome Browser [10] except for GENCODE [2], which was downloaded from the GENCODE website http://www.gencodegenes. org/releases/. Junction coordinates from hg38 annotations were lifted over to hg19 before the union was performed. Of all gene annotations listed here, the Swedish Bioinformatics Institute (SIB) genes has the most, with over 400,000 junctions for each of hg19 and hg38.  ";

        List<OffsetPosition> offsetPositions = Lexicon.characterPositionsUrlPatternWithPdfAnnotations(tokenisedInput, pdfAnnotations, inputReal);

        assertThat(offsetPositions, hasSize(1));
        OffsetPosition url = offsetPositions.get(0);
        assertThat(inputReal.substring(url.start, url.end), is("http://www.gencodegenes. org/releases/"));
    }


    @Test
    public void testCharacterPositionsUrlPatternWithPDFAnnotations_URL_shouldReturnCorrectIntervalBasedOnText2() throws Exception {
        final String input = "Opportunities (International Rice Research Institute) \n" +
            "(available at: http://lad.nafri.org.la/fulltext/231-0.pdf) \n" +
            "Salinas-Melgoza M A, Skutsch M, Lovett J C and Borrego A \n" +
            "2017 Carbon emissions from dryland shifting cultivation: a \n" +
            "case study of Mexican tropical dry forest Silva Fenn. \n" +
            "51 1553 \n";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        LayoutToken lastTokenOfTheURL1 = tokenisedInput.get(37);
        lastTokenOfTheURL1.setPage(11);
        lastTokenOfTheURL1.setX(519.4089069767441);
        lastTokenOfTheURL1.setY(733.461);
        lastTokenOfTheURL1.setWidth(9.03006976744186);
        lastTokenOfTheURL1.setHeight(7.5199);

        LayoutToken lastTokenOfTheURL2 = tokenisedInput.get(38);
        lastTokenOfTheURL2.setPage(11);
        lastTokenOfTheURL2.setX(519.4089069767441);
        lastTokenOfTheURL2.setY(733.461);
        lastTokenOfTheURL2.setWidth(9.03006976744186);
        lastTokenOfTheURL2.setHeight(7.5199);

        PDFAnnotation annotation = new PDFAnnotation();
        annotation.setPageNumber(11);
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        boundingBoxes.add(BoundingBox.fromPointAndDimensions(11, 402.018, 732.661, 126.47100000000006, 8.480000000000018));
        annotation.setBoundingBoxes(boundingBoxes);
        annotation.setDestination("http://lad.nafri.org.la/fulltext/231-0.pdf");
        annotation.setType(PDFAnnotation.Type.URI);
        List<PDFAnnotation> pdfAnnotations = List.of(annotation);

        List<Pair<OffsetPosition, String>> positionsAndDestinations = Lexicon.characterPositionsUrlPatternWithPdfAnnotations(tokenisedInput, pdfAnnotations);

        assertThat(positionsAndDestinations, hasSize(1));
        OffsetPosition url = positionsAndDestinations.get(0).getLeft();
        assertThat(input.substring(url.start, url.end), is("http://lad.nafri.org.la/fulltext/231-0.pdf"));
        assertThat(positionsAndDestinations.get(0).getRight(), is("http://lad.nafri.org.la/fulltext/231-0.pdf"));
    }


    @Test
    public void testCharacterPositionsUrlPatternWithPDFAnnotations_URL_shouldReturnCorrectIntervalBasedOnText3() throws Exception {
        final String input = "). The Laos official forest change \n" +
            "maps (https://nfms.maf.gov.la/) are created from the \n" +
            "land cover classification maps from the start year and \n" +
            "end year for each period (see the periods in table ";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        LayoutToken lastTokenOfTheURL = tokenisedInput.get(28);
        lastTokenOfTheURL.setPage(9);
        lastTokenOfTheURL.setX(461.1686153846154);
        lastTokenOfTheURL.setY(653.148);
        lastTokenOfTheURL.setWidth(3.9706923076923077);
        lastTokenOfTheURL.setHeight(9.3999);

        LayoutToken beforeLastTokenOfTheURL = tokenisedInput.get(27);
        beforeLastTokenOfTheURL.setPage(9);
        beforeLastTokenOfTheURL.setX(453.2272307692308);
        beforeLastTokenOfTheURL.setY(653.148);
        beforeLastTokenOfTheURL.setWidth(7.9413846153846155);
        beforeLastTokenOfTheURL.setHeight(9.3999);

        LayoutToken afterLastTokenOfTheURL = tokenisedInput.get(29);
        afterLastTokenOfTheURL.setPage(9);
        afterLastTokenOfTheURL.setX(453.2272307692308);
        afterLastTokenOfTheURL.setY(653.148);
        afterLastTokenOfTheURL.setWidth(7.9413846153846155);
        afterLastTokenOfTheURL.setHeight(9.3999);

        PDFAnnotation annotation = new PDFAnnotation();
        annotation.setPageNumber(9);
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        boundingBoxes.add(BoundingBox.fromPointAndDimensions(9, 369.582, 652.149, 95.82900000000001, 10.600000000000023));
        annotation.setBoundingBoxes(boundingBoxes);
        annotation.setDestination("https://nfms.maf.gov.la/");
        annotation.setType(PDFAnnotation.Type.URI);
        List<PDFAnnotation> pdfAnnotations = List.of(annotation);

        List<Pair<OffsetPosition, String>> positionsAndDestinations = Lexicon.characterPositionsUrlPatternWithPdfAnnotations(tokenisedInput, pdfAnnotations);

        assertThat(positionsAndDestinations, hasSize(1));
        OffsetPosition url = positionsAndDestinations.get(0).getLeft();
        assertThat(input.substring(url.start, url.end), is("https://nfms.maf.gov.la/"));
        assertThat(positionsAndDestinations.get(0).getRight(), is("https://nfms.maf.gov.la/"));

    }


    @Test
    public void testCharacterPositionsUrlPatternWithPDFAnnotations_URL_shouldReturnCorrectIntervalBasedOnText4() throws Exception {
        final String input = "Google Earth Engine applications to visualize the \n" +
            "datasets: https://github.com/shijuanchen/shift_cult \n" +
            "Map products visualization: https://sites.google. \n" +
            "com/view/shijuanchen/research/shift_cult \n";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        LayoutToken lastTokenOfTheURL1 = tokenisedInput.get(28);
        lastTokenOfTheURL1.setPage(10);
        lastTokenOfTheURL1.setX(504.75295121951217);
        lastTokenOfTheURL1.setY(626.353);
        lastTokenOfTheURL1.setWidth(40.858048780487806);
        lastTokenOfTheURL1.setHeight(9.3999);

        LayoutToken lastTokenOfTheURL2 = tokenisedInput.get(44);
        lastTokenOfTheURL2.setPage(10);
        lastTokenOfTheURL2.setX(526.9964666666667);
        lastTokenOfTheURL2.setY(638.853);
        lastTokenOfTheURL2.setWidth(22.0712);
        lastTokenOfTheURL2.setHeight(9.3999);

        PDFAnnotation annotation1 = new PDFAnnotation();
        annotation1.setPageNumber(10);
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        boundingBoxes.add(BoundingBox.fromPointAndDimensions(10, 378.093, 625.354, 167.51799999999997, 10.599999999999909));
        annotation1.setBoundingBoxes(boundingBoxes);
        annotation1.setDestination("https://github.com/shijuanchen/shift_cult");
        annotation1.setType(PDFAnnotation.Type.URI);

        PDFAnnotation annotation2 = new PDFAnnotation();
        annotation2.setPageNumber(10);
        List<BoundingBox> boundingBoxes2 = new ArrayList<>();
        boundingBoxes2.add(BoundingBox.fromPointAndDimensions(10, 475.497, 637.854, 77.26, 10.60));
        annotation2.setBoundingBoxes(boundingBoxes2);
        annotation2.setDestination("https://sites.google.com/view/shijuanchen/research/shift_cult");
        annotation2.setType(PDFAnnotation.Type.URI);
        List<PDFAnnotation> pdfAnnotations = List.of(annotation1, annotation2);

        List<Pair<OffsetPosition, String>> positionsAndDestinations = Lexicon.characterPositionsUrlPatternWithPdfAnnotations(tokenisedInput, pdfAnnotations);

        assertThat(positionsAndDestinations, hasSize(2));
        OffsetPosition url0 = positionsAndDestinations.get(0).getLeft();
        assertThat(input.substring(url0.start, url0.end), is("https://github.com/shijuanchen/shift_cult"));
        assertThat(positionsAndDestinations.get(0).getRight(), is("https://github.com/shijuanchen/shift_cult"));
        OffsetPosition url1 = positionsAndDestinations.get(1).getLeft();
        assertThat(input.substring(url1.start, url1.end), is("https://sites.google. \ncom/view/shijuanchen/research/shift_cult"));
        assertThat(positionsAndDestinations.get(1).getRight(), is("https://sites.google.com/view/shijuanchen/research/shift_cult"));
    }

    @Test
    public void testCharacterPositionsUrlPatternWithPDFAnnotations_DuplicatedMatchingPDFAnnotations_shouldReturnCorrectIntervalBasedOnText4() throws Exception {
        final String input = "Google Earth Engine applications to visualize the \n" +
            "datasets: https://github.com/shijuanchen/shift_cult \n" +
            "Map products visualization: https://sites.google. \n" +
            "com/view/shijuanchen/research/shift_cult \n";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        LayoutToken lastTokenOfTheURL1 = tokenisedInput.get(28);
        lastTokenOfTheURL1.setPage(10);
        lastTokenOfTheURL1.setX(504.75295121951217);
        lastTokenOfTheURL1.setY(626.353);
        lastTokenOfTheURL1.setWidth(40.858048780487806);
        lastTokenOfTheURL1.setHeight(9.3999);

        LayoutToken lastTokenOfTheURL2 = tokenisedInput.get(44);
        lastTokenOfTheURL2.setPage(10);
        lastTokenOfTheURL2.setX(526.9964666666667);
        lastTokenOfTheURL2.setY(638.853);
        lastTokenOfTheURL2.setWidth(22.0712);
        lastTokenOfTheURL2.setHeight(9.3999);

        PDFAnnotation annotation1 = new PDFAnnotation();
        annotation1.setPageNumber(10);
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        boundingBoxes.add(BoundingBox.fromPointAndDimensions(10, 378.093, 625.354, 167.51799999999997, 10.599999999999909));
        annotation1.setBoundingBoxes(boundingBoxes);
        annotation1.setDestination("https://github.com/shijuanchen/shift_cult");
        annotation1.setType(PDFAnnotation.Type.URI);

        PDFAnnotation annotation2 = new PDFAnnotation();
        annotation2.setPageNumber(10);
        List<BoundingBox> boundingBoxes2 = new ArrayList<>();
        boundingBoxes2.add(BoundingBox.fromPointAndDimensions(10, 475.497, 637.854, 77.26, 10.60));
        annotation2.setBoundingBoxes(boundingBoxes2);
        annotation2.setDestination("https://www.google.com");
        annotation2.setType(PDFAnnotation.Type.URI);

        PDFAnnotation annotation3 = new PDFAnnotation();
        annotation3.setPageNumber(10);
        List<BoundingBox> boundingBoxes3 = new ArrayList<>();
        boundingBoxes3.add(BoundingBox.fromPointAndDimensions(10, 475.497, 637.854, 77.26, 10.60));
        annotation3.setBoundingBoxes(boundingBoxes3);
        annotation3.setDestination("https://sites.google.com/view/shijuanchen/research/shift_cult");
        annotation3.setType(PDFAnnotation.Type.URI);

        List<PDFAnnotation> pdfAnnotations = List.of(annotation1, annotation2, annotation3);

        List<Pair<OffsetPosition, String>> positionsAndDestinations = Lexicon.characterPositionsUrlPatternWithPdfAnnotations(tokenisedInput, pdfAnnotations);

        assertThat(positionsAndDestinations, hasSize(2));
        OffsetPosition url0 = positionsAndDestinations.get(0).getLeft();
        assertThat(input.substring(url0.start, url0.end), is("https://github.com/shijuanchen/shift_cult"));
        assertThat(positionsAndDestinations.get(0).getRight(), is("https://github.com/shijuanchen/shift_cult"));
        OffsetPosition url1 = positionsAndDestinations.get(1).getLeft();
        assertThat(input.substring(url1.start, url1.end), is("https://sites.google. \ncom/view/shijuanchen/research/shift_cult"));
        assertThat(positionsAndDestinations.get(1).getRight(), is("https://sites.google.com/view/shijuanchen/research/shift_cult"));
    }


    @Test
    public void testCharacterPositionsUrlPatternWithPDFAnnotations_URL_shouldReturnCorrectIntervalBasedOnText5() throws Exception {
        final String input = ", accessible through the University of Hawaii Sea Level Center with station ID of UHSLC ID 57 \n" +
            "(https://uhslc.soest.hawaii.edu/stations/?stn=057#levels). You can access a processed dataset of nearshore wave \n";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        LayoutToken lastTokenOfTheURL1 = tokenisedInput.get(53);
        lastTokenOfTheURL1.setPage(6);
        lastTokenOfTheURL1.setX(334.9306551724138);
        lastTokenOfTheURL1.setY(719.076);
        lastTokenOfTheURL1.setWidth(21.514758620689655);
        lastTokenOfTheURL1.setHeight(9.2001);

        LayoutToken lastTokenOfTheURL2 = tokenisedInput.get(54);
        lastTokenOfTheURL2.setPage(6);
        lastTokenOfTheURL2.setX(356.4454137931035);
        lastTokenOfTheURL2.setY(719.076);
        lastTokenOfTheURL2.setWidth(3.585793103448276);
        lastTokenOfTheURL2.setHeight(9.2001);

        LayoutToken lastTokenOfTheURL3 = tokenisedInput.get(55);
        lastTokenOfTheURL3.setPage(54);
        lastTokenOfTheURL3.setX(360.0312068965518);
        lastTokenOfTheURL3.setY(719.076);
        lastTokenOfTheURL3.setWidth(3.585793103448276);
        lastTokenOfTheURL3.setHeight(9.2001);

        PDFAnnotation annotation1 = new PDFAnnotation();
        annotation1.setPageNumber(6);
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        boundingBoxes.add(BoundingBox.fromPointAndDimensions(6, 158.784, 716.0, 199.524, 12.275999999999954));
        annotation1.setBoundingBoxes(boundingBoxes);
        annotation1.setDestination("https://uhslc.soest.hawaii.edu/stations/?stn=057#levels");
        annotation1.setType(PDFAnnotation.Type.URI);

        List<PDFAnnotation> pdfAnnotations = List.of(annotation1);

        List<Pair<OffsetPosition, String>> positionsAndDestinations = Lexicon.characterPositionsUrlPatternWithPdfAnnotations(tokenisedInput, pdfAnnotations);

        assertThat(positionsAndDestinations, hasSize(1));
        OffsetPosition url0 = positionsAndDestinations.get(0).getLeft();
        assertThat(input.substring(url0.start, url0.end), is("https://uhslc.soest.hawaii.edu/stations/?stn=057#levels"));
        assertThat(positionsAndDestinations.get(0).getRight(), is("https://uhslc.soest.hawaii.edu/stations/?stn=057#levels"));
    }

    @Test
    public void testGetTokenPosition() throws Exception {

        //NOTE LF: The current behaviour will return -1 if the tokens are not matching with the positions
        // of the characters
        //Here the url is https://paperpile.com/c/QlNkzH/Hj7c+4D5e but because `Lameness` is attached the last token
        // is `Hj7c+4D5eLameness` which will cause troubles.

        String input = "https://paperpile.com/c/QlNkzH/Hj7c+4D5eLameness";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        OffsetPosition tokenPositions = Lexicon.getTokenPositions(40, 48, tokens);

        assertThat(tokenPositions.start, is(-1));
        assertThat(tokenPositions.end, is(-1));
    }

    @Test
    public void testGetTokenPosition2() throws Exception {

        String input = "https://doi.org/10.1038/s41586-024-07891-2.";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        OffsetPosition tokenPositions = Lexicon.getTokenPositions(0, 42, tokens);

        assertThat(tokenPositions.start, is(0));
        //NOTE: when doing sublist of the output of this method the end is non-inclusive, so a +1 is needed
        assertThat(tokenPositions.end, is(18));
    }

    @Test
    public void testGetTokenPosition3() throws Exception {

        String input = "(https://doi.org/10.1038/s41586-024-07891-2.";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        OffsetPosition tokenPositions = Lexicon.getTokenPositions(1, 43, tokens);

        assertThat(tokenPositions.start, is(1));
        //NOTE: when doing sublist of the output of this method the end is non-inclusive, so a +1 is needed
        assertThat(tokenPositions.end, is(19));
    }

    @Test
    public void testGetTokenPosition4() throws Exception {

        String input = "(https://doi.org/10.1038/s41586-024-07891-2.";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        OffsetPosition tokenPositions = Lexicon.getTokenPositions(1, 43, tokens);

        assertThat(tokenPositions.start, is(1));
        //NOTE: when doing sublist of the output of this method the end is non-inclusive, so a +1 is needed
        assertThat(tokenPositions.end, is(19));
        assertThat(LayoutTokensUtil.toText(tokens.subList(tokenPositions.start, tokenPositions.end + 1)), is("https://doi.org/10.1038/s41586-024-07891-2"));
    }

    @Test
    public void testGetTokenIndexMatchingURLDestination() throws Exception {
        String input = "(https://doi.org/10.1038/s41586-024-07891-2.";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        String destination = "https://doi.org/10.1038/s41586-024-07891-2";

        OffsetPosition tokenPositions = Lexicon.getTokenIndexMatchingURLDestination(tokens, destination);

        assertThat(tokenPositions.start, is(1));
        //NOTE: when doing sublist of the output of this method the end is non-inclusive, so a +1 is needed
        assertThat(tokenPositions.end, is(19));
        assertThat(LayoutTokensUtil.toText(tokens.subList(tokenPositions.start, tokenPositions.end + 1)), is("https://doi.org/10.1038/s41586-024-07891-2"));
    }

    @Test
    public void testGetTokenIndexMatchingURLDestination2() throws Exception {
        String input = "(https://doi.org/10.1038/ s41586-024-07891-2.";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        String destination = "https://doi.org/10.1038/s41586-024-07891-2";

        OffsetPosition tokenPositions = Lexicon.getTokenIndexMatchingURLDestination(tokens, destination);

        assertThat(tokenPositions.start, is(1));
        //NOTE: when doing sublist of the output of this method the end is non-inclusive, so a +1 is needed
        assertThat(tokenPositions.end, is(20));
        assertThat(LayoutTokensUtil.toText(tokens.subList(tokenPositions.start, tokenPositions.end + 1)), is("https://doi.org/10.1038/ s41586-024-07891-2"));
    }

    @Test
    public void testGetTokenIndexMatchingURLDestination3() throws Exception {
        String input = "(https://doi.org/10.1038/ s41586-024- 07891-2.";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        String destination = "https://doi.org/10.1038/s41586-024-07891-2";

        OffsetPosition tokenPositions = Lexicon.getTokenIndexMatchingURLDestination(tokens, destination);

        assertThat(tokenPositions.start, is(1));
        //NOTE: when doing sublist of the output of this method the end is non-inclusive, so a +1 is needed
        assertThat(tokenPositions.end, is(21));
    }


    @Test
    public void testCharacterPositionsUrlPattern_URLRegexMatchesTooLittle_shouldReturnCorrectInterval_1() throws Exception {
        final String input = "We appreciate assistance from The Research Support Center, Research Center for Human Disease Modeling, \n" +
            "and Kyushu University Graduate School of Medical Sciences. We thank Dr. Mitsuru Watanabe and Ms. Eriko \n" +
            "Matsuo from the Department of Neurology, Kyushu University, for the technical assistance in the flow cytometric \n" +
            "analysis. We thank Ms. Sachiko Koyama and Hideko Noguchi from the Department of Neuropathology, Kyushu \n" +
            "University, for excellent technical assistance in the histological analysis. We thank Mr. Tetsuo Kishi from the \n" +
            "Department of Medicine, Kyushu University School of Medicine for the immunohistochemical analysis. We \n" +
            "thank J. Ludovic Croxford, PhD, from Edanz (https:// jp. edanz. com/ ac) for editing a draft of this manuscript.";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        //These have to overlap with the regex output to make sure that the annotation is selected
        LayoutToken lastTokenOfTheURL1 = tokenisedInput.get(219);
        lastTokenOfTheURL1.setPage(15);
        lastTokenOfTheURL1.setX(322.49060000000003);
        lastTokenOfTheURL1.setY(454.586);
        lastTokenOfTheURL1.setWidth(16.338);
        lastTokenOfTheURL1.setHeight(9.099);

        LayoutToken lastTokenOfTheURL2 = tokenisedInput.get(220);
        lastTokenOfTheURL2.setPage(15);
        lastTokenOfTheURL2.setX(338.8286);
        lastTokenOfTheURL2.setY(454.586);
        lastTokenOfTheURL2.setWidth(3.2676);
        lastTokenOfTheURL2.setHeight(9.099);

        LayoutToken lastTokenOfTheURL3 = tokenisedInput.get(221);
        lastTokenOfTheURL3.setPage(15);
        lastTokenOfTheURL3.setX(342.0962);
        lastTokenOfTheURL3.setY(454.586);
        lastTokenOfTheURL3.setWidth(3.2676);
        lastTokenOfTheURL3.setHeight(9.099);

        LayoutToken lastTokenOfTheURL4 = tokenisedInput.get(222);
        lastTokenOfTheURL4.setPage(15);
        lastTokenOfTheURL4.setX(345.3638);
        lastTokenOfTheURL4.setY(454.586);
        lastTokenOfTheURL4.setWidth(3.2676);
        lastTokenOfTheURL4.setHeight(9.099);

        LayoutToken lastTokenOfTheURL5 = tokenisedInput.get(224);
        lastTokenOfTheURL5.setPage(15);
        lastTokenOfTheURL5.setX(348.667);
        lastTokenOfTheURL5.setY(454.586);
        lastTokenOfTheURL5.setWidth(5.868599999999999);
        lastTokenOfTheURL5.setHeight(9.099);

        LayoutToken lastTokenOfTheURL6 = tokenisedInput.get(225);
        lastTokenOfTheURL6.setPage(15);
        lastTokenOfTheURL6.setX(354.5356);
        lastTokenOfTheURL6.setY(454.586);
        lastTokenOfTheURL6.setWidth(2.9342999999999995);
        lastTokenOfTheURL6.setHeight(9.099);

        LayoutToken lastTokenOfTheURL7 = tokenisedInput.get(227);
        lastTokenOfTheURL7.setPage(15);
        lastTokenOfTheURL7.setX(357.514);
        lastTokenOfTheURL7.setY(454.586);
        lastTokenOfTheURL7.setWidth(19.5645);
        lastTokenOfTheURL7.setHeight(9.099);

        LayoutToken lastTokenOfTheURL10 = tokenisedInput.get(231);
        lastTokenOfTheURL10.setPage(15);
        lastTokenOfTheURL10.setX(395.106375);
        lastTokenOfTheURL10.setY(454.586);
        lastTokenOfTheURL10.setWidth(4.690125);
        lastTokenOfTheURL10.setHeight(9.099);

        LayoutToken lastTokenOfTheURL11 = tokenisedInput.get(233);
        lastTokenOfTheURL11.setPage(15);
        lastTokenOfTheURL11.setX(399.842);
        lastTokenOfTheURL11.setY(454.586);
        lastTokenOfTheURL11.setWidth(7.295399999999999);
        lastTokenOfTheURL11.setHeight(9.099);

        LayoutToken lastTokenOfTheURL12 = tokenisedInput.get(234);
        lastTokenOfTheURL12.setPage(15);
        lastTokenOfTheURL12.setX(407.13739999999996);
        lastTokenOfTheURL12.setY(454.586);
        lastTokenOfTheURL12.setWidth(3.6476999999999995);
        lastTokenOfTheURL12.setHeight(9.099);

        PDFAnnotation annotation1 = new PDFAnnotation();
        annotation1.setPageNumber(15);
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        boundingBoxes.add(BoundingBox.fromPointAndDimensions(15, 322.37, 451.55, 85.305, 12.140999999999963));
        annotation1.setBoundingBoxes(boundingBoxes);
        annotation1.setDestination("https://jp.edanz.com/ac");
        annotation1.setType(PDFAnnotation.Type.URI);

        List<PDFAnnotation> pdfAnnotations = List.of(annotation1);

        List<Pair<OffsetPosition, String>> positionsAndDestinations = Lexicon.characterPositionsUrlPatternWithPdfAnnotations(tokenisedInput, pdfAnnotations);

        assertThat(positionsAndDestinations, hasSize(1));
        OffsetPosition url0 = positionsAndDestinations.get(0).getLeft();
        assertThat(input.substring(url0.start, url0.end), is("https:// jp. edanz. com/ ac"));
        assertThat(positionsAndDestinations.get(0).getRight(), is("https://jp.edanz.com/ac"));
    }

    @Test
    public void testCharacterPositionsUrlPattern_URLRegexMatchesTooLittle_shouldReturnCorrectInterval_2() throws Exception {
        /*
         * This test only aims for the last link
         */
        final String input = ", \n" +
            "based on the sorted BAM files generated by using BWA-MEM (v.0.7.17; http:// \n" +
            "biobwa.sourceforge.net/) and SAMtools (v1.546; http://www.htslib.org/). MetaBAT2 \n" +
            "was applied to bin the assemblies with contig depth results under the default \n" +
            "parameters (minimum contig length ≥ 1500 bp). CheckM v.1.0.3 (https://ecogenom \n" +
            "ics.github.io/CheckM/) with the lineage_wf workflow was used to estimate the complete \n" +
            "ness and contamination of MAGs ";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        //These have to overlap with the regex output to make sure that the annotation is selected
        LayoutToken lastTokenOfTheURL1 = tokenisedInput.get(132);
        lastTokenOfTheURL1.setPage(5);
        lastTokenOfTheURL1.setX(331.7820588235294);
        lastTokenOfTheURL1.setY(467.682);
        lastTokenOfTheURL1.setWidth(4.307294117647059);
        lastTokenOfTheURL1.setHeight(10.818);

        LayoutToken lastTokenOfTheURL2 = tokenisedInput.get(133);
        lastTokenOfTheURL2.setPage(5);
        lastTokenOfTheURL2.setX(336.08935294117646);
        lastTokenOfTheURL2.setY(467.682);
        lastTokenOfTheURL2.setWidth(4.307294117647059);
        lastTokenOfTheURL2.setHeight(10.818);

        LayoutToken lastTokenOfTheURL3 = tokenisedInput.get(134);
        lastTokenOfTheURL3.setPage(5);
        lastTokenOfTheURL3.setX(340.39664705882353);
        lastTokenOfTheURL3.setY(467.682);
        lastTokenOfTheURL3.setWidth(34.45835294117647);
        lastTokenOfTheURL3.setHeight(10.818);

        LayoutToken lastTokenOfTheURL5 = tokenisedInput.get(137);
        lastTokenOfTheURL5.setPage(5);
        lastTokenOfTheURL5.setX(41.9999);
        lastTokenOfTheURL5.setY(479.682);
        lastTokenOfTheURL5.setWidth(11.487272727272726);
        lastTokenOfTheURL5.setHeight(10.818);

        PDFAnnotation annotation1 = new PDFAnnotation();
        annotation1.setPageNumber(5);
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        boundingBoxes.add(BoundingBox.fromPointAndDimensions(5, 41.00, 468.50, 335.00, 23.00));
        boundingBoxes.add(BoundingBox.fromPointAndDimensions(5, 134.01, 454.50, 170.18, 24.00));
        boundingBoxes.add(BoundingBox.fromPointAndDimensions(5, 123.68, 481.50, 0.00, 9.00));
        annotation1.setBoundingBoxes(boundingBoxes);
        annotation1.setDestination("https://ecogenomics.github.io/CheckM/");
        annotation1.setType(PDFAnnotation.Type.URI);

        List<PDFAnnotation> pdfAnnotations = List.of(annotation1);

        List<Pair<OffsetPosition, String>> positionsAndDestinations = Lexicon.characterPositionsUrlPatternWithPdfAnnotations(tokenisedInput, pdfAnnotations);

        assertThat(positionsAndDestinations, hasSize(3));
        OffsetPosition url2 = positionsAndDestinations.get(2).getLeft();
        assertThat(input.substring(url2.start, url2.end), is("https://ecogenom \n" +
            "ics.github.io/CheckM/"));
        assertThat(positionsAndDestinations.get(2).getRight(),
            is("https://ecogenomics.github.io/CheckM/"));
    }

    @Test
    public void testCharacterPositionsUrlPattern_NoAnnotations_URLRegexMatchesTooLittle_shouldReturnCorrectInterval_1() throws Exception {
        final String input = "We appreciate assistance from The Research Support Center, Research Center for Human Disease Modeling, \n" +
            "and Kyushu University Graduate School of Medical Sciences. We thank Dr. Mitsuru Watanabe and Ms. Eriko \n" +
            "Matsuo from the Department of Neurology, Kyushu University, for the technical assistance in the flow cytometric \n" +
            "analysis. We thank Ms. Sachiko Koyama and Hideko Noguchi from the Department of Neuropathology, Kyushu \n" +
            "University, for excellent technical assistance in the histological analysis. We thank Mr. Tetsuo Kishi from the \n" +
            "Department of Medicine, Kyushu University School of Medicine for the immunohistochemical analysis. We \n" +
            "thank J. Ludovic Croxford, PhD, from Edanz (https:// jp. edanz. com/ ac) for editing a draft of this manuscript.";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        //These have to overlap with the regex output to make sure that the annotation is selected
        LayoutToken lastTokenOfTheURL1 = tokenisedInput.get(219);
        lastTokenOfTheURL1.setPage(15);
        lastTokenOfTheURL1.setX(322.49060000000003);
        lastTokenOfTheURL1.setY(454.586);
        lastTokenOfTheURL1.setWidth(16.338);
        lastTokenOfTheURL1.setHeight(9.099);

        LayoutToken lastTokenOfTheURL2 = tokenisedInput.get(220);
        lastTokenOfTheURL2.setPage(15);
        lastTokenOfTheURL2.setX(338.8286);
        lastTokenOfTheURL2.setY(454.586);
        lastTokenOfTheURL2.setWidth(3.2676);
        lastTokenOfTheURL2.setHeight(9.099);

        LayoutToken lastTokenOfTheURL3 = tokenisedInput.get(221);
        lastTokenOfTheURL3.setPage(15);
        lastTokenOfTheURL3.setX(342.0962);
        lastTokenOfTheURL3.setY(454.586);
        lastTokenOfTheURL3.setWidth(3.2676);
        lastTokenOfTheURL3.setHeight(9.099);

        LayoutToken lastTokenOfTheURL4 = tokenisedInput.get(222);
        lastTokenOfTheURL4.setPage(15);
        lastTokenOfTheURL4.setX(345.3638);
        lastTokenOfTheURL4.setY(454.586);
        lastTokenOfTheURL4.setWidth(3.2676);
        lastTokenOfTheURL4.setHeight(9.099);

        LayoutToken lastTokenOfTheURL5 = tokenisedInput.get(224);
        lastTokenOfTheURL5.setPage(15);
        lastTokenOfTheURL5.setX(348.667);
        lastTokenOfTheURL5.setY(454.586);
        lastTokenOfTheURL5.setWidth(5.868599999999999);
        lastTokenOfTheURL5.setHeight(9.099);

        LayoutToken lastTokenOfTheURL6 = tokenisedInput.get(225);
        lastTokenOfTheURL6.setPage(15);
        lastTokenOfTheURL6.setX(354.5356);
        lastTokenOfTheURL6.setY(454.586);
        lastTokenOfTheURL6.setWidth(2.9342999999999995);
        lastTokenOfTheURL6.setHeight(9.099);

        LayoutToken lastTokenOfTheURL7 = tokenisedInput.get(227);
        lastTokenOfTheURL7.setPage(15);
        lastTokenOfTheURL7.setX(357.514);
        lastTokenOfTheURL7.setY(454.586);
        lastTokenOfTheURL7.setWidth(19.5645);
        lastTokenOfTheURL7.setHeight(9.099);

        LayoutToken lastTokenOfTheURL10 = tokenisedInput.get(231);
        lastTokenOfTheURL10.setPage(15);
        lastTokenOfTheURL10.setX(395.106375);
        lastTokenOfTheURL10.setY(454.586);
        lastTokenOfTheURL10.setWidth(4.690125);
        lastTokenOfTheURL10.setHeight(9.099);

        LayoutToken lastTokenOfTheURL11 = tokenisedInput.get(233);
        lastTokenOfTheURL11.setPage(15);
        lastTokenOfTheURL11.setX(399.842);
        lastTokenOfTheURL11.setY(454.586);
        lastTokenOfTheURL11.setWidth(7.295399999999999);
        lastTokenOfTheURL11.setHeight(9.099);

        LayoutToken lastTokenOfTheURL12 = tokenisedInput.get(234);
        lastTokenOfTheURL12.setPage(15);
        lastTokenOfTheURL12.setX(407.13739999999996);
        lastTokenOfTheURL12.setY(454.586);
        lastTokenOfTheURL12.setWidth(3.6476999999999995);
        lastTokenOfTheURL12.setHeight(9.099);

        List<PDFAnnotation> pdfAnnotations = new ArrayList<>();

        List<Pair<OffsetPosition, String>> positionsAndDestinations = Lexicon.characterPositionsUrlPatternWithPdfAnnotations(tokenisedInput, pdfAnnotations);

        assertThat(positionsAndDestinations, hasSize(1));
        OffsetPosition url0 = positionsAndDestinations.get(0).getLeft();
        assertThat(input.substring(url0.start, url0.end), is("https:// jp"));
        assertThat(positionsAndDestinations.get(0).getRight(), is(nullValue()));
    }

    @Test
    public void testCharacterPositionsUrlPattern_NoAnnotations_URLRegexMatchesTooLittle_shouldReturnCorrectInterval_2() throws Exception {
        /*
         * This test only aims for the last link
         */
        final String input = ", \n" +
            "based on the sorted BAM files generated by using BWA-MEM (v.0.7.17; http:// \n" +
            "biobwa.sourceforge.net/) and SAMtools (v1.546; http://www.htslib.org/). MetaBAT2 \n" +
            "was applied to bin the assemblies with contig depth results under the default \n" +
            "parameters (minimum contig length ≥ 1500 bp). CheckM v.1.0.3 (https://ecogenom \n" +
            "ics.github.io/CheckM/) with the lineage_wf workflow was used to estimate the complete \n" +
            "ness and contamination of MAGs ";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        //These have to overlap with the regex output to make sure that the annotation is selected
        LayoutToken lastTokenOfTheURL1 = tokenisedInput.get(132);
        lastTokenOfTheURL1.setPage(5);
        lastTokenOfTheURL1.setX(331.7820588235294);
        lastTokenOfTheURL1.setY(467.682);
        lastTokenOfTheURL1.setWidth(4.307294117647059);
        lastTokenOfTheURL1.setHeight(10.818);

        LayoutToken lastTokenOfTheURL2 = tokenisedInput.get(133);
        lastTokenOfTheURL2.setPage(5);
        lastTokenOfTheURL2.setX(336.08935294117646);
        lastTokenOfTheURL2.setY(467.682);
        lastTokenOfTheURL2.setWidth(4.307294117647059);
        lastTokenOfTheURL2.setHeight(10.818);

        LayoutToken lastTokenOfTheURL3 = tokenisedInput.get(134);
        lastTokenOfTheURL3.setPage(5);
        lastTokenOfTheURL3.setX(340.39664705882353);
        lastTokenOfTheURL3.setY(467.682);
        lastTokenOfTheURL3.setWidth(34.45835294117647);
        lastTokenOfTheURL3.setHeight(10.818);

        LayoutToken lastTokenOfTheURL5 = tokenisedInput.get(137);
        lastTokenOfTheURL5.setPage(5);
        lastTokenOfTheURL5.setX(41.9999);
        lastTokenOfTheURL5.setY(479.682);
        lastTokenOfTheURL5.setWidth(11.487272727272726);
        lastTokenOfTheURL5.setHeight(10.818);

        List<PDFAnnotation> pdfAnnotations = new ArrayList<>();

        List<Pair<OffsetPosition, String>> positionsAndDestinations = Lexicon.characterPositionsUrlPatternWithPdfAnnotations(tokenisedInput, pdfAnnotations);

        assertThat(positionsAndDestinations, hasSize(3));
        OffsetPosition url2 = positionsAndDestinations.get(2).getLeft();
        assertThat(input.substring(url2.start, url2.end), is("https://ecogenom"));
        assertThat(positionsAndDestinations.get(2).getRight(),
            is(nullValue()));
    }

    @Test
    public void testCharacterPositionsUrlPattern_URLContainsSpuriosBreklineHypen_shouldReturnCorrectInterval() throws Exception {
        /*
         * This test only aims for the last link
         */
        final String input = "Details and code for using the IntOGen framework are available at \n" +
            "https://intogen.readthedocs.io/en/latest/index.html. The specific \n" +
            "code to perform this analysis is available in the Genomics England \n" +
            "research environment (https://re-docs.genomicsengland.co.uk/ \n" +
            "access/) under /re_gecip/shared_allGeCIPs/pancancer_drivers/code/. \n" +
            "The link to becoming a member of the Genomics England research \n" +
            "network and obtaining access can be found at https://www.genomic-\n" +
            "sengland.co.uk/research/academic/join-gecip. The code to perform \n" +
            "the canSAR chemogenomics analysis is available through Zenodo \n" +
            "(https://doi.org/10.5281/zenodo.8329054) (ref. ";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        //These have to overlap with the regex output to make sure that the annotation is selected
        LayoutToken lastTokenOfTheURL0 = tokenisedInput.get(153);
        lastTokenOfTheURL0.setPage(11);
        lastTokenOfTheURL0.setX(523.39535);
        lastTokenOfTheURL0.setY(436.559);
        lastTokenOfTheURL0.setWidth(4.205850000000001);
        lastTokenOfTheURL0.setHeight(8.217);

        LayoutToken lastTokenOfTheURL1 = tokenisedInput.get(154);
        lastTokenOfTheURL1.setPage(11);
        lastTokenOfTheURL1.setX(527.6012);
        lastTokenOfTheURL1.setY(436.559);
        lastTokenOfTheURL1.setWidth(29.44095);
        lastTokenOfTheURL1.setHeight(8.217);

        LayoutToken lastTokenOfTheURL2 = tokenisedInput.get(155);
        lastTokenOfTheURL2.setPage(11);
        lastTokenOfTheURL2.setX(557.04215);
        lastTokenOfTheURL2.setY(436.559);
        lastTokenOfTheURL2.setWidth(8.217);
        lastTokenOfTheURL2.setHeight(10.818);

        LayoutToken lastTokenOfTheURL3 = tokenisedInput.get(157);
        lastTokenOfTheURL3.setPage(11);
        lastTokenOfTheURL3.setX(306.141);
        lastTokenOfTheURL3.setY(447.309);
        lastTokenOfTheURL3.setWidth(31.902000000000005);
        lastTokenOfTheURL3.setHeight(8.217);

        PDFAnnotation annotation1 = new PDFAnnotation();
        annotation1.setPageNumber(11);
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        boundingBoxes.add(BoundingBox.fromPointAndDimensions(11, 477.14, 434.60, 84.12, 10.18));
//        boundingBoxes.add(BoundingBox.fromPointAndDimensions(5, 134.01, 454.50, 170.18, 24.00));
//        boundingBoxes.add(BoundingBox.fromPointAndDimensions(5, 123.68, 481.50, 0.00, 9.00));
        annotation1.setBoundingBoxes(boundingBoxes);
        annotation1.setDestination("https://www.genomicsengland.co.uk/research/academic/join-gecip");
        annotation1.setType(PDFAnnotation.Type.URI);

        List<PDFAnnotation> pdfAnnotations = List.of(annotation1);

        List<Pair<OffsetPosition, String>> positionsAndDestinations = Lexicon.characterPositionsUrlPatternWithPdfAnnotations(tokenisedInput, pdfAnnotations);

        assertThat(positionsAndDestinations, hasSize(4));
        OffsetPosition url2 = positionsAndDestinations.get(2).getLeft();
        assertThat(input.substring(url2.start, url2.end), is("https://www.genomic-\n" +
            "sengland.co.uk/research/academic/join-gecip"));
        assertThat(positionsAndDestinations.get(2).getRight(),
            is("https://www.genomicsengland.co.uk/research/academic/join-gecip"));
    }

}
