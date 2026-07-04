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
package org.grobid.core.data;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represent the funding / acknowledgement statement
 */
public class FundingAcknowledgmentParse {
    List<Funding> fundingList = new ArrayList<>();
    List<Person> personList = new ArrayList<>();
    List<Affiliation> affiliations = new ArrayList<>();
    //    List<Pair<OffsetPosition, Element> statementAnnotations = new ArrayList<>();

    public List<Funding> getFundings() {
        return fundingList;
    }

    public void setFundings(List<Funding> fundingList) {
        this.fundingList = fundingList;
    }

    public List<Person> getPersons() {
        return personList;
    }

    public void setPersons(List<Person> personList) {
        this.personList = personList;
    }

    public List<Affiliation> getAffiliations() {
        return affiliations;
    }

    public void setAffiliations(List<Affiliation> fundingBodies) {
        this.affiliations = fundingBodies;
    }

    //    public List<GrobidAnnotation> getStatementAnnotations() {
    //        return statementAnnotations;
    //    }

    //    public void setStatementAnnotations(List<GrobidAnnotation> statementAnnotations) {
    //        this.statementAnnotations = statementAnnotations;
    //    }
}
