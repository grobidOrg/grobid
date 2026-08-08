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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.grobid.core.exceptions.GrobidException;

/**
 * Specification of field XML paths in different result documents for evaluation.
 *
 * <p>This class doubles as the <b>catalogue</b> of every field the end-to-end evaluation knows
 * how to score, held as three per-section maps ({@code header} / {@code fulltext} /
 * {@code citations}). The catalogue is deliberately larger than what any single run evaluates:
 * which fields are actually scored is decided by {@link EvaluationFieldSelection}, per flavor.
 * A field present in the catalogue but named by no selection is simply disabled &mdash; which is
 * how fields such as {@code affiliations}, {@code publisher} or {@code figure_caption} that used
 * to be commented-out code are now expressed.</p>
 *
 * <p>The sections need <b>separate</b> maps rather than one flat one because field names collide
 * across them by design: {@code title}, {@code authors}, {@code first_author}, {@code date} and
 * {@code doi} all exist in both the header and the citation section, with different XPaths.</p>
 */
public class FieldSpecification {

    /**
     * Name of the pseudo-field giving the base path of a citation structure. It is not a scored
     * field: it is always present at index 0 of a non-empty citation field list, and is consumed
     * separately by the evaluation to locate each citation before the real fields are matched.
     */
    public static final String BASE_FIELD_NAME = "base";

    public String fieldName = null;

    public List<String> nlmPath = new ArrayList<String>();
    public List<String> grobidPath = new ArrayList<String>();
    public List<String> pdfxPath = new ArrayList<String>();
    public List<String> cerminePath = new ArrayList<String>();

    public boolean isTextual = false;
    public boolean computeDocumentLevelMetrics = false;
    public boolean mergeMultipleValues = false;

    /**
     * When non-null, this field is scored by the given strategy instead of the generic
     * extract-and-string-compare path, and its {@code *Path} lists are never read.
     */
    public CustomFieldEvaluator customEvaluator = null;

    /**
     * When true, the support column of this field reports the number of contributing documents
     * rather than the raw count of expected units.
     */
    public boolean reportsSupportAsDocumentCount = false;

    /** Optional explanatory note appended to the report when this field is evaluated. */
    public String reportNote = null;

    // ------------------------------------------------------------------
    // builders
    // ------------------------------------------------------------------

    private static FieldSpecification field(String name) {
        FieldSpecification specification = new FieldSpecification();
        specification.fieldName = name;
        return specification;
    }

    private FieldSpecification grobid(String... paths) {
        Collections.addAll(this.grobidPath, paths);
        return this;
    }

    private FieldSpecification nlm(String... paths) {
        Collections.addAll(this.nlmPath, paths);
        return this;
    }

    private FieldSpecification pdfx(String... paths) {
        Collections.addAll(this.pdfxPath, paths);
        return this;
    }

    private FieldSpecification textual() {
        this.isTextual = true;
        return this;
    }

    private FieldSpecification merged() {
        this.mergeMultipleValues = true;
        return this;
    }

    private FieldSpecification documentLevel() {
        this.computeDocumentLevelMetrics = true;
        return this;
    }

    private FieldSpecification evaluatedBy(CustomFieldEvaluator evaluator) {
        this.customEvaluator = evaluator;
        return this;
    }

    private FieldSpecification supportAsDocumentCount() {
        this.reportsSupportAsDocumentCount = true;
        return this;
    }

    private FieldSpecification note(String note) {
        this.reportNote = note;
        return this;
    }

    private static void register(Map<String, FieldSpecification> catalogue, FieldSpecification specification) {
        if (catalogue.put(specification.fieldName, specification) != null) {
            throw new GrobidException("Duplicate evaluation field in the catalogue: " + specification.fieldName);
        }
    }

    // ------------------------------------------------------------------
    // catalogues
    // ------------------------------------------------------------------

    private static final FieldSpecification CITATION_BASE = field(BASE_FIELD_NAME)
            .grobid("//back/div/listBibl/biblStruct")
            // note: sometimes we just have the raw citation below this!
            .nlm("//ref-list/ref")
            // note: there is nothing beyond that in pdfx xml results!
            .pdfx("//ref-list/ref");

    private static final Map<String, FieldSpecification> HEADER = buildHeaderCatalogue();
    private static final Map<String, FieldSpecification> FULLTEXT = buildFulltextCatalogue();
    private static final Map<String, FieldSpecification> CITATION = buildCitationCatalogue();

    private static Map<String, FieldSpecification> buildHeaderCatalogue() {
        Map<String, FieldSpecification> catalogue = new LinkedHashMap<>();

        register(
                catalogue,
                field("title").textual()
                        .grobid("//titleStmt/title/text()")
                        .nlm("/article/front/article-meta/title-group/article-title//text()")
                        .pdfx("/pdfx/article/front/title-group/article-title/text()"));

        register(
                catalogue,
                field("authors").textual()
                        .grobid("//sourceDesc/biblStruct/analytic/author/persName/surname/text()")
                        .nlm(
                                "/article/front/article-meta/contrib-group/contrib[@contrib-type=\"author\"]/name/surname/text()")
                        .pdfx("/pdfx/article/front/contrib-group/contrib[@contrib-type=\"author\"]/name/text()"));

        register(
                catalogue,
                field("first_author").textual()
                        .grobid("//sourceDesc/biblStruct/analytic/author[1]/persName/surname/text()")
                        .nlm(
                                "/article/front/article-meta/contrib-group/contrib[@contrib-type=\"author\"][1]/name/surname/text()")
                        .pdfx("/pdfx/article/front/contrib-group/contrib[@contrib-type=\"author\"][1]/name/text()"));

        // flat affiliation extraction: concatenates all affiliation text across all authors,
        // so it measures extraction only, not author-to-affiliation linking. Superseded by
        // affiliation_linked below; kept in the catalogue but selected by no flavor.
        register(
                catalogue,
                field("affiliations").textual()
                        .grobid("//sourceDesc/biblStruct/analytic/author/affiliation/orgName/text()")
                        .nlm("/article/front/article-meta/contrib-group/aff/text()")
                        .pdfx("/pdfx/article/front/contrib-group"));

        // linking-aware affiliation metric, scored by a custom evaluator rather than by XPath
        // extraction; see LinkedAffiliationEvaluator.
        register(
                catalogue,
                field(LinkedAffiliationEvaluator.AFFILIATION_LINKED_LABEL).textual()
                        .evaluatedBy(new LinkedAffiliationEvaluator())
                        .supportAsDocumentCount()
                        .note(LinkedAffiliationEvaluator.REPORT_NOTE));

        //in bioRxiv: <pub-date pub-type="epub"><year>2014</year></pub-date>
        register(
                catalogue,
                field("date")
                        .grobid("//publicationStmt/date[1]/@when")
                        .nlm("/article/front/article-meta/pub-date[@pub-type=\"pmc-release\"][1]//text()"));

        register(
                catalogue,
                field("abstract").textual()
                        .grobid("//profileDesc/abstract//text()")
                        .nlm("/article/front/article-meta/abstract//text()"));

        register(
                catalogue,
                field("keywords").textual()
                        .grobid("//profileDesc/textClass/keywords//text()")
                        .nlm("/article/front/article-meta/kwd-group/kwd/text()"));

        register(
                catalogue,
                field("doi")
                        .grobid("//sourceDesc/biblStruct/idno[@type=\"DOI\"]/text()")
                        .nlm("/article/front/article-meta/article-id[@pub-id-type=\"doi\"]/text()"));

        return catalogue;
    }

    /** Citation field paths are relative to the base path, see {@link #CITATION_BASE}. */
    private static Map<String, FieldSpecification> buildCitationCatalogue() {
        Map<String, FieldSpecification> catalogue = new LinkedHashMap<>();

        register(
                catalogue,
                field("title").textual()
                        .grobid("analytic/title/text()")
                        .nlm("*/article-title//text()"));

        register(
                catalogue,
                field("authors").textual()
                        .grobid("analytic/author/persName/surname/text()")
                        .nlm("*//surname[parent::name|parent::string-name]/text()"));

        register(
                catalogue,
                field("first_author").textual()
                        .grobid("analytic/author[1]/persName/surname/text()")
                        .nlm("*//name[1]/surname/text()", "*//string-name[1]/surname/text()"));

        register(
                catalogue,
                field("date")
                        .grobid("monogr/imprint/date/@when")
                        .nlm("*/year/text()"));

        // monograph title
        register(
                catalogue,
                field("inTitle").textual()
                        .grobid("monogr/title/text()")
                        .nlm("*/source/text()"));

        register(
                catalogue,
                field("volume")
                        .grobid("monogr/imprint/biblScope[@unit=\"volume\" or @unit=\"vol\"]/text()")
                        .nlm("*/volume/text()"));

        register(
                catalogue,
                field("issue")
                        .grobid("monogr/imprint/biblScope[@unit=\"issue\"]/text()")
                        .nlm("*/issue/text()"));

        // first page
        register(
                catalogue,
                field("page")
                        .grobid("monogr/imprint/biblScope[@unit=\"page\"]/@from")
                        .nlm("*/fpage/text()"));

        register(
                catalogue,
                field("publisher").textual()
                        .grobid("monogr/imprint/publisher/text()")
                        .nlm("*/publisher-name/text()"));

        // citation identifier (will be used for citation mapping, not for matching)
        register(
                catalogue,
                field("id").textual()
                        .grobid("@id")
                        .nlm("@id"));

        register(
                catalogue,
                field("doi").textual()
                        .grobid("analytic/idno[@type=\"DOI\"]/text()")
                        .nlm("*/pub-id[@pub-id-type=\"doi\"]/text()"));

        register(
                catalogue,
                field("pmid").textual()
                        .grobid("analytic/idno[@type=\"PMID\"]/text()")
                        .nlm("*/pub-id[@pub-id-type=\"pmid\"]/text()"));

        register(
                catalogue,
                field("pmcid").textual()
                        .grobid("analytic/idno[@type=\"PMCID\"]/text()")
                        .nlm("*/pub-id[@pub-id-type=\"pmcid\"]/text()"));

        return catalogue;
    }

    private static Map<String, FieldSpecification> buildFulltextCatalogue() {
        Map<String, FieldSpecification> catalogue = new LinkedHashMap<>();

        register(
                catalogue,
                field("references").textual()
                        .grobid("//back/div/listBibl/biblStruct//text()")
                        .nlm("//ref-list/ref//text()"));

        register(
                catalogue,
                field("section_title").textual()
                        .grobid("//text/body/div/head/text()")
                        .nlm("//body//sec/title/text()"));

        register(
                catalogue,
                field("reference_citation").textual()
                        .grobid("//ref[@type=\"bibr\"]/text()")
                        .nlm("//xref[@ref-type=\"bibr\"]/text()"));

        register(
                catalogue,
                field("reference_figure").textual()
                        .grobid("//ref[@type=\"figure\"]/text()")
                        .nlm("//xref[@ref-type=\"fig\"]/text()"));

        register(
                catalogue,
                field("reference_table").textual()
                        .grobid("//ref[@type=\"table\"]/text()")
                        .nlm("//xref[@ref-type=\"table\"]/text()"));

        register(
                catalogue,
                field("figure_title").textual()
                        .grobid("//figure[not(@type)]/head/text()")
                        // second path for eLife JATS support
                        .nlm("//fig/label/text()", "//fig/caption/title/text()"));

        register(
                catalogue,
                field("figure_caption").textual()
                        .grobid("//figure[not(@type)]/figDesc/text()")
                        .nlm("//fig/caption/p/text()"));

        register(
                catalogue,
                field("figure_label").textual()
                        .grobid("//figure[not(@type)]/label/text()")
                        .nlm("//fig/label/text()"));

        register(
                catalogue,
                field("table_title").textual()
                        .grobid("//figure[@type=\"table\"]/head/text()")
                        // second path for eLife JATS support
                        .nlm("//table-wrap/label/text()", "//table-wrap/caption/title/text()"));

        // note: named "figure_label" in the legacy commented-out code, which was a copy-paste slip
        register(
                catalogue,
                field("table_label").textual()
                        .grobid("//figure[@type=\"table\"]/label/text()")
                        .nlm("//fig/label/text()"));

        register(
                catalogue,
                field("table_caption").textual()
                        .grobid("//figure[@type=\"table\"]/figDesc/text()")
                        .nlm("//table-wrap/caption/p/text()"));

        register(
                catalogue,
                field("availability_stmt").textual()
                        .merged()
                        .documentLevel()
                        .grobid("//div[@type=\"availability\"]//text()")
                        .nlm(
                                "//sec[@sec-type=\"availability\"]//text()",
                                "//p[@content-type=\"availability\"]//text()",
                                "//sec[@specific-use=\"availability\"]//text()",
                                // for eLife JATS support
                                "//sec[@sec-type=\"data-availability\"]//text()",
                                // the following for PLOS JATS support
                                "//custom-meta[@id=\"data-availability\"]/meta-value//text()"));

        register(
                catalogue,
                field("funding_stmt").textual()
                        .grobid("//div[@type=\"funding\"]//text()")
                        .nlm(
                                "//sec[@sec-type=\"funding\"]//text()",
                                "//p[@content-type=\"funding\"]//text()",
                                "//sec[@specific-use=\"funding\"]//text()",
                                // the following for PLOS support
                                "//funding-statement//text()"));

        register(
                catalogue,
                field("conflict_stmt").textual()
                        .merged()
                        .documentLevel()
                        .grobid("//div[@type=\"conflict\"]//text()")
                        //PLOs JATS uses fn-type="con" for contribution and fn-type="conflict" for conflict of interest
                        .nlm(
                                "//fn-group[@content-type=\"competing-interest\"]//text()",
                                "//fn[@fn-type=\"COI\"]//text()",
                                "//fn[@fn-type=\"conflict\"]//text()",
                                "//fn[@type=\"conflict\"]//text()",
                                "//sec[@type=\"conflict\"]//text()",
                                "//sec[@sec-type=\"COI-statement\"]//text()",
                                "//sec[@sec-type=\"conflict\"]//text()"));

        register(
                catalogue,
                field("contribution_stmt").textual()
                        .merged()
                        .documentLevel()
                        .grobid("//div[@type=\"contribution\"]//text()")
                        // Ambiguous in PLOS JATS: fn-type="con" is used for contribution but also for
                        // conflict of interest. We added type="contribution" for such cases
                        .nlm(
                                "//fn[@type=\"contribution\"]//text()",
                                "//fn-group[@content-type=\"author-contribution\"]//text()",
                                "//sec[@sec-type=\"contribution\"]//text()",
                                "//sec[@type=\"contribution\"]//text()"));

        return catalogue;
    }

    // ------------------------------------------------------------------
    // accessors
    // ------------------------------------------------------------------

    private static List<FieldSpecification> select(
            Map<String, FieldSpecification> catalogue,
            List<String> names,
            String section) {
        List<FieldSpecification> selected = new ArrayList<>();
        for (String name : names) {
            FieldSpecification specification = catalogue.get(name);
            if (specification == null) {
                throw new GrobidException("Unknown " + section + " evaluation field: " + name);
            }
            selected.add(specification);
        }
        return selected;
    }

    public static List<FieldSpecification> headerFields(List<String> names) {
        return select(HEADER, names, "header");
    }

    public static List<FieldSpecification> fulltextFields(List<String> names) {
        return select(FULLTEXT, names, "fulltext");
    }

    /**
     * The selected citation fields, with the base path pseudo-field prepended at index 0 (the
     * evaluation relies on it being there). An empty selection yields an <b>empty</b> list, not a
     * list holding only the base field, so that an empty selection reliably disables the whole
     * citation section.
     */
    public static List<FieldSpecification> citationFields(List<String> names) {
        if (names.isEmpty()) {
            return new ArrayList<>();
        }
        List<FieldSpecification> selected = new ArrayList<>();
        selected.add(CITATION_BASE);
        selected.addAll(select(CITATION, names, "citation"));
        return selected;
    }

    public static FieldSpecification citationBase() {
        return CITATION_BASE;
    }

    public static Set<String> headerFieldNames() {
        return Collections.unmodifiableSet(HEADER.keySet());
    }

    public static Set<String> fulltextFieldNames() {
        return Collections.unmodifiableSet(FULLTEXT.keySet());
    }

    public static Set<String> citationFieldNames() {
        return Collections.unmodifiableSet(CITATION.keySet());
    }

    public static String grobidCitationContextId = "//ref[@type=\"bibr\"]/@target";
    public static String grobidBibReferenceId = "//listBibl/biblStruct/@id";

    public static String nlmCitationContextId = "//xref[@ref-type=\"bibr\"]/@rid";
    public static String nlmBibReferenceId = "//ref-list/ref/@id";

}
