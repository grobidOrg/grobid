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

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.lexicon.LexiconIntegrationTest;

public class Runner {

    public static void main(String[] args) {
        GrobidFactory.getInstance();
        int totalTestRuns = 0;
        int totalFailures = 0;

        // test date parser

        Result result = JUnitCore.runClasses(TestDate.class);
        totalTestRuns++;
        System.out.print("test Date: ");

        if (result.getFailures().size() == 0) {
            System.out.println("OK");
        }

        for (Failure failure : result.getFailures()) {
            System.out.println(failure.toString());
            totalFailures++;
        }

        // test name parser for headers

        result = JUnitCore.runClasses(TestNameParser.class);
        totalTestRuns++;
        System.out.print("test NameParser: ");

        if (result.getFailures().size() == 0) {
            System.out.println("OK");
        }

        for (Failure failure : result.getFailures()) {
            System.out.println(failure.toString());
            totalFailures++;
        }

        // test lexicon fast matcher

        result = JUnitCore.runClasses(LexiconIntegrationTest.class);
        totalTestRuns++;
        System.out.print("test Lexicon Fast Matcher: ");

        if (result.getFailures().size() == 0) {
            System.out.println("OK");
        }

        for (Failure failure : result.getFailures()) {
            System.out.println(failure.toString());
            totalFailures++;
        }

        // test affiliation parser

        result = JUnitCore.runClasses(TestAffiliationAddressParser.class);
        totalTestRuns++;
        System.out.print("test AffiliationAddressParser: ");

        if (result.getFailures().size() == 0) {
            System.out.println("OK");
        }

        for (Failure failure : result.getFailures()) {
            System.out.println(failure.toString());
            totalFailures++;
        }

        // test header parser

        result = JUnitCore.runClasses(TestHeaderParser.class);
        totalTestRuns++;
        System.out.print("test HeaderParser: ");

        if (result.getFailures().size() == 0) {
            System.out.println("OK");
        }

        for (Failure failure : result.getFailures()) {
            System.out.println(failure.toString());
            totalFailures++;
        }

        result = JUnitCore.runClasses(TestCitationParser.class);
        totalTestRuns++;
        System.out.print("test CitationParser: ");

        if (result.getFailures().size() == 0) {
            System.out.println("OK");
        }

        for (Failure failure : result.getFailures()) {
            System.out.println(failure.toString());
            totalFailures++;
        }

        result = JUnitCore.runClasses(TestReferencesParser.class);
        totalTestRuns++;
        System.out.print("test ReferencesParser: ");

        if (result.getFailures().size() == 0) {
            System.out.println("OK");
        }

        for (Failure failure : result.getFailures()) {
            System.out.println(failure.toString());
            totalFailures++;
        }

        result = JUnitCore.runClasses(TestFullTextParser.class);
        totalTestRuns++;
        System.out.print("test FullTextParser: ");

        if (result.getFailures().size() == 0) {
            System.out.println("OK");
        }

        for (Failure failure : result.getFailures()) {
            System.out.println(failure.toString());
            totalFailures++;
        }

        result = JUnitCore.runClasses(TestCitationPatentParser.class);
        totalTestRuns++;
        System.out.print("test CitationPatentParser: ");

        if (result.getFailures().size() == 0) {
            System.out.println("OK");
        }

        for (Failure failure : result.getFailures()) {
            System.out.println(failure.toString());
            totalFailures++;
        }

        /*result = JUnitCore.runClasses(TestChemicalNameParser.class);
        totalTestRuns++;
        System.out.print("test ChemicalNameParser: ");

        if (result.getFailures().size() == 0) {
        	System.out.println("OK");
        }

        for (Failure failure : result.getFailures()) {
        	System.out.println(failure.toString());
        	totalFailures++;
        }
        */

        /*result = JUnitCore.runClasses(TestEbookParser.class);
        totalTestRuns++;
        System.out.print("test EbookParser: ");

        if (result.getFailures().size() == 0) {
        	System.out.println("OK");
        }

        for (Failure failure : result.getFailures()) {
        	System.out.println(failure.toString());
        	totalFailures++;
        }*/

        System.out.println("Test run: " + totalTestRuns + ", Failures: " + totalFailures);
    }

}
