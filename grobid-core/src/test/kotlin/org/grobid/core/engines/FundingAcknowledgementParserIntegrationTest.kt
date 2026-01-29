package org.grobid.core.engines

import org.grobid.core.engines.config.GrobidAnalysisConfig
import org.grobid.core.factory.AbstractEngineFactory
import org.grobid.core.utilities.GrobidConfig
import org.grobid.core.utilities.GrobidProperties
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.xmlunit.matchers.CompareMatcher
import java.util.*

class FundingAcknowledgementParserIntegrationTest {

    private lateinit var target: FundingAcknowledgementParser

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val modelParameters = GrobidConfig.ModelParameters()
        modelParameters.name = "bao"
        GrobidProperties.addModel(modelParameters)
        target = FundingAcknowledgementParser()
    }

    @Test
    fun testXmlFragmentProcessing_withoutSentenceSegmentation_shouldReturnSameXML() {
        val input = "\n\t\t\t<div type=\"acknowledgement\">\n<div><head>Acknowledgments</head><p>This research was " +
            "funded by the NASA Land-Cover and Land-Use Change Program (Grant Number: 80NSSC18K0315), the NASA " +
            "Carbon Monitoring System (Grant Number: 80NSSC20K0022), and </p></div>\n\t\t\t</div>\n\n"

        // Expected
//        val output = "\n\t\t\t<div type=\"acknowledgement\">\n<div><head>Acknowledgments</head><p>This research was " +
//            "funded by the <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"funder\">NASA</rs> " +
//            "<rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"programName\">Land-Cover and Land-Use Change Program</rs> " +
//            "(Grant Number: <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"grantNumber\">80NSSC18K0315</rs>), " +
//            "the <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"funder\">NASA Carbon Monitoring System</rs> " +
//            "(Grant Number: <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"grantNumber\">80NSSC20K0022</rs>), " +
//            "and </p></div>\n\t\t\t</div>\n\n"

        // Current version output
        val output = "<div type=\"acknowledgement\">\n<div><head>Acknowledgments</head><p>This research was " +
            "funded by the <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"funder\">NASA</rs> " +
            "<rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"programName\">Land-Cover and Land-Use Change Program</rs> " +
            "(Grant Number: <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"grantNumber\">80NSSC18K0315</rs>), " +
            "the <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"funder\">NASA Carbon Monitoring System</rs> " +
            "(Grant Number: <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"grantNumber\">80NSSC20K0022</rs>), " +
            "and </p></div>\n\t\t\t</div>"

        val config = GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .withSentenceSegmentation(false)
            .build()

        val (element, mutableTriple) = target.processingXmlFragment(input, config)

        assertThat(element.toXML(), CompareMatcher.isIdenticalTo(output))
        assertThat(mutableTriple.left, hasSize(2))
    }

    @Test
    fun testXmlFragmentProcessing2_withoutSentenceSegmentation_shouldReturnSameXML() {
        val input = "\n" +
            "\t\t\t<div type=\"acknowledgement\">\n" +
            "<div xmlns=\"http://www.tei-c.org/ns/1.0\"><head>Acknowledgements</head><p>Our warmest thanks to Patrice Lopez, the author of Grobid <ref type=\"bibr\" target=\"#b21\">[22]</ref>, DeLFT <ref type=\"bibr\" target=\"#b19\">[20]</ref>, and other open-source projects for his continuous support and inspiration with ideas, suggestions, and fruitful discussions. We thank Pedro Baptista de Castro for his support during this work. Special thanks to Erina Fujita for useful tips on the manuscript.</p></div>\n" +
            "\t\t\t</div>\n\n"

        // Expected
//        val output = "\n\t\t\t<div type=\"acknowledgement\">\n" +
//            "<div><head>Acknowledgements</head><p>Our warmest thanks to <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Patrice Lopez</rs>, the author of Grobid [22], DeLFT [20], and other open-source projects for his continuous support and inspiration with ideas, suggestions, and fruitful discussions. We thank <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Pedro Baptista de Castro</rs> for his support during this work. Special thanks to <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Erina Fujita</rs> for useful tips on the manuscript.</p></div>\n" +
//            "\t\t\t</div>\n\n"

        // Current version output
        val output = "<div type=\"acknowledgement\">\n" +
            "<div><head>Acknowledgements</head><p>Our warmest thanks to <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Patrice Lopez</rs>, the author of Grobid <ref type=\"bibr\" target=\"#b21\">[22]</ref>, DeLFT <ref type=\"bibr\" target=\"#b19\">[20]</ref>, and other open-source projects for his continuous support and inspiration with ideas, suggestions, and fruitful discussions. We thank <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Pedro Baptista de Castro</rs> for his support during this work. Special thanks to <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Erina Fujita</rs> for useful tips on the manuscript.</p></div>\n" +
            "\t\t\t</div>"

        val config = GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .withSentenceSegmentation(false)
            .build()

        val (element, mutableTriple) = target.processingXmlFragment(input, config)

        assertThat(element.toXML(), CompareMatcher.isIdenticalTo(output))
    }

    @Test
    fun testXmlFragmentProcessing2_withSentenceSegmentation_shouldWork() {
        val input = "\n" +
            "\t\t\t<div type=\"acknowledgement\">\n" +
            "<div xmlns=\"http://www.tei-c.org/ns/1.0\"><head>Acknowledgements</head><p><s>Our warmest thanks to Patrice Lopez, the author of Grobid <ref type=\"bibr\" target=\"#b21\">[22]</ref>, DeLFT <ref type=\"bibr\" target=\"#b19\">[20]</ref>, and other open-source projects for his continuous support and inspiration with ideas, suggestions, and fruitful discussions.</s><s>We thank Pedro Baptista de Castro for his support during this work.</s><s>Special thanks to Erina Fujita for useful tips on the manuscript.</s></p></div>\n" +
            "\t\t\t</div>\n\n"

        val output = "<div type=\"acknowledgement\">\n" +
            "<div><head>Acknowledgements</head><p><s>Our warmest thanks to <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Patrice Lopez</rs>, the author of Grobid <ref type=\"bibr\" target=\"#b21\">[22]</ref>, DeLFT <ref type=\"bibr\" target=\"#b19\">[20]</ref>, and other open-source projects for his continuous support and inspiration with ideas, suggestions, and fruitful discussions.</s><s>We thank <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Pedro Baptista de Castro</rs> for his support during this work.</s><s>Special thanks to <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Erina Fujita</rs> for useful tips on the manuscript.</s></p></div>\n" +
            "\t\t\t</div>"

        val config = GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .withSentenceSegmentation(true)
            .build()

        val (element, mutableTriple) = target.processingXmlFragment(input, config)

        assertThat(element.toXML(), CompareMatcher.isIdenticalTo(output))
    }

    @Test
    fun testXmlFragmentProcessing_ErrorCase_withSentenceSegmentation_shouldWork() {
        val input = """
			<div type="funding">
<div><p><s>Florentina Münzner, Lucy Schlicht, Adrian Tanara, Sany Tchanra and Marie-Jeanne Pesant for the manual curation of logsheets and archiving data at PANGAEA.</s><s>We also acknowledge the work of Andree Behnken who developed the dds-fdp web service.</s><s>All authors approved the final manuscript.</s><s>This article is contribution number 26 of the Tara Oceans Consortium.</s><s>The collection of Tara Oceans data was made possible by those who contributed to sampling and to logistics during the Tara Oceans Expedition: Alain Giese, Alan Deidun, Alban Lazar, Aldine Amiel, Ali Chase, Aline Tribollet, Ameer Abdullah, Amélie Betus, André Abreu, Andres Peyrot, Andrew Baker, Anna Deniaud, Anne Doye, Anne Ghuysen Watrin, Anne Royer, Anne Thompson, Annie McGrother, Antoine Sciandra, Antoine Triller, Aurélie Chambouvet, Baptiste Bernard, Baptiste Regnier, Beatriz Fernandez, Benedetto Barone, Bertrand Manzano, Bianca Silva, Brett Grant, Brigitte Sabard, Bruno Dunckel, Camille Clérissi, Catarina Marcolin, Cédric Guigand, Céline Bachelier, Céline Blanchard, Céline Dimier-Hugueney, Céline Rottier, Chris Bowler, Christian Rouvière, Christian Sardet, Christophe Boutte, Christophe Castagne, Claudie Marec, Claudie Marec, Claudio Stalder, Colomban De Vargas, Cornelia Maier, Cyril Tricot, Dana Sardet, Daniel Bayley, Daniel Cron, Daniele Iudicone, David Mountain, David Obura, David Sauveur, Defne Arslan, Denis Dausse, Denis de La Broise, Diana Ruiz Pino, Didier Zoccola, Édouard Leymarie, Éloïse Fontaine, Émilie Sauvage, Emilie Villar, Emmanuel Boss, Emmanuel G. Reynaud, Éric Béraud, Eric Karsenti, Eric Pelletier, Éric Roettinger, Erica Goetz, Fabien Perault, Fabiola Canard, Fabrice Not, Fabrizio D'Ortenzio, Fabrizio Limena, Floriane Desprez, Franck Prejger, François Aurat, François Noël, Franscisco Cornejo, Gabriel Gorsky, Gabriele Procaccini, Gabriella Gilkes, Gipsi Lima-Mendez, Grigor Obolensky, Guillaume Bracq, Guillem Salazar, Halldor Stefansson, Hélène Santener, Hervé Bourmaud, Hervé Le Goff, Hiroyuki Ogata, Hubert Gautier, Hugo Sarmento, Ian Probert, Isabel Ferrera, Isabelle Taupier-Letage, Jan Wengers, Jarred Swalwell, Javier del Campo, Jean-Baptiste Romagnan, Jean-Claude Gascard, Jean-Jacques Kerdraon, Jean-Louis Jamet, Jean-Michel Grisoni, Jennifer Gillette, Jérémie Capoulade, Jérôme Bastion, Jérôme Teigné, Joannie Ferland, Johan Decelle, Judith Prihoda, Julie Poulain, Julien Daniel, Julien Girardot, Juliette Chatelin, Lars Stemmann, Laurence Garczarek, Laurent Beguery, Lee Karp-Boss, Leila Tirichine, Linda Mollestan, Lionel Bigot, Loïc Vallette, Lucie Bittner, Lucie Subirana, Luis Gutiérrez, Lydiane Mattio, Magali Puiseux, Marc Domingos, Marc Picheral, Marc Wessner, Marcela Cornejo, Margaux Carmichael, Marion Lauters, Martin Hertau, Martina Sailerova, Mathilde Ménard, Matthieu Labaste, Matthieu Oriot, Matthieu Bretaud, Mattias Ormestad, Maya Dolan, Melissa Duhaime, Michael Pitiot, Mike Lunn, Mike Sieracki, Montse Coll, Myriam Thomas, Nadine Lebois, Nicole Poulton, Nigel Grimsley, Noan Le Bescot, Oleg Simakov, Olivier Broutin, Olivier Desprez, Olivier Jaillon, Olivier Marien, Olivier Poirot, Olivier Quesnel, Pamela Labbe-Ibanez, Pascal Hingamp, Pascal Morin, Pascale Joannot, Patrick Chang, Patrick Wincker, Paul Muir, Philippe Clais, Philippe Koubbi, Pierre Testor, Rachel Moreau, Raphaël Morard, Roland Heilig, Romain Troublé, Roxana Di Mauro, Roxanne Boonstra, Ruby Pillay, Sabrina Speich, Sacha Bollet, Samuel Audrain, Sandra Da Costa, Sarah Searson, Sasha Tozzi, Sébastien Colin, Sergey Pisarev, Shirley Falcone, Sibylle Le Barrois d'Orgeval, Silvia G. Acinas, Simon Morisset, Sophie Marinesque, Sophie Nicaud, Stefanie Kandels-Lewis, Stéphane Audic, Stephane Pesant, Stéphanie Reynaud, Thierry Mansir, Thomas Lefort, Uros Krzic, Valérian Morzadec, Vincent Hilaire, Vincent Le Pennec, Vincent Taillandier, Xavier Bailly, Xavier Bougeard, Xavier Durrieu de Madron, Yann Chavance, Yann Depays, Yohann Mucherie.</s></p></div>
			</div>

"""

        val output = """
			<div type="funding">
<div><p><s><rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Florentina Münzner</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Lucy Schlicht</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Adrian Tanara</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Sany Tchanra</rs> and <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Marie-Jeanne Pesant</rs> for the manual curation of logsheets and archiving data at PANGAEA.</s><s>We also acknowledge the work of <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Andree Behnken</rs> who developed the dds-fdp web service.</s><s>All authors approved the final manuscript.</s><s>This article is contribution number <rs xmlns="http://www.tei-c.org/ns/1.0" type="grantNumber">26</rs> of the <rs xmlns="http://www.tei-c.org/ns/1.0" type="institution">Tara Oceans Consortium</rs>.</s><s>The collection of Tara Oceans data was made possible by those who contributed to sampling and to logistics during the Tara Oceans Expedition: <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Alain Giese</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Alan Deidun</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Alban Lazar</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Aldine Amiel</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Ali Chase</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Aline Tribollet</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Ameer Abdullah</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Amélie Betus</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">André Abreu</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Andres Peyrot</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Andrew Baker</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Anna Deniaud</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Anne Doye</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Anne Ghuysen Watrin</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Anne Royer</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Anne Thompson</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Annie McGrother</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Antoine Sciandra</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Antoine Triller</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Aurélie Chambouvet</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Baptiste Bernard</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Baptiste Regnier</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Beatriz Fernandez</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Benedetto Barone</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Bertrand Manzano</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Bianca Silva</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Brett Grant</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Brigitte Sabard</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Bruno Dunckel</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Camille Clérissi</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Catarina Marcolin</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Cédric Guigand</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Céline Bachelier</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Céline Blanchard</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Céline Dimier-Hugueney</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Céline Rottier</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Chris Bowler</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Christian Rouvière</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Christian Sardet</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Christophe Boutte</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Christophe Castagne</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Claudie Marec</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Claudie Marec</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Claudio Stalder</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Colomban De Vargas</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Cornelia Maier</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Cyril Tricot</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Dana Sardet</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Daniel Bayley</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Daniel Cron</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Daniele Iudicone</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">David Mountain</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">David Obura</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">David Sauveur</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Defne Arslan</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Denis Dausse</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Denis de La Broise</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Diana Ruiz Pino</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Didier Zoccola</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Édouard Leymarie</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Éloïse Fontaine</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Émilie Sauvage</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Emilie Villar</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Emmanuel Boss</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Emmanuel G. Reynaud</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Éric Béraud</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Eric Karsenti</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Eric Pelletier</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Éric Roettinger</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Erica Goetz</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Fabien Perault</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Fabiola Canard</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Fabrice Not</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Fabrizio D'Ortenzio</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Fabrizio Limena</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Floriane Desprez</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Franck Prejger</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">François Aurat</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">François Noël</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Franscisco Cornejo</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Gabriel Gorsky</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Gabriele Procaccini</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Gabriella Gilkes</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Gipsi Lima-Mendez</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Grigor Obolensky</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Guillaume Bracq</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Guillem Salazar</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Halldor Stefansson</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Hélène Santener</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Hervé Bourmaud</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Hervé Le Goff</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Hiroyuki Ogata</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Hubert Gautier</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Hugo Sarmento</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Ian Probert</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Isabel Ferrera</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Isabelle Taupier-Letage</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Jan Wengers</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Jarred Swalwell</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Javier del Campo</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Jean-Baptiste Romagnan</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Jean-Claude Gascard</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Jean-Jacques Kerdraon</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Jean-Louis Jamet</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Jean-Michel Grisoni</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Jennifer Gillette</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Jérémie Capoulade</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Jérôme Bastion</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Jérôme Teigné</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Joannie Ferland</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Johan Decelle</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Judith Prihoda</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Julie Poulain</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Julien Daniel</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Julien Girardot</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Juliette Chatelin</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Lars Stemmann</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Laurence Garczarek</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Laurent Beguery</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Lee Karp-Boss</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Leila Tirichine</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Linda Mollestan</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Lionel Bigot</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Loïc Vallette</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Lucie Bittner</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Lucie Subirana</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Luis Gutiérrez</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Lydiane Mattio</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Magali Puiseux</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Marc Domingos</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Marc Picheral</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Marc Wessner</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Marcela Cornejo</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Margaux Carmichael</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Marion Lauters</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Martin Hertau</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Martina Sailerova</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Mathilde Ménard</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Matthieu Labaste</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Matthieu Oriot</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Matthieu Bretaud</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Mattias Ormestad</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Maya Dolan</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Melissa Duhaime</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Michael Pitiot</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Mike Lunn</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Mike Sieracki</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Montse Coll</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Myriam Thomas</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Nadine Lebois</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Nicole Poulton</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Nigel Grimsley</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Noan Le Bescot</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Oleg Simakov</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Olivier Broutin</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Olivier Desprez</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Olivier Jaillon</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Olivier Marien</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Olivier Poirot</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Olivier Quesnel</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="institution">Pamela Labbe-Ibanez, Pascal Hingamp, Pascal Morin</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Pascale Joannot</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Patrick Chang</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Patrick Wincker</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Paul Muir</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Philippe Clais</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Philippe Koubbi</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Pierre Testor</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Rachel Moreau</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Raphaël Morard</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Roland Heilig</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Romain Troublé</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Roxana Di Mauro</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Roxanne Boonstra</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Ruby Pillay</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Sabrina Speich</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Sacha Bollet</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Samuel Audrain</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Sandra Da Costa</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Sarah Searson</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Sasha Tozzi</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Sébastien Colin</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Sergey Pisarev</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Shirley Falcone</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Sibylle Le Barrois d'Orgeval</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Silvia G. Acinas</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Simon Morisset</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Sophie Marinesque</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Sophie Nicaud</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Stefanie Kandels-Lewis</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Stéphane Audic</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Stephane Pesant</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Stéphanie Reynaud</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Thierry Mansir</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Thomas Lefort</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Uros Krzic</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Valérian Morzadec</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Vincent Hilaire</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Vincent Le Pennec</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Vincent Taillandier</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Xavier Bailly</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Xavier Bougeard</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Xavier Durrieu de Madron</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Yann Chavance</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Yann Depays</rs>, <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Yohann Mucherie</rs>.</s></p></div>
			</div>

"""

        val config = GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .withSentenceSegmentation(true)
            .build()

        val (element, mutableTriple) = target.processingXmlFragment(input, config)

        assertThat(element.toXML(), CompareMatcher.isIdenticalTo(output))
    }

    @Test
    fun testXmlFragmentProcessing_ErrorCase2_withSentenceSegmentation_shouldWork() {
        val input = """
			<div type="acknowledgement">
<div><head>Acknowledgements</head><p><s>The authors would like to acknowledge Lucy Popplewell in the preparation of EMR notes for this study.</s></p></div>
<div><head>The authors would like to acknowledge Keele University's Prognosis and Consultation Epidemiology</head><p><s>Research Group who have given us permission to utilise the morbidity definitions (©2014).</s><s>The copyright of the morbidity definitions/categorization lists (©2014) used in this publication is owned by Keele University, the development of which was supported by the Primary Care Research Consortium; For access/details relating to the morbidity definitions/categorisation lists (©2014) please go to www.keele.ac.uk/mrr.</s></p></div>
			</div>

"""

        val output = """
			<div type="acknowledgement">
<div><head>Acknowledgements</head><p><s>The authors would like to acknowledge <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">Lucy Popplewell</rs> in the preparation of EMR notes for this study.</s></p></div>
<div><head>The authors would like to acknowledge Keele University's Prognosis and Consultation Epidemiology</head><p><s>Research Group who have given us permission to utilise the morbidity definitions (<rs xmlns="http://www.tei-c.org/ns/1.0" type="grantNumber">©2014</rs>).</s><s>The copyright of the morbidity definitions/categorization lists (<rs xmlns="http://www.tei-c.org/ns/1.0" type="grantNumber">©2014</rs>) used in this publication is owned by <rs xmlns="http://www.tei-c.org/ns/1.0" type="funder">Keele University</rs>, the development of which was supported by the <rs xmlns="http://www.tei-c.org/ns/1.0" type="funder">Primary Care Research Consortium</rs>; For access/details relating to the morbidity definitions/categorisation lists (<rs xmlns="http://www.tei-c.org/ns/1.0" type="grantNumber">©2014</rs>) please go to www.keele.ac.uk/mrr.</s></p></div>
			</div>

"""
        val config = GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .withSentenceSegmentation(true)
            .build()

        val (element, mutableTriple) = target.processingXmlFragment(input, config)

        assertThat(element.toXML(), CompareMatcher.isIdenticalTo(output))
    }

    @Test
    fun testXmlFragmentProcessing_ErrorCase3_withSentenceSegmentation_shouldWork() {
        val input = """
			<div type="funding">
<div><head>Funding</head><p><s>This work was supported by European Molecular Biology Laboratory, the NSF award "BIGDATA: Mid-Scale: DA: ESCE: Collaborative Research: Scalable Statistical Computing for Emerging Omics Data Streams" and Genentech Inc.</s></p></div>
			</div>

"""

        val output = """
			<div type="funding">
<div><head>Funding</head><p><s>This work was supported by <rs xmlns="http://www.tei-c.org/ns/1.0" type="funder">European Molecular Biology Laboratory</rs>, the <rs xmlns="http://www.tei-c.org/ns/1.0" type="funder">NSF</rs> award "<rs xmlns="http://www.tei-c.org/ns/1.0" type="projectName">BIGDATA: Mid-Scale: DA: ESCE: Collaborative Research: Scalable Statistical Computing for Emerging Omics Data Streams</rs>" and <rs xmlns="http://www.tei-c.org/ns/1.0" type="funder">Genentech Inc.</rs></s></p></div>
			</div>

"""
        val config = GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .withSentenceSegmentation(true)
            .build()

        val (element, mutableTriple) = target.processingXmlFragment(input, config)

        assertThat(element.toXML(), CompareMatcher.isIdenticalTo(output))
    }

    @Test
    fun testXmlFragmentProcessing_mergingSentences_shouldMergeCorrectly() {
        val input = "\n" +
            "\t\t\t<div type=\"acknowledgement\">\n" +
            "<div xmlns=\"http://www.tei-c.org/ns/1.0\"><head>Acknowledgements</head><p><s>Our warmest thanks to Patrice</s><s>Lopez, the author of Grobid <ref type=\"bibr\" target=\"#b21\">[22]</ref>, DeLFT <ref type=\"bibr\" target=\"#b19\">[20]</ref>, and other open-source projects for his continuous support and inspiration with ideas, suggestions, and fruitful discussions.</s><s>We thank Pedro Baptista</s><s>de</s><s>Castro for his support during this work.</s><s>Special thanks to Erina Fujita for useful tips on the manuscript.</s></p></div>\n" +
            "\t\t\t</div>\n\n"

        val output = "<div type=\"acknowledgement\">\n" +
            "<div><head>Acknowledgements</head><p><s>Our warmest thanks to <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">PatriceLopez</rs>, the author of Grobid <ref type=\"bibr\" target=\"#b21\">[22]</ref>, DeLFT <ref type=\"bibr\" target=\"#b19\">[20]</ref>, and other open-source projects for his continuous support and inspiration with ideas, suggestions, and fruitful discussions.</s><s>We thank <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Pedro BaptistadeCastro</rs> for his support during this work.</s><s>Special thanks to <rs xmlns=\"http://www.tei-c.org/ns/1.0\" type=\"person\">Erina Fujita</rs> for useful tips on the manuscript.</s></p></div>\n" +
            "\t\t\t</div>"

        val config = GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .withSentenceSegmentation(true)
            .build()

        val (element, mutableTriple) = target.processingXmlFragment(input, config)

        assertThat(element.toXML(), CompareMatcher.isIdenticalTo(output))
    }

    @Test
    fun testXmlFragmentProcessing_mergingSentencesAndCoordinatesInTheSamePage_shouldMergeCoordinates() {
        val input = """<div type="acknowledgement">" +
            "<div xmlns="http://www.tei-c.org/ns/1.0"><head>Acknowledgements</head><p><s coords="1,56.80,41.48,432.74,26.53">This is sentence 1 in page 1 where we thanks Patrice</s><s coords="1,56.80,41.48,432.74,26.57">Lopez, who is also overlapping in sentence 2, page 2, with annotations <ref type="bibr" target="#b21">[22]</ref>, DeLFT <ref type="bibr" target="#b19">[20]</ref>, and more text.</s></p></div>\n" +
            "</div>"""

        val output = """<div type="acknowledgement">" +
            "<div><head>Acknowledgements</head><p><s coords="1,56.80,41.48,432.74,26.57">This is sentence 1 in page 1 where we thanks <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">PatriceLopez</rs>, who is also overlapping in sentence 2, page 2, with annotations <ref type="bibr" target="#b21">[22]</ref>, DeLFT <ref type="bibr" target="#b19">[20]</ref>, and more text.</s></p></div>\n" +
            "</div>"""

        val config = GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .withSentenceSegmentation(true)
            .generateTeiCoordinates(listOf("s"))
            .build()

        val (element, mutableTriple) = target.processingXmlFragment(input, config)

        assertThat(element.toXML(), CompareMatcher.isIdenticalTo(output))
    }

    @Test
    fun testXmlFragmentProcessing_mergingSentencesAndCoordinatesInTheSamePage2_shouldMergeCoordinates() {
        val input = """<div type="acknowledgement">" +
            "<div xmlns="http://www.tei-c.org/ns/1.0"><head>Acknowledgements</head><p><s coords="1,56.80,41.48,432.74,26.53">This is sentence 1 in page 1 where we thanks Patrice</s><s coords="1,86.80,141.48,532.74,26.57">Lopez, who is also overlapping in sentence 2, page 2, with annotations <ref type="bibr" target="#b21">[22]</ref>, DeLFT <ref type="bibr" target="#b19">[20]</ref>, and more text.</s></p></div>\n" +
            "</div>"""

        val output = """<div type="acknowledgement">" +
            "<div><head>Acknowledgements</head><p><s coords="1,56.80,41.48,432.74,26.53;1,86.80,141.48,532.74,26.57">This is sentence 1 in page 1 where we thanks <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">PatriceLopez</rs>, who is also overlapping in sentence 2, page 2, with annotations <ref type="bibr" target="#b21">[22]</ref>, DeLFT <ref type="bibr" target="#b19">[20]</ref>, and more text.</s></p></div>\n" +
            "</div>"""

        val config = GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .withSentenceSegmentation(true)
            .generateTeiCoordinates(listOf("s"))
            .build()

        val (element, mutableTriple) = target.processingXmlFragment(input, config)

        assertThat(element.toXML(), CompareMatcher.isIdenticalTo(output))
    }

    @Test
    fun testXmlFragmentProcessing_mergingSentencesAndCoordinatesInDifferentPages_shouldNotMergeCoordinates() {
        val input = """<div type="acknowledgement">" +
            "<div xmlns="http://www.tei-c.org/ns/1.0"><head>Acknowledgements</head><p><s coords="1,56.80,41.48,432.74,26.57">This is sentence 1 in page 1 where we thanks Patrice</s><s coords="2,56.80,41.48,432.74,26.57">Lopez, who is also overlapping in sentence 2, page 2, with annotations <ref type="bibr" target="#b21">[22]</ref>, DeLFT <ref type="bibr" target="#b19">[20]</ref>, and more text.</s></p></div>\n" +
            "</div>"""

        val output = """<div type="acknowledgement">" +
            "<div><head>Acknowledgements</head><p><s coords="1,56.80,41.48,432.74,26.57;2,56.80,41.48,432.74,26.57">This is sentence 1 in page 1 where we thanks <rs xmlns="http://www.tei-c.org/ns/1.0" type="person">PatriceLopez</rs>, who is also overlapping in sentence 2, page 2, with annotations <ref type="bibr" target="#b21">[22]</ref>, DeLFT <ref type="bibr" target="#b19">[20]</ref>, and more text.</s></p></div>\n" +
            "</div>"""

        val config = GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .withSentenceSegmentation(true)
            .generateTeiCoordinates(listOf("s"))
            .build()

        val (element, mutableTriple) = target.processingXmlFragment(input, config)

        assertThat(element.toXML(), CompareMatcher.isIdenticalTo(output))
    }

    companion object {
        @JvmStatic
        @BeforeClass
        @Throws(java.lang.Exception::class)
        fun setInitialContext() {
            AbstractEngineFactory.init()
        }
    }
}
