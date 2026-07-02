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
package org.grobid.core.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.grobid.core.data.BiblioItem;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.LayoutToken;

/**
 * Some utilities methods that I don't know where to put.
 *
 */
public class Utilities {

    /**
     * Deletes all files and subdirectories under dir. Returns true if all
     * deletions were successful. If a deletion fails, the method stops
     * attempting to delete and returns false.
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            //String[] children = dir.list();
            File[] children = dir.listFiles();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(children[i]);
                if (!success) {
                    return false;
                }
            }
        }
        // the directory is now empty so delete it
        return dir.delete();
    }

    public static String uploadFile(String urlmsg, String path, String name) {
        try {
            System.out.println("Sending: " + urlmsg);
            URL url = new URL(urlmsg);

            File outFile = new File(path, name);
            FileOutputStream out = new FileOutputStream(outFile);

            // Serve the file
            InputStream in = url.openStream();
            byte[] buf = new byte[4 * 1024]; // 4K buffer
            int bytesRead;
            while ((bytesRead = in.read(buf)) != -1) {
                out.write(buf, 0, bytesRead);
            }

            out.close();
            in.close();
            return path + name;
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

    public static String punctuationsSub = "([,;])";

    /**
     * Special cleaning for ZFN extracted data in a BiblioItem
     */
    @Deprecated
    public static BiblioItem cleanZFNMetadata(BiblioItem item) {
        // general cleaning: remove brackets, parenthesis, etc.

        // date
        if (item.getPublicationDate() != null) {
            String new_date = "";
            for (int i = 0; i < item.getPublicationDate().length(); i++) {
                char c = item.getPublicationDate().charAt(i);
                if (TextUtilities.fullPunctuations.indexOf(c) == -1)
                    new_date += c;
            }
            item.setPublicationDate(new_date.trim());
        }

        // affiliation
        String affiliation = item.getAffiliation();
        if (affiliation != null) {
            if (affiliation.startsWith("Aus dem"))
                affiliation = affiliation.replace("Aus dem", "");
            if (affiliation.startsWith("Aus der"))
                affiliation = affiliation.replace("Aus der", "");
            affiliation = affiliation.trim();
            item.setAffiliation(affiliation);
        }

        // journal
        String journal = item.getJournal();
        if (journal != null) {
            String new_journal = "";
            for (int i = 0; i < journal.length(); i++) {
                char c = journal.charAt(i);
                if (punctuationsSub.indexOf(c) == -1)
                    new_journal += c;
            }
            journal = new_journal.trim();
            journal = journal.replace(" .", ".");
            if ((journal.startsWith(",")) | (journal.startsWith("."))) {
                journal = journal.substring(1, journal.length()).trim();
            }
            item.setJournal(journal);
        }

        // page block
        String pageRange = item.getPageRange();
        if (pageRange != null) {
            String new_pageRange = "";
            for (int i = 0; i < pageRange.length(); i++) {
                char c = pageRange.charAt(i);
                if (punctuationsSub.indexOf(c) == -1)
                    new_pageRange += c;
            }
            pageRange = new_pageRange.trim();
            item.setPageRange(pageRange);
        }

        // note
        String note = item.getNote();
        if (note != null) {
            String new_note = "";
            for (int i = 0; i < note.length(); i++) {
                char c = note.charAt(i);
                if (punctuationsSub.indexOf(c) == -1)
                    new_note += c;
            }
            note = new_note.trim();
            note = note.replace(" .", ".");
            note = note.replace("...", ".");
            note = note.replace("..", ".");
            if ((note.startsWith(",")) | (note.startsWith("."))) {
                note = note.substring(1, note.length()).trim();
            }
            //note = note.replace("@BULLET", " • ");
            item.setNote(note);
        }

        // submission
        String submission = item.getSubmission();
        if (submission != null) {
            String new_submission = "";
            for (int i = 0; i < submission.length(); i++) {
                char c = submission.charAt(i);
                if (punctuationsSub.indexOf(c) == -1)
                    new_submission += c;
            }
            submission = new_submission.trim();
            submission = submission.replace(" .", ".");
            submission = submission.replace("...", ".");
            submission = submission.replace("..", ".");
            if ((submission.startsWith(",")) | (submission.startsWith("."))) {
                submission = submission.substring(1, submission.length())
                        .trim();
            }
            //submission = submission.replace("@BULLET", " • ");
            item.setSubmission(submission);
        }

        // dedication
        String dedication = item.getDedication();
        if (dedication != null) {
            String new_dedication = "";
            for (int i = 0; i < dedication.length(); i++) {
                char c = dedication.charAt(i);
                if (punctuationsSub.indexOf(c) == -1)
                    new_dedication += c;
            }
            dedication = new_dedication.trim();
            dedication = dedication.replace(" .", ".");
            dedication = dedication.replace("...", ".");
            dedication = dedication.replace("..", ".");
            if ((dedication.startsWith(",")) | (dedication.startsWith("."))) {
                dedication = dedication.substring(1, dedication.length())
                        .trim();
            }
            //dedication = dedication.replace("@BULLET", " • ");
            item.setDedication(dedication);
        }

        // title
        String title = item.getTitle();
        if (title != null) {
            if (title.endsWith("'")) {
                title = title.substring(0, title.length() - 1).trim();
            }
            //title = title.replace("@BULLET", " • ");
            item.setTitle(title);
        }

        // English title
        String english_title = item.getEnglishTitle();
        if (english_title != null) {
            if (english_title.endsWith("'")) {
                english_title = english_title.substring(
                        0,
                        english_title.length() - 1).trim();
            }
            //english_title = english_title.replace("@BULLET", " • ");
            item.setEnglishTitle(english_title);
        }

        // abstract
        String abstract_ = item.getAbstract();
        if (abstract_ != null) {
            if (abstract_.startsWith(") ")) {
                abstract_ = abstract_.substring(1, abstract_.length()).trim();
            }
            //abstract_ = abstract_.replace("@BULLET", " • ");
            item.setAbstract(abstract_);
        }

        // address
        String address = item.getAddress();
        if (address != null) {
            address.replace("\t", " ");
            address = address.trim();
            if ((address.startsWith(",")) | (address.startsWith("("))) {
                address = address.substring(1, address.length()).trim();
            }
            if (address.endsWith(")")) {
                address = address.substring(0, address.length() - 1).trim();
            }
            item.setAddress(address);
        }

        // email
        String email = item.getEmail();
        if (email != null) {
            if (email.startsWith("E-mail :")) {
                email = email.replace("E-mail :", "").trim();
                item.setEmail(email);
            }
        }

        // authors
        String authors = item.getAuthors();
        if (authors != null) {
            authors = authors.replace("0. ", "O. ");
            item.setAuthors(authors);
        }

        // keywords
        String keyword = item.getKeyword();
        if (keyword != null) {
            if (keyword.startsWith(":")) {
                keyword = keyword.substring(1, keyword.length()).trim();
                item.setKeyword(keyword);
            }
        }

        return item;
    }

    /**
     * Return the name of directory to use given the os and the architecture.<br>
     * Possibles returned values should match one of the following:<br>
     * win-32<br>
     * lin-32<br>
     * lin-64<br>
     * mac-64<br>
     *
     * @return name of the directory corresponding to the os name and
     *         architecture.
     */
    public static String getOsNameAndArch() {
        String osPart = System.getProperty("os.name")
                .replace(" ", "")
                .toLowerCase()
                .substring(0, 3);
        if (StringUtils.equals(osPart, "mac") || StringUtils.equals(osPart, "lin")) {
            if (StringUtils.equals(System.getProperty("os.arch"), "aarch64")) {
                osPart = osPart + "_arm";
            }
        }
        String archPart = System.getProperty("sun.arch.data.model");
        return String.format("%s-%s", osPart, archPart);
    }

    /**
     * Convert a string to boolean.
     *
     * @param value
     *            the value to convert
     * @return true if the string value is "true", false is it equals to
     *         "false". <br>
     *         If the value does not correspond to one of these 2 values, return
     *         false.
     */
    public static boolean stringToBoolean(String value) {
        boolean res = false;
        if (StringUtils.isNotBlank(value)
                && Boolean.toString(true).equalsIgnoreCase(value.trim())) {
            res = true;
        }
        return res;
    }

    /**
     * Call a java method using the method name given in string.
     *
     * @param obj
     *            Class in which the method is.
     * @param args
     *            the arguments of the method.
     * @param methodName
     *            the name of the method.
     * @return result of the called method.
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static Object launchMethod(
            Object obj,
            Object[] args,
            String methodName) throws Exception {
        Class[] paramTypes = null;
        if (args != null) {
            paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; ++i) {
                paramTypes[i] = args[i].getClass();
            }
        }

        if (StringUtils.isEmpty(methodName)) {
            throw new GrobidException(
                    "Missing method in command line. To specify with -exe [methodName]. " + methodName);
        }
        return getMethod(obj, paramTypes, methodName).invoke(obj, args);
    }

    /**
     * Call a java method using the method name given in string.
     *
     * @param obj
     *            Class in which the method is.
     * @param args
     *            the arguments of the method.
     * @param paramTypes
     *            types of the arguments.
     * @param methodName
     *            the name of the method.
     * @return result of the called method.
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static Object launchMethod(
            Object obj,
            Object[] args,
            Class[] paramTypes,
            String methodName) throws Exception {
        return getMethod(obj, paramTypes, methodName).invoke(obj, args);
    }

    /**
     * Get the method given in string in input corresponding to the given
     * arguments.
     *
     * @param obj
     *            Class in which the method is.
     * @param paramTypes
     *            types of the arguments.
     * @param methodName
     *            the name of the method.
     * @return Methood
     *
     * @throws NoSuchMethodException
     */
    @SuppressWarnings("rawtypes")
    public static Method getMethod(
            Object obj,
            Class[] paramTypes,
            String methodName) throws NoSuchMethodException {
        Method method = obj.getClass().getMethod(methodName, paramTypes);
        return method;
    }

    /**
     * Format a date in string using pFormat.
     *
     * @param pDate
     *            the date to parse.
     * @param pFormat
     *            the format to use following SimpleDateFormat patterns.
     *
     * @return the formatted date.
     */
    public static String dateToString(Date pDate, String pFormat) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pFormat);
        return dateFormat.format(pDate);
    }

    public static boolean doubleEquals(double d1, double d2) {
        return Math.abs(d1 - d2) <= Double.MIN_VALUE;
    }

    public static boolean doubleEquals(double d1, double d2, double epsilon) {
        return Math.abs(d1 - d2) <= epsilon;
    }

    /**
     * Merge the offset positions of two lists, merging overlapping positions
     * into a spanning one.
     *
     * @param positions1
     *            the first offset position list to be merged
     * @param positions2
     *            the second offset position list to be merged
     *
     * @return the merged list of (merged) offset positions
     */
    public static List<OffsetPosition> mergePositions(
            List<OffsetPosition> positions1,
            List<OffsetPosition> positions2) {
        if (CollectionUtils.isEmpty(positions1))
            return positions2;
        if (CollectionUtils.isEmpty(positions2))
            return positions1;

        Collections.sort(positions1);
        Collections.sort(positions2);

        List<OffsetPosition> result = new ArrayList<OffsetPosition>();
        for (OffsetPosition pos : positions1) {
            result.add(pos);
        }
        for (OffsetPosition pos : positions2) {
            if (!result.contains(pos))
                result.add(pos);
        }
        Collections.sort(result);
        List<OffsetPosition> finalResult = new ArrayList<OffsetPosition>();
        OffsetPosition prevPos = null;
        for (OffsetPosition pos : result) {
            if (prevPos == null) {
                finalResult.add(pos);
                prevPos = pos;
            } else {
                if ((pos.start >= prevPos.start) && (pos.end <= prevPos.end)) {
                    // nothing to do
                } else if (prevPos.end >= pos.start) {
                    prevPos.end = pos.end;
                } else {
                    prevPos = pos;
                    finalResult.add(pos);
                }
            }
        }

        return finalResult;
    }

    /**
     * This version uses general LayoutToken offsets relative to the complete document.
     * It supposes that the stringPosition have been identified on the complete document string
     */
    public static List<OffsetPosition> convertStringOffsetToTokenOffsetOld(
            List<OffsetPosition> stringPosition,
            List<LayoutToken> tokens) {
        List<OffsetPosition> result = new ArrayList<OffsetPosition>();
        int indexToken = 0;
        OffsetPosition currentPosition = null;
        LayoutToken token = null;
        for (OffsetPosition pos : stringPosition) {
            while (indexToken < tokens.size()) {
                token = tokens.get(indexToken);
                if (token.getOffset() >= pos.start) {
                    // we have a start
                    currentPosition = new OffsetPosition(indexToken, indexToken);
                    // we need an end
                    boolean found = false;
                    while (indexToken < tokens.size()) {
                        token = tokens.get(indexToken);
                        if (token.getOffset() + token.getText().length() >= pos.end) {
                            // we have an end
                            currentPosition.end = indexToken;
                            result.add(currentPosition);
                            found = true;
                            break;
                        }
                        indexToken++;
                    }
                    if (found) {
                        indexToken++;
                        break;
                    } else {
                        currentPosition.end = indexToken - 1;
                        result.add(currentPosition);
                    }
                }
                indexToken++;
            }
        }
        return result;
    }

    /**
     * This version uses actual LayoutToken offsets relative to the tokens present in argument only.
     * It supposes that the stringPosition have been identified on the provided tokens only, and not
     * restricted to the complete document.
     */
    public static List<OffsetPosition> convertStringOffsetToTokenOffset(
            List<OffsetPosition> stringPosition,
            List<LayoutToken> tokens) {
        List<OffsetPosition> result = new ArrayList<OffsetPosition>();
        int indexText = 0;
        int indexToken = 0;
        OffsetPosition currentPosition = null;
        LayoutToken token = null;
        for (OffsetPosition pos : stringPosition) {
            while (indexToken < tokens.size()) {

                token = tokens.get(indexToken);
                if (token.getText() == null) {
                    indexToken++;
                    continue;
                }

                if (indexText >= pos.start) {
                    // we have a start
                    currentPosition = new OffsetPosition(indexToken, indexToken);
                    // we need an end
                    boolean found = false;
                    while (indexToken < tokens.size()) {
                        token = tokens.get(indexToken);

                        if (token.getText() == null) {
                            indexToken++;
                            continue;
                        }

                        if (indexText + token.getText().length() >= pos.end) {
                            // we have an end
                            currentPosition.end = indexToken;
                            result.add(currentPosition);
                            found = true;
                            break;
                        }
                        indexToken++;
                        indexText += token.getText().length();
                    }
                    if (found) {
                        indexToken++;
                        indexText += token.getText().length();
                        break;
                    } else {
                        currentPosition.end = indexToken - 1;
                        result.add(currentPosition);
                    }
                }
                indexToken++;
                indexText += token.getText().length();
            }
        }
        return result;
    }

}
