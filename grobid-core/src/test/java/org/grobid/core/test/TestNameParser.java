package org.grobid.core.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.grobid.core.data.Person;
import org.grobid.core.factory.GrobidFactory;
import org.junit.AfterClass;
import org.junit.Test;

public class TestNameParser extends EngineTest {

    @AfterClass
    public static void tearDown() {
        GrobidFactory.reset();
    }

    @Test
    public void testNameParserHeader() throws Exception {
        String authorSequence1 = "José-María Carazo, Alberto Pascual-Montano";
        List<Person> res = engine.processAuthorsHeader(authorSequence1);
        assertNotNull(res);
        assertEquals(2, res.size());
        if (res.size() > 0) {
            assertThat(res.get(0).getFirstName(), is("José-María"));
            assertThat(res.get(0).getLastName(), is("Carazo"));
        }
        if (res.size() > 1) {
            assertThat(res.get(1).getFirstName(), is("Alberto"));
            assertThat(res.get(1).getLastName(), is("Pascual-Montano"));
        }

        String authorSequence2 = "Farzaneh Sarafraz*, James M. Eales*, Reza Mohammadi, Jonathan Dickerson, David Robertson, Goran Nenadic*";
        res = engine.processAuthorsHeader(authorSequence2);
        assertNotNull(res);
        assertEquals(6, res.size());
        if (res.size() > 0) {
            assertThat(res.get(0).getFirstName(), is("Farzaneh"));
            assertThat(res.get(0).getLastName(), is("Sarafraz"));
        }
        if (res.size() > 1) {
            assertThat(res.get(1).getFirstName(), is("James"));
            assertThat(res.get(1).getMiddleName(), is("M"));
            assertThat(res.get(1).getLastName(), is("Eales"));
        }
        if (res.size() > 2) {
            assertThat(res.get(2).getFirstName(), is("Reza"));
            assertThat(res.get(2).getLastName(), is("Mohammadi"));
        }

        String authorSequence3 = "KARL-HEINZ HÖCKER";
        res = engine.processAuthorsHeader(authorSequence3);
        assertNotNull(res);
        if (res != null) {
            // assertEquals(1, res.size());
            if (res.size() > 0) {
                // assertThat(res.get(0).getFirstName(), is("SF"));
                assertThat(res.get(0).getLastName(), is("Höcker"));
                assertThat(res.get(0).getFirstName(), is("Karl-Heinz"));
            }
        }
    }

    @Test
    public void testNameParserCitation() throws Exception {

        String authorSequence1 = "Tsuruoka Y. et al.";
        List<Person> res = engine.processAuthorsCitation(authorSequence1);
        assertNotNull(res);
        assertEquals(1, res.size());
        if (res.size() > 0) {
            assertThat(res.get(0).getFirstName(), is("Y"));
            assertThat(res.get(0).getLastName(), is("Tsuruoka"));
        }

        String authorSequence2 = "Altschul SF, Madden TL, Schäffer AA, Zhang J, Zhang Z, Miller W, Lipman DJ";
        res = engine.processAuthorsCitation(authorSequence2);
        assertNotNull(res);
        if (res != null) {
            // assertEquals(1, res.size());
            if (res.size() > 0) {
                // assertThat(res.get(0).getFirstName(), is("SF"));
                assertThat(res.get(0).getLastName(), is("Altschul"));
            }
        }
    }

}
