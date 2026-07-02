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
package org.grobid.core.utilities.crossref;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.grobid.core.data.BiblioItem;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;

/**
 * Test class with 100 requests (works/doi)
 *
 */
public class Test {

    public static String[] DOIs = {
            "10.1086/107043",
            "10.1086/102351",
            "10.1086/100853",
            "10.1086/105172",
            "10.1086/106972",
            "10.1086/106203",
            "10.1086/106965",
            "10.1086/112074",
            "10.1086/112157",
            "10.1086/104710",
            "10.1086/105874",
            "10.1086/511976",
            "10.1086/522786",
            "10.1086/523598",
            "10.1086/522794",
            "10.1086/522302",
            "10.1086/522782",
            "10.1086/521647",
            "10.1086/523597",
            "10.1086/522886",
            "10.1086/522053",
            "10.1086/522334",
            "10.1086/523596",
            "10.1086/521358",
            "10.1086/522054",
            "10.1086/521819",
            "10.1086/522627",
            "10.1086/522626",
            "10.1086/522962",
            "10.1086/522831",
            "10.1086/522783",
            "10.1086/522793",
            "10.1086/521817",
            "10.1086/523631",
            "10.1086/521821",
            "10.1086/522945",
            "10.1086/521927",
            "10.1086/522888",
            "10.1086/522787",
            "10.1086/518558",
            "10.1086/522369",
            "10.1086/522963",
            "10.1086/521985",
            "10.1086/522784",
            "10.1086/522229",
            "10.1086/522795",
            "10.1086/521926",
            "10.1086/521645",
            "10.1086/522205",
            "10.1086/521649",
            "10.1086/522628",
            "10.1086/522943",
            "10.1086/521925",
            "10.1086/521984",
            "10.1086/522112",
            "10.1086/521651",
            "10.1086/112571",
            "10.1086/112836",
            "10.1086/112324",
            "10.1086/112352",
            "10.1086/112337",
            "10.1086/112331",
            "10.1086/112325",
            "10.1086/112366",
            "10.1086/112354",
            "10.1086/112370",
            "10.1086/112332",
            "10.1086/112326",
            "10.1086/521552",
            "10.1086/520921",
            "10.1086/521432",
            "10.1086/521022",
            "10.1086/521434",
            "10.1086/521356",
            "10.1086/520958",
            "10.1086/520878",
            "10.1086/521396",
            "10.1086/519974",
            "10.1086/521397",
            "10.1086/521392",
            "10.1086/520807",
            "10.1086/521707",
            "10.1086/521652",
            "10.1086/521556",
            "10.1086/521394",
            "10.1086/521343",
            "10.1086/521823",
            "10.1086/521341",
            "10.1086/521554",
            "10.1086/521429",
            "10.1086/519975",
            "10.1086/519379",
            "10.1086/521148",
            "10.1086/520500",
            "10.1086/520813",
            "10.1086/521815",
            "10.1086/521555",
            "10.1086/521703",
            "10.1086/521430",
            "10.1086/520641"
    };

    public static void main(String[] args) {
        LibraryLoader.load();
        GrobidProperties.getInstance();

        CrossrefClient client = CrossrefClient.getInstance();
        WorkDeserializer workDeserializer = new WorkDeserializer();
        long threadId = Thread.currentThread().getId();
        try {

            for (int i = 0; i < DOIs.length; i++) {
                String doi = DOIs[i];
                final int id = i;
                Map<String, String> arguments = new HashMap<>();
                arguments.put("doi", doi);
                // ASYNCHRONOUS TEST (50 first requests)
                if (i < 90) {

                    client.pushRequest("works", arguments, workDeserializer, threadId, new CrossrefRequestListener<>() {

                        @Override
                        public void onSuccess(List<BiblioItem> results) {
                            System.out.println("Success request " + id);
                            for (BiblioItem biblio : results) {
                                System.out.println(" -> res title: " + biblio.getTitle());
                            }
                        }

                        @Override
                        public void onError(int status, String message, Exception exception) {
                            System.out.println("ERROR (" + status + ") : " + message);
                        }
                    });

                }
                // SYNCHRONOUS TEST (10 last requests)
                else {

                    CrossrefRequestListener<BiblioItem> requestListener = new CrossrefRequestListener<>();
                    client.pushRequest("works", arguments, workDeserializer, threadId, requestListener);

                    synchronized (requestListener) {
                        try {
                            requestListener.wait(6000); // timeout after 6 seconds
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    CrossrefRequestListener.Response<BiblioItem> response = requestListener.getResponse();

                    if (response == null)
                        System.out.println("ERROR : No response ! Maybe timeout.");

                    else if (response.hasError() || !response.hasResults())
                        System.out.println("ERROR (" + response.status + ") : " + response.errorMessage);

                    else { // success
                        System.out.println("Success request " + id);
                        for (BiblioItem biblio : response.results) {
                            System.out.println(" -> res title: " + biblio.getTitle());
                        }
                    }

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
