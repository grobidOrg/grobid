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
    // List<Pair<OffsetPosition, Element> statementAnnotations = new ArrayList<>();

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

    // public List<GrobidAnnotation> getStatementAnnotations() {
    // return statementAnnotations;
    // }

    // public void setStatementAnnotations(List<GrobidAnnotation> statementAnnotations) {
    // this.statementAnnotations = statementAnnotations;
    // }
}
