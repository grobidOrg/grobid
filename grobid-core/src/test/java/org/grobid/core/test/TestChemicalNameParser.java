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
package org.grobid.core.test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;

import org.grobid.core.data.ChemicalEntity;
import org.grobid.core.exceptions.GrobidException;

@Ignore
public class TestChemicalNameParser extends EngineTest {

    public File getResourceDir(String resourceDir) {
        File file = new File(resourceDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new GrobidException("Cannot start test, because test resource folder is not correctly set.");
            }
        }
        return (file);
    }

    //@Test
    public void testChemicalNameParser() throws Exception {
        File textFile = new File(
                this.getResourceDir("./src/test/resources/").getAbsoluteFile() + "/patents/sample3.txt");
        if (!textFile.exists()) {
            throw new GrobidException("Cannot start test, because test resource folder is not correctly set.");
        }
        String text = FileUtils.readFileToString(textFile, StandardCharsets.UTF_8);

        List<ChemicalEntity> chemicalResults = engine.extractChemicalEntities(text);
        if (chemicalResults != null) {
            System.out.println(chemicalResults.size() + " extracted chemical entities");
            for (ChemicalEntity entity : chemicalResults) {
                System.out.println(entity.toString());
            }
        } else {
            System.out.println("no extracted chemical entities");
        }
    }

}
