package org.grobid.core.data;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class PersonTest {

    private Person person;

    @Before
    public void setUp() {
        person = new Person();
    }

    @Test
    public void testSetFirstName() {
        person.setFirstName("John");
        assertThat(person.getFirstName(), is("John"));
    }

    @Test
    public void testSetLastName() {
        person.setLastName("Doe");
        assertThat(person.getLastName(), is("Doe"));
    }

    @Test
    public void testSetMiddleName() {
        person.setMiddleName("Michael");
        assertThat(person.getMiddleName(), is("Michael"));
    }

    @Test
    public void testSetRawName() {
        person.setRawName("John M. Doe");
        assertThat(person.getRawName(), is("John M. Doe"));
    }

    @Test
    public void testSetTitle_withParentheses_shouldRemoveThem() {
        person.setTitle("(Prof.)");
        assertThat(person.getTitle(), is("Prof."));
    }

    @Test
    public void testSetTitle_withMultipleParentheses_shouldRemoveAll() {
        person.setTitle("((Dr.))");
        assertThat(person.getTitle(), is("Dr."));
    }

    @Test
    public void testSetTitle_withoutParentheses() {
        person.setTitle("Dr.");
        assertThat(person.getTitle(), is("Dr."));
    }

    @Test
    public void testSetSuffix() {
        person.setSuffix("Jr.");
        assertThat(person.getSuffix(), is("Jr."));
    }

    @Test
    public void testSetCorresp() {
        person.setCorresp(true);
        assertThat(person.getCorresp(), is(true));
    }

    @Test
    public void testSetORCID_withHttpPrefix_shouldRemovePrefix() {
        person.setORCID("http://orcid.org/0000-0001-2345-6789");
        assertThat(person.getORCID(), is("0000-0001-2345-6789"));
    }

    @Test
    public void testSetORCID_withHttpsPrefix_shouldRemovePrefix() {
        person.setORCID("https://orcid.org/0000-0001-2345-6789");
        assertThat(person.getORCID(), is("0000-0001-2345-6789"));
    }

    @Test
    public void testSetORCID_withoutPrefix() {
        person.setORCID("0000-0001-2345-6789");
        assertThat(person.getORCID(), is("0000-0001-2345-6789"));
    }

    @Test
    public void testSetORCID_null_shouldNotThrow() {
        person.setORCID(null);
        assertThat(person.getORCID(), is(nullValue()));
    }

    @Test
    public void testSetEmail() {
        person.setEmail("john.doe@example.com");
        assertThat(person.getEmail(), is("john.doe@example.com"));
    }

    @Test
    public void testNotNull_allFieldsNull_shouldReturnFalse() {
        assertThat(person.notNull(), is(false));
    }

    @Test
    public void testNotNull_withFirstName_shouldReturnTrue() {
        person.setFirstName("John");
        assertThat(person.notNull(), is(true));
    }

    @Test
    public void testNotNull_withLastName_shouldReturnTrue() {
        person.setLastName("Doe");
        assertThat(person.notNull(), is(true));
    }

    @Test
    public void testNotNull_withMiddleName_shouldReturnTrue() {
        person.setMiddleName("Michael");
        assertThat(person.notNull(), is(true));
    }

    @Test
    public void testNotNull_withTitle_shouldReturnTrue() {
        person.setTitle("Dr.");
        assertThat(person.notNull(), is(true));
    }

    @Test
    public void testAddAffiliationMarker() {
        person.addAffiliationMarker("1");
        person.addAffiliationMarker("2");

        List<String> markers = person.getAffiliationMarkers();
        assertThat(markers, hasSize(2));
        assertThat(markers.get(0), is("1"));
        assertThat(markers.get(1), is("2"));
    }

    @Test
    public void testAddMarker_shouldRemoveSpaces() {
        person.addMarker("1 2");

        List<String> markers = person.getMarkers();
        assertThat(markers, hasSize(1));
        assertThat(markers.get(0), is("12"));
    }

    @Test
    public void testAddAffiliation() {
        Affiliation affiliation = new Affiliation();
        affiliation.setName("MIT");

        person.addAffiliation(affiliation);

        List<Affiliation> affiliations = person.getAffiliations();
        assertThat(affiliations, hasSize(1));
        assertThat(affiliations.get(0).getName(), is("MIT"));
    }

    @Test
    public void testClonePerson() {
        person.setFirstName("John");
        person.setLastName("Doe");
        person.setMiddleName("Michael");
        person.setEmail("john@example.com");
        person.setORCID("0000-0001-2345-6789");
        person.setCorresp(true);

        Person cloned = person.clonePerson();

        assertThat(cloned.getFirstName(), is("John"));
        assertThat(cloned.getLastName(), is("Doe"));
        assertThat(cloned.getMiddleName(), is("Michael"));
        assertThat(cloned.getEmail(), is("john@example.com"));
        assertThat(cloned.getORCID(), is("0000-0001-2345-6789"));
        assertThat(cloned.getCorresp(), is(true));
    }

    @Test
    public void testSetAffiliationBlocks() {
        List<String> blocks = Arrays.asList("block1", "block2");
        person.setAffiliationBlocks(blocks);

        assertThat(person.getAffiliationBlocks(), is(blocks));
    }

    @Test
    public void testAddAffiliationBlocks() {
        person.addAffiliationBlocks("block1");
        person.addAffiliationBlocks("block2");

        List<String> blocks = person.getAffiliationBlocks();
        assertThat(blocks, hasSize(2));
    }

    @Test
    public void testSetMarkers() {
        List<String> markers = Arrays.asList("1", "2", "3");
        person.setMarkers(markers);

        assertThat(person.getMarkers(), is(markers));
    }

    @Test
    public void testSetAffiliationMarkers() {
        List<String> markers = Arrays.asList("a", "b");
        person.setAffiliationMarkers(markers);

        assertThat(person.getAffiliationMarkers(), is(markers));
    }

    @Test
    public void testSetAffiliations() {
        Affiliation aff1 = new Affiliation();
        aff1.setName("MIT");
        Affiliation aff2 = new Affiliation();
        aff2.setName("Stanford");

        List<Affiliation> affiliations = Arrays.asList(aff1, aff2);
        person.setAffiliations(affiliations);

        assertThat(person.getAffiliations(), hasSize(2));
        assertThat(person.getAffiliations().get(0).getName(), is("MIT"));
        assertThat(person.getAffiliations().get(1).getName(), is("Stanford"));
    }
}

