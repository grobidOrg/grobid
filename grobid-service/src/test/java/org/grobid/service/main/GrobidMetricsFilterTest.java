/*
 * Copyright 2008-2026 GROBID contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grobid.service.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import io.prometheus.client.CollectorRegistry;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.junit.Before;
import org.junit.Test;

import org.grobid.service.metrics.ApplicationMetrics;

public class GrobidMetricsFilterTest {

    private CollectorRegistry registry;
    private ApplicationMetrics metrics;
    private GrobidMetricsFilter filter;

    @Before
    public void setUp() {
        registry = new CollectorRegistry();
        metrics = new ApplicationMetrics(registry);
        filter = new GrobidMetricsFilter(metrics);
    }

    @Test
    public void lifecycle_recordsRequestAndBalancesInFlight() throws IOException {
        ContainerRequestContext req = mock(ContainerRequestContext.class);
        ContainerResponseContext resp = mock(ContainerResponseContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(req.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("processFulltextDocument");
        when(req.getProperty(anyString())).thenReturn(System.nanoTime());
        when(req.getLength()).thenReturn(5000);
        when(req.getMediaType()).thenReturn(MediaType.MULTIPART_FORM_DATA_TYPE);
        when(resp.getStatus()).thenReturn(200);

        filter.filter(req);   // request phase: in-flight -> 1
        filter.filter(req, resp); // response phase: record + in-flight -> 0

        assertEquals(
                1.0,
                sample(
                        "grobid_requests_total",
                        new String[]{"endpoint", "http_status"},
                        new String[]{"processFulltextDocument", "200"}),
                0.0);
        assertEquals(1.0, sample("grobid_files_processed_total", new String[]{}, new String[]{}), 0.0);
        assertEquals(0.0, sample("grobid_requests_in_flight", new String[]{}, new String[]{}), 0.0);
    }

    @Test
    public void serverError_onUpload_incrementsFileErrorCounter() throws IOException {
        ContainerRequestContext req = mock(ContainerRequestContext.class);
        ContainerResponseContext resp = mock(ContainerResponseContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(req.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("processFulltextDocument");
        when(req.getProperty(anyString())).thenReturn(System.nanoTime());
        when(req.getLength()).thenReturn(5000);
        when(req.getMediaType()).thenReturn(MediaType.MULTIPART_FORM_DATA_TYPE);
        when(resp.getStatus()).thenReturn(503);

        filter.filter(req, resp);

        assertEquals(
                1.0,
                sample(
                        "grobid_files_processing_errors_total",
                        new String[]{},
                        new String[]{}),
                0.0);
        assertEquals(
                1.0,
                sample(
                        "grobid_requests_total",
                        new String[]{"endpoint", "http_status"},
                        new String[]{"processFulltextDocument", "503"}),
                0.0);
    }

    @Test
    public void nonUpload_doesNotTouchFileCounters() throws IOException {
        ContainerRequestContext req = mock(ContainerRequestContext.class);
        ContainerResponseContext resp = mock(ContainerResponseContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(req.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("isalive");
        when(req.getProperty(anyString())).thenReturn(System.nanoTime());
        when(req.getLength()).thenReturn(-1);
        when(req.getMediaType()).thenReturn(MediaType.TEXT_PLAIN_TYPE);
        when(resp.getStatus()).thenReturn(200);

        filter.filter(req, resp);

        // Label-less counter exists at 0 from registration; a non-upload request must leave it at 0.
        assertEquals(0.0, sample("grobid_files_processed_total", new String[]{}, new String[]{}), 0.0);
        assertEquals(
                1.0,
                sample(
                        "grobid_requests_total",
                        new String[]{"endpoint", "http_status"},
                        new String[]{"isalive", "200"}),
                0.0);
    }

    private double sample(String name, String[] labelNames, String[] labelValues) {
        Double value = registry.getSampleValue(name, labelNames, labelValues);
        assertNotNull("expected sample " + name + " to be present", value);
        return value;
    }
}
