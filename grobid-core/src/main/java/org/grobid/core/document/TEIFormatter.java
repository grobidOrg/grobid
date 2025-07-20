package org.grobid.core.document;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.*;
import org.grobid.core.data.CopyrightsLicense.License;
import org.grobid.core.data.CopyrightsLicense.CopyrightsOwner;
import org.grobid.core.data.Date;
import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.FullTextParser;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.*;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.*;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.matching.EntityMatcherException;
import org.grobid.core.utilities.matching.ReferenceMarkerMatcher;
import org.grobid.core.engines.citations.CalloutAnalyzer.MarkerType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


import static org.grobid.core.document.xml.XmlBuilderUtils.teiElement;
import static org.grobid.core.document.xml.XmlBuilderUtils.addXmlId;
import static org.grobid.core.document.xml.XmlBuilderUtils.textNode;

/**
 * Class for generating a TEI representation of a document.
 *
 */
@SuppressWarnings("StringConcatenationInsideStringBuilderAppend")
public class TEIFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TEIFormatter.class);

    private Document doc = null;
    private FullTextParser fullTextParser = null;
    public static final Set<TaggingLabel> MARKER_LABELS = Sets.newHashSet(
            TaggingLabels.CITATION_MARKER,
            TaggingLabels.FIGURE_MARKER,
            TaggingLabels.TABLE_MARKER,
            TaggingLabels.EQUATION_MARKER);

    // possible association to Grobid customised TEI schemas: DTD, XML schema, RelaxNG or compact RelaxNG
    // DEFAULT means no schema association in the generated XML documents
    public enum SchemaDeclaration {
        DEFAULT, DTD, XSD, RNG, RNC
    }

    private Boolean inParagraph = false;

    private ArrayList<String> elements = null;

    // static variable for the position of italic and bold features in the CRF model
    private static final int ITALIC_POS = 16;
    private static final int BOLD_POS = 15;

    private static Pattern numberRef = Pattern.compile("(\\[|\\()\\d+\\w?(\\)|\\])");
    private static Pattern numberRefCompact =
            Pattern.compile("(\\[|\\()((\\d)+(\\w)?(\\-\\d+\\w?)?,\\s?)+(\\d+\\w?)(\\-\\d+\\w?)?(\\)|\\])");
    private static Pattern numberRefCompact2 = Pattern.compile("(\\[|\\()(\\d+)(-|‒|–|—|―|\u2013)(\\d+)(\\)|\\])");

    private static Pattern startNum = Pattern.compile("^(\\d+\\.?\\s)(.*)");

    private static final String SCHEMA_XSD_LOCATION = "https://raw.githubusercontent.com/kermitt2/grobid/master/grobid-home/schemas/xsd/Grobid.xsd";
    private static final String SCHEMA_DTD_LOCATION = "https://raw.githubusercontent.com/kermitt2/grobid/master/grobid-home/schemas/dtd/Grobid.dtd";
    private static final String SCHEMA_RNG_LOCATION = "https://raw.githubusercontent.com/kermitt2/grobid/master/grobid-home/schemas/rng/Grobid.rng";

    public TEIFormatter(Document document, FullTextParser fullTextParser) {
        this.doc = document;
        this.fullTextParser = fullTextParser;
    }

    public StringBuilder toTEIHeader(BiblioItem biblio,
                                     String defaultPublicationStatement,
                                     List<BibDataSet> bds,
                                     List<MarkerType> markerTypes,
                                     List<Funding> fundings,
                                     GrobidAnalysisConfig config) {
        return toTEIHeader(
            biblio,
            SchemaDeclaration.XSD,
            defaultPublicationStatement,
            bds,
            markerTypes,
            fundings,
            config
        );
    }

    public StringBuilder toTEIHeader(
        BiblioItem biblio,
        SchemaDeclaration schemaDeclaration,
        String defaultPublicationStatement,
        List<BibDataSet> bds,
        List<MarkerType> markerTypes,
        List<Funding> fundings,
        GrobidAnalysisConfig config) {
        StringBuilder tei = new StringBuilder();
        tei.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        if (config.isWithXslStylesheet()) {
            tei.append("<?xml-stylesheet type=\"text/xsl\" href=\"../jsp/xmlverbatimwrapper.xsl\"?> \n");
        }
        if (schemaDeclaration == SchemaDeclaration.DTD) {
            tei.append("<!DOCTYPE TEI SYSTEM \"" + SCHEMA_DTD_LOCATION + "\">\n");
        } else if (schemaDeclaration == SchemaDeclaration.XSD) {
            // XML schema
            tei.append("<TEI xml:space=\"preserve\" xmlns=\"http://www.tei-c.org/ns/1.0\" \n" +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                    "xsi:schemaLocation=\"http://www.tei-c.org/ns/1.0 " +
                    SCHEMA_XSD_LOCATION +
                    "\"\n xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n");
//				"\n xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">\n");
        } else if (schemaDeclaration == SchemaDeclaration.RNG) {
            // standard RelaxNG
            tei.append("<?xml-model href=\"" + SCHEMA_RNG_LOCATION +
                    "\" schematypens=\"http://relaxng.org/ns/structure/1.0\"?>\n");
        }

        // by default there is no schema association
        if (schemaDeclaration != SchemaDeclaration.XSD) {
            tei.append("<TEI xml:space=\"preserve\" xmlns=\"http://www.tei-c.org/ns/1.0\">\n");
        }

        if (doc.getLanguage() != null) {
            tei.append("\t<teiHeader xml:lang=\"" + doc.getLanguage() + "\">");
        } else {
            tei.append("\t<teiHeader>");
        }

        tei.append("\n\t\t<fileDesc>\n\t\t\t<titleStmt>\n\t\t\t\t<title level=\"a\" type=\"main\"");
        if (config.isGenerateTeiIds()) {
            String divID = KeyGen.getKey().substring(0, 7);
            tei.append(" xml:id=\"_" + divID + "\"");
        }

        if (config.isGenerateTeiCoordinates("title")) {
            List<LayoutToken> titleTokens = biblio.getLayoutTokens(TaggingLabels.HEADER_TITLE);
            if (CollectionUtils.isNotEmpty(titleTokens)) {
                String coords = LayoutTokensUtil.getCoordsString(titleTokens);
                tei.append(" coords=\"" + coords + "\"");
            }
        }

        tei.append(">");

        if (biblio == null) {
            // if the biblio object is null, we simply create an empty one
            biblio = new BiblioItem();
        }

        if (biblio.getTitle() != null) {
            tei.append(TextUtilities.HTMLEncode(biblio.getTitle()));
        }

        tei.append("</title>\n");

        if (CollectionUtils.isNotEmpty(fundings)) {
            Map<String,Funder> funderSignatures = new TreeMap<>();
            for(Funding funding : fundings) {
                if (funding.getFunder() != null && funding.getFunder().getFullName() != null) {
                    if (funderSignatures.get(funding.getFunder().getFullName()) == null) {
                        funderSignatures.put(funding.getFunder().getFullName(), funding.getFunder());
                    } else {
                        funding.setFunder(funderSignatures.get(funding.getFunder().getFullName()));
                    }
                }
            }

            Map<Funder,List<Funding>> fundingRelation = new HashMap<>();
            for(Funding funding : fundings) {
                if (funding.getFunder() == null) {
                    List<Funding> localfundings = fundingRelation.get(Funder.EMPTY);
                    if (localfundings == null)
                        localfundings = new ArrayList<>();
                    localfundings.add(funding);
                    fundingRelation.put(Funder.EMPTY, localfundings);
                } else {
                    List<Funding> localfundings = fundingRelation.get(funding.getFunder());
                    if (localfundings == null)
                        localfundings = new ArrayList<>();
                    localfundings.add(funding);
                    fundingRelation.put(funding.getFunder(), localfundings);
                }
            }

            List<Funder> localFunders = new ArrayList<>();
            for (Map.Entry<Funder, List<Funding>> entry : fundingRelation.entrySet()) {
                localFunders.add(entry.getKey());
            }

            Map<Integer,Funder> consolidatedFunders = null;
            if (config.getConsolidateFunders() != 0) {
                consolidatedFunders = Consolidation.getInstance().consolidateFunders(localFunders);
            }

            int n =0;
            for (Map.Entry<Funder, List<Funding>> entry : fundingRelation.entrySet()) {
                String funderPiece = null;
                Funder consolidatedFunder = null;
                if (consolidatedFunders != null) {
                    consolidatedFunder = consolidatedFunders.get(n);
                }

                if (consolidatedFunder != null && config.getConsolidateFunders() == 1) {
                    funderPiece = consolidatedFunder.toTEI(4);
                } else if (consolidatedFunder != null && config.getConsolidateFunders() == 2) {
                    Funder localFunder = entry.getKey();
                    localFunder.setDoi(consolidatedFunder.getDoi());
                    funderPiece = localFunder.toTEI(4);
                } else {
                    funderPiece = entry.getKey().toTEI(4);
                }

                // inject funding ref in the funder entries
                StringBuilder referenceString = new StringBuilder();
                for(Funding funderFunding : entry.getValue()) {
                    if (funderFunding.isNonEmptyFunding())
                        referenceString.append(" #").append(funderFunding.getIdentifier());
                }

                if (funderPiece != null) {
                    if (referenceString.length()>0)
                        funderPiece = funderPiece.replace("<funder>", "<funder ref=\"" + referenceString.toString().trim() + "\">");
                    tei.append(funderPiece);
                }
                n++;
            }
        }

        tei.append("\t\t\t</titleStmt>\n");

        if ((biblio.getPublisher() != null) ||
                (biblio.getPublicationDate() != null) ||
                (biblio.getNormalizedPublicationDate() != null) ||
                biblio.getCopyrightsLicense() != null) {
            tei.append("\t\t\t<publicationStmt>\n");

            CopyrightsLicense copyrightsLicense = biblio.getCopyrightsLicense();

            if (biblio.getPublisher() != null) {
                // publisher and date under <publicationStmt> for better TEI conformance
                tei.append("\t\t\t\t<publisher>" + TextUtilities.HTMLEncode(biblio.getPublisher()) +
                        "</publisher>\n");
            } else {
                // a dummy publicationStmt is still necessary according to TEI
                tei.append("\t\t\t\t<publisher/>\n");
            }

            // We introduce something more meaningful with TEI customization to encode copyrights information:
            // - @resp with value "publisher", "authors", "unknown", we add a comment to clarify that @resp
            //   should be interpreted as the copyrights owner
            // - license related to copyrights exception is encoded via <licence>
            // (note: I have no clue what can mean "free" as status for a document - there are always some sort of
            // restrictions like moral rights even for public domain documents)
            if (copyrightsLicense != null) {
                tei.append("\t\t\t\t<availability ");

                boolean addCopyrightsComment = false;
                if (copyrightsLicense.getCopyrightsOwner() != null && copyrightsLicense.getCopyrightsOwner() != CopyrightsOwner.UNDECIDED) {
                    tei.append("resp=\""+ copyrightsLicense.getCopyrightsOwner().getName() +"\" ");
                    addCopyrightsComment = true;
                }

                if (copyrightsLicense.getLicense() != null && copyrightsLicense.getLicense() != License.UNDECIDED) {
                    tei.append("status=\"restricted\">\n");
                    if (addCopyrightsComment) {
                        tei.append("\t\t\t\t\t<!-- the @rest attribute above gives the document copyrights owner (publisher, authors), if known -->\n");
                    }
                    tei.append("\t\t\t\t\t<licence>"+copyrightsLicense.getLicense().getName()+"</licence>\n");
                } else {
                    tei.append(" status=\"unknown\">\n");
                    if (addCopyrightsComment) {
                        tei.append("\t\t\t\t\t<!-- the @rest attribute above gives the document copyrights owner (publisher, authors), if known -->\n");
                    }
                    tei.append("\t\t\t\t\t<licence/>\n");
                }

                if (config.getIncludeRawCopyrights() && biblio.getCopyright() != null && biblio.getCopyright().length()>0) {
                    tei.append("\t\t\t\t\t<p type=\"raw\">");
                    tei.append(TextUtilities.HTMLEncode(biblio.getCopyright()));
                    tei.append("</p>\n");
                }

                tei.append("\t\t\t\t</availability>\n");
            } else {
                tei.append("\t\t\t\t<availability ");

                tei.append(" status=\"unknown\">\n");
                tei.append("\t\t\t\t\t<licence/>\n");

                if (defaultPublicationStatement != null) {
                    tei.append("\t\t\t\t\t<p>" +
                            TextUtilities.HTMLEncode(defaultPublicationStatement) + "</p>\n");
                }

                if (config.getIncludeRawCopyrights() && biblio.getCopyright() != null && biblio.getCopyright().length()>0) {
                    tei.append("\t\t\t\t\t<p type=\"raw\">");
                    tei.append(TextUtilities.HTMLEncode(biblio.getCopyright()));
                    tei.append("</p>\n");
                }

                tei.append("\t\t\t\t</availability>\n");
            }

            if (biblio.getNormalizedPublicationDate() != null) {
                Date date = biblio.getNormalizedPublicationDate();

                String when = Date.toISOString(date);
                if (StringUtils.isNotBlank(when)) {
                    tei.append("\t\t\t\t<date type=\"published\" when=\"");
                    tei.append(when).append("\">");
                } else {
                    tei.append("\t\t\t\t<date>");
                }

                if (biblio.getPublicationDate() != null) {
                    tei.append(TextUtilities.HTMLEncode(biblio.getPublicationDate()));
                } else {
                    tei.append(when);
                }
                tei.append("</date>\n");
            } else if ((biblio.getYear() != null) && (biblio.getYear().length() > 0)) {
                String when = "";
                if (biblio.getYear().length() == 1)
                    when += "000" + biblio.getYear();
                else if (biblio.getYear().length() == 2)
                    when += "00" + biblio.getYear();
                else if (biblio.getYear().length() == 3)
                    when += "0" + biblio.getYear();
                else if (biblio.getYear().length() == 4)
                    when += biblio.getYear();

                if ((biblio.getMonth() != null) && (biblio.getMonth().length() > 0)) {
                    if (biblio.getMonth().length() == 1)
                        when += "-0" + biblio.getMonth();
                    else
                        when += "-" + biblio.getMonth();
                    if ((biblio.getDay() != null) && (biblio.getDay().length() > 0)) {
                        if (biblio.getDay().length() == 1)
                            when += "-0" + biblio.getDay();
                        else
                            when += "-" + biblio.getDay();
                    }
                }
                tei.append("\t\t\t\t<date type=\"published\" when=\"");
                tei.append(when + "\">");
                if (biblio.getPublicationDate() != null) {
                    tei.append(TextUtilities.HTMLEncode(biblio.getPublicationDate()));
                } else {
                    tei.append(when);
                }
                tei.append("</date>\n");
            } else if (biblio.getE_Year() != null) {
                String when = "";
                if (biblio.getE_Year().length() == 1)
                    when += "000" + biblio.getE_Year();
                else if (biblio.getE_Year().length() == 2)
                    when += "00" + biblio.getE_Year();
                else if (biblio.getE_Year().length() == 3)
                    when += "0" + biblio.getE_Year();
                else if (biblio.getE_Year().length() == 4)
                    when += biblio.getE_Year();

                if (biblio.getE_Month() != null) {
                    if (biblio.getE_Month().length() == 1)
                        when += "-0" + biblio.getE_Month();
                    else
                        when += "-" + biblio.getE_Month();

                    if (biblio.getE_Day() != null) {
                        if (biblio.getE_Day().length() == 1)
                            when += "-0" + biblio.getE_Day();
                        else
                            when += "-" + biblio.getE_Day();
                    }
                }
                tei.append("\t\t\t\t<date type=\"ePublished\" when=\"");
                tei.append(when + "\">");
                if (biblio.getPublicationDate() != null) {
                    tei.append(TextUtilities.HTMLEncode(biblio.getPublicationDate()));
                } else {
                    tei.append(when);
                }
                tei.append("</date>\n");
            } else if (biblio.getPublicationDate() != null) {
                tei.append("\t\t\t\t<date type=\"published\">");
                tei.append(TextUtilities.HTMLEncode(biblio.getPublicationDate())
                        + "</date>");
            }
            tei.append("\t\t\t</publicationStmt>\n");
        } else {
            tei.append("\t\t\t<publicationStmt>\n");
            tei.append("\t\t\t\t<publisher/>\n");
            tei.append("\t\t\t\t<availability status=\"unknown\"><licence/></availability>\n");
            tei.append("\t\t\t</publicationStmt>\n");
        }
        tei.append("\t\t\t<sourceDesc>\n\t\t\t\t<biblStruct>\n\t\t\t\t\t<analytic>\n");

        // authors + affiliation
        //biblio.createAuthorSet();
        //biblio.attachEmails();
        //biblio.attachAffiliations();

        tei.append(biblio.toTEIAuthorBlock(6, config));

        // title
        String title = biblio.getTitle();
        String language = biblio.getLanguage();
        String english_title = biblio.getEnglishTitle();
        if (title != null) {
            tei.append("\t\t\t\t\t\t<title");
            /*if ( (bookTitle == null) & (journal == null) )
                    tei.append(" level=\"m\"");
		    	else */
            tei.append(" level=\"a\" type=\"main\"");

            if (config.isGenerateTeiIds()) {
                String divID = KeyGen.getKey().substring(0, 7);
                tei.append(" xml:id=\"_" + divID + "\"");
            }

            if (config.isGenerateTeiCoordinates("title")) {
                List<LayoutToken> titleTokens = biblio.getLayoutTokens(TaggingLabels.HEADER_TITLE);
                if (CollectionUtils.isNotEmpty(titleTokens)) {
                    String coords = LayoutTokensUtil.getCoordsString(titleTokens);
                    tei.append(" coords=\"" + coords + "\"");
                }
            }

            // here check the language ?
            if (english_title == null)
                tei.append(">" + TextUtilities.HTMLEncode(title) + "</title>\n");
            else
                tei.append(" xml:lang=\"" + language + "\">" + TextUtilities.HTMLEncode(title) + "</title>\n");
        }

        boolean hasEnglishTitle = false;
        boolean generateIDs = config.isGenerateTeiIds();
        if (english_title != null) {
            // here do check the language!
            LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
            Language resLang = languageUtilities.runLanguageId(english_title);

            if (resLang != null) {
                String resL = resLang.getLang();
                if (resL.equals(Language.EN)) {
                    hasEnglishTitle = true;
                    tei.append("\t\t\t\t\t\t<title");
                    //if ( (bookTitle == null) & (journal == null) )
                    //	tei.append(" level=\"m\"");
                    //else 
                    tei.append(" level=\"a\"");
                    if (generateIDs) {
                        String divID = KeyGen.getKey().substring(0, 7);
                        tei.append(" xml:id=\"_" + divID + "\"");
                    }
                    tei.append(" xml:lang=\"en\">")
                            .append(TextUtilities.HTMLEncode(english_title)).append("</title>\n");
                }
            }
            // if it's not something in English, we will write it anyway as note without type at the end
        }

        tei.append("\t\t\t\t\t</analytic>\n");

        if ((biblio.getJournal() != null) ||
                (biblio.getJournalAbbrev() != null) ||
                (biblio.getISSN() != null) ||
                (biblio.getISSNe() != null) ||
                (biblio.getPublisher() != null) ||
                (biblio.getPublicationDate() != null) ||
                (biblio.getVolumeBlock() != null) ||
                (biblio.getItem() == BiblioItem.Periodical) ||
                (biblio.getItem() == BiblioItem.InProceedings) ||
                (biblio.getItem() == BiblioItem.Proceedings) ||
                (biblio.getItem() == BiblioItem.InBook) ||
                (biblio.getItem() == BiblioItem.Book) ||
                (biblio.getItem() == BiblioItem.Serie) ||
                (biblio.getItem() == BiblioItem.InCollection)) {
            tei.append("\t\t\t\t\t<monogr");
            tei.append(">\n");

            if (biblio.getJournal() != null) {
                tei.append("\t\t\t\t\t\t<title level=\"j\" type=\"main\"");
                if (generateIDs) {
                    String divID = KeyGen.getKey().substring(0, 7);
                    tei.append(" xml:id=\"_" + divID + "\"");
                }
                tei.append(">" + TextUtilities.HTMLEncode(biblio.getJournal()) + "</title>\n");
            } else if (biblio.getBookTitle() != null) {
                tei.append("\t\t\t\t\t\t<title level=\"m\"");
                if (generateIDs) {
                    String divID = KeyGen.getKey().substring(0, 7);
                    tei.append(" xml:id=\"_" + divID + "\"");
                }
                tei.append(">" + TextUtilities.HTMLEncode(biblio.getBookTitle()) + "</title>\n");
            }

            if (biblio.getJournalAbbrev() != null) {
                tei.append("\t\t\t\t\t\t<title level=\"j\" type=\"abbrev\">" +
                        TextUtilities.HTMLEncode(biblio.getJournalAbbrev()) + "</title>\n");
            }

            if (biblio.getISSN() != null) {
                tei.append("\t\t\t\t\t\t<idno type=\"ISSN\">" +
                        TextUtilities.HTMLEncode(biblio.getISSN()) + "</idno>\n");
            }

            if (biblio.getISSNe() != null) {
                if (!biblio.getISSNe().equals(biblio.getISSN()))
                    tei.append("\t\t\t\t\t\t<idno type=\"eISSN\">" +
                            TextUtilities.HTMLEncode(biblio.getISSNe()) + "</idno>\n");
            }

//            if (biblio.getEvent() != null) {
//                // TODO:
//            }

            // in case the book title corresponds to a proceedings, we can try to indicate the meeting title
            String meeting = biblio.getBookTitle();
            boolean meetLoc = false;
            if (biblio.getEvent() != null)
                meeting = biblio.getEvent();
            else if (meeting != null) {
                meeting = meeting.trim();
                for (String prefix : BiblioItem.confPrefixes) {
                    if (meeting.startsWith(prefix)) {
                        meeting = meeting.replace(prefix, "");
                        meeting = meeting.trim();
                        tei.append("\t\t\t\t\t\t<meeting>" + TextUtilities.HTMLEncode(meeting));
                        if ((biblio.getLocation() != null) || (biblio.getTown() != null) ||
                                (biblio.getCountry() != null)) {
                            tei.append(" <address>");
                            if (biblio.getTown() != null) {
                                tei.append("<settlement>" + TextUtilities.HTMLEncode(biblio.getTown()) + "</settlement>");
                            }
                            if (biblio.getCountry() != null) {
                                tei.append("<country>" + TextUtilities.HTMLEncode(biblio.getCountry()) + "</country>");
                            }
                            if ((biblio.getLocation() != null) && (biblio.getTown() == null) &&
                                    (biblio.getCountry() == null)) {
                                tei.append("<addrLine>" + TextUtilities.HTMLEncode(biblio.getLocation()) + "</addrLine>");
                            }
                            tei.append("</address>\n");
                            meetLoc = true;
                        }
                        tei.append("\t\t\t\t\t\t</meeting>\n");
                        break;
                    }
                }
            }

            if (((biblio.getLocation() != null) || (biblio.getTown() != null) ||
                    (biblio.getCountry() != null))
                    && (!meetLoc)) {
                tei.append("\t\t\t\t\t\t<meeting>");
                tei.append(" <address>");
                if (biblio.getTown() != null) {
                    tei.append(" <settlement>" + TextUtilities.HTMLEncode(biblio.getTown()) + "</settlement>");
                }
                if (biblio.getCountry() != null) {
                    tei.append(" <country>" + TextUtilities.HTMLEncode(biblio.getCountry()) + "</country>");
                }
                if ((biblio.getLocation() != null) && (biblio.getTown() == null)
                        && (biblio.getCountry() == null)) {
                    tei.append("<addrLine>" + TextUtilities.HTMLEncode(biblio.getLocation()) + "</addrLine>");
                }
                tei.append("</address>\n");
                tei.append("\t\t\t\t\t\t</meeting>\n");
            }

            String pageRange = biblio.getPageRange();

            if (biblio.getVolumeBlock() != null
                || biblio.getPublicationDate() != null
                || biblio.getNormalizedPublicationDate() != null
                || pageRange != null
                || biblio.getIssue() != null
                || biblio.getBeginPage() != -1
                || biblio.getPublisher() != null) {

                tei.append("\t\t\t\t\t\t<imprint>\n");

                if (biblio.getPublisher() != null) {
                    tei.append("\t\t\t\t\t\t\t<publisher>" + TextUtilities.HTMLEncode(biblio.getPublisher())
                            + "</publisher>\n");
                }

                if (biblio.getVolumeBlock() != null) {
                    String vol = biblio.getVolumeBlock();
                    vol = vol.replace(" ", "").trim();
                    tei.append("\t\t\t\t\t\t\t<biblScope unit=\"volume\">" +
                            TextUtilities.HTMLEncode(vol) + "</biblScope>\n");
                }

                if (biblio.getIssue() != null) {
                    tei.append("\t\t\t\t\t\t\t<biblScope unit=\"issue\">"
                            + TextUtilities.HTMLEncode(biblio.getIssue()) + "</biblScope>\n");
                }

                if (pageRange != null) {
                    StringTokenizer st = new StringTokenizer(pageRange, "--");
                    if (st.countTokens() == 2) {
                        tei.append("\t\t\t\t\t\t\t<biblScope unit=\"page\"");
                        tei.append(" from=\"" + TextUtilities.HTMLEncode(st.nextToken()) + "\"");
                        tei.append(" to=\"" + TextUtilities.HTMLEncode(st.nextToken()) + "\"/>\n");
                        //tei.append(">" + TextUtilities.HTMLEncode(pageRange) + "</biblScope>\n");
                    } else {
                        tei.append("\t\t\t\t\t\t\t<biblScope unit=\"page\">" + TextUtilities.HTMLEncode(pageRange)
                                + "</biblScope>\n");
                    }
                } else if (biblio.getBeginPage() != -1) {
                    if (biblio.getEndPage() != -1) {
                        tei.append("\t\t\t\t\t\t\t<biblScope unit=\"page\"");
                        tei.append(" from=\"" + biblio.getBeginPage() + "\"");
                        tei.append(" to=\"" + biblio.getEndPage() + "\"/>\n");
                    } else {
                        tei.append("\t\t\t\t\t\t\t<biblScope unit=\"page\"");
                        tei.append(" from=\"" + biblio.getBeginPage() + "\"/>\n");
                    }
                }

                if (biblio.getNormalizedPublicationDate() != null) {
                    Date date = biblio.getNormalizedPublicationDate();

                    String when = Date.toISOString(date);
                    if (StringUtils.isNotBlank(when)) {
                        if (biblio.getPublicationDate() != null) {
                            tei.append("\t\t\t\t\t\t\t<date type=\"published\" when=\"");
                            tei.append(when + "\">");
                            tei.append(TextUtilities.HTMLEncode(biblio.getPublicationDate())
                                    + "</date>\n");
                        } else {
                            tei.append("\t\t\t\t\t\t\t<date type=\"published\" when=\"");
                            tei.append(when + "\" />\n");
                        }
                    } else {
                        if (biblio.getPublicationDate() != null) {
                            tei.append("\t\t\t\t\t\t\t<date type=\"published\">");
                            tei.append(TextUtilities.HTMLEncode(biblio.getPublicationDate())
                                    + "</date>\n");
                        }
                    }
                } else if (biblio.getYear() != null) {
                    String when = "";
                    if (biblio.getYear().length() == 1)
                        when += "000" + biblio.getYear();
                    else if (biblio.getYear().length() == 2)
                        when += "00" + biblio.getYear();
                    else if (biblio.getYear().length() == 3)
                        when += "0" + biblio.getYear();
                    else if (biblio.getYear().length() == 4)
                        when += biblio.getYear();

                    if (biblio.getMonth() != null) {
                        if (biblio.getMonth().length() == 1)
                            when += "-0" + biblio.getMonth();
                        else
                            when += "-" + biblio.getMonth();
                        if (biblio.getDay() != null) {
                            if (biblio.getDay().length() == 1)
                                when += "-0" + biblio.getDay();
                            else
                                when += "-" + biblio.getDay();
                        }
                    }
                    if (biblio.getPublicationDate() != null) {
                        tei.append("\t\t\t\t\t\t\t<date type=\"published\" when=\"");
                        tei.append(when + "\">");
                        tei.append(TextUtilities.HTMLEncode(biblio.getPublicationDate())
                                + "</date>\n");
                    } else {
                        tei.append("\t\t\t\t\t\t\t<date type=\"published\" when=\"");
                        tei.append(when + "\" />\n");
                    }
                } else if (biblio.getE_Year() != null) {
                    String when = "";
                    if (biblio.getE_Year().length() == 1)
                        when += "000" + biblio.getE_Year();
                    else if (biblio.getE_Year().length() == 2)
                        when += "00" + biblio.getE_Year();
                    else if (biblio.getE_Year().length() == 3)
                        when += "0" + biblio.getE_Year();
                    else if (biblio.getE_Year().length() == 4)
                        when += biblio.getE_Year();

                    if (biblio.getE_Month() != null) {
                        if (biblio.getE_Month().length() == 1)
                            when += "-0" + biblio.getE_Month();
                        else
                            when += "-" + biblio.getE_Month();

                        if (biblio.getE_Day() != null) {
                            if (biblio.getE_Day().length() == 1)
                                when += "-0" + biblio.getE_Day();
                            else
                                when += "-" + biblio.getE_Day();
                        }
                    }
                    tei.append("\t\t\t\t\t\t\t<date type=\"ePublished\" when=\"");
                    tei.append(when + "\" />\n");
                } else if (biblio.getPublicationDate() != null) {
                    tei.append("\t\t\t\t\t\t\t<date type=\"published\">");
                    tei.append(TextUtilities.HTMLEncode(biblio.getPublicationDate())
                            + "</date>\n");
                }

                // Fix for issue #31
                tei.append("\t\t\t\t\t\t</imprint>\n");
            }
            tei.append("\t\t\t\t\t</monogr>\n");
        } else {
            tei.append("\t\t\t\t\t<monogr>\n");
            tei.append("\t\t\t\t\t\t<imprint>\n");
            tei.append("\t\t\t\t\t\t\t<date/>\n");
            tei.append("\t\t\t\t\t\t</imprint>\n");
            tei.append("\t\t\t\t\t</monogr>\n");
        }

        if (!StringUtils.isEmpty(doc.getMD5())) {
            tei.append("\t\t\t\t\t<idno type=\"MD5\">" + doc.getMD5() + "</idno>\n");
        }

        if (!StringUtils.isEmpty(biblio.getDOI())) {
            String theDOI = TextUtilities.HTMLEncode(biblio.getDOI());
            if (theDOI.endsWith(".xml")) {
                theDOI = theDOI.replace(".xml", "");
            }
            tei.append("\t\t\t\t\t<idno type=\"DOI\">" + TextUtilities.HTMLEncode(theDOI) + "</idno>\n");
        }

        if (!StringUtils.isEmpty(biblio.getHalId())) {
            tei.append("\t\t\t\t\t<idno type=\"halId\">" + TextUtilities.HTMLEncode(biblio.getHalId()) + "</idno>\n");
        }

        if (!StringUtils.isEmpty(biblio.getArXivId())) {
            tei.append("\t\t\t\t\t<idno type=\"arXiv\">" + TextUtilities.HTMLEncode(biblio.getArXivId()) + "</idno>\n");
        }

        if (!StringUtils.isEmpty(biblio.getPMID())) {
            tei.append("\t\t\t\t\t<idno type=\"PMID\">" + TextUtilities.HTMLEncode(biblio.getPMID()) + "</idno>\n");
        }

        if (!StringUtils.isEmpty(biblio.getPMCID())) {
            tei.append("\t\t\t\t\t<idno type=\"PMCID\">" + TextUtilities.HTMLEncode(biblio.getPMCID()) + "</idno>\n");
        }

        if (!StringUtils.isEmpty(biblio.getPII())) {
            tei.append("\t\t\t\t\t<idno type=\"PII\">" + TextUtilities.HTMLEncode(biblio.getPII()) + "</idno>\n");
        }

        if (!StringUtils.isEmpty(biblio.getArk())) {
            tei.append("\t\t\t\t\t<idno type=\"ark\">" + TextUtilities.HTMLEncode(biblio.getArk()) + "</idno>\n");
        }

        if (!StringUtils.isEmpty(biblio.getIstexId())) {
            tei.append("\t\t\t\t\t<idno type=\"istexId\">" + TextUtilities.HTMLEncode(biblio.getIstexId()) + "</idno>\n");
        }

        if (!StringUtils.isEmpty(biblio.getOAURL())) {
            tei.append("\t\t\t\t\t<ptr type=\"open-access\" target=\"").append(TextUtilities.HTMLEncode(biblio.getOAURL())).append("\" />\n");
        }

        if (biblio.getSubmission() != null) {
            tei.append("\t\t\t\t\t<note type=\"submission\">" +
                    TextUtilities.HTMLEncode(biblio.getSubmission()) + "</note>\n");
        }

        if (biblio.getDedication() != null) {
            tei.append("\t\t\t\t\t<note type=\"dedication\">" + TextUtilities.HTMLEncode(biblio.getDedication())
                    + "</note>\n");
        }

        if ((english_title != null) & (!hasEnglishTitle)) {
            tei.append("\t\t\t\t\t<note type=\"title\"");
            if (generateIDs) {
                String divID = KeyGen.getKey().substring(0, 7);
                tei.append(" xml:id=\"_" + divID + "\"");
            }
            tei.append(">" + TextUtilities.HTMLEncode(english_title) + "</note>\n");
        }

        if (biblio.getNote() != null) {
            tei.append("\t\t\t\t\t<note");
            if (generateIDs) {
                String divID = KeyGen.getKey().substring(0, 7);
                tei.append(" xml:id=\"_" + divID + "\"");
            }
            tei.append(">" + TextUtilities.HTMLEncode(biblio.getNote()) + "</note>\n");
        }

        tei.append("\t\t\t\t</biblStruct>\n");

        if (biblio.getURL() != null) {
            tei.append("\t\t\t\t<ref target=\"" + biblio.getURL() + "\" />\n");
        }

        tei.append("\t\t\t</sourceDesc>\n");

        // We collect the discarded text from the header and add it as a <noteStmt>
        if (config.isIncludeDiscardedText()) {
            List<TaggingLabel> locationTextNotProcessed = Arrays.asList(
                SegmentationLabels.OTHER
//                SegmentationLabels.PAGE_NUMBER,
//                SegmentationLabels.HEADNOTE,
//                SegmentationLabels.COVER
            );

            List<List<LayoutToken>> discardedTextElsewhere = new ArrayList<>();
            for (TaggingLabel label : locationTextNotProcessed) {
                SortedSet<DocumentPiece> docPieces = doc.getDocumentPart(label);
                for (DocumentPiece docPiece: docPieces) {
                    List<LayoutToken> tokens = doc.getDocumentPieceTokenization(docPiece);
                    if (CollectionUtils.isEmpty(tokens)) {
                        continue;
                    }
                    discardedTextElsewhere.add(tokens);
                }
            }


            if (CollectionUtils.isNotEmpty(biblio.getDiscardedPiecesTokens()) || CollectionUtils.isNotEmpty(discardedTextElsewhere)) {
                tei.append("\t\t\t<notesStmt>\n");
                for (List<LayoutToken> discardedPieceTokens : biblio.getDiscardedPiecesTokens()) {
                    tei
                        .append("\t\t\t\t")
                        .append(generateDiscardedTextNote(discardedPieceTokens, doc, this, config).toXML())
                        .append("\n");
                }

                for (List<LayoutToken> discardedPieceTokens : discardedTextElsewhere) {
                    tei
                        .append("\t\t\t\t")
                        .append(generateDiscardedTextNote(discardedPieceTokens, doc, this, config).toXML())
                        .append("\n");
                }

                tei.append("\t\t\t</notesStmt>\n");
            }
        }

        tei.append("\t\t</fileDesc>\n");

        // encodingDesc gives info about the producer of the file
        tei.append("\t\t<encodingDesc>\n");
        tei.append("\t\t\t<appInfo>\n");

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        df.setTimeZone(tz);
        String dateISOString = df.format(new java.util.Date());

        tei.append("\t\t\t\t<application version=\"").append(GrobidProperties.getVersion())
            .append("\" ident=\"GROBID\" when=\"")
            .append(dateISOString)
            .append("\">\n");
        tei.append("\t\t\t\t\t<desc>GROBID - A machine learning software for extracting information from scholarly documents</desc>\n");
        tei.append("\t\t\t\t\t<label type=\"revision\">")
            .append(GrobidProperties.getRevision())
            .append("</label>\n");
        tei.append("\t\t\t\t\t<label type=\"parameters\">")
            .append(config.toStringTEI())
            .append("</label>\n");
        tei.append("\t\t\t\t\t<ref target=\"https://github.com/kermitt2/grobid\"/>\n");
        tei.append("\t\t\t\t</application>\n");
        tei.append("\t\t\t</appInfo>\n");
        tei.append("\t\t</encodingDesc>\n");

        boolean textClassWritten = false;

        tei.append("\t\t<profileDesc>\n");

        // keywords here !! Normally the keyword field has been preprocessed
        // if the segmentation into individual keywords worked, the first conditional
        // statement will be used - otherwise the whole keyword field is outputed
        if (CollectionUtils.isNotEmpty(biblio.getKeywords())) {
            textClassWritten = true;
            tei.append("\t\t\t<textClass>\n");
            tei.append("\t\t\t\t<keywords>\n");

            List<Keyword> keywords = biblio.getKeywords();
            int pos = 0;
            for (Keyword keyw : keywords) {
                if (StringUtils.isBlank(keyw.getKeyword()))
                    continue;
                String res = keyw.getKeyword().trim();
                if (res.startsWith(":")) {
                    res = res.substring(1);
                }
                if (pos == (keywords.size() - 1)) {
                    if (res.endsWith(".")) {
                        res = res.substring(0, res.length() - 1);
                    }
                }
                tei.append("\t\t\t\t\t<term");
                if (generateIDs) {
                    String divID = KeyGen.getKey().substring(0, 7);
                    tei.append(" xml:id=\"_" + divID + "\"");
                }
                tei.append(">" + TextUtilities.HTMLEncode(res) + "</term>\n");
                pos++;
            }
            tei.append("\t\t\t\t</keywords>\n");
        } else if (biblio.getKeyword() != null) {
            String keywords = biblio.getKeyword();
            textClassWritten = true;
            tei.append("\t\t\t<textClass>\n");
            tei.append("\t\t\t\t<keywords");

            if (generateIDs) {
                String divID = KeyGen.getKey().substring(0, 7);
                tei.append(" xml:id=\"_" + divID + "\"");
            }
            tei.append(">");
            tei.append(TextUtilities.HTMLEncode(biblio.getKeyword())).append("</keywords>\n");
        }

        if (biblio.getCategories() != null) {
            if (!textClassWritten) {
                textClassWritten = true;
                tei.append("\t\t\t<textClass>\n");
            }
            List<String> categories = biblio.getCategories();
            tei.append("\t\t\t\t<keywords>");
            for (String category : categories) {
                tei.append("\t\t\t\t\t<term");
                if (generateIDs) {
                    String divID = KeyGen.getKey().substring(0, 7);
                    tei.append(" xml:id=\"_" + divID + "\"");
                }
                tei.append(">" + TextUtilities.HTMLEncode(category.trim()) + "</term>\n");
            }
            tei.append("\t\t\t\t</keywords>\n");
        }

        if (textClassWritten)
            tei.append("\t\t\t</textClass>\n");

        String abstractText = biblio.getAbstract();

        Language resLang = null;
        if (abstractText != null) {
            LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
            resLang = languageUtilities.runLanguageId(abstractText);
        }
        if (resLang != null) {
            String resL = resLang.getLang();
            if (!resL.equals(doc.getLanguage())) {
                tei.append("\t\t\t<abstract xml:lang=\"").append(resL).append("\">\n");
            } else {
                tei.append("\t\t\t<abstract>\n");
            }
        } else if ((abstractText == null) || (abstractText.length() == 0)) {
            tei.append("\t\t\t<abstract/>\n");
        } else {
            tei.append("\t\t\t<abstract>\n");
        }

        if (StringUtils.isNotBlank(abstractText)) {
            if (StringUtils.isNotBlank (biblio.getLabeledAbstract()) ) {
                // we have available structured abstract, which can be serialized as a full text "piece"
                StringBuilder buffer = new StringBuilder();
                try {
                    buffer = toTEITextPiece(
                        buffer,
                        biblio.getLabeledAbstract(),
                        biblio,
                        bds,
                        false,
                        new LayoutTokenization(biblio.getLayoutTokens(TaggingLabels.HEADER_ABSTRACT)),
                        null,
                        null,
                        null,
                        null,
                        markerTypes,
                        doc,
                        config); // no figure, no table, no equation
                } catch(Exception e) {
                    throw new GrobidException("An exception occurred while serializing TEI.", e);
                }
                tei.append(buffer.toString());
            } else {
                tei.append("\t\t\t\t<p");
                if (generateIDs) {
                    String divID = KeyGen.getKey().substring(0, 7);
                    tei.append(" xml:id=\"_" + divID + "\"");
                }
                tei.append(">").append(TextUtilities.HTMLEncode(abstractText)).append("</p>");
            }

            tei.append("\n\t\t\t</abstract>\n");
        }

        tei.append("\t\t</profileDesc>\n");

        if ((biblio.getA_Year() != null) |
                (biblio.getS_Year() != null) |
                (biblio.getSubmissionDate() != null) |
                (biblio.getNormalizedSubmissionDate() != null)
                ) {
            tei.append("\t\t<revisionDesc>\n");
        }

        // submission and other review dates here !
        if (biblio.getA_Year() != null) {
            String when = biblio.getA_Year();
            if (biblio.getA_Month() != null) {
                when += "-" + biblio.getA_Month();
                if (biblio.getA_Day() != null) {
                    when += "-" + biblio.getA_Day();
                }
            }
            tei.append("\t\t\t\t<date type=\"accepted\" when=\"");
            tei.append(when).append("\" />\n");
        }
        if (biblio.getNormalizedSubmissionDate() != null) {
            Date date = biblio.getNormalizedSubmissionDate();
            int year = date.getYear();
            int month = date.getMonth();
            int day = date.getDay();

            String when = "" + year;
            if (month != -1) {
                when += "-" + month;
                if (day != -1) {
                    when += "-" + day;
                }
            }
            tei.append("\t\t\t\t<date type=\"submission\" when=\"");
            tei.append(when).append("\" />\n");
        } else if (biblio.getS_Year() != null) {
            String when = biblio.getS_Year();
            if (biblio.getS_Month() != null) {
                when += "-" + biblio.getS_Month();
                if (biblio.getS_Day() != null) {
                    when += "-" + biblio.getS_Day();
                }
            }
            tei.append("\t\t\t\t<date type=\"submission\" when=\"");
            tei.append(when).append("\" />\n");
        } else if (biblio.getSubmissionDate() != null) {
            tei.append("\t\t\t<date type=\"submission\">")
                    .append(TextUtilities.HTMLEncode(biblio.getSubmissionDate())).append("</date>\n");

            /*tei.append("\t\t\t<change when=\"");
            tei.append(TextUtilities.HTMLEncode(biblio.getSubmissionDate()));
			tei.append("\">Submitted</change>\n");
			*/
        }
        if ((biblio.getA_Year() != null) |
                (biblio.getS_Year() != null) |
                (biblio.getSubmissionDate() != null)
                ) {
            tei.append("\t\t</revisionDesc>\n");
        }

        tei.append("\t</teiHeader>\n");

        // output pages dimensions in the case coordinates will also be provided for some structures
        try {
            tei = toTEIPages(tei, doc, config);
        } catch(Exception e) {
            LOGGER.warn("Problem when serializing page size", e);
        }

        if (doc.getLanguage() != null) {
            tei.append("\t<text xml:lang=\"").append(doc.getLanguage()).append("\">\n");
        } else {
            tei.append("\t<text>\n");
        }

        return tei;
    }

    public static Element generateDiscardedTextNote(List<LayoutToken> discardedPieceTokens, Document doc, TEIFormatter formatter, GrobidAnalysisConfig config) {
        LayoutToken first = Iterables.getFirst(discardedPieceTokens, null);
        String place = first == null || CollectionUtils.isEmpty(first.getLabels()) ? "unknown" : first.getLabels().get(0).getGrobidModel().getModelName();

        Element note = XmlBuilderUtils.teiElement("note");
        note.addAttribute(new Attribute("type", "other"));
        note.addAttribute(new Attribute("place", place));
        Element p = teiElement("p");
        note.appendChild(p);

        if (config.isGenerateTeiIds()) {
            String divID = KeyGen.getKey().substring(0, 7);
            addXmlId(note, "_" + divID);
            divID = KeyGen.getKey().substring(0, 7);
            addXmlId(p, "_" + divID);
        }

        p.appendChild(LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(discardedPieceTokens)).trim());
        if (config.isWithSentenceSegmentation()) {
            // we need a sentence segmentation of the table caption
            formatter.segmentIntoSentences(p, discardedPieceTokens, config, doc.getLanguage(), doc.getPDFAnnotations());
        }

        if (config.isGenerateTeiCoordinates("note")) {
            String coords = LayoutTokensUtil.getCoordsString(discardedPieceTokens);
            note.addAttribute(new Attribute("coords", coords));
        }

        return note;
    }


    /**
     * TEI formatting of the body where only basic logical document structures are present.
     * This TEI format avoids most of the risks of ill-formed TEI due to structure recognition
     * errors and frequent PDF noises.
     * It is adapted to fully automatic process and simple exploitation of the document structures
     * like structured indexing and search.
     */
    public StringBuilder toTEIBody(StringBuilder buffer,
                                   String result,
                                   BiblioItem biblio,
                                   List<BibDataSet> bds,
                                   LayoutTokenization layoutTokenization,
                                   List<Figure> figures,
                                   List<Table> tables,
                                   List<Equation> equations,
                                   List<MarkerType> markerTypes,
                                   Document doc,
                                   GrobidAnalysisConfig config) throws Exception {
        if ((result == null) || (layoutTokenization == null) || (layoutTokenization.getTokenization() == null)) {
            buffer.append("\t\t<body/>\n");
            return buffer;
        }
        buffer.append("\t\t<body>\n");

        List<Note> notes = getTeiNotes(doc);

        buffer = toTEITextPiece(
            buffer,
            result,
            biblio,
            bds,
            true,
            layoutTokenization,
            figures,
            tables,
            equations,
            notes,
            markerTypes,
            doc,
            config
        );

        // notes are still in the body
        buffer = toTEINote(buffer, notes, doc, markerTypes, config);

        buffer.append("\t\t</body>\n");

        return buffer;
    }

    protected List<Note> getTeiNotes(Document doc) {
        // There are two types of structured notes currently supported, foot notes and margin notes.
        // We consider that head notes are always only presentation matter and are never references
        // in a text body.

        SortedSet<DocumentPiece> documentNoteParts = doc.getDocumentPart(SegmentationLabels.FOOTNOTE);
        List<Note> notes = getTeiNotes(doc, documentNoteParts, Note.NoteType.FOOT);

        documentNoteParts = doc.getDocumentPart(SegmentationLabels.MARGINNOTE);
        notes.addAll(getTeiNotes(doc, documentNoteParts, Note.NoteType.MARGIN));

        return notes;
    }

    protected List<Note> getTeiNotes(Document doc, SortedSet<DocumentPiece> documentNoteParts, Note.NoteType noteType) {

        List<Note> notes = new ArrayList<>();
        if (documentNoteParts == null) {
            return notes;
        }

        List<String> allNotes = new ArrayList<>();

        for (DocumentPiece docPiece : documentNoteParts) {
            List<LayoutToken> noteTokens = doc.getDocumentPieceTokenization(docPiece);
            if (CollectionUtils.isEmpty(noteTokens)) {
                continue;
            }

            String footText = doc.getDocumentPieceText(docPiece);
            footText = footText.replace("\n", " ");
            //footText = footText.replace("  ", " ").trim();
            if (footText.length() < 6)
                continue;
            if (allNotes.contains(footText)) {
                // basically we have here the "recurrent" headnote/footnote for each page,
                // no need to add them several times (in the future we could even use them
                // differently combined with the header)
                continue;
            }

            allNotes.add(footText);

            List<Note> localNotes = makeNotes(noteTokens, footText, noteType, notes.size());
            if (localNotes != null)
                notes.addAll(localNotes);
        }

        notes.stream()
            .forEach(n -> n.setText(TextUtilities.dehyphenize(n.getText())));

        return notes;
    }

    protected List<Note> makeNotes(List<LayoutToken> noteTokens, String footText, Note.NoteType noteType, int startIndex) {
        if (footText == null)
            return null;

        List<Note> notes = new ArrayList<>();

        Matcher ma = startNum.matcher(footText);
        int currentNumber = -1;
        // this string represents the possible characters after a note number (usually nothing or a dot)
        String sugarText = null;
        if (ma.find()) {
            String groupStr = ma.group(1);
            footText = ma.group(2);
            try {
                if (groupStr.contains("."))
                    sugarText = ".";
                String groupStrNormalized = groupStr.replace(".", "");
                groupStrNormalized = groupStrNormalized.trim();
                currentNumber = Integer.parseInt(groupStrNormalized);

                // remove this number from the layout tokens of the note
                if (currentNumber != -1) {
                    String toConsume =  groupStr;
                    int start = 0;
                    for(LayoutToken token : noteTokens) {
                        if (StringUtils.isEmpty(token.getText())) {
                            continue;
                        }
                        if (toConsume.startsWith(token.getText())) {
                            start++;
                            toConsume = toConsume.substring(token.getText().length());
                        } else
                            break;

                        if (StringUtils.isEmpty(toConsume))
                            break;
                    }
                    if (start != 0) {
                        noteTokens = noteTokens.subList(start, noteTokens.size());
                    }
                }
            } catch (NumberFormatException e) {
                currentNumber = -1;
            }
        }

        Note localNote = null;
        if (currentNumber == -1)
            localNote = new Note(null, noteTokens, footText, noteType);
        else
            localNote = new Note(""+currentNumber, noteTokens, footText, noteType);

        notes.add(localNote);

        // add possible subsequent notes concatenated in the same note sequence (this is a common error,
        // which is addressed here by heuristics, it may not be necessary in the future with a better
        // segmentation model using more footnotes training data)
        if (currentNumber != -1) {
            String nextLabel = " " + (currentNumber+1);
            // sugar characters after note number must be consistent with the previous ones to avoid false match
            if (sugarText != null)
                nextLabel += sugarText;

            int nextFootnoteLabelIndex = footText.indexOf(nextLabel);
            if (nextFootnoteLabelIndex != -1) {
                // optionally we could restrict here to superscript numbers
                // review local note
                localNote.setText(footText.substring(0, nextFootnoteLabelIndex));
                int pos = 0;
                List<LayoutToken> previousNoteTokens = new ArrayList<>();
                List<LayoutToken> nextNoteTokens = new ArrayList<>();
                for(LayoutToken localToken : noteTokens) {
                    if (StringUtils.isEmpty(localToken.getText()))
                        continue;
                    pos += localToken.getText().length();
                    if (pos <= nextFootnoteLabelIndex+1) {
                        previousNoteTokens.add(localToken);
                    } else {
                        nextNoteTokens.add(localToken);
                    }
                }
                localNote.setTokens(previousNoteTokens);
                String nextFootText = footText.substring(nextFootnoteLabelIndex+1);

                // process the concatenated note
                if (CollectionUtils.isNotEmpty(nextNoteTokens) && StringUtils.isNotEmpty(nextFootText)) {
                    List<Note> nextNotes = makeNotes(nextNoteTokens, nextFootText, noteType, notes.size());
                    if (CollectionUtils.isNotEmpty(nextNotes))
                        notes.addAll(nextNotes);
                }
            }
        }

        for(int noteIndex=0; noteIndex<notes.size(); noteIndex++) {
            Note oneNote = notes.get(noteIndex);
            oneNote.setIdentifier(oneNote.getNoteTypeName() + "_" + (noteIndex+startIndex));
        }

        return notes;
    }

    private StringBuilder toTEINote(StringBuilder tei,
                                    List<Note> notes,
                                    Document doc,
                                    List<MarkerType> markerTypes,
                                    GrobidAnalysisConfig config) throws Exception {
        // pattern is <note n="1" place="foot" xml:id="foot_1">
        // or
        // pattern is <note n="1" place="margin" xml:id="margin_1">

        // if no note label is found, no @n attribute but we generate a random xml:id (not be used currently)

        for (Note note : notes) {
            Element desc = XmlBuilderUtils.teiElement("note");
            desc.addAttribute(new Attribute("place", note.getNoteTypeName()));
            if (note.getLabel() != null) {
                desc.addAttribute(new Attribute("n", note.getLabel()));
            }

            addXmlId(desc, note.getIdentifier());

            // this is a paragraph element for storing text content of the note, which is
            // better practice than just putting the text under the <note> element
            Element pNote = XmlBuilderUtils.teiElement("p");
            if (config.isGenerateTeiIds()) {
                String pID = KeyGen.getKey().substring(0, 7);
                addXmlId(pNote, "_" + pID);
            }

            if (config.isGenerateTeiCoordinates("p")) {
                String coords = LayoutTokensUtil.getCoordsString(note.getTokens());
                desc.addAttribute(new Attribute("coords", coords));
            }

            // for labelling bibliographical references in notes 
            List<LayoutToken> noteTokens = note.getTokens();

            String coords = null;
            if (config.isGenerateTeiCoordinates("note")) {
                coords = LayoutTokensUtil.getCoordsString(noteTokens);
            }

            if (coords != null) {
                desc.addAttribute(new Attribute("coords", coords));
            }

            org.apache.commons.lang3.tuple.Pair<String, List<LayoutToken>> noteProcess =
                fullTextParser.processShort(noteTokens, doc);

            if (noteProcess == null) {
                continue;
            }

            String labeledNote = noteProcess.getLeft();
            List<LayoutToken> noteLayoutTokens = noteProcess.getRight();

            if ( (labeledNote != null) && (labeledNote.length() > 0) ) {
                TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FULLTEXT, labeledNote, noteLayoutTokens);
                List<TaggingTokenCluster> clusters = clusteror.cluster();

                for (TaggingTokenCluster cluster : clusters) {
                    if (cluster == null) {
                        continue;
                    }

                    TaggingLabel clusterLabel = cluster.getTaggingLabel();
                    String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
                    if (clusterLabel.equals(TaggingLabels.CITATION_MARKER)) {
                        try {
                            List<Node> refNodes = this.markReferencesTEILuceneBased(
                                    cluster.concatTokens(),
                                    doc.getReferenceMarkerMatcher(),
                                    config.isGenerateTeiCoordinates("ref"),
                                    false);
                            if (refNodes != null) {
                                for (Node n : refNodes) {
                                    pNote.appendChild(n);
                                }
                            }
                        } catch(Exception e) {
                            LOGGER.warn("Problem when serializing TEI fragment for figure caption", e);
                        }
                    } else {
                        pNote.appendChild(textNode(clusterContent));
                    }
                }
            } else {
                String noteText = note.getText();
                noteText = noteText.replace("  ", " ").trim();
                if (noteText == null) {
                    noteText = LayoutTokensUtil.toText(note.getTokens());
                } else {
                    noteText = noteText.trim();
                }
                pNote.appendChild(LayoutTokensUtil.normalizeText(noteText));
            }


            if (config.isWithSentenceSegmentation()) {
                segmentIntoSentences(pNote, noteTokens, config, doc.getLanguage(), doc.getPDFAnnotations());
            }

            desc.appendChild(pNote);

            tei.append("\t\t\t");
            tei.append(desc.toXML());
            tei.append("\n");
        }

        return tei;
    }

    public StringBuilder processTEIDivSection(String xmlType,
                                              String indentation,
                                              String text,
                                              List<LayoutToken> tokens,
                                              List<BibDataSet> biblioData,
                                              GrobidAnalysisConfig config) throws Exception {
        StringBuilder outputTei = new StringBuilder();

        if ((StringUtils.isBlank(text)) || (tokens == null)) {
            return outputTei;
        }

        outputTei.append("\n").append(indentation).append("<div type=\"").append(xmlType).append("\">\n");
        StringBuilder contentBuffer = new StringBuilder();

        contentBuffer = toTEITextPiece(contentBuffer, text, null, biblioData, false,
                new LayoutTokenization(tokens), null, null, null,
            null, null, doc, config);
        String result = contentBuffer.toString();
        String[] resultAsArray = result.split("\n");

        if (resultAsArray.length != 0) {
            for (int i = 0; i < resultAsArray.length; i++) {
                if (resultAsArray[i].trim().length() == 0)
                    continue;
                outputTei.append(TextUtilities.dehyphenize(resultAsArray[i])).append("\n");
            }
        }
        outputTei.append(indentation).append("</div>\n\n");

        return outputTei;
    }

    public StringBuilder toTEIAnnex(StringBuilder buffer,
                                    String result,
                                    BiblioItem biblio,
                                    List<BibDataSet> bds,
                                    List<LayoutToken> tokenizations,
                                    List<Figure> figures,
                                    List<Table> tables,
                                    List<Equation> equations,
                                    List<MarkerType> markerTypes,
                                    Document doc,
                                    GrobidAnalysisConfig config) throws Exception {
        if ((result == null) || (tokenizations == null)) {
            return buffer;
        }

        buffer.append("\t\t\t<div type=\"annex\">\n");
        buffer = toTEITextPiece(buffer, result, biblio, bds, true,
                new LayoutTokenization(tokenizations), figures, tables, equations, null,
                markerTypes, doc, config);
        buffer.append("\t\t\t</div>\n");

        return buffer;
    }

    public StringBuilder toTEITextPiece(
        StringBuilder buffer,
        String result,
        BiblioItem biblio,
        List<BibDataSet> bds,
        boolean keepUnsolvedCallout,
        LayoutTokenization layoutTokenization,
        List<Figure> figures,
        List<Table> tables,
        List<Equation> equations,
        List<Note> notes,
        List<MarkerType> markerTypes,
        Document doc,
        GrobidAnalysisConfig config) throws Exception {
        TaggingLabel lastClusterLabel = null;
        int startPosition = buffer.length();

        //boolean figureBlock = false; // indicate that a figure or table sequence was met
        // used for reconnecting a paragraph that was cut by a figure/table

        List<LayoutToken> tokenizations = layoutTokenization.getTokenization();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FULLTEXT, result, tokenizations);

        String tokenLabel = null;
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        List<Element> divResults = new ArrayList<>();

        Element curDiv = teiElement("div");
        if (config.isGenerateTeiIds()) {
            String divID = KeyGen.getKey().substring(0, 7);
            addXmlId(curDiv, "_" + divID);
        }
        divResults.add(curDiv);
        Element curParagraph = null;
        List<LayoutToken> curParagraphTokens = null;
        Element curList = null;
        int equationIndex = 0; // current equation index position 
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);
            if (clusterLabel.equals(TaggingLabels.SECTION)) {
                String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
                curDiv = teiElement("div");
                Element head = teiElement("head");
                // section numbers
                org.grobid.core.utilities.Pair<String, String> numb = getSectionNumber(clusterContent);
                if (numb != null) {
                    head.addAttribute(new Attribute("n", numb.b));
                    head.appendChild(numb.a);
                } else {
                    head.appendChild(clusterContent);
                }

                if (config.isGenerateTeiIds()) {
                    String divID = KeyGen.getKey().substring(0, 7);
                    addXmlId(head, "_" + divID);
                }

                if (config.isGenerateTeiCoordinates("head") ) {
                    String coords = LayoutTokensUtil.getCoordsString(cluster.concatTokens());
                    if (coords != null) {
                        head.addAttribute(new Attribute("coords", coords));
                    }
                }

                curDiv.appendChild(head);
                divResults.add(curDiv);
            } else if (clusterLabel.equals(TaggingLabels.EQUATION) ||
                    clusterLabel.equals(TaggingLabels.EQUATION_LABEL)) {
                // get starting position of the cluster
                int start = -1;
                if (CollectionUtils.isNotEmpty (cluster.concatTokens()) ) {
                    start = cluster.concatTokens().get(0).getOffset();
                }
                // get the corresponding equation
                if (start != -1) {
                    Equation theEquation = null;
                    if (equations != null) {
                        for(int i=0; i<equations.size(); i++) {
                            if (i < equationIndex)
                                continue;
                            Equation equation = equations.get(i);
                            if (equation.getStart() == start) {
                                theEquation = equation;
                                equationIndex = i;
                                break;
                            }
                        }
                        if (theEquation != null) {
                            Element element = theEquation.toTEIElement(config);
                            if (element != null)
                                curDiv.appendChild(element);
                        }
                    }
                }
            } else if (clusterLabel.equals(TaggingLabels.ITEM)) {
                String clusterContent = LayoutTokensUtil.normalizeText(cluster.concatTokens());
                //curDiv.appendChild(teiElement("item", clusterContent));
                Element itemNode = teiElement("item", clusterContent);
                if (!MARKER_LABELS.contains(lastClusterLabel) && (lastClusterLabel != TaggingLabels.ITEM)) {
                    curList = teiElement("list");
                    curDiv.appendChild(curList);
                }
                if (curList != null) {
                    curList.appendChild(itemNode);
                }
            } else if (clusterLabel.equals(TaggingLabels.OTHER)) {
                String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
                Element note = teiElement("note", clusterContent);
                note.addAttribute(new Attribute("type", "other"));
                if (config.isGenerateTeiIds()) {
                    String divID = KeyGen.getKey().substring(0, 7);
                    addXmlId(note, "_" + divID);
                }
                curDiv.appendChild(note);
            } else if (clusterLabel.equals(TaggingLabels.PARAGRAPH)) {
                List<LayoutToken> clusterTokens = cluster.concatTokens();
                int clusterPage = Iterables.getLast(clusterTokens).getPage();

                List<Note> notesSamePage = null;
                List<Triple<String, String, OffsetPosition>> matchedLabelPositions = new ArrayList<>();

                // map the matched note labels to their corresponding note objects
                Map<String, Note> labels2Notes = new TreeMap<>();
                if (CollectionUtils.isNotEmpty(notes)) {
                    notesSamePage = notes.stream()
                                .filter(f -> !f.isIgnored() && f.getPageNumber() == clusterPage)
                                .collect(Collectors.toList());

                    // we need to cover several footnote callouts in the same paragraph segment

                    // we also can't assume notes are sorted and will appear first in the text as the same order
                    // they are defined in the note areas - this might not always be the case in
                    // ill-formed documents

                    // map a note label (string) to a valid matching position in the sequence of Layout Tokens
                    // of the paragraph segment

                    int start = 0;
                    for (Note note : notesSamePage) {
                        List<LayoutToken> clusterReduced = clusterTokens.subList(start, clusterTokens.size());
                        Optional<LayoutToken> matching = clusterReduced
                            .stream()
                            .filter(t -> t.getText().equals(note.getLabel()) && t.isSuperscript())
                            .findFirst();

                        if (matching.isPresent()) {
                            int idx = clusterReduced.indexOf(matching.get()) + start;
                            note.setIgnored(true);
                            OffsetPosition matchingPosition = new OffsetPosition();
                            matchingPosition.start = idx;
                            matchingPosition.end = idx+1; // to be review, might be more than one layout token
                            start = matchingPosition.end;
                            matchedLabelPositions.add(Triple.of(note.getIdentifier(), "note", matchingPosition));
                            labels2Notes.put(note.getIdentifier(), note);
                        }
                    }

                }

                //Identify URLs and attach reference in the text
                List<Pair<OffsetPosition, String>> offsetPositionsAndDestinationUrls = Lexicon.tokenPositionUrlPatternWithPdfAnnotations(clusterTokens, doc.getPDFAnnotations());

                offsetPositionsAndDestinationUrls.stream()
//                    .filter(opu -> opu.getLeft().end - opu.getLeft().start > 0)
                    .forEach(opu -> {
                            // We correct the latest token here, since later we will do a substring in the shared code,
                            // and we cannot add a +1 there.
                        matchedLabelPositions.add(
                            Triple.of(opu.getRight() != null ? opu.getRight() : LayoutTokensUtil.normalizeDehyphenizeText(clusterTokens.subList(opu.getLeft().start, opu.getLeft().end + 1)),
                                "url",
                                new OffsetPosition(opu.getLeft().start, opu.getLeft().end + 1)
                            )
                        );
                    }
                    );

                // We can add more elements to be extracted from the paragraphs, here. Each labelPosition it's a
                // Triple with three main elements: the text of the item, the type, and the offsetPositions.

                if (CollectionUtils.isEmpty(matchedLabelPositions)){
                    String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(clusterTokens);
                    if (isNewParagraph(lastClusterLabel, curParagraph)) {
                        if (curParagraph != null && config.isWithSentenceSegmentation()) {
                            segmentIntoSentences(curParagraph, curParagraphTokens, config, doc.getLanguage());
                        }
                        curParagraph = teiElement("p");
                        if (config.isGenerateTeiIds()) {
                            String divID = KeyGen.getKey().substring(0, 7);
                            addXmlId(curParagraph, "_" + divID);
                        }

                        if (config.isGenerateTeiCoordinates("p")) {
                            String coords = LayoutTokensUtil.getCoordsString(clusterTokens);
                            curParagraph.addAttribute(new Attribute("coords", coords));
                        }

                        curDiv.appendChild(curParagraph);
                        curParagraphTokens = new ArrayList<>();
                    } else {
                        if (config.isGenerateTeiCoordinates("p")) {
                            String coords = LayoutTokensUtil.getCoordsString(clusterTokens);
                            if (curParagraph.getAttribute("coords") != null && !curParagraph.getAttributeValue("coords").contains(coords)) {
                                curParagraph.addAttribute(new Attribute("coords", curParagraph.getAttributeValue("coords") + ";" + coords));
                            }
                        }
                    }
                    curParagraph.appendChild(clusterContent);
                    curParagraphTokens.addAll(clusterTokens);
                } else {
                    if (isNewParagraph(lastClusterLabel, curParagraph)) {
                        if (curParagraph != null && config.isWithSentenceSegmentation()) {
                            segmentIntoSentences(curParagraph, curParagraphTokens, config, doc.getLanguage(), doc.getPDFAnnotations());
                        }
                        curParagraph = teiElement("p");
                        if (config.isGenerateTeiIds()) {
                            String divID = KeyGen.getKey().substring(0, 7);
                            addXmlId(curParagraph, "_" + divID);
                        }

                        if (config.isGenerateTeiCoordinates("p")) {
                            String coords = LayoutTokensUtil.getCoordsString(clusterTokens);
                            curParagraph.addAttribute(new Attribute("coords", coords));
                        }

                        curDiv.appendChild(curParagraph);
                        curParagraphTokens = new ArrayList<>();
                    }

                    // sort the matches by position
                    List<Triple<String, String, OffsetPosition>> sortedFilteredMatchedLabelPositions = matchedLabelPositions.stream()
                        .filter(a -> StringUtils.isNotBlank(a.getLeft()))
                        .sorted(Comparator.comparingInt(m -> m.getRight().start))
                        .collect(Collectors.toList());

                    // position in the layout token index
                    int pos = 0;

                    // build the paragraph segment, match by match
                    for (Triple<String, String, OffsetPosition> referenceInformation : sortedFilteredMatchedLabelPositions) {
                        String type = referenceInformation.getMiddle();
                        OffsetPosition matchingPosition = referenceInformation.getRight();

                        if (pos >  matchingPosition.start)
                            break;

                        List<LayoutToken> before = clusterTokens.subList(pos, matchingPosition.start);
                        String clusterContentBefore = LayoutTokensUtil.normalizeDehyphenizeText(before);

                        if (CollectionUtils.isNotEmpty(before) && before.get(0).getText().equals(" ")) {
                            curParagraph.appendChild(new Text(" "));
                        }

                        curParagraph.appendChild(clusterContentBefore);
                        if (config.isGenerateTeiCoordinates("p")) {
                            String coords = LayoutTokensUtil.getCoordsString(before);
                            if (curParagraph.getAttribute("coords") != null && !curParagraph.getAttributeValue("coords").contains(coords)) {
                                curParagraph.addAttribute(new Attribute("coords", curParagraph.getAttributeValue("coords") + ";" + coords));
                            }
                        }

                        curParagraphTokens.addAll(before);

                        Element ref = null;
                        List<LayoutToken> calloutTokens = clusterTokens.subList(matchingPosition.start, matchingPosition.end);
                        if (type.equals("note")) {
                            Note note = labels2Notes.get(referenceInformation.getLeft());
                            ref = generateNoteRef(calloutTokens, referenceInformation.getLeft(), note, config);
                        } else if (type.equals("url")) {
                            String destinationText = referenceInformation.getLeft();
                            ref = generateURLRef(destinationText, calloutTokens, config.isGenerateTeiCoordinates("ref"));

                            //We might need to add a space if it's in the layout tokens
                            if (CollectionUtils.isNotEmpty(before) && StringUtils.equalsAnyIgnoreCase(Iterables.getLast(before).getText(), " ", "\n")) {
                                curParagraph.appendChild(new Text(" "));
                            }
                        }

                        pos = matchingPosition.end;
                        if (ref != null) {
                            curParagraph.appendChild(ref);
                        } else {
                            LOGGER.warn("Detected empty reference or note after " + clusterContentBefore);
                        }
                    }

                    // add last chunk of paragraph stuff (or whole paragraph if no note callout matching)
                    List<LayoutToken> remaining = clusterTokens.subList(pos, clusterTokens.size());
                    String remainingClusterContent = LayoutTokensUtil.normalizeDehyphenizeText(remaining);

                    if (CollectionUtils.isNotEmpty(remaining) && remaining.get(0).getText().equals(" ")) {
                        curParagraph.appendChild(new Text(" "));
                    }

                    if (config.isGenerateTeiCoordinates("p")) {
                        String coords = LayoutTokensUtil.getCoordsString(remaining);
                        if (curParagraph.getAttribute("coords") != null && !curParagraph.getAttributeValue("coords").contains(coords)) {
                            curParagraph.addAttribute(new Attribute("coords", curParagraph.getAttributeValue("coords") + ";" + coords));
                        }
                    }

                    curParagraph.appendChild(remainingClusterContent);
                    curParagraphTokens.addAll(remaining);
                }
            } else if (MARKER_LABELS.contains(clusterLabel)) {
                List<LayoutToken> refTokens = cluster.concatTokens();
                refTokens = LayoutTokensUtil.dehyphenize(refTokens);
                String chunkRefString = LayoutTokensUtil.toText(refTokens);

                Element parent = curParagraph != null ? curParagraph : curDiv;
                parent.appendChild(new Text(" "));

                List<Node> refNodes;
                MarkerType citationMarkerType = null;
                if (markerTypes != null && markerTypes.size()>0) {
                    citationMarkerType = markerTypes.get(0);
                }
                if (clusterLabel.equals(TaggingLabels.CITATION_MARKER)) {
                    refNodes = markReferencesTEILuceneBased(refTokens,
                            doc.getReferenceMarkerMatcher(),
                            config.isGenerateTeiCoordinates("ref"),
                            keepUnsolvedCallout, citationMarkerType);

                } else if (clusterLabel.equals(TaggingLabels.FIGURE_MARKER)) {
                    refNodes = markReferencesFigureTEI(chunkRefString,
                        refTokens, figures, config.isGenerateTeiCoordinates("ref"));
                } else if (clusterLabel.equals(TaggingLabels.TABLE_MARKER)) {
                    refNodes = markReferencesTableTEI(chunkRefString,
                        refTokens, tables, config.isGenerateTeiCoordinates("ref"));
                } else if (clusterLabel.equals(TaggingLabels.EQUATION_MARKER)) {
                    refNodes = markReferencesEquationTEI(chunkRefString, refTokens, equations,
                            config.isGenerateTeiCoordinates("ref"));
                } else {
                    throw new IllegalStateException("Unsupported marker type: " + clusterLabel);
                }

                if (refNodes != null) {
                    boolean footNoteCallout = false;

                    if (refNodes.size() == 1 && (refNodes.get(0) instanceof Text)) {
                        // filtered out superscript reference marker (based on the defined citationMarkerType) might
                        // be foot note callout - se we need in this particular case to try to match existing notes
                        // similarly as within paragraph
                        if (citationMarkerType == null || citationMarkerType != MarkerType.SUPERSCRIPT_NUMBER) {
                            // is refTokens superscript?
                            if (refTokens.size()>0 && refTokens.get(0).isSuperscript()) {
                                // check note callout matching
                                int clusterPage = Iterables.getLast(refTokens).getPage();
                                List<Note> notesSamePage = null;
                                if (notes != null && notes.size() > 0) {
                                    notesSamePage = notes.stream()
                                                .filter(f -> !f.isIgnored() && f.getPageNumber() == clusterPage)
                                                .collect(Collectors.toList());
                                }

                                if (notesSamePage != null) {
                                    for (Note note : notesSamePage) {
                                        if (chunkRefString.trim().equals(note.getLabel())) {
                                            footNoteCallout = true;
                                            note.setIgnored(true);

                                            Element ref = generateNoteRef(refTokens, chunkRefString.trim(), note, config);

                                            parent.appendChild(ref);

                                            if (chunkRefString.endsWith(" ")) {
                                                parent.appendChild(new Text(" "));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!footNoteCallout) {
                        for (Node n : refNodes) {
                            parent.appendChild(n);
                        }
                    }
                }

                if (curParagraph != null)
                    curParagraphTokens.addAll(cluster.concatTokens());
            } else if (clusterLabel.equals(TaggingLabels.FIGURE) || clusterLabel.equals(TaggingLabels.TABLE)) {
                //figureBlock = true;
                if (curParagraph != null)
                    curParagraph.appendChild(new Text(" "));
            }

            lastClusterLabel = cluster.getTaggingLabel();
        }

        // in case we segment paragraph into sentences, we still need to do it for the last paragraph 
        if (curParagraph != null && config.isWithSentenceSegmentation()) {
            segmentIntoSentences(curParagraph, curParagraphTokens, config, doc.getLanguage(), doc.getPDFAnnotations());
        }

        // remove possibly empty div in the div list
        if (divResults.size() != 0) {
            for(int i = divResults.size()-1; i>=0; i--) {
                Element theDiv = divResults.get(i);
                if ( (theDiv.getChildElements() == null) || (theDiv.getChildElements().size() == 0) ) {
                    divResults.remove(i);
                }
            }
        }

        if (divResults.size() != 0)
            buffer.append(XmlBuilderUtils.toXml(divResults));
        else
            buffer.append(XmlBuilderUtils.toXml(curDiv));

        // we apply some overall cleaning and simplification
        buffer = TextUtilities.replaceAll(buffer, "</head><head",
                "</head>\n\t\t\t</div>\n\t\t\t<div>\n\t\t\t\t<head");
        buffer = TextUtilities.replaceAll(buffer, "</p>\t\t\t\t<p>", " ");

        //TODO: work on reconnection
        // we evaluate the need to reconnect paragraphs cut by a figure or a table
        int indP1 = buffer.indexOf("</p0>", startPosition - 1);
        while (indP1 != -1) {
            int indP2 = buffer.indexOf("<p>", indP1 + 1);
            if ((indP2 != 1) && (buffer.length() > indP2 + 5)) {
                if (Character.isUpperCase(buffer.charAt(indP2 + 4)) &&
                        Character.isLowerCase(buffer.charAt(indP2 + 5))) {
                    // a marker for reconnecting the two paragraphs
                    buffer.setCharAt(indP2 + 1, 'q');
                }
            }
            indP1 = buffer.indexOf("</p0>", indP1 + 1);
        }
        buffer = TextUtilities.replaceAll(buffer, "</p0>(\\n\\t)*<q>", " ");
        buffer = TextUtilities.replaceAll(buffer, "</p0>", "</p>");
        buffer = TextUtilities.replaceAll(buffer, "<q>", "<p>");

        if (figures != null) {
            for (Figure figure : figures) {
                String figSeg = figure.toTEI(config, doc, this, markerTypes);
                if (figSeg != null) {
                    buffer.append(figSeg).append("\n");
                }
            }
        }
        if (tables != null) {
            for (Table table : tables) {
                String tabSeg = table.toTEI(config, doc, this, markerTypes);
                if (tabSeg != null) {
                    buffer.append(tabSeg).append("\n");
                }
            }
        }

        return buffer;
    }

    private static Element generateNoteRef(List<LayoutToken> noteTokens, String noteLabel,  Note note, GrobidAnalysisConfig config) {
        Element ref = teiElement("ref");
        //TODO: is this normal that it's hardcoded "foot"?
        ref.addAttribute(new Attribute("type", "foot"));

        if (config.isGenerateTeiCoordinates("ref")) {
            String coords = LayoutTokensUtil.getCoordsString(noteTokens);
            if (coords != null) {
                ref.addAttribute(new Attribute("coords", coords));
            }
        }

        ref.appendChild(noteLabel);
        ref.addAttribute(new Attribute("target", "#" + note.getIdentifier()));
        return ref;
    }

    public static boolean isNewParagraph(TaggingLabel lastClusterLabel, Element curParagraph) {
        return (!MARKER_LABELS.contains(lastClusterLabel) && lastClusterLabel != TaggingLabels.FIGURE
                && lastClusterLabel != TaggingLabels.TABLE) || curParagraph == null;
    }

    public void segmentIntoSentences(Element curParagraph, List<LayoutToken> curParagraphTokens, GrobidAnalysisConfig config, String lang) {
        segmentIntoSentences(curParagraph, curParagraphTokens, config, lang, new ArrayList<>());
    }

    public void segmentIntoSentences(Element curParagraph, List<LayoutToken> curParagraphTokens, GrobidAnalysisConfig config, String lang, List<PDFAnnotation> annotations) {
        // in order to avoid having a sentence boundary in the middle of a ref element
        // (which is frequent given the abbreviation in the reference expression, e.g. Fig.)
        // we only consider for sentence segmentation texts under <p> and skip the text under <ref>.
        if (curParagraph == null)
            return;

        // in xom, the following gives all the text under the element, for the whole subtree
        String text = curParagraph.getValue();
        if (StringUtils.isBlank(text))
            return;

        // identify ref nodes, ref spans and ref positions
        Map<Integer,Node> mapRefNodes = new HashMap<>();
        List<Integer> refPositions = new ArrayList<>();
        List<OffsetPosition> forbiddenPositions = new ArrayList<>();
        int pos = 0;
        for(int i=0; i<curParagraph.getChildCount(); i++) {
            Node theNode = curParagraph.getChild(i);
            if (theNode instanceof Text) {
                String chunk = theNode.getValue();
                pos += chunk.length();
            } else if (theNode instanceof Element) {
                // for readability in another conditional
                if (((Element) theNode).getLocalName().equals("ref")) {
                    // map character offset of the node
                    mapRefNodes.put(pos, theNode);
                    refPositions.add(pos);

                    String chunk = theNode.getValue();
                    forbiddenPositions.add(new OffsetPosition(pos, pos+chunk.length()));
                    pos += chunk.length();
                }
            }
        }

        // We add URL that are identified using the PDF features for annotations, in this way we avoid mangling URLs
        // in different sentences.
        List<OffsetPosition> offsetPositionsUrls = Lexicon.characterPositionsUrlPatternWithPdfAnnotations(curParagraphTokens, annotations, text);
        forbiddenPositions.addAll(offsetPositionsUrls);

        Language language = new Language("en");
        if (lang != null) {
            language = new Language(lang);
        } else {
            LOGGER.warn("There wasn't enough usable text to detect the language. Defaulting to English (en) for applying sentence segmentation. ");
        }

        List<OffsetPosition> theSentences =
            SentenceUtilities.getInstance().runSentenceDetection(text, forbiddenPositions, curParagraphTokens, language);
    
        /*if (theSentences.size() == 0) {
            // this should normally not happen, but it happens (depending on sentence splitter, usually the text 
            // is just a punctuation)
            // in this case we consider the current text as a unique sentence as fall back
            theSentences.add(new OffsetPosition(0, text.length()));
        }*/

        // segment the list of layout tokens according to the sentence segmentation if the coordinates are needed
        List<List<LayoutToken>> segmentedParagraphTokens = new ArrayList<>();
        List<LayoutToken> currentSentenceTokens = new ArrayList<>();
        pos = 0;

        if (config.isGenerateTeiCoordinates("s")) {

            int currentSentenceIndex = 0;
            String sentenceChunk = text.substring(theSentences.get(currentSentenceIndex).start, theSentences.get(currentSentenceIndex).end);

            for(int i=0; i<curParagraphTokens.size(); i++) {
                LayoutToken token = curParagraphTokens.get(i);
                if (StringUtils.isEmpty(token.getText()))
                    continue;
                int newPos = sentenceChunk.indexOf(token.getText(), pos);
                if ((newPos != -1) || SentenceUtilities.toSkipToken(token.getText())) {
                    // just move on
                    currentSentenceTokens.add(token);
                    if (newPos != -1 && !SentenceUtilities.toSkipToken(token.getText()))
                        pos = newPos;
                } else {
                    if (currentSentenceTokens.size() > 0) {
                        segmentedParagraphTokens.add(currentSentenceTokens);
                        currentSentenceIndex++;
                        if (currentSentenceIndex >= theSentences.size()) {
                            currentSentenceTokens = new ArrayList<>();
                            break;
                        }
                        int endPosition = Math.min(theSentences.get(currentSentenceIndex).end, text.length());
                        sentenceChunk = text.substring(theSentences.get(currentSentenceIndex).start, endPosition);
                    }
                    currentSentenceTokens = new ArrayList<>();
                    currentSentenceTokens.add(token);
                    pos = 0;
                }

                if (currentSentenceIndex >= theSentences.size())
                    break;
            }
            // last sentence
            if (currentSentenceTokens.size() > 0) {
                // check sentence index too ?
                segmentedParagraphTokens.add(currentSentenceTokens);
            }

/*if (segmentedParagraphTokens.size() != theSentences.size()) {
System.out.println("ERROR, segmentedParagraphTokens size:" + segmentedParagraphTokens.size() + " vs theSentences size: " + theSentences.size());
System.out.println(text);
System.out.println(theSentences.toString());
int k = 0;
for (List<LayoutToken> segmentedParagraphToken : segmentedParagraphTokens) {
    if (k < theSentences.size())
        System.out.println(k + " sentence segmented text-only: " + text.substring(theSentences.get(k).start, theSentences.get(k).end));
    else
        System.out.println("no text-only sentence at index " + k);
    System.out.print(k + " layout token segmented sentence: ");
    System.out.println(segmentedParagraphToken);
    k++;
}
}*/
        }

        // update the xml paragraph element
        int currenChildIndex = 0;
        pos = 0;
        int posInSentence = 0;
        int refIndex = 0;
        for(int i=0; i<theSentences.size(); i++) {
            pos = theSentences.get(i).start;
            posInSentence = 0;
            Element sentenceElement = teiElement("s");
            if (config.isGenerateTeiIds()) {
                String sID = KeyGen.getKey().substring(0, 7);
                addXmlId(sentenceElement, "_" + sID);
            }
            if (config.isGenerateTeiCoordinates("s")) {
                if (segmentedParagraphTokens.size()>=i+1) {
                    currentSentenceTokens = segmentedParagraphTokens.get(i);
                    String coords = LayoutTokensUtil.getCoordsString(currentSentenceTokens);
                    if (coords != null) {
                        sentenceElement.addAttribute(new Attribute("coords", coords));
                    }
                }
            }

            int sentenceLength = theSentences.get(i).end - pos;
            // check if we have a ref between pos and pos+sentenceLength
            for(int j=refIndex; j<refPositions.size(); j++) {
                int refPos = refPositions.get(j).intValue();
                if (refPos < pos+posInSentence)
                    continue;

                if (refPos >= pos+posInSentence && refPos <= pos+sentenceLength) {
                    Node valueNode = mapRefNodes.get(Integer.valueOf(refPos));
                    if (pos+posInSentence < refPos) {
                        String local_text_chunk = text.substring(pos+posInSentence, refPos);
                        local_text_chunk = XmlBuilderUtils.stripNonValidXMLCharacters(local_text_chunk);
                        sentenceElement.appendChild(local_text_chunk);
                    }
                    valueNode.detach();
                    sentenceElement.appendChild(valueNode);
                    refIndex = j;
                    posInSentence = refPos+valueNode.getValue().length()-pos;
                }
                if (refPos > pos+sentenceLength) {
                    break;
                }
            }

            int endPosition = Math.min(theSentences.get(i).end, text.length());
            if (pos+posInSentence <= endPosition) {
                String local_text_chunk = text.substring(pos+posInSentence, endPosition);
                local_text_chunk = XmlBuilderUtils.stripNonValidXMLCharacters(local_text_chunk);
                sentenceElement.appendChild(local_text_chunk);
            }
            curParagraph.appendChild(sentenceElement);
        }

        for(int i=curParagraph.getChildCount()-1; i>=0; i--) {
            Node theNode = curParagraph.getChild(i);
            if (theNode instanceof Text) {
                curParagraph.removeChild(theNode);
            } else if (theNode instanceof Element) {
                // for readability in another conditional
                if (!((Element) theNode).getLocalName().equals("s")) {
                    curParagraph.removeChild(theNode);
                }
            }
        }

    }

    /**
     * Return the graphic objects in a given interval position in the document.
     */
    private List<GraphicObject> getGraphicObject(List<GraphicObject> graphicObjects, int startPos, int endPos) {
        List<GraphicObject> result = new ArrayList<GraphicObject>();
        for (GraphicObject nto : graphicObjects) {
            if ((nto.getStartPosition() >= startPos) && (nto.getStartPosition() <= endPos)) {
                result.add(nto);
            }
            if (nto.getStartPosition() > endPos) {
                break;
            }
        }
        return result;
    }

    private org.grobid.core.utilities.Pair<String, String> getSectionNumber(String text) {
        Matcher m1 = BasicStructureBuilder.headerNumbering1.matcher(text);
        Matcher m2 = BasicStructureBuilder.headerNumbering2.matcher(text);
        Matcher m3 = BasicStructureBuilder.headerNumbering3.matcher(text);
        Matcher m = null;
        String numb = null;
        if (m1.find()) {
            numb = m1.group(0);
            m = m1;
        } else if (m2.find()) {
            numb = m2.group(0);
            m = m2;
        } else if (m3.find()) {
            numb = m3.group(0);
            m = m3;
        }
        if (numb != null) {
            text = text.replace(numb, "").trim();
            numb = numb.replace(" ", "");
            return new org.grobid.core.utilities.Pair<>(text, numb);
        } else {
            return null;
        }
    }

    public StringBuilder toTEIReferences(StringBuilder tei,
                                         List<BibDataSet> bds,
                                         GrobidAnalysisConfig config) throws Exception {
        tei.append("\t\t\t<div type=\"references\">\n\n");

        if ((bds == null) || (bds.size() == 0))
            tei.append("\t\t\t\t<listBibl/>\n");
        else {
            tei.append("\t\t\t\t<listBibl>\n");

            int p = 0;
            if (bds.size() > 0) {
                for (BibDataSet bib : bds) {
                    BiblioItem bit = bib.getResBib();
                    bit.setReference(bib.getRawBib());
                    if (bit != null) {
                        tei.append("\n" + bit.toTEI(p, 0, config));
                    } else {
                        tei.append("\n");
                    }
                    p++;
                }
            }
            tei.append("\n\t\t\t\t</listBibl>\n");
        }
        tei.append("\t\t\t</div>\n");

        return tei;
    }


    //bounding boxes should have already been calculated when calling this method
    public static String getCoordsAttribute(List<BoundingBox> boundingBoxes, boolean generateCoordinates) {
        if (!generateCoordinates || boundingBoxes == null || boundingBoxes.isEmpty()) {
            return "";
        }
        String coords = Joiner.on(";").join(boundingBoxes);
        return "coords=\"" + coords + "\"";
    }


    /**
     * Mark using TEI annotations the identified references in the text body build with the machine learning model.
     */
    public List<Node> markReferencesTEILuceneBased(List<LayoutToken> refTokens,
                                                   ReferenceMarkerMatcher markerMatcher,
                                                   boolean generateCoordinates,
                                                   boolean keepUnsolvedCallout) throws EntityMatcherException {
        return markReferencesTEILuceneBased(refTokens, markerMatcher, generateCoordinates, keepUnsolvedCallout, null);
    }

    public List<Node> markReferencesTEILuceneBased(List<LayoutToken> refTokens,
                                                   ReferenceMarkerMatcher markerMatcher,
                                                   boolean generateCoordinates,
                                                   boolean keepUnsolvedCallout,
                                                   MarkerType citationMarkerType) throws EntityMatcherException {
        // safety tests
        if ( (refTokens == null) || (refTokens.size() == 0) )
            return null;
        String text = LayoutTokensUtil.toText(refTokens);
        if (text == null || text.trim().length() == 0 || text.endsWith("</ref>") || text.startsWith("<ref") || markerMatcher == null)
            return Collections.<Node>singletonList(new Text(text));

        boolean spaceEnd = false;
        text = text.replace("\n", " ");
        if (text.endsWith(" "))
            spaceEnd = true;

        // check constraints on global marker type, we need to discard reference markers that do not follow the
        // reference marker pattern of the document
        if (citationMarkerType != null) {
            // do we have superscript numbers in the ref tokens?
            boolean hasSuperScriptNumber = false;
            for(LayoutToken refToken : refTokens) {
                if (refToken.isSuperscript()) {
                    hasSuperScriptNumber = true;
                    break;
                }
            }

            if (citationMarkerType == MarkerType.SUPERSCRIPT_NUMBER) {
                // we need to check that the reference tokens have some superscript numbers
                if (!hasSuperScriptNumber) {
                    return Collections.<Node>singletonList(new Text(text));
                }
            } else {
                // if the reference tokens has some superscript numbers, it is a callout for a different type of object
                // (e.g. a foot note)
                if (hasSuperScriptNumber) {
                    return Collections.<Node>singletonList(new Text(text));
                }
            }

            // TBD: check other constraints and consistency issues
        }

        List<Node> nodes = new ArrayList<>();
        List<ReferenceMarkerMatcher.MatchResult> matchResults = markerMatcher.match(refTokens);
        if (matchResults != null) {
            for (ReferenceMarkerMatcher.MatchResult matchResult : matchResults) {
                // no need to HTMLEncode since XOM will take care about the correct escaping
                String markerText = LayoutTokensUtil.normalizeText(matchResult.getText());
                String coords = null;
                if (generateCoordinates && matchResult.getTokens() != null) {
                    coords = LayoutTokensUtil.getCoordsString(matchResult.getTokens());
                }

                Element ref = teiElement("ref");
                ref.addAttribute(new Attribute("type", "bibr"));

                if (coords != null) {
                    ref.addAttribute(new Attribute("coords", coords));
                }
                ref.appendChild(markerText);

                boolean solved = false;
                if (matchResult.getBibDataSet() != null) {
                    ref.addAttribute(new Attribute("target", "#b" + matchResult.getBibDataSet().getResBib().getOrdinal()));
                    solved = true;
                }
                if ( solved || (!solved && keepUnsolvedCallout) )
                    nodes.add(ref);
                else
                    nodes.add(textNode(matchResult.getText()));
            }
        }
        if (spaceEnd)
            nodes.add(new Text(" "));
        return nodes;
    }

    public List<Node> markReferencesFigureTEI(
        String refText,
        List<LayoutToken> allRefTokens,
        List<Figure> figures,
        boolean generateCoordinates
    ) {
        return markReferencesFigureOrTableTEI(refText, allRefTokens, figures, FigureTableType.FIGURE, generateCoordinates);
    }

    public List<Node> markReferencesTableTEI(
        String refText,
        List<LayoutToken> allRefTokens,
        List<Table> tables,
        boolean generateCoordinates
    ) {
        return markReferencesFigureOrTableTEI(refText, allRefTokens, tables, FigureTableType.TABLE, generateCoordinates);
    }

    private List<Node> markReferencesFigureOrTableTEI(
        String refText,
        List<LayoutToken> allRefTokens,
        List<? extends Figure> figuresOrTables,
        FigureTableType type,
        boolean generateCoordinates
    ) {
        if (refText == null ||
            refText.trim().isEmpty()) {
            return null;
        }

        List<Node> nodes = new ArrayList<>();

        if (refText.trim().length() == 1 && TextUtilities.fullPunctuations.contains(refText.trim())) {
            // the reference text marker is punctuation
            nodes.add(new Text(refText));
            return nodes;
        }

        List<org.grobid.core.utilities.Pair<String, List<LayoutToken>>> labels = null;

        List<List<LayoutToken>> allYs = LayoutTokensUtil.split(allRefTokens, ReferenceMarkerMatcher.FIGURE_TABLES_REF_SEPARATORS, true);
        if (allYs.size() > 1) {
            labels = new ArrayList<>();
            for (List<LayoutToken> ys : allYs) {
                labels.add(new org.grobid.core.utilities.Pair<>(LayoutTokensUtil.toText(LayoutTokensUtil.dehyphenize(ys)), ys));
            }
        } else {
            // possibly expand the range of reference numbers (like for numerical bibliographical markers)
            labels = ReferenceMarkerMatcher.getNumberedLabels(allRefTokens, false);
        }

        if (labels == null || labels.size() <= 1) {
            org.grobid.core.utilities.Pair<String, List<LayoutToken>> localLabel =
                new org.grobid.core.utilities.Pair<>(refText, allRefTokens);
            labels = new ArrayList<>();
            labels.add(localLabel);
        }

        for (org.grobid.core.utilities.Pair<String, List<LayoutToken>> theLabel : labels) {
            String text = theLabel.a;
            List<LayoutToken> refTokens = theLabel.b;

            String textLow = text.toLowerCase().trim();
            String bestFigureOrTable = null;

            if (figuresOrTables != null) {
                for (Figure figureOrTable : figuresOrTables) {
                    if (StringUtils.isNotBlank(figureOrTable.getLabel())) {
                        String label = TextUtilities.cleanField(figureOrTable.getLabel(), false);
                        if (StringUtils.isNotBlank(label) && textLow.equals(label.toLowerCase())) {
                            bestFigureOrTable = figureOrTable.getId();
                            break;
                        }
                    }
                }
                if (bestFigureOrTable == null) {
                    // second pass with relaxed figure marker matching
                    for(int i=figuresOrTables.size()-1; i>=0; i--) {
                        Figure figureOrTable = figuresOrTables.get(i);
                        if (StringUtils.isNotBlank(figureOrTable.getLabel())) {
                            String label = TextUtilities.cleanField(figureOrTable.getLabel(), false);
                            if (StringUtils.isNotBlank(label) &&
                                    (textLow.contains(label.toLowerCase()))) {
                                bestFigureOrTable = figureOrTable.getId();
                                break;
                            }
                        }
                    }
                }
            }

            boolean spaceEnd = false;
            boolean spaceStart = false;
            text = text.replace("\n", " ");
            if (text.endsWith(" ")) {
                spaceEnd = true;
            }
            if (!text.equals(" ") & text.startsWith(" ")) {
                spaceStart = true;
            }
            text = text.trim();

            if (StringUtils.isBlank(text)) {
                if (spaceStart) {
                    nodes.add(new Text(" "));
                }
                nodes.add(new Text(text));
                if (spaceEnd) {
                    nodes.add(new Text(" "));
                }
                continue;
            }

            String andWordString = null;
            if (text.endsWith("and") || text.endsWith("&") || text.endsWith(",")) {
                if (text.equals("and") || text.equals("&") || text.equals(",")) {
                    if (spaceStart) {
                        nodes.add(new Text(" "));
                    }
                    nodes.add(new Text(text));
                    if (spaceEnd) {
                        nodes.add(new Text(" "));
                    }
                    continue;
                } else if (text.endsWith("and")) {
                    // the AND_WORD_PATTERN case, we want to exclude the AND word from the tagged chunk
                    text = text.substring(0, text.length()-3);
                    andWordString = "and";
                    refTokens = refTokens.subList(0,refTokens.size()-1);
                } else if (text.endsWith("&")) {
                    text = text.substring(0, text.length()-1);
                    andWordString = "&";
                    refTokens = refTokens.subList(0,refTokens.size()-1);
                } else if (text.endsWith(",")) {
                    text = text.substring(0, text.length()-1);
                    andWordString = ",";
                    refTokens = refTokens.subList(0,refTokens.size()-1);
                }

                if (text.endsWith(" ")) {
                    andWordString = " " + andWordString;
                    refTokens = refTokens.subList(0,refTokens.size()-1);
                }
                text = text.trim();
            }

            String coords = null;
            if (generateCoordinates && refTokens != null) {
                coords = LayoutTokensUtil.getCoordsString(refTokens);
            }

            Element ref = teiElement("ref");

            ref.addAttribute(new Attribute("type", type.getValue()));

            if (coords != null) {
                ref.addAttribute(new Attribute("coords", coords));
            }
            ref.appendChild(text);

            if (bestFigureOrTable != null) {
                if (type == FigureTableType.TABLE) {
                    ref.addAttribute(new Attribute("target", "#tab_" + bestFigureOrTable));
                } else if (type == FigureTableType.FIGURE) {
                    ref.addAttribute(new Attribute("target", "#fig_" + bestFigureOrTable));
                }
            }
            if (spaceStart) {
                nodes.add(new Text(" "));
            }
            nodes.add(ref);

            if (andWordString != null) {
                nodes.add(new Text(andWordString));
            }

            if (spaceEnd) {
                nodes.add(new Text(" "));
            }
        }
        return nodes;
    }

    private static Pattern patternNumber = Pattern.compile("\\d+");

    public List<Node> markReferencesEquationTEI(String text,
                                            List<LayoutToken> refTokens,
                                            List<Equation> equations,
                                            boolean generateCoordinates) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        text = TextUtilities.cleanField(text, false);
        String textNumber = null;
        Matcher m = patternNumber.matcher(text);
        if (m.find()) {
            textNumber = m.group();
        }

        List<Node> nodes = new ArrayList<>();

        String textLow = text.toLowerCase();
        String bestFormula = null;
        if (equations != null) {
            for (Equation equation : equations) {
                if (StringUtils.isNotBlank(equation.getLabel())) {
                    String label = TextUtilities.cleanField(equation.getLabel(), false);
                    Matcher m2 = patternNumber.matcher(label);
                    String labelNumber = null;
                    if (m2.find()) {
                        labelNumber = m2.group();
                    }
                    //if ((label.length() > 0) &&
                    //        (textLow.contains(label.toLowerCase()))) {
                    if ( (labelNumber != null && textNumber != null && labelNumber.length()>0 &&
                        labelNumber.equals(textNumber)) ||
                        ((label.length() > 0) && (textLow.equals(label.toLowerCase()))) ) {
                        bestFormula = equation.getId();
                        break;
                    }
                }
            }
        }

        boolean spaceEnd = false;
        text = text.replace("\n", " ");
        if (text.endsWith(" "))
            spaceEnd = true;
        text = text.trim();

        String coords = null;
        if (generateCoordinates && refTokens != null) {
            coords = LayoutTokensUtil.getCoordsString(refTokens);
        }

        Element ref = teiElement("ref");
        ref.addAttribute(new Attribute("type", "formula"));

        if (coords != null) {
            ref.addAttribute(new Attribute("coords", coords));
        }
        ref.appendChild(text);
        if (bestFormula != null) {
            ref.addAttribute(new Attribute("target", "#formula_" + bestFormula));
        }
        nodes.add(ref);
        if (spaceEnd)
            nodes.add(new Text(" "));
        return nodes;
    }

    public Element generateURLRef(String destination,
                                  List<LayoutToken> refTokens,
                                  boolean generateCoordinates) {
        if (StringUtils.isEmpty(destination)) {
            return null;
        }

        // For URLs, we remove spaces
        String cleanText = StringUtils.trim(LayoutTokensUtil.toText(refTokens).replace("\n", " "));
        String cleanDestination = StringUtils.trim(destination.replace("\n", " ").replace(" ", ""));

        String coords = null;
        if (generateCoordinates && refTokens != null) {
            coords = LayoutTokensUtil.getCoordsString(refTokens);
        }

        Element ref = teiElement("ref");
        ref.addAttribute(new Attribute("type", "url"));

        if (coords != null) {
            ref.addAttribute(new Attribute("coords", coords));
        }
        ref.appendChild(cleanText);
        ref.addAttribute(new Attribute("target", cleanDestination));

        return ref;
    }

    private String normalizeText(String localText) {
        localText = localText.trim();
        localText = TextUtilities.dehyphenize(localText);
        localText = localText.replace("\n", " ");
        localText = localText.replace("  ", " ");

        return localText.trim();
    }

    /**
     * In case, the coordinates of structural elements are provided in the TEI
     * representation, we need the page sizes in order to scale the coordinates
     * appropriately. These size information are provided via the TEI facsimile
     * element, with a surface element for each page carrying the page size info.
     */
    public StringBuilder toTEIPages(StringBuilder buffer,
                                   Document doc,
                                   GrobidAnalysisConfig config) throws Exception {
        if (!config.isGenerateTeiCoordinates()) {
            // no cooredinates, nothing to do
            return buffer;
        }

        // page height and width
        List<Page> pages = doc.getPages();
        int pageNumber = 1;
        buffer.append("\t<facsimile>\n");
        for(Page page : pages) {
            buffer.append("\t\t<surface ");
            buffer.append("n=\"" + pageNumber + "\" ");
            buffer.append("ulx=\"0.0\" uly=\"0.0\" ");
            buffer.append("lrx=\"" + page.getWidth() + "\" lry=\"" + page.getHeight() + "\"");
            buffer.append("/>\n");
            pageNumber++;
        }
        buffer.append("\t</facsimile>\n");

        return buffer;
    }
}