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

import java.util.List;

/**
 * Specification of field XML paths in different result documents for evaluation.
 */
public class FieldSpecificationFlavors {
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

        // date
        FieldSpecification dateField = new FieldSpecification();
        dateField.fieldName = "date";
        dateField.grobidPath.add("//publicationStmt/date[1]/@when");
        dateField.nlmPath.add("/article/front/article-meta/pub-date[@pub-type=\"pmc-release\"][1]//text()");
        //in bioRxiv: <pub-date pub-type="epub"><year>2014</year></pub-date>
        //        headerFields.add(dateField);
        //        headerLabels.add("date");

        // DOI (header)
        FieldSpecification doiField = new FieldSpecification();
        doiField.fieldName = "doi";
        doiField.grobidPath.add("//sourceDesc/biblStruct/idno[@type=\"DOI\"]/text()");
        doiField.nlmPath.add("/article/front/article-meta/article-id[@pub-id-type=\"doi\"]/text()");
        //        headerFields.add(doiField);
        //        headerLabels.add("doi");

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
    }

    public static String grobidCitationContextId = "//ref[@type=\"bibr\"]/@target";
    public static String grobidBibReferenceId = "//listBibl/biblStruct/@id";

    public static String nlmCitationContextId = "//xref[@ref-type=\"bibr\"]/@rid";
    public static String nlmBibReferenceId = "//ref-list/ref/@id";

}
