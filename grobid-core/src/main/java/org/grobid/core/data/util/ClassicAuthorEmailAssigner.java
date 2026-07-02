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
package org.grobid.core.data.util;

import java.util.ArrayList;
import java.util.List;

import org.grobid.core.data.Person;
import org.grobid.core.utilities.TextUtilities;

public class ClassicAuthorEmailAssigner implements AuthorEmailAssigner {

    @Override
    public void assign(List<Person> fullAuthors, List<String> emails) {
        List<Integer> winners = new ArrayList<Integer>();

        // if 1 email and 1 author, not too hard...
        if (fullAuthors != null) {
            if ((emails.size() == 1) && (fullAuthors.size() == 1)) {
                fullAuthors.get(0).setEmail(emails.get(0));
            } else {
                // we associate emails to the authors based on string proximity
                for (String mail : emails) {
                    int maxDist = 1000;
                    int best = -1;
                    int ind = mail.indexOf("@");
                    if (ind != -1) {
                        String name = mail.substring(0, ind).toLowerCase();
                        int k = 0;
                        for (Person aut : fullAuthors) {
                            Integer kk = k;
                            if (!winners.contains(kk)) {
                                List<String> emailVariants = TextUtilities
                                        .generateEmailVariants(aut.getFirstName(), aut.getLastName());

                                for (String variant : emailVariants) {
                                    variant = variant.toLowerCase();

                                    int dist = TextUtilities.getLevenshteinDistance(name, variant);
                                    if (dist < maxDist) {
                                        best = k;
                                        maxDist = dist;
                                    }
                                }
                            }
                            k++;
                        }

                        // make sure that the best candidate found is not too far
                        if (best != -1 && maxDist < name.length() / 2) {
                            Person winner = fullAuthors.get(best);
                            winner.setEmail(mail);
                            winners.add(best);
                        }
                    }
                }
            }
        }
    }
}
