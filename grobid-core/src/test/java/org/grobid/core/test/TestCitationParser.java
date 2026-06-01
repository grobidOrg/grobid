package org.grobid.core.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.*;

import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.main.LibraryLoader;

public class TestCitationParser extends EngineTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        LibraryLoader.load();
    }

    @AfterClass
    public static void tearDown() {
        GrobidFactory.reset();
    }

    @Test
    @Ignore("Check this test")
    public void processingReferenceSection() throws Exception {

        String text = "(1) Ahrens, M. Home fires that began with upholstered furniture; National Fire Protection Association: Quincy, Massachusetts, 2011.\n"
                +
                "(2) Evarts, B. Home fires that began with matresses and bedding; National Fire Protection Association: Quincy, Massachusetts, 2011.\n"
                +
                "(3) Kramer, R. H.; Zammarano, M.; Linteris, G. T.; Gedde, U. W.; Gilman, J. W. Polym. Degrad. Stab. 2010, 95, 1115−1122.\n"
                +
                "(4) Underwriter Laboratories, UL test: Legacy home contents and new content fires, YouTube online video clip, 2011.\n"
                +
                "(5) Gallagher, S.; Campbell, J. In Siloxane-Phosphonate Finishes on Cellulose: Thermal Characterization and Flammability Data; Proceed-ings of the Beltwide Cotton Conference, San Antonio, TX, 2004; pp 2443-2847.\n"
                +
                "(6) Watanabe, I.; Sakai, S. Environ. Int. 2003, 29, 665−682. (7) Babrauskas, V.; Blum,\n";

        List<BibDataSet> res = engine.getParsers()
                .getCitationParser()
                .processingReferenceSection(text, engine.getParsers().getReferenceSegmenterParser());
        assertNotNull(res);
        assertTrue(res.size() > 2);
    }

    @Test
    public void testCitationParser1_withoutConsolidation() throws Exception {
        String citation1 = "A. Cau, R. Kuiper, and W.-P. de Roever. Formalising Dijkstra's development "
                +
                "strategy within Stark's formalism. In C. B. Jones, R. C. Shaw, and "
                +
                "T. Denvir, editors, Proc. 5th. BCS-FACS Refinement Workshop, London, UK, 1992.";
        BiblioItem resCitation = engine.processRawReference(citation1, 0);
        assertNotNull(resCitation);
        assertThat(
                resCitation.getTitle(),
                is("Formalising Dijkstra's development strategy within Stark's formalism"));
        assertNotNull(resCitation.getFullAuthors());
    }

    @Test
    public void testCitationParser2_withoutConsolidation() throws Exception {
        String citation2 = "Sanda M. Harabagiu, Steven J. Maiorano and Marius A. Pasca. Open-Domain Textual "
                +
                "Question Answering Techniques. Natural Language Engineering, 9 (3):1-38, 2003.";
        BiblioItem resCitation = engine.processRawReference(citation2, 0);
        assertNotNull(resCitation);

        assertThat(
                resCitation.getTitle(),
                is("Open-Domain Textual Question Answering Techniques"));
        assertNotNull(resCitation.getFullAuthors());

    }

    //@Test
    public void testCitationParser3_withConsolidation() throws Exception {

        String citation3 = "Graff, Expert. Opin. Ther. Targets (2002) 6(1): 103-113";
        BiblioItem resCitation = engine.processRawReference(citation3, 1);
        assertNotNull(resCitation);
        assertNotNull(resCitation.getNormalizedPublicationDate());
        assertThat(
                resCitation.getNormalizedPublicationDate().getYear(),
                is(2002));
    }

    //@Test
    public void testCitationParser4_withConsolidation() throws Exception {
        String citation4 = "Zholudev Vyacheslav, Kohlhase Michael, Rabe Florian. A [insert XML Format] "
                +
                "Database for [insert cool application] (extended version); Technical Report , Jacobs "
                +
                "University Bremen 2010.";
        BiblioItem resCitation = engine.processRawReference(citation4, 1);
        assertNotNull(resCitation);
        assertNotNull(resCitation.getNormalizedPublicationDate());
        assertThat(
                resCitation.getNormalizedPublicationDate().getYear(),
                is(2010));
        assertNotNull(resCitation.getFullAuthors());
    }

    @Test
    public void testCitationParser5_withoutConsolidation() throws Exception {

        String citation5 = "Altschul SF, Madden TL, Schäffer AA, Zhang J, Zhang Z, Miller W, Lipman DJ: Gapped BLAST and PSI-BLAST: a new generation of protein database search programs. Nucleic Acid Res 1997 25:3389-3402";
        BiblioItem resCitation = engine.processRawReference(citation5, 0);
        assertNotNull(resCitation);
        assertNotNull(resCitation.getNormalizedPublicationDate());
        assertThat(
                resCitation.getNormalizedPublicationDate().getYear(),
                is(1997));
        assertNotNull(resCitation.getFullAuthors());

    }
}
