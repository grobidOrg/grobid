package org.grobid.core.lexicon;

import static org.grobid.core.utilities.Utilities.convertStringOffsetToTokenOffset;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.PDFAnnotation;
import org.grobid.core.sax.CountryCodeSaxParser;
import org.grobid.core.utilities.*;

/**
 * Class for managing all the lexical resources.
 */
public class Lexicon {
    private static final Logger LOGGER = LoggerFactory.getLogger(Lexicon.class);
    // private static volatile Boolean instanceController = false;
    private static volatile Lexicon instance;

    // gazetteers
    private Set<String> dictionary_en = null;
    private Set<String> dictionary_de = null;
    private Set<String> lastNames = null;
    private Set<String> firstNames = null;
    private Map<String, String> countryCodes = null;
    private Set<String> countries = null;

    // retrieve basic naming information about a research infrastructure (key must be lower case!)
    private Map<String, List<OrganizationRecord>> researchOrganizations = null;

    // fast matchers for efficient and flexible pattern matching in layout token sequence or strings
    private FastMatcher abbrevJournalPattern = null;
    private FastMatcher conferencePattern = null;
    private FastMatcher publisherPattern = null;
    private FastMatcher journalPattern = null;
    private FastMatcher cityPattern = null;
    private FastMatcher organisationPattern = null;
    private FastMatcher researchInfrastructurePattern = null;
    private FastMatcher locationPattern = null;
    private FastMatcher countryPattern = null;
    private FastMatcher orgFormPattern = null;
    private FastMatcher collaborationPattern = null;
    private FastMatcher funderPattern = null;
    private FastMatcher personTitlePattern = null;
    private FastMatcher personSuffixPattern = null;

    /**
     * @deprecated Use {@link #builder()} instead. This method is preserved for backward
     *     compatibility with existing call sites and reproduces the original behavior
     *     exactly: it eagerly loads {@link Builder#withDefaults() the historical eager
     *     set} (wordforms, person names, country codes); every other gazetteer loads
     *     lazily on first lookup. Migrate to {@link #builder()} and, optionally, request
     *     the gazetteers you want pre-warmed eagerly.
     */
    @Deprecated
    public static Lexicon getInstance() {
        return builder().withDefaults().build();
    }

    /**
     * Returns the bare singleton without triggering any loading. Used internally by
     * {@link Builder#build()}; not part of the public API — callers go through
     * {@link #builder()}.
     */
    static synchronized Lexicon getRawInstance() {
        if (instance == null) {
            LOGGER.debug("Get new instance of Lexicon");
            GrobidProperties.getInstance();
            instance = new Lexicon();
        }
        return instance;
    }

    /**
     * Returns a builder for declaratively configuring a {@link Lexicon}. The Builder is
     * the canonical entry point and declares only what to load <em>eagerly</em>: each
     * {@code withX()} call pre-loads one gazetteer at {@link Builder#build() build()}
     * time instead of waiting for first use.
     *
     * <pre>
     * Lexicon lex = Lexicon.builder()
     *         .withDefaults()           // eager: wordforms + people + countries
     *         .withOrganisations()      // eager: pre-load the org gazetteer too
     *         .build();
     * </pre>
     *
     * Loading is <strong>lazy by default</strong>: any gazetteer not named here loads
     * transparently on first lookup, so a {@link Lexicon} obtained from any entry point
     * is always fully functional and never throws for a missing gazetteer.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent configurator for {@link Lexicon}. Each {@code withX()} call marks one
     * gazetteer for <em>eager</em> pre-loading. {@link #build()} returns the singleton
     * after triggering each requested loader once (idempotent — already-loaded
     * gazetteers are not reloaded). Gazetteers not requested here still load lazily on
     * first lookup; {@code withX()} only controls <em>when</em> a gazetteer loads, never
     * whether a lookup succeeds.
     */
    public static class Builder {
        private boolean wordforms = false;
        private boolean people = false;
        private boolean countries = false;
        private boolean journals = false;
        private boolean conferences = false;
        private boolean publishers = false;
        private boolean cities = false;
        private boolean locations = false;
        private boolean personTitles = false;
        private boolean personSuffixes = false;
        private boolean funders = false;
        private boolean researchInfrastructures = false;
        private boolean collaborations = false;
        private boolean organisations = false;
        private boolean orgForms = false;

        /** Enables the English/German wordform dictionaries (english.wf, german.wf). */
        public Builder withWordforms() {
            this.wordforms = true;
            return this;
        }

        /** Enables the first/last name lists (names.family, lastname.5k, names.female, names.male, firstname.5k). */
        public Builder withPeople() {
            this.people = true;
            return this;
        }

        /** Enables ISO 3166 country codes and the country-name pattern (CountryCodes.xml). */
        public Builder withCountries() {
            this.countries = true;
            return this;
        }

        /** Enables the journal-name gazetteer (journals.txt + abbrev_journals.txt). */
        public Builder withJournals() {
            this.journals = true;
            return this;
        }

        /** Enables the conference/proceedings gazetteer (proceedings.txt). */
        public Builder withConferences() {
            this.conferences = true;
            return this;
        }

        /** Enables the publisher gazetteer (publishers.txt). */
        public Builder withPublishers() {
            this.publishers = true;
            return this;
        }

        /** Enables the city gazetteer (cities15000.txt). */
        public Builder withCities() {
            this.cities = true;
            return this;
        }

        /** Enables the location gazetteer (places/location.txt). */
        public Builder withLocations() {
            this.locations = true;
            return this;
        }

        /** Enables the person-title gazetteer (VincentNgPeopleTitles.txt). */
        public Builder withPersonTitles() {
            this.personTitles = true;
            return this;
        }

        /** Enables the person-name suffix gazetteer (suffix.txt). */
        public Builder withPersonSuffixes() {
            this.personSuffixes = true;
            return this;
        }

        /** Enables the funder gazetteer (funders.txt) used by the funding model. */
        public Builder withFunders() {
            this.funders = true;
            return this;
        }

        /** Enables the research-infrastructure gazetteer (research_infrastructures.txt + research_infrastructures_map.txt). */
        public Builder withResearchInfrastructures() {
            this.researchInfrastructures = true;
            return this;
        }

        /** Enables the collaboration gazetteer (inspire_collaborations.txt) used by the citation model. */
        public Builder withCollaborations() {
            this.collaborations = true;
            return this;
        }

        /**
         * Enables the organisation gazetteer (Wikipedia orgs + government agencies +
         * known corporations + venture-funded companies). ~133K entries.
         */
        public Builder withOrganisations() {
            this.organisations = true;
            return this;
        }

        /** Enables the organisation-form gazetteer (Inc., Ltd., Corp., GmbH, etc.). */
        public Builder withOrgForms() {
            this.orgForms = true;
            return this;
        }

        /**
         * Eagerly pre-loads the historical eager set from the original {@code Lexicon()}
         * constructor: wordforms, people (first/last names), and country codes. These
         * are the gazetteers every parser flow needs at startup; everything else
         * (journals, locations, funders, etc.) still loads lazily on first lookup, or
         * can be pre-warmed eagerly via the corresponding {@code withX()}.
         */
        public Builder withDefaults() {
            return withWordforms()
                    .withPeople()
                    .withCountries();
        }

        /**
         * Returns the {@link Lexicon} singleton, ensuring every requested gazetteer
         * has been loaded. Already-loaded gazetteers are not reloaded.
         */
        public Lexicon build() {
            Lexicon lex = getRawInstance();
            if (wordforms && lex.dictionary_en == null) {
                lex.loadWordforms();
            }
            if (people && lex.firstNames == null) {
                lex.loadPeople();
            }
            if (countries && lex.countryCodes == null) {
                lex.loadCountries();
            }
            if (journals && lex.journalPattern == null) {
                lex.initJournals();
            }
            if (conferences && lex.conferencePattern == null) {
                lex.initConferences();
            }
            if (publishers && lex.publisherPattern == null) {
                lex.initPublishers();
            }
            if (cities && lex.cityPattern == null) {
                lex.initCities();
            }
            if (locations && lex.locationPattern == null) {
                lex.initLocations();
            }
            if (personTitles && lex.personTitlePattern == null) {
                lex.initPersonTitles();
            }
            if (personSuffixes && lex.personSuffixPattern == null) {
                lex.initPersonSuffix();
            }
            if (funders && lex.funderPattern == null) {
                lex.initFunders();
            }
            if (researchInfrastructures && lex.researchInfrastructurePattern == null) {
                lex.initResearchInfrastructures();
            }
            if (collaborations && lex.collaborationPattern == null) {
                lex.initCollaborations();
            }
            if (organisations && lex.organisationPattern == null) {
                lex.initOrganisations();
            }
            if (orgForms && lex.orgFormPattern == null) {
                lex.initOrgForms();
            }
            return lex;
        }
    }

    /**
     * Hidden constructor. The constructor performs no loading — every gazetteer is
     * loaded explicitly via {@link Builder#build()} based on the requested flags.
     */
    private Lexicon() {
    }

    private synchronized void loadWordforms() {
        if (dictionary_en != null) {
            return;
        }
        dictionary_en = new HashSet<>();
        dictionary_de = new HashSet<>();
        addDictionary(
                GrobidProperties.getGrobidHomePath()
                        + File.separator
                        +
                        "lexicon"
                        + File.separator
                        + "wordforms"
                        + File.separator
                        + "english.wf",
                Language.EN);
        addDictionary(
                GrobidProperties.getGrobidHomePath()
                        + File.separator
                        +
                        "lexicon"
                        + File.separator
                        + "wordforms"
                        + File.separator
                        + "german.wf",
                Language.DE);
    }

    private synchronized void loadPeople() {
        if (firstNames != null) {
            return;
        }
        firstNames = new HashSet<>();
        lastNames = new HashSet<>();
        addLastNames(
                GrobidProperties.getGrobidHomePath()
                        + File.separator
                        +
                        "lexicon"
                        + File.separator
                        + "names"
                        + File.separator
                        + "names.family");
        addLastNames(
                GrobidProperties.getGrobidHomePath()
                        + File.separator
                        +
                        "lexicon"
                        + File.separator
                        + "names"
                        + File.separator
                        + "lastname.5k");
        addFirstNames(
                GrobidProperties.getGrobidHomePath()
                        + File.separator
                        +
                        "lexicon"
                        + File.separator
                        + "names"
                        + File.separator
                        + "names.female");
        addFirstNames(
                GrobidProperties.getGrobidHomePath()
                        + File.separator
                        +
                        "lexicon"
                        + File.separator
                        + "names"
                        + File.separator
                        + "names.male");
        addFirstNames(
                GrobidProperties.getGrobidHomePath()
                        + File.separator
                        +
                        "lexicon"
                        + File.separator
                        + "names"
                        + File.separator
                        + "firstname.5k");
    }

    private synchronized void loadCountries() {
        if (countryCodes != null) {
            return;
        }
        countryCodes = new HashMap<>();
        countries = new HashSet<>();
        countryPattern = new FastMatcher();
        addCountryCodes(
                GrobidProperties.getGrobidHomePath()
                        + File.separator
                        +
                        "lexicon"
                        + File.separator
                        + "countries"
                        + File.separator
                        + "CountryCodes.xml");
    }

    /**
     * A basic class to hold dictionary/naming information about an organization for a given language
     */
    public class OrganizationRecord {
        public String name;
        public String fullName;
        public String lang; // ISO 2-characters language code

        public OrganizationRecord(String name, String fullName, String lang) {
            this.name = name;
            this.fullName = fullName;
            this.lang = lang;
        }
    }

    private void initDictionary() {
        LOGGER.debug("Initiating dictionary");
        dictionary_en = new HashSet<>();
        dictionary_de = new HashSet<>();
        LOGGER.debug("End of Initialization of dictionary");
    }

    public final void addDictionary(String path, String lang) {
        File file = new File(path);
        if (!file.exists()) {
            throw new GrobidResourceException("Cannot add entries to dictionary (language '"
                    + lang
                    +
                    "'), because file '"
                    + file.getAbsolutePath()
                    + "' does not exists.");
        }
        if (!file.canRead()) {
            throw new GrobidResourceException("Cannot add entries to dictionary (language '"
                    + lang
                    +
                    "'), because cannot read file '"
                    + file.getAbsolutePath()
                    + "'.");
        }
        InputStream ist = null;
        InputStreamReader isr = null;
        BufferedReader dis = null;
        try {
            ist = new FileInputStream(file);
            isr = new InputStreamReader(ist, StandardCharsets.UTF_8);
            dis = new BufferedReader(isr);

            String l = null;
            while ((l = dis.readLine()) != null) {
                if (StringUtils.isBlank(l)) {
                    continue;
                }

                // the first token, separated by a tabulation, gives the word form
                if (lang.equals(Language.EN)) {
                    // multext format
                    StringTokenizer st = new StringTokenizer(l, "\t");
                    if (st.hasMoreTokens()) {
                        String word = st.nextToken();
                        dictionary_en.add(word);
                    }
                } else if (lang.equals(Language.DE)) {
                    // celex format
                    StringTokenizer st = new StringTokenizer(l, "\\");
                    if (st.hasMoreTokens()) {
                        st.nextToken(); // id
                        String word = st.nextToken();
                        word = word.replace("\"a", "ä");
                        word = word.replace("\"u", "ü");
                        word = word.replace("\"o", "ö");
                        word = word.replace("$", "ß");
                        dictionary_de.add(word);
                    }
                }
            }
        } catch (IOException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            IOUtils.closeQuietly(ist, isr, dis);
        }
    }

    public boolean isCountry(String tok) {
        if (countries == null) {
            loadCountries();
        }
        return countries.contains(tok.toLowerCase());
    }

    private void addCountryCodes(String path) {
        File file = getFile(path, "country codes");
        InputStream ist = null;
        try {
            ist = new FileInputStream(file);
            CountryCodeSaxParser parser = new CountryCodeSaxParser(countryCodes, countries);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            //get a new instance of parser
            SAXParser p = spf.newSAXParser();
            p.parse(ist, parser);
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            IOUtils.closeQuietly(ist);
        }

        for (String country : countries) {
            countryPattern.loadTerm(country, GrobidAnalyzer.getInstance(), false, false); // ignore delimiters, not case sensitive
        }
    }

    private static @NonNull File getFile(String path, String lexiconName) {
        File file = new File(path);
        if (!file.exists()) {
            throw new GrobidResourceException("Cannot add "
                    + lexiconName
                    + " to dictionary, because file '"
                    + file.getAbsolutePath()
                    + "' does not exists.");
        }
        if (!file.canRead()) {
            throw new GrobidResourceException("Cannot add "
                    + lexiconName
                    + " to dictionary, because cannot read file '"
                    +
                    file.getAbsolutePath()
                    + "'.");
        }
        return file;
    }

    public String getCountryCode(String country) {
        if (countryCodes == null) {
            loadCountries();
        }
        String code = (String) countryCodes.get(country.toLowerCase());
        return code;
    }

    public void initCountryPatterns() {
        if (countries == null || countries.isEmpty()) {
            // it should never be the case
            addCountryCodes(
                    GrobidProperties.getGrobidHomePath()
                            + File.separator
                            +
                            "lexicon"
                            + File.separator
                            + "countries"
                            + File.separator
                            + "CountryCodes.xml");
        }

        for (String country : countries) {
            countryPattern.loadTerm(country, GrobidAnalyzer.getInstance(), false, false); // ignore delimiters, not case sensitive
        }
    }

    public final void addFirstNames(String path) {
        File file = getFile(path, "first names");
        InputStream ist = null;
        InputStreamReader isr = null;
        BufferedReader dis = null;
        try {
            ist = new FileInputStream(file);
            isr = new InputStreamReader(ist, StandardCharsets.UTF_8);
            dis = new BufferedReader(isr);

            String l = null;
            while ((l = dis.readLine()) != null) {
                // read the line
                // the first token, separated by a tabulation, gives the word form
                StringTokenizer st = new StringTokenizer(l, "\t\n-");
                if (st.hasMoreTokens()) {
                    String word = st.nextToken().toLowerCase().trim();
                    if (!firstNames.contains(word)) {
                        firstNames.add(word);
                    }
                }
            }
        } catch (IOException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            IOUtils.closeQuietly(dis, isr, ist);
        }
    }

    public final void addLastNames(String path) {
        File file = getFile(path, "last names");
        InputStream ist = null;
        InputStreamReader isr = null;
        BufferedReader dis = null;
        try {
            ist = new FileInputStream(file);
            isr = new InputStreamReader(ist, "UTF8");
            dis = new BufferedReader(isr);

            String l = null;
            while ((l = dis.readLine()) != null) {
                // read the line
                // the first token, separated by a tabulation, gives the word form
                StringTokenizer st = new StringTokenizer(l, "\t\n-");
                if (st.hasMoreTokens()) {
                    String word = st.nextToken().toLowerCase().trim();
                    if (!lastNames.contains(word)) {
                        lastNames.add(word);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } catch (IOException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            IOUtils.closeQuietly(dis, isr, ist);
        }
    }

    /**
     * Lexical look-up, default is English
     *
     * @param s a string to test
     * @return true if in the dictionary
     */
    public boolean inDictionary(String s) {
        return inDictionary(s, Language.EN);
    }

    public boolean inDictionary(String s, String lang) {
        if (dictionary_en == null) {
            loadWordforms();
        }
        if (s == null)
            return false;
        if ((s.endsWith(".")) | (s.endsWith(",")) | (s.endsWith(":")) | (s.endsWith(";")) | (s.endsWith(".")))
            s = s.substring(0, s.length() - 1);
        int i1 = s.indexOf('-');
        int i2 = s.indexOf(' ');
        if (i1 != -1) {
            String s1 = s.substring(0, i1);
            String s2 = s.substring(i1 + 1, s.length());
            if (lang.equals(Language.DE)) {
                if ((dictionary_de.contains(s1)) & (dictionary_de.contains(s2)))
                    return true;
                else
                    return false;
            } else {
                if ((dictionary_en.contains(s1)) & (dictionary_en.contains(s2)))
                    return true;
                else
                    return false;
            }
        }
        if (i2 != -1) {
            String s1 = s.substring(0, i2);
            String s2 = s.substring(i2 + 1, s.length());
            if (lang.equals(Language.DE)) {
                if ((dictionary_de.contains(s1)) & (dictionary_de.contains(s2)))
                    return true;
                else
                    return false;
            } else {
                if ((dictionary_en.contains(s1)) & (dictionary_en.contains(s2)))
                    return true;
                else
                    return false;
            }
        } else {
            if (lang.equals(Language.DE)) {
                return dictionary_de.contains(s);
            } else {
                return dictionary_en.contains(s);
            }
        }
    }

    public void initJournals() {
        try {
            abbrevJournalPattern = new FastMatcher(
                    new File(GrobidProperties.getGrobidHomePath() + "/lexicon/journals/abbrev_journals.txt"));

            journalPattern = new FastMatcher(
                    new File(GrobidProperties.getGrobidHomePath() + "/lexicon/journals/journals.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException(
                    "Error when compiling lexicon matcher for abbreviated journal names.", e);
        }
    }

    public void initConferences() {
        // ArrayList<String> conferences = new ArrayList<String>();
        try {
            conferencePattern = new FastMatcher(
                    new File(GrobidProperties.getGrobidHomePath() + "/lexicon/journals/proceedings.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for conference names.", e);
        }
    }

    public void initPublishers() {
        try {
            publisherPattern = new FastMatcher(
                    new File(GrobidProperties.getGrobidHomePath() + "/lexicon/publishers/publishers.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for conference names.", e);
        }
    }

    public void initCities() {
        try {
            cityPattern = new FastMatcher(
                    new File(GrobidProperties.getGrobidHomePath() + "/lexicon/places/cities15000.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for cities.", e);
        }
    }

    public void initCollaborations() {
        try {
            //collaborationPattern = new FastMatcher(new
            //        File(GrobidProperties.getGrobidHomePath() + "/lexicon/organisations/collaborations.txt"));
            collaborationPattern = new FastMatcher(new File(
                    GrobidProperties.getGrobidHomePath() + "/lexicon/organisations/inspire_collaborations.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for collaborations.", e);
        }
    }

    /**
     * Loads the organisation gazetteer (Wikipedia organisations, government agencies,
     * known corporations, venture-funded companies). Not loaded by the default
     * {@link #getInstance()}; use {@link Lexicon#builder()} with {@code .withOrganisations()}
     * to enable, or call this method directly.
     */
    public void initOrganisations() {
        try {
            organisationPattern = new FastMatcher(
                    new File(GrobidProperties.getGrobidHomePath() + "/lexicon/organisations/WikiOrganizations.lst"));
            organisationPattern.loadTerms(
                    new File(GrobidProperties.getGrobidHomePath()
                            +
                            "/lexicon/organisations/government.government_agency"));
            organisationPattern.loadTerms(
                    new File(GrobidProperties.getGrobidHomePath()
                            +
                            "/lexicon/organisations/known_corporations.lst"));
            organisationPattern.loadTerms(
                    new File(GrobidProperties.getGrobidHomePath()
                            +
                            "/lexicon/organisations/venture_capital.venture_funded_company"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for organisations.", e);
        } catch (IOException e) {
            throw new GrobidResourceException("Cannot add term to matcher, because the lexicon resource file "
                    +
                    "does not exist or cannot be read.", e);
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid Lexicon init.", e);
        }
    }

    /**
     * Loads the organisation-form gazetteer (org closings like "Inc.", "Ltd."). Not loaded
     * by the default {@link #getInstance()}; use {@link Lexicon#builder()} with
     * {@code .withOrgForms()} to enable.
     */
    public void initOrgForms() {
        try {
            orgFormPattern = new FastMatcher(
                    new File(GrobidProperties.getGrobidHomePath() + "/lexicon/organisations/orgClosings.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for organisations.", e);
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid Lexicon init.", e);
        }
    }

    public void initLocations() {
        try {
            locationPattern = new FastMatcher(
                    new File(GrobidProperties.getGrobidHomePath() + "/lexicon/places/location.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for locations.", e);
        }
    }

    public void initPersonTitles() {
        try {
            personTitlePattern = new FastMatcher(
                    new File(GrobidProperties.getGrobidHomePath() + "/lexicon/names/VincentNgPeopleTitles.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for person titles.", e);
        }
    }

    public void initPersonSuffix() {
        try {
            personSuffixPattern = new FastMatcher(
                    new File(GrobidProperties.getGrobidHomePath() + "/lexicon/names/suffix.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for person name suffix.", e);
        }
    }

    public void initFunders() {
        try {
            funderPattern = new FastMatcher(
                    new File(GrobidProperties.getGrobidHomePath() + "/lexicon/organisations/funders.txt"),
                    GrobidAnalyzer.getInstance(), true);
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for funders.", e);
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid Lexicon init.", e);
        }
    }

    public void initResearchInfrastructures() {
        try {
            String infrastructureFilePath = GrobidProperties.getGrobidHomePath()
                    + "/lexicon/organisations/research_infrastructures.txt";
            researchInfrastructurePattern = new FastMatcher(
                    new File(infrastructureFilePath),
                    GrobidAnalyzer.getInstance(), true);
            // store some name mapping
            researchOrganizations = new TreeMap<>();

            String infrastructureMapFilePath = GrobidProperties.getGrobidHomePath()
                    + "/lexicon/organisations/research_infrastructures_map.txt";
            File file = getFile(infrastructureMapFilePath, "infrastructure names");
            InputStream ist = null;
            BufferedReader dis = null;
            try {
                ist = new FileInputStream(file);
                dis = new BufferedReader(new InputStreamReader(ist, StandardCharsets.UTF_8));

                String line;
                while ((line = dis.readLine()) != null) {
                    // read the line
                    line = line.trim();
                    if (line.length() == 0 || line.startsWith("#"))
                        continue;
                    String[] pieces = line.split(";", -1); // -1 for getting empty tokens too
                    if (pieces.length == 3) {
                        if (pieces[0].length() > 0) {

                            if (pieces[1].length() > 0) {
                                OrganizationRecord localInfra = new OrganizationRecord(pieces[0], pieces[1], "en");
                                List<OrganizationRecord> localInfraList = researchOrganizations
                                        .get(pieces[0].toLowerCase());
                                if (localInfraList == null) {
                                    localInfraList = new ArrayList<>();
                                }
                                localInfraList.add(localInfra);
                                researchOrganizations.put(pieces[0].toLowerCase(), localInfraList);
                                researchOrganizations.put(pieces[1].toLowerCase(), localInfraList);
                            }

                            if (pieces[2].length() > 0) {
                                OrganizationRecord localInfra = new OrganizationRecord(pieces[0], pieces[2], "fr");
                                List<OrganizationRecord> localInfraList = researchOrganizations
                                        .get(pieces[0].toLowerCase());
                                if (localInfraList == null) {
                                    localInfraList = new ArrayList<>();
                                }
                                localInfraList.add(localInfra);
                                researchOrganizations.put(pieces[0].toLowerCase(), localInfraList);
                                researchOrganizations.put(pieces[2].toLowerCase(), localInfraList);
                            }
                        }
                    } else {
                        LOGGER.warn("research_infrastructures map file, invalid line format: " + line);
                    }
                }
            } catch (FileNotFoundException e) {
                throw new GrobidException("An exception occurred while running Grobid.", e);
            } catch (IOException e) {
                throw new GrobidException("An exception occurred while running Grobid.", e);
            } finally {
                try {
                    if (ist != null)
                        ist.close();
                    if (dis != null)
                        dis.close();
                } catch (Exception e) {
                    throw new GrobidResourceException("Cannot close all streams.", e);
                }
            }
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for research infrastructure.", e);
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid Lexicon init.", e);
        }
    }

    /**
     * Look-up in first name gazetteer
     */
    public boolean inFirstNames(String s) {
        if (firstNames == null) {
            loadPeople();
        }
        return firstNames.contains(s);
    }

    /**
     * Look-up in last name gazetteer
     */
    public boolean inLastNames(String s) {
        if (lastNames == null) {
            loadPeople();
        }
        return lastNames.contains(s);
    }

    public List<OrganizationRecord> getOrganizationNamingInfo(String name) {
        if (researchOrganizations == null) {
            initResearchInfrastructures();
        }
        return researchOrganizations.get(name.toLowerCase());
    }

    /**
     * Soft look-up in journal name gazetteer with token positions
     */
    public List<OffsetPosition> tokenPositionsJournalNames(String s) {
        if (journalPattern == null) {
            initJournals();
        }
        List<OffsetPosition> results = journalPattern.matchToken(s);
        return results;
    }

    /**
     * Soft look-up in journal name gazetteer for a given list of LayoutToken objects
     * with token positions
     */
    public List<OffsetPosition> tokenPositionsJournalNames(List<LayoutToken> s) {
        if (journalPattern == null) {
            initJournals();
        }
        List<OffsetPosition> results = journalPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in journal abbreviated name gazetteer with token positions
     */
    public List<OffsetPosition> tokenPositionsAbbrevJournalNames(String s) {
        if (abbrevJournalPattern == null) {
            initJournals();
        }
        List<OffsetPosition> results = abbrevJournalPattern.matchToken(s);
        return results;
    }

    /**
     * Soft look-up in journal abbreviated name gazetteer for a given list of LayoutToken objects
     * with token positions
     */
    public List<OffsetPosition> tokenPositionsAbbrevJournalNames(List<LayoutToken> s) {
        if (abbrevJournalPattern == null) {
            initJournals();
        }
        List<OffsetPosition> results = abbrevJournalPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in conference/proceedings name gazetteer with token positions
     */
    public List<OffsetPosition> tokenPositionsConferenceNames(String s) {
        if (conferencePattern == null) {
            initConferences();
        }
        List<OffsetPosition> results = conferencePattern.matchToken(s);
        return results;
    }

    /**
     * Soft look-up in conference/proceedings name gazetteer for a given list of LayoutToken objects
     * with token positions
     */
    public List<OffsetPosition> tokenPositionsConferenceNames(List<LayoutToken> s) {
        if (conferencePattern == null) {
            initConferences();
        }
        List<OffsetPosition> results = conferencePattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in conference/proceedings name gazetteer with token positions
     */
    public List<OffsetPosition> tokenPositionsPublisherNames(String s) {
        if (publisherPattern == null) {
            initPublishers();
        }
        List<OffsetPosition> results = publisherPattern.matchToken(s);
        return results;
    }

    /**
     * Soft look-up in publisher name gazetteer for a given list of LayoutToken objects
     * with token positions
     */
    public List<OffsetPosition> tokenPositionsPublisherNames(List<LayoutToken> s) {
        if (publisherPattern == null) {
            initPublishers();
        }
        List<OffsetPosition> results = publisherPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in collaboration name gazetteer for a given list of LayoutToken objects
     * with token positions
     */
    public List<OffsetPosition> tokenPositionsCollaborationNames(List<LayoutToken> s) {
        if (collaborationPattern == null) {
            initCollaborations();
        }
        List<OffsetPosition> results = collaborationPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Case sensitive look-up in funder name gazetteer for a given list of LayoutToken objects
     * with token positions
     */
    public List<OffsetPosition> tokenPositionsFunderNames(List<LayoutToken> s) {
        if (funderPattern == null) {
            initFunders();
        }
        List<OffsetPosition> results = funderPattern.matchLayoutToken(s, true, true);
        return results;
    }

    /**
     * Case sensitive look-up in research infrastructure name gazetteer for a given list of LayoutToken objects
     * with token positions
     */
    public List<OffsetPosition> tokenPositionsResearchInfrastructureNames(List<LayoutToken> s) {
        if (researchInfrastructurePattern == null) {
            initResearchInfrastructures();
        }
        List<OffsetPosition> results = researchInfrastructurePattern.matchLayoutToken(s, true, true);
        return results;
    }

    /**
     * Soft look-up in city name gazetteer for a given string with token positions
     */
    public List<OffsetPosition> tokenPositionsCityNames(String s) {
        if (cityPattern == null) {
            initCities();
        }
        List<OffsetPosition> results = cityPattern.matchToken(s);
        return results;
    }

    /**
     * Soft look-up in city name gazetteer for a given list of LayoutToken objects
     * with token positions
     */
    public List<OffsetPosition> tokenPositionsCityNames(List<LayoutToken> s) {
        if (cityPattern == null) {
            initCities();
        }
        List<OffsetPosition> results = cityPattern.matchLayoutToken(s);
        return results;
    }

    /** Organisation names **/

    /**
     * Soft look-up in organisation name gazetteer for a given string with token positions
     */
    public List<OffsetPosition> tokenPositionsOrganisationNames(String s) {
        if (organisationPattern == null) {
            initOrganisations();
        }
        List<OffsetPosition> results = organisationPattern.matchToken(s);
        return results;
    }

    /**
     * Soft look-up in organisation name gazetteer for a given list of LayoutToken objects
     * with token positions
     */
    public List<OffsetPosition> tokenPositionsOrganisationNames(List<LayoutToken> s) {
        if (organisationPattern == null) {
            initOrganisations();
        }
        List<OffsetPosition> results = organisationPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in country name gazetteer for a given list of LayoutToken objects
     * with token positions
     */
    public List<OffsetPosition> tokenPositionsCountryNames(List<LayoutToken> s) {
        if (countryPattern == null) {
            loadCountries();
        }
        List<OffsetPosition> results = countryPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in organisation names gazetteer for a string.
     * It return a list of positions referring to the character positions within the string.
     *
     * @param s the input string
     * @return a list of positions referring to the character position in the input string
     */
    public List<OffsetPosition> charPositionsOrganisationNames(String s) {
        if (organisationPattern == null) {
            initOrganisations();
        }
        List<OffsetPosition> results = organisationPattern.matchCharacter(s);
        return results;
    }

    /**
     * Soft look-up in organisation names gazetteer for a tokenize sequence.
     * It return a list of positions referring to the character positions within the input
     * sequence.
     *
     * @param s the input list of LayoutToken
     * @return a list of positions referring to the character position in the input sequence
     */
    public List<OffsetPosition> charPositionsOrganisationNames(List<LayoutToken> s) {
        if (organisationPattern == null) {
            initOrganisations();
        }
        List<OffsetPosition> results = organisationPattern.matchCharacterLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in organisation form name gazetteer for a given string with token positions
     */
    public List<OffsetPosition> tokenPositionsOrgForm(String s) {
        if (orgFormPattern == null) {
            initOrgForms();
        }
        List<OffsetPosition> results = orgFormPattern.matchToken(s);
        return results;
    }

    /**
     * Soft look-up in organisation form name gazetteer for a given list of LayoutToken objects
     * with token positions
     */
    public List<OffsetPosition> tokenPositionsOrgForm(List<LayoutToken> s) {
        if (orgFormPattern == null) {
            initOrgForms();
        }
        List<OffsetPosition> results = orgFormPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in org form names gazetteer for a string.
     * It return a list of positions referring to the character positions within the string.
     *
     * @param s the input string
     * @return a list of positions referring to the character position in the input string
     */
    public List<OffsetPosition> charPositionsOrgForm(String s) {
        if (orgFormPattern == null) {
            initOrgForms();
        }
        List<OffsetPosition> results = orgFormPattern.matchCharacter(s);
        return results;
    }

    /**
     * Soft look-up in org form names gazetteer for a tokenized string.
     * It return a list of positions referring to the character positions within the sequence.
     *
     * @param s the input list of LayoutToken
     * @return a list of positions referring to the character position in the input sequence
     */
    public List<OffsetPosition> charPositionsOrgForm(List<LayoutToken> s) {
        if (orgFormPattern == null) {
            initOrgForms();
        }
        List<OffsetPosition> results = orgFormPattern.matchCharacterLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in location name gazetteer for a given string with token positions
     */
    public List<OffsetPosition> tokenPositionsLocationNames(String s) {
        if (locationPattern == null) {
            initLocations();
        }
        List<OffsetPosition> results = locationPattern.matchToken(s);
        return results;
    }

    /**
     * Soft look-up in location name gazetteer for a given list of LayoutToken objects
     * with token positions
     */
    public List<OffsetPosition> tokenPositionsLocationNames(List<LayoutToken> s) {
        if (locationPattern == null) {
            initLocations();
        }
        List<OffsetPosition> results = locationPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in location name gazetteer for a string, return a list of positions referring
     * to the character positions within the string.
     * <p>
     * For example "The car is in Milan" as Milan is a location, would return OffsetPosition(14,19)
     *
     * @param s the input string
     * @return a list of positions referring to the character position in the input string
     */
    public List<OffsetPosition> charPositionsLocationNames(String s) {
        if (locationPattern == null) {
            initLocations();
        }
        List<OffsetPosition> results = locationPattern.matchCharacter(s);
        return results;
    }

    /**
     * Soft look-up in location name gazetteer for a list of LayoutToken, return a list of
     * positions referring to the character positions in the input sequence.
     * <p>
     * For example "The car is in Milan" as Milan is a location, would return OffsetPosition(14,19)
     *
     * @param s the input list of LayoutToken
     * @return a list of positions referring to the character position in the input sequence
     */
    public List<OffsetPosition> charPositionsLocationNames(List<LayoutToken> s) {
        if (locationPattern == null) {
            initLocations();
        }
        List<OffsetPosition> results = locationPattern.matchCharacterLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in person title gazetteer for a given string with token positions
     */
    public List<OffsetPosition> tokenPositionsPersonTitle(String s) {
        if (personTitlePattern == null) {
            initPersonTitles();
        }
        List<OffsetPosition> results = personTitlePattern.matchToken(s);
        return results;
    }

    /**
     * Soft look-up in person title gazetteer for a given list of LayoutToken objects
     * with token positions
     */
    public List<OffsetPosition> tokenPositionsPersonTitle(List<LayoutToken> s) {
        if (personTitlePattern == null) {
            initPersonTitles();
        }
        List<OffsetPosition> results = personTitlePattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in person name suffix gazetteer for a given list of LayoutToken objects
     * with token positions
     */
    public List<OffsetPosition> tokenPositionsPersonSuffix(List<LayoutToken> s) {
        if (personSuffixPattern == null) {
            initPersonSuffix();
        }
        List<OffsetPosition> results = personSuffixPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in person title name gazetteer for a string.
     * It returns a list of positions referring to the character positions within the string.
     *
     * @param s the input string
     * @return a list of positions referring to the character position in the input string
     */
    public List<OffsetPosition> charPositionsPersonTitle(String s) {
        if (personTitlePattern == null) {
            initPersonTitles();
        }
        List<OffsetPosition> results = personTitlePattern.matchCharacter(s);
        return results;
    }

    /**
     * Soft look-up in person title name gazetteer for a list of LayoutToken.
     * It return a list of positions referring to the character positions in the input
     * sequence.
     *
     * @param s the input list of LayoutToken
     * @return a list of positions referring to the character position in the input sequence
     */
    public List<OffsetPosition> charPositionsPersonTitle(List<LayoutToken> s) {
        if (personTitlePattern == null) {
            initPersonTitles();
        }
        List<OffsetPosition> results = personTitlePattern.matchCharacterLayoutToken(s);
        return results;
    }

    /**
     * Identify in tokenized input the positions of identifier patterns with token positions
     */
    public List<OffsetPosition> tokenPositionsIdentifierPattern(List<LayoutToken> tokens) {
        List<OffsetPosition> result = new ArrayList<OffsetPosition>();
        String text = LayoutTokensUtil.toText(tokens);

        // DOI positions
        result = tokenPositionsDOIPattern(tokens, text);

        // arXiv
        List<OffsetPosition> positions = tokenPositionsArXivPattern(tokens, text);
        result = Utilities.mergePositions(result, positions);

        // ISSN and ISBN
        /*positions = tokenPositionsISSNPattern(tokens);
        result = Utilities.mergePositions(result, positions);
        positions = tokenPositionsISBNPattern(tokens);
        result = Utilities.mergePositions(result, positions);*/

        return result;
    }

    /**
     * Identify in tokenized input the positions of the DOI patterns with token positions
     */
    public List<OffsetPosition> tokenPositionsDOIPattern(List<LayoutToken> tokens, String text) {
        List<OffsetPosition> textResult = new ArrayList<OffsetPosition>();
        Matcher doiMatcher = TextUtilities.DOIPattern.matcher(text);
        while (doiMatcher.find()) {
            textResult.add(new OffsetPosition(doiMatcher.start(), doiMatcher.end()));
        }
        return convertStringOffsetToTokenOffset(textResult, tokens);
    }

    /**
     * Identify in tokenized input the positions of the arXiv identifier patterns
     * with token positions
     */
    public List<OffsetPosition> tokenPositionsArXivPattern(List<LayoutToken> tokens, String text) {
        List<OffsetPosition> textResult = new ArrayList<OffsetPosition>();
        Matcher arXivMatcher = TextUtilities.arXivPattern.matcher(text);
        while (arXivMatcher.find()) {
            //System.out.println(arXivMatcher.start() + " / " + arXivMatcher.end() + " / " + text.substring(arXivMatcher.start(), arXivMatcher.end()));
            textResult.add(new OffsetPosition(arXivMatcher.start(), arXivMatcher.end()));
        }
        return convertStringOffsetToTokenOffset(textResult, tokens);
    }

    /**
     * Identify in tokenized input the positions of ISSN patterns with token positions
     */
    public List<OffsetPosition> tokenPositionsISSNPattern(List<LayoutToken> tokens) {
        List<OffsetPosition> result = new ArrayList<OffsetPosition>();

        // TBD !

        return result;
    }

    /**
     * Identify in tokenized input the positions of ISBN patterns with token positions
     */
    public List<OffsetPosition> tokenPositionsISBNPattern(List<LayoutToken> tokens) {
        List<OffsetPosition> result = new ArrayList<OffsetPosition>();

        // TBD !!

        return result;
    }

    /**
     * Identify in tokenized input the positions of an URL pattern with token positions
     */
    public static List<OffsetPosition> tokenPositionsUrlPattern(List<LayoutToken> tokens) {
        List<OffsetPosition> textResult = characterPositionsUrlPattern(tokens);
        return convertStringOffsetToTokenOffset(textResult, tokens);
    }

    /**
     * Identify in tokenized input the positions of an URL pattern with character positions
     */
    public static List<OffsetPosition> characterPositionsUrlPattern(List<LayoutToken> tokens) {
        String text = LayoutTokensUtil.toText(tokens);
        List<OffsetPosition> textResult = new ArrayList<>();
        Matcher urlMatcher = TextUtilities.urlPattern1.matcher(text);
        while (urlMatcher.find()) {
            textResult.add(new OffsetPosition(urlMatcher.start(), urlMatcher.end()));
        }
        return textResult;
    }

    /**
     * Identify in tokenized input the positions of a URL pattern with character positions,
     * and refine positions based on possible PDF URI annotations.
     * <p>
     * This will produce better quality recognized URL, avoiding missing suffixes and problems
     * with break lines and spaces.
     **/
    public static List<OffsetPosition> characterPositionsUrlPatternWithPdfAnnotations(
            List<LayoutToken> layoutTokens,
            List<PDFAnnotation> pdfAnnotations,
            String text) {

        List<Pair<OffsetPosition, String>> urlTokensPositionsAndDestinations = tokenPositionUrlPatternWithPdfAnnotations(
                layoutTokens,
                pdfAnnotations);

        // We only need the positions here
        List<OffsetPosition> urlTokensPositions = urlTokensPositionsAndDestinations.stream()
                .map(Pair::getLeft)
                .collect(Collectors.toList());

        // We need to adjust the end of the positions to avoid problems with the sublist that is used in the following method
        urlTokensPositions.stream().forEach(o -> o.end += 1);

        // here we need to match the offsetPositions related to the text obtained by the layoutTokens, with the text
        // which may be different (spaces, hyphen, breakline)
        return TextUtilities.matchTokenAndString(layoutTokens, text, urlTokensPositions);
    }

    /**
     * This method returns the token positions in respect of the layout tokens, result is (inclusive, inclusive), so for
     * calling this subList after this method, remember to add +1  to the end offset.
     */
    public static List<Pair<OffsetPosition, String>> tokenPositionUrlPatternWithPdfAnnotations(
            List<LayoutToken> layoutTokens,
            List<PDFAnnotation> pdfAnnotations) {

        List<Pair<OffsetPosition, String>> characterPositionsAndDestinations = characterPositionsUrlPatternWithPdfAnnotations(
                layoutTokens,
                pdfAnnotations);
        List<OffsetPosition> characterPositions = characterPositionsAndDestinations.stream()
                .map(Pair::getLeft)
                .collect(Collectors.toList());
        List<OffsetPosition> tokenOffsetPositionsWithRegex = convertStringOffsetToTokenOffset(
                characterPositions,
                layoutTokens);
        List<Pair<OffsetPosition, String>> tokenOffsetPositionsAndDestinationsWithRegex = IntStream
                .range(0, tokenOffsetPositionsWithRegex.size())
                .mapToObj(
                        i -> Pair.of(
                                tokenOffsetPositionsWithRegex.get(i),
                                characterPositionsAndDestinations.get(i).getRight()))
                .collect(Collectors.toList());

        List<Pair<OffsetPosition, String>> tokenOffsetPositionsFromAnyURLs = tokenPositionsAnyURLMatchingPdfAnnotations(
                layoutTokens,
                pdfAnnotations);

        // Consolidate the two lists
        if (CollectionUtils.isEmpty(tokenOffsetPositionsFromAnyURLs)) {
            return tokenOffsetPositionsAndDestinationsWithRegex;
        } else {
            // We add possible URL that weren't bound to any PDF annotations
            for (Pair<OffsetPosition, String> item : tokenOffsetPositionsAndDestinationsWithRegex) {
                String dest = item.getRight();

                if (dest == null) {
                    // if the destination offsets does not overlap any other offsets, we add it
                    boolean overlaps = tokenOffsetPositionsFromAnyURLs.stream()
                            .anyMatch(existingItem -> existingItem.getLeft().overlaps(item.getLeft()));

                    if (!overlaps) {
                        tokenOffsetPositionsFromAnyURLs.add(item);
                    }
                }
            }
            return tokenOffsetPositionsFromAnyURLs;
        }
    }

    public static OffsetPosition getTokenPositions(int startPos, int endPos, List<LayoutToken> layoutTokens) {
        // token sublist
        int startTokenIndex = -1;
        int endTokensIndex = -1;

        List<LayoutToken> urlTokens = new ArrayList<>();
        int tokenPos = 0;
        int tokenIndex = 0;
        for (LayoutToken localToken : layoutTokens) {
            if (startPos <= tokenPos && (tokenPos + localToken.getText().length() <= endPos)) {
                urlTokens.add(localToken);
                if (startTokenIndex == -1) {
                    startTokenIndex = tokenIndex;
                }
                if (tokenIndex > endTokensIndex) {
                    endTokensIndex = tokenIndex;
                }
            }
            if (tokenPos > endPos) {
                break;
            }
            tokenPos += localToken.getText().length();
            tokenIndex++;
        }

        return new OffsetPosition(startTokenIndex, endTokensIndex);
    }

    public static OffsetPosition getTokenIndexMatchingURLDestination(List<LayoutToken> urlTokens, String destination) {
        String urlString = LayoutTokensUtil.toText(urlTokens);

        String joinedNoSpaces = urlString.replaceAll("\\s", "");
        String destinationNoSpaces = destination.replaceAll("\\s", "");

        // Find the start index in the space-less string
        int destStartNoSpaces = joinedNoSpaces.indexOf(destinationNoSpaces);
        if (destStartNoSpaces == -1) {
            // Not found, handle as needed
            return new OffsetPosition();
        }

        int destEndNoSpaces = destStartNoSpaces + destinationNoSpaces.length();

        // Map to token indices
        int charCount = 0;
        int indexStart = -1, indexEnd = -1;
        for (int i = 0; i < urlTokens.size(); i++) {
            String tokenText = urlTokens.get(i).getText();
            for (int j = 0; j < tokenText.length(); j++) {
                if (!Character.isWhitespace(tokenText.charAt(j))) {
                    if (charCount == destStartNoSpaces && indexStart == -1) {
                        indexStart = i;
                    }
                    if (charCount == destEndNoSpaces - 1) {
                        indexEnd = i;
                    }
                    charCount++;
                }
            }
            if (indexEnd != -1)
                break;
        }
        return new OffsetPosition(indexStart, indexEnd);
    }

    /**
     * This method returns the character offsets in relation to the string obtained by the layout tokens.
     * Notice the absence of the String text parameter.
     */
    public static List<Pair<OffsetPosition, String>> tokenPositionsAnyURLMatchingPdfAnnotations(
            List<LayoutToken> layoutTokens,
            List<PDFAnnotation> pdfAnnotations) {

        List<Integer> urlsInPage = layoutTokens.parallelStream()
                .map(LayoutToken::getPage)
                .distinct()
                .collect(Collectors.toList());

        Map<String, List<PDFAnnotation>> relevantURIAnnotations = pdfAnnotations.parallelStream()
                .filter(
                        a -> urlsInPage.contains(a.getPageNumber())
                                && StringUtils.isNotBlank(a.getDestination())
                                && a.getType().equals(PDFAnnotation.Type.URI))
                .collect(Collectors.groupingBy(PDFAnnotation::getDestination));

        List<PDFAnnotation> mergedAnnotations = new ArrayList<>();

        for (Map.Entry<String, List<PDFAnnotation>> item : relevantURIAnnotations.entrySet()) {
            List<PDFAnnotation> annotations = item.getValue();

            if (annotations.size() <= 1) {
                mergedAnnotations.addAll(annotations);
                continue;
            }

            PDFAnnotation first = annotations.get(0);
            int page = annotations.stream().mapToInt(PDFAnnotation::getPageNumber).min().orElse(first.getPageNumber());

            PDFAnnotation merged = new PDFAnnotation();
            merged.setPageNumber(page);
            merged.setDestination(first.getDestination());
            merged.setType(first.getType());

            merged.setBoundingBoxes(
                    annotations.stream()
                            .map(PDFAnnotation::getBoundingBoxes)
                            .filter(Objects::nonNull)
                            .flatMap(List::stream)
                            .collect(Collectors.toList()));

            mergedAnnotations.add(merged);
        }

        // we calculate the token positions of all the URLs in the layout tokens
        List<Pair<OffsetPosition, String>> urlPositions = new ArrayList<>();
        for (PDFAnnotation annotation : mergedAnnotations) {
            String destination = annotation.getDestination();
            // Identify the tokens covered by the annotation
            List<LayoutToken> urlTokens = layoutTokens.stream()
                    .filter(
                            annotation::cover)
                    .collect(Collectors.toList());

            if (urlTokens.isEmpty()) {
                continue;
            }

            // Refine the URL tokens based on the destination URL from the annotation.
            // Differently from when we recognise the URLs via regex, here we may have to remove characters also in front of the URL.
            String urlString = LayoutTokensUtil.toText(urlTokens);
            String urlStringWithoutSpaces = urlString.replaceAll("\\s", "");

            if (urlStringWithoutSpaces.contains(destination)) {
                // In this case the list of tokens has catches too much, usually this should be limited to a few characters,
                // but we cannot know it for sure.

                int startUrl = urlString.indexOf(destination);
                int endDestinationURL = startUrl + destination.length();
                if (startUrl < 0) {
                    // If we cannot find the destination in the URL string, we try to find it without spaces
                    startUrl = urlStringWithoutSpaces.indexOf(destination);
                    endDestinationURL = startUrl + urlString.length();
                }
                OffsetPosition newTokenPositions = getTokenPositions(startUrl, endDestinationURL, urlTokens);

                if (newTokenPositions.end < 0) {
                    // The difference is within the last token, even if we split the layout tokens, here,
                    // it won't solve the problem so we limit collateral damage.
                    newTokenPositions.end = urlTokens.size() - 1;
                }

                urlTokens = urlTokens.subList(newTokenPositions.start, newTokenPositions.end + 1);
            }

            //Cleanup edges
            if (Iterables.getFirst(urlTokens, new LayoutToken()).getText().endsWith("(")) {
                urlTokens.remove(0);
            }

            if (CollectionUtils.isEmpty(urlTokens)) {
                continue;
            }
            if (Iterables.getLast(urlTokens).getText().endsWith(")")) {
                long openedParenthesis = LayoutTokensUtil.toText(urlTokens).chars().filter(ch -> ch == '(').count();
                long closedParenthesis = LayoutTokensUtil.toText(urlTokens).chars().filter(ch -> ch == ')').count();
                if (openedParenthesis < closedParenthesis) {
                    urlTokens.remove(urlTokens.size() - 1);
                }
            }

            if (CollectionUtils.isEmpty(urlTokens)) {
                continue;
            }

            if (Iterables.getLast(urlTokens).getText().equals(".")) {
                urlTokens.remove(urlTokens.size() - 1);
            }

            if (CollectionUtils.isEmpty(urlTokens)) {
                continue;
            }

            //Find the token index positions in the layoutTokens object
            int startTokenIndex = layoutTokens.indexOf(urlTokens.get(0));
            int endTokenIndex = layoutTokens.indexOf(urlTokens.get(urlTokens.size() - 1));
            OffsetPosition resultPosition = new OffsetPosition(startTokenIndex, endTokenIndex);

            urlPositions.add(Pair.of(resultPosition, destination));
        }

        return urlPositions;
    }

    /**
     * This method returns the character offsets in relation to the string obtained by the layout tokens.
     * Notice the absence of the String text parameter.
     */
    public static List<Pair<OffsetPosition, String>> characterPositionsUrlPatternWithPdfAnnotations(
            List<LayoutToken> layoutTokens,
            List<PDFAnnotation> pdfAnnotations) {

        List<OffsetPosition> urlPositions = Lexicon.characterPositionsUrlPattern(layoutTokens);
        List<Pair<OffsetPosition, String>> resultPositions = new ArrayList<>();

        // Do we need to extend the url position based on additional position of the corresponding
        // PDF annotation?
        for (OffsetPosition urlPosition : urlPositions) {
            int startPos = urlPosition.start;
            int endPos = urlPosition.end;

            OffsetPosition tokenPositions = getTokenPositions(startPos, endPos, layoutTokens);

            int startTokenIndex = tokenPositions.start;
            int endTokensIndex = tokenPositions.end;

            // There are no token that matches the character offsets, this may happen rarely when
            // the character offset falls in the middle of a token, this is likely due to a badly
            // constructed PDF document
            if (startTokenIndex < 0 || endTokensIndex < 0) {
                continue;
            }

            List<LayoutToken> urlTokens = new ArrayList<>(layoutTokens.subList(startTokenIndex, endTokensIndex + 1));

            String urlString = LayoutTokensUtil.toText(urlTokens);

            // This variable is used to adjust the last token index
            int correctedLastTokenIndex = 0;
            PDFAnnotation targetAnnotation = null;
            if (CollectionUtils.isNotEmpty(urlTokens)) {
                LayoutToken lastToken = urlTokens.get(urlTokens.size() - 1);
                if (pdfAnnotations != null) {
                    targetAnnotation = matchPdfAnnotationsBasedOnCoordinatesDestinationOrLastTokens(
                            pdfAnnotations,
                            urlTokens);

                    correctedLastTokenIndex = urlTokens.size() - 1;

                    // If we cannot match, maybe the regex got some characters too much, e.g. dots, parenthesis,etc..
                    // so we try to check the tokens before the last only if the n-token is a single special characters
                    // TODO: Stop after a few characters, instead of when reaching zero?
                    if (targetAnnotation == null) {
                        String lastTokenText = lastToken.getText();
                        int index = urlTokens.size() - 1;
                        // The error should be within a few characters, so we stop if the token length is greater than 1
                        while (index > 0 && lastTokenText.length() == 1
                                && !Character.isLetterOrDigit(lastTokenText.charAt(0)) && targetAnnotation == null) {
                            index -= 1;
                            LayoutToken finalLastToken1 = urlTokens.get(index);
                            targetAnnotation = matchPdfAnnotationsBasedOnCoordinatesDestinationOrLastTokens(
                                    pdfAnnotations,
                                    urlTokens);

                            correctedLastTokenIndex = index;
                        }
                    }
                }
            }

            String destination = null;

            if (targetAnnotation != null) {
                destination = targetAnnotation.getDestination();

                int destinationPos = 0;
                if (urlString.replaceAll("\\s", "").equals(destination)) {
                    // Nothing to do here, we ignore the correctedLastTokenIndex because the regex got everything we need
                } else if (destination.contains(urlString)
                        || destination.contains(urlString.replaceAll("\\s", ""))
                        || destination.contains(StringUtils.stripEnd(urlString, "-"))) {
                    //In this case the regex did not catch all the URL, so we need to extend it using the
                    // destination URL from the annotation
                    destinationPos = destination.indexOf(urlString) + urlString.length();
                    if (endTokensIndex < layoutTokens.size() - 1) {
                        int additionalSpaces = 0;
                        int additionalTokens = 0;
                        for (int j = endTokensIndex + 1; j < layoutTokens.size(); j++) {
                            LayoutToken nextToken = layoutTokens.get(j);

                            if ("\n".equals(nextToken.getText()) ||
                                    " ".equals(nextToken.getText()) ||
                                    nextToken.getText().isEmpty()) {
                                endPos += nextToken.getText().length();
                                additionalSpaces += nextToken.getText().length();
                                additionalTokens += 1;
                                urlTokens.add(nextToken);
                                continue;
                            }

                            int pos = destination.indexOf(nextToken.getText(), destinationPos);
                            if (pos != -1) {
                                if (additionalTokens > 0) {
                                    additionalSpaces = 0;
                                    additionalTokens = 0;
                                }
                                endPos += nextToken.getText().length();
                                destinationPos = pos + nextToken.getText().length();
                                urlTokens.add(nextToken);
                            } else {
                                break;
                            }
                        }

                        // We don't match anything after, but we added spaces, we should take them back
                        if (additionalTokens > 0) {
                            urlTokens = urlTokens.subList(0, urlTokens.size() - additionalTokens);
                            endPos -= additionalSpaces;
                        }
                    }
                } else if (urlString.contains(destination) || urlString.replaceAll("\\s", "").contains(destination)) {
                    // In this case the regex has catches too much, usually this should be limited to a few characters,
                    // but we cannot know it for sure. Here we first find the difference between the destination and the
                    // urlString, and then we identify the tokens in which this "difference" is falling,
                    // and we remove them from the urlTokens.

                    int startCharDifference = urlString.indexOf(destination) + destination.length();
                    String difference = urlString.substring(startCharDifference);
                    OffsetPosition newTokenPositions = getTokenPositions(
                            startCharDifference,
                            urlString.length(),
                            urlTokens);

                    if (newTokenPositions.end < 0) {
                        // The difference is within the last token, even if we split the layout tokens, here,
                        // it won't solve the problem so we limit collateral damage.
                        // At some point we could return the destination containing the clean URL to fill up the
                        // "target" attribute in the TEI
                        newTokenPositions.end = urlTokens.size() - 1;
                    }

                    urlTokens = urlTokens.subList(0, newTokenPositions.end);
                    endPos = startPos + LayoutTokensUtil.toText(urlTokens).length();
                } else {
                    // In this case the regex has catches too much, usually this should be limited to a few characters
                    // NOTE: Here it might not contain the URL string just because of space
                    // TODO: stop after a few characters instead of reaching zero?

                    urlTokens = urlTokens.subList(0, correctedLastTokenIndex + 1);
                    endPos = startPos + LayoutTokensUtil.toText(urlTokens).length();
                }
            }

            // finally avoid ending a URL by a dot, because it can harm the sentence segmentation
            if (Iterables.getLast(urlTokens).getText().endsWith(".")) {
                endPos = endPos - 1;
            } else if (Iterables.getLast(urlTokens).getText().endsWith(")")) {
                long openedParenthesis = LayoutTokensUtil.toText(urlTokens).chars().filter(ch -> ch == '(').count();
                long closedParenthesis = LayoutTokensUtil.toText(urlTokens).chars().filter(ch -> ch == ')').count();
                if (openedParenthesis < closedParenthesis) {
                    endPos = endPos - 1;
                }
            }

            OffsetPosition position = new OffsetPosition();
            position.start = startPos;
            position.end = endPos;
            // LF: if the destination is null, we will use the URL string int he tei construction
            resultPositions.add(Pair.of(position, destination));
        }
        return resultPositions;
    }

    /**
     * Find and return the PDFAnnotation that best matches the given URL tokens, based on
     * their coordinates, destination, or the last tokens in the sequence.
     * This helps refine the association between detected URLs in the text and
     * their corresponding PDF annotations, improving the accuracy of URL extraction
     * from PDF documents.
     */
    @Nullable
    private static PDFAnnotation matchPdfAnnotationsBasedOnCoordinatesDestinationOrLastTokens(
            List<PDFAnnotation> pdfAnnotations,
            List<LayoutToken> urlTokens) {

        LayoutToken lastToken = urlTokens.get(urlTokens.size() - 1);
        String urlString = LayoutTokensUtil.toText(urlTokens);

        List<PDFAnnotation> possibleTargetAnnotations = pdfAnnotations.stream()
                .filter(
                        pdfAnnotation -> pdfAnnotation.getType() != null
                                && pdfAnnotation.getType() == PDFAnnotation.Type.URI
                                && pdfAnnotation.cover(lastToken))
                .collect(Collectors.toList());

        PDFAnnotation targetAnnotation;
        if (possibleTargetAnnotations.size() > 1) {
            possibleTargetAnnotations = possibleTargetAnnotations.stream()
                    .filter(pdfAnnotation -> pdfAnnotation.getDestination().contains(urlString))
                    .collect(Collectors.toList());

            if (possibleTargetAnnotations.size() > 1) {
                // If the lastToken is any of ./:_ we should add the token before
                int index = urlTokens.size() - 1;
                if (urlTokens.size() > 1 && lastToken.getText().matches("[.:_\\-/]")) {
                    index -= 1;
                }

                while (index > 0 && possibleTargetAnnotations.size() > 1) {
                    final String lastTokenText2 = LayoutTokensUtil
                            .toText(urlTokens.subList(index - 1, urlTokens.size()));

                    possibleTargetAnnotations = possibleTargetAnnotations.stream()
                            .filter(pdfAnnotation -> pdfAnnotation.getDestination().contains(lastTokenText2))
                            .collect(Collectors.toList());
                    index--;
                }

                targetAnnotation = possibleTargetAnnotations.stream()
                        .findFirst()
                        .orElse(null);

            } else {
                targetAnnotation = possibleTargetAnnotations.stream()
                        .findFirst()
                        .orElse(null);
            }

        } else {
            targetAnnotation = possibleTargetAnnotations.stream()
                    .findFirst()
                    .orElse(null);
        }

        return targetAnnotation;
    }

    /**
     * Identify in tokenized input the positions of an email address pattern with token positions
     */
    public List<OffsetPosition> tokenPositionsEmailPattern(List<LayoutToken> tokens) {
        //List<OffsetPosition> result = new ArrayList<OffsetPosition>();
        String text = LayoutTokensUtil.toText(tokens);
        if (text.indexOf("@") == -1)
            return new ArrayList<OffsetPosition>();
        List<OffsetPosition> textResult = new ArrayList<OffsetPosition>();
        Matcher emailMatcher = TextUtilities.emailPattern.matcher(text);
        while (emailMatcher.find()) {
            //System.out.println(urlMatcher.start() + " / " + urlMatcher.end() + " / " + text.substring(urlMatcher.start(), urlMatcher.end()));
            textResult.add(new OffsetPosition(emailMatcher.start(), emailMatcher.end()));
        }
        return convertStringOffsetToTokenOffset(textResult, tokens);
    }

}
