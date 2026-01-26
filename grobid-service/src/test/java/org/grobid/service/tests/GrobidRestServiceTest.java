/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grobid.service.tests;

import com.google.inject.Guice;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.*;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.service.GrobidPaths;
import org.grobid.service.GrobidRestService;
import org.grobid.service.GrobidServiceConfiguration;
import org.grobid.service.data.ServiceInfo;
import org.grobid.service.main.GrobidServiceApplication;
import org.grobid.service.module.GrobidServiceModuleTest;
import org.grobid.service.util.BibTexMediaType;
import org.junit.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Tests the RESTful service of the grobid-service project. This class can also
 * tests a remote system, when setting system property
 * org.grobid.service.test.uri to host to test.
 *
 * @author Florian Zipser
 */

@ExtendWith(DropwizardExtensionsSupport.class)
public class GrobidRestServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidRestServiceTest.class);

    @BeforeClass
    public static void setInitialContext() throws Exception {
    }

    @AfterClass
    public static void destroyInitialContext() throws Exception {
    }

    @ClassRule
    public static DropwizardAppRule<GrobidServiceConfiguration> APP =
            new DropwizardAppRule<>(GrobidServiceApplication.class, GrobidServiceModuleTest.TEST_CONFIG_FILE);


    private String baseUrl() {
        return String.format("http://localhost:%d%s" + "api/", APP.getLocalPort(), APP.getEnvironment().getApplicationContext().getContextPath());
    }

    @Before
    public void setUp() throws IOException {
//        JerseyGuiceUtils.reset();

        GrobidServiceModuleTest testWorkerModule = new GrobidServiceModuleTest() {
            // redefine methods that are needed:
        };

        Guice.createInjector(testWorkerModule).injectMembers(this);
    }


    private static File getResourceDir() {
        return (new File("./src/test/resources/"));
    }

    private static Client getClient() {
        Client client = new JerseyClientBuilder().build();
        client.register(MultiPartFeature.class);
        return client;
    }


    /**
     * test the synchronous fully state less rest call
     */
    @Test
    public void testFullyRestLessHeaderDocument() throws Exception {
        String resp = getStrResponse(sample4(), GrobidPaths.PATH_HEADER);
        assertNotNull(resp);
    }


    /*
     * Test the synchronous fully state less rest call
     */
    @Test
    public void testFullyRestLessFulltextDocument() throws Exception {
        String resp = getStrResponse(sample4(), GrobidPaths.PATH_FULL_TEXT);
        assertNotNull(resp);
    }

    /**
     * Test the synchronous state less rest call for dates
     */
    @Test
    public void testRestDate() throws Exception {
        String resp = getStrResponse("date", "November 14 1999", GrobidPaths.PATH_DATE);
        assertNotNull(resp);
    }

    /**
     * Test the synchronous state less rest call for author sequences in headers
     */
    @Test
    public void testRestNamesHeader() throws Exception {
        String names = "Ahmed Abu-Rayyan *,a, Qutaiba Abu-Salem b, Norbert Kuhn * ,b, Cäcilia Maichle-Mößmer b";

        String resp = getStrResponse("names", names, GrobidPaths.PATH_HEADER_NAMES);
        assertNotNull(resp);
    }

    /**
     * Test the synchronous state less rest call for author sequences in
     * citations
     */
    @Test
    public void testRestNamesCitations() throws Exception {
        String names = "Marc Shapiro and Susan Horwitz";
        String resp = getStrResponse("names", names, GrobidPaths.PATH_CITE_NAMES);
        assertNotNull(resp);
    }


    /**
     * Test the synchronous state less rest call for affiliation + address
     * blocks
     */
    @Test
    public void testRestAffiliations() throws Exception {
        String affiliations = "Atomic Physics Division, Department of Atomic Physics and Luminescence, "
                + "Faculty of Applied Physics and Mathematics, Gdansk University of "
                + "Technology, Narutowicza 11/12, 80-233 Gdansk, Poland";
        String resp = getStrResponse("affiliations", affiliations, GrobidPaths.PATH_AFFILIATION);
        assertNotNull(resp);
    }

    /**
     * Test the synchronous state less rest call for patent citation extraction.
     * Send all xml and xml.gz ST36 files found in a given folder test/resources/patent
     * to the web service and write back the results in the test/sample
     */
    @Test
    @Ignore
    public void testRestPatentCitation() throws Exception {
        Client client = getClient();
        
        File xmlDirectory = new File(getResourceDir().getAbsoluteFile() + "/patent");
        File[] files = xmlDirectory.listFiles();
        assertNotNull(files);

        for (final File currXML : files) {
            try {
                if (currXML.getName().toLowerCase().endsWith(".xml") ||
                        currXML.getName().toLowerCase().endsWith(".xml.gz")) {

                    assertTrue("Cannot run the test, because the sample file '" + currXML
                            + "' does not exists.", currXML.exists());
                    FormDataMultiPart form = new FormDataMultiPart();
                    form.field("input", currXML, MediaType.MULTIPART_FORM_DATA_TYPE);
                    form.field("consolidate", "0", MediaType.MULTIPART_FORM_DATA_TYPE);

                    Response response = client.target(
                            baseUrl() + GrobidPaths.PATH_CITATION_PATENT_ST36)
                            .request()
                            .accept(MediaType.APPLICATION_XML + ";charset=utf-8")
                            .post(Entity.entity(form, MediaType.MULTIPART_FORM_DATA_TYPE));

                    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

                    String tei = response.readEntity(String.class);

                    File outputFile = new File(getResourceDir().getAbsoluteFile() +
                            "/../sample/" + currXML.getName().replace(".xml", ".tei.xml").replace(".gz", ""));

                    // writing the result in the sample directory
                    FileUtils.writeStringToFile(outputFile, tei, "UTF-8");
                }
            } catch (final Exception exp) {
                LOGGER.error("An error occured while processing the file "
                        + currXML.getAbsolutePath() + ". Continuing the process for the other files");
            }
        }
    }

    @Test
    public void testGetVersion_shouldReturnCurrentGrobidVersion() throws Exception {
        Response resp = getClient().target(baseUrl() + GrobidPaths.PATH_GET_VERSION)
                .request()
                .get();

        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        ServiceInfo responseEntity = resp.readEntity(ServiceInfo.class);
        assertEquals(GrobidProperties.getVersion(), responseEntity.getVersion());
        assertEquals(GrobidProperties.getRevision(), responseEntity.getRevision());
    }

    @Test
    public void isAliveReturnsTrue() throws Exception {
        Response resp = getClient().target(baseUrl() + GrobidPaths.PATH_IS_ALIVE)
                .request()
                .get();
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        assertEquals("true", resp.readEntity(String.class));
    }

    @Test
    public void processCitationReturnsCorrectBibTeXForMissingFirstName() {
        Form form = new Form();
        form.param(GrobidRestService.CITATION, "Graff, Expert. Opin. Ther. Targets (2002) 6(1): 103-113");
        Response response = getClient().target(baseUrl()).path(GrobidPaths.PATH_CITATION)
                                       .request()
                                       .accept(BibTexMediaType.MEDIA_TYPE)
                                       .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("@article{-1,\n" +
                "  author = {Graff},\n" +
                "  journal = {Expert. Opin. Ther. Targets},\n" +
                "  date = {2002},\n" +
                "  year = {2002},\n" +
                "  pages = {103--113},\n" +
                "  volume = {6},\n" +
                "  number = {1}\n" +
                "}\n",
            response.readEntity(String.class));
    }

    @Test
    public void processCitationReturnsBibTeX() {
        Form form = new Form();
        form.param(GrobidRestService.CITATION, "Kolb, S., Wirtz G.: Towards Application Portability in Platform as a Service\n" +
            "Proceedings of the 8th IEEE International Symposium on Service-Oriented System Engineering (SOSE), Oxford, United Kingdom, April 7 - 10, 2014.");
        Response response = getClient().target(baseUrl()).path(GrobidPaths.PATH_CITATION)
                                       .request()
                                       .accept(BibTexMediaType.MEDIA_TYPE)
                                       .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("@inproceedings{-1,\n" +
                "  author = {Kolb, S. and Wirtz, G.},\n" +
                "  booktitle = {Towards Application Portability in Platform as a Service Proceedings of the 8th IEEE International Symposium on Service-Oriented System Engineering (SOSE)},\n" +
                "  date = {2014},\n" +
                "  year = {2014},\n" +
//                "  year = {April 7 - 10, 2014},\n" +
                "  address = {Oxford, United Kingdom}\n" +
                "}\n",
            response.readEntity(String.class));
    }

    @Test
    public void processCitationReturnsBibTeXAndCanInludeRaw() {
        Form form = new Form();
        form.param(GrobidRestService.CITATION, "Kolb, S., Wirtz G.: Towards Application Portability in Platform as a Service\n" +
            "Proceedings of the 8th IEEE International Symposium on Service-Oriented System Engineering (SOSE), Oxford, United Kingdom, April 7 - 10, 2014.");
        form.param(GrobidRestService.INCLUDE_RAW_CITATIONS, "1");
        Response response = getClient().target(baseUrl()).path(GrobidPaths.PATH_CITATION)
                                       .request()
                                       .accept(BibTexMediaType.MEDIA_TYPE)
                                       .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("@inproceedings{-1,\n" +
                "  author = {Kolb, S. and Wirtz, G.},\n" +
                "  booktitle = {Towards Application Portability in Platform as a Service Proceedings of the 8th IEEE International Symposium on Service-Oriented System Engineering (SOSE)},\n" +
                "  date = {2014},\n" +
                "  year = {2014},\n" +
//                "  year = {April 7 - 10, 2014},\n" +
                "  address = {Oxford, United Kingdom},\n" +
                "  raw = {Kolb, S., Wirtz G.: Towards Application Portability in Platform as a Service\n" +
                "Proceedings of the 8th IEEE International Symposium on Service-Oriented System Engineering (SOSE), Oxford, United Kingdom, April 7 - 10, 2014.}\n" +
                "}\n",
            response.readEntity(String.class));
    }

    @Ignore
    public void processStatelessReferencesDocumentReturnsValidBibTeXForKolbAndKopp() throws Exception {
        final FileDataBodyPart filePart = new FileDataBodyPart(GrobidRestService.INPUT, new File(this.getClass().getResource("/sample5/gadr.pdf").toURI()));
        FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
        //final FormDataMultiPart multipart = (FormDataMultiPart) formDataMultiPart.field("foo", "bar").bodyPart(filePart);
        final FormDataMultiPart multipart = (FormDataMultiPart) formDataMultiPart.bodyPart(filePart);
        Response response = getClient().target(baseUrl() + GrobidPaths.PATH_REFERENCES)
                                       .request(BibTexMediaType.MEDIA_TYPE)
                                       .post(Entity.entity(multipart, multipart.getMediaType()));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("@techreport{0,\n" +
            "  author = {Büchler, A},\n" +
            "  year = {2017}\n" +
            "}\n" +
            "\n" +
            "@article{1,\n" +
            "  author = {Kopp, O. and Armbruster, A. and Zimmermann, O.},\n" +
            "  title = {Markdown Architectural Decision Records: Format and Tool Support},\n" +
            "  booktitle = {ZEUS. CEUR Workshop Proceedings},\n" + 
            "  year = {2018},\n" +
            "  volume = {2072}\n" +
            "}\n" +
            "\n" +
            "@article{2,\n" +
            "  author = {Thurimella, A. and Schubanz, M. and Pleuss, A. and Botterweck, G.},\n" +
            "  title = {Guidelines for Managing Requirements Rationales},\n" +
            "  journal = {IEEE Software},\n" +
            "  year = {Jan 2017},\n" +
            "  pages = {82--90},\n" +
            "  volume = {34},\n" +
            "  number = {1}\n" +
            "}\n" +
            "\n" +
            "@article{3,\n" +
            "  author = {Zdun, U. and Capilla, R. and Tran, H. and Zimmermann, O.},\n" +
            "  title = {Sustainable Architectural Design Decisions},\n" +
            "  journal = {IEEE Software},\n" +
            "  year = {Nov 2013},\n" +
            "  pages = {46--53},\n" +
            "  volume = {30},\n" +
            "  number = {6}\n" +
            "}\n" +
            "\n" +
            "@inbook{4,\n" +
            "  author = {Zimmermann, O. and Wegmann, L. and Koziolek, H. and Goldschmidt, T.},\n" +
            "  title = {Architectural Decision Guidance Across Projects -Problem Space Modeling, Decision Backlog Management and Cloud Computing Knowledge},\n" +
            "  booktitle = {Working IEEE/IFIP Conference on Software Architecture},\n" +
            "  year = {2015}\n" +
            "}\n" +
            "\n" +
            "@inbook{5,\n" +
            "  author = {Zimmermann, O and Miksovic, C},\n" +
            "  title = {Decisions required vs. decisions made},\n" +
            "  booktitle = {Aligning Enterprise, System, and Software Architectures},\n" +
            "  publisher = {IGI Global},\n" +
            "  year = {2013}\n" +
            "}\n" +
            "\n", response.readEntity(String.class));
    }

    private String getStrResponse(File pdf, String method) {
        assertTrue("Cannot run the test, because the sample file '" + pdf + "' does not exists.", pdf.exists());

        FormDataMultiPart form = new FormDataMultiPart();
        form.field("input", pdf, MediaType.MULTIPART_FORM_DATA_TYPE);
        form.field("consolidate", "0", MediaType.MULTIPART_FORM_DATA_TYPE);

        Response response = getClient().target(baseUrl() + method)
                .request()
                .post(Entity.entity(form, MediaType.MULTIPART_FORM_DATA));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String cont = response.readEntity(String.class);
        
        return cont;
    }

    private String getStrResponse(String key, String val, String method) {
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add(key, val);

        Response response = getClient().target(baseUrl() + method)
                .request()
                .post(Entity.entity(formData, MediaType.APPLICATION_FORM_URLENCODED));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String postResp = response.readEntity(String.class);
        assertNotNull(postResp);
        return postResp;
    }

    private static File sample4() {
        return new File(getResourceDir().getAbsoluteFile() + "/sample4/sample.pdf");
    }

}
