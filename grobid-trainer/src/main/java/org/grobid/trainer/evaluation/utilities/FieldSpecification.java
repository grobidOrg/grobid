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

import java.util.*;

/**
 * Specification of field XML paths in different result documents for evaluation.
 */
public class FieldSpecification {

    public String fieldName = null;

    public List<String> nlmPath = new ArrayList<String>();
    public List<String> grobidPath = new ArrayList<String>();
    public List<String> pdfxPath = new ArrayList<String>();
    public List<String> cerminePath = new ArrayList<String>();

    public boolean isTextual = false;
    public boolean computeDocumentLevelMetrics = false;
    public boolean mergeMultipleValues = false;

    /**
     * This static method instantiates the fields with the appropriate paths
     * in the different gold and extraction formats.
     */
    public static void setUpFields(
            List<FieldSpecification> headerFields,
            List<FieldSpecification> fulltextFields,
            List<FieldSpecification> citationsFields,
            List<String> headerLabels,
            List<String> fulltextLabels,
            List<String> citationsLabels) {
        // header

        // title
        FieldSpecification titleField = new FieldSpecification();
        titleField.fieldName = "title";
        titleField.isTextual = true;
        titleField.grobidPath.add("//titleStmt/title/text()");
        titleField.nlmPath.add("/article/front/article-meta/title-group/article-title//text()");
        titleField.pdfxPath.add("/pdfx/article/front/title-group/article-title/text()");
        headerFields.add(titleField);
        headerLabels.add("title");

        // authors
        FieldSpecification authorField = new FieldSpecification();
        authorField.fieldName = "authors";
        authorField.isTextual = true;
        //authorField.hasMultipleValue = true;
        /*authorField.grobidPath.
        	add("//sourceDesc/biblStruct/analytic/author/persName/forename[@type=\"first\"]");
        authorField.grobidPath.
        	add("//sourceDesc/biblStruct/analytic/author/persName/forename[@type=\"middle\"]");*/
        authorField.grobidPath.add("//sourceDesc/biblStruct/analytic/author/persName/surname/text()");
        //authorField.nlmPath.
        //	add("/article/front/article-meta/contrib-group/contrib[@contrib-type=\"author\"]/name/given-names");
        authorField.nlmPath
                .add("/article/front/article-meta/contrib-group/contrib[@contrib-type=\"author\"]/name/surname/text()");
        authorField.pdfxPath.add("/pdfx/article/front/contrib-group/contrib[@contrib-type=\"author\"]/name/text()");
        headerFields.add(authorField);
        headerLabels.add("authors");

        // authors
        FieldSpecification firstAuthorField = new FieldSpecification();
        firstAuthorField.fieldName = "first_author";
        firstAuthorField.isTextual = true;
        /*firstAuthorField.grobidPath
        	.add("//sourceDesc/biblStruct/analytic/author/persName/forename[@type=\"first\"]");
        firstAuthorField.grobidPath
        	.add("//sourceDesc/biblStruct/analytic/author/persName/forename[@type=\"middle\"]");*/
        firstAuthorField.grobidPath
                .add("//sourceDesc/biblStruct/analytic/author[1]/persName/surname/text()");
        //firstAuthorField.nlmPath
        //	.add("/article/front/article-meta/contrib-group/contrib[@contrib-type=\"author\"]/name/given-names");
        firstAuthorField.nlmPath
                .add(
                        "/article/front/article-meta/contrib-group/contrib[@contrib-type=\"author\"][1]/name/surname/text()");
        firstAuthorField.pdfxPath
                .add("/pdfx/article/front/contrib-group/contrib[@contrib-type=\"author\"][1]/name/text()");
        headerFields.add(firstAuthorField);
        headerLabels.add("first_author");

        // affiliation
        FieldSpecification affiliationField = new FieldSpecification();
        affiliationField.fieldName = "affiliations";
        affiliationField.isTextual = true;
        //affiliationField.hasMultipleValue = true;
        affiliationField.grobidPath.add("//sourceDesc/biblStruct/analytic/author/affiliation/orgName/text()");
        affiliationField.nlmPath.add("/article/front/article-meta/contrib-group/aff/text()");
        affiliationField.pdfxPath.add("/pdfx/article/front/contrib-group");
        //headerFields.add(affiliationField);
        //headerLabels.add("affiliations");

        // date
        FieldSpecification dateField = new FieldSpecification();
        dateField.fieldName = "date";
        dateField.grobidPath.add("//publicationStmt/date[1]/@when");
        dateField.nlmPath.add("/article/front/article-meta/pub-date[@pub-type=\"pmc-release\"][1]//text()");
        //in bioRxiv: <pub-date pub-type="epub"><year>2014</year></pub-date>
        //headerFields.add(dateField);
        //headerLabels.add("date");

        // abstract
        FieldSpecification abstractField = new FieldSpecification();
        abstractField.fieldName = "abstract";
        abstractField.isTextual = true;
        abstractField.grobidPath.add("//profileDesc/abstract//text()");
        abstractField.nlmPath.add("/article/front/article-meta/abstract//text()");
        headerFields.add(abstractField);
        headerLabels.add("abstract");

        // keywords
        FieldSpecification keywordsField = new FieldSpecification();
        keywordsField.fieldName = "keywords";
        keywordsField.isTextual = true;
        keywordsField.grobidPath.add("//profileDesc/textClass/keywords//text()");
        keywordsField.nlmPath.add("/article/front/article-meta/kwd-group/kwd/text()");
        headerFields.add(keywordsField);
        headerLabels.add("keywords");

        // DOI (header)
        FieldSpecification doiField = new FieldSpecification();
        doiField.fieldName = "doi";
        doiField.grobidPath.add("//sourceDesc/biblStruct/idno[@type=\"DOI\"]/text()");
        doiField.nlmPath.add("/article/front/article-meta/article-id[@pub-id-type=\"doi\"]/text()");
        //headerFields.add(doiField);
        //headerLabels.add("doi");

        // citations

        // the first field gives the base path for each citation structure
        FieldSpecification baseCitation = new FieldSpecification();
        baseCitation.fieldName = "base";
        baseCitation.grobidPath.add("//back/div/listBibl/biblStruct");
        baseCitation.nlmPath.add("//ref-list/ref"); // note: sometimes we just have the raw citation below this!
        baseCitation.pdfxPath.add("//ref-list/ref"); // note: there is nothing beyond that in pdfx xml results!
        citationsFields.add(baseCitation);
        // the rest of the citation fields are relative to the base path

        // title
        FieldSpecification titleField2 = new FieldSpecification();
        titleField2.fieldName = "title";
        titleField2.isTextual = true;
        titleField2.grobidPath.add("analytic/title/text()");
        titleField2.nlmPath.add("*/article-title//text()");
        citationsFields.add(titleField2);
        citationsLabels.add("title");

        // authors
        FieldSpecification authorField2 = new FieldSpecification();
        authorField2.fieldName = "authors";
        authorField2.isTextual = true;
        authorField2.grobidPath.add("analytic/author/persName/surname/text()");
        authorField2.nlmPath.add("*//surname[parent::name|parent::string-name]/text()");
        //authorField2.nlmPath.add("*//name/surname/text()");
        //authorField2.nlmPath.add("*//string-name/surname/text()");
        citationsFields.add(authorField2);
        citationsLabels.add("authors");

        // authors
        FieldSpecification firstAuthorField2 = new FieldSpecification();
        firstAuthorField2.fieldName = "first_author";
        firstAuthorField2.isTextual = true;
        firstAuthorField2.grobidPath.add("analytic/author[1]/persName/surname/text()");
        //firstAuthorField2.nlmPath.add("*//surname[parent::name|parent::string-name][1]/text()");
        firstAuthorField2.nlmPath.add("*//name[1]/surname/text()");
        firstAuthorField2.nlmPath.add("*//string-name[1]/surname/text()");
        citationsFields.add(firstAuthorField2);
        citationsLabels.add("first_author");

        // date
        FieldSpecification dateField2 = new FieldSpecification();
        dateField2.fieldName = "date";
        dateField2.grobidPath.add("monogr/imprint/date/@when");
        dateField2.nlmPath.add("*/year/text()");
        citationsFields.add(dateField2);
        citationsLabels.add("date");

        // monograph title
        FieldSpecification inTitleField2 = new FieldSpecification();
        inTitleField2.fieldName = "inTitle";
        inTitleField2.isTextual = true;
        inTitleField2.grobidPath.add("monogr/title/text()");
        inTitleField2.nlmPath.add("*/source/text()");
        citationsFields.add(inTitleField2);
        citationsLabels.add("inTitle");

        // volume
        FieldSpecification volumeField = new FieldSpecification();
        volumeField.fieldName = "volume";
        volumeField.grobidPath.add("monogr/imprint/biblScope[@unit=\"volume\" or @unit=\"vol\"]/text()");
        volumeField.nlmPath.add("*/volume/text()");
        citationsFields.add(volumeField);
        citationsLabels.add("volume");

        // issue
        FieldSpecification issueField = new FieldSpecification();
        issueField.fieldName = "issue";
        issueField.grobidPath.add("monogr/imprint/biblScope[@unit=\"issue\"]/text()");
        issueField.nlmPath.add("*/issue/text()");
        citationsFields.add(issueField);
        citationsLabels.add("issue");

        // first page
        FieldSpecification pageField = new FieldSpecification();
        pageField.fieldName = "page";
        pageField.grobidPath.add("monogr/imprint/biblScope[@unit=\"page\"]/@from");
        pageField.nlmPath.add("*/fpage/text()");
        citationsFields.add(pageField);
        citationsLabels.add("page");

        // publisher
        FieldSpecification publisherField = new FieldSpecification();
        publisherField.fieldName = "publisher";
        publisherField.isTextual = true;
        publisherField.grobidPath.add("monogr/imprint/publisher/text()");
        publisherField.nlmPath.add("*/publisher-name/text()");
        //citationsFields.add(publisherField);
        //citationsLabels.add("publisher");

        // citation identifier (will be used for citation mapping, not for matching)
        FieldSpecification citationIdField = new FieldSpecification();
        citationIdField.fieldName = "id";
        citationIdField.isTextual = true;
        citationIdField.grobidPath.add("@id");
        citationIdField.nlmPath.add("@id");
        citationsFields.add(citationIdField);
        citationsLabels.add("id");

        // DOI
        FieldSpecification citationDOIField = new FieldSpecification();
        citationDOIField.fieldName = "doi";
        citationDOIField.isTextual = true;
        citationDOIField.grobidPath.add("analytic/idno[@type=\"DOI\"]/text()");
        citationDOIField.nlmPath.add("*/pub-id[@pub-id-type=\"doi\"]/text()");
        citationsFields.add(citationDOIField);
        citationsLabels.add("doi");

        // PMID
        FieldSpecification citationPMIDField = new FieldSpecification();
        citationPMIDField.fieldName = "pmid";
        citationPMIDField.isTextual = true;
        citationPMIDField.grobidPath.add("analytic/idno[@type=\"PMID\"]/text()");
        citationPMIDField.nlmPath.add("*/pub-id[@pub-id-type=\"pmid\"]/text()");
        citationsFields.add(citationPMIDField);
        citationsLabels.add("pmid");

        // PMC
        FieldSpecification citationPMCIDField = new FieldSpecification();
        citationPMCIDField.fieldName = "pmcid";
        citationPMCIDField.isTextual = true;
        citationPMCIDField.grobidPath.add("analytic/idno[@type=\"PMCID\"]/text()");
        citationPMCIDField.nlmPath.add("*/pub-id[@pub-id-type=\"pmcid\"]/text()");
        citationsFields.add(citationPMCIDField);
        citationsLabels.add("pmcid");

        // full text structures
        /*FieldSpecification sectionReferenceField = new FieldSpecification();
        sectionReferenceField.fieldName = "references";
        sectionReferenceField.isTextual = true;
        sectionReferenceField.grobidPath.
        	add("//back/div/listBibl/biblStruct//text()");
        sectionReferenceField.nlmPath.
        	add("//ref-list/ref//text()");
        fulltextFields.add(sectionReferenceField);
        fulltextLabels.add("references");*/

        FieldSpecification sectionTitleField = new FieldSpecification();
        sectionTitleField.fieldName = "section_title";
        sectionTitleField.isTextual = true;
        sectionTitleField.grobidPath.add("//text/body/div/head/text()");
        sectionTitleField.nlmPath.add("//body//sec/title/text()");
        fulltextFields.add(sectionTitleField);
        fulltextLabels.add("section_title");

        FieldSpecification referenceMarkerField = new FieldSpecification();
        referenceMarkerField.fieldName = "reference_citation";
        referenceMarkerField.isTextual = true;
        referenceMarkerField.grobidPath.add("//ref[@type=\"bibr\"]/text()");
        referenceMarkerField.nlmPath.add("//xref[@ref-type=\"bibr\"]/text()");
        fulltextFields.add(referenceMarkerField);
        fulltextLabels.add("reference_citation");

        FieldSpecification referenceFigureField = new FieldSpecification();
        referenceFigureField.fieldName = "reference_figure";
        referenceFigureField.isTextual = true;
        referenceFigureField.grobidPath.add("//ref[@type=\"figure\"]/text()");
        referenceFigureField.nlmPath.add("//xref[@ref-type=\"fig\"]/text()");
        fulltextFields.add(referenceFigureField);
        fulltextLabels.add("reference_figure");

        FieldSpecification referenceTableField = new FieldSpecification();
        referenceTableField.fieldName = "reference_table";
        referenceTableField.isTextual = true;
        referenceTableField.grobidPath.add("//ref[@type=\"table\"]/text()");
        referenceTableField.nlmPath.add("//xref[@ref-type=\"table\"]/text()");
        fulltextFields.add(referenceTableField);
        fulltextLabels.add("reference_table");

        FieldSpecification figureTitleField = new FieldSpecification();
        figureTitleField.fieldName = "figure_title";
        figureTitleField.isTextual = true;
        figureTitleField.grobidPath.add("//figure[not(@type)]/head/text()");
        figureTitleField.nlmPath.add("//fig/label/text()");
        // eLife JATS support
        figureTitleField.nlmPath.add("//fig/caption/title/text()");
        fulltextFields.add(figureTitleField);
        fulltextLabels.add("figure_title");

        /*FieldSpecification figureCaptionField = new FieldSpecification();
        figureCaptionField.fieldName = "figure_caption";
        figureCaptionField.isTextual = true;
        figureCaptionField.grobidPath.
        	add("//figure[not(@type)]/figDesc/text()");
        figureCaptionField.nlmPath.
        	add("//fig/caption/p/text()");
        fulltextFields.add(figureCaptionField);
        fulltextLabels.add("figure_caption");*/

        /*FieldSpecification figureLabelField = new FieldSpecification();
        figureLabelField.fieldName = "figure_label";
        figureLabelField.isTextual = true;
        figureLabelField.grobidPath.
        	add("//figure[not(@type)]/label/text()");
        figureLabelField.nlmPath.
        	add("//fig/label/text()");
        fulltextFields.add(figureLabelField);
        fulltextLabels.add("figure_label");*/

        FieldSpecification tableTitleField = new FieldSpecification();
        tableTitleField.fieldName = "table_title";
        tableTitleField.isTextual = true;
        tableTitleField.grobidPath.add("//figure[@type=\"table\"]/head/text()");
        tableTitleField.nlmPath.add("//table-wrap/label/text()");
        // eLife JATS support
        tableTitleField.nlmPath.add("//table-wrap/caption/title/text()");
        fulltextFields.add(tableTitleField);
        fulltextLabels.add("table_title");

        /*FieldSpecification tableLabelField = new FieldSpecification();
        tableLabelField.fieldName = "figure_label";
        tableLabelField.isTextual = true;
        tableLabelField.grobidPath.
        	add("//figure[@type=\"table\"]/label/text()");
        tableLabelField.nlmPath.
        	add("//fig/label/text()");
        fulltextFields.add(tableLabelField);
        fulltextLabels.add("figure_label");*/

        /*FieldSpecification tableCaptionField = new FieldSpecification();
        tableCaptionField.fieldName = "table_caption";
        tableCaptionField.isTextual = true;
        tableCaptionField.grobidPath.
        	add("//figure[@type=\"table\"]/figDesc/text()");
        tableCaptionField.nlmPath.
        	add("//table-wrap/caption/p/text()");
        fulltextFields.add(tableCaptionField);
        fulltextLabels.add("figure_caption");*/

        //labels.add("section_title");
        //labels.add("paragraph");
        //labels.add("citation_marker");
        //labels.add("figure_marker");
        //labels.add("table_marker");

        FieldSpecification dataAvailabilityFulltextField = new FieldSpecification();
        dataAvailabilityFulltextField.fieldName = "availability_stmt";
        dataAvailabilityFulltextField.isTextual = true;
        dataAvailabilityFulltextField.mergeMultipleValues = true;
        dataAvailabilityFulltextField.computeDocumentLevelMetrics = true;
        dataAvailabilityFulltextField.grobidPath
                .add("//div[@type=\"availability\"]//text()");
        dataAvailabilityFulltextField.nlmPath
                .add("//sec[@sec-type=\"availability\"]//text()");
        dataAvailabilityFulltextField.nlmPath
                .add("//p[@content-type=\"availability\"]//text()");
        dataAvailabilityFulltextField.nlmPath
                .add("//sec[@specific-use=\"availability\"]//text()");
        // for eLife JATS support
        dataAvailabilityFulltextField.nlmPath
                .add("//sec[@sec-type=\"data-availability\"]//text()");
        // the following for PLOS JATS support
        dataAvailabilityFulltextField.nlmPath
                .add("//custom-meta[@id=\"data-availability\"]/meta-value//text()");
        fulltextFields.add(dataAvailabilityFulltextField);
        fulltextLabels.add("availability_stmt");

        FieldSpecification fundingFulltextField = new FieldSpecification();
        fundingFulltextField.fieldName = "funding_stmt";
        fundingFulltextField.isTextual = true;
        fundingFulltextField.grobidPath
                .add("//div[@type=\"funding\"]//text()");
        fundingFulltextField.nlmPath
                .add("//sec[@sec-type=\"funding\"]//text()");
        fundingFulltextField.nlmPath
                .add("//p[@content-type=\"funding\"]//text()");
        fundingFulltextField.nlmPath
                .add("//sec[@specific-use=\"funding\"]//text()");
        // for eLife JATS support
        // the following for PLOS support
        fundingFulltextField.nlmPath
                .add("//funding-statement//text()");
        fulltextFields.add(fundingFulltextField);
        fulltextLabels.add("funding_stmt");

        FieldSpecification conflictFulltextField = new FieldSpecification();
        conflictFulltextField.fieldName = "conflict_stmt";
        conflictFulltextField.isTextual = true;
        conflictFulltextField.mergeMultipleValues = true;
        conflictFulltextField.computeDocumentLevelMetrics = true;
        conflictFulltextField.grobidPath.add("//div[@type=\"conflict\"]//text()");
        //PLOs JATS uses fn-type="con" for contribution and fn-type="conflict" for conflict of interest
        conflictFulltextField.nlmPath.add("//fn-group[@content-type=\"competing-interest\"]//text()");
        conflictFulltextField.nlmPath.add("//fn[@fn-type=\"COI\"]//text()");
        conflictFulltextField.nlmPath.add("//fn[@fn-type=\"conflict\"]//text()");
        conflictFulltextField.nlmPath.add("//fn[@type=\"conflict\"]//text()");
        conflictFulltextField.nlmPath.add("//sec[@type=\"conflict\"]//text()");
        conflictFulltextField.nlmPath.add("//sec[@sec-type=\"COI-statement\"]//text()");
        conflictFulltextField.nlmPath.add("//sec[@sec-type=\"conflict\"]//text()");
        fulltextFields.add(conflictFulltextField);
        fulltextLabels.add("conflict_stmt");

        FieldSpecification contributionFulltextField = new FieldSpecification();
        contributionFulltextField.fieldName = "contribution_stmt";
        contributionFulltextField.isTextual = true;
        contributionFulltextField.mergeMultipleValues = true;
        contributionFulltextField.computeDocumentLevelMetrics = true;
        contributionFulltextField.grobidPath.add("//div[@type=\"contribution\"]//text()");
        // Ambiguous in PLOS JATS: fn-type="con" is used for contribution but also for conflict of interest
        // We added type="contribution" for such cases
        contributionFulltextField.nlmPath.add("//fn[@type=\"contribution\"]//text()");
        contributionFulltextField.nlmPath.add("//fn-group[@content-type=\"author-contribution\"]//text()");
        contributionFulltextField.nlmPath.add("//sec[@sec-type=\"contribution\"]//text()");
        contributionFulltextField.nlmPath.add("//sec[@type=\"contribution\"]//text()");
        fulltextFields.add(contributionFulltextField);
        fulltextLabels.add("contribution_stmt");
    }

    public static String grobidCitationContextId = "//ref[@type=\"bibr\"]/@target";
    public static String grobidBibReferenceId = "//listBibl/biblStruct/@id";

    public static String nlmCitationContextId = "//xref[@ref-type=\"bibr\"]/@rid";
    public static String nlmBibReferenceId = "//ref-list/ref/@id";

}
