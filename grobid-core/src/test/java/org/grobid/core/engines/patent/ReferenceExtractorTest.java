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
package org.grobid.core.engines.patent;

import static org.grobid.core.utilities.TextUtilities.delimiters;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.PatentItem;
import org.grobid.core.factory.AbstractEngineFactory;

public class ReferenceExtractorTest {

    public static final Logger LOGGER = LoggerFactory
            .getLogger(ReferenceExtractorTest.class);

    @BeforeClass
    public static void setInitialContext() throws Exception {
        AbstractEngineFactory.init();
    }

    @AfterClass
    public static void destroyInitialContext() throws Exception {
    }

    // extractor.extractAllReferencesXMLFile(new
    // File("src/test/resources/org/grobid/core/engines/patent/ReferenceExtractor/sample-24514352.tei.xml").getAbsolutePath(),
    // false, false, patents, articles);

    @Test
    public void extractAllReferencesStringNull() {
        ReferenceExtractor extractor = new ReferenceExtractor();
        List<String> toExtracts = new ArrayList<>();
        toExtracts.add("Economic Development Quarterly November 2011 25: 353-365, first published on August 25, 2011.");
        String res = extractor
                .extractAllReferencesString(
                        toExtracts,
                        false,
                        0,
                        false,
                        null,
                        null);
        //assertEquals(0, nbRes);
    }

    //@Test
    public void extractAllReferencesStringArticles() {
        ReferenceExtractor extractor = new ReferenceExtractor();
        List<PatentItem> patents = new ArrayList<>();
        List<BibDataSet> articles = new ArrayList<>();
        String toExtract = "Some other description includes ref. US 2011/0155847 A1 in aerodynamic"
                +
                " and applied physics. "
                +
                "This patent, ref. US 7930197 says data mining of personal data is patented. "
                +
                "That article refers to Economic Development Quarterly November 2011 25:353-365, first"
                +
                " published on August 25, 2011.";
        List<String> toExtracts = new ArrayList<>();
        toExtracts.add(toExtract);
        extractor.extractAllReferencesString(toExtracts, false, 0, false, patents, articles);
        assertEquals(2, patents.size());
        assertEquals(1, articles.size());
    }

    //@Test
    public void extractAllReferencesStringArticles2() {
        ReferenceExtractor extractor = new ReferenceExtractor();
        List<PatentItem> patents = new ArrayList<>();
        List<BibDataSet> articles = new ArrayList<>();
        List<String> toExtracts = new ArrayList<>();
        toExtracts.add(
                "That article It refers to Economic Development Quarterly November 2011 25: 353-365,"
                        +
                        " first published on August 25, 2011.");
        extractor.extractAllReferencesString(
                toExtracts,
                false,
                0,
                false,
                patents,
                articles);
        LOGGER.info("BibDataSet: " + articles.toString());
        assertEquals(0, patents.size());
        assertEquals(1, articles.size());

        List<Integer> offsets = articles.get(0).getOffsets();
        int startOffset = -1;
        int endOffset = -1;
        String rawBib = articles.get(0).getRawBib();
        if (!offsets.isEmpty() && offsets.get(0) != null) {
            startOffset = offsets.get(0).intValue();
            StringTokenizer stt = new StringTokenizer(rawBib, delimiters, true);
            int count2 = 0;
            int charCpt = 0;
            String token2;
            while (stt.hasMoreTokens()) {
                token2 = stt.nextToken();

                if (token2.trim().length() != 0) {
                    count2++;
                }

                charCpt += token2.length();
                System.err.println(
                        token2
                                + "   count="
                                + count2
                                + "   charCpt="
                                + charCpt);
            }
            endOffset = startOffset + count2;

            System.out.println("RawBib=" + rawBib);
            System.out.println(
                    "Start="
                            + startOffset
                            + "   offset="
                            + count2
                            + "   end="
                            + endOffset);
        }

        LOGGER.info(articles.get(0).getOffsets().toString());
    }

    //@Test
    public void extractAllReferencesStringPatents() {
        ReferenceExtractor extractor = new ReferenceExtractor();
        List<PatentItem> patents = new ArrayList<>();
        List<BibDataSet> articles = new ArrayList<>();
        String toExtract = "US-8303618, Intravascular filter and method A filter disposed at the distal end of an elongate guidewire. Catheters are provided for delivering the filter to, and retrieving the filter from, a treatment...";
        toExtract = "this patent refers US-8303618, bla bla";
        List<String> toExtracts = new ArrayList<>();
        toExtracts.add(toExtract);
        extractor.extractAllReferencesString(toExtracts, false, 0, false, patents, articles);
        LOGGER.info("PatentItem: " + patents.toString());
        assertEquals(1, patents.size());
        assertEquals(0, articles.size());
        PatentItem patent = patents.get(0);
        assertEquals("8303618", patent.getNumberEpoDoc());
        System.out.println("context=" + patent.getContext());
        System.out.println(
                "offset start/end/raw="
                        + patent.getOffsetBegin()
                        + "/"
                        + patent.getOffsetEnd()
                        + "/"
                        + patent.getOffsetRaw());
    }

    @Test
    public void extractAllReferencesXmlST36() {
        ReferenceExtractor extractor = new ReferenceExtractor();
        List<PatentItem> patents = new ArrayList<>();
        List<BibDataSet> articles = new ArrayList<>();
        extractor.extractAllReferencesXMLFile(
                new File(
                        "src/test/resources/org/grobid/core/engines/patent/ReferenceExtractor/st36-sample-1.xml")
                        .getAbsolutePath(),
                false,
                0,
                false,
                patents,
                articles);
        LOGGER.info("PatentItem: " + patents.toString());
        assertEquals(2, patents.size());
        assertEquals(0, articles.size());
        assertEquals("9937368", patents.get(0).getNumberEpoDoc());
        assertEquals("6083121", patents.get(1).getNumberEpoDoc());
    }

    @Test
    public void extractAllReferencesXml() {
        ReferenceExtractor extractor = new ReferenceExtractor();
        List<PatentItem> patents = new ArrayList<>();
        List<BibDataSet> articles = new ArrayList<>();
        extractor.extractAllReferencesXMLFile(
                new File(
                        "src/test/resources/patents/006271747.xml")
                        .getAbsolutePath(),
                false,
                0,
                false,
                patents,
                articles);
        assertEquals("20050675311", patents.get(0).getNumberEpoDoc());
        assertEquals("9202190", patents.get(1).getNumberEpoDoc());
    }

    @Ignore
    public void extractAllReferencesPdf() {
        ReferenceExtractor extractor = new ReferenceExtractor();
        List<PatentItem> patents = new ArrayList<>();
        List<BibDataSet> articles = new ArrayList<>();
        extractor.extractAllReferencesPDFFile(
                new File(
                        "src/test/resources/org/grobid/core/engines/patent/ReferenceExtractor/sample-1.pdf")
                        .getAbsolutePath(),
                false,
                0,
                false,
                patents,
                articles);
    }

    //@Test
    public void jaProcessing() {
        String text_jp = "すなわち、相対的な頻度で、エポキシドをベースとする液体接着剤及び接着結合剤が、"
                +
                "例えばＷＯ９８／２１２８７Ａ１。これらの主な使用分野は、硬質装置のみならず適度に柔軟な装置における縁部の結合である。"
                +
                "硬化は、熱により又はＵＶ照射により行われる。";
        List<String> toExtracts = new ArrayList<>();
        toExtracts.add(text_jp);
        ReferenceExtractor extractor = new ReferenceExtractor();
        List<PatentItem> patents = new ArrayList<>();
        extractor.extractAllReferencesString(toExtracts, false, 0, false, patents, null);
        LOGGER.info("PatentItem: " + patents.toString());
        assertEquals(1, patents.size());
        assertEquals("21287", patents.get(0).getNumberEpoDoc());
    }

    //@Test
    public void koProcessing() {
        String text_ko = "미국의 애플사의 미국 출원 2012/012710. 따라서, 전기화학적 센서들의 제조, 특히 혈액 또는 간질액과 같은 신체 마커들(포도당, 프룩토사민, "
                +
                "하에마토크릿 등)의측정을 위한 전기화학적 센서들을 제조하기 위해 개선된 프로세스가 필요하다. 또한, 합리적인 가격으로 센서 스트립들을제조하기 "
                +
                "위한 고속의 예측가능하고 재생가능한 방법에 대한 필요성이 있다. 또한, 각각의 완료된 스트립이 재생가능한 방법으로 체액의 분석 대상물들을 "
                +
                "신뢰성있고 예측가능하며 정밀하게 측정하는데 사용될 수 있는 매우 작은 특성들을 갖는센서 스트립들을 고속의 예측가능하고 반복가능한 방법으로 제조할 필요가 있다.";
        List<String> toExtracts = new ArrayList<>();
        ReferenceExtractor extractor = new ReferenceExtractor();
        List<PatentItem> patents = new ArrayList<>();
        toExtracts.add(text_ko);
        extractor.extractAllReferencesString(toExtracts, false, 0, false, patents, null);
        LOGGER.info("PatentItem: " + patents.toString());
        assertEquals(1, patents.size());
        assertEquals("2012012710", patents.get(0).getNumberEpoDoc());
    }

    //@Test
    public void zhProcessing() {
        String text_zh = "在本申请的申请人于2008年8月26日提交的申请号为US2008/001534的PCT国际申请中，"
                +
                "揭示了一种等截面三角形定向棱镜圆形反光板及由其制成的圆板灯。该圆板灯包括：等截面三角形微棱镜圆形导光板；"
                +
                "围绕导光板的散热框，该散热框与导光板之间形成间隙而构成环形灯槽；以及嵌装于环形灯槽内的环形灯组件，"
                +
                "该环形灯组件由多个发光二极管(LED)贴片、电阻和线路板构成。该申请的全部内容，通过引用结合于此。";
        List<String> toExtracts = new ArrayList<>();
        ReferenceExtractor extractor = new ReferenceExtractor();
        List<PatentItem> patents = new ArrayList<>();
        toExtracts.add(text_zh);
        extractor.extractAllReferencesString(toExtracts, false, 0, false, patents, null);
        LOGGER.info("PatentItem: " + patents.toString());
        assertEquals(1, patents.size());
        assertEquals("2008001534", patents.get(0).getNumberEpoDoc());
    }
}
