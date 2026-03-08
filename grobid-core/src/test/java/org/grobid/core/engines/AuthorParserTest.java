package org.grobid.core.engines;

import org.grobid.core.data.Person;
import org.grobid.core.main.LibraryLoader;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AuthorParserTest {

    @BeforeClass
    public static void init() {
        LibraryLoader.load();
    }

    @Test
    public void testEtAlCitation() throws Exception {
        AuthorParser parser = new AuthorParser();
        String input = "Smith, J. et al.";
        List<Person> persons = parser.processingCitation(input);

        assertNotNull(persons);
        assertFalse("Output should not contain 'et al'", persons.toString().contains("et al"));
    }

    @Test
    public void testEtAlHeader() throws Exception {
        AuthorParser parser = new AuthorParser();
        String input = "Smith, J. et al.";
        List<Person> persons = parser.processingHeader(input);
        assertNotNull(persons);
        assertFalse("Output should not contain 'et al'", persons.toString().contains("et al"));
    }

    @Test
    public void testEtAlWithGarbage() throws Exception {
        AuthorParser parser = new AuthorParser();
        String input = "Smith, J. et al. some garbage text here";
        List<Person> persons = parser.processingCitation(input);

        assertNotNull(persons);
        assertFalse("Output should not contain 'et al'", persons.toString().contains("et al"));
        assertFalse("Output should not contain trailing garbage text", persons.toString().contains("garbage"));
    }

    @Test
    public void testEtAlWithLeadingWhitespace() throws Exception {
        AuthorParser parser = new AuthorParser();
        String input = "  Smith, J. et al.";
        List<Person> persons = parser.processingCitation(input);

        assertNotNull(persons);
        assertFalse("Output should not contain 'et al'", persons.toString().contains("et al"));
    }

    @Test
    public void testNoEtAl() throws Exception {
        AuthorParser parser = new AuthorParser();
        String input = "Smith, J.";
        List<Person> persons = parser.processingCitation(input);
        assertNotNull(persons);
        // Should contain Smith
    }
}
