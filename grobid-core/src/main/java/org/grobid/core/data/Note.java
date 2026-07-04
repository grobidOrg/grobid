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

import static com.google.common.collect.Iterables.getLast;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.*;

public class Note {

    public enum NoteType {
        FOOT,
        MARGIN
    };

    private String identifier;

    private String label;

    private List<LayoutToken> tokens;

    private String text;

    private int offsetStartInPage;

    private boolean ignored = false;

    private NoteType noteType;

    public Note() {
        this.identifier = KeyGen.getKey().substring(0, 7);
    }

    public Note(String label, List<LayoutToken> tokens, String text, NoteType noteType) {
        this.identifier = KeyGen.getKey().substring(0, 7);
        this.label = label;
        this.tokens = tokens;
        this.text = text;
        this.noteType = noteType;
    }

    public Note(String label, List<LayoutToken> tokens, String text, int offsetStartInPage, NoteType noteType) {
        this.identifier = KeyGen.getKey().substring(0, 7);
        this.label = label;
        this.tokens = tokens;
        this.text = text;
        this.offsetStartInPage = offsetStartInPage;
        this.noteType = noteType;
    }

    public Note(String label, List<LayoutToken> tokens, NoteType noteType) {
        this.identifier = KeyGen.getKey().substring(0, 7);
        this.label = label;
        this.tokens = tokens;
        this.noteType = noteType;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public int getOffsetStartInPage() {
        return offsetStartInPage;
    }

    public void setOffsetStartInPage(int offsetStartInPage) {
        this.offsetStartInPage = offsetStartInPage;
    }

    public int getPageNumber() {
        if (CollectionUtils.isNotEmpty(tokens))
            return tokens.get(0).getPage();
        else {
            return -1;
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<LayoutToken> getTokens() {
        return tokens;
    }

    public void setTokens(List<LayoutToken> tokens) {
        this.tokens = tokens;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getOffsetEndInPage() {
        return getLast(tokens).getOffset();
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    public NoteType getNoteType() {
        return this.noteType;
    }

    public void setNoteType(NoteType noteType) {
        this.noteType = noteType;
    }

    public String getNoteTypeName() {
        return StringUtils.lowerCase(noteType.name());
    }
}
