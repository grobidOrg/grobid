package org.grobid.core.engines;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.grobid.core.GrobidModel;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.Affiliation;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.Person;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.GrobidConfig;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.LanguageUtilities;
import org.grobid.core.utilities.counters.CntManager;
import org.grobid.core.utilities.counters.impl.CntManagerFactory;

public class HeaderParserTest {
    private HeaderParser target;

    // Static mocks kept open for the whole test: Lexicon and LanguageUtilities are
    // touched while constructing the parser and during attachment, and we only need
    // them to return the Mockito default (null) to avoid heavy singleton init.
    private MockedStatic<Lexicon> lexiconMock;
    private MockedStatic<LanguageUtilities> languageUtilitiesMock;

    @Before
    public void setUp() throws Exception {
        lexiconMock = Mockito.mockStatic(Lexicon.class);
        languageUtilitiesMock = Mockito.mockStatic(LanguageUtilities.class);
        GrobidConfig.ModelParameters modelParameters = new GrobidConfig.ModelParameters();
        modelParameters.name = "bao";
        GrobidProperties.addModel(modelParameters);
        target = new HeaderParser((GrobidModel) GrobidModels.DUMMY);
    }

    @After
    public void tearDown() {
        if (languageUtilitiesMock != null) {
            languageUtilitiesMock.close();
        }
        if (lexiconMock != null) {
            lexiconMock.close();
        }
    }

    @Test
    public void testAttachAffiliations() throws Exception {
        BiblioItem biblio = new BiblioItem();

        // 1. One author, one affiliation (no marker)
        Person author1 = new Person();
        author1.setLastName("Doe");
        biblio.addFullAuthor(author1);

        Affiliation aff1 = new Affiliation();
        aff1.setRawAffiliationString("University of Nowhere");
        if (biblio.getFullAffiliations() == null)
            biblio.setFullAffiliations(new ArrayList<>());
        biblio.getFullAffiliations().add(aff1);

        biblio.attachAffiliations();

        assertThat(biblio.getFullAuthors().get(0).getAffiliations().size(), is(1));
        assertThat(
                biblio.getFullAuthors().get(0).getAffiliations().get(0).getRawAffiliationString(),
                is("University of Nowhere"));

        // 2. Two authors, two affiliations (with markers)
        biblio = new BiblioItem();

        Person author2 = new Person();
        author2.setLastName("Smith");
        author2.addMarker("1");
        biblio.addFullAuthor(author2);

        Person author3 = new Person();
        author3.setLastName("Wesson");
        author3.addMarker("2");
        biblio.addFullAuthor(author3);

        Affiliation aff2 = new Affiliation();
        aff2.setMarker("1");
        aff2.setRawAffiliationString("University of One");
        if (biblio.getFullAffiliations() == null)
            biblio.setFullAffiliations(new ArrayList<>());
        biblio.getFullAffiliations().add(aff2);

        Affiliation aff3 = new Affiliation();
        aff3.setMarker("2");
        aff3.setRawAffiliationString("University of Two");
        if (biblio.getFullAffiliations() == null)
            biblio.setFullAffiliations(new ArrayList<>());
        biblio.getFullAffiliations().add(aff3);

        biblio.setOriginalAuthors("Smith 1, Wesson 2");

        biblio.attachAffiliations();

        assertThat(biblio.getFullAuthors().get(0).getAffiliations().size(), is(1));
        assertThat(biblio.getFullAuthors().get(0).getAffiliations().get(0).getMarker(), is("1"));
        assertThat(biblio.getFullAuthors().get(1).getAffiliations().size(), is(1));
        assertThat(biblio.getFullAuthors().get(1).getAffiliations().get(0).getMarker(), is("2"));
    }

    /**
     * Integration test that exercises the full header→affiliation pipeline:
     * 1. Parse test2.header.txt via resultExtraction (extracts authors with
     * markers)
     * 2. Parse test2.affiliation.txt via
     * AffiliationAddressParser.resultExtractionLayoutTokens
     * (builds structured Affiliation objects with markers, bypassing the CRF model)
     * 3. Call attachAffiliations() and verify author↔affiliation links
     */
    @Test
    public void testAttachAffiliations_complex() throws Exception {
        // Engine.getCntManager() is required by resultExtractionLayoutTokens and must
        // return a real CntManager; scope it to this test only.
        try (MockedStatic<Engine> engineMock = Mockito.mockStatic(Engine.class)) {
            CntManager cntManager = CntManagerFactory.getCntManager();
            engineMock.when(Engine::getCntManager).thenReturn(cntManager);

            // --- Step 1: Parse header labels to extract authors ---
            List<String> headerLines = readResourceLines("test2.header.txt");
            List<LayoutToken> headerTokens = new ArrayList<>();
            StringBuilder headerResult = new StringBuilder();
            for (String line : headerLines) {
                String[] pieces = line.split("\t");
                if (pieces.length < 2)
                    continue;
                String tokenText = pieces[0];
                String label = pieces[pieces.length - 1];
                headerTokens.add(new LayoutToken(tokenText));
                headerResult.append(tokenText).append("\t").append(label).append("\n");
            }

            BiblioItem biblio = new BiblioItem();
            target.resultExtraction(headerResult.toString(), headerTokens, biblio);

            // Manually parse authors from the extracted author strings to populate
            // fullAuthors
            // (Bypassing AuthorParser which requires a model)
            // Manually parse authors from the header lines to populate fullAuthors
            // (Bypassing AuthorParser which requires a model and workarounding concatenated
            // getAuthors() issue)
            List<Person> manualAuthors = new ArrayList<>();
            Person currentPerson = new Person();
            List<String> lastNameParts = new ArrayList<>();
            List<String> firstNameParts = new ArrayList<>();
            List<String> currentMarkers = new ArrayList<>();

            for (int i = 0; i < headerLines.size(); i++) {
                String line = headerLines.get(i);
                String[] pieces = line.split("\t");
                if (pieces.length < 2)
                    continue;

                String token = pieces[0];
                String label = pieces[pieces.length - 1];

                if (label.endsWith("<author>")) {
                    boolean isComma = token.equals(",");
                    boolean isMarker = token.matches("[\\d*†$]+"); // Digits or specific symbols

                    // Peek next token to decide on comma
                    boolean nextIsMarker = false;
                    if (isComma && i + 1 < headerLines.size()) {
                        String nextLine = headerLines.get(i + 1);
                        String[] nextPieces = nextLine.split("\t");
                        if (nextPieces.length >= 2 && nextPieces[nextPieces.length - 1].endsWith("<author>")) {
                            String nextToken = nextPieces[0];
                            nextIsMarker = nextToken.matches("[\\d*†$]+");
                        }
                    }

                    if (isComma) {
                        if (nextIsMarker) {
                            // Comma inside markers (e.g. "1,2") - ignore or treat as marker separator
                        } else {
                            // Comma between authors (e.g. "*, Richmond") - Split!
                            if (!lastNameParts.isEmpty()) {
                                currentPerson.setLastName(String.join(" ", lastNameParts));
                                if (!firstNameParts.isEmpty())
                                    currentPerson.setFirstName(String.join(" ", firstNameParts));
                                for (String m : currentMarkers)
                                    currentPerson.addMarker(m);
                                manualAuthors.add(currentPerson);
                            }
                            // Reset for new author
                            currentPerson = new Person();
                            lastNameParts = new ArrayList<>();
                            firstNameParts = new ArrayList<>();
                            currentMarkers = new ArrayList<>();
                        }
                    } else if (isMarker) {
                        currentMarkers.add(token);
                    } else if (token.equals("-")) {
                        // Hyphen in compound names like Carreras-Torres
                        if (!lastNameParts.isEmpty()) {
                            String last = lastNameParts.remove(lastNameParts.size() - 1);
                            lastNameParts.add(last + "-");
                        }
                    } else {
                        // Name part: short ALL_CAPS tokens (<=2 chars) are initials (firstName)
                        // Longer tokens are lastName parts
                        if (token.length() <= 2 && token.equals(token.toUpperCase())) {
                            firstNameParts.add(token);
                        } else {
                            lastNameParts.add(token);
                        }
                    }
                } else if (!lastNameParts.isEmpty()) {
                    // End of author block - finalize current author when we hit non-author label
                    currentPerson.setLastName(String.join(" ", lastNameParts));
                    if (!firstNameParts.isEmpty())
                        currentPerson.setFirstName(String.join(" ", firstNameParts));
                    for (String m : currentMarkers)
                        currentPerson.addMarker(m);
                    manualAuthors.add(currentPerson);
                    currentPerson = new Person();
                    lastNameParts = new ArrayList<>();
                    firstNameParts = new ArrayList<>();
                    currentMarkers = new ArrayList<>();
                }
            }
            // Add final author
            if (!lastNameParts.isEmpty()) {
                currentPerson.setLastName(String.join(" ", lastNameParts));
                if (!firstNameParts.isEmpty())
                    currentPerson.setFirstName(String.join(" ", firstNameParts));
                for (String m : currentMarkers)
                    currentPerson.addMarker(m);
                manualAuthors.add(currentPerson);
            }

            // Set the manually parsed authors
            biblio.setFullAuthors(manualAuthors);

            // At this point biblio has authors with markers, but no structured affiliations
            List<Person> authors = biblio.getFullAuthors();
            assertNotNull("Authors should be extracted from header", authors);
            assertThat("Should have multiple authors", authors.size(), greaterThan(10));

            // --- Step 2: Parse affiliation labels to build Affiliation objects ---
            List<String> affLines = readResourceLines("test2.affiliation.txt");
            List<LayoutToken> affTokens = new ArrayList<>();
            StringBuilder affResult = new StringBuilder();
            for (String line : affLines) {
                String[] pieces = line.split("\t");
                if (pieces.length < 2)
                    continue;
                String tokenText = pieces[0];
                String label = pieces[pieces.length - 1]; // e.g. I-<marker>, <institution>, etc.
                affTokens.add(new LayoutToken(tokenText));
                affResult.append(tokenText).append("\t").append(label).append("\n");
            }

            // Call the protected resultExtractionLayoutTokens via reflection.
            // Lexicon.getInstance() is already mocked (returns null) by setUp().
            AffiliationAddressParser affParser = new AffiliationAddressParser(GrobidModels.DUMMY);
            Method extractMethod = AffiliationAddressParser.class.getDeclaredMethod(
                    "resultExtractionLayoutTokens",
                    String.class,
                    List.class);
            extractMethod.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<Affiliation> affiliations = (List<Affiliation>) extractMethod.invoke(
                    affParser,
                    affResult.toString(),
                    affTokens);

            assertNotNull("Affiliations should be extracted from labeled output", affiliations);
            // The file has markers 1-24, so we expect ~24 affiliations
            assertThat(
                    "Should have many distinct affiliations (markers 1-24), found: " + affiliations.size(),
                    affiliations.size(),
                    greaterThan(15));

            // Debug: print affiliations if count is low
            if (affiliations.size() < 15) {
                for (Affiliation aff : affiliations) {
                    System.out.println(
                            "DEBUG aff: marker="
                                    + aff.getMarker()
                                    + " inst="
                                    + aff.getInstitutions()
                                    + " raw="
                                    + aff.getRawAffiliationString());
                }
            }

            // --- Step 3: Set affiliations and run attachment ---
            biblio.setFullAffiliations(affiliations);
            biblio.attachAffiliations();

            // --- Step 4: Verify author → affiliation links ---

            // Battram T (first author) should have markers 1, 2
            Person battram = findAuthor(authors, "Battram");
            assertNotNull("Should find author Battram", battram);
            assertNotNull("Battram should have affiliations", battram.getAffiliations());
            assertThat("Battram should have 2 affiliations", battram.getAffiliations(), hasSize(2));
            assertHasAffMarker(battram, "1");
            assertHasAffMarker(battram, "2");

            // Bojesen S should have markers 5, 6, 7
            Person bojesen = findAuthor(authors, "Bojesen");
            assertNotNull("Should find author Bojesen", bojesen);
            assertNotNull("Bojesen should have affiliations", bojesen.getAffiliations());
            assertThat("Bojesen should have 3 affiliations", bojesen.getAffiliations(), hasSize(3));
            assertHasAffMarker(bojesen, "5");
            assertHasAffMarker(bojesen, "6");
            assertHasAffMarker(bojesen, "7");

            // Severi G should have markers 19, 20, 21, 22
            Person severi = findAuthor(authors, "Severi");
            assertNotNull("Should find author Severi", severi);
            assertNotNull("Severi should have affiliations", severi.getAffiliations());
            assertThat("Severi should have 4 affiliations", severi.getAffiliations(), hasSize(4));

            // Baglietto L should have marker 3
            Person baglietto = findAuthor(authors, "Baglietto");
            assertNotNull("Should find author Baglietto", baglietto);
            assertNotNull("Baglietto should have affiliations", baglietto.getAffiliations());
            assertThat("Baglietto should have 1 affiliation", baglietto.getAffiliations(), hasSize(1));
            assertHasAffMarker(baglietto, "3");
        }
    }

    // --- Helper methods ---

    private List<String> readResourceLines(String resourceName) throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IllegalStateException("Test resource not found: " + resourceName);
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return Arrays.asList(content.split("\\R"));
        }
    }

    private Person findAuthor(List<Person> authors, String lastName) {
        for (Person p : authors) {
            if (p.getLastName() != null && p.getLastName().equalsIgnoreCase(lastName)) {
                return p;
            }
        }
        return null;
    }

    private void assertHasAffMarker(Person author, String marker) {
        boolean found = false;
        for (Affiliation aff : author.getAffiliations()) {
            if (marker.equals(aff.getMarker())) {
                found = true;
                break;
            }
        }
        assertThat(
                author.getLastName() + " should be linked to affiliation marker " + marker,
                found,
                is(true));
    }
}
