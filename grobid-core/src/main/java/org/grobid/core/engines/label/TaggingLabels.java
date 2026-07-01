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
package org.grobid.core.engines.label;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.grobid.core.GrobidModel;
import org.grobid.core.GrobidModels;
import org.grobid.core.engines.tagging.GenericTaggerUtils;
import org.grobid.core.utilities.Pair;

public class TaggingLabels {

    protected static final ConcurrentMap<Pair<GrobidModel, String>, TaggingLabel> cache = new ConcurrentHashMap<>();

    //IOB labels and prefixes
    public static final String IOB_START_ENTITY_LABEL_PREFIX = "B-";
    public static final String IOB_INSIDE_LABEL_PREFIX = "I-";
    public static final String IOB_OTHER_LABEL = "O";

    //ENAMEX NER label and prefixes
    public static final String ENAMEX_START_ENTITY_LABEL_PREFIX = "E-";

    //Grobid generic labels
    public static final String GROBID_START_ENTITY_LABEL_PREFIX = "I-";
    public static final String GROBID_INSIDE_ENTITY_LABEL_PREFIX = "";
    public final static String OTHER_LABEL = "<other>";

    //Grobid specific labels

    public final static String AVAILABILITY_LABEL = "<availability>";
    public final static String FUNDING_LABEL = "<funding>";
    public final static String CONFLICT_OF_INTEREST_LABEL = "<conflict>";
    public final static String AUTHOR_CONTRIBUTION_LABEL = "<contribution>";

    public static final String CITATION_MARKER_LABEL = "<citation_marker>";
    public static final String TABLE_MARKER_LABEL = "<table_marker>";
    public static final String FIGURE_MARKER_LABEL = "<figure_marker>";
    public static final String EQUATION_MARKER_LABEL = "<equation_marker>";

    public final static String PARAGRAPH_LABEL = "<paragraph>";
    public final static String ITEM_LABEL = "<item>";
    public final static String SECTION_LABEL = "<section>";
    public final static String FIGURE_LABEL = "<figure>";
    public final static String TABLE_LABEL = "<table>";
    public final static String EQUATION_LAB = "<equation>";
    public final static String EQUATION_ID_LABEL = "<equation_label>";
    public final static String DESCRIPTION_LABEL = "<figDesc>";
    public final static String HEADER_LABEL = "<figure_head>";
    public final static String CONTENT_LABEL = "<content>";
    public final static String LABEL_LABEL = "<label>";
    public final static String DATE_LABEL = "<date>";
    public final static String DATE_YEAR_LABEL = "<year>";
    public final static String DATE_MONTH_LABEL = "<month>";
    public final static String DATE_DAY_LABEL = "<day>";

    public final static String TITLE_LABEL = "<title>";
    public final static String ABSTRACT_LABEL = "<abstract>";
    public final static String AUTHOR_LABEL = "<author>";
    public final static String TECH_LABEL = "<tech>";
    public final static String LOCATION_LABEL = "<location>";
    public final static String DATESUB_LABEL = "<date-submission>";
    public final static String PAGE_LABEL = "<page>";
    public final static String EDITOR_LABEL = "<editor>";
    public final static String INSTITUTION_LABEL = "<institution>";
    public final static String NOTE_LABEL = "<note>";
    public final static String REFERENCE_LABEL = "<reference>";
    public final static String COPYRIGHT_LABEL = "<copyright>";
    public final static String AFFILIATION_LABEL = "<affiliation>";
    public final static String ADDRESS_LABEL = "<address>";
    public final static String EMAIL_LABEL = "<email>";
    public final static String PUBNUM_LABEL = "<pubnum>";
    public final static String KEYWORD_LABEL = "<keyword>";
    public final static String PHONE_LABEL = "<phone>";
    public final static String DEGREE_LABEL = "<degree>";
    public final static String WEB_LABEL = "<web>";
    public final static String DEDICATION_LABEL = "<dedication>";
    public final static String SUBMISSION_LABEL = "<submission>";
    public final static String ENTITLE_LABEL = "<entitle>";
    //public final static String INTRO_LABEL = "<intro>";
    public final static String VERSION_LABEL = "<version>";
    public final static String DOCTYPE_LABEL = "<doctype>";
    public final static String DOWNLOAD_LABEL = "<date-download>";
    public final static String WORKINGGROUP_LABEL = "<group>";
    public final static String MEETING_LABEL = "<meeting>";

    public final static String COLLABORATION_LABEL = "<collaboration>";
    public final static String JOURNAL_LABEL = "<journal>";
    public final static String BOOKTITLE_LABEL = "<booktitle>";
    public final static String SERIES_LABEL = "<series>";
    public final static String VOLUME_LABEL = "<volume>";
    public final static String ISSUE_LABEL = "<issue>";
    public final static String PAGES_LABEL = "<pages>";
    public final static String PUBLISHER_LABEL = "<publisher>";

    public final static String MARKER_LABEL = "<marker>";
    public final static String FORENAME_LABEL = "<forename>";
    public final static String MIDDLENAME_LABEL = "<middlename>";
    public final static String SURNAME_LABEL = "<surname>";
    public final static String SUFFIX_LABEL = "<suffix>";

    public final static String COVER_LABEL = "<cover>";
    public final static String SUMMARY_LABEL = "<summary>";
    public final static String BIOGRAPHY_LABEL = "<biography>";
    public final static String ADVERTISEMENT_LABEL = "<advertisement>";
    public final static String TOC_LABEL = "<toc>";
    public final static String TOF_LABEL = "<tof>";
    public final static String PREFACE_LABEL = "<preface>";
    public final static String UNIT_LABEL = "<unit>";
    public final static String ANNEX_LABEL = "<annex>";
    public final static String INDEX_LABEL = "<index>";
    public final static String GLOSSARY_LABEL = "<glossary>";
    public final static String BACK_LABEL = "<back>";

    public final static String PATENT_CITATION_PL_LABEL = "<refPatent>";
    public final static String PATENT_CITATION_NPL_LABEL = "<refNPL>";

    public final static String FUNDER_NAME_LABEL = "<funderName>";
    public final static String FUNDER_ABBRV_NAME_LABEL = "<funderAbbrv>";
    public final static String PROGRAM_NAME_LABEL = "<programName>";
    public final static String PROGRAM_ABBRV_NAME_LABEL = "<programAbbrv>";
    public final static String GRANT_NUMBER_LABEL = "<grantNumber>";
    public final static String GRANT_NAME_LABEL = "<grantName>";
    public final static String PROJECT_NAME_LABEL = "<projectName>";
    public final static String PROJECT_ABBRV_NAME_LABEL = "<projectAbbrv>";
    public final static String URL_LABEL = "<url>";
    public final static String PERSON_LABEL = "<person>";
    public final static String INFRASTRUCTURE_LABEL = "<infrastructure>";

    public final static String DEPARTMENT_LABEL = "<department>";
    public final static String LABORATORY_LABEL = "<laboratory>";
    public final static String COUNTRY_LABEL = "<country>";
    public final static String POSTCODE_LABEL = "<postCode>";
    public final static String POSTBOX_LABEL = "<postBox>";
    public final static String REGION_LABEL = "<region>";
    public final static String SETTLEMENT_LABEL = "<settlement>";
    public final static String ADDRESSLINE_LABEL = "<addrLine>";

    /* title page (secondary title page)
     *       publisher page (publication information, including usually the copyrights info)
     *       summary (include executive summary)
     *       biography
     *       advertising (other works by the author/publisher)
     *       table of content
     *       preface (foreword)
     *       dedication (I dedicate this label to my family and my thesis director ;)
     *       unit (chapter or standalone article)
     *       reference (a full chapter of references, not to be confused with references attached to an article)
     *       annex
     *       index
     *       glossary (also abbreviations and acronyms)
     *       back cover page
     *       other
     */

    public static final TaggingLabel CITATION_MARKER = new TaggingLabelImpl(GrobidModels.FULLTEXT,
            CITATION_MARKER_LABEL);
    public static final TaggingLabel TABLE_MARKER = new TaggingLabelImpl(GrobidModels.FULLTEXT, TABLE_MARKER_LABEL);
    public static final TaggingLabel FIGURE_MARKER = new TaggingLabelImpl(GrobidModels.FULLTEXT, FIGURE_MARKER_LABEL);
    public static final TaggingLabel EQUATION_MARKER = new TaggingLabelImpl(GrobidModels.FULLTEXT,
            EQUATION_MARKER_LABEL);
    public static final TaggingLabel PARAGRAPH = new TaggingLabelImpl(GrobidModels.FULLTEXT, PARAGRAPH_LABEL);
    public static final TaggingLabel ITEM = new TaggingLabelImpl(GrobidModels.FULLTEXT, ITEM_LABEL);
    public static final TaggingLabel OTHER = new TaggingLabelImpl(GrobidModels.FULLTEXT, OTHER_LABEL);
    public static final TaggingLabel SECTION = new TaggingLabelImpl(GrobidModels.FULLTEXT, SECTION_LABEL);
    public static final TaggingLabel FIGURE = new TaggingLabelImpl(GrobidModels.FULLTEXT, FIGURE_LABEL);
    public static final TaggingLabel TABLE = new TaggingLabelImpl(GrobidModels.FULLTEXT, TABLE_LABEL);
    public static final TaggingLabel EQUATION = new TaggingLabelImpl(GrobidModels.FULLTEXT, EQUATION_LAB);
    public static final TaggingLabel EQUATION_LABEL = new TaggingLabelImpl(GrobidModels.FULLTEXT, EQUATION_ID_LABEL);

    public static final TaggingLabel HEADER_DATE = new TaggingLabelImpl(GrobidModels.HEADER, DATE_LABEL);
    public static final TaggingLabel HEADER_TITLE = new TaggingLabelImpl(GrobidModels.HEADER, TITLE_LABEL);
    public static final TaggingLabel HEADER_ABSTRACT = new TaggingLabelImpl(GrobidModels.HEADER, ABSTRACT_LABEL);
    public static final TaggingLabel HEADER_AUTHOR = new TaggingLabelImpl(GrobidModels.HEADER, AUTHOR_LABEL);
    public static final TaggingLabel HEADER_TECH = new TaggingLabelImpl(GrobidModels.HEADER, TECH_LABEL);
    public static final TaggingLabel HEADER_LOCATION = new TaggingLabelImpl(GrobidModels.HEADER, LOCATION_LABEL);
    public static final TaggingLabel HEADER_DATESUB = new TaggingLabelImpl(GrobidModels.HEADER, DATESUB_LABEL);
    public static final TaggingLabel HEADER_PAGE = new TaggingLabelImpl(GrobidModels.HEADER, PAGE_LABEL);
    public static final TaggingLabel HEADER_EDITOR = new TaggingLabelImpl(GrobidModels.HEADER, EDITOR_LABEL);
    public static final TaggingLabel HEADER_INSTITUTION = new TaggingLabelImpl(GrobidModels.HEADER, INSTITUTION_LABEL);
    public static final TaggingLabel HEADER_NOTE = new TaggingLabelImpl(GrobidModels.HEADER, NOTE_LABEL);
    public static final TaggingLabel HEADER_OTHER = new TaggingLabelImpl(GrobidModels.HEADER, OTHER_LABEL);
    public static final TaggingLabel HEADER_REFERENCE = new TaggingLabelImpl(GrobidModels.HEADER, REFERENCE_LABEL);
    public static final TaggingLabel HEADER_FUNDING = new TaggingLabelImpl(GrobidModels.HEADER, FUNDING_LABEL);
    public static final TaggingLabel HEADER_COPYRIGHT = new TaggingLabelImpl(GrobidModels.HEADER, COPYRIGHT_LABEL);
    public static final TaggingLabel HEADER_AFFILIATION = new TaggingLabelImpl(GrobidModels.HEADER, AFFILIATION_LABEL);
    public static final TaggingLabel HEADER_ADDRESS = new TaggingLabelImpl(GrobidModels.HEADER, ADDRESS_LABEL);
    public static final TaggingLabel HEADER_EMAIL = new TaggingLabelImpl(GrobidModels.HEADER, EMAIL_LABEL);
    public static final TaggingLabel HEADER_PUBNUM = new TaggingLabelImpl(GrobidModels.HEADER, PUBNUM_LABEL);
    public static final TaggingLabel HEADER_KEYWORD = new TaggingLabelImpl(GrobidModels.HEADER, KEYWORD_LABEL);
    public static final TaggingLabel HEADER_PHONE = new TaggingLabelImpl(GrobidModels.HEADER, PHONE_LABEL);
    public static final TaggingLabel HEADER_DEGREE = new TaggingLabelImpl(GrobidModels.HEADER, DEGREE_LABEL);
    public static final TaggingLabel HEADER_WEB = new TaggingLabelImpl(GrobidModels.HEADER, WEB_LABEL);
    public static final TaggingLabel HEADER_DEDICATION = new TaggingLabelImpl(GrobidModels.HEADER, DEDICATION_LABEL);
    public static final TaggingLabel HEADER_SUBMISSION = new TaggingLabelImpl(GrobidModels.HEADER, SUBMISSION_LABEL);
    public static final TaggingLabel HEADER_ENTITLE = new TaggingLabelImpl(GrobidModels.HEADER, ENTITLE_LABEL);
    //public static final TaggingLabel HEADER_INTRO = new TaggingLabelImpl(GrobidModels.HEADER, INTRO_LABEL);
    public static final TaggingLabel HEADER_COLLABORATION = new TaggingLabelImpl(GrobidModels.HEADER,
            COLLABORATION_LABEL);
    public static final TaggingLabel HEADER_VERSION = new TaggingLabelImpl(GrobidModels.HEADER, VERSION_LABEL);
    public static final TaggingLabel HEADER_DOCTYPE = new TaggingLabelImpl(GrobidModels.HEADER, DOCTYPE_LABEL);
    public static final TaggingLabel HEADER_DOWNLOAD = new TaggingLabelImpl(GrobidModels.HEADER, DOWNLOAD_LABEL);
    public static final TaggingLabel HEADER_WORKINGGROUP = new TaggingLabelImpl(GrobidModels.HEADER,
            WORKINGGROUP_LABEL);
    public static final TaggingLabel HEADER_MEETING = new TaggingLabelImpl(GrobidModels.HEADER, MEETING_LABEL);
    public static final TaggingLabel HEADER_PUBLISHER = new TaggingLabelImpl(GrobidModels.HEADER, PUBLISHER_LABEL);
    public static final TaggingLabel HEADER_JOURNAL = new TaggingLabelImpl(GrobidModels.HEADER, JOURNAL_LABEL);
    public static final TaggingLabel HEADER_AVAILABILITY = new TaggingLabelImpl(GrobidModels.HEADER,
            AVAILABILITY_LABEL);
    public static final TaggingLabel HEADER_CONFLICT_OF_INTEREST = new TaggingLabelImpl(GrobidModels.HEADER,
            CONFLICT_OF_INTEREST_LABEL);
    public static final TaggingLabel HEADER_AUTHOR_CONTRIBUTION = new TaggingLabelImpl(GrobidModels.HEADER,
            AUTHOR_CONTRIBUTION_LABEL);

    public static final TaggingLabel DATE_YEAR = new TaggingLabelImpl(GrobidModels.DATE, DATE_YEAR_LABEL);
    public static final TaggingLabel DATE_MONTH = new TaggingLabelImpl(GrobidModels.DATE, DATE_MONTH_LABEL);
    public static final TaggingLabel DATE_DAY = new TaggingLabelImpl(GrobidModels.DATE, DATE_DAY_LABEL);

    public static final TaggingLabel FIG_DESC = new TaggingLabelImpl(GrobidModels.FIGURE, DESCRIPTION_LABEL);
    public static final TaggingLabel FIG_HEAD = new TaggingLabelImpl(GrobidModels.FIGURE, HEADER_LABEL);
    public static final TaggingLabel FIG_CONTENT = new TaggingLabelImpl(GrobidModels.FIGURE, CONTENT_LABEL);
    public static final TaggingLabel FIG_LABEL = new TaggingLabelImpl(GrobidModels.FIGURE, LABEL_LABEL);
    public static final TaggingLabel FIG_OTHER = new TaggingLabelImpl(GrobidModels.FIGURE, OTHER_LABEL);

    public static final TaggingLabel TBL_DESC = new TaggingLabelImpl(GrobidModels.TABLE, DESCRIPTION_LABEL);
    public static final TaggingLabel TBL_HEAD = new TaggingLabelImpl(GrobidModels.TABLE, HEADER_LABEL);
    public static final TaggingLabel TBL_CONTENT = new TaggingLabelImpl(GrobidModels.TABLE, CONTENT_LABEL);
    public static final TaggingLabel TBL_LABEL = new TaggingLabelImpl(GrobidModels.TABLE, LABEL_LABEL);
    public static final TaggingLabel TBL_OTHER = new TaggingLabelImpl(GrobidModels.TABLE, OTHER_LABEL);
    public static final TaggingLabel TBL_NOTE = new TaggingLabelImpl(GrobidModels.TABLE, NOTE_LABEL);

    public static final TaggingLabel CITATION_TITLE = new TaggingLabelImpl(GrobidModels.CITATION, TITLE_LABEL);
    public static final TaggingLabel CITATION_JOURNAL = new TaggingLabelImpl(GrobidModels.CITATION, JOURNAL_LABEL);
    public static final TaggingLabel CITATION_BOOKTITLE = new TaggingLabelImpl(GrobidModels.CITATION, BOOKTITLE_LABEL);
    public static final TaggingLabel CITATION_COLLABORATION = new TaggingLabelImpl(GrobidModels.CITATION,
            COLLABORATION_LABEL);
    public static final TaggingLabel CITATION_AUTHOR = new TaggingLabelImpl(GrobidModels.CITATION, AUTHOR_LABEL);
    public static final TaggingLabel CITATION_EDITOR = new TaggingLabelImpl(GrobidModels.CITATION, EDITOR_LABEL);
    public static final TaggingLabel CITATION_DATE = new TaggingLabelImpl(GrobidModels.CITATION, DATE_LABEL);
    public static final TaggingLabel CITATION_INSTITUTION = new TaggingLabelImpl(GrobidModels.CITATION,
            INSTITUTION_LABEL);
    public static final TaggingLabel CITATION_NOTE = new TaggingLabelImpl(GrobidModels.CITATION, NOTE_LABEL);
    public static final TaggingLabel CITATION_TECH = new TaggingLabelImpl(GrobidModels.CITATION, TECH_LABEL);
    public static final TaggingLabel CITATION_VOLUME = new TaggingLabelImpl(GrobidModels.CITATION, VOLUME_LABEL);
    public static final TaggingLabel CITATION_ISSUE = new TaggingLabelImpl(GrobidModels.CITATION, ISSUE_LABEL);
    public static final TaggingLabel CITATION_PAGES = new TaggingLabelImpl(GrobidModels.CITATION, PAGES_LABEL);
    public static final TaggingLabel CITATION_LOCATION = new TaggingLabelImpl(GrobidModels.CITATION, LOCATION_LABEL);
    public static final TaggingLabel CITATION_PUBLISHER = new TaggingLabelImpl(GrobidModels.CITATION, PUBLISHER_LABEL);
    public static final TaggingLabel CITATION_WEB = new TaggingLabelImpl(GrobidModels.CITATION, WEB_LABEL);
    public static final TaggingLabel CITATION_PUBNUM = new TaggingLabelImpl(GrobidModels.CITATION, PUBNUM_LABEL);
    public static final TaggingLabel CITATION_SERIES = new TaggingLabelImpl(GrobidModels.CITATION, SERIES_LABEL);
    public static final TaggingLabel CITATION_OTHER = new TaggingLabelImpl(GrobidModels.CITATION, OTHER_LABEL);

    public static final TaggingLabel NAMES_HEADER_MARKER = new TaggingLabelImpl(GrobidModels.NAMES_HEADER,
            MARKER_LABEL);
    public static final TaggingLabel NAMES_HEADER_TITLE = new TaggingLabelImpl(GrobidModels.NAMES_HEADER, TITLE_LABEL);
    public static final TaggingLabel NAMES_HEADER_FORENAME = new TaggingLabelImpl(GrobidModels.NAMES_HEADER,
            FORENAME_LABEL);
    public static final TaggingLabel NAMES_HEADER_MIDDLENAME = new TaggingLabelImpl(GrobidModels.NAMES_HEADER,
            MIDDLENAME_LABEL);
    public static final TaggingLabel NAMES_HEADER_SURNAME = new TaggingLabelImpl(GrobidModels.NAMES_HEADER,
            SURNAME_LABEL);
    public static final TaggingLabel NAMES_HEADER_SUFFIX = new TaggingLabelImpl(GrobidModels.NAMES_HEADER,
            SUFFIX_LABEL);

    public static final TaggingLabel NAMES_CITATION_TITLE = new TaggingLabelImpl(GrobidModels.NAMES_CITATION,
            TITLE_LABEL);
    public static final TaggingLabel NAMES_CITATION_FORENAME = new TaggingLabelImpl(GrobidModels.NAMES_CITATION,
            FORENAME_LABEL);
    public static final TaggingLabel NAMES_CITATION_MIDDLENAME = new TaggingLabelImpl(GrobidModels.NAMES_CITATION,
            MIDDLENAME_LABEL);
    public static final TaggingLabel NAMES_CITATION_SURNAME = new TaggingLabelImpl(GrobidModels.NAMES_CITATION,
            SURNAME_LABEL);
    public static final TaggingLabel NAMES_CITATION_SUFFIX = new TaggingLabelImpl(GrobidModels.NAMES_CITATION,
            SUFFIX_LABEL);

    public static final TaggingLabel PATENT_CITATION_PL = new TaggingLabelImpl(GrobidModels.PATENT_CITATION,
            PATENT_CITATION_PL_LABEL);
    public static final TaggingLabel PATENT_CITATION_NPL = new TaggingLabelImpl(GrobidModels.PATENT_CITATION,
            PATENT_CITATION_NPL_LABEL);

    public static final TaggingLabel MONOGRAPH_COVER = new TaggingLabelImpl(GrobidModels.MONOGRAPH, COVER_LABEL);
    public static final TaggingLabel MONOGRAPH_TITLE = new TaggingLabelImpl(GrobidModels.MONOGRAPH, TITLE_LABEL);
    public static final TaggingLabel MONOGRAPH_PUBLISHER = new TaggingLabelImpl(GrobidModels.MONOGRAPH,
            PUBLISHER_LABEL);
    public static final TaggingLabel MONOGRAPH_SUMMARY = new TaggingLabelImpl(GrobidModels.MONOGRAPH, SUMMARY_LABEL);
    public static final TaggingLabel MONOGRAPH_BIOGRAPHY = new TaggingLabelImpl(GrobidModels.MONOGRAPH,
            BIOGRAPHY_LABEL);
    public static final TaggingLabel MONOGRAPH_ADVERTISEMENT = new TaggingLabelImpl(GrobidModels.MONOGRAPH,
            ADVERTISEMENT_LABEL);
    public static final TaggingLabel MONOGRAPH_TOC = new TaggingLabelImpl(GrobidModels.MONOGRAPH, TOC_LABEL);
    public static final TaggingLabel MONOGRAPH_TOF = new TaggingLabelImpl(GrobidModels.MONOGRAPH, TOF_LABEL);
    public static final TaggingLabel MONOGRAPH_PREFACE = new TaggingLabelImpl(GrobidModels.MONOGRAPH, PREFACE_LABEL);
    public static final TaggingLabel MONOGRAPH_DEDICATION = new TaggingLabelImpl(GrobidModels.MONOGRAPH,
            DEDICATION_LABEL);
    public static final TaggingLabel MONOGRAPH_UNIT = new TaggingLabelImpl(GrobidModels.MONOGRAPH, UNIT_LABEL);
    public static final TaggingLabel MONOGRAPH_REFERENCE = new TaggingLabelImpl(GrobidModels.MONOGRAPH,
            REFERENCE_LABEL);
    public static final TaggingLabel MONOGRAPH_ANNEX = new TaggingLabelImpl(GrobidModels.MONOGRAPH, ANNEX_LABEL);
    public static final TaggingLabel MONOGRAPH_INDEX = new TaggingLabelImpl(GrobidModels.MONOGRAPH, INDEX_LABEL);
    public static final TaggingLabel MONOGRAPH_GLOSSARY = new TaggingLabelImpl(GrobidModels.MONOGRAPH, GLOSSARY_LABEL);
    public static final TaggingLabel MONOGRAPH_BACK = new TaggingLabelImpl(GrobidModels.MONOGRAPH, BACK_LABEL);
    public static final TaggingLabel MONOGRAPH_OTHER = new TaggingLabelImpl(GrobidModels.MONOGRAPH, OTHER_LABEL);

    public static final TaggingLabel FUNDING_FUNDER_NAME = new TaggingLabelImpl(GrobidModels.FUNDING_ACKNOWLEDGEMENT,
            FUNDER_NAME_LABEL);
    public static final TaggingLabel FUNDING_FUNDER_ABBRV_NAME = new TaggingLabelImpl(
            GrobidModels.FUNDING_ACKNOWLEDGEMENT, FUNDER_ABBRV_NAME_LABEL);
    public static final TaggingLabel FUNDING_PROGRAM_NAME = new TaggingLabelImpl(GrobidModels.FUNDING_ACKNOWLEDGEMENT,
            PROGRAM_NAME_LABEL);
    public static final TaggingLabel FUNDING_PROGRAM_ABBRV_NAME = new TaggingLabelImpl(
            GrobidModels.FUNDING_ACKNOWLEDGEMENT, PROGRAM_ABBRV_NAME_LABEL);
    public static final TaggingLabel FUNDING_GRANT_NUMBER = new TaggingLabelImpl(GrobidModels.FUNDING_ACKNOWLEDGEMENT,
            GRANT_NUMBER_LABEL);
    public static final TaggingLabel FUNDING_GRANT_NAME = new TaggingLabelImpl(GrobidModels.FUNDING_ACKNOWLEDGEMENT,
            GRANT_NAME_LABEL);
    public static final TaggingLabel FUNDING_PROJECT_NAME = new TaggingLabelImpl(GrobidModels.FUNDING_ACKNOWLEDGEMENT,
            PROJECT_NAME_LABEL);
    public static final TaggingLabel FUNDING_PROJECT_ABBRV_NAME = new TaggingLabelImpl(
            GrobidModels.FUNDING_ACKNOWLEDGEMENT, PROJECT_ABBRV_NAME_LABEL);
    public static final TaggingLabel FUNDING_URL = new TaggingLabelImpl(GrobidModels.FUNDING_ACKNOWLEDGEMENT,
            URL_LABEL);
    public static final TaggingLabel FUNDING_PERSON = new TaggingLabelImpl(GrobidModels.FUNDING_ACKNOWLEDGEMENT,
            PERSON_LABEL);
    public static final TaggingLabel FUNDING_INSTITUTION = new TaggingLabelImpl(GrobidModels.FUNDING_ACKNOWLEDGEMENT,
            INSTITUTION_LABEL);
    public static final TaggingLabel FUNDING_INFRASTRUCTURE = new TaggingLabelImpl(GrobidModels.FUNDING_ACKNOWLEDGEMENT,
            INFRASTRUCTURE_LABEL);
    public static final TaggingLabel FUNDING_AFFILIATION = new TaggingLabelImpl(GrobidModels.FUNDING_ACKNOWLEDGEMENT,
            AFFILIATION_LABEL);
    public static final TaggingLabel FUNDING_OTHER = new TaggingLabelImpl(GrobidModels.FUNDING_ACKNOWLEDGEMENT,
            OTHER_LABEL);

    public static final TaggingLabel AFFILIATION_MARKER = new TaggingLabelImpl(GrobidModels.AFFILIATION_ADDRESS,
            MARKER_LABEL);
    public static final TaggingLabel AFFILIATION_INSTITUTION = new TaggingLabelImpl(GrobidModels.AFFILIATION_ADDRESS,
            INSTITUTION_LABEL);
    public static final TaggingLabel AFFILIATION_DEPARTMENT = new TaggingLabelImpl(GrobidModels.AFFILIATION_ADDRESS,
            DEPARTMENT_LABEL);
    public static final TaggingLabel AFFILIATION_LABORATORY = new TaggingLabelImpl(GrobidModels.AFFILIATION_ADDRESS,
            LABORATORY_LABEL);
    public static final TaggingLabel AFFILIATION_COUNTRY = new TaggingLabelImpl(GrobidModels.AFFILIATION_ADDRESS,
            COUNTRY_LABEL);
    public static final TaggingLabel AFFILIATION_POSTCODE = new TaggingLabelImpl(GrobidModels.AFFILIATION_ADDRESS,
            POSTCODE_LABEL);
    public static final TaggingLabel AFFILIATION_POSTBOX = new TaggingLabelImpl(GrobidModels.AFFILIATION_ADDRESS,
            POSTBOX_LABEL);
    public static final TaggingLabel AFFILIATION_REGION = new TaggingLabelImpl(GrobidModels.AFFILIATION_ADDRESS,
            REGION_LABEL);
    public static final TaggingLabel AFFILIATION_SETTLEMENT = new TaggingLabelImpl(GrobidModels.AFFILIATION_ADDRESS,
            SETTLEMENT_LABEL);
    public static final TaggingLabel AFFILIATION_ADDRESSLINE = new TaggingLabelImpl(GrobidModels.AFFILIATION_ADDRESS,
            ADDRESSLINE_LABEL);
    public static final TaggingLabel AFFILIATION_OTHER = new TaggingLabelImpl(GrobidModels.AFFILIATION_ADDRESS,
            OTHER_LABEL);

    protected static void register(TaggingLabel label) {
        cache.putIfAbsent(new Pair<>(label.getGrobidModel(), label.getLabel()), label);
    }

    static {
        //fulltext
        register(CITATION_MARKER);
        register(TABLE_MARKER);
        register(FIGURE_MARKER);
        register(EQUATION_MARKER);
        register(PARAGRAPH);
        register(ITEM);
        register(OTHER);
        register(SECTION);
        register(FIGURE);
        register(TABLE);
        register(EQUATION);
        register(EQUATION_LABEL);

        //header
        register(HEADER_DATE);
        register(HEADER_TITLE);
        register(HEADER_ABSTRACT);
        register(HEADER_AUTHOR);
        //register(HEADER_LOCATION);
        //register(HEADER_DATESUB);
        register(HEADER_EDITOR);
        //register(HEADER_INSTITUTION);
        register(HEADER_NOTE);
        register(HEADER_OTHER);
        register(HEADER_REFERENCE);
        register(HEADER_FUNDING);
        register(HEADER_COPYRIGHT);
        register(HEADER_AFFILIATION);
        register(HEADER_ADDRESS);
        register(HEADER_EMAIL);
        register(HEADER_PUBNUM);
        register(HEADER_KEYWORD);
        register(HEADER_PHONE);
        //register(HEADER_DEGREE);
        register(HEADER_WEB);
        //register(HEADER_DEDICATION);
        register(HEADER_SUBMISSION);
        //register(HEADER_ENTITLE);
        //register(HEADER_INTRO);
        //register(HEADER_COLLABORATION);
        //register(HEADER_VERSION);
        register(HEADER_DOCTYPE);
        //register(HEADER_DOWNLOAD);
        register(HEADER_WORKINGGROUP);
        register(HEADER_MEETING);
        register(HEADER_PUBLISHER);
        register(HEADER_JOURNAL);
        register(HEADER_PAGE);
        register(HEADER_AVAILABILITY);
        register(HEADER_CONFLICT_OF_INTEREST);
        register(HEADER_AUTHOR_CONTRIBUTION);

        //date
        register(DATE_YEAR);
        register(DATE_MONTH);
        register(DATE_DAY);

        //figures
        register(FIG_DESC);
        register(FIG_HEAD);
        register(FIG_CONTENT);
        register(FIG_LABEL);
        register(FIG_OTHER);

        // table
        register(TBL_DESC);
        register(TBL_HEAD);
        register(TBL_CONTENT);
        register(TBL_LABEL);
        register(TBL_OTHER);

        // citation
        register(CITATION_TITLE);
        register(CITATION_JOURNAL);
        register(CITATION_BOOKTITLE);
        register(CITATION_COLLABORATION);
        register(CITATION_AUTHOR);
        register(CITATION_EDITOR);
        register(CITATION_DATE);
        register(CITATION_INSTITUTION);
        register(CITATION_NOTE);
        register(CITATION_TECH);
        register(CITATION_VOLUME);
        register(CITATION_ISSUE);
        register(CITATION_PAGES);
        register(CITATION_LOCATION);
        register(CITATION_PUBLISHER);
        register(CITATION_WEB);
        register(CITATION_PUBNUM);
        register(CITATION_OTHER);
        register(CITATION_SERIES);

        // person names
        register(NAMES_HEADER_MARKER);
        register(NAMES_HEADER_TITLE);
        register(NAMES_HEADER_FORENAME);
        register(NAMES_HEADER_MIDDLENAME);
        register(NAMES_HEADER_SURNAME);
        register(NAMES_HEADER_SUFFIX);

        register(NAMES_CITATION_TITLE);
        register(NAMES_CITATION_FORENAME);
        register(NAMES_CITATION_MIDDLENAME);
        register(NAMES_CITATION_SURNAME);
        register(NAMES_CITATION_SUFFIX);

        // citations in patent
        register(PATENT_CITATION_PL);
        register(PATENT_CITATION_NPL);

        // monograph
        register(MONOGRAPH_COVER);
        register(MONOGRAPH_TITLE);
        register(MONOGRAPH_PUBLISHER);
        register(MONOGRAPH_BIOGRAPHY);
        register(MONOGRAPH_SUMMARY);
        register(MONOGRAPH_ADVERTISEMENT);
        register(MONOGRAPH_TOC);
        register(MONOGRAPH_TOF);
        register(MONOGRAPH_PREFACE);
        register(MONOGRAPH_DEDICATION);
        register(MONOGRAPH_UNIT);
        register(MONOGRAPH_REFERENCE);
        register(MONOGRAPH_ANNEX);
        register(MONOGRAPH_INDEX);
        register(MONOGRAPH_GLOSSARY);
        register(MONOGRAPH_BACK);
        register(MONOGRAPH_OTHER);

        // funding-acknowledgement
        register(FUNDING_FUNDER_NAME);
        register(FUNDING_FUNDER_ABBRV_NAME);
        register(FUNDING_PROGRAM_NAME);
        register(FUNDING_PROGRAM_ABBRV_NAME);
        register(FUNDING_GRANT_NUMBER);
        register(FUNDING_GRANT_NAME);
        register(FUNDING_PROJECT_NAME);
        register(FUNDING_PROJECT_ABBRV_NAME);
        register(FUNDING_URL);
        register(FUNDING_PERSON);
        register(FUNDING_AFFILIATION);
        register(FUNDING_INSTITUTION);
        register(FUNDING_OTHER);
        register(FUNDING_INFRASTRUCTURE);

        // affiliation-address
        register(AFFILIATION_MARKER);
        register(AFFILIATION_INSTITUTION);
        register(AFFILIATION_DEPARTMENT);
        register(AFFILIATION_LABORATORY);
        register(AFFILIATION_COUNTRY);
        register(AFFILIATION_POSTCODE);
        register(AFFILIATION_POSTBOX);
        register(AFFILIATION_REGION);
        register(AFFILIATION_SETTLEMENT);
        register(AFFILIATION_ADDRESSLINE);
        register(AFFILIATION_OTHER);
    }

    protected TaggingLabels() {
    }

    public static TaggingLabel labelFor(final GrobidModel model, final String label) {
        final String plainLabel = GenericTaggerUtils.getPlainLabel(label);

        cache.putIfAbsent(
                new Pair<>(model, plainLabel.toString(/*null-check*/)),
                new TaggingLabelImpl(model, plainLabel));

        return cache.get(new Pair(model, plainLabel));
    }
}
