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
package org.grobid.core.tokenization;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.LayoutTokensUtil;

public class TaggingTokenClusterorTest {

    @BeforeClass
    public static void init() {
        GrobidProperties.getInstance();
    }

    @Test
    public void testExclusion_notPresent_shouldReturnTrue() throws Exception {
        final TaggingTokenClusteror.LabelTypeExcludePredicate labelTypeExcludePredicate = new TaggingTokenClusteror.LabelTypeExcludePredicate(
                TaggingLabels.EQUATION, TaggingLabels.HEADER_KEYWORD);

        assertThat(
                labelTypeExcludePredicate.apply(new TaggingTokenCluster(TaggingLabels.FIGURE)),
                is(true));
    }

    @Test
    public void testExclusion_shouldReturnFalse() throws Exception {
        final TaggingTokenClusteror.LabelTypeExcludePredicate labelTypeExcludePredicate = new TaggingTokenClusteror.LabelTypeExcludePredicate(
                TaggingLabels.EQUATION, TaggingLabels.FIGURE);

        assertThat(
                labelTypeExcludePredicate.apply(new TaggingTokenCluster(TaggingLabels.FIGURE)),
                is(false));
    }

    @Test
    public void testInclusion_notPresent_shouldReturnFalse() throws Exception {
        final TaggingTokenClusteror.LabelTypePredicate labelTypePredicate = new TaggingTokenClusteror.LabelTypePredicate(
                TaggingLabels.HEADER_KEYWORD);

        assertThat(
                labelTypePredicate.apply(new TaggingTokenCluster(TaggingLabels.FIGURE)),
                is(false));
    }

    @Test
    public void testInclusion_present_shouldReturnTrue() throws Exception {
        final TaggingTokenClusteror.LabelTypePredicate labelTypePredicate = new TaggingTokenClusteror.LabelTypePredicate(
                TaggingLabels.FIGURE);

        assertThat(
                labelTypePredicate.apply(new TaggingTokenCluster(TaggingLabels.FIGURE)),
                is(true));
    }

    /**
     * In the NER the beginning labels are starting by B-
     * In GROBID the beginning labels are starting by I-
     **/
    @Test
    public void testCluster_mixedBeginningLabels_shouldWork() throws Exception {
        final InputStream is = this.getClass().getResourceAsStream("example.wapiti.output.2.txt");
        List<LayoutToken> tokenisation = GrobidAnalyzer.getInstance()
                .tokenizeWithLayoutToken(
                        "Austria invaded and fought the Serbian army at the Battle of Cer and Battle of Kolubara beginning on 12 August.",
                        new Language(Language.EN));

        final String s = IOUtils.toString(is, UTF_8);

        TaggingTokenClusteror target = new TaggingTokenClusteror(GrobidModels.ENTITIES_NER, s, tokenisation);

        List<TaggingTokenCluster> clusters = target.cluster();

        assertThat(clusters, hasSize(10));

        assertThat(clusters.get(0).getTaggingLabel().getLabel(), is("LOCATION"));
        assertThat(
                LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(clusters.get(0).concatTokens())),
                is("Austria"));

        assertThat(clusters.get(2).getTaggingLabel().getLabel(), is("ORGANISATION"));
        assertThat(
                LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(clusters.get(2).concatTokens())),
                is("Serbian army"));

        assertThat(clusters.get(4).getTaggingLabel().getLabel(), is("EVENT"));
        assertThat(
                LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(clusters.get(4).concatTokens())),
                is("Battle of Cer"));

        assertThat(clusters.get(6).getTaggingLabel().getLabel(), is("EVENT"));
        assertThat(
                LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(clusters.get(6).concatTokens())),
                is("Battle of Kolubara"));

        assertThat(clusters.get(8).getTaggingLabel().getLabel(), is("PERIOD"));
        assertThat(
                LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(clusters.get(8).concatTokens())),
                is("12 August"));
    }

    @Test
    public void testCluster_longFile() throws Exception {
        final InputStream is = this.getClass().getResourceAsStream("example.wapiti.output.1.txt");

        List<LayoutToken> tokenisation = GrobidAnalyzer.getInstance()
                .tokenizeWithLayoutToken(
                        "Austria invaded and fought the Serbian army at the Battle of Cer and Battle of Kolubara beginning on 12 August. \n"
                                +
                                "\n"
                                +
                                "The army, led by general Paul von Hindenburg defeated Russia in a series of battles collectively known as the First Battle of Tannenberg (17 August - 2 September). But the failed Russian invasion, causing the fresh German troops to move to the east, allowed the tactical Allied victory at the First Battle of the Marne. \n"
                                +
                                "\n"
                                +
                                "Unfortunately for the Allies, the pro-German King Constantine I dismissed the pro-Allied government of E. Venizelos before the Allied expeditionary force could arrive. Beginning in 1915, the Italians under Cadorna mounted eleven offensives on the Isonzo front along the Isonzo River, northeast of Trieste.\n"
                                +
                                "\n"
                                +
                                " At the Siege of Maubeuge about 40000 French soldiers surrendered, at the battle of Galicia Russians took about 100-120000 Austrian captives, at the Brusilov Offensive about 325 000 to 417 000 Germans and Austrians surrendered to Russians, at the Battle of Tannenberg 92,000 Russians surrendered.\n"
                                +
                                "\n"
                                +
                                " After marching through Belgium, Luxembourg and the Ardennes, the German Army advanced, in the latter half of August, into northern France where they met both the French army, under Joseph Joffre, and the initial six divisions of the British Expeditionary Force, under Sir John French. A series of engagements known as the Battle of the Frontiers ensued. Key battles included the Battle of Charleroi and the Battle of Mons. In the former battle the French 5th Army was almost destroyed by the German 2nd and 3rd Armies and the latter delayed the German advance by a day. A general Allied retreat followed, resulting in more clashes such as the Battle of Le Cateau, the Siege of Maubeuge and the Battle of St. Quentin (Guise). \n"
                                +
                                "\n"
                                +
                                "The German army came within 70 km (43 mi) of Paris, but at the First Battle of the Marne (6-12 September), French and British troops were able to force a German retreat by exploiting a gap which appeared between the 1st and 2nd Armies, ending the German advance into France. The German army retreated north of the Aisne River and dug in there, establishing the beginnings of a static western front that was to last for the next three years. Following this German setback, the opposing forces tried to outflank each other in the Race for the Sea, and quickly extended their trench systems from the North Sea to the Swiss frontier. The resulting German-occupied territory held 64% of France's pig-iron production, 24% of its steel manufacturing, dealing a serious, but not crippling setback to French industry.\n"
                                +
                                " ",
                        new Language(Language.EN));

        final String s = IOUtils.toString(is, UTF_8);

        TaggingTokenClusteror target = new TaggingTokenClusteror(GrobidModels.ENTITIES_NER, s, tokenisation);

        List<TaggingTokenCluster> clusters = target.cluster();

        assertThat(clusters, hasSize(164));

        //        for (TaggingTokenCluster cluster : clusters) {
        //            String clusterContent = LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(cluster.concatTokens()));
        //            System.out.println(clusterContent + " --> " + cluster.getTaggingLabel().getLabel());
        //        }

    }

}
