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
package org.grobid.core.sax

import org.easymock.EasyMock
import org.grobid.core.document.Document
import org.grobid.core.document.DocumentNode
import org.grobid.core.document.DocumentNode.findNodeDepth
import org.grobid.core.document.DocumentSource
import org.hamcrest.CoreMatchers
import org.hamcrest.collection.IsCollectionWithSize
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import javax.xml.parsers.SAXParserFactory

class PDFALTOOutlineSaxHandlerTest {
    var spf: SAXParserFactory = SAXParserFactory.newInstance()

    var target: PDFALTOOutlineSaxHandler? = null
    var mockDocumentSource: DocumentSource? = null
    var document: Document? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        mockDocumentSource = EasyMock.createMock<DocumentSource?>(DocumentSource::class.java)

        document = Document.createFromText("")
        target = PDFALTOOutlineSaxHandler(document)
    }

    @Test
    @Throws(Exception::class)
    fun testParsing_pdf2XMLOutline_ShouldWork() {
        val `is` = this.javaClass.getResourceAsStream("pdfalto.xml_outline.xml")

        val p = spf.newSAXParser()
        p.parse(`is`, target)

        val root = target!!.getRootNode()
        Assert.assertTrue(root.getChildren().size > 0)
        assertThat<MutableList<DocumentNode?>?>(
            root.getChildren(),
            IsCollectionWithSize.hasSize<DocumentNode?>(9),
        )
        assertThat<String?>(root.getChildren().get(0).getLabel(), CoreMatchers.`is`<String?>("Abstract"))
        assertThat<MutableList<DocumentNode?>?>(
            root.getChildren().get(0).getChildren(),
            CoreMatchers.`is`<Any?>(
                CoreMatchers.nullValue(),
            ),
        )
        assertThat<Int?>(root.getChildren().get(0).getBoundingBox().getPage(), CoreMatchers.`is`<Int?>(1))
        // <LINK page="1" top="592.00" bottom="0.00" left="0.00" right="0.00"/>
//        assertThat(root.getChildren().get(0).getBoundingBox().getY(), is(0.0));
//        assertThat(root.getChildren().get(0).getBoundingBox().getHeight(), is(-1.0));
//        assertThat(root.getChildren().get(0).getBoundingBox().getX(), is(0.0));
//        assertThat(root.getChildren().get(0).getBoundingBox().getWidth(), is(0.0));
    }

    @Test
    @Throws(Exception::class)
    fun testParsing_pdf2XMLOutline2_ShouldWork() {
        val `is` = this.javaClass.getResourceAsStream("example1_outline.xml")

        val p = spf.newSAXParser()
        p.parse(`is`, target)

        val root = target!!.rootNode
        Assert.assertTrue(root.getChildren().size > 0)
        // The single depth-1 node is the article title; every section hangs under it.
        assertThat<MutableList<DocumentNode?>?>(
            root.getChildren(),
            IsCollectionWithSize.hasSize<DocumentNode?>(1),
        )

        // Sections after the nested "4 Experimental procedures" list must reattach to the
        // title node, not leak up to the root (regression guard for the parent-id stack).
        val titleNode = root.getChildren().get(0)
        val titleChildLabels = titleNode.getChildren().map { it!!.getLabel() }
        assertThat(titleChildLabels, CoreMatchers.hasItem("Acknowledgements"))
        assertThat(titleChildLabels, CoreMatchers.hasItem("References"))

        val introDepth = findNodeDepth(root, "Introduction", 0)
        assertThat(introDepth, CoreMatchers.`is`<Int?>(2))
    }

    @Test
    @Throws(Exception::class)
    fun testParsing_pdf2XMLOutline3_ShouldWork() {
        val `is` = this.javaClass.getResourceAsStream("buggy_outline.xml")

        val p = spf.newSAXParser()
        p.parse(`is`, target)

        val root = target!!.rootNode
        Assert.assertTrue(root.getChildren().size > 0)
        // The single depth-1 node is the article title; every section hangs under it.
        assertThat<MutableList<DocumentNode?>?>(
            root.getChildren(),
            IsCollectionWithSize.hasSize<DocumentNode?>(1),
        )

        // Labels here carry soft hyphens and a '|' number separator (e.g. "1|INTRODUCTION"),
        // so this also exercises normalization in findNodeDepth.
        val introDepth = findNodeDepth(root, "Introduction", 0)
        assertThat(introDepth, CoreMatchers.`is`<Int?>(2))
    }

    @Test
    @Throws(Exception::class)
    fun testParsing_pdf2XMLOutline_errorcase_ShouldWork() {
        val `is` = this.javaClass.getResourceAsStream("test_outline.xml")

        val p = spf.newSAXParser()
        p.parse(`is`, target)

        val root = target!!.getRootNode()
        assertThat<MutableList<DocumentNode?>?>(
            root.getChildren(),
            IsCollectionWithSize.hasSize<DocumentNode?>(5),
        )

        assertThat<String?>(root.getChildren().get(0).getLabel(), CoreMatchers.`is`<String?>("A Identification"))
        assertThat<MutableList<DocumentNode?>?>(
            root.getChildren().get(0).getChildren(),
            CoreMatchers.`is`<Any?>(
                CoreMatchers.nullValue(),
            ),
        )
        // <LINK page="2" top="71.0000" bottom="0.0000" left="68.0000" right="0.0000"/>
        assertThat<Int?>(root.getChildren().get(0).getBoundingBox().getPage(), CoreMatchers.`is`<Int?>(2))

        //        assertThat(root.getChildren().get(0).getBoundingBox().getY(), is(71.000));
//        assertThat(root.getChildren().get(0).getBoundingBox().getHeight(), is(0.0));
//        assertThat(root.getChildren().get(0).getBoundingBox().getX(), is(68.000));
//        assertThat(root.getChildren().get(0).getBoundingBox().getWidth(), is(0.0));
        assertThat<String?>(
            root.getChildren().get(1).getLabel(),
            CoreMatchers.`is`<String?>("B Résumé consolidé public."),
        )
        assertThat<MutableList<DocumentNode?>?>(
            root.getChildren().get(1).getChildren(),
            IsCollectionWithSize.hasSize<DocumentNode?>(1),
        )
        // <LINK page="2" top="377.000" bottom="0.0000" left="68.0000" right="0.0000"/>
        assertThat<Int?>(root.getChildren().get(1).getBoundingBox().getPage(), CoreMatchers.`is`<Int?>(2))

        //        assertThat(root.getChildren().get(1).getBoundingBox().getY(), is(377.000));
//        assertThat(root.getChildren().get(1).getBoundingBox().getHeight(), is(0.0));
//        assertThat(root.getChildren().get(1).getBoundingBox().getX(), is(68.000));
//        assertThat(root.getChildren().get(1).getBoundingBox().getWidth(), is(0.0));
        assertThat<MutableList<DocumentNode?>?>(
            root.getChildren().get(1).getChildren(),
            IsCollectionWithSize.hasSize<DocumentNode?>(1),
        )
        assertThat<String?>(
            root.getChildren().get(1).getChildren().get(0).getLabel(),
            CoreMatchers.`is`<String?>("B.1 Résumé consolidé public en français"),
        )
        // <LINK page="2" top="412.000" bottom="0.0000" left="68.0000" right="0.0000"/>
        assertThat<Int?>(
            root.getChildren().get(1).getChildren().get(0).getBoundingBox().getPage(),
            CoreMatchers.`is`<Int?>(2),
        )

        //        assertThat(root.getChildren().get(1).getChildren().get(0).getBoundingBox().getY(), is(412.000));
//        assertThat(root.getChildren().get(1).getChildren().get(0).getBoundingBox().getHeight(), is(0.0));
//        assertThat(root.getChildren().get(1).getChildren().get(0).getBoundingBox().getX(), is(68.000));
//        assertThat(root.getChildren().get(1).getChildren().get(0).getBoundingBox().getWidth(), is(0.0));
        assertThat<String?>(
            root.getChildren().get(2).getLabel(),
            CoreMatchers.`is`<String?>("C Mémoire scientifique en français"),
        )
        assertThat<MutableList<DocumentNode?>?>(
            root.getChildren().get(2).getChildren(),
            IsCollectionWithSize.hasSize<DocumentNode?>(6),
        )
        assertThat<String?>(
            root.getChildren().get(2).getChildren().get(2).getLabel(),
            CoreMatchers.`is`<String?>("C.3 Approche scientifique et technique"),
        )
        assertThat<String?>(
            root.getChildren().get(3).getLabel(),
            CoreMatchers.`is`<String?>("D Liste des livrables"),
        )
        assertThat<MutableList<DocumentNode?>?>(
            root.getChildren().get(3).getChildren(),
            CoreMatchers.`is`<Any?>(
                CoreMatchers.nullValue(),
            ),
        )
        assertThat<String?>(
            root.getChildren().get(4).getLabel(),
            CoreMatchers.`is`<String?>("E Impact du projet"),
        )
        assertThat<MutableList<DocumentNode?>?>(
            root.getChildren().get(4).getChildren(),
            IsCollectionWithSize.hasSize<DocumentNode?>(4),
        )
        assertThat<String?>(
            root.getChildren().get(4).getChildren().get(1).getLabel(),
            CoreMatchers.`is`<String?>("E.2 Liste des publications et communications"),
        )
        assertThat<String?>(
            root.getChildren().get(4).getChildren().get(2).getLabel(),
            CoreMatchers.`is`<String?>("E.3 Liste des autres valorisations scientifiques"),
        )
        // <LINK page="1" top="170.000" bottom="0.0000" left="68.0000" right="0.0000"/>
        assertThat<Int?>(
            root.getChildren().get(4).getChildren().get(2).getBoundingBox().getPage(),
            CoreMatchers.`is`<Int?>(1),
        )
        //        assertThat(root.getChildren().get(4).getChildren().get(2).getBoundingBox().getY(), is(170.000));
//        assertThat(root.getChildren().get(4).getChildren().get(2).getBoundingBox().getHeight(), is(0.0));
//        assertThat(root.getChildren().get(4).getChildren().get(2).getBoundingBox().getX(), is(68.000));
//        assertThat(root.getChildren().get(4).getChildren().get(2).getBoundingBox().getWidth(), is(0.0));
    }
}
