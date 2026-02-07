package org.grobid.core.data.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class EmailSanitizer {
    private static final Pattern DASHES_PATTERN = Pattern.compile("(%E2%80%90|%e2%80%90)");

    private static final Set<String> BLACKLISTED_EMAIL_WORDS = Sets.newHashSet(
            "firstname",
            "lastname",
            "publication",
            "theses",
            "thesis",
            "editor",
            "press",
            "contact",
            "info",
            "feedback",
            "journal",
            "please",
            "pubs",
            "iza@iza",
            "admin",
            "help",
            "subs",
            "news",
            "archives",
            "order",
            "postmaster@",
            "informa",
            "reprint",
            "comunicacion@",
            "revista",
            "digitalcommons",
            "group@",
            "root@",
            "deposit@",
            "studies",
            "permiss",
            "print",
            "paper",
            "report",
            "support",
            "pedocs",
            "investigaciones@",
            "medicin",
            "copyright",
            "rights",
            "sales@",
            "pacific@",
            "redaktion",
            "publicidad",
            "surface@",
            "comstat@",
            "service@",
            "omnia@",
            "letter",
            "scholar",
            "staff",
            "delivery",
            "epubs",
            "office",
            "technolog",
            "compute",
            "elsevier");

    private static final Pattern[] EMAIL_STRIP_PATTERNS = new Pattern[]{
            Pattern.compile("^(e\\-mail|email|e\\smail|mail):"),
            Pattern.compile("[\\r\\n\\t ]"), // newlines, tabs and spaces
            Pattern.compile("\\(.*\\)$"),
    };

    private static final Pattern[] AT_SYMBOL_REPLACEMENTS = new Pattern[]{
            Pattern.compile("&#64;"),
            Pattern.compile("@\\."),
            Pattern.compile("\\.@"),
    };

    private static final Pattern EMAIL_SPLITTER_PATTERN = Pattern.compile("(\\sor\\s|,|;|/)");

    private static final Pattern AT_SPLITTER = Pattern.compile("@");

    /**
     * @param addresses email addresses
     * @return cleaned addresses
     */
    public List<String> splitAndClean(List<String> addresses) {
        if (addresses == null) {
            return null;
        }

        List<String> result = new ArrayList<String>();
        Set<String> emails = new HashSet<String>();
        for (String emailAddress : addresses) {

            emailAddress = initialReplace(emailAddress);

            //            StringTokenizer st = new StringTokenizer(emailAddress, ", ");
            //            List<String> emails = new ArrayList<String>();
            //            while (st.hasMoreTokens()) {
            //                String token = st.nextToken();
            //                if (token.length() > 2) {
            //                    emails.add(token);
            //                }
            //            }
            //
            //            int i = 0;
            //            for (String token : emails) {
            //                if (!token.contains("@")) {
            //                    // the domain information is missing, we are taking the first one of the next tokens
            //                    String newToken = null;
            //                    int j = 0;
            //                    for (String token2 : emails) {
            //                        if (j <= i) {
            //                            j++;
            //                        } else {
            //                            int ind = token2.indexOf("@");
            //                            if (ind != -1) {
            //                                newToken = token + token2.substring(ind, token2.length());
            //                                break;
            //                            }
            //                            j++;
            //                        }
            //                    }
            //                    if (newToken != null) {
            //                        emails.set(i, newToken);
            //                    }
            //                }
            //                i++;
            //            }
            //

            List<String> splitEmails = Lists.newArrayList(
                    Splitter.on(EMAIL_SPLITTER_PATTERN)
                            .omitEmptyStrings()
                            .split(emailAddress.toLowerCase())
                            .iterator());

            if (splitEmails.size() > 1) {
                // Some emails are of the form jiglesia,cmt@ll.iac.es or jiglesia;cmt@ll.iac.es or bono/caputo/vittorio@mporzio.astro.it
                List<String> atSeparatedStrings = Lists.newArrayList(
                        Splitter.on(AT_SPLITTER)
                                .omitEmptyStrings()
                                .split(emailAddress.toLowerCase())
                                .iterator());
                if (atSeparatedStrings.size() == 2) {
                    // Only the last email address has a domain, so append it to the rest of the split emails
                    int atIndex = splitEmails.get(splitEmails.size() - 1).indexOf('@');
                    String domain = splitEmails.get(splitEmails.size() - 1).substring(atIndex + 1);
                    for (int i = 0; i < splitEmails.size() - 1; i++) {
                        splitEmails.set(i, splitEmails.get(i) + "@" + domain);
                    }
                }
            }

            for (String splitEmail : splitEmails) {
                String email;
                try {
                    email = cleanEmail(splitEmail);
                } catch (Exception e) {
                    // Cleaning failed so its probably an invalid email so don't keep it
                    continue;
                }

                if (email != null && !email.isEmpty()) {

                    // Check for duplicate emails
                    if (emails.contains(email)) {
                        continue;
                    }

                    email = postValidateAddress(email);

                    if (email == null) {
                        continue;
                    }

                    emails.add(email);
                    result.add(email);
                }
            }
        }

        if (result.isEmpty()) {
            return null;
        }

        return result;
    }

    private String initialReplace(String email) {
        email = email.replace("{", "");
        email = email.replace("}", "");
        email = email.replace("(", "");
        email = email.replace(")", "").trim();
        // Regex patterns may intentionally match OCR typos in source text
        email = email.replaceAll("(E|e)lectronic(\\s)(A|a)ddress(\\:)?", "");  // codespell:ignore ddress
        email = email.replaceAll("^(e|E)?(\\-)?mail(\\:)?(\\s)(A|a)ddress(\\:)?", "");  // codespell:ignore ddress
        email = email.replaceAll("^(e|E)?(\\-)?mail(\\:)?(\\s)?", "");
        // case: Peter Pan -peter.pan@email.org with asterisks and spaces
        email = email.replaceAll("^[A-Z][a-z]+\\s+[A-Z][a-z]+(\\*)?(\\s)*-(\\s)*", "");
        return email;

    }

    private static String postValidateAddress(String emStr) {
        String orig = emStr;
        for (String b : BLACKLISTED_EMAIL_WORDS) {
            if (orig.contains(b)) {
                return null;
            }
        }

        for (Pattern p : EMAIL_STRIP_PATTERNS) {
            Matcher matcher = p.matcher(orig);
            orig = matcher.replaceAll("");
        }

        if (!orig.contains("@")) {
            return null;
        }

        return orig;
    }

    private static String cleanEmail(String email) throws UnsupportedEncodingException {
        if (email == null) {
            return null;
        }

        // Fix any incorrect dashes
        Matcher dashes = DASHES_PATTERN.matcher(email);
        email = dashes.replaceAll("-");

        // Some emails may contain HTML encoded characters, so decode just in case
        email = URLDecoder.decode(email, "UTF-8");

        email = email.toLowerCase().trim();

        for (Pattern p : EMAIL_STRIP_PATTERNS) {
            Matcher matcher = p.matcher(email);
            email = matcher.replaceAll("");
        }

        for (Pattern r : AT_SYMBOL_REPLACEMENTS) {
            Matcher matcher = r.matcher(email);
            email = matcher.replaceAll("@");
        }
        return email;
    }

}
