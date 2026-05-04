package org.grobid.core.engines;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.grobid.core.GrobidModel;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.Date;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorDate;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.TextUtilities;

public class DateParser extends AbstractParser {

    private static final Pattern NEWLINE_REGEX_PATTERN = Pattern.compile("[ \n]");

    public DateParser() {
        super(GrobidModels.DATE);
    }

    DateParser(GrobidModel model) {
        super(model);
    }

    /**
     * Deprecated, @Use process(String input)
     **/
    @Deprecated
    public List<Date> processing(String input) {
        return process(input);
    }

    public List<Date> process(String input) {
        List<String> dateBlocks = new ArrayList<>();
        // force English language for the tokenization only
        List<String> tokenizations = analyzer.tokenize(input, new Language("en", 1.0));
        if (CollectionUtils.isEmpty(tokenizations)) {
            return null;
        }

        for (String tok : tokenizations) {
            if (!" ".equals(tok) && !"\n".equals(tok)) {
                // para final sanitisation
                tok = NEWLINE_REGEX_PATTERN.matcher(tok).replaceAll("");
                dateBlocks.add(tok + " <date>");
            }
        }

        return processCommon(dateBlocks);
    }

    public List<Date> process(List<LayoutToken> input) {
        List<String> dateBlocks = new ArrayList<>();
        for (LayoutToken tok : input) {
            if (!" ".equals(tok.getText()) && !"\n".equals(tok.getText())) {
                // para final sanitisation
                String normalizedText = tok.getText().replaceAll("[ \n]", "");
                dateBlocks.add(normalizedText + " <date>");
            }
        }

        return processCommon(dateBlocks);
    }

    protected List<Date> processCommon(List<String> input) {
        if (CollectionUtils.isEmpty(input))
            return null;

        try {
            String features = FeaturesVectorDate.addFeaturesDate(input);
            String res = label(features);

            List<LayoutToken> tokenization = input.stream()
                    .map(token -> new LayoutToken(token.split(" ")[0]))
                    .collect(Collectors.toList());

            // extract results from the processed file
            return resultExtraction(res, tokenization);
        } catch (Exception e) {
            throw new GrobidException("An exception on " + this.getClass().getName() + " occurred while running Grobid.",
                    e);
        }
    }

    public List<Date> resultExtraction(String result, List<LayoutToken> tokenizations) {

        List<Date> dates = new ArrayList<>();
        Date date = new Date();
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.DATE, result, tokenizations);

        List<TaggingTokenCluster> clusters = clusteror.cluster();

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }
            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);

            String clusterText = LayoutTokensUtil.toText(cluster.concatTokens());
            if (clusterLabel.equals(TaggingLabels.DATE_YEAR)) {
                if (isNotBlank(date.getYearString())) {
                    if (date.isNotNull()) {
                        Date normalizedDate = normalizeAndClean(date);
                        dates.add(normalizedDate);
                        date = new Date();
                    }
                    date.setYearString(clusterText);

                } else {
                    date.setYearString(clusterText);
                }
            } else if (clusterLabel.equals(TaggingLabels.DATE_DAY)) {
                if (isNotBlank(date.getDayString())) {
                    if (date.isNotNull()) {
                        Date normalizedDate = normalizeAndClean(date);
                        dates.add(normalizedDate);
                        date = new Date();
                    }
                    date.setDayString(clusterText);
                } else {
                    date.setDayString(clusterText);
                }

            } else if (clusterLabel.equals(TaggingLabels.DATE_MONTH)) {
                if (isNotBlank(date.getMonthString())) {
                    if (date.isNotNull()) {
                        Date normalizedDate = normalizeAndClean(date);
                        dates.add(normalizedDate);
                        date = new Date();
                    }
                    date.setMonthString(clusterText);
                } else {
                    date.setMonthString(clusterText);
                }
            }
        }

        if (date.isNotNull()) {
            Date normalizedDate = normalizeAndClean(date);
            dates.add(normalizedDate);
        }
        return dates;
    }

    public static final Pattern jan = Pattern.compile(
            "([Jj]an$|[Jj]anuary$|[Jj]anvier$|[Jj]annewaori$|[Jj]anuar$|[Ee]nero$|[Jj]anuaro$|[Jj]anuari$|[Jj]aneiro$|[Gg]ennaio$|[Gg]en$|[Oo]cak$|[Jj]a$|(^1$)|(^01$)|(1月))");
    public static final Pattern feb = Pattern.compile(
            "([Ff]eb$|[Ff]ebruary$|[Ff][eé]vrier$|[Ff]ebruar$|[Ff]ebrewaori$|[Ff]ebrero$|[Ff]evereiro$|[Ff]ebbraio$|[Ll]uty$|[Ss]tyczeń$|Ş$|ubat$|[Ff]e$|^2$|^02$|2月)");
    public static final Pattern mar = Pattern.compile(
            "([Mm]ar$|[Mm]arch$|[Mm]ars$|[Mm]eert$|[Mm]ärz$|[Mm]arzo$|[Mm]arço$|[Mm]art$|[Mm]a$|[Mm]a$|^3$|^03$|3月)");
    public static final Pattern apr = Pattern.compile(
            "([Aa]pr$|[Aa]br$|[Aa]vr$|[Aa]pril$|[Aa]vril$|[Aa]pril$|[Aa]prile$|[Aa]bril$|[Nn]isan$|[Aa]p$|^4$|^04$|4月)");
    public static final Pattern may = Pattern
            .compile("([Mm]ay$|[Mm]ai$|[Mm]ay$|[Mm]ayıs$|[Mm]ei$|[Mm]aio$|[Mm]aggio$|[Mm]eie$|[Mm]a$|^5$|^05$|5月)");
    public static final Pattern jun = Pattern
            .compile("([Jj]un$|[Jj]une$|[Jj]uin$|[Jj]uni$|[Jj]unho$|[Gg]iugno$|[Hh]aziran$|^6$|^06$|6月)");
    public static final Pattern jul = Pattern
            .compile("([Jj]ul$|[Jj]uly$|[Jj]uillet$|[Jj]uli$|[Tt]emmuz$|[Ll]uglio$|[Jj]ulho$|^7$|^07$|7月)");
    public static final Pattern aug = Pattern
            .compile("([Aa]ug$|[Aa]ugust$|[Aa]o[uû]t$|[Aa]ugust$|[Aa]gosto$|[Aa]ugustus$|[Aa]ğustos$|^8$|^08$|8月)");
    public static final Pattern sep = Pattern.compile(
            "([Ss]ep$|[Ss]ept$|[Ss]eptember$|[Ss]eptembre$|[Ss]eptember$|[Ss]ettembre$|[Ss]etembro$|[Ee]ylül$|^9$|^09$|9月)");
    public static final Pattern oct = Pattern.compile(
            "([Oo]ct$|[Oo]cto$|[Oo]ctober$|[Oo]ctobre$|[Ee]kim$|[Oo]ktober$|[Oo]ttobre$|[Oo]utubro$|^10$|10月)");
    public static final Pattern nov = Pattern
            .compile("([Nn]ov$|[Nn]ovember$|[Nn]ovembre$|[Kk]asım$|[Nn]oviembre$|[Nn]ovembro$|^11$|11月)");
    public static final Pattern dec = Pattern
            .compile("([Dd]ec$|[Dd]ecember$|[Dd][eé]cembre$|[Dd]iciembre$|[Aa]ralık$|^12$|12月)");

    public static final Pattern[] months = {jan, feb, mar, apr, may, jun, jul, aug, sep, oct, nov, dec};

    public Date normalizeAndClean(Date date) {
        return cleaning(normalize(date));
    }

    public Date normalize(Date date) {
        Date normalizedDate = new Date();

        // normalize day
        if (isNotBlank(date.getDayString())) {
            StringBuilder dayStringBis = new StringBuilder();
            String dayString = date.getDayString().trim();
            normalizedDate.setDayString(dayString);
            for (int n = 0; n < dayString.length(); n++) {
                char c = dayString.charAt(n);
                if (Character.isDigit(c)) {
                    dayStringBis.append(c);
                }
            }
            try {
                int day = Integer.parseInt(dayStringBis.toString());
                normalizedDate.setDay(day);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

        //normalize month
        if (isNotBlank(date.getMonthString())) {
            String month = date.getMonthString().trim();
            normalizedDate.setMonthString(month);
            int n = 0;
            while (n < 12) {
                Matcher ma = months[n].matcher(month);
                if (ma.find()) {
                    normalizedDate.setMonth(n + 1);
                    break;
                }
                n++;
            }
        }

        if (StringUtils.isNotBlank(date.getYearString())) {
            StringBuilder yearStringBis = new StringBuilder();
            String yearString = date.getYearString().trim();
            normalizedDate.setYearString(yearString);
            for (int n = 0; n < yearString.length(); n++) {
                char c = yearString.charAt(n);
                if (Character.isDigit(c)) {
                    yearStringBis.append(c);
                }
            }
            try {
                int year = Integer.parseInt(yearStringBis.toString());
                if ((year >= 20) && (year < 100)) {
                    year = year + 1900;
                } else if ((year >= 0) && (year < 20)) {
                    year = year + 2000;
                }
                normalizedDate.setYear(year);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

        // if we don't have day and month, but a year with 8 digits, we might have a YYYYMMDD pattern
        int maxYear = Calendar.getInstance().getWeekYear() + 4;
        if (date.getDay() == -1 && date.getMonth() == -1 && date.getYear() != -1 && date.getYear() > 19000000
                && date.getYear() < maxYear * 10000 + 1231) {
            int yearPart = date.getYear() / 10000;
            if (yearPart > 1900 && yearPart < maxYear) {
                String yearString = "" + date.getYear();
                String theMonthString = yearString.substring(4, 6);
                String theDayString = yearString.substring(6, 8);

                int dayPart = -1;
                try {
                    dayPart = Integer.parseInt(theDayString);
                } catch (Exception e) {
                    //e.printStackTrace();
                }

                int monthPart = -1;
                try {
                    monthPart = Integer.parseInt(theMonthString);
                } catch (Exception e) {
                    //e.printStackTrace();
                }

                if (dayPart != -1 && monthPart != -1) {
                    if (dayPart > 0 && dayPart < 32 && monthPart > 0 && monthPart < 13) {
                        normalizedDate.setDay(dayPart);
                        normalizedDate.setDayString(theDayString);
                        normalizedDate.setMonth(monthPart);
                        normalizedDate.setMonthString(theMonthString);
                        normalizedDate.setYear(yearPart);
                    }
                }
            }
        }

        return normalizedDate;
    }

    /**
     * Simple and loose date validation, checking:
     *  - the year has not more than 4 digits
     *  - the month and day has not more than 2 digits
     *
     *  Assuming that incomplete dates of any form and nature can pass by here, only the information that are "out of bounds" will be reverted.
     *
     * @return the date where invalid information are removed or reverted
     */
    public static Date cleaning(Date originalDate) {
        Date validatedDate = new Date();

        if (originalDate.getDay() > -1) {
            if (String.valueOf(originalDate.getDay()).length() < 3) {
                validatedDate.setDay(originalDate.getDay());
                validatedDate.setDayString(originalDate.getDayString());
            }
        }

        if (originalDate.getMonth() > -1) {
            if (String.valueOf(originalDate.getMonth()).length() < 3) {
                validatedDate.setMonth(originalDate.getMonth());
                validatedDate.setMonthString(originalDate.getMonthString());
            }
        }

        if (originalDate.getYear() > -1) {
            if (String.valueOf(originalDate.getYear()).length() < 5) {
                validatedDate.setYear(originalDate.getYear());
                validatedDate.setYearString(originalDate.getYearString());
            }
        }

        return validatedDate;
    }

    /**
     * Extract results from a date string in the training format without any string modification.
     */
    public StringBuilder trainingExtraction(List<String> inputs) {
        StringBuilder buffer = new StringBuilder();
        try {
            if (inputs == null)
                return null;

            if (inputs.size() == 0)
                return null;

            List<String> tokenizations = null;
            List<String> dateBlocks = new ArrayList<String>();
            for (String input : inputs) {
                if (input == null)
                    continue;

                //StringTokenizer st = new StringTokenizer(input, " \t\n"+TextUtilities.fullPunctuations, true);
                //StringTokenizer st = new StringTokenizer(input, "([" + TextUtilities.punctuations, true);
                tokenizations = analyzer.tokenize(input);

                //if (st.countTokens() == 0)
                if (tokenizations.size() == 0)
                    return null;
                //while (st.hasMoreTokens()) {
                //    String tok = st.nextToken();
                for (String tok : tokenizations) {
                    if (tok.equals("\n")) {
                        dateBlocks.add("@newline");
                    } else if (!tok.equals(" ")) {
                        dateBlocks.add(tok + " <date>");
                    }
                    //tokenizations.add(tok);
                }
                dateBlocks.add("\n");
            }

            String headerDate = FeaturesVectorDate.addFeaturesDate(dateBlocks);
            String res = label(headerDate);

            // extract results from the processed file

            //System.out.print(res.toString());
            StringTokenizer st2 = new StringTokenizer(res, "\n");
            String lastTag = null;
            boolean tagClosed = false;
            int q = 0;
            boolean addSpace;
            boolean hasYear = false;
            boolean hasMonth = false;
            boolean hasDay = false;
            String lastTag0;
            String currentTag0;
            boolean start = true;
            while (st2.hasMoreTokens()) {
                String line = st2.nextToken();
                addSpace = false;
                if ((line.trim().length() == 0)) {
                    // new date
                    buffer.append("</date>\n");
                    hasYear = false;
                    hasMonth = false;
                    hasDay = false;
                    buffer.append("\t<date>");
                    continue;
                } else {
                    String theTok = tokenizations.get(q);
                    while (theTok.equals(" ")) {
                        addSpace = true;
                        q++;
                        theTok = tokenizations.get(q);
                    }
                    q++;
                }

                StringTokenizer st3 = new StringTokenizer(line, "\t");
                int ll = st3.countTokens();
                int i = 0;
                String s1 = null;
                String s2 = null;
                //String s3 = null;
                //List<String> localFeatures = new ArrayList<String>();
                while (st3.hasMoreTokens()) {
                    String s = st3.nextToken().trim();
                    if (i == 0) {
                        s2 = TextUtilities.HTMLEncode(s); // string
                    } /*else if (i == ll - 2) {
                        s3 = s; // pre-label, in this case it should always be <date>
                      } */
                    else if (i == ll - 1) {
                        s1 = s; // label
                    }
                    i++;
                }

                if (start && (s1 != null)) {
                    buffer.append("\t<date>");
                    start = false;
                }

                lastTag0 = null;
                if (lastTag != null) {
                    if (lastTag.startsWith("I-")) {
                        lastTag0 = lastTag.substring(2, lastTag.length());
                    } else {
                        lastTag0 = lastTag;
                    }
                }
                currentTag0 = null;
                if (s1 != null) {
                    if (s1.startsWith("I-")) {
                        currentTag0 = s1.substring(2, s1.length());
                    } else {
                        currentTag0 = s1;
                    }
                }

                tagClosed = lastTag0 != null && testClosingTag(buffer, currentTag0, lastTag0);

                /*if (newLine) {
                        if (tagClosed) {
                            buffer.append("\t\t\t\t\t\t\t<lb/>\n");
                        }
                        else {
                            buffer.append("<lb/>");
                        }

                    }*/

                String output = writeField(s1, lastTag0, s2, "<day>", "<day>", addSpace, 0);
                if (output != null) {
                    if (lastTag0 != null) {
                        if (hasDay && !lastTag0.equals("<day>")) {
                            buffer.append("</date>\n");
                            hasYear = false;
                            hasMonth = false;
                            buffer.append("\t<date>");
                        }
                    }
                    hasDay = true;
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                } else {
                    output = writeField(s1, lastTag0, s2, "<other>", "<other>", addSpace, 0);
                }
                if (output == null) {
                    output = writeField(s1, lastTag0, s2, "<month>", "<month>", addSpace, 0);
                } else {
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                }
                if (output == null) {
                    output = writeField(s1, lastTag0, s2, "<year>", "<year>", addSpace, 0);
                } else {
                    if (lastTag0 != null) {
                        if (hasMonth && !lastTag0.equals("<month>")) {
                            buffer.append("</date>\n");
                            hasYear = false;
                            hasDay = false;
                            buffer.append("\t<date>");
                        }
                    }
                    buffer.append(output);
                    hasMonth = true;
                    lastTag = s1;
                    continue;
                }
                if (output != null) {
                    if (lastTag0 != null) {
                        if (hasYear && !lastTag0.equals("<year>")) {
                            buffer.append("</date>\n");
                            hasDay = false;
                            hasMonth = false;
                            buffer.append("\t<date>");
                        }
                    }
                    buffer.append(output);
                    hasYear = true;
                    lastTag = s1;
                    continue;
                }
                lastTag = s1;
            }

            if (lastTag != null) {
                if (lastTag.startsWith("I-")) {
                    lastTag0 = lastTag.substring(2, lastTag.length());
                } else {
                    lastTag0 = lastTag;
                }
                currentTag0 = "";
                testClosingTag(buffer, currentTag0, lastTag0);
                buffer.append("</date>\n");
            }
        } catch (Exception e) {
            //			e.printStackTrace();
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return buffer;
    }

    private String writeField(
            String s1,
            String lastTag0,
            String s2,
            String field,
            String outField,
            boolean addSpace,
            int nbIndent) {
        String result = null;
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            if ((s1.equals("<other>") || s1.equals("I-<other>"))) {
                if (addSpace)
                    result = " " + s2;
                else
                    result = s2;
            } else if (s1.equals(lastTag0) || s1.equals("I-" + lastTag0)) {
                if (addSpace)
                    result = " " + s2;
                else
                    result = s2;
            } else {
                result = "";
                for (int i = 0; i < nbIndent; i++) {
                    result += "\t";
                }
                if (addSpace)
                    result += " " + outField + s2;
                else
                    result += outField + s2;
            }
        }
        return result;
    }

    private boolean testClosingTag(
            StringBuilder buffer,
            String currentTag0,
            String lastTag0) {
        boolean res = false;
        if (!currentTag0.equals(lastTag0)) {
            res = true;
            // we close the current tag
            if (lastTag0.equals("<other>")) {
                buffer.append("");
            } else if (lastTag0.equals("<day>")) {
                buffer.append("</day>");
            } else if (lastTag0.equals("<month>")) {
                buffer.append("</month>");
            } else if (lastTag0.equals("<year>")) {
                buffer.append("</year>");
            } else {
                res = false;
            }

        }
        return res;
    }

}
