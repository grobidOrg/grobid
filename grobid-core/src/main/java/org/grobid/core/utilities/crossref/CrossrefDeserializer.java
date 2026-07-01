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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Abstract deserializer to parse json response from crossref.
 * Normalize results to a list of java objects even if only one result is given.
 * As example: WorkDeserializer.
 *
 */
public abstract class CrossrefDeserializer<T extends Object> extends JsonDeserializer<List<T>> {

    protected ObjectMapper mapper;
    protected SimpleModule module;

    public CrossrefDeserializer() {
        mapper = new ObjectMapper();
        module = new SimpleModule();
        module.addDeserializer(List.class, this);
        mapper.registerModule(module);
    }

    /**
     * Describe how to deserialize one json item from response
     */
    protected abstract T deserializeOneItem(JsonNode item);

    /**
     * Parse a json String, usually the response body. Give back a list of java objects.
     */
    @SuppressWarnings("unchecked")
    public List<T> parse(String body) throws JsonParseException, JsonMappingException, IOException {
        return (List<T>) mapper.readValue(body, List.class);
    }

    /**
     * Normalize results to get always an object list even if you fetch only one object.
     */
    protected ArrayNode normalizeResults(JsonParser parser) throws IOException {
        JsonNode treeNode = parser.readValueAsTree();
        ArrayNode results = null;

        JsonNode messageNode = treeNode.get("message");

        if (messageNode == null || !messageNode.isObject()) {
            //throw new ClientProtocolException("No message found in json result.");
            // glutton
            results = mapper.createArrayNode();
            results.add(treeNode);
        } else {
            ObjectNode message = (ObjectNode) messageNode;
            JsonNode itemsNode = message.get("items");

            if (itemsNode == null || !itemsNode.isArray()) {
                results = mapper.createArrayNode();
                results.add(message);
            } else
                results = (ArrayNode) itemsNode;
        }

        return results;
    }

    @Override
    public List<T> deserialize(JsonParser parser, DeserializationContext context) throws IOException {

        ArrayList<T> res = new ArrayList<T>();

        ArrayNode items = normalizeResults(parser);

        Iterator<JsonNode> it = items.elements();
        while (it.hasNext()) {

            JsonNode item = it.next();

            T one = deserializeOneItem(item);

            res.add(one);
        }

        return res;
    }
}
