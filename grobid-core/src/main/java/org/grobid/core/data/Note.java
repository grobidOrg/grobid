package org.grobid.core.data;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.*;

import java.util.List;

import static com.google.common.collect.Iterables.getLast;

public class Note {

    public enum NoteType {
        FOOT, MARGIN
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
