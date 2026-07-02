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
package org.grobid.core.main.batch;

import org.grobid.core.GrobidModels;

/**
 * Class containing args of the batch {@link GrobidMain}.
 *
 */
public class GrobidMainArgs {

    private String path2grobidHome;

    private String path2grobidProperty;

    private String path2Input;

    private String path2Output;

    private String processMethodName;

    private String input;

    private boolean isPdf;

    private boolean recursive = false;

    private boolean saveAssets = true;

    private boolean teiCoordinates = false;

    private boolean consolidateHeader = true;

    private boolean consolidateCitation = false;

    private boolean segmentSentences = false;

    private boolean addElementId = false;

    private GrobidModels.Flavor modelFlavor = null;

    /**
     * @return the path2grobidHome
     */
    public final String getPath2grobidHome() {
        return path2grobidHome;
    }

    /**
     * @param pPath2grobidHome
     *            the path2grobidHome to set
     */
    public final void setPath2grobidHome(final String pPath2grobidHome) {
        path2grobidHome = pPath2grobidHome;
    }

    /**
     * @return the path2grobidProperty
     */
    public final String getPath2grobidProperty() {
        return path2grobidProperty;
    }

    /**
     * @param pPath2grobidProperty
     *            the path2grobidProperty to set
     */
    public final void setPath2grobidProperty(final String pPath2grobidProperty) {
        path2grobidProperty = pPath2grobidProperty;
    }

    /**
     * @return the path2input
     */
    public final String getPath2Input() {
        return path2Input;
    }

    /**
     * @param pPath2input
     *            the path2input to set
     */
    public final void setPath2Input(final String pPath2input) {
        path2Input = pPath2input;
    }

    /**
     * @return the path2Output
     */
    public final String getPath2Output() {
        return path2Output;
    }

    /**
     * @param pPath2Output
     *            the path2Output to set
     */
    public final void setPath2Output(final String pPath2Output) {
        path2Output = pPath2Output;
    }

    /**
     * @return the processMethodName
     */
    public final String getProcessMethodName() {
        return processMethodName;
    }

    /**
     * @param pProcessMethodName
     *            the processMethodName to set
     */
    public final void setProcessMethodName(final String pProcessMethodName) {
        processMethodName = pProcessMethodName;
    }

    /**
     * @return the input
     */
    public final String getInput() {
        return input;
    }

    /**
     * @param pInput
     *            the input to set
     */
    public final void setInput(final String pInput) {
        input = pInput;
    }

    /**
     * @return the isPdf
     */
    public final boolean isPdf() {
        return isPdf;
    }

    /**
     * @param pIsPdf
     *            the isPdf to set
     */
    public final void setPdf(final boolean pIsPdf) {
        isPdf = pIsPdf;
    }

    /**
     * @return true if recursive file processing
     */
    public final boolean isRecursive() {
        return recursive;
    }

    /**
     * @return true if consolidation of header metadata should be done
     */
    public final boolean isConsolidateHeader() {
        return consolidateHeader;
    }

    /**
     * @return true if consolidation of citation metadata should be done
     */
    public final boolean isConsolidateCitation() {
        return consolidateCitation;
    }

    /**
     * @return true if consolidation of header metadata should be done
     */
    public final boolean getConsolidateHeader() {
        return consolidateHeader;
    }

    /**
     * @return true if consolidation of citation metadata should be done
     */
    public final boolean getConsolidateCitation() {
        return consolidateCitation;
    }

    /**
     * @return true if the PDF assets (bitmaps, vector graphics) should be also extracted and saved
     */
    public final boolean getSaveAssets() {
        return saveAssets;
    }

    /**
     * @param pSaveAssets true if the PDF assets (bitmaps, vector graphics) should be also extracted and saved
     */
    public final void setSaveAssets(boolean pSaveAssets) {
        saveAssets = pSaveAssets;
    }

    /**
     * @param pRecursive
     *            recursive file processing parameter to set
     */
    public final void setRecursive(final boolean pRecursive) {
        recursive = pRecursive;
    }

    /**
     * @return true if output a subset of the identified structures with coordinates in the original PDF
     */
    public final boolean getTeiCoordinates() {
        return teiCoordinates;
    }

    /**
     * @param pTeiCoordinates
     *            output a subset of the identified structures with coordinates in the original PDF
     */
    public final void setTeiCoordinates(final boolean pTeiCoordinates) {
        teiCoordinates = pTeiCoordinates;
    }

    /**
     * @return true if output a subset of the xml:id attributes must be added automatically to the resulting TEI XML elements
     */
    public final boolean getAddElementId() {
        return addElementId;
    }

    /**
     * @param pAddElementId
     *            add xml:id attribute automatically on elements in the resulting TEI XML
     */
    public final void setAddElementId(final boolean pAddElementId) {
        addElementId = pAddElementId;
    }

    /**
     * @return true if we add sentence segmentation level structures for paragraphs in the TEI XML result
     */
    public final boolean getSegmentSentences() {
        return segmentSentences;
    }

    /**
     * @param pSegmentSentences
     *            add sentence segmentation level structures for paragraphs in the TEI XML result
     */
    public final void setSegmentSentences(final boolean pSegmentSentences) {
        segmentSentences = pSegmentSentences;
    }

    public GrobidModels.Flavor getModelFlavor() {
        return modelFlavor;
    }

    public void setModelFlavor(GrobidModels.Flavor modelFlavor) {
        this.modelFlavor = modelFlavor;
    }
}
