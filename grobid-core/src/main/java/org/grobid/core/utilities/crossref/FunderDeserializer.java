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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.grobid.core.data.Funder;
import org.grobid.core.utilities.TextUtilities;

/**
 * Convert a JSON Funder model - from a glutton or crossref response - to a Funder object
 * (understandable by this stupid GROBID).
 *
 * Input JSON format is from the REST API query. For example:
 * https://api.crossref.org/funders?query=agence+nationale+de+la+recherche
 *
 * For better data (Crossref funder registry one), we can then use the data API:
 * http://data.crossref.org/fundingdata/funder/10.13039/501100001665
 *
 */
public class FunderDeserializer extends CrossrefDeserializer<Funder> {

    @Override
    protected Funder deserializeOneItem(JsonNode item) {
        Funder funder = null;
        String type = null; // the crossref type of the item, see http://api.crossref.org/types

        if (item.isObject()) {
            funder = new Funder();
            //System.out.println(item.toString());

            JsonNode locationNode = item.get("location");
            if (locationNode != null && (!locationNode.isMissingNode())) {
                String location = locationNode.asText();
                funder.setCountry(location);
            }

            // we always have a uri field, and we can get the DOI from this...
            // surprisingly no DOI field !
            JsonNode uriNode = item.get("uri");
            if (uriNode != null && (!uriNode.isMissingNode())) {
                String uri = uriNode.asText();
                if (uri != null)
                    uri = uri.replace("http://dx.doi.org/", "");
                funder.setDoi(uri);
            }

            JsonNode nameNode = item.get("name");
            if (nameNode != null && (!nameNode.isMissingNode())) {
                String name = nameNode.asText();
                funder.setFullName(name);
            }

            JsonNode altNamesNode = item.get("alt-names");
            if (altNamesNode != null && (!altNamesNode.isMissingNode()) &&
                    altNamesNode.isArray() && (((ArrayNode) altNamesNode).size() > 0)) {

                // here we just keep an acronym form - better names with lang info via the data API
                for (int i = 0; i < ((ArrayNode) altNamesNode).size(); i++) {
                    String altName = ((ArrayNode) altNamesNode).get(i).asText();
                    if (altName.equals("INCa")) {
                        funder.setAbbreviatedName("INCa");
                        funder.setFullName("Institut National du Cancer");
                        break;
                    } else if (altName.equals("Anses")) {
                        funder.setAbbreviatedName("Anses");
                        funder.setFullName("Agence nationale de recherches sur le sida et les hépatites virales");
                        break;
                    } else if (TextUtilities.isAllUpperCase(altName) && altName.length() < 10) {
                        funder.setAbbreviatedName(altName);
                        break;
                    }
                }
            }

            //System.out.println(funder.toTEI());
        }

        return funder;
    }
}
