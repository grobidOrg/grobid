package org.grobid.core.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.collections4.CollectionUtils;

import org.grobid.core.data.util.AuthorEmailAssigner;
import org.grobid.core.data.util.ClassicAuthorEmailAssigner;
import org.grobid.core.data.util.EmailSanitizer;
import org.grobid.core.document.*;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.LanguageUtilities;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.KeyGen;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.GrobidModels;
import org.grobid.core.engines.label.TaggingLabels;

import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.grobid.core.utilities.Consolidation.CONSOLIDATION_STATUS_EXTRACTED;

/**
 * Class for representing and exchanging a bibliographical item.
 *
 */
public class BiblioItem {
    protected static final Logger LOGGER = LoggerFactory.getLogger(BiblioItem.class);

    private LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
    private AuthorEmailAssigner authorEmailAssigner = new ClassicAuthorEmailAssigner();
    private EmailSanitizer emailSanitizer = new EmailSanitizer();
    private String teiId;
    //TODO: keep in sync with teiId - now teiId is generated in many different places
    private Integer ordinal;
    private List<BoundingBox> coordinates = null;

    // map of labels (e.g. <title> or <abstract>) to LayoutToken
    private Map<String, List<LayoutToken>> labeledTokens;

    // accumulation of the LayoutTokens for sequences of affiliation/address
    private List<List<LayoutToken>> affiliationAddresslabeledTokens;

    /**
     * The following are internal working structures not meant to be used outside. 
     * For collecting layout tokens of the various bibliographical component, 
     * please refers to @See(getLayoutTokens(TaggingLabels label)
     */
    private List<LayoutToken> authorsTokensWorkingCopy = new ArrayList<>();
    private List<LayoutToken> abstractTokensWorkingCopy = new ArrayList<>();

    @Override
    public String toString() {
        return "BiblioItem{" +
                "submission_date='" + submission_date + '\'' +
                ", download_date='" + download_date + '\'' +
                ", server_date='" + server_date + '\'' +
                ", languageUtilities=" + languageUtilities +
                ", item=" + item +
                ", parentItem=" + parentItem +
                ", ISBN13='" + ISBN13 + '\'' +
                ", ISBN10='" + ISBN10 + '\'' +
                ", title='" + title + '\'' +
                ", publisher='" + publisher + '\'' +
                ", nbPages=" + nbPages +
                ", edition='" + edition + '\'' +
                ", language='" + language + '\'' +
                ", subtitle='" + subtitle + '\'' +
                ", publication_date='" + publication_date + '\'' +
                ", normalized_publication_date=" + normalized_publication_date +
                ", editors='" + editors + '\'' +
                ", publisher_website='" + publisher_website + '\'' +
                ", serie='" + serie + '\'' +
                ", ISSN='" + ISSN + '\'' +
                ", ISSNe='" + ISSNe + '\'' +
                ", volume='" + volume + '\'' +
                ", number='" + number + '\'' +
                ", month='" + month + '\'' +
                ", support_type='" + support_type + '\'' +
                ", version='" + version + '\'' +
                ", smallImageURL='" + smallImageURL + '\'' +
                ", largeImageURL='" + largeImageURL + '\'' +
                ", publisherPlace='" + publisherPlace + '\'' +
                ", review='" + review + '\'' +
                ", keywords=" + keywords +
                ", subjects=" + subjects +
                ", categories='" + categories + '\'' +
                ", type='" + type + '\'' +
                ", typeDescription='" + typeDescription + '\'' +
                ", book_type='" + book_type + '\'' +
                ", DOI='" + doi + '\'' +
                ", arXivId='" + arXivId + '\'' +
                ", PMID='" + PMID + '\'' +
                ", PMCID='" + PMCID + '\'' +
                ", PII='" + PII + '\'' +
                ", HALId='" + halId + '\'' +
                ", ark='" + ark + '\'' +
                ", istexId='" + istexId + '\'' +
                ", inDOI='" + inDOI + '\'' +
                ", abstract_='" + abstract_ + '\'' +
                ", authors='" + authors + '\'' +
                ", firstAuthorSurname='" + firstAuthorSurname + '\'' +
                ", location='" + location + '\'' +
                ", bookTitle='" + bookTitle + '\'' +
                ", serieTitle='" + serieTitle + '\'' +
                ", pageRange='" + pageRange + '\'' +
                ", journal='" + journal + '\'' +
                ", volumeBlock='" + volumeBlock + '\'' +
                ", institution='" + institution + '\'' +
                ", note='" + note + '\'' +
                ", affiliation='" + affiliation + '\'' +
                ", address='" + address + '\'' +
                ", country='" + country + '\'' +
                ", town='" + town + '\'' +
                ", email='" + email + '\'' +
                ", pubnum='" + pubnum + '\'' +
                ", keyword='" + keyword + '\'' +
                ", phone='" + phone + '\'' +
                ", degree='" + degree + '\'' +
                ", web='" + web + '\'' +
                ", issue='" + issue + '\'' +
                ", journal_abbrev='" + journal_abbrev + '\'' +
                ", event='" + event + '\'' +
                ", abstractHeader='" + abstractHeader + '\'' +
                ", day='" + day + '\'' +
                ", locationPublisher='" + locationPublisher + '\'' +
                ", dedication='" + dedication + '\'' +
                ", submission='" + submission + '\'' +
                ", english_title='" + english_title + '\'' +
                ", url='" + url + '\'' +
                ", oaUrl='" + oaUrl + '\'' +
                ", uri='" + uri + '\'' +
                ", confidence='" + confidence + '\'' +
                ", conf=" + conf +
                ", e_year='" + e_year + '\'' +
                ", e_month='" + e_month + '\'' +
                ", e_day='" + e_day + '\'' +
                ", s_year='" + s_year + '\'' +
                ", s_month='" + s_month + '\'' +
                ", s_day='" + s_day + '\'' +
                ", d_year='" + d_year + '\'' +
                ", d_month='" + d_month + '\'' +
                ", d_day='" + d_day + '\'' +
                ", a_year='" + a_year + '\'' +
                ", a_month='" + a_month + '\'' +
                ", a_day='" + a_day + '\'' +
                ", authorList=" + authorList +
                ", editorList=" + editorList +
                ", affiliationList=" + affiliationList +
                ", addressList=" + addressList +
                ", emailList=" + emailList +
                ", webList=" + webList +
                ", phoneList=" + phoneList +
                ", markers=" + markers +
                ", fullAuthors=" + fullAuthors +
                ", fullEditors=" + fullEditors +
                ", fullAffiliations=" + fullAffiliations +
                ", reference='" + reference + '\'' +
                ", copyright='" + copyright + '\'' +
                ", funding='" + funding + '\'' +
                ", affiliationAddressBlock='" + affiliationAddressBlock + '\'' +
                ", articleTitle='" + articleTitle + '\'' +
                ", beginPage=" + beginPage +
                ", endPage=" + endPage +
                ", year='" + year + '\'' +
                ", authorString='" + authorString + '\'' +
                ", path='" + path + '\'' +
                ", collaboration='" + collaboration + '\'' +
                ", postProcessEditors=" + postProcessEditors +
                ", crossrefError=" + crossrefError +
                ", normalized_submission_date=" + normalized_submission_date +
                ", normalized_download_date=" + normalized_download_date +
                ", originalAffiliation='" + originalAffiliation + '\'' +
                ", originalAbstract='" + originalAbstract + '\'' +
                ", originalTitle='" + originalTitle + '\'' +
                ", originalAuthors='" + originalAuthors + '\'' +
                ", originalEditors='" + originalEditors + '\'' +
                ", originalAddress='" + originalAddress + '\'' +
                ", originalNote='" + originalNote + '\'' +
                ", originalKeyword='" + originalKeyword + '\'' +
                ", originalVolumeBlock='" + originalVolumeBlock + '\'' +
                ", originalJournal='" + originalJournal + '\'' +
                ", workingGroup='" + workingGroup + '\'' +
                ", documentType='" + documentType + '\'' +
                '}';
    }

    public int item = -1;

    public static final int Book = 0; // the whole book
    public static final int Periodical = 1; // the journal or magazine item
    public static final int Digital_support = 2;
    public static final int Article = 3; // of a journal or magazine
    public static final int Unknown = 4;
    public static final int InBook = 5;
    public static final int InProceedings = 6;
    public static final int InCollection = 7;
    public static final int Manual = 8;
    public static final int TechReport = 9;
    public static final int MasterThesis = 10;
    public static final int PhdThesis = 11;
    public static final int Unpublished = 12;
    public static final int Proceedings = 13;
    public static final int Serie = 14;

    private BiblioItem parentItem = null; // the bibliographic item "container", i.e. 
    // the book for a chapter
    // the journal for a journal article, etc. 

    private String ISBN13 = null;
    private String ISBN10 = null;
    private String title = null;
    private String publisher = null;
    private int nbPages = -1;
    private String edition = null;
    private String language = null;
    private String subtitle = null;
    private String publication_date = null;
    private Date normalized_publication_date = null;
    private String editors = null;
    private String publisher_website = null;
    private String serie = null;
    private String ISSN = null; // print/default
    private String ISSNe = null; // electronic
    private String volume = null;
    private String number = null;
    private String month = null;
    private String support_type = null;
    private String version = null;
    private String smallImageURL = null;
    private String largeImageURL = null;
    private String publisherPlace = null;
    private String review = null;
    private List<Keyword> keywords;
    private List<String> subjects;
    private List<String> categories;
    private String type = null; // book, journal, proceedings, in book, etc
    private String typeDescription = null;
    private String book_type = null;
    private String doi = null;
    private String inDOI = null;
    private String arXivId = null;
    private String PMID = null;
    private String PMCID = null;
    private String PII = null;
    private String halId = null;
    private String ark = null;
    private String istexId = null;
    private String abstract_ = null;
    private String collaboration = null;
    private String documentType = null;

    // for convenience GROBIDesque
    private String authors = null;
    //private List<LayoutToken> authorsTokens = new ArrayList<>();
    private String firstAuthorSurname = null;
    private String location = null;
    private String bookTitle = null;
    private String serieTitle = null;
    private String pageRange = null;
    private String journal = null;
    private String volumeBlock = null;
    private String institution = null;
    private String note = null;
    private String affiliation = null;
    private String address = null;
    private String country = null;
    private String town = null;
    private String email = null;
    private String pubnum = null;
    private String keyword = null;
    private String phone = null;
    private String degree = null;
    private String web = null;
    private String issue = null;
    private String journal_abbrev = null;
    private String event = null;
    private String abstractHeader = null;
    private String day = null;
    private String locationPublisher = null;
    private String dedication = null;
    private String submission = null;
    private String english_title = null;
    private String url = null;
    private String oaUrl = null;
    private String uri = null;
    private String confidence = null;
    private double conf = 0.0;

    // abstract labeled featured sequence (to produce a structured abstract with, in particular, reference callout)
    private String labeledAbstract = null;

    // date for electronic publishing
    private String e_year = null;
    private String e_month = null;
    private String e_day = null;

    // date of submission 
    private String s_year = null;
    private String s_month = null;
    private String s_day = null;

    // date of acceptance 
    private String a_year = null;
    private String a_month = null;
    private String a_day = null;

    // date of download 
    private String d_year = null;
    private String d_month = null;
    private String d_day = null;

    // advanced grobid recognitions
    private List<String> authorList;
    private List<String> editorList;
    private List<String> affiliationList;
    private List<String> addressList;
    private List<String> emailList;
    private List<String> webList;
    private List<String> phoneList;
    private List<String> markers;

    private List<Person> fullAuthors = null;
    private List<Person> fullEditors = null;
    private List<Affiliation> fullAffiliations = null;

    private String reference = null;
    private String copyright = null;
    private String funding = null;

    //public List<String> affiliationAddressBlock = null; 
    public String affiliationAddressBlock = null;

    // just for articles
    private String articleTitle = null;
    private int beginPage = -1;
    private int endPage = -1;
    private String year = null; // default is publication date on print media 
    private String authorString = null;
    private String path = "";
    private boolean postProcessEditors = false;
    private boolean crossrefError = true;
    private String submission_date = null;
    private Date normalized_submission_date = null;
    private String download_date = null;
    private Date normalized_download_date = null;
    private String server_date = null;
    private Date normalized_server_date = null;

    // for OCR post-corrections
    private String originalAffiliation = null;
    private String originalAbstract = null;
    private String originalTitle = null;
    private String originalAuthors = null;
    private String originalEditors = null;
    private String originalAddress = null;
    private String originalNote = null;
    private String originalKeyword = null;
    private String originalVolumeBlock = null;
    private String originalJournal = null;

    private String workingGroup = null;
    private String rawMeeting = null;

    // Availability statement
    private String availabilityStmt = null;

    // Conflict of interests
    private String conflictStmt = null;

    // Credits / Author contributions
    private String contributionStmt = null;

    // Copyrights/license information object
    CopyrightsLicense copyrightsLicense = null;

    // Source (whether the data was consolidated)
    private String status = CONSOLIDATION_STATUS_EXTRACTED;

    // All the tokens that are considered noise will be collected here
    private List<String> discardedPieces = new ArrayList<>();
    private List<List<LayoutToken>> discardedPiecesTokens = new ArrayList<>();

    public static final List<String> confPrefixes = Arrays.asList("Proceedings of", "proceedings of",
            "In Proceedings of the", "In: Proceeding of", "In Proceedings, ", "In Proceedings of",
            "In Proceeding of", "in Proceeding of", "in Proceeding", "In Proceeding", "Proceedings",
            "proceedings", "In Proc", "in Proc", "In Proc.", "in Proc.", "In proc.", "in proc", "in proc.",
            "In proc", "Proc", "proc", "Proc.", "proc.", "Acte de la", "Acte de", "Acte", "acte de la",
            "acte de", "acte");

    public BiblioItem() {
    }

    public void setParentItem(BiblioItem bi) {
        parentItem = bi;
    }

    public BiblioItem getParentItem() {
        return parentItem;
    }

    public int getItem() {
        return item;
    }

    public void setItem(int type) {
        item = type;
    }

    public String getISBN13() {
        return this.ISBN13;
    }

    public String getISBN10() {
        return this.ISBN10;
    }

    public String getTitle() {
        return this.title;
    }

    public String getPublisher() {
        return this.publisher;
    }

    public String getEdition() {
        return this.edition;
    }

    public String getLanguage() {
        return this.language;
    }

    public String getSubtitle() {
        if (subtitle != null)
            if (subtitle.length() != 0)
                if (!subtitle.equals("null"))
                    return this.subtitle;
        return null;
    }

    public String getPublicationDate() {
        return this.publication_date;
    }

    public Date getNormalizedPublicationDate() {
        return normalized_publication_date;
    }

    public String getEditors() {
        return this.editors;
    }

    public String getPublisherWebsite() {
        return this.publisher_website;
    }

    public String getSerie() {
        return this.serie;
    }

    public String getISSN() {
        return this.ISSN;
    }

    public String getISSNe() {
        return this.ISSNe;
    }

    public String getVolume() {
        return this.volume;
    }

    public String getNumber() {
        return this.number;
    }

    public String getMonth() {
        return this.month;
    }

    public String getSupportType() {
        return this.support_type;
    }

    public String getVersion() {
        return this.version;
    }

    public String getSmallImageURL() {
        return this.smallImageURL;
    }

    public String getLargeImageURL() {
        return this.largeImageURL;
    }

    public String getPublisherPlace() {
        return publisherPlace;
    }

    public String getReview() {
        return this.review;
    }

    public List<String> getCategories() {
        return this.categories;
    }

    public int getNbPages() {
        return nbPages;
    }

    public String getType() {
        return type;
    }

    public String getTypeDescription() {
        return typeDescription;
    }

    public String getBookType() {
        return book_type;
    }

    public String getDOI() {
        return doi;
    }

    public String getHalId() {
        return halId;
    }

    public String getArk() {
        return ark;
    }

    public String getIstexId() {
        return istexId;
    }

    public String getInDOI() {
        return inDOI;
    }

    public String getArXivId() {
        return arXivId;
    }

    public String getPMID() {
        return PMID;
    }

    public String getPMCID() {
        return PMCID;
    }

    public String getPII() {
        return PII;
    }

    public String getArticleTitle() {
        return articleTitle;
    }

    public int getBeginPage() {
        return beginPage;
    }

    public int getEndPage() {
        return endPage;
    }

    public String getYear() {
        return year;
    }

    public String getAbstract() {
        return abstract_;
    }

    public String getLabeledAbstract() {
        return labeledAbstract;
    }

    public String getEmail() {
        return email;
    }

    public String getPubnum() {
        return pubnum;
    }

    public String getCollaboration() {
        return collaboration;
    }

    public String getSerieTitle() {
        return serieTitle;
    }

    public String getURL() {
        return url;
    }

    public String getOAURL() {
        return oaUrl;
    }

    public String getURI() {
        return uri;
    }

    public String getConfidence() {
        return confidence;
    }

    // temp
    public String getAuthors() {
        return authors;
    }

    public String getLocation() {
        return location;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public String getPageRange() {
        if (pageRange != null)
            return pageRange;
        else if ((beginPage != -1) && (endPage != -1))
            return "" + beginPage + "--" + endPage;
        else
            return null;
    }

    public String getJournal() {
        return journal;
    }

    public String getVolumeBlock() {
        return volumeBlock;
    }

    public String getInstitution() {
        return institution;
    }

    public String getNote() {
        return note;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public String getAddress() {
        return address;
    }

    public String getCountry() {
        return country;
    }

    public String getTown() {
        return town;
    }

    public String getKeyword() {
        return keyword;
    }

    public List<Keyword> getKeywords() {
        return keywords;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public String getPhone() {
        return phone;
    }

    public String getDegree() {
        return degree;
    }

    public String getWeb() {
        return web;
    }

    public String getIssue() {
        return issue;
    }

    public String getJournalAbbrev() {
        return journal_abbrev;
    }

    public String getEvent() {
        return event;
    }

    public boolean getError() {
        return crossrefError;
    }

    public String getAbstractHeader() {
        return abstractHeader;
    }

    public String getDay() {
        return day;
    }

    public String getLocationPublisher() {
        return locationPublisher;
    }

    public String getAuthorString() {
        return authorString;
    }

    public String getE_Year() {
        return e_year;
    }

    public String getE_Month() {
        return e_month;
    }

    public String getE_Day() {
        return e_day;
    }

    public String getS_Year() {
        return s_year;
    }

    public String getS_Month() {
        return s_month;
    }

    public String getS_Day() {
        return s_day;
    }

    public String getA_Year() {
        return a_year;
    }

    public String getA_Month() {
        return a_month;
    }

    public String getA_Day() {
        return a_day;
    }

    public String getD_Year() {
        return d_year;
    }

    public String getD_Month() {
        return d_month;
    }

    public String getD_Day() {
        return d_day;
    }

    public String getDedication() {
        return dedication;
    }

    public String getSubmission() {
        return submission;
    }

    public String getEnglishTitle() {
        return english_title;
    }

    public String getSubmissionDate() {
        return submission_date;
    }

    public Date getNormalizedSubmissionDate() {
        return normalized_submission_date;
    }

    public String getDownloadDate() {
        return download_date;
    }

    public Date getNormalizedDownloadDate() {
        return normalized_download_date;
    }

    public String getServerDate() {
        return server_date;
    }

    public Date getNormalizedServerDate() {
        return normalized_server_date;
    }

    public String getOriginalAffiliation() {
        return originalAffiliation;
    }

    public String getOriginalAbstract() {
        return originalAbstract;
    }

    public String getOriginalAuthors() {
        return originalAuthors;
    }

    public String getOriginalEditors() {
        return originalEditors;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public String getOriginalAddress() {
        return originalAddress;
    }

    public String getOriginalNote() {
        return originalNote;
    }

    public String getOriginalKeyword() {
        return originalKeyword;
    }

    public String getOriginalVolumeBlock() {
        return originalVolumeBlock;
    }

    public String getOriginalJournal() {
        return originalJournal;
    }

    public List<org.grobid.core.data.Person> getFullAuthors() {
        return fullAuthors;
    }

    public List<org.grobid.core.data.Person> getFullEditors() {
        return fullEditors;
    }

    public List<org.grobid.core.data.Affiliation> getFullAffiliations() {
        return fullAffiliations;
    }

    public String getReference() {
        return reference;
    }

    public String getCopyright() {
        return copyright;
    }

    public String getFunding() {
        return funding;
    }

    public String getWorkingGroup() {
        return workingGroup;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setISBN13(String isbn) {
        /* some cleaning... */
        this.ISBN13 = StringUtils.normalizeSpace(cleanISBNString(isbn));
    }

    public void setISBN10(String isbn) {
        /* some cleaning... */
        this.ISBN10 = StringUtils.normalizeSpace(isbn);
    }

    public void setTitle(String theTitle) {
        this.title = StringUtils.normalizeSpace(theTitle);
    }

    public void setPublisher(String thePublisher) {
        this.publisher = StringUtils.normalizeSpace(thePublisher);
    }

    public void setEdition(String theEdition) {
        if (theEdition != null) {
            if (theEdition.length() > 10) {
                theEdition = theEdition.substring(0, 9);
            }
        }
        this.edition = StringUtils.normalizeSpace(theEdition);
    }

    public void setLanguage(String theLanguage) {
        this.language = StringUtils.normalizeSpace(theLanguage);
    }

    public void setSubtitle(String theSubtitle) {
        this.subtitle = StringUtils.normalizeSpace(theSubtitle);
    }

    public void setPublicationDate(String theDate) {
        this.publication_date = StringUtils.normalizeSpace(theDate);
    }

    public void setNormalizedPublicationDate(Date theDate) {
        this.normalized_publication_date = theDate;
    }

    public void mergeNormalizedPublicationDate(Date theDate) {
        this.normalized_publication_date = Date.merge(this.normalized_publication_date , theDate);
    }

    public void setEditors(String theEditors) {
        this.editors = StringUtils.normalizeSpace(theEditors);
    }

    public void setPublisherWebsite(String theWebsite) {
        this.publisher_website = StringUtils.normalizeSpace(theWebsite);
    }

    public void setSerie(String theSerie) {
        this.serie = StringUtils.normalizeSpace(theSerie);
    }

    public void setISSN(String theISSN) {
        this.ISSN = StringUtils.normalizeSpace(theISSN);
    }

    public void setISSNe(String theISSN) {
        this.ISSNe = StringUtils.normalizeSpace(theISSN);
    }

    public void setVolume(String theVolume) {
        this.volume = StringUtils.normalizeSpace(theVolume);
    }

    public void setNumber(String theNumber) {
        this.number = StringUtils.normalizeSpace(theNumber);
    }

    public void setMonth(String theMonth) {
        this.month = StringUtils.normalizeSpace(theMonth);
    }

    public void setSupportType(String theType) {
        this.support_type = StringUtils.normalizeSpace(theType);
    }

    public void setVersion(String theVersion) {
        this.version = StringUtils.normalizeSpace(theVersion);
    }

    public void setSmallImageURL(String url) {
        this.smallImageURL = url;
    }

    public void setLargeImageURL(String url) {
        this.largeImageURL = url;
    }

    public void setPublisherPlace(String p) {
        this.publisherPlace = StringUtils.normalizeSpace(p);
    }

    public void setCategories(List<String> cat) {
        this.categories = cat;
    }

    public void addCategory(String cat) {
        if (categories == null) {
            categories = new ArrayList<String>();
        }
        categories.add(cat);
    }

    public void setNbPages(int nb) {
        this.nbPages = nb;
    }

    public void setReview(String rev) {
        this.review = rev;
    }

    public void setType(String t) {
        this.type = t;
    }

    public void setTypeDescription(String t) {
        typeDescription = t;
    }

    public void setBookType(String bt) {
        this.book_type = StringUtils.normalizeSpace(bt);
    }

    public void setDOI(String id) {
        if (id == null)
            return;
        this.doi = cleanDOI(id);
    } 

    public void setInDOI(String id) {
        if (id != null) {
            inDOI = StringUtils.normalizeSpace(id);
            inDOI = inDOI.replace(" ", "");
            inDOI = cleanDOI(inDOI);
        }
    }

    public static String cleanDOI(String doi) {
        if (doi == null) {
            return doi;
        }

        doi = StringUtils.normalizeSpace(doi);
        doi = doi.replace(" ", "");
        doi = doi.replaceAll("https?\\://(dx\\.)?doi\\.org/", "");

        //bibl = bibl.replace("//", "/");
        if (doi.toLowerCase().startsWith("doi:") || doi.toLowerCase().startsWith("doi/")) {
            doi = doi.substring(4);
        }
        if (doi.toLowerCase().startsWith("doi")) {
            doi = doi.substring(3);
        }
        // pretty common wrong extraction pattern:
        // 43-61.DOI:10.1093/jpepsy/14.1.436/7
        // 367-74.DOI:10.1080/14034940210165064
        // (pages concatenated to the DOI) - easy/safe to fix
        if (StringUtils.containsIgnoreCase(doi, "doi:10.")) {
            doi = doi.substring(StringUtils.indexOfIgnoreCase(doi, "doi:10.")+4);
        }

        // for DOI coming from PDF links, we have some prefix cleaning to make
        if (doi.startsWith("file://") || doi.startsWith("https://") || doi.startsWith("http://")) {
            int ind = doi.indexOf("/10.");
            if (ind != -1)
                doi = doi.substring(ind+1);
        }

        doi = doi.trim();
        int ind = doi.indexOf("http://");
        if (ind > 10) {
            doi = doi.substring(0, ind);
        }

        doi = doi.replaceAll("[\\p{M}]", "");
        doi = doi.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // remove possible starting/trailing parenthesis
        if (doi.startsWith("(") || doi.startsWith("[") || doi.startsWith("⟨"))
            doi = doi.substring(1);

        if (doi.endsWith(")") || doi.endsWith("]") || doi.endsWith("⟩"))
            doi = doi.substring(0,doi.length()-1);

        return doi;
    }

    public void setHalId(String halId) {
        this.halId = halId;
    }

    public void setArXivId(String id) {
        if (id != null) {
            arXivId = StringUtils.normalizeSpace(id);
            arXivId = arXivId.replace(" ", "");
        }
    }

    public void setPMID(String id) {
        if (id != null) {
            PMID = StringUtils.normalizeSpace(id);
            PMID = PMID.replace(" ", "");
        }
    }

    public void setPMCID(String id) {
        if (id != null) {
            PMCID = StringUtils.normalizeSpace(id);
            PMCID = PMCID.replace(" ", "");
        }
    }

    public void setPII(String id) {
        if (id != null) {
            PII = StringUtils.normalizeSpace(id);
            PII = PII.replace(" ", "");
        }
    }

    public void setIstexId(String id) {
        istexId = id;
    }

    public void setArk(String id) {
        ark = id;
    }

    public void setArticleTitle(String ti) {
        articleTitle = StringUtils.normalizeSpace(ti);
    }

    public void setBeginPage(int p) {
        beginPage = p;
    }

    public void setEndPage(int p) {
        endPage = p;
    }

    public void setYear(String y) {
        year = StringUtils.normalizeSpace(y);
    }

    public void setAbstract(String a) {
        abstract_ = cleanAbstract(a);
    }

    public void setLabeledAbstract(String labeledAbstract) {
        this.labeledAbstract = labeledAbstract;
    }

    public void setLocationPublisher(String s) {
        locationPublisher = StringUtils.normalizeSpace(s);
    }

    public void setSerieTitle(String s) {
        serieTitle = StringUtils.normalizeSpace(s);
    }

    public void setAuthorString(String s) {
        authorString = s;
    }

    public void setURL(String s) {
        url = StringUtils.normalizeSpace(s);
    }

    public void setOAURL(String s) {
        oaUrl = s;
    }

    public void setURI(String s) {
        uri = StringUtils.normalizeSpace(s);
    }

    public void setConfidence(String s) {
        confidence = s;
    }

    public void setConf(double b) {
        conf = b;
    }

    public void setFullAuthors(List<org.grobid.core.data.Person> full) {
        fullAuthors = full;
    }

    public void setFullEditors(List<org.grobid.core.data.Person> full) {
        fullEditors = full;
    }

    public void setFullAffiliations(List<org.grobid.core.data.Affiliation> full) {
        fullAffiliations = full;
        // if no id is present in the affiliation objects, we add one
        int num = 0;
        if (fullAffiliations != null) {
            for (Affiliation affiliation : fullAffiliations) {
                if (affiliation.getKey() == null) {
                    affiliation.setKey("aff"+num);
                }
                num++;
            }
        }
    }

    public void setWorkingGroup(String wg) {
        this.workingGroup = wg;
    }

    public void setDocumentType(String doctype) {
        this.documentType = doctype;
    }

    // temp
    public void setAuthors(String aut) {
        authors = aut;
    }

    public BiblioItem collectAuthorsToken(LayoutToken lt) {
        authorsTokensWorkingCopy.add(lt);
        return this;
    }

    public void collectAuthorsTokens(List<LayoutToken> layoutTokens) {
        this.authorsTokensWorkingCopy.addAll(layoutTokens);
    }

    public void collectAbstractTokens(List<LayoutToken> layoutTokens) {
        this.abstractTokensWorkingCopy.addAll(layoutTokens);
    }

    public void addAuthor(String aut) {
        if (authors == null)
            authors = aut;
        else
            authors += " ; " + aut;

        if (authorList == null)
            authorList = new ArrayList<String>();
        if (!authorList.contains(aut))
            authorList.add(aut);
    }

    public void addFullAuthor(Person aut) {
        if (fullAuthors == null)
            fullAuthors = new ArrayList<Person>();
        if (!fullAuthors.contains(aut))
            fullAuthors.add(aut);
    }

    public void addFullEditor(Person aut) {
        if (fullEditors == null)
            fullEditors = new ArrayList<Person>();
        if (!fullEditors.contains(aut))
            fullEditors.add(aut);
    }

    public void addEditor(String aut) {
        if (editors == null)
            editors = aut;
        else
            editors += " ; " + aut;

        if (editorList == null)
            editorList = new ArrayList<String>();
        if (!editorList.contains(aut))
            editorList.add(aut);
    }

    public void setLocation(String loc) {
        location = StringUtils.normalizeSpace(loc);
    }

    public void setBookTitle(String book) {
        bookTitle = StringUtils.normalizeSpace(book);
    }

    public void setPageRange(String pages) {
        pageRange = StringUtils.normalizeSpace(pages);
    }

    public void setJournal(String jour) {
        journal = StringUtils.normalizeSpace(jour);
    }

    public void setVolumeBlock(String vol, boolean postProcess) {
        volumeBlock = StringUtils.normalizeSpace(vol);
        if (postProcess)
            volumeBlock = postProcessVolumeBlock();
    }

    public void setInstitution(String inst) {
        institution = StringUtils.normalizeSpace(inst);
    }

    public void setNoteOrConcatenateIfNotEmpty(String note) {
        if (StringUtils.isBlank(this.note)) {
            this.note = StringUtils.normalizeSpace(note);
        } else {
            this.note += " " + StringUtils.normalizeSpace(note);
        }
    }

    public void setNote(String not) {
        note = StringUtils.normalizeSpace(not);
    }

    public void setAffiliation(String a) {
        affiliation = a;
    }

    public void setAddress(String a) {
        address = a;
    }

    public void setCountry(String a) {
        country = a;
    }

    public void setTown(String a) {
        town = a;
    }

    public void setEmail(String e) {
        email = e;
    }

    public void setPubnum(String p) {
        pubnum = StringUtils.normalizeSpace(p);
    }

    public void setKeyword(String k) {
        keyword = cleanKeywords(k);
    }

    public void addKeyword(String k) {
        if (keywords == null)
            keywords = new ArrayList<>();
		String theKey = cleanKeywords(k);
		if (theKey.toLowerCase().contains("introduction")) {
			// if the keyword contains introduction, this is normally a segmentation error
			theKey = null;
		}
		if (theKey != null) {
        	keywords.add(new Keyword(theKey));
		}
    }

    public void setKeywords(List<Keyword> k) {
        keywords = k;
    }

    public void addSubject(String k) {
        if (subjects == null)
            subjects = new ArrayList<String>();
        subjects.add(k);
    }

    public void setSubjects(List<String> k) {
        subjects = k;
    }

    public void setPhone(String p) {
        phone = p;
    }

    public void setDegree(String d) {
        degree = StringUtils.normalizeSpace(d);
    }

    public void setWeb(String w) {
        web = StringUtils.normalizeSpace(w);
        web = web.replace(" ", "");

        if (StringUtils.isEmpty(doi)) {
            Matcher doiMatcher = TextUtilities.DOIPattern.matcher(web);
            if (doiMatcher.find()) { 
                setDOI(doiMatcher.group());
            }
        } 
    }

    public void setCollaboration(String collab) {
        collaboration = StringUtils.normalizeSpace(collab);
    }

    public void setIssue(String i) {
        issue = StringUtils.normalizeSpace(i);
    }

    public void setJournalAbbrev(String j) {
        journal_abbrev = StringUtils.normalizeSpace(j);
    }

    public void setEvent(String e) {
        event = StringUtils.normalizeSpace(e);
    }

    public void setError(boolean e) {
        crossrefError = e;
    }

    public void setAbstractHeader(String a) {
        abstractHeader = StringUtils.normalizeSpace(a);
    }

    public void setPath(String p) {
        path = p;
    }

    public void setDay(String d) {
        day = d;
    }

    public void setE_Year(String d) {
        e_year = d;
    }

    public void setE_Month(String d) {
        e_month = d;
    }

    public void setE_Day(String d) {
        e_day = d;
    }

    public void setA_Year(String d) {
        a_year = d;
    }

    public void setA_Month(String d) {
        a_month = d;
    }

    public void setA_Day(String d) {
        a_day = d;
    }

    public void setS_Year(String d) {
        s_year = d;
    }

    public void setS_Month(String d) {
        s_month = d;
    }

    public void setS_Day(String d) {
        s_day = d;
    }

    public void setD_Year(String d) {
        d_year = d;
    }

    public void setD_Month(String d) {
        d_month = d;
    }

    public void setD_Day(String d) {
        d_day = d;
    }

    public void setDedication(String d) {
        dedication = StringUtils.normalizeSpace(d);
    }

    public void setSubmission(String s) {
        submission = StringUtils.normalizeSpace(s);
    }

    public void setEnglishTitle(String d) {
        english_title = StringUtils.normalizeSpace(d);
    }

    public void setSubmissionDate(String d) {
        submission_date = StringUtils.normalizeSpace(d);
    }

    public void setNormalizedSubmissionDate(Date d) {
        normalized_submission_date = d;
    }

    public void setDownloadDate(String d) {
        download_date = StringUtils.normalizeSpace(d);
    }

    public void setNormalizedDownloadDate(Date d) {
        normalized_download_date = d;
    }

    public void setServerDate(String d) {
        server_date = StringUtils.normalizeSpace(d);
    }

    public void setNormalizedServerDate(Date d) {
        normalized_server_date = d;
    }

    public void setOriginalAffiliation(String original) {
        originalAffiliation = original;
    }

    public void setOriginalAbstract(String original) {
        originalAbstract = original;
    }

    public void setOriginalAuthors(String original) {
        originalAuthors = original;
    }

    public void setOriginalEditors(String original) {
        originalEditors = original;
    }

    public void setOriginalTitle(String original) {
        originalTitle = original;
    }

    public void setOriginalAddress(String original) {
        originalAddress = original;
    }

    public void setOriginalNote(String original) {
        originalNote = original;
    }

    public void setOriginalKeyword(String original) {
        originalKeyword = original;
    }

    public void setOriginalVolumeBlock(String original) {
        originalVolumeBlock = original;
    }

    public void setOriginalJournal(String original) {
        originalJournal = original;
    }

    public void setReference(String ref) {
        reference = ref;
    }

    public void setCopyright(String cop) {
        copyright = StringUtils.normalizeSpace(cop);
    }

    public void setFunding(String gra) {
        funding = StringUtils.normalizeSpace(gra);
    }

    public String getMeeting() {
        return rawMeeting;
    }

    public void setMeeting(String meet) {
        this.rawMeeting = meet;
    }

    /**
     * General string cleaining for SQL strings. This method might depend on the chosen
     * relational database.
     */
    public static String cleanSQLString(String str) {
        if (str == null)
            return null;
        if (str.length() == 0)
            return null;
        String cleanedString = "";
        boolean special = false;
        for (int index = 0; (index < str.length()); index++) {
            char currentCharacter = str.charAt(index);
            if ((currentCharacter == '\'') || (currentCharacter == '%') || (currentCharacter == '_')) {
                special = true;
                cleanedString += '\\';
            }
            cleanedString += currentCharacter;
        }

        return cleanedString;
    }

    /**
     * Special string cleaining of ISBN and ISSN numbers.
     */
    public static String cleanISBNString(String str) {
        String cleanedString = "";
        for (int index = 0; (index < str.length()); index++) {
            char currentCharacter = str.charAt(index);
            if ((currentCharacter != '-') && (currentCharacter != ' ') && (currentCharacter != '\''))
                cleanedString += currentCharacter;
        }

        return StringUtils.normalizeSpace(cleanedString);
    }

    /**
     * Reinit all the values of the current bibliographical item
     */
    public void reset() {
        ISBN13 = null;
        ISBN10 = null;
        title = null;
        publisher = null;
        edition = null;
        language = null;
        subtitle = null;
        publication_date = null;
        normalized_publication_date = null;
        editors = null;
        publisher_website = null;
        serie = null;
        ISSN = null;
        ISSNe = null;
        volume = null;
        number = null;
        month = null;
        support_type = null;
        version = null;
        smallImageURL = null;
        largeImageURL = null;
        publisherPlace = null;
        review = null;
        categories = null;
        nbPages = -1;
        type = null;
        book_type = null;
        doi = null;
        halId = null;
        istexId = null;
        ark = null;
        inDOI = null;
        arXivId = null;
        PMID = null;
        PMCID = null;
        PII = null;
        abstract_ = null;
        url = null;
        oaUrl = null;
        uri = null;

        authors = null;
        location = null;
        bookTitle = null;
        pageRange = null;
        journal = null;
        volumeBlock = null;
        institution = null;
        note = null;
        affiliation = null;
        address = null;
        email = null;
        pubnum = null;
        keyword = null;
        phone = null;
        degree = null;
        web = null;
        issue = null;
        journal_abbrev = null;
        event = null;
        day = null;
        submission_date = null;
        normalized_submission_date = null;
        download_date = null;
        normalized_download_date = null;
        server_date = null;
        normalized_server_date = null;

        beginPage = -1;
        endPage = -1;
        articleTitle = null;
        dedication = null;
        submission = null;
        english_title = null;

        fullAuthors = null;
        fullAffiliations = null;
        reference = null;
        copyright = null;
        funding = null;

        workingGroup = null;
        documentType = null;
    }

    /**
     * Post process the volume block in order to distinguish when
     * possible and when appropriate volume and issue
     */
    public String postProcessVolumeBlock() {
        if (volumeBlock == null) {
            return null;
        }
        if (volumeBlock.length() == 0) {
            return volumeBlock;
        }
        volumeBlock = StringUtils.normalizeSpace(volumeBlock);

        // the volume is always the first full number sequence of the block
        // first we remove the possible non digit character prefix
        boolean stop = false;
        int p = 0;
        while (!stop && (p < volumeBlock.length())) {
            if (Character.isDigit(volumeBlock.charAt(p)))
                stop = true;
            else
                p++;
        }

        if (!stop)
            return volumeBlock; // we just have letters... we can't do anything

        int i = p;
        stop = false;
        while (!stop && (i < volumeBlock.length())) {
            if (!Character.isDigit(volumeBlock.charAt(i)))
                stop = true;
            else
                i++;
        }

        String resVolume = null;
        if (stop)
            resVolume = volumeBlock.substring(p, i);
        else
            return volumeBlock.substring(p);

        // we then have at least one non numerical character
        stop = false;
        while (!stop && (i < volumeBlock.length())) {
            if (Character.isDigit(volumeBlock.charAt(i)))
                stop = true;
            else
                i++;
        }

        if (!stop)
            return resVolume;

        // if present, the second number sequence is the issue 
        stop = false;
        int j = i + 1;
        while (!stop && (j < volumeBlock.length())) {
            if (!Character.isDigit(volumeBlock.charAt(j)))
                stop = true;
            else
                j++;
        }

        if (!stop)
            j = volumeBlock.length();

        issue = volumeBlock.substring(i, j);
        return resVolume;
    }

    /**
     * Some little cleaning of the abstract field.
     * 
     * To be done: use a short text model to structure abstract
     */
    public static final String[] ABSTRACT_PREFIXES = {"abstract", "summary", "résumé", "abrégé", "a b s t r a c t"};

    public String cleanAbstract(String string) {

        if (string == null)
            return null;
        if (string.length() == 0)
            return string;
        String res = StringUtils.normalizeSpace(string);
        String res0 = res.toLowerCase();

        for (String abstractPrefix : ABSTRACT_PREFIXES) {
            if (res0.startsWith(abstractPrefix)) {
                if (abstractPrefix.length() < res.length()) {
                    res = res.substring(abstractPrefix.length(), res.length());
                    res.trim();
                } else {
                    res = "";
                }
                abstractHeader = abstractPrefix;
                break;
            }
        }

        if ((res.startsWith(".")) || (res.startsWith(":")) || (res.startsWith(")"))) {
            res = res.substring(1, res.length());
            res = res.trim();
        }

        //res = res.replace("@BULLET", " • ");

        res = res.replace("( ", "(");
        res = res.replace(" )", ")");
        res = res.replace("  ", " ");

        return res;
    }

    public static List<LayoutToken> cleanAbstractLayoutTokens(List<LayoutToken> tokens) {
        if (tokens == null)
            return null;
        if (tokens.size() == 0)
            return tokens;

        int n = 0;
        while(n < tokens.size()) {
            String tokenString = StringUtils.normalizeSpace(tokens.get(n).getText().toLowerCase());
            if (tokenString.length() == 0 || TextUtilities.delimiters.contains(tokenString)) {
                n++;
                continue;
            }
            boolean matchPrefix = false;
            for (String abstractPrefix : ABSTRACT_PREFIXES) {
                if (tokenString.equals(abstractPrefix)) {
                    matchPrefix = true;
                    break;
                }
            }
            if (matchPrefix) {
                n++;
                continue;
            }
            break;
        }

        return tokens.subList(n, tokens.size());
    }

    public static void cleanTitles(BiblioItem bibl) {
        if (bibl.getTitle() != null) {
            String localTitle = TextUtilities.cleanField(bibl.getTitle(), false);
            if (localTitle != null && localTitle.endsWith(" y")) {
                // some markers at the end of the title are extracted from the pdf as " y" at the end of the title
                // e.g. <title level="a" type="main">Computations in finite-dimensional Lie algebras y</title>
                localTitle = localTitle.substring(0, localTitle.length() - 2);
            }
            bibl.setTitle(localTitle);
        }
        if (bibl.getBookTitle() != null) {
            bibl.setBookTitle(TextUtilities.cleanField(bibl.getBookTitle(), false));
        }
    }

    /**
     * Some little cleaning of the keyword field (likely unnecessary with latest header model).
     */
    public static String cleanKeywords(String string) {
        if (string == null)
            return null;
        if (string.length() == 0)
            return string;
        String res = StringUtils.normalizeSpace(string);
        String resLow = res.toLowerCase();
        if (resLow.startsWith("keywords")) {
            res = res.substring(8);
        } else if (resLow.startsWith("key words") || resLow.startsWith("mots clés") || resLow.startsWith("mots cles")) {
            res = res.substring(9);
        } else if (resLow.startsWith("mots clefs")) {
            res = res.substring(10);
        }
		
        res = res.trim();
        if (res.startsWith(":") || res.startsWith("—") || res.startsWith("-")) {
            res = res.substring(1);
        }
        if (res.endsWith(".")) {
            res = res.substring(0, res.length() - 1);
        }

        return res.trim();
    }

    /**
     * Keyword field segmentation.
     * 
     * TBD: create a dedicated model to analyse the keyword field, segmenting them properly and 
     * identifying the possible schemes
     */
    public static List<Keyword> segmentKeywords(String string) {
        if (string == null)
            return null;
        if (string.length() == 0)
            return null;
		String type = null;
        if (string.startsWith("Categories and Subject Descriptors")) {
            type = "subject-headers";
			string = string.replace("Categories and Subject Descriptors", "").trim();
        } 
		else if (string.startsWith("PACS Numbers") || string.startsWith("PACS") ) {
            type = "pacs";
            string = string.replace("PACS Numbers", "").replace("PACS", "").trim();
			if (string.startsWith(":")) {
	            string = string.substring(1);
			}
        }
		else {
			type = "author";
		}
		
		List<Keyword> result = new ArrayList<Keyword>();
		// the list of possible keyword separators
		List<String> separators = Arrays.asList(";","■", "•", "ㆍ", "Á", "\n", ",", ".", ":", "/", "|");
        List<String> separatorsSecondary = Arrays.asList("•", "■");
		for(String separator : separators) {
	        StringTokenizer st = new StringTokenizer(string, separator);
	        if (st.countTokens() > 2) {
	            while (st.hasMoreTokens()) {
					String res = st.nextToken().trim();
					if (res.startsWith(":")) {
			            res = res.substring(1);
			        }
                    boolean noSecondary = true;
					res = res.replace("\n", " ").replaceAll("( )+", " ");
                    for(String separatorSecondary : separatorsSecondary) {
                        StringTokenizer st2 = new StringTokenizer(res, separatorSecondary);
                        if (st2.countTokens() > 1) {
                            while (st2.hasMoreTokens()) {
                                String res2 = st2.nextToken().trim();
                                res2 = res2.replace("\n", " ").replaceAll("( )+", " ");
                                Keyword keyw = new Keyword(res2, type);
                                result.add(keyw);
                            }
                            noSecondary = false;
                        }
                    }
                    if (noSecondary) {
    					Keyword keyw = new Keyword(res, type);
	       				result.add(keyw);
                    }
	            }
				break;
	        }
		}
		
		return result;
	}	

    /**
     * Format initials by appending a dot to single-letter tokens.
     * Splits on whitespace and hyphens (preserving hyphens).
     * e.g. "W S" -> "W. S.", "J-L" -> "J.-L.", "Nicholas" -> "Nicholas", "W" -> "W."
     */
    private static String formatInitials(String name) {
        if (StringUtils.isBlank(name)) {
            return name;
        }
        String[] spaceParts = name.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < spaceParts.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            // Handle hyphenated parts
            String[] hyphenParts = spaceParts[i].split("(?<=-)(?=[^-])|(?<=[^-])(?=-)");
            for (String hp : hyphenParts) {
                if (hp.equals("-")) {
                    result.append("-");
                } else if (hp.length() == 1 && Character.isLetter(hp.charAt(0))) {
                    result.append(hp).append(".");
                } else {
                    result.append(hp);
                }
            }
        }
        return result.toString();
    }

    /**
     * Format a Person's name for BibTeX output.
     * Returns the formatted name string, or empty string if no name parts are present.
     */
    private static String formatPersonNameBibTeX(Person person) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(person.getLastName())) {
            sb.append(person.getLastName().trim());
        }
        if (StringUtils.isNotBlank(person.getFirstName())) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(formatInitials(person.getFirstName().trim()));
        }
        if (StringUtils.isNotBlank(person.getMiddleName())) {
            if (sb.length() > 0) {
                sb.append(StringUtils.isBlank(person.getFirstName()) ? ", " : " ");
            }
            sb.append(formatInitials(person.getMiddleName().trim()));
        }
        return sb.toString();
    }

    /**
     * Generate a BibTeX key from the first author's surname, the publication year,
     * and the first significant word of the title (all lowercased).
     * If the first word of the title is 2 characters or fewer, it is merged with the second word.
     */
    public String generateBibTeXKey() {
        StringBuilder key = new StringBuilder();

        // Author component
        String surname = getFirstAuthorSurname();
        if (StringUtils.isNotBlank(surname)) {
            key.append(surname.replaceAll("[^\\p{L}]", "").toLowerCase());
        }

        // Year component
        if (normalized_publication_date != null && normalized_publication_date.getYear() >= 0) {
            key.append(normalized_publication_date.getYear());
        } else if (StringUtils.isNotBlank(publication_date)) {
            String yearStr = publication_date.replaceAll("[^0-9]", "");
            if (yearStr.length() >= 4) {
                key.append(yearStr.substring(0, 4));
            } else if (!yearStr.isEmpty()) {
                key.append(yearStr);
            }
        }

        // Title component (fall back to bookTitle if title is missing)
        String titleForKey = StringUtils.isNotBlank(title) ? title : bookTitle;
        if (StringUtils.isNotBlank(titleForKey)) {
            String[] words = titleForKey.trim().split("\\s+");
            if (words.length > 0) {
                String firstWord = words[0].replaceAll("[^\\p{L}]", "").toLowerCase();
                if (firstWord.length() <= 2 && words.length > 1) {
                    firstWord += words[1].replaceAll("[^\\p{L}]", "").toLowerCase();
                }
                key.append(firstWord);
            }
        }

        if (key.length() == 0) {
            return "unknown";
        }
        return key.toString();
    }

    /**
     * Export to BibTeX format using an auto-generated key.
     */
    public String toBibTeX() {
		return toBibTeX(generateBibTeXKey());
	}

    /**
     * Export to BibTeX format
     *
     * @param id the BibTeX key to use.
     */
    public String toBibTeX(String id) {
        return toBibTeX(id, new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder().includeRawCitations(false).build());
    }

    /**
     * Export to BibTeX format
     *
     * @param id the BibTeX key to use
     */
    public String toBibTeX(String id, GrobidAnalysisConfig config) {
        String type;
        if (journal != null) {
            type = "article";
        } else if (book_type != null) {
            type = "techreport";
        } else if (bookTitle != null) {
            if (StringUtils.containsIgnoreCase(bookTitle, "proceedings") ||
                (bookTitle.startsWith("proc")) || (bookTitle.startsWith("Proc")) ||
                (bookTitle.startsWith("In Proc")) || (bookTitle.startsWith("In proc"))) {
                type = "inproceedings";
            } else {
                LOGGER.debug("No journal given, but a booktitle. However, the booktitle does not start with \"proc\" or similar strings. Returning inbook");
                type = "inbook";
            }
        } else {
            // using "misc" as fallback type
            type = "misc";
        }

        StringJoiner bibtex = new StringJoiner(",\n", "@" + type + "{" + id + ",\n", "\n}\n");

        try {

            // author 
            // fullAuthors has to be used instead
            if (collaboration != null) {
                bibtex.add("  author = {" + collaboration + "}");
            } else {
                StringJoiner authors = new StringJoiner(" and ", "  author = {", "}");
                if (CollectionUtils.isNotEmpty(fullAuthors)) {
                    fullAuthors.stream()
                       .filter(Objects::nonNull)
                       .forEachOrdered(person -> {
                           String author = formatPersonNameBibTeX(person);
                           if (StringUtils.isNotBlank(author)) {
                               authors.add(author);
                           }
                       });
                } else if (this.authors != null) {
                    StringTokenizer st = new StringTokenizer(this.authors, ";");
                    while (st.hasMoreTokens()) {
                        String author = st.nextToken();
                        if (author != null) {
                            authors.add(author.trim());
                        }
                    }
                }
                bibtex.add(authors.toString());
            }

            // title
            if (title != null) {
                bibtex.add("  title = {" + title + "}");
            }

            // journal
            if (journal != null) {
                bibtex.add("  journal = {" + journal + "}");
            }

            // booktitle
            if ((journal == null) && (book_type == null) && (bookTitle != null)) {
                bibtex.add("  booktitle = {" + bookTitle + "}");
            }

            // booktitle
            if ((journal == null) && (serieTitle != null)) {
                bibtex.add("  series = {" + serieTitle + "}");
            }

            // publisher
            if (publisher != null) {
                bibtex.add("  publisher = {" + publisher + "}");
            }

            // editors
            boolean editorsAdded = false;
            if (CollectionUtils.isNotEmpty(fullEditors)) {
                String editorStr = fullEditors.stream()
                       .filter(Objects::nonNull)
                       .map(BiblioItem::formatPersonNameBibTeX)
                       .filter(StringUtils::isNotBlank)
                       .collect(Collectors.joining(" and "));
                if (StringUtils.isNotBlank(editorStr)) {
                    bibtex.add("  editor = {" + editorStr + "}");
                    editorsAdded = true;
                }
            }
            if (!editorsAdded && editors != null) {
                String locEditors = editors.replace(" ; ", " and ");
                bibtex.add("  editor = {" + locEditors + "}");
            }

            // dates
            if (normalized_publication_date != null) {
                String isoDate = Date.toISOString(normalized_publication_date);
                if (isoDate != null) {
                    bibtex.add("  date = {" + isoDate + "}");
                }
                if (normalized_publication_date.getYear() >= 0) {
                    bibtex.add("  year = {" + normalized_publication_date.getYear() + "}");
                
                    if (normalized_publication_date.getMonth() >= 0) {
                        bibtex.add("  month = {" + normalized_publication_date.getMonth() + "}");
                    
                        if (normalized_publication_date.getDay() >= 0) {
                            bibtex.add("  day = {" + normalized_publication_date.getDay() + "}");
                        }
                    }
                }
            } else if (publication_date != null) {
                bibtex.add("  year = {" + publication_date + "}");
            }

            // address
            if (location != null) {
                bibtex.add("  address = {" + location + "}");
            }

            // pages
            if (pageRange != null) {
                bibtex.add("  pages = {" + pageRange + "}");
            }

			// volume
			if (volumeBlock != null) {
                bibtex.add("  volume = {" + volumeBlock + "}");
			}

			// issue (named number in BibTeX)
			if (issue != null) {
                bibtex.add("  number = {" + issue + "}");
			}

            // DOI
            if (!StringUtils.isEmpty(doi)) {
                bibtex.add("  doi = {" + doi + "}");
            }

            // arXiv identifier
            if (!StringUtils.isEmpty(arXivId)) {
                bibtex.add("  eprint = {" + arXivId + "}");
            }
            /* note that the following is now recommended for arXiv citations: 
                    archivePrefix = "arXiv",
                    eprint        = "0707.3168",
                    primaryClass  = "hep-th",
                (here old identifier :( ))
                see https://arxiv.org/hypertex/bibstyles/
            */

            // abstract
            if (!StringUtils.isEmpty(abstract_)) {
                bibtex.add("  abstract = {" + abstract_ + "}");
            }

            // keywords
            if (keywords != null) {
                String value = keywords.stream()
                        .map(keyword -> keyword.getKeyword())
                        .filter(keyword -> !StringUtils.isBlank(keyword))
                        .collect(Collectors.joining(", ", "keywords = {", "}"));
                bibtex.add(value);
            }

            if (config.getIncludeRawCitations() && !StringUtils.isEmpty(reference) ) {
                // escape all " signs
                bibtex.add("  raw = {" + reference + "}");
            }
        } catch (Exception e) {
            LOGGER.error("Cannot export BibTex format, because of nested exception.", e);
            throw new GrobidException("Cannot export BibTex format, because of nested exception.", e);
        }
        return bibtex.toString();
    }

    /** 
     * Check if the identifier pubnum is a DOI or an arXiv identifier. If yes, instanciate 
     * the corresponding field and reset the generic pubnum field.
     */
    public void checkIdentifier() {
        // DOI
        if (!StringUtils.isEmpty(pubnum) && StringUtils.isEmpty(doi)) {
            Matcher doiMatcher = TextUtilities.DOIPattern.matcher(pubnum);
            if (doiMatcher.find()) { 
                setDOI(pubnum);
                setPubnum(null);
            } else {
                doiMatcher = TextUtilities.DOIPattern.matcher(pubnum.replace(" ", ""));
                if (doiMatcher.find()) { 
                    setDOI(pubnum);
                    setPubnum(null);
                }
            }
        } 
        // arXiv id (this covers old and new versions)
        if (!StringUtils.isEmpty(pubnum) && StringUtils.isEmpty(arXivId)) {
            Matcher arxivMatcher = TextUtilities.arXivPattern.matcher(pubnum);
            if (arxivMatcher.find()) { 
                setArXivId(pubnum);
                setPubnum(null);
            }
        } 
        // PMID 
        if (!StringUtils.isEmpty(pubnum) && StringUtils.isEmpty(PMID)) {
            Matcher pmidMatcher = TextUtilities.pmidPattern.matcher(pubnum);
            if (pmidMatcher.find()) { 
                // last group gives the PMID digits
                String digits = pmidMatcher.group(pmidMatcher.groupCount());
                setPMID(digits);
                setPubnum(null);
            }
        } 
        // PMC ID
        if (!StringUtils.isEmpty(pubnum) && StringUtils.isEmpty(PMCID)) {
            Matcher pmcidMatcher = TextUtilities.pmcidPattern.matcher(pubnum);
            if (pmcidMatcher.find()) { 
                // last group gives the PMC ID digits, but the prefix PMC must be added to follow the NIH guidelines
                String digits = pmcidMatcher.group(pmcidMatcher.groupCount());
                setPMCID("PMC"+digits);
                setPubnum(null);
            }
        } 
        // ISSN
        if (!StringUtils.isEmpty(pubnum) && StringUtils.isEmpty(ISSN)) {
            if (pubnum.toLowerCase().indexOf("issn") != -1) {
                pubnum = pubnum.replace("issn", "");
                pubnum = pubnum.replace("ISSN", "");
                pubnum = TextUtilities.cleanField(pubnum, true);
                if (pubnum != null)
                    setISSN(pubnum);
                setPubnum(null);
            }
        }

        // ISBN
        if (!StringUtils.isEmpty(pubnum) && StringUtils.isEmpty(ISBN13)) {
            if (pubnum.toLowerCase().indexOf("isbn") != -1) {
                pubnum = pubnum.replace("isbn", "");
                pubnum = pubnum.replace("ISBN", "");
                pubnum = TextUtilities.cleanField(pubnum, true);
                if (pubnum != null && pubnum.length() == 10)
                    setISBN10(pubnum);
                else if (pubnum != null && pubnum.length() == 13)
                    setISBN13(pubnum);
                setPubnum(null);
            }
        }

        // TODO: PII and HALId

    }

    /**
     * Export the bibliographical item into a TEI BiblStruct string
     *
     * @param n - the index of the bibliographical record, the corresponding id will be b+n
     */
    public String toTEI(int n) {
        return toTEI(n, 0, GrobidAnalysisConfig.defaultInstance());
    }

    /**
     * Export the bibliographical item into a TEI BiblStruct string
     *
     * @param n - the index of the bibliographical record, the corresponding id will be b+n
     */
    public String toTEI(int n, GrobidAnalysisConfig config) {
        return toTEI(n, 0, config);
    }

    /**
     * Export the bibliographical item into a TEI BiblStruct string
     *
     * @param n - the index of the bibliographical record, the corresponding id will be b+n
     * @param indent - the tabulation indentation for the output of the xml elements
     */
    public String toTEI(int n, int indent) {
        return toTEI(n, indent, GrobidAnalysisConfig.defaultInstance());
    }

    /**
     * Export the bibliographical item into a TEI BiblStruct string
     *
     * @param n      - the index of the bibliographical record, the corresponding id will be b+n
     * @param indent - the tabulation indentation for the output of the xml elements
     */
    public String toTEI(int n, int indent, GrobidAnalysisConfig config) {
        StringBuilder tei = new StringBuilder();
        boolean generateIDs = config.isGenerateTeiIds();
        try {
            // we just produce here xml strings
            for (int i = 0; i < indent; i++) {
                tei.append("\t");
            }
            tei.append("<biblStruct");
            boolean withCoords = (config.getGenerateTeiCoordinates() != null) && (config.getGenerateTeiCoordinates().contains("biblStruct"));
            tei.append(" ");
            if (withCoords)
                tei.append(TEIFormatter.getCoordsAttribute(coordinates, withCoords)).append(" ");

            tei.append("status=\"" + getStatus() + "\" ").append(" ");

            if (!StringUtils.isEmpty(language)) {
                if (n == -1) {
                    tei.append("xml:lang=\"" + language + ">\n");
                } else {
                    teiId = "b" + n;
                    tei.append("xml:lang=\"" + language + "\" xml:id=\"" + teiId + "\">\n");
                }
                // TBD: we need to ensure that the language is normalized following xml lang attributes !
            } else {
                if (n == -1) {
                    tei.append(">\n");
                } else {
                    teiId = "b" + n;
                    tei.append("xml:id=\"" + teiId + "\">\n");
                }
            }

            boolean openAnalytic = false;
            if ( ((bookTitle == null) && (journal == null) && (serieTitle == null)) || 
                ((bookTitle != null) && (title == null) && (articleTitle == null) && (journal == null) && (serieTitle == null)) ) {
                for (int i = 0; i < indent + 1; i++) {
                    tei.append("\t");
                }
                tei.append("<monogr>\n");
            } else {
                for (int i = 0; i < indent + 1; i++) {
                    tei.append("\t");
                }
                tei.append("<analytic>\n");
                openAnalytic = true;
            }

            // title
            if (title != null) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<title");
                if ((bookTitle == null) && (journal == null) && (serieTitle == null)) {
                    tei.append(" level=\"m\" type=\"main\"");
                    if (config.isGenerateTeiCoordinates("title")) {
                        // title for articles or chapters
                        List<LayoutToken> titleTokens = getLayoutTokens(TaggingLabels.CITATION_BOOKTITLE);
                        if (titleTokens == null || titleTokens.size()==0) {
                            titleTokens = getLayoutTokens(TaggingLabels.CITATION_TITLE);
                        }

                        if (titleTokens != null && titleTokens.size()>0) {
                            String coords = LayoutTokensUtil.getCoordsString(titleTokens);
                            if (coords != null && coords.length()>0) {
                                tei.append(" coords=\"" + coords + "\"");
                            }
                        } 
                    }
                } else {
                    tei.append(" level=\"a\" type=\"main\"");

                    if (config.isGenerateTeiCoordinates("title")) {
                        // title for articles or chapters
                        List<LayoutToken> titleTokens = getLayoutTokens(TaggingLabels.CITATION_TITLE);
                        if (titleTokens != null && titleTokens.size()>0) {
                            String coords = LayoutTokensUtil.getCoordsString(titleTokens);
                            if (coords != null && coords.length()>0) {
                                tei.append(" coords=\"" + coords + "\"");
                            }
                        } 
                    }
                }
				if (generateIDs) {
					String divID = KeyGen.getKey().substring(0,7);
					tei.append(" xml:id=\"_" + divID + "\"");
				}

                // here check the language ?
                if (StringUtils.isEmpty(english_title)) {
                    tei.append(">").append(TextUtilities.HTMLEncode(title)).append("</title>\n");
                } else {
                    tei.append(" xml:lang=\"").append(language)
						.append("\">").append(TextUtilities.HTMLEncode(title)).append("</title>\n");
                }
            }
			else if (bookTitle == null) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<title/>\n");
			}
            boolean hasEnglishTitle = false;
            if (english_title != null) {
                // here do check the language !
                Language resLang = languageUtilities.runLanguageId(english_title);
                if (resLang != null) {
                    String resL = resLang.getLang();
                    if (resL.equals(Language.EN)) {
                        hasEnglishTitle = true;
                        for (int i = 0; i < indent + 2; i++) {
                            tei.append("\t");
                        }
                        tei.append("<title");
                        if ((bookTitle == null) && (journal == null)) {
                            tei.append(" level=\"m\"");
                        } else {
                            tei.append(" level=\"a\"");
                        }
						if (generateIDs) {
							String divID = KeyGen.getKey().substring(0,7);
							tei.append(" xml:id=\"_" + divID + "\"");
						}

                        tei.append(" xml:lang=\"en\">")
							.append(TextUtilities.HTMLEncode(english_title)).append("</title>\n");
                    }
                }
                // if it's not something in English, we will write it anyway as note without type at the end
            }

            tei.append(toTEIAuthorBlock(2, config));

            if (!StringUtils.isEmpty(doi)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<idno type=\"DOI\">" + TextUtilities.HTMLEncode(doi) + "</idno>\n");
            }

            if (!StringUtils.isEmpty(halId)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<idno type=\"HALid\">" + TextUtilities.HTMLEncode(halId) + "</idno>\n");
            }

            if (!StringUtils.isEmpty(arXivId)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<idno type=\"arXiv\">" + TextUtilities.HTMLEncode(arXivId) + "</idno>\n");
            }

            if (!StringUtils.isEmpty(PMID)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<idno type=\"PMID\">" + TextUtilities.HTMLEncode(PMID) + "</idno>\n");
            }

            if (!StringUtils.isEmpty(PMCID)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<idno type=\"PMCID\">" + TextUtilities.HTMLEncode(PMCID) + "</idno>\n");
            }

            if (!StringUtils.isEmpty(PII)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<idno type=\"PII\">" + TextUtilities.HTMLEncode(PII) + "</idno>\n");
            }

            if (!StringUtils.isEmpty(ark)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<idno type=\"ark\">" + TextUtilities.HTMLEncode(ark) + "</idno>\n");
            }

            if (!StringUtils.isEmpty(istexId)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<idno type=\"istexId\">" + TextUtilities.HTMLEncode(istexId) + "</idno>\n");
            }

            if (!StringUtils.isEmpty(pubnum)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<idno>").append(TextUtilities.HTMLEncode(pubnum)).append("</idno>\n");
            }

            if (!StringUtils.isEmpty(oaUrl)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<ptr type=\"open-access\" target=\"").append(TextUtilities.HTMLEncode(oaUrl)).append("\" />\n");
            }

            if (!StringUtils.isEmpty(web)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<ptr target=\"").append(TextUtilities.HTMLEncode(web)).append("\" />\n");
            }

            if (openAnalytic) {
                for (int i = 0; i < indent + 1; i++) {
                    tei.append("\t");
                }
                tei.append("</analytic>\n");
                for (int i = 0; i < indent + 1; i++) {
                    tei.append("\t");
                }
                tei.append("<monogr>\n");
            }

            if (bookTitle != null) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<title level=\"m\"");
				if (generateIDs) {
					String divID = KeyGen.getKey().substring(0,7);
					tei.append(" xml:id=\"_" + divID + "\"");
				}
                if (config.isGenerateTeiCoordinates("title")) {
                    List<LayoutToken> titleTokens = getLayoutTokens(TaggingLabels.CITATION_BOOKTITLE);
                    if (titleTokens != null && titleTokens.size()>0) {
                        String coords = LayoutTokensUtil.getCoordsString(titleTokens);
                        if (coords != null && coords.length()>0) {
                            tei.append(" coords=\"" + coords + "\"");
                        }
                    } 
                }

				tei.append(">" + TextUtilities.HTMLEncode(bookTitle) + "</title>\n");

                if (!StringUtils.isEmpty(serieTitle)) {
                    // in case the book is part of an indicated series
                    for (int i = 0; i < indent + 2; i++) {
                        tei.append("\t");
                    }
                    tei.append("<title level=\"s\"");
                    if (generateIDs) {
                        String divID = KeyGen.getKey().substring(0,7);
                        tei.append(" xml:id=\"_" + divID + "\"");
                    }   

                    if (config.isGenerateTeiCoordinates("title")) {
                        // title for articles or chapters
                        List<LayoutToken> titleTokens = getLayoutTokens(TaggingLabels.CITATION_SERIES);
                        if (titleTokens != null && titleTokens.size()>0) {
                            String coords = LayoutTokensUtil.getCoordsString(titleTokens);
                            if (coords != null && coords.length()>0) {
                                tei.append(" coords=\"" + coords + "\"");
                            }
                        } 
                    }

                    tei.append(">" + TextUtilities.HTMLEncode(serieTitle) + "</title>\n");
                }

                if (fullEditors != null && fullEditors.size()>0) {
                    for(Person editor : fullEditors) {
                        String localString = editor.toTEI(false);
                        if (localString == null || localString.length() == 0)
                            continue;

                        for (int i = 0; i < indent + 2; i++) {
                            tei.append("\t");
                        }
                        tei.append("<editor>\n");
                        for (int i = 0; i < indent + 3; i++) {
                            tei.append("\t");
                        }
                        
                        localString = localString.replace(" xmlns=\"http://www.tei-c.org/ns/1.0\"", "");
                        tei.append(localString).append("\n");
                        for (int i = 0; i < indent + 2; i++) {
                            tei.append("\t");
                        }
                        tei.append("</editor>\n");
                    }
                } else if (!StringUtils.isEmpty(editors)) {
                    //postProcessingEditors();

                    StringTokenizer st = new StringTokenizer(editors, ";");
                    if (st.countTokens() > 0) {
                        while (st.hasMoreTokens()) {
                            String editor = st.nextToken();
                            if (editor != null)
                                editor = editor.trim();
                            for (int i = 0; i < indent + 2; i++) {
                                tei.append("\t");
                            }
                            tei.append("<editor>" + TextUtilities.HTMLEncode(editor) + "</editor>\n");
                        }
                    } else {
                        if (editors != null)
                            for (int i = 0; i < indent + 2; i++) {
                                tei.append("\t");
                            }
                        tei.append("<editor>" + TextUtilities.HTMLEncode(editors) + "</editor>\n");
                    }
                }

                // in case the booktitle corresponds to a proceedings, we can try to indidate the meeting title
                String meeting = bookTitle;
                boolean meetLoc = false;
                if (event != null)
                    meeting = event;
                else {
                    meeting = meeting.trim();
                    for (String prefix : confPrefixes) {
                        if (meeting.startsWith(prefix)) {
                            meeting = meeting.replace(prefix, "");
                            meeting = meeting.trim();
                            meeting = TextUtilities.cleanField(meeting, false);
                            for (int i = 0; i < indent + 2; i++) {
                                tei.append("\t");
                            }
                            tei.append("<meeting>" + TextUtilities.HTMLEncode(meeting));
                            if ((location != null) || (town != null) || (country != null)) {
                                tei.append("<address>");
                                if (town != null) {
                                    tei.append("<settlement>" + TextUtilities.HTMLEncode(town) + "</settlement>");
                                }
                                if (country != null) {
                                    tei.append("<country>" + TextUtilities.HTMLEncode(country) + "</country>");
                                }
                                if ((location != null) && (town == null) && (country == null)) {
                                    tei.append("<addrLine>" + TextUtilities.HTMLEncode(location) + "</addrLine>");
                                }
                                tei.append("</address>");
                                meetLoc = true;
                            }
                            tei.append("</meeting>\n");
                            break;
                        }
                        //break;
                    }
                }

                if (((location != null) || (town != null) || (country != null)) && (!meetLoc)) {
                    for (int i = 0; i < indent + 2; i++) {
                        tei.append("\t");
                    }
                    tei.append("<meeting>");
                    tei.append("<address>");
                    if (town != null) {
                        tei.append("<settlement>" + town + "</settlement>");
                    }
                    if (country != null) {
                        tei.append("<country>" + country + "</country>");
                    }
                    if ((location != null) && (town == null) && (country == null)) {
                        tei.append("<addrLine>" + TextUtilities.HTMLEncode(location) + "</addrLine>");
                    }
                    tei.append("</address>");
                    tei.append("</meeting>\n");
                }

                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                if ((publication_date != null) || (pageRange != null) || (publisher != null) || (volumeBlock != null)) {   
                    tei.append("<imprint>\n");
                }
				else 
					tei.append("<imprint/>\n");
				
                if (publisher != null) {
                    for (int i = 0; i < indent + 3; i++) {
                        tei.append("\t");
                    }
                    tei.append("<publisher>" + TextUtilities.HTMLEncode(publisher) + "</publisher>\n");
                }

                if (normalized_publication_date != null) {
                    if (normalized_publication_date.getYear() != -1) {
                        String when = Date.toISOString(normalized_publication_date);
                        if (when != null) {
	                        for (int i = 0; i < indent + 3; i++) {
	                            tei.append("\t");
	                        }
	                        tei.append("<date type=\"published\" when=\"");
	                        tei.append(when + "\"");

                            if (publication_date != null && publication_date.length() > 0) {
                                tei.append(">");
                                tei.append(TextUtilities.HTMLEncode(publication_date) );
                                tei.append("</date>\n");
                            } else {
                                tei.append(" />\n");
                            }
						}
                    } else if (this.getYear() != null) {
						String when = "";
						if (this.getYear().length() == 1)
							when += "000" + this.getYear();
						else if (this.getYear().length() == 2)
							when += "00" + this.getYear();
						else if (this.getYear().length() == 3)
							when += "0" + this.getYear();
						else if (this.getYear().length() == 4)
							when += this.getYear();
				
		                if (this.getMonth() != null) {
							if (this.getMonth().length() == 1)
								when += "-0" + this.getMonth();
							else
								when += "-" + this.getMonth();
		                    if (this.getDay() != null) {
								if (this.getDay().length() == 1)
									when += "-0" + this.getDay();
								else
									when += "-" + this.getDay();
		                    }
		                }
                        for (int i = 0; i < indent + 3; i++) {
                            tei.append("\t");
                        }
                        tei.append("<date type=\"published\" when=\"");
                        tei.append(when + "\"");

                        if (publication_date != null && publication_date.length() > 0) {
                            tei.append(">");
                            tei.append(TextUtilities.HTMLEncode(publication_date) );
                            tei.append("</date>\n");
                        } else {
                            tei.append(" />\n");
                        }
                    } else {
                        for (int i = 0; i < indent + 3; i++) {
                            tei.append("\t");
                        }
                        tei.append("<date>" + TextUtilities.HTMLEncode(publication_date) + "</date>\n");
                    }
                } else if (publication_date != null) {
                    for (int i = 0; i < indent + 3; i++) {
                        tei.append("\t");
                    }
                    tei.append("<date>" + TextUtilities.HTMLEncode(publication_date) + "</date>\n");
                }

                if (volumeBlock != null) {
                    for (int i = 0; i < indent + 3; i++) {
                        tei.append("\t");
                    }
                    tei.append("<biblScope unit=\"volume\">" + TextUtilities.HTMLEncode(volumeBlock) + "</biblScope>\n");
                }

                if (!StringUtils.isEmpty(pageRange)) {
                    StringTokenizer st = new StringTokenizer(pageRange, "--");
                    if (st.countTokens() == 2) {
                        for (int i = 0; i < indent + 3; i++) {
                            tei.append("\t");
                        }
						tei.append("<biblScope unit=\"page\" from=\"" + TextUtilities.HTMLEncode(st.nextToken()) + "\" to=\"" 
								+ TextUtilities.HTMLEncode(st.nextToken()) + "\" />\n");  
                    } else {
                        for (int i = 0; i < indent + 3; i++) {
                            tei.append("\t");
                        }
                        tei.append("<biblScope unit=\"page\">" + TextUtilities.HTMLEncode(pageRange) + "</biblScope>\n");
                    }
                }
                if ((publication_date != null) || (pageRange != null) || (publisher != null) || (volumeBlock != null)) {
	                for (int i = 0; i < indent + 2; i++) {
	                    tei.append("\t");
	                }
                    tei.append("</imprint>\n");
                }
            } else if (!StringUtils.isEmpty(journal) || !StringUtils.isEmpty(serieTitle)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                if (!StringUtils.isEmpty(journal)) {
                    tei.append("<title level=\"j\"");
    				if (generateIDs) {
    					String divID = KeyGen.getKey().substring(0,7);
    					tei.append(" xml:id=\"_" + divID + "\"");
    				}	

                    if (config.isGenerateTeiCoordinates("title")) {
                        // title for articles or chapters
                        List<LayoutToken> titleTokens = getLayoutTokens(TaggingLabels.CITATION_JOURNAL);
                        if (titleTokens != null && titleTokens.size()>0) {
                            String coords = LayoutTokensUtil.getCoordsString(titleTokens);
                            if (coords != null && coords.length()>0) {
                                tei.append(" coords=\"" + coords + "\"");
                            }
                        } 
                    }

    				tei.append(">" + TextUtilities.HTMLEncode(journal) + "</title>\n");

                    if (!StringUtils.isEmpty(getJournalAbbrev())) {
                        for (int i = 0; i < indent + 2; i++) {
                            tei.append("\t");
                        }
                        tei.append("<title level=\"j\" type=\"abbrev\">"
                                + TextUtilities.HTMLEncode(getJournalAbbrev()) + "</title>\n");
                    }
                } else if (!StringUtils.isEmpty(serieTitle)) {
                    tei.append("<title level=\"s\"");
                    if (generateIDs) {
                        String divID = KeyGen.getKey().substring(0,7);
                        tei.append(" xml:id=\"_" + divID + "\"");
                    }   

                    if (config.isGenerateTeiCoordinates("title")) {
                        // title for articles or chapters
                        List<LayoutToken> titleTokens = getLayoutTokens(TaggingLabels.CITATION_SERIES);
                        if (titleTokens != null && titleTokens.size()>0) {
                            String coords = LayoutTokensUtil.getCoordsString(titleTokens);
                            if (coords != null && coords.length()>0) {
                                tei.append(" coords=\"" + coords + "\"");
                            }
                        } 
                    }

                    tei.append(">" + TextUtilities.HTMLEncode(serieTitle) + "</title>\n");
                }

                if (fullEditors != null && fullEditors.size()>0) {
                    for(Person editor : fullEditors) {
                        for (int i = 0; i < indent + 2; i++) {
                            tei.append("\t");
                        }
                        tei.append("<editor>\n");
                        for (int i = 0; i < indent + 3; i++) {
                            tei.append("\t");
                        }
                        String localString = editor.toTEI(false);
                        localString = localString.replace(" xmlns=\"http://www.tei-c.org/ns/1.0\"", "");
                        tei.append(localString).append("\n");
                        for (int i = 0; i < indent + 2; i++) {
                            tei.append("\t");
                        }
                        tei.append("</editor>\n");
                    }
                } else if (!StringUtils.isEmpty(editors)) {
                    //postProcessingEditors();

                    StringTokenizer st = new StringTokenizer(editors, ";");
                    if (st.countTokens() > 0) {
                        while (st.hasMoreTokens()) {
                            String editor = st.nextToken();
                            if (editor != null) {
                                for (int i = 0; i < indent + 2; i++) {
                                    tei.append("\t");
                                }
                                editor = editor.trim();
                                tei.append("<editor>" + TextUtilities.HTMLEncode(editor) + "</editor>\n");
                            }
                        }
                    } else {
                        if (!StringUtils.isEmpty(editors)) {
                            for (int i = 0; i < indent + 2; i++) {
                                tei.append("\t");
                            }
                            tei.append("<editor>" + TextUtilities.HTMLEncode(editors) + "</editor>\n");
                        }
                    }
                }

                if (!StringUtils.isEmpty(getISSN())) {
                    for (int i = 0; i < indent + 2; i++) {
                        tei.append("\t");
                    }
                    tei.append("<idno type=\"ISSN\">" + TextUtilities.HTMLEncode(getISSN()) + "</idno>\n");
                }

                if (!StringUtils.isEmpty(getISSNe())) {
                    if (!getISSNe().equals(getISSN())) {
                        for (int i = 0; i < indent + 2; i++) {
                            tei.append("\t");
                        }
                        tei.append("<idno type=\"ISSNe\">" + TextUtilities.HTMLEncode(getISSNe()) + "</idno>\n");
                    }
                }

                if ((volumeBlock != null) | (issue != null) || (pageRange != null) || (publication_date != null)
                        || (publisher != null)) {
                    for (int i = 0; i < indent + 2; i++) {
                        tei.append("\t");
                    }
					tei.append("<imprint>\n");
                    if (volumeBlock != null) {
                        for (int i = 0; i < indent + 3; i++) {
                            tei.append("\t");
                        }
                        tei.append("<biblScope unit=\"volume\">" + TextUtilities.HTMLEncode(volumeBlock) + "</biblScope>\n");
                    }
                    if (issue != null) {
                        for (int i = 0; i < indent + 3; i++) {
                            tei.append("\t");
                        }
                        tei.append("<biblScope unit=\"issue\">" + TextUtilities.HTMLEncode(issue) + "</biblScope>\n");
                    }
                    if (pageRange != null) {
                        StringTokenizer st = new StringTokenizer(pageRange, "--");
                        if (st.countTokens() == 2) {
                            for (int i = 0; i < indent + 3; i++) {
                                tei.append("\t");
                            }
							tei.append("<biblScope unit=\"page\" from=\"" + 
								TextUtilities.HTMLEncode(st.nextToken()) + "\" to=\"" + 
								TextUtilities.HTMLEncode(st.nextToken()) + "\" />\n");
                        } else {
                            for (int i = 0; i < indent + 3; i++) {
                                tei.append("\t");
                            }
                            tei.append("<biblScope unit=\"page\">" + TextUtilities.HTMLEncode(pageRange) + "</biblScope>\n");
                        }
                    }

                    // date
                    if (normalized_publication_date != null) {
                        if (normalized_publication_date.getYear() != -1) {
                            String when = Date.toISOString(normalized_publication_date);
                            if (when != null) {
	                            for (int i = 0; i < indent + 3; i++) {
	                                tei.append("\t");
	                            }
	                            tei.append("<date type=\"published\" when=\"");
                                tei.append(when + "\"");

                                if (publication_date != null && publication_date.length() > 0) {
                                    tei.append(">");
                                    tei.append(TextUtilities.HTMLEncode(publication_date) );
                                    tei.append("</date>\n");
                                } else {
                                    tei.append(" />\n");
                                }
							}
                        } else if (this.getYear() != null) {
							String when = "";
							if (this.getYear().length() == 1)
								when += "000" + this.getYear();
							else if (this.getYear().length() == 2)
								when += "00" + this.getYear();
							else if (this.getYear().length() == 3)
								when += "0" + this.getYear();
							else if (this.getYear().length() == 4)
								when += this.getYear();
				
			                if (this.getMonth() != null) {
								if (this.getMonth().length() == 1)
									when += "-0" + this.getMonth();
								else
									when += "-" + this.getMonth();
			                    if (this.getDay() != null) {
									if (this.getDay().length() == 1)
										when += "-0" + this.getDay();
									else
										when += "-" + this.getDay();
			                    }
			                }
                            for (int i = 0; i < indent + 3; i++) {
                                tei.append("\t");
                            }
                            tei.append("<date type=\"published\" when=\"");
                            tei.append(when + "\"");

                            if (publication_date != null && publication_date.length() > 0) {
                                tei.append(">");
                                tei.append(TextUtilities.HTMLEncode(publication_date) );
                                tei.append("</date>\n");
                            } else {
                                tei.append(" />\n");
                            }
                        } else {
                            for (int i = 0; i < indent + 3; i++) {
                                tei.append("\t");
                            }
                            tei.append("<date>" + TextUtilities.HTMLEncode(publication_date) + "</date>\n");
                        }
                    } else if (publication_date != null) {
                        for (int i = 0; i < indent + 3; i++) {
                            tei.append("\t");
                        }
                        tei.append("<date>" + TextUtilities.HTMLEncode(publication_date) + "</date>\n");
                    }

                    if (getPublisher() != null) {
                        for (int i = 0; i < indent + 3; i++) {
                            tei.append("\t");
                        }
                        tei.append("<publisher>" + TextUtilities.HTMLEncode(getPublisher()) + "</publisher>\n");
                    }

                    if (location != null && location.length()>0) {
                        for (int i = 0; i < indent + 3; i++) {
                            tei.append("\t");
                        }
                        tei.append("<pubPlace>" + TextUtilities.HTMLEncode(location) + "</pubPlace>\n");
                    }

                    for (int i = 0; i < indent + 2; i++) {
                        tei.append("\t");
                    }
                    tei.append("</imprint>\n");
                }
				else {
                    for (int i = 0; i < indent + 2; i++) {
                        tei.append("\t");
                    }
					tei.append("<imprint/>\n");
				}
            } else {
                // not a journal and not something in a book...
                if (editors != null) {
                    //postProcessingEditors();

                    StringTokenizer st = new StringTokenizer(editors, ";");
                    if (st.countTokens() > 0) {
                        while (st.hasMoreTokens()) {
                            String editor = st.nextToken();
                            if (editor != null) {
                                editor = editor.trim();
                                for (int i = 0; i < indent + 2; i++) {
                                    tei.append("\t");
                                }
                                tei.append("<editor>" + TextUtilities.HTMLEncode(editor) + "</editor>\n");
                            }
                        }
                    } else {
                        if (editors != null) {
                            for (int i = 0; i < indent + 2; i++) {
                                tei.append("\t");
                            }
                            tei.append("<editor>" + TextUtilities.HTMLEncode(editors) + "</editor>\n");
                        }
                    }
                }

                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                if (normalized_publication_date != null ||
                    publication_date != null || 
                    pageRange != null || 
                    location != null || 
                    publisher != null || 
                    volumeBlock != null) {
                    tei.append("<imprint>\n");
                }
				else {
					tei.append("<imprint/>\n");
				}
                // date
                if (normalized_publication_date != null) {
                    if (normalized_publication_date.getYear() != -1) {
                        String when = Date.toISOString(normalized_publication_date);
                        if (when != null) {                        
	                        for (int i = 0; i < indent + 3; i++) {
	                            tei.append("\t");
	                        }
	                        tei.append("<date type=\"published\" when=\"");
	                        tei.append(when + "\"");

                            if (publication_date != null && publication_date.length() > 0) {
                                tei.append(">");
                                tei.append(TextUtilities.HTMLEncode(publication_date) );
                                tei.append("</date>\n");
                            } else {
                                tei.append(" />\n");
                            }
						}
                    } else if (this.getYear() != null) {
						String when = "";
						if (this.getYear().length() == 1)
							when += "000" + this.getYear();
						else if (this.getYear().length() == 2)
							when += "00" + this.getYear();
						else if (this.getYear().length() == 3)
							when += "0" + this.getYear();
						else if (this.getYear().length() == 4)
							when += this.getYear();
				
		                if (this.getMonth() != null) {
							if (this.getMonth().length() == 1)
								when += "-0" + this.getMonth();
							else
								when += "-" + this.getMonth();
		                    if (this.getDay() != null) {
								if (this.getDay().length() == 1)
									when += "-0" + this.getDay();
								else
									when += "-" + this.getDay();
		                    }
		                }
                        for (int i = 0; i < indent + 3; i++) {
                            tei.append("\t");
                        }
                        tei.append("<date type=\"published\" when=\"");
                        tei.append(when + "\"");

                        if (publication_date != null && publication_date.length() > 0) {
                            tei.append(">");
                            tei.append(TextUtilities.HTMLEncode(publication_date) );
                            tei.append("</date>\n");
                        } else {
                            tei.append(" />\n");
                        }
                    } else {
                        for (int i = 0; i < indent + 3; i++) {
                            tei.append("\t");
                        }
                        tei.append("<date>" + TextUtilities.HTMLEncode(publication_date) + "</date>\n");
                    }
                } else if (publication_date != null) {
                    for (int i = 0; i < indent + 3; i++) {
                        tei.append("\t");
                    }
                    tei.append("<date>" + TextUtilities.HTMLEncode(publication_date) + "</date>\n");
                }

                if (publisher != null) {
                    for (int i = 0; i < indent + 3; i++) {
                        tei.append("\t");
                    }
                    tei.append("<publisher>" + TextUtilities.HTMLEncode(publisher) + "</publisher>\n");
                }
                if (volumeBlock != null) {
                    for (int i = 0; i < indent + 3; i++) {
                        tei.append("\t");
                    }
                    tei.append("<biblScope unit=\"volume\">" + TextUtilities.HTMLEncode(volumeBlock) + "</biblScope>\n");
                }
                if (pageRange != null) {
                    StringTokenizer st = new StringTokenizer(pageRange, "--");
                    if (st.countTokens() == 2) {
                        for (int i = 0; i < indent + 3; i++) {
                            tei.append("\t");
                        }
						tei.append("<biblScope unit=\"page\" from=\"" + 
							TextUtilities.HTMLEncode(st.nextToken()) + 
								"\" to=\"" + TextUtilities.HTMLEncode(st.nextToken()) + "\" />\n");   	
                    } else {
                        for (int i = 0; i < indent + 3; i++) {
                            tei.append("\t");
                        }
                        tei.append("<biblScope unit=\"page\">" + TextUtilities.HTMLEncode(pageRange) + "</biblScope>\n");
                    }
                }
                if (location != null) {
                    for (int i = 0; i < indent + 3; i++) {
                        tei.append("\t");
                    }
                    tei.append("<pubPlace>" + TextUtilities.HTMLEncode(location) + "</pubPlace>\n");
                }

                if ((publication_date != null) || (pageRange != null) || (location != null) || (publisher != null) || (volumeBlock != null)) {
                    for (int i = 0; i < indent + 2; i++) {
                        tei.append("\t");
                    }
                    tei.append("</imprint>\n");
                }
            }

            if (!StringUtils.isEmpty(institution)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<respStmt>\n");
                for (int i = 0; i < indent + 3; i++) {
                    tei.append("\t");
                }
                tei.append("<orgName>" + TextUtilities.HTMLEncode(institution) + "</orgName>\n");
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("</respStmt>\n");
            }

            for (int i = 0; i < indent + 1; i++) {
                tei.append("\t");
            }
            tei.append("</monogr>\n");

            if (submission != null) {
                for (int i = 0; i < indent + 1; i++) {
                    tei.append("\t");
                }
                tei.append("<note type=\"submission\">" + TextUtilities.HTMLEncode(submission) + "</note>\n");
            }
            if (getSubmissionDate() != null) {
                for (int i = 0; i < indent + 1; i++) {
                    tei.append("\t");
                }
                tei.append("<date type=\"submission\">" + TextUtilities.HTMLEncode(getSubmissionDate()) + "</date>\n");
            }
            if (getDownloadDate() != null) {
                for (int i = 0; i < indent + 1; i++) {
                    tei.append("\t");
                }
                tei.append("<date type=\"download\">" + TextUtilities.HTMLEncode(getDownloadDate()) + "</date>\n");
            }

            if (dedication != null) {
                for (int i = 0; i < indent + 1; i++) {
                    tei.append("\t");
                }
                tei.append("<note type=\"dedication\">" + TextUtilities.HTMLEncode(dedication) + "</note>\n");
            }

            if (book_type != null) {
                for (int i = 0; i < indent + 1; i++) {
                    tei.append("\t");
                }
                tei.append("<note type=\"report_type\">" + TextUtilities.HTMLEncode(book_type) + "</note>\n");
            }

            if (note != null) {      
                for (int i = 0; i < indent + 1; i++) {
                    tei.append("\t");
                }
                tei.append("<note>" + TextUtilities.HTMLEncode(note) + "</note>\n");
            }

            if ((english_title != null) && (!hasEnglishTitle)) {
                for (int i = 0; i < indent + 1; i++) {
                    tei.append("\t");
                }
                tei.append("<note>" + TextUtilities.HTMLEncode(english_title) + "</note>\n");
            }

            if (subjects != null) {
                if (subjects.size() > 0) {
                    for (int i = 0; i < indent + 1; i++) {
                        tei.append("\t");
                    }
                    tei.append("<keywords scheme=\"hal\"><list>\n");
                    for (String subject : subjects) {
                        for (int i = 0; i < indent + 2; i++) {
                            tei.append("\t");
                        }
                        tei.append("<item>" + TextUtilities.HTMLEncode(subject) + "</item>\n");
                    }
                    tei.append("</list></keywords>\n");
                }
            }

            // keywords here !!
            if (!StringUtils.isEmpty(getKeyword())) {
                String keywords = getKeyword();
                if (keywords.startsWith("Categories and Subject Descriptors")) {
                    int start = keywords.indexOf("Keywords");
                    if (start != -1) {
                        String keywords1 = keywords.substring(0, start - 1);
                        String keywords2 = keywords.substring(start + 9, keywords.length());
                        for (int i = 0; i < indent + 1; i++) {
                            tei.append("\t");
                        }
                        tei.append("<keywords type=\"subject-headers\">" + TextUtilities.HTMLEncode(keywords1) + "</keywords>\n");
                        for (int i = 0; i < indent + 1; i++) {
                            tei.append("\t");
                        }
                        tei.append("<keywords>" + TextUtilities.HTMLEncode(keywords2) + "</keywords>\n");
                    } else {
                        for (int i = 0; i < indent + 1; i++) {
                            tei.append("\t");
                        }
                        tei.append("<keywords>" + TextUtilities.HTMLEncode(getKeyword()) + "</keywords>\n");
                    }
                } else
                    for (int i = 0; i < indent + 1; i++) {
                        tei.append("\t");
                    }
                tei.append("<keywords>" + TextUtilities.HTMLEncode(getKeyword()) + "</keywords>\n");
            }

            if (uri != null) {
                /*if (uri.startsWith("http://hal.") || ) {
                    for (int i = 0; i < indent + 1; i++) {
                        tei.append("\t");
                    }
                    tei.append("<idno type=\"HALid\">" + TextUtilities.HTMLEncode(uri) + "</idno>\n");
                } else */
                {
                    for (int i = 0; i < indent + 1; i++) {
                        tei.append("\t");
                    }
                    tei.append("<idno>" + TextUtilities.HTMLEncode(uri) + "</idno>\n");
                }
            }

            if (url != null) {
                if (url.startsWith("http://hal.") || url.startsWith("https://hal.")) {
                    for (int i = 0; i < indent + 1; i++) {
                        tei.append("\t");
                    }
                    tei.append("<idno type=\"HALFile\">" + TextUtilities.HTMLEncode(url) + "</idno>\n");
                }
            }

            if (abstract_ != null) {
                if (abstract_.length() > 0) {
                    for (int i = 0; i < indent + 1; i++) {
                        tei.append("\t");
                    }
                    tei.append("<div type=\"abstract\">" + TextUtilities.HTMLEncode(abstract_) + "</div>\n");
                }
            }

            if (config.getIncludeRawCitations() && !StringUtils.isEmpty(reference) ) {
                for (int i = 0; i < indent + 1; i++) {
                    tei.append("\t");
                }
                String localReference = TextUtilities.HTMLEncode(reference);
                localReference = localReference.replace("\n", " ");
                localReference = localReference.replaceAll("( )+", " ");
                tei.append("<note type=\"raw_reference\">" + localReference + "</note>\n");
            }

            for (int i = 0; i < indent; i++) {
                tei.append("\t");
            }
            tei.append("</biblStruct>\n");
        } catch (Exception e) {
            throw new GrobidException("Cannot convert  bibliographical item into a TEI, " +
                    "because of nested exception.", e);
        }

        return tei.toString();
    }

    /**
     * Export the bibliographical item into OpenURL 1.0.
     */
    public String toOpenURL(String authors) {
        String openurl = "";

        try {
            // general - independent from the type of bibliographical object
            //openurl += "url_ver=Z39.88-2004";
            openurl += "ctx_ver=Z39.88-2004";

            if (doi != null) {
                //openurl += "&rft.doi=" + HTMLEncode(DOI);
                openurl += "&rft_id=info:doi/" + URLEncoder.encode(doi, "UTF-8");
                //openurl += "&rft.doi=" + URLEncoder.encode(DOI,"UTF-8");
                // we can finish here
                openurl += "&url_ctx_fmt=info:ofi/fmt:kev:mtx:ctx&rft.genre=article ";
                return openurl;
            }

            // journal
            if ((bookTitle != null) || (journal != null)) {
                if (journal != null)
                    openurl += "&rft_val_fmt=info:ofi/fmt:kev:mtx:journal";
                openurl += "&rft.genre=article"; // ? always to be written ?
                if (ISSN != null)
                    openurl += "&rft.issn=" + URLEncoder.encode(ISSN, "UTF-8");
                if (title != null)
                    openurl += "&rft.atitle=" + URLEncoder.encode(title, "UTF-8");
                if (journal != null)
                    openurl += "&rft.jtitle=" + URLEncoder.encode(journal, "UTF-8");
                else if (bookTitle != null)
                    openurl += "&rft.btitle=" + URLEncoder.encode(bookTitle, "UTF-8");
                if (volumeBlock != null)
                    openurl += "&rft.volume=" + URLEncoder.encode(volumeBlock, "UTF-8");
                if (issue != null)
                    openurl += "&rft.issue=" + URLEncoder.encode(issue, "UTF-8");

                if (pageRange != null) {
                    StringTokenizer st = new StringTokenizer(pageRange, "--");
                    if (st.countTokens() > 0) {
                        if (st.hasMoreTokens()) {
                            String spage = st.nextToken();
                            openurl += "&rft.spage=" + URLEncoder.encode(spage, "UTF-8");
                        }
                        if (st.hasMoreTokens()) {
                            String epage = st.nextToken();
                            openurl += "&rft.epage=" + URLEncoder.encode(epage, "UTF-8");
                        }
                    }
                }
            } else {
                // book 	    
                openurl += "&rft_val_fmt=info:ofi/fmt:kev:mtx:book";
                if (ISBN13 != null)
                    openurl += "&rft.isbn=" + URLEncoder.encode(ISBN13, "UTF-8");
                if (title != null)
                    openurl += "&rft.genre=book&rft.btitle=" + URLEncoder.encode(title, "UTF-8");
            }

            if (publication_date != null)
                openurl += "&rft.date=" + URLEncoder.encode(publication_date, "UTF-8"); // year is enough!

            // authors
            if (authors != null) {
                String localAuthor = getFirstAuthorSurname();
                if (localAuthor != null) {
                    openurl += "&rft.aulast=" + URLEncoder.encode(localAuthor, "UTF-8");
                }
            }

            openurl += "&url_ctx_fmt=info:ofi/fmt:kev:mtx:ctx";
        } catch (Exception e) {
            throw new GrobidException("Cannot open url to DOI, because of nested exception.", e);
        }
        return openurl;
    }


    /**
     * Export the bibliographical item into a COinS (OpenURL ContextObject in SPAN).
     */
    public String toCOinS() {
        String res = "<span class=\"Z3988\" title=\"" + toOpenURL(authors) + "\"></span>";
        return res;
    }


    /**
     * Export the bibliographical item into an OpenURL with given link resolver address.
     */
    public String toFullOpenURL(String linkResolver, String imageLinkResolver) {
        String res = "<a href=\"" + linkResolver + toOpenURL(authors)
                + "\"  target=\"_blank\"><img src=\"" + imageLinkResolver + "\"/></a>";
        return res;
    }

    public void setFirstAuthorSurname(String firstAuthorSurname) {
        this.firstAuthorSurname = firstAuthorSurname;
    }

    /**
     * Return the surname of the first author.
     */
    public String getFirstAuthorSurname() {
        if (this.firstAuthorSurname != null) {
            return this.firstAuthorSurname;
            //return TextUtilities.HTMLEncode(this.firstAuthorSurname);
        }

        if (fullAuthors != null) {
            if (fullAuthors.size() > 0) {
                Person aut = fullAuthors.get(0);
                String sur = aut.getLastName();
                if (sur != null) {
                    if (sur.length() > 0) {
                        this.firstAuthorSurname = sur;
                        //return TextUtilities.HTMLEncode(sur);
                        return sur;
                    }
                }
            }
        }

        if (authors != null) {
            StringTokenizer st = new StringTokenizer(authors, ";");
            if (st.countTokens() > 0) {
                if (st.hasMoreTokens()) { // we take just the first author
                    String author = st.nextToken();
                    if (author != null)
                        author = author.trim();
                    int ind = author.lastIndexOf(" ");
                    if (ind != -1) {
                        this.firstAuthorSurname = author.substring(ind + 1);
                        //return TextUtilities.HTMLEncode(author.substring(ind + 1));
                        return author.substring(ind + 1);
                    } else {
                        this.firstAuthorSurname = author;
                        //return TextUtilities.HTMLEncode(author);
                        return author;
                    }
                }
            }

        }
        return null;
    }

    /**
     * Attach existing recognized emails to authors (default) or editors
     */
    public void attachEmails() {
        attachEmails(fullAuthors);
    }

    public void attachEmails(List<Person> folks) {
        // do we have an email field recognized? 
        if (email == null)
            return;
        // we check if we have several emails in the field
        email = email.trim();
        email = email.replace(" and ", "\t");
        ArrayList<String> emailles = new ArrayList<String>();
        StringTokenizer st0 = new StringTokenizer(email, "\t");
        while (st0.hasMoreTokens()) {
            emailles.add(st0.nextToken().trim());
        }

        List<String> sanitizedEmails = emailSanitizer.splitAndClean(emailles);

        if (sanitizedEmails != null) {
            authorEmailAssigner.assign(folks, sanitizedEmails);
        }
    }

    /**
     * Attach existing recognized emails to authors
     */
    public void attachAuthorEmails() {
        attachEmails(fullAuthors);
    }

    /**
     * Attach existing recognized emails to editors
     */
    public void attachEditorEmails() {
        attachEmails(fullEditors);
    }

    /**
     * Attach existing recognized affiliations to authors
     */
    public void attachAffiliations() {
        if (fullAffiliations == null) {
            return;
        }

        if (fullAuthors == null) {
            return;
        }
        int nbAffiliations = fullAffiliations.size();
        int nbAuthors = fullAuthors.size();

        boolean hasMarker = false;

        // do we have markers in the affiliations?
        for (Affiliation aff : fullAffiliations) {
            if (aff.getMarker() != null) {
                hasMarker = true;
                break;
            }
        }

        if (nbAffiliations == 1) {
            // we distribute this affiliation to each author
            Affiliation aff = fullAffiliations.get(0);
            for (Person aut : fullAuthors) {
                aut.addAffiliation(aff);
            }
            aff.setFailAffiliation(false);
        } else if ((nbAuthors == 1) && (nbAffiliations > 1)) {
            // we put all the affiliations to the single author
            Person auth = fullAuthors.get(0);
            for (Affiliation aff : fullAffiliations) {
                auth.addAffiliation(aff);
                aff.setFailAffiliation(false);
            }
        } else if (hasMarker) {
            // we get the marker for each affiliation and try to find the related author in the
            // original author field
            int indexAffiliation = 0;
            for (Affiliation aff : fullAffiliations) {

                // circuit breaker
                if (indexAffiliation > 60)
                    break;

                if (aff.getMarker() != null && aff.getMarker().length()>0) {
                    String marker = aff.getMarker();
                    int from = 0;
                    int ind = 0;
                    ArrayList<Integer> winners = new ArrayList<Integer>();
                    while (ind != -1) {
                        ind = originalAuthors.indexOf(marker, from);

                        boolean bad = false;
                        if (ind != -1) {
                            // we check if we have a digit/letter (1) matching incorrectly
                            //  a double digit/letter (11), or a special non-digit (*) matching incorrectly
                            //  a double special non-digit (**)
                            if (marker.length() == 1) {
                                if (Character.isDigit(marker.charAt(0))) {
                                    if (ind - 1 > 0) {
                                        if (Character.isDigit(originalAuthors.charAt(ind - 1))) {
                                            bad = true;
                                        }
                                    }
                                    if (ind + 1 < originalAuthors.length()) {
                                        if (Character.isDigit(originalAuthors.charAt(ind + 1))) {
                                            bad = true;
                                        }
                                    }
                                } else if (Character.isLetter(marker.charAt(0))) {
                                    if (ind - 1 > 0) {
                                        if (Character.isLetter(originalAuthors.charAt(ind - 1))) {
                                            bad = true;
                                        }
                                    }
                                    if (ind + 1 < originalAuthors.length()) {
                                        if (Character.isLetter(originalAuthors.charAt(ind + 1))) {
                                            bad = true;
                                        }
                                    }
                                } else if (marker.charAt(0) == '*') {
                                    if (ind - 1 > 0) {
                                        if (originalAuthors.charAt(ind - 1) == '*') {
                                            bad = true;
                                        }
                                    }
                                    if (ind + 1 < originalAuthors.length()) {
                                        if (originalAuthors.charAt(ind + 1) == '*') {
                                            bad = true;
                                        }
                                    }
                                }
                            }
                            if (marker.length() == 2) {
                                // case with ** as marker
                                if ((marker.charAt(0) == '*') && (marker.charAt(1) == '*')) {
                                    if (ind - 2 > 0) {
                                        if ((originalAuthors.charAt(ind - 1) == '*') &&
                                                (originalAuthors.charAt(ind - 2) == '*')) {
                                            bad = true;
                                        }
                                    }
                                    if (ind + 2 < originalAuthors.length()) {
                                        if ((originalAuthors.charAt(ind + 1) == '*') &&
                                                (originalAuthors.charAt(ind + 2) == '*')) {
                                            bad = true;
                                        }
                                    }
                                    if ((ind - 1 > 0) && (ind + 1 < originalAuthors.length())) {
                                        if ((originalAuthors.charAt(ind - 1) == '*') &&
                                                (originalAuthors.charAt(ind + 1) == '*')) {
                                            bad = true;
                                        }
                                    }
                                }
                            }
                        }

                        if ((ind != -1) && !bad) {
                            // we find the associated author name 
                            String original = originalAuthors.toLowerCase();
                            int p = 0;
                            int best = -1;
                            int ind2 = -1;
                            int bestDistance = 1000;
                            for (Person aut : fullAuthors) {
                                if (!winners.contains(Integer.valueOf(p))) {
                                    String lastname = aut.getLastName();

                                    if (lastname != null) {
                                        lastname = lastname.toLowerCase();
                                        ind2 = original.indexOf(lastname, ind2 + 1);
                                        int dist = Math.abs(ind - (ind2 + lastname.length()));
                                        if (dist < bestDistance) {
                                            best = p;
                                            bestDistance = dist;
                                        }
                                    }
                                }
                                p++;
                            }

                            // and we associate this affiliation to this author
                            if (best != -1) {
                                fullAuthors.get(best).addAffiliation(aff);
                                aff.setFailAffiliation(false);
                                winners.add(Integer.valueOf(best));
                            }

                            from = ind + 1;
                        }
                        if ((ind != -1) && bad) {
                            from = ind + 1;
                            bad = false;
                        }

                        // circuit breaker
                        if (ind > originalAuthors.length() || ind > 1000)
                            break;
                    }
                }
                indexAffiliation++;
            }
        } /*else if (nbAuthors == nbAffiliations) {
            // risky heuristics, we distribute in this case one affiliation per author
            // preserving author 
            // sometimes 2 affiliations belong both to 2 authors, for these case, the layout
            // positioning should be studied
            for (int p = 0; p < nbAuthors; p++) {
                fullAuthors.get(p).addAffiliation(fullAffiliations.get(p));
                System.out.println("attachment: " + p);
                System.out.println(fullAuthors.get(p));
                fullAffiliations.get(p).setFailAffiliation(false);
            }
        }*/
    }

    /**
     * Create the TEI encoding for the author+affiliation block for the current biblio object.
     */
    public String toTEIAuthorBlock(int nbTag) {
        return toTEIAuthorBlock(nbTag, GrobidAnalysisConfig.defaultInstance());
    }

    /**
     * Create the TEI encoding for the author+affiliation block for the current biblio object.
     */
    public String toTEIAuthorBlock(int nbTag, GrobidAnalysisConfig config) {
        StringBuffer tei = new StringBuffer();
        int nbAuthors = 0;
        int nbAffiliations = 0;
        int nbAddresses = 0;

        boolean withCoordinates = false;
        if (config != null && config.getGenerateTeiCoordinates() != null) {
            withCoordinates = config.getGenerateTeiCoordinates().contains("persName");
        }

        if ( (collaboration != null) && 
            ( (fullAuthors == null) || (fullAuthors.size() == 0) ) ) {
            // collaboration plays at the same time the role of author and affiliation
            TextUtilities.appendN(tei, '\t', nbTag);
            tei.append("<author>").append("\n");
            TextUtilities.appendN(tei, '\t', nbTag+1);
            tei.append("<orgName type=\"collaboration\"");
            if (withCoordinates && (labeledTokens != null) ) {
                List<LayoutToken> collabTokens = labeledTokens.get("<collaboration>");
                if (withCoordinates && (collabTokens != null) && (!collabTokens.isEmpty())) {                
                   tei.append(" coords=\"" + LayoutTokensUtil.getCoordsString(collabTokens) + "\"");
                }
            }
            tei.append(">").append(TextUtilities.HTMLEncode(collaboration)).append("</orgName>").append("\n");
            TextUtilities.appendN(tei, '\t', nbTag);
            tei.append("</author>").append("\n");
            return tei.toString();
        }

        List<Person> auts = fullAuthors;

        Lexicon lexicon = Lexicon.getInstance();

        List<Affiliation> affs = fullAffiliations;
        if (affs == null)
            nbAffiliations = 0;
        else
            nbAffiliations = affs.size();

        if (auts == null)
            nbAuthors = 0;
        else
            nbAuthors = auts.size();
        boolean failAffiliation = true;

        //if (getAuthors() != null) {
        if (auts != null) {
            failAffiliation = false;
            if (nbAuthors > 0) {
                int autRank = 0;
                int contactAut = -1;
                //check if we have a single author of contact
                for (Person author : auts) {
                    if (author.getEmail() != null) {
                        if (contactAut == -1)
                            contactAut = autRank;
                        else {
                            contactAut = -1;
                            break;
                        }
                    }
                    autRank++;
                }
                autRank = 0;
                for (Person author : auts) {
                    if (author.getLastName() != null) {
                        if (author.getLastName().length() < 2)
                            continue;
                    }

			        if ( (author.getFirstName() == null) && (author.getMiddleName() == null) &&
			                (author.getLastName() == null) ) {
						continue;
					}

                    TextUtilities.appendN(tei, '\t', nbTag);
                    tei.append("<author");

                    if (autRank == contactAut) {
                        tei.append(" role=\"corresp\">\n");
                    } else
                        tei.append(">\n");

                    TextUtilities.appendN(tei, '\t', nbTag + 1);
                    
                    String localString = author.toTEI(withCoordinates);
                    localString = localString.replace(" xmlns=\"http://www.tei-c.org/ns/1.0\"", "");
                    tei.append(localString).append("\n");
                    if (author.getEmail() != null) {
                        TextUtilities.appendN(tei, '\t', nbTag + 1);
                        tei.append("<email>" + TextUtilities.HTMLEncode(author.getEmail()) + "</email>\n");
                    }
                    if (author.getORCID() != null) {
                        TextUtilities.appendN(tei, '\t', nbTag + 1);
                        tei.append("<idno type=\"ORCID\">" + TextUtilities.HTMLEncode(author.getORCID()) + "</idno>\n");
                    }

                    if (author.getAffiliations() != null) {
                        for (Affiliation aff : author.getAffiliations()) {
                            this.appendAffiliation(tei, nbTag + 1, aff, config, lexicon);
                        }
                    } else if (collaboration != null) {
                        TextUtilities.appendN(tei, '\t', nbTag + 1);
                        tei.append("<affiliation>\n");

                        TextUtilities.appendN(tei, '\t', nbTag + 2);
                        tei.append("<orgName type=\"collaboration\">" +
                                            TextUtilities.HTMLEncode(collaboration) + "</orgName>\n");
                        TextUtilities.appendN(tei, '\t', nbTag + 1);
                        tei.append("</affiliation>\n");
                    }

                    TextUtilities.appendN(tei, '\t', nbTag);
                    tei.append("</author>\n");
                    autRank++;
                }
            }
        }

        // if the affiliations were not outputted with the authors, we add them here 
        // (better than nothing!)		
        if (affs != null) {
            for (Affiliation aff : affs) {
                if (aff.getFailAffiliation()) {
                    // dummy <author> for TEI conformance
                    TextUtilities.appendN(tei, '\t', nbTag);
                    tei.append("<author>\n");
                    this.appendAffiliation(tei, nbTag + 1, aff, config, lexicon);
                    TextUtilities.appendN(tei, '\t', nbTag);
                    tei.append("</author>\n");
                }
            }
        } else if (affiliation != null) {
            StringTokenizer st2 = new StringTokenizer(affiliation, ";");
            int affiliationRank = 0;
            while (st2.hasMoreTokens()) {
                String aff = st2.nextToken();
                TextUtilities.appendN(tei, '\t', nbTag);
                tei.append("<author>\n");
                TextUtilities.appendN(tei, '\t', nbTag+1);
                tei.append("<affiliation>\n");
                TextUtilities.appendN(tei, '\t', nbTag+2);
                tei.append("<orgName>" + TextUtilities.HTMLEncode(aff) + "</orgName>\n");
                if (nbAddresses == nbAffiliations) {
                    int addressRank = 0;
                    if (address != null) {
                        StringTokenizer st3 = new StringTokenizer(address, ";");
                        while (st3.hasMoreTokens()) {
                            String add = st3.nextToken();
                            if (addressRank == affiliationRank) {
                                TextUtilities.appendN(tei, '\t', nbTag + 2);
                                tei.append("<address><addrLine>" + TextUtilities.HTMLEncode(add)
                                        + "</addrLine></address>\n");
                                break;
                            }
                            addressRank++;
                        }
                    }
                }
                TextUtilities.appendN(tei, '\t', nbTag+1);
                tei.append("</affiliation>\n");

                TextUtilities.appendN(tei, '\t', nbTag);
                tei.append("</author>\n");

                affiliationRank++;
            }
        }
        return tei.toString();

    }

    private void appendAffiliation(
        StringBuffer tei,
        int nbTag,
        Affiliation aff,
        GrobidAnalysisConfig config,
        Lexicon lexicon
    ) {
        boolean affiliationWithCoords = (config != null) && 
                                        (config.getGenerateTeiCoordinates() != null) && 
                                        (config.getGenerateTeiCoordinates().contains("affiliation"));
        boolean orgnameWithCoords = (config != null) && 
                                    (config.getGenerateTeiCoordinates() != null) && 
                                    (config.getGenerateTeiCoordinates().contains("orgName"));

        TextUtilities.appendN(tei, '\t', nbTag);
        tei.append("<affiliation");
        if (aff.getKey() != null)
            tei.append(" key=\"").append(aff.getKey()).append("\"");
        if (affiliationWithCoords) {
            // we serialize the coordinates for the whole affiliation block
            String coords = LayoutTokensUtil.getCoordsString(aff.getLayoutTokens());
            if (coords != null && coords.length()>0) {
                tei.append(" coords=\"" + coords + "\"");
            }
        }
        tei.append(">\n");

        if (
            config.getIncludeRawAffiliations()
            && !StringUtils.isEmpty(aff.getRawAffiliationString())
        ) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            String encodedRawAffiliationString = TextUtilities.HTMLEncode(
                aff.getRawAffiliationString()
            );
            tei.append("<note type=\"raw_affiliation\">");
            LOGGER.debug("marker: {}", aff.getMarker());
            if (StringUtils.isNotEmpty(aff.getMarker())) {
                tei.append("<label>");
                tei.append(TextUtilities.HTMLEncode(aff.getMarker()));
                tei.append("</label> ");
            }
            tei.append(encodedRawAffiliationString);
            tei.append("</note>\n");
        }

        if (aff.getDepartments() != null) {
            if (aff.getDepartments().size() == 1) {
                TextUtilities.appendN(tei, '\t', nbTag + 1);
                tei.append("<orgName type=\"department\">" +
                        TextUtilities.HTMLEncode(aff.getDepartments().get(0)) + "</orgName>\n");
            } else {
                int q = 1;
                for (String depa : aff.getDepartments()) {
                    TextUtilities.appendN(tei, '\t', nbTag + 1);
                    tei.append("<orgName type=\"department\" key=\"dep" + q + "\"");
                    tei.append(">" +TextUtilities.HTMLEncode(depa) + "</orgName>\n");
                    q++;
                }
            }
        }

        if (aff.getLaboratories() != null) {
            if (aff.getLaboratories().size() == 1) {
                TextUtilities.appendN(tei, '\t', nbTag + 1);
                tei.append("<orgName type=\"laboratory\">" +
                        TextUtilities.HTMLEncode(aff.getLaboratories().get(0)) + "</orgName>\n");
            } else {
                int q = 1;
                for (String labo : aff.getLaboratories()) {
                    TextUtilities.appendN(tei, '\t', nbTag + 1);
                    tei.append("<orgName type=\"laboratory\" key=\"lab" + q + "\">" +
                            TextUtilities.HTMLEncode(labo) + "</orgName>\n");
                    q++;
                }
            }
        }

        if (aff.getInstitutions() != null) {
            if (aff.getInstitutions().size() == 1) {
                TextUtilities.appendN(tei, '\t', nbTag + 1);
                tei.append("<orgName type=\"institution\">" +
                        TextUtilities.HTMLEncode(aff.getInstitutions().get(0)) + "</orgName>\n");
            } else {
                int q = 1;
                for (String inst : aff.getInstitutions()) {
                    TextUtilities.appendN(tei, '\t', nbTag + 1);
                    tei.append("<orgName type=\"institution\" key=\"instit" + q + "\">" +
                            TextUtilities.HTMLEncode(inst) + "</orgName>\n");
                    q++;
                }
            }
        }

        if (
                aff.getAddrLine() != null ||
                aff.getPostBox() != null ||
                aff.getPostCode() != null ||
                aff.getSettlement() != null ||
                aff.getRegion() != null ||
                aff.getCountry() != null
            ) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            
            tei.append("<address>\n");
            /*if (aff.getAddressString() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<addrLine>" + TextUtilities.HTMLEncode(aff.getAddressString()) +
                        "</addrLine>\n");
            }*/
            if (aff.getAddrLine() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<addrLine>" + TextUtilities.HTMLEncode(aff.getAddrLine()) +
                        "</addrLine>\n");
            }
            if (aff.getPostBox() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<postBox>" + TextUtilities.HTMLEncode(aff.getPostBox()) +
                        "</postBox>\n");
            }
            if (aff.getPostCode() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<postCode>" + TextUtilities.HTMLEncode(aff.getPostCode()) +
                        "</postCode>\n");
            }
            if (aff.getSettlement() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<settlement>" + TextUtilities.HTMLEncode(aff.getSettlement()) +
                        "</settlement>\n");
            }
            if (aff.getRegion() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<region>" + TextUtilities.HTMLEncode(aff.getRegion()) +
                        "</region>\n");
            }
            if (aff.getCountry() != null) {
                String code = lexicon.getCountryCode(aff.getCountry());
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<country");
                if (code != null)
                    tei.append(" key=\"" + code + "\"");
                tei.append(">" + TextUtilities.HTMLEncode(aff.getCountry()) +
                        "</country>\n");
            }

            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("</address>\n");
        }

        TextUtilities.appendN(tei, '\t', nbTag);
        tei.append("</affiliation>\n");
    }

    private static volatile String possiblePreFixPageNumber = "[A-Ze]?";
    private static volatile String possiblePostFixPageNumber = "[A-Z]?";
    private static volatile Pattern page = Pattern.compile("("+possiblePreFixPageNumber+"\\d+"+possiblePostFixPageNumber+")");
    private static volatile Pattern pageDigits = Pattern.compile("\\d+");

    /**
     * Try to normalize the page range, which can be expressed in abbreviated forms and with letter prefix.
     */
    public void postProcessPages() {
        if (pageRange != null) {
            Matcher matcher = page.matcher(pageRange);
            if (matcher.find()) {

                // below for the string form of the page numbers
                String firstPage = null;
                String lastPage = null;

                // alphaPrefix or alphaPostfix are for storing possible alphabetical prefix or postfix to page number, 
                // e.g. "L" in Smith, G. P., Mazzotta, P., Okabe, N., et al. 2016, MNRAS, 456, L74  
                // or "D" in  "Am J Cardiol. 1999, 83:143D-150D. 10.1016/S0002-9149(98)01016-9"
                String alphaPrefixStart = null;
                String alphaPrefixEnd = null;
                String alphaPostfixStart = null;
                String alphaPostfixEnd = null;

                // below for the integer form of the page numbers (part in case alphaPrefix is not null)
                int beginPage = -1;
                int endPage = -1;

                if (matcher.groupCount() > 0) {
                    firstPage = matcher.group(0);
                }

                if (firstPage != null) {
                    try {
                        beginPage = Integer.parseInt(firstPage);
                    } catch (Exception e) {
                        beginPage = -1;
                    }
					if (beginPage != -1) {
						pageRange = "" + beginPage;
                    } else {
                        pageRange = firstPage;

                        // try to get the numerical part of the page number, useful for later
                        Matcher matcher2 = pageDigits.matcher(firstPage);
                        if (matcher2.find()) {
                            try {
                                beginPage = Integer.parseInt(matcher2.group());
                                if (firstPage.length() > 0) {
                                    alphaPrefixStart = firstPage.substring(0,1);
                                    // is it really alphabetical character?
                                    if (!Pattern.matches(possiblePreFixPageNumber, alphaPrefixStart)) {
                                        alphaPrefixStart = null;
                                        // look at postfix
                                        alphaPostfixStart = firstPage.substring(firstPage.length()-1,firstPage.length());
                                        if (!Pattern.matches(possiblePostFixPageNumber, alphaPostfixStart)) {
                                            alphaPostfixStart = null;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                beginPage = -1;
                            }
                        }
                    }

                    if (matcher.find()) {
                        if (matcher.groupCount() > 0) {
                            lastPage = matcher.group(0);
                        }

                        if (lastPage != null) {
                            try {
                                endPage = Integer.parseInt(lastPage);
                            } catch (Exception e) {
                                endPage = -1;
                            }
							
                            if (endPage == -1) {
                                // try to get the numerical part of the page number, to be used for later
                                Matcher matcher2 = pageDigits.matcher(lastPage);
                                if (matcher2.find()) {
                                    try {
                                        endPage = Integer.parseInt(matcher2.group());
                                        if (lastPage.length() > 0) {
                                            alphaPrefixEnd = lastPage.substring(0,1);
                                            // is it really alphabetical character?
                                            if (!Pattern.matches(possiblePreFixPageNumber, alphaPrefixEnd)) {
                                                alphaPrefixEnd = null;
                                                // look at postfix
                                                alphaPostfixEnd = lastPage.substring(lastPage.length()-1,lastPage.length());
                                                if (!Pattern.matches(possiblePostFixPageNumber, alphaPostfixEnd)) {
                                                    alphaPostfixEnd = null;
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        endPage = -1;
                                    }
                                }
                            }

							if ( (endPage != -1) && (endPage < beginPage)) {
                                // there are two possibilities: 
                                // - the substitution, e.g. 433–8 -> 433--438, for example American Medical Association citation style
                                // - the addition, e.g. 433–8 -> 433--441
                                // unfortunately, it depends on the citation style

                                // we try to guess/refine the re-composition of pages

                                if (endPage >= 50) {
                                    // we assume no journal articles have more than 49 pages and is expressed as addition, 
                                    // so it's a substitution
                                    int upperBound = firstPage.length() - lastPage.length();
                                    if (upperBound<firstPage.length() && upperBound > 0)
                                        lastPage = firstPage.substring(0, upperBound) + lastPage;
                                    pageRange += "--" + lastPage;
                                } else {
                                    if (endPage < 10) {
                                        // case 1 digit for endPage

                                        // last digit of begin page
                                        int lastDigitBeginPage = beginPage % 10;

                                        // if digit of lastPage lower than last digit of beginPage, it's an addition for sure
                                        if (endPage < lastDigitBeginPage)
                                            endPage = beginPage + endPage;
                                        else {
                                            // otherwise defaulting to substitution
                                            endPage = beginPage - lastDigitBeginPage + endPage;
                                        }
                                    } else if (endPage < 50) {
                                        // case 2 digit for endPage, we apply a similar heuristics
                                        int lastDigitBeginPage = beginPage % 100;
                                        if (endPage < lastDigitBeginPage)
                                            endPage = beginPage + endPage;
                                        else {
                                            // otherwise defaulting to substitution
                                            endPage = beginPage - lastDigitBeginPage + endPage;
                                        }
                                    }

                                    // we assume there is no article of more than 99 pages expressed in this abbreviated way 
                                    // (which are for journal articles only, so short animals)

                                    if (alphaPrefixEnd != null) 
                                        pageRange += "--" + alphaPrefixEnd + endPage;
                                    else if (alphaPostfixEnd != null) 
                                        pageRange += "--" + endPage + alphaPostfixEnd;
                                    else
                                        pageRange += "--" + endPage;
                                }
							} else if ((endPage != -1)) {
                                if (alphaPrefixEnd != null) 
                                    pageRange += "--" + alphaPrefixEnd + endPage;
                                else if (alphaPostfixEnd != null) 
                                    pageRange += "--" + endPage + alphaPostfixEnd;
                                else
                                    pageRange += "--" + lastPage;
                            } else {
                                pageRange += "--" + lastPage;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Correct/add identifiers of the first biblio item based on the second one
     */
    public static void injectIdentifiers(BiblioItem destination, BiblioItem source) {
        destination.setDOI(source.getDOI());
        // optionally associated strong identifiers are also injected
        destination.setPMID(source.getPMID());
        destination.setPMCID(source.getPMCID());
        destination.setPII(source.getPII());
        destination.setIstexId(source.getIstexId());
        destination.setArk(source.getArk());
        destination.setHalId(source.getHalId());
    }

    /**
     * Correct fields of the first biblio item based on the second one and the reference string
     * 
     * @param bib extracted from document
     * @param bibo fetched from metadata provider (biblioglutton, crossref..)
     */
    public static void correct(BiblioItem bib, BiblioItem bibo) {
        //System.out.println("correct: \n" + bib.toTEI(0));
        //System.out.println("with: \n" + bibo.toTEI(0));
        if (bibo.getDOI() != null)
            bib.setDOI(bibo.getDOI());
        if (bibo.getPMID() != null)
            bib.setPMID(bibo.getPMID());
        if (bibo.getPMCID() != null)
            bib.setPMCID(bibo.getPMCID());
        if (bibo.getPII() != null)
            bib.setPII(bibo.getPII());
        if (bibo.getIstexId() != null)
            bib.setIstexId(bibo.getIstexId());
        if (bibo.getArk() != null)
            bib.setArk(bibo.getArk());
        if (bibo.getHalId() != null)
            bib.setHalId(bibo.getHalId());

        if (bibo.getOAURL() != null)
            bib.setOAURL(bibo.getOAURL());

        if (bibo.getJournal() != null) {
            bib.setJournal(bibo.getJournal());
            // document type consistency (correction might change overall item type, and some
            // fields become unconsistent)
            if (bibo.getBookTitle() == null) {
                bib.setBookTitle(null);
            }
        }
        if (bibo.getAuthors() != null)
            bib.setAuthors(bibo.getAuthors());
        if (bibo.getEditors() != null)
            bib.setEditors(bibo.getEditors());
        if (bibo.getBookTitle() != null) {
            bib.setBookTitle(bibo.getBookTitle());
            // document type consistency
            if (bibo.getJournal() == null) {
                bib.setJournal(null);
            }
        }
        if (bibo.getVolume() != null)
            bib.setVolume(bibo.getVolume());
        if (bibo.getVolumeBlock() != null)
            bib.setVolumeBlock(bibo.getVolumeBlock(), false);
        if (bibo.getIssue() != null)
            bib.setIssue(bibo.getIssue());
        if (bibo.getBeginPage() != -1)
            bib.setBeginPage(bibo.getBeginPage());
        if (bibo.getEndPage() != -1)
            bib.setEndPage(bibo.getEndPage());
        if (bibo.getPageRange() != null)
            bib.setPageRange(bibo.getPageRange());
        if (bibo.getPublicationDate() != null)
            bib.setPublicationDate(bibo.getPublicationDate());
        if (bibo.getSubmissionDate() != null)
            bib.setSubmissionDate(bibo.getSubmissionDate());
        if (bibo.getDownloadDate() != null)
            bib.setDownloadDate(bibo.getDownloadDate());
       
        if (bibo.getNormalizedPublicationDate() != null) {
            if (bib.getNormalizedPublicationDate() != null) {
                bib.mergeNormalizedPublicationDate(bibo.getNormalizedPublicationDate());
            }
            else {
                bib.setNormalizedPublicationDate(bibo.getNormalizedPublicationDate());
            }
        }
         if (bibo.getYear() != null)
            bib.setYear(bibo.getYear());
        if (bibo.getMonth() != null)
            bib.setMonth(bibo.getMonth());
        if (bibo.getDay() != null)
            bib.setDay(bibo.getDay());
        if (bibo.getE_Year() != null)
            bib.setE_Year(bibo.getE_Year());
        if (bibo.getE_Month() != null)
            bib.setE_Month(bibo.getE_Month());
        if (bibo.getE_Day() != null)
            bib.setE_Day(bibo.getE_Day());
        if (bibo.getA_Year() != null)
            bib.setA_Year(bibo.getA_Year());
        if (bibo.getA_Month() != null)
            bib.setA_Month(bibo.getA_Month());
        if (bibo.getA_Day() != null)
            bib.setA_Day(bibo.getA_Day());
        if (bibo.getS_Year() != null)
            bib.setS_Year(bibo.getS_Year());
        if (bibo.getS_Month() != null)
            bib.setS_Month(bibo.getS_Month());
        if (bibo.getS_Day() != null)
            bib.setS_Day(bibo.getS_Day());

        if (bibo.getD_Year() != null)
            bib.setD_Year(bibo.getD_Year());
        if (bibo.getD_Month() != null)
            bib.setD_Month(bibo.getD_Month());
        if (bibo.getD_Day() != null)
            bib.setD_Day(bibo.getD_Day());

        if (bibo.getLocation() != null)
            bib.setLocation(bibo.getLocation());
        if (bibo.getPublisher() != null)
            bib.setPublisher(bibo.getPublisher());
        if (bibo.getTitle() != null) {
            bib.setTitle(bibo.getTitle());
        }
        if (bibo.getArticleTitle() != null) {
            bib.setArticleTitle(bibo.getArticleTitle());
        }
        if (bibo.getJournalAbbrev() != null) {
            bib.setJournalAbbrev(bibo.getJournalAbbrev());
        }
        if (bibo.getISSN() != null)
            bib.setISSN(bibo.getISSN());
        if (bibo.getISSNe() != null)
            bib.setISSNe(bibo.getISSNe());
        if (bibo.getISBN10() != null)
            bib.setISBN10(bibo.getISBN10());
        if (bibo.getISBN13() != null)
            bib.setISBN13(bibo.getISBN13());
        if (bibo.getHalId() != null)
            bib.setHalId(bibo.getHalId());

        if (bibo.getItem() != -1) {
            bib.setItem(bibo.getItem());
        }
        if (bibo.getCollaboration() != null) {
            bib.setCollaboration(bibo.getCollaboration());
        }

        // authors present in fullAuthors list should be in the existing resources 
        // at least the corresponding author
        if (!CollectionUtils.isEmpty(bibo.getFullAuthors())) {
            if (CollectionUtils.isEmpty(bib.getFullAuthors()))
                bib.setFullAuthors(bibo.getFullAuthors());
            else if (bibo.getFullAuthors().size() == 1) {
                // we have the corresponding author 
                // check if the author exists in the obtained list
                Person auto = (Person) bibo.getFullAuthors().get(0);
                List<Person> auts = bib.getFullAuthors();
                if (auts != null) {
                    for (Person aut : auts) {
                        if (StringUtils.isNotBlank(aut.getLastName()) && StringUtils.isNotBlank(auto.getLastName())) {
                            if (aut.getLastName().toLowerCase().equals(auto.getLastName().toLowerCase())) {
                                if (StringUtils.isBlank(aut.getFirstName()) ||
                                   (auto.getFirstName() != null && 
                                    aut.getFirstName().length() <= auto.getFirstName().length() && 
                                         auto.getFirstName().toLowerCase().startsWith(aut.getFirstName().toLowerCase()))) {
                                    aut.setFirstName(auto.getFirstName());
                                    aut.setCorresp(true);
                                    if (StringUtils.isNotBlank(auto.getEmail())) 
                                        aut.setEmail(auto.getEmail());
                                    // should we also check the country ? affiliation?
                                    if (StringUtils.isNotBlank(auto.getMiddleName()) && (StringUtils.isBlank(aut.getMiddleName())))
                                        aut.setMiddleName(auto.getMiddleName());
                                    // crossref is considered more reliable than PDF annotations
                                    aut.setORCID(auto.getORCID());
                                }
                            }
                        }
                    }
                }
            } else if (bibo.getFullAuthors().size() > 1) {
                // we have the complete list of authors so we can take them from the second
                // biblio item and merge some possible extra from the first when a match is 
                // reliable
                for (Person aut : bibo.getFullAuthors()) {
                    // try to find the author in the first item (we know it's not empty)
                    for (Person aut2 : bib.getFullAuthors()) {


                        if (StringUtils.isNotBlank(aut2.getLastName())) {
                            String aut2_lastname = aut2.getLastName().toLowerCase();

                            if (StringUtils.isNotBlank(aut.getLastName())) {
                                String aut_lastname = aut.getLastName().toLowerCase();

                                if (aut_lastname.equals(aut2_lastname)) {
                                    // check also first name if present - at least for the initial
                                    if ( StringUtils.isBlank(aut2.getFirstName()) || 
                                         (StringUtils.isNotBlank(aut2.getFirstName()) && StringUtils.isNotBlank(aut.getFirstName())) ) {
                                        // we have no first name or a match (full first name)

                                        if ( StringUtils.isBlank(aut2.getFirstName()) 
                                            || 
                                             aut.getFirstName().equals(aut2.getFirstName())
                                            ||
                                             ( aut.getFirstName().length() == 1 && 
                                               aut.getFirstName().equals(aut2.getFirstName().substring(0,1)) ) 
                                            ) {
                                            // we have a match (full or initial)
                                            if (StringUtils.isNotBlank(aut2.getFirstName()) &&
                                                aut2.getFirstName().length() > aut.getFirstName().length())
                                                aut.setFirstName(aut2.getFirstName());
                                            if (StringUtils.isBlank(aut.getMiddleName()))
                                                aut.setMiddleName(aut2.getMiddleName());
                                            if (StringUtils.isBlank(aut.getTitle()))
                                                aut.setTitle(aut2.getTitle());
                                            if (StringUtils.isBlank(aut.getSuffix()))
                                                aut.setSuffix(aut2.getSuffix());
                                            if (StringUtils.isBlank(aut.getEmail()))
                                                aut.setEmail(aut2.getEmail());
                                            if(!CollectionUtils.isEmpty(aut2.getAffiliations()))
                                                aut.setAffiliations(aut2.getAffiliations());
                                            if (!CollectionUtils.isEmpty(aut2.getAffiliationBlocks())) 
                                                aut.setAffiliationBlocks(aut2.getAffiliationBlocks());
                                            if (!CollectionUtils.isEmpty(aut2.getAffiliationMarkers())) 
                                                aut.setAffiliationMarkers(aut2.getAffiliationMarkers());
                                            if (!CollectionUtils.isEmpty(aut2.getMarkers())) 
                                                aut.setMarkers(aut2.getMarkers());
                                            if (!CollectionUtils.isEmpty(aut2.getLayoutTokens()))
                                                aut.setLayoutTokens(aut2.getLayoutTokens());
                                            // preserve PDF-extracted ORCID when crossref doesn't have one
                                            if (StringUtils.isBlank(aut.getORCID()) && StringUtils.isNotBlank(aut2.getORCID()))
                                                aut.setORCID(aut2.getORCID());
                                            break;
                                        } 
                                    }  
                                }
                            }
                        }
                    }
                }
                bib.setFullAuthors(bibo.getFullAuthors());
            }
        }
        bib.setStatus(bibo.getStatus());
    }

	/**
     *  Check is the biblio item can be considered as a minimally valid bibliographical reference. 
	 *  A certain minimal number of core metadata have to be instanciated. Otherwise, the biblio
	 *  item can be considered as "garbage" extracted incorrectly.  
	 */
	public boolean rejectAsReference() {
		boolean titleSet = true;
		if ( (title == null) && (bookTitle == null) && (journal == null) && 
				(ISSN == null) && (ISBN13 == null)  && (ISBN10 == null))
			titleSet = false;
		boolean authorSet = true;
		if (fullAuthors == null && collaboration == null) 
			authorSet = false;
		// normally properties authors and authorList are null in the current Grobid version
		if (!titleSet && !authorSet && url == null && doi == null && halId ==null)
			return true;
		else
			return false;
	}

    public String getTeiId() {
        return teiId;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public void setCoordinates(List<BoundingBox> coordinates) {
        this.coordinates = coordinates;
    }

    public List<BoundingBox> getCoordinates() {
        return coordinates;
    }

    public Map<String, List<LayoutToken>> getLabeledTokens() {
        return labeledTokens;
    }

    public void setLabeledTokens(Map<String, List<LayoutToken>> labeledTokens) {
        this.labeledTokens = labeledTokens;
    }

    public List<LayoutToken> getLayoutTokens(TaggingLabel biblioLabel) {
        if (labeledTokens == null) {
            LOGGER.debug("labeledTokens is null");
            return null;
        }
        if (biblioLabel.getLabel() == null) {
            LOGGER.debug("biblioLabel.getLabel() is null");
            return null;
        }
        return labeledTokens.get(biblioLabel.getLabel());
    }

    public void setLayoutTokensForLabel(List<LayoutToken> tokens, TaggingLabel biblioLabel) {
        if (labeledTokens == null)
            labeledTokens = new TreeMap<>();
        labeledTokens.put(biblioLabel.getLabel(), tokens);
    }

    public void generalResultMappingHeader(String labeledResult, List<LayoutToken> tokenizations) {
        if (labeledTokens == null)
            labeledTokens = new TreeMap<>();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.HEADER, labeledResult, tokenizations);
        List<TaggingTokenCluster> clusters = clusteror.cluster();
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            List<LayoutToken> clusterTokens = cluster.concatTokens();
            List<LayoutToken> theList = labeledTokens.get(clusterLabel.getLabel());

            theList = theList == null ? new ArrayList<>() : theList;
            theList.addAll(clusterTokens);
            labeledTokens.put(clusterLabel.getLabel(), theList);

            if (clusterLabel.equals(TaggingLabels.HEADER_AFFILIATION) || clusterLabel.equals(TaggingLabels.HEADER_ADDRESS)) {
                if (affiliationAddresslabeledTokens == null) 
                    affiliationAddresslabeledTokens = new ArrayList<>();
                if (!affiliationAddresslabeledTokens.contains(clusterTokens))
                    affiliationAddresslabeledTokens.add(clusterTokens);
            }
        }
    }

    public void generalResultMappingReference(String labeledResult, List<LayoutToken> tokenizations) {
        if (labeledTokens == null)
            labeledTokens = new TreeMap<>();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.CITATION, labeledResult, tokenizations);
        List<TaggingTokenCluster> clusters = clusteror.cluster();
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            List<LayoutToken> clusterTokens = cluster.concatTokens();
            List<LayoutToken> theList = labeledTokens.get(clusterLabel.getLabel());

            theList = theList == null ? new ArrayList<>() : theList;
            theList.addAll(clusterTokens);
            labeledTokens.put(clusterLabel.getLabel(), theList);
        }
    }

    public List<LayoutToken> getAuthorsTokensWorkingCopy() {
        return authorsTokensWorkingCopy;
    }

    public List<LayoutToken> getAbstractTokensWorkingCopy() {
        return abstractTokensWorkingCopy;
    }

    public String getAvailabilityStmt() {
        return availabilityStmt;
    }

    public void setAvailabilityStmt(String availabilityStmt) {
        this.availabilityStmt = availabilityStmt;
    }

    public List<List<LayoutToken>> getAffiliationAddresslabeledTokens() {
        return affiliationAddresslabeledTokens;
    }

    public void setCopyrightsLicense(CopyrightsLicense copyrightsLicense) {
        this.copyrightsLicense = copyrightsLicense;
    }

    public CopyrightsLicense getCopyrightsLicense() {
        return this.copyrightsLicense;
    }

    public List<String> getDiscardedPieces() {
        return discardedPieces;
    }

    public void setDiscardedPieces(List<String> discardedPieces) {
        this.discardedPieces = discardedPieces;
    }

    public void addDiscardedPiece(String piece) {
        this.discardedPieces.add(piece);
    }

    public List<List<LayoutToken>> getDiscardedPiecesTokens() {
        return discardedPiecesTokens;
    }

    public void setDiscardedPiecesTokens(List<List<LayoutToken>> discardedPiecesTokens) {
        this.discardedPiecesTokens = discardedPiecesTokens;
    }

    public void addDiscardedPieceTokens(List<LayoutToken> pieceToken) {
        this.discardedPiecesTokens.add(pieceToken);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConflictStmt() {
        return conflictStmt;
    }

    public void setConflictStmt(String conflictStmt) {
        this.conflictStmt = conflictStmt;
    }

    public String getContributionStmt() {
        return contributionStmt;
    }

    public void setContributionStmt(String contributionStmt) {
        this.contributionStmt = contributionStmt;
    }
}
