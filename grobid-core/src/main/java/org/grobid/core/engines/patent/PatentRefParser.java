package org.grobid.core.engines.patent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.*;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.data.PatentItem;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.TextUtilities;

/**
 * Parser for patent references based on regular language rewriting.
 * Input raw references are WISIWIG references (i.e. reference string as
 * they appear). Expected output is the patent reference in the EPO Epoque
 * format.
 *
 */

public class PatentRefParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceExtractor.class);

    private String rawText = null;
    private int rawTextOffset = 0; // starting offset of the current raw text
    private Pattern patent_pattern = null;
    private Pattern number_pattern = null;

    // this is the complete list of existing authorities that was identified in the nature, always
    // two upper-case letter codes
    static public List<String> authorities = Arrays.asList(
            "AP",
            "AL",
            "DZ",
            "AR",
            "AU",
            "AT",
            "BE",
            "BX",
            "BR",
            "BG",
            "CA",
            "CL",
            "CN",
            "CO",
            "HR",
            "CU",
            "CY",
            "CZ",
            "CS",
            "DK",
            "EG",
            "EA",
            "EP",
            "DE",
            "DD",
            "FI",
            "FR",
            "GB",
            "GR",
            "HK",
            "HU",
            "IS",
            "IN",
            "ID",
            "IB",
            "TP",
            "IR",
            "IQ",
            "IE",
            "IL",
            "IT",
            "JP",
            "JO",
            "KE",
            "KP",
            "KR",
            "LV",
            "LT",
            "LU",
            "MW",
            "MY",
            "MX",
            "MD",
            "MC",
            "MN",
            "MA",
            "NL",
            "NZ",
            "NG",
            "NO",
            "OA",
            "WO",
            "PE",
            "PH",
            "PL",
            "PT",
            "RD",
            "RO",
            "RU",
            "SA",
            "SG",
            "SK",
            "SI",
            "ZA",
            "SU",
            "ES",
            "LK",
            "SE",
            "CH",
            "TW",
            "TH",
            "TT",
            "TN",
            "TR",
            "UA",
            "GB",
            "US",
            "UY",
            "VE",
            "VN",
            "YU",
            "ZM",
            "ZW");

    // this is the list of supported languages - language codes given ISO 639-1, two-letter codes
    static public List<String> languages = Arrays.asList("en", "de", "fr", "es", "it", "ja", "ko", "pt", "zh", "ar");

    // list of regular expressions for identifying the authority in the raw reference string
    private List<Pattern> autority_patterns = new ArrayList<Pattern>();

    // map giving for a language and an authority name the list of language specific expressions
    // this uses the language resource files *.local under grobid-home/lexicon/patent/
    private Map<String, List<String>> languageResources = null;

    private Pattern application_pattern = null;
    private Pattern publication_pattern = null;
    private Pattern pct_application_pattern = null;
    private Pattern provisional_pattern = null;
    private Pattern non_provisional_pattern = null;
    private Pattern us_serial_pattern = null;
    private Pattern translation_pattern = null;
    private Pattern utility_pattern = null;
    private Pattern kindcode_pattern1 = null;
    private Pattern kindcode_pattern2 = null;
    private Pattern kindcode_pattern3 = null;
    private Pattern jp_kokai_pattern = null;
    private Pattern jp_heisei_pattern = null;
    private Pattern standardText = null;

    public PatentRefParser() {
        patent_pattern = Pattern
                .compile("([UEWDJFA])[\\.\\s]?([SPOERKU])[\\.\\s]?-?(A|B|C)?\\s?-?([\\s,0-9/-]+(A|B|C)?[\\s,0-9/-]?)");

        //number_pattern = Pattern.compile("[ABC]?([\\s,0-9/-]*)+[ABC]?([\\s,0-9/-])*[ABC]?");
        number_pattern = Pattern.compile(
                "((RE|PP)[\\s,0-9/\\-\\.\\\\]*)|(PCT(/|\\\\)[A-Z][A-Z]([\\s,0-9/\\-\\.\\\\]*))|([ABC]?([0-9][\\s,0-9/\\-\\.\\\\]*)+[ABCUT]?([\\s,0-9/\\-\\.\\\\])*[ABCUT]?)");
        //number_pattern = Pattern.compile("((RE|PP)[\\s,0-9/\\-\\.\\\\]*)|(PCT(/|\\\\)[A-Z][A-Z]([\\s,0-9/\\-\\.\\\\]*))|([ABC]?([0-9][\\s,0-9/\\-\\.\\\\]*)+[ABCUT][0-9])|([ABC]?([0-9][\\s,0-9/\\-\\.\\\\]*)+[ABCUT])|([ABC]?([0-9][\\s,0-9/\\-\\.\\\\]*)+)");
        //number_pattern = Pattern.compile("((RE|PP)[\\s,0-9/\\-\\.\\\\]*)|(PCT(/|\\\\)[A-Z][A-Z]([\\s,0-9/\\-\\.\\\\]*))|([ABC]?([\\s,0-9/\\-\\.\\\\ABC])+[ABCUT]?)");
        kindcode_pattern1 = Pattern.compile("([ABC][0-9]?)"); // before number
        kindcode_pattern2 = Pattern.compile("([ABCUT][0-9]?)"); // after number
        kindcode_pattern3 = Pattern.compile("^([ABC][0-9]?)-"); // as prefix of the number

        standardText = Pattern.compile("[a-z][A-Z]");

        //application_pattern = Pattern.compile("((A|a)pplicat)|((a|A)ppln)");
        //publication_pattern = Pattern.compile("((P|p)ublicat)|((p|P)ub)");
        pct_application_pattern = Pattern.compile(
                "(PCT/(GB|EP|US|JP|DE|FR|UK|BE|CA|CH|AT|AU|KR|RU|FI|NL|SE|ES|DK|DD)/?([0-9][0-9]([0-9][0-9])?))");
        //provisional_pattern = Pattern.compile("((P|p)rovisional)");
        non_provisional_pattern = Pattern.compile("((n|N)on.(P|p)rovisional)");
        translation_pattern = Pattern.compile("((T|t)ranslation)");
        //utility_pattern = Pattern.compile("((U|u)tility)");

        us_serial_pattern = Pattern.compile("((S|s)erial(\\s|-)+((n|N)o(\\.)?)(\\s|-)*[0-9]*/)");

        jp_kokai_pattern = Pattern.compile("(k|K)oka(l|i)");
        jp_heisei_pattern = Pattern.compile("(H|h)(E|e)(I|i)");

        initLanguageResources();

        // we compile the different authority regular expression patterns based on the language resource files
        for (String authorityName : authorities) {
            autority_patterns.add(compilePattern(authorityName));
        }

        // compiling additional non-authority patterns: application, publication, provisional, utility
        application_pattern = compilePattern("application");
        publication_pattern = compilePattern("publication");
        provisional_pattern = compilePattern("provisional");
        utility_pattern = compilePattern("utility");

        // note: multilingual lexical patterns are expressed in external resource files under grobid-home/lexicon/patent/
    }

    private final void initLanguageResources() {
        languageResources = new TreeMap<String, List<String>>();
        for (String language : languages) {
            // opening the corresponding language resource file
            String path = GrobidProperties.getGrobidHomePath() + "/lexicon/patent/" + language + ".local";
            File localFile = new File(path);
            if (!localFile.exists()) {
                throw new GrobidResourceException(
                        "Cannot add language resources for patent processing (language '"
                                + language
                                +
                                "'), because file '"
                                + localFile.getAbsolutePath()
                                + "' does not exists.");
            }
            if (!localFile.canRead()) {
                throw new GrobidResourceException(
                        "Cannot add language resources for patent processing (language '"
                                + language
                                +
                                "'), because cannot read file '"
                                + localFile.getAbsolutePath()
                                + "'.");
            }

            InputStream ist = null;
            InputStreamReader isr = null;
            BufferedReader dis = null;
            try {
                ist = new FileInputStream(localFile);
                isr = new InputStreamReader(ist, "UTF8");
                dis = new BufferedReader(isr);

                String l = null;
                while ((l = dis.readLine()) != null) {
                    if (l.length() == 0)
                        continue;
                    // the first token, separated by a '=', gives the authority name
                    String[] parts = l.split("=");
                    String authority = parts[0].trim();
                    // this will cover authority as well as some other patterns such as publication, application, ...
                    String expressions = parts[1].trim();
                    if (expressions.trim().length() > 0) {
                        String[] subparts = expressions.split(",");
                        List<String> listExpressions = new ArrayList<String>();
                        for (int i = 0; i < subparts.length; i++) {
                            listExpressions.add(subparts[i].trim());
                        }
                        languageResources.put(language + authority, listExpressions);
                    }
                }
            } catch (FileNotFoundException e) {
                throw new GrobidException("An exception occurred while running Grobid.", e);
            } catch (IOException e) {
                throw new GrobidException("An exception occurred while running Grobid.", e);
            } finally {
                IOUtils.closeQuietly(ist, isr, dis);
            }
        }
    }

    private Pattern compilePattern(String authorityName) {
        // default authority two character name
        String er = "((\\s|,|\\.|^|\\-)";
        er += authorityName + ")";

        if (authorityName.length() == 2) {
            // authority name with dots
            er += "|(" + authorityName.charAt(0) + "\\.(\\s)?" + authorityName.charAt(1) + ")";
        }

        // using language resources for authority patterns
        for (String language : languages) {
            List<String> expressions = languageResources.get(language + authorityName);
            if (expressions != null) {
                for (String expression : expressions) {
                    if ((expression != null) && (expression.trim().length() > 1)) {
                        expression = expression.trim();

                        if (!expression.contains("-") && !expression.contains(".")) {
                            if (TextUtilities.isAllLowerCase(expression)) {
                                expression = "("
                                        + expression.charAt(0)
                                        + "|"
                                        + Character.toUpperCase(expression.charAt(0))
                                        + ")"
                                        + expression.substring(1, expression.length());
                            }
                        } else {
                            if (expression.contains("-")) {
                                String[] parts = expression.split("-");
                                expression = "";
                                for (int j = 0; j < parts.length; j++) {
                                    String part = parts[j];
                                    if (j > 0) {
                                        expression += "(\\s|-)*";
                                    }

                                    if (TextUtilities.isAllLowerCase(part)) {
                                        expression += "("
                                                + part.charAt(0)
                                                + "|"
                                                + Character.toUpperCase(part.charAt(0))
                                                + ")"
                                                + part.substring(1, part.length());
                                    }
                                }
                            }

                            if (expression.contains(".")) {
                                String[] parts = expression.split(".");
                                expression = "";
                                for (int j = 0; j < parts.length; j++) {
                                    String part = parts[j];
                                    if (j > 0) {
                                        expression += "(\\s)?\\.(\\s)?";
                                    }

                                    if (TextUtilities.isAllLowerCase(part)) {
                                        expression += "("
                                                + part.charAt(0)
                                                + "|"
                                                + Character.toUpperCase(part.charAt(0))
                                                + ")"
                                                + part.substring(1, part.length());
                                    }
                                }
                            }
                        }
                        er += "|(" + expression + ")";
                    }
                }
            }
        }

        return Pattern.compile(er);
    }

    public void setRawRefText(String s) {
        rawText = s;
    }

    public void setRawRefTextOffset(int s) {
        rawTextOffset = s;
    }

    public List<PatentItem> processRawRefText() {
        List<PatentItem> res = new ArrayList<PatentItem>();
        //System.out.println("processRawRefText: " + rawText);
        String country = null;
        int country_position = -1;
        while (true) {
            Matcher fitCountry = null;

            int i = 0;
            for (String authority : authorities) {
                Pattern thePattern = autority_patterns.get(i);

                fitCountry = thePattern.matcher(rawText);
                if (fitCountry.find()) {
                    country = authority;
                    country_position = fitCountry.end();
                    break;
                }
                i++;
            }
            break;
        }

        if (country != null) {
            List<String> numbers = new ArrayList<String>();
            List<Integer> offsets_begin = new ArrayList<Integer>();
            List<Integer> offsets_end = new ArrayList<Integer>();
            Matcher fitNumber = number_pattern.matcher(rawText);
            while (fitNumber.find()) {
                String toto = fitNumber.group(0);

                int inde_begin = rawText.indexOf(toto) + rawTextOffset;
                int inde_end = inde_begin + toto.length() - 1;
                //toto = toto.replaceAll("(A|B|C|\\s|-|/)", "");
                //toto = toto.replaceAll("(-)", "");
                toto = toto.replaceAll("()", "");
                if (toto.length() > 0) {
                    boolean notPieces = true;
                    // additional tests are necessary for , and .
                    if (toto.length() > 14) {
                        // we have mostlikely two patents separated by a ,
                        // count the number of ,
                        int countComma = 0;
                        String[] pieces = null;
                        pieces = toto.split(",");
                        countComma = pieces.length - 1;

                        if (countComma > 0) {
                            // we split depending on number of comma
                            double ratio = (double) toto.length() / countComma;
                            if (ratio < 10)
                                pieces = toto.split(", ");
                            if (pieces.length == 2) {
                                if (((pieces[0].length() > pieces[1].length()) &&
                                        (pieces[0].length() - pieces[1].length() < 4)) ||
                                        ((pieces[0].length() <= pieces[1].length()) &&
                                                (pieces[1].length() - pieces[0].length() < 4))) {
                                    for (int i = 0; i < 2; i++) {
                                        String piece = pieces[i];
                                        addNumber(
                                                numbers,
                                                offsets_begin,
                                                offsets_end,
                                                piece,
                                                inde_begin,
                                                inde_end,
                                                toto);
                                    }
                                    notPieces = false;
                                }
                            } else if ((toto.length() > (6 * pieces.length))) {
                                for (int i = 0; i < pieces.length; i++) {
                                    String piece = pieces[i];
                                    addNumber(numbers, offsets_begin, offsets_end, piece, inde_begin, inde_end, toto);
                                }
                                notPieces = false;
                            }
                        }

                    }

                    if (notPieces) {
                        addNumber(numbers, offsets_begin, offsets_end, toto, inde_begin, inde_end, null);
                    }
                }
            }

            List<Boolean> applications = new ArrayList<Boolean>();
            List<Boolean> provisionals = new ArrayList<Boolean>();
            List<Boolean> pctapps = new ArrayList<Boolean>();
            List<Boolean> designs = new ArrayList<Boolean>();
            List<Boolean> reissueds = new ArrayList<Boolean>();
            List<Boolean> plants = new ArrayList<Boolean>();
            List<String> kindcodes = new ArrayList<String>();

            for (String number : numbers) {
                applications.add(Boolean.valueOf(false));
                provisionals.add(Boolean.valueOf(false));
                pctapps.add(Boolean.valueOf(false));
                designs.add(Boolean.valueOf(false));
                reissueds.add(Boolean.valueOf(false));
                plants.add(Boolean.valueOf(false));
                kindcodes.add(null);
            }

            List<String> newNumbers = new ArrayList<String>();
            List<String> originalNumbers = new ArrayList<String>();
            int i = 0;
            int lastPositionVisited = country_position;
            for (String number : numbers) {
                String originalNumber = number;

                // try to get the kind code
                boolean kindCodeFound = false;
                // do we have the kind code directly in the number prefix?
                Matcher fitKindCode = kindcode_pattern3.matcher(number);
                if (fitKindCode.find()) {
                    String tata = fitKindCode.group(0);
                    int posKind = fitKindCode.end();
                    // if we have standard text between the kind code and the number, the kind code is not valid
                    tata = tata.replaceAll("[- ]", "");
                    kindcodes.set(i, tata);

                    lastPositionVisited = offsets_end.get(i) - rawTextOffset;
                    kindCodeFound = true;
                    int ind = number.indexOf("-");
                    number = number.substring(ind, number.length());
                }

                if (!kindCodeFound && (offsets_begin.get(i) - rawTextOffset >= lastPositionVisited)) {
                    // is there a kind code between the last position and position of this number?
                    String interChunk = rawText.substring(lastPositionVisited, (offsets_begin.get(i) - rawTextOffset));
                    fitKindCode = kindcode_pattern1.matcher(interChunk);
                    if (fitKindCode.find()) {
                        String tata = fitKindCode.group(0);
                        int posKind = fitKindCode.end();

                        // if we have standard text between the kind code and the number, the kind code is not valid
                        String subChunk = interChunk.substring(posKind, interChunk.length());
                        Matcher m = standardText.matcher(subChunk);
                        // just try to find a match
                        if (!m.find()) {
                            // if the distance between the kind code and the number is too large,
                            // the kind code is not valid

                            if (interChunk.length() - posKind <= 4) {
                                // otherwise, we validated the kind code for this patent reference
                                kindcodes.set(i, tata);
                                if (offsets_end.get(i) < rawTextOffset) {
                                    offsets_end.set(i, offsets_end.get(i) + rawTextOffset);
                                }
                                lastPositionVisited = offsets_end.get(i) - rawTextOffset;
                                kindCodeFound = true;
                            }
                        }
                    }
                }

                if (!kindCodeFound) {
                    // is there a kind code immediately after the number?
                    int postLength = 0;
                    if (rawText.length() - (offsets_end.get(i) - rawTextOffset) >= 3)
                        postLength = 3;
                    else
                        postLength = rawText.length() - (offsets_end.get(i) - rawTextOffset);
                    if (postLength > 0) {
                        String postChunk = rawText.substring(
                                (offsets_end.get(i) - rawTextOffset - 1),
                                (offsets_end.get(i) - rawTextOffset) + postLength);
                        fitKindCode = kindcode_pattern2.matcher(postChunk);
                        if (fitKindCode.find()) {
                            String tata = fitKindCode.group(0);
                            kindcodes.set(i, tata);
                            kindCodeFound = true;
                            lastPositionVisited = (offsets_end.get(i) + postLength) - rawTextOffset;
                        }
                    }
                }

                number = number.replace("-", "");
                // do we have an application or a patent publication?
                if (country.equals("WO") || country.equals("W0")) {
                    number = number.replaceAll("[\\.\\s]", "");
                    // in case of usual typo W0 for WO
                    String numm = number.replaceAll("[/,\\.]", "").trim();
                    originalNumber = numm;
                    if ((numm.startsWith("0")) && (numm.length() == 11)) {
                        // a useless zero has been inserted
                        number = number.substring(1, number.length());
                    } else if (((numm.startsWith("09")) && (numm.length() == 8)) ||
                            ((numm.startsWith("00")) && (numm.length() == 8))) {
                        // a useless zero has been inserted (WO format before July 2002!)
                        number = number.substring(1, number.length());
                    }
                    // PCT application checking
                    Matcher fitApplication = pct_application_pattern.matcher(number);
                    if (fitApplication.find()) {
                        String titi = fitApplication.group(0);
                        int move = titi.length();
                        boolean application = true;
                        titi = titi.replace("PCT/", "");
                        if (titi.length() > 2) {
                            String countr = titi.substring(0, 2);
                            String year = null;
                            if (titi.charAt(2) != '/')
                                year = titi.substring(2, titi.length());
                            else
                                year = titi.substring(3, titi.length());
                            if (year.length() == 2) {
                                if ((year.charAt(0) == '7') || (year.charAt(0) == '8') || (year.charAt(0) == '9')) {
                                    year = "19" + year;
                                } else {
                                    year = "20" + year;
                                }
                            } else if ((year.length() != 4) && (year.length() > 1)) {
                                year = year.substring(0, 2);
                                if ((year.charAt(0) == '7') || (year.charAt(0) == '8') || (year.charAt(0) == '9')) {
                                    year = "19" + year;
                                } else {
                                    year = "20" + year;
                                }
                            } else if ((titi.charAt(2) == '/') && (year.length() == 4) && (year.length() > 1)) {
                                year = year.substring(0, 2);
                                move = move - 1;
                                if ((year.charAt(0) == '7') || (year.charAt(0) == '8') || (year.charAt(0) == '9')) {
                                    year = "19" + year;
                                } else {
                                    year = "20" + year;
                                }
                            }
                            number = year + countr + number.substring(move, number.length());
                            number = number.replaceAll("[/,\\.]", "").trim();
                            if (number.length() == 12) {
                                if (number.charAt(6) == '0')
                                    number = number.substring(0, 6) + number.substring(7, 12);
                            }
                        }
                        applications.set(i, Boolean.valueOf(true));
                        pctapps.set(i, Boolean.valueOf(true));
                    }

                } else {
                    Matcher fitApplication = application_pattern.matcher(rawText);
                    Matcher fitPublication = publication_pattern.matcher(rawText);

                    boolean appli = fitApplication.find();
                    boolean publi = fitPublication.find();

                    if (appli && !publi) {
                        applications.set(i, Boolean.valueOf(true));
                    }
                    if (publi) {
                        applications.set(i, Boolean.valueOf(false));
                    }

                    if (country.equals("EP")) {
                        String numm = number.replaceAll("[ABCU,\\.\\s/]", "").trim();
                        originalNumber = numm;
                        if ((numm.length() == 8)) {
                            applications.set(i, Boolean.valueOf(true));
                            // epodoc format with the full year as prefix
                            if (numm.startsWith("0") || numm.startsWith("1")) {
                                number = "20" + numm.substring(0, 2) + "0" + numm.substring(2, numm.length());
                            } else {
                                // we will have a problem in 2078 guys...
                                number = "19" + numm.substring(0, 2) + "0" + numm.substring(2, numm.length());
                            }
                        } else if (numm.length() <= 7) {
                            applications.set(i, Boolean.valueOf(false));
                        }
                    }
                    if (country.equals("US")) {
                        // do we have a provisional?
                        Matcher fitProvisional = provisional_pattern.matcher(rawText);
                        Matcher fitNonProvisional = non_provisional_pattern.matcher(rawText);

                        if ((fitProvisional.find()) && (!fitNonProvisional.find())) {
                            provisionals.set(i, Boolean.valueOf(true));
                        }

                        // interpretation of prefix "serial code" is given here:
                        // https://www.uspto.gov/web/offices/ac/ido/oeip/taf/filingyr.htm
                        // we need to identify the year based on the serial number range

                        // provisional starts with 60 or 61 or 62 or 63 (time flies!)
                        if (number.startsWith("60") && (appli || number.startsWith("60/"))) {
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(true));
                            originalNumber = number;
                            number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 9474)
                                year = "1995";
                            else if (numb < 34487)
                                year = "1996";
                            else if (numb < 70310)
                                year = "1997";
                            else if (numb < 113787)
                                year = "1998";
                            else if (numb < 173038)
                                year = "1999";
                            else if (numb < 256730)
                                year = "2000";
                            else if (numb < 343564)
                                year = "2001";
                            else if (numb < 437173)
                                year = "2002";
                            else if (numb < 532638)
                                year = "2003";
                            else if (numb < 639450)
                                year = "2004";
                            else if (numb < 754464)
                                year = "2005";
                            else if (numb < 877460)
                                year = "2006";
                            else if (numb < 999999)
                                year = "2007";
                            number = year + "0" + number;
                        } else if (number.startsWith("61") && (appli || number.startsWith("61/"))) {
                            // same as for 60 but the ranges are different
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(true));
                            originalNumber = number;
                            number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 9389)
                                year = "2007";
                            else if (numb < 203947)
                                year = "2008";
                            else if (numb < 335046)
                                year = "2009";
                            else if (numb < 460301)
                                year = "2010";
                            else if (numb < 631245)
                                year = "2011";
                            else if (numb < 848274)
                                year = "2012";
                            else if (numb < 964276)
                                year = "2013";
                            else if (numb < 999999)
                                year = "2014";
                            number = year + "0" + number;
                        } else if (number.startsWith("62") && (appli || number.startsWith("62/"))) {
                            // same as for 60 but the ranges are different
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(true));
                            originalNumber = number;
                            number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 124715)
                                year = "2014";
                            else if (numb < 387330)
                                year = "2015";
                            else if (numb < 498538)
                                year = "2016";
                            else if (numb < 708919)
                                year = "2017";
                            else if (numb < 917758)
                                year = "2018";
                            else if (numb < 974841)
                                year = "2019";
                            else if (numb < 999999)
                                year = "2020";
                            number = year + "0" + number;
                        } else if (number.startsWith("63") && (appli || number.startsWith("63/"))) {
                            // same as for 60 but the ranges are different
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(true));
                            originalNumber = number;
                            number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 205597)
                                year = "2020";
                            else
                                year = "2021";
                            number = year + "0" + number;
                        } else if (number.startsWith("29") && (appli || number.startsWith("29/"))) {
                            // design patent application starts with 29
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            designs.set(i, Boolean.valueOf(true));
                            originalNumber = number;
                            number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 3180)
                                year = "1992";
                            else if (numb < 16976)
                                year = "1993";
                            else if (numb < 32919)
                                year = "1994";
                            else if (numb < 48507)
                                year = "1995";
                            else if (numb < 64454)
                                year = "1996";
                            else if (numb < 81426)
                                year = "1997";
                            else if (numb < 98302)
                                year = "1998";
                            else if (numb < 116135)
                                year = "1999";
                            else if (numb < 134406)
                                year = "2000";
                            else if (numb < 152739)
                                year = "2001";
                            else if (numb < 173499)
                                year = "2002";
                            else if (numb < 196307)
                                year = "2003";
                            else if (numb < 220177)
                                year = "2004";
                            else if (numb < 245663)
                                year = "2005";
                            else if (numb < 270581)
                                year = "2006";
                            else if (numb < 294213)
                                year = "2007";
                            else if (numb < 313375)
                                year = "2008";
                            else if (numb < 348400)
                                year = "2009";
                            else if (numb < 372670)
                                year = "2010";
                            else if (numb < 395318)
                                year = "2011";
                            else if (numb < 442191)
                                year = "2012";
                            else if (numb < 463549)
                                year = "2013";
                            else if (numb < 474693)
                                year = "2014";
                            else if (numb < 505607)
                                year = "2015";
                            else if (numb < 620459)
                                year = "2016";
                            else if (numb < 651149)
                                year = "2017";
                            else if (numb < 651684)
                                year = "2018";
                            else if (numb < 742106)
                                year = "2019";
                            else if (numb < 742402)
                                year = "2020";
                            else
                                year = "2021";
                            number = year + "0" + number;
                        } else if (number.startsWith("17") && (appli || number.startsWith("17/"))) {
                            // standard patent application, not yet in the table
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = "2021";
                            number = year + "0" + number;
                        } else if (number.startsWith("16") && (appli || number.startsWith("16/"))) {
                            // standard patent application, most recent serial code
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 350739)
                                year = "2018";
                            else if (numb < 602938)
                                year = "2019";
                            else if (numb < 974313)
                                year = "2020";
                            else
                                year = "2021";
                            number = year + "0" + number;
                        } else if (number.startsWith("15") && (appli || number.startsWith("15/"))) {
                            // standard patent application
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 530347)
                                year = "2016";
                            else if (numb < 732787)
                                year = "2017";
                            else
                                year = "2018";
                            number = year + "0" + number;
                        } else if (number.startsWith("14") && (appli || number.startsWith("14/"))) {
                            // standard patent application
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 544379)
                                year = "2014";
                            else if (numb < 757791)
                                year = "2015";
                            else
                                year = "2016";
                            number = year + "0" + number;
                        } else if (number.startsWith("13") && (appli || number.startsWith("13/"))) {
                            // standard patent application
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 374487)
                                year = "2011";
                            else if (numb < 694748)
                                year = "2012";
                            else if (numb < 998975)
                                year = "2013";
                            else
                                year = "2014";
                            number = year + "0" + number;
                        } else if (number.startsWith("12") && (appli || number.startsWith("12/"))) {
                            // standard patent application
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 5841)
                                year = "2007";
                            else if (numb < 317884)
                                year = "2008";
                            else if (numb < 655475)
                                year = "2009";
                            else if (numb < 930166)
                                year = "2010";
                            else
                                year = "2011";
                            number = year + "0" + number;
                        } else if (number.startsWith("11") && (appli || number.startsWith("11/"))) {
                            // standard patent application
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 023305)
                                year = "2004";
                            else if (numb < 320178)
                                year = "2005";
                            else if (numb < 646743)
                                year = "2006";
                            else
                                year = "2007";
                            number = year + "0" + number;
                        } else if (number.startsWith("10") && (appli || number.startsWith("10/"))) {
                            // standard patent application
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 32443)
                                year = "2001";
                            else if (numb < 334164)
                                year = "2002";
                            else if (numb < 746297)
                                year = "2003";
                            else
                                year = "2004";
                            number = year + "0" + number;
                        } else if (number.startsWith("9/") || number.startsWith("09/")) {
                            // standard patent application
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            if (number.startsWith("9/"))
                                number = number.substring(2, number.length());
                            else
                                number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 219723)
                                year = "1998";
                            else if (numb < 471932)
                                year = "1999";
                            else if (numb < 740756)
                                year = "2000";
                            else
                                year = "2001";
                            number = year + "0" + number;
                        } else if (number.startsWith("8/") || number.startsWith("08/")) {
                            // standard patent application
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            if (number.startsWith("8/"))
                                number = number.substring(2, number.length());
                            else
                                number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 176047)
                                year = "1993";
                            else if (numb < 367542)
                                year = "1994";
                            else if (numb < 581739)
                                year = "1995";
                            else if (numb < 777991)
                                year = "1996";
                            else
                                year = "1997";
                            number = year + "0" + number;
                        } else if (number.startsWith("7/") || number.startsWith("07/")) {
                            // standard patent application
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            if (number.startsWith("7/"))
                                number = number.substring(2, number.length());
                            else
                                number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 140321)
                                year = "1987";
                            else if (numb < 292671)
                                year = "1988";
                            else if (numb < 459413)
                                year = "1989";
                            else if (numb < 636609)
                                year = "1990";
                            else if (numb < 815501)
                                year = "1991";
                            else
                                year = "1992";
                            number = year + "0" + number;
                        } else if (number.startsWith("6/") || number.startsWith("06/")) {
                            // standard patent application
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            if (number.startsWith("6/"))
                                number = number.substring(2, number.length());
                            else
                                number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 108971)
                                year = "1979";
                            else if (numb < 221957)
                                year = "1980";
                            else if (numb < 336510)
                                year = "1981";
                            else if (numb < 454954)
                                year = "1982";
                            else if (numb < 567457)
                                year = "1983";
                            else if (numb < 688174)
                                year = "1984";
                            else if (numb < 815454)
                                year = "1985";
                            else
                                year = "1986";
                            number = year + "0" + number;
                        } else if (number.startsWith("5/") || number.startsWith("05/")) {
                            // standard patent application
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            if (number.startsWith("5/"))
                                number = number.substring(2, number.length());
                            else
                                number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 103000)
                                year = "1970";
                            else if (numb < 214538)
                                year = "1971";
                            else if (numb < 319971)
                                year = "1972";
                            else if (numb < 429701)
                                year = "1973";
                            else if (numb < 537821)
                                year = "1974";
                            else if (numb < 645931)
                                year = "1975";
                            else if (numb < 756051)
                                year = "1976";
                            else if (numb < 866211)
                                year = "1977";
                            else
                                year = "1978";
                            number = year + "0" + number;
                        } else if (number.startsWith("4/") || number.startsWith("04/")) {
                            // standard patent application
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            if (number.startsWith("4/"))
                                number = number.substring(2, number.length());
                            else
                                number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 80000)
                                year = "1960";
                            else if (numb < 163000)
                                year = "1961";
                            else if (numb < 248000)
                                year = "1962";
                            else if (numb < 335000)
                                year = "1963";
                            else if (numb < 423000)
                                year = "1964";
                            else if (numb < 518000)
                                year = "1965";
                            else if (numb < 606000)
                                year = "1966";
                            else if (numb < 695000)
                                year = "1967";
                            else if (numb < 788000)
                                year = "1968";
                            else
                                year = "1969";
                            number = year + "0" + number;
                        } else if (number.startsWith("3/") || number.startsWith("03/")) {
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            if (number.startsWith("3/"))
                                number = number.substring(2, number.length());
                            else
                                number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 68000)
                                year = "1948";
                            else if (numb < 136000)
                                year = "1949";
                            else if (numb < 204000)
                                year = "1950";
                            else if (numb < 264000)
                                year = "1951";
                            else if (numb < 329000)
                                year = "1952";
                            else if (numb < 401000)
                                year = "1953";
                            else if (numb < 479000)
                                year = "1954";
                            else if (numb < 557000)
                                year = "1955";
                            else if (numb < 632000)
                                year = "1956";
                            else if (numb < 706000)
                                year = "1957";
                            else if (numb < 784000)
                                year = "1958";
                            else
                                year = "1959";
                            number = year + "0" + number;
                        } else if (number.startsWith("2/") || number.startsWith("02/")) {
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            if (number.startsWith("2/"))
                                number = number.substring(2, number.length());
                            else
                                number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            if (numb < 57000)
                                year = "1935";
                            else if (numb < 119000)
                                year = "1936";
                            else if (numb < 183000)
                                year = "1937";
                            else if (numb < 249000)
                                year = "1938";
                            else if (numb < 312000)
                                year = "1939";
                            else if (numb < 372000)
                                year = "1940";
                            else if (numb < 425000)
                                year = "1941";
                            else if (numb < 471000)
                                year = "1942";
                            else if (numb < 516000)
                                year = "1943";
                            else if (numb < 570000)
                                year = "1944";
                            else if (numb < 638000)
                                year = "1945";
                            else if (numb < 719000)
                                year = "1946";
                            else
                                year = "1947";
                            number = year + "0" + number;
                        } else if (number.startsWith("1/") || number.startsWith("01/")) {
                            applications.set(i, Boolean.valueOf(true));
                            provisionals.set(i, Boolean.valueOf(false));
                            originalNumber = number;
                            if (number.startsWith("1/"))
                                number = number.substring(2, number.length());
                            else
                                number = number.substring(3, number.length());
                            number = number.replaceAll("[\\.\\s/,]", "");
                            // we check the range of the number for deciding about a year
                            int numb = -1;
                            try {
                                numb = Integer.parseInt(number);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Cannot parse extracted patent number: " + number);
                            }
                            if (numb == -1)
                                continue;
                            String year = null;
                            // years 1915-1924 are redundant with 1925-1934 because apparently they forgot
                            // incrementing the prefix...
                            /*if (numb < 70000)
                                year = "1915";
                            else if (numb < 140000)
                                year = "1916";
                            else if (numb < 210000)
                                year = "1917";
                            else if (numb < 270000)
                                year = "1918";
                            else if (numb < 349000)
                                year = "1919";
                            else if (numb < 435000)
                                year = "1920";
                            else if (numb < 526000)
                                year = "1921";
                            else if (numb < 610000)
                                year = "1922";
                            else if (numb < 684000)
                                year = "1923";
                            else if (numb < )
                                year = "1924";
                            else */
                            if (numb < 78000)
                                year = "1925";
                            else if (numb < 158000)
                                year = "1926";
                            else if (numb < 244000)
                                year = "1927";
                            else if (numb < 330000)
                                year = "1928";
                            else if (numb < 418000)
                                year = "1929";
                            else if (numb < 506000)
                                year = "1930";
                            else if (numb < 584000)
                                year = "1931";
                            else if (numb < 650000)
                                year = "1932";
                            else if (numb < 705000)
                                year = "1933";
                            else
                                year = "1934";
                            number = year + "0" + number;
                        } else if (number.startsWith("RE")) {
                            // we have a reissued patent USRE with 5 digits number normally
                            reissueds.set(i, Boolean.valueOf(true));
                            applications.set(i, Boolean.valueOf(false));
                            provisionals.set(i, Boolean.valueOf(false));
                        } else if (number.startsWith("PP")) {
                            // we have a plant patent USPP
                            plants.set(i, Boolean.valueOf(true));
                            applications.set(i, Boolean.valueOf(false));
                            provisionals.set(i, Boolean.valueOf(false));
                        } else {
                            // even if it is indicated as an application, the serial coding indicates
                            // that it is maybe not !
                            // access to OPS would be necessary to decide but heuristitics can help !
                            String numm = number.replaceAll("[ABCU,\\.\\s/\\\\]", "").trim();
                            originalNumber = numm;
                            if ((numm.length() == 10) || (numm.length() == 11)) {
                                applications.set(i, Boolean.valueOf(false));
                                provisionals.set(i, Boolean.valueOf(false));

                                //if (publi && (numm.length() == 11)) {
                                if ((!applications.get(i).booleanValue()) && (numm.length() == 11)) {
                                    if (numm.charAt(4) == '0') {
                                        number = numm.substring(0, 4) + numm.substring(5, numm.length());
                                    }
                                }
                            } else if ((number.indexOf("/") != -1) && !publi) {
                                applications.set(i, Boolean.valueOf(true));
                            }
                        }
                    } else if (country.equals("JP")) {
                        String numm = number.replaceAll("[ABCU,\\.\\s/]", "").trim();
                        originalNumber = numm;
                        if ((numm.length() == 10)) {
                            applications.set(i, Boolean.valueOf(false));
                            provisionals.set(i, Boolean.valueOf(false));
                        }
                        // first do we have a modern numbering
                        if ((numm.length() == 9) && (numm.startsWith("20") || numm.startsWith("19"))) {
                            // publication, we need to add a 0 after the 4 digit year
                            number = numm.substring(0, 4) + "0" + numm.substring(4, numm.length());
                        }
                        //else if ((numm.length() == 7)) {

                        //}
                        else if (applications.get(i)
                                && ((numm.length() == 7) || (numm.length() == 8))) {
                            // for application !
                            // emperor reign post processing
                            // we need to get the prefix in the original number
                            String prefix = "" + number.charAt(0);
                            int move = 0;
                            if ((prefix.equals("A")) || (prefix.equals("B")) || (prefix.equals("C"))) {
                                // kind code
                                kindcodes.set(i, prefix);
                                prefix = null;
                                move = 1;
                                applications.set(i, Boolean.valueOf(false));
                                // it was not an application number but a publication !
                            } else if (Character.isDigit(prefix.charAt(0))) {
                                if ((originalNumber.charAt(1) == '-') ||
                                        (originalNumber.charAt(1) == '/')) {
                                    move = 1;
                                } else if (Character.isDigit(number.charAt(1))) {
                                    prefix += number.charAt(1);
                                    move = 2;
                                } else
                                    move = 1;
                            } else {
                                if (Character.isDigit(number.charAt(1))) {
                                    prefix = "" + number.charAt(1);
                                    move = 2;
                                } else
                                    prefix = null;
                            }
                            if (prefix != null) {
                                String year = null;
                                // this is an heuristics: for small numbers (<25) we have Heisei reign
                                // for higher, we have Showa reign... this works from 1950
                                int emperorYear = Integer.parseInt(prefix);
                                if (emperorYear <= 25) {
                                    year = "" + (emperorYear + 1988);
                                } else if (emperorYear <= 63) {
                                    year = "" + (emperorYear + 1925);
                                }
                                number = year + number.substring(move, number.length());
                            }
                        }

                    } else if (country.equals("DE")) {
                        // Application numbering format up to 2003. The first digit indicates the type of
                        // application (1 for patent). The next 2 digits are the filing year. the remaining
                        // digits are the serial number
                        // ex: 195 00 002.1 -> DE19951000002

                        // Numbering format (as of 1st January 2004). First two digits indicates application
                        // type (10 for patent). The 4-digit year of filing is next, followed by a 6-digit
                        // serial number, and an optional check digit.
                        // ex: 102004005106.7 -> DE200410005106

                        // otherwise a publication

                    } else if (country.equals("GB")) {
                        if (applications.get(i).booleanValue()) {
                            String numm = number.replaceAll("[ABCU,\\.\\s/]", "").trim();
                            originalNumber = numm;
                            if (numm.length() == 7) {
                                String year = numm.substring(0, 2);
                                if ((year.charAt(0) == '7') || (year.charAt(0) == '8') || (year.charAt(0) == '9')) {
                                    year = "19" + year;
                                } else {
                                    year = "20" + year;
                                }
                                number = year + "00" + numm.substring(2, numm.length());
                            }
                        }
                    } else if (country.equals("FR")) {
                        // A 2 digit year followed by a 5-digit serial number in sequential order according to
                        // year
                        // ex: 96 03098 -> FR19960003098
                    }
                }
                newNumbers.add(number);
                if (originalNumber == null)
                    originalNumber = number;
                originalNumbers.add(originalNumber);
                i++;
            }

            numbers = newNumbers;
            i = 0;
            for (String number : numbers) {
                if (number != null) {
                    PatentItem res0 = new PatentItem();
                    res0.setAuthority(country);
                    res0.setApplication(applications.get(i).booleanValue());
                    res0.setProvisional(provisionals.get(i).booleanValue());
                    res0.setReissued(reissueds.get(i).booleanValue());
                    res0.setPlant(plants.get(i).booleanValue());
                    if (pctapps.get(i).booleanValue())
                        res0.setNumberEpoDoc(number.replaceAll("[\\.\\s/\\\\]", ""));
                    else
                        res0.setNumberEpoDoc(number.replaceAll("[ABCU,\\.\\s/\\\\]", ""));
                    if (i < originalNumbers.size()) {
                        res0.setNumberWysiwyg(originalNumbers.get(i).replaceAll("[ABCU,\\.\\s/\\\\]", ""));
                    }

                    // number completion
                    if (country.equals("EP")) {
                        if (!res0.getApplication()) {
                            while (res0.getNumberEpoDoc().length() < 7) {
                                res0.setNumberEpoDoc("0" + res0.getNumberEpoDoc());
                            }
                        }
                    }

                    res0.setKindCode(kindcodes.get(i));

                    res0.setOffsetBegin(offsets_begin.get(i).intValue());
                    res0.setOffsetEnd(offsets_end.get(i).intValue());
                    res.add(res0);
                }
                i++;
            }
        }

        return res;
    }

    private void addNumber(
            List<String> numbers,
            List<Integer> offsets_begin,
            List<Integer> offsets_end,
            String toto,
            int offset_begin,
            int offset_end,
            String sequence) {
        // we have to check if we have a check code at the end of the number
        toto = toto.trim();
        if (toto.length() > 2) {
            if ((toto.charAt(toto.length() - 2) == '.') && (Character.isDigit(toto.charAt(toto.length() - 1)))) {
                toto = toto.substring(0, toto.length() - 2);
            }
            if ((toto.length() > 2) &&
                    ((toto.charAt(toto.length() - 2) == 'A') ||
                            (toto.charAt(toto.length() - 2) == 'B') ||
                            (toto.charAt(toto.length() - 2) == 'C'))
                    &&
                    (Character.isDigit(toto.charAt(toto.length() - 1)))) {
                toto = toto.substring(0, toto.length() - 2);
            }
        }

        if ((toto.length() > 4) && toto.length() < 20) {
            if (sequence == null) {
                numbers.add(toto.trim());
                offsets_begin.add(Integer.valueOf(offset_begin));
                offsets_end.add(Integer.valueOf(offset_end));
            } else {
                // we might have an enumeration and we need to match the target number in it
                int localStart = sequence.indexOf(toto);
                int localEnd = localStart + toto.length();
                numbers.add(toto.trim());
                if (localStart != -1) {
                    offsets_begin.add(Integer.valueOf(localStart + offset_begin));
                    offsets_end.add(Integer.valueOf(localEnd + offset_begin));
                } else {
                    offsets_begin.add(Integer.valueOf(offset_begin));
                    offsets_end.add(Integer.valueOf(offset_end));
                }
            }
        }
    }

}
