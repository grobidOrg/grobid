package org.grobid.service.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.grobid.core.utilities.GrobidProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GrobidRestProcessTrainingTasksTest {

    private GrobidRestProcessTraining target;
    private final List<File> tokenDirectories = new ArrayList<>();

    @Before
    public void setUp() {
        GrobidProperties.getInstance();
        target = new GrobidRestProcessTraining();
    }

    @After
    public void tearDown() throws IOException {
        for (File tokenDirectory : tokenDirectories) {
            FileUtils.deleteDirectory(tokenDirectory);
        }
    }

    @Test
    public void allTraining_shouldListOnlyOngoingTokens() throws Exception {
        String ongoingToken = createTokenWithStatus("ongoing");
        String doneToken = createTokenWithStatus("done");

        Response response = target.allTraining();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String body = (String) response.getEntity();
        assertTrue(body.contains(ongoingToken));
        assertFalse(body.contains(doneToken));
    }

    @Test
    public void killTraining_shouldUpdateStaleOngoingStatus() throws Exception {
        String token = createTokenWithStatus("ongoing");
        File statusFile = new File(getTrainingHistoryDirectory(), token + "/status");

        Response response = target.killTraining(token);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("killed", FileUtils.readFileToString(statusFile, "UTF-8"));
    }

    @Test
    public void killTraining_shouldReturnBadRequestForUnknownToken() {
        Response response = target.killTraining("unknown-token");
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    private String createTokenWithStatus(String status) throws IOException {
        String token = "unit-training-" + System.nanoTime();
        File tokenDirectory = new File(getTrainingHistoryDirectory(), token);
        FileUtils.forceMkdir(tokenDirectory);
        FileUtils.writeStringToFile(new File(tokenDirectory, "status"), status, "UTF-8");
        tokenDirectories.add(tokenDirectory);
        return token;
    }

    private File getTrainingHistoryDirectory() throws IOException {
        File trainingHistoryDirectory = new File(GrobidProperties.getInstance().getGrobidHomePath(), "training-history");
        FileUtils.forceMkdir(trainingHistoryDirectory);
        return trainingHistoryDirectory;
    }
}
