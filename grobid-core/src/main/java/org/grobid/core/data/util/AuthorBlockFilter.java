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
package org.grobid.core.data.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.data.Person;

/**
 * Selects the persons of the header author blocks that form the author list.
 *
 * <p>The HEADER model labels every name list it sees as {@code <author>}, so besides the author
 * list a header can carry later mentions of the same people (contact or correspondence lines,
 * contribution statements, ORCID lists, funding tables) and of other people (funders, editors).
 * Each {@code <author>} cluster is parsed as a separate block. Two structural rules decide which
 * blocks contribute authors; they look only at the blocks themselves, never at their wording:</p>
 * <ol>
 * <li>the first block yielding persons is always kept; a later block is a re-mention when at least
 * half of its persons are already in the list (see {@link Person#isSamePerson(Person, Person)}),
 * and is then dropped entirely, together with the name fragments it contains;</li>
 * <li>when the author list starts before the abstract, blocks placed after the abstract are not
 * part of it and are dropped. Headers whose only author block follows the abstract are not
 * affected.</li>
 * </ol>
 * <p>Remaining exact duplicates are handled afterwards by {@link Person#deduplicate(List)}.</p>
 */
public class AuthorBlockFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorBlockFilter.class);

    /**
     * @param blocks        persons of each author block, in document order
     * @param afterAbstract for each block, whether it is located after an abstract; null when the
     *                      position is not known, in which case only the re-mention rule applies
     * @return the persons kept, in document order
     */
    public static List<Person> filter(List<List<Person>> blocks, List<Boolean> afterAbstract) {
        List<Person> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(blocks)) {
            return result;
        }
        if (afterAbstract != null && afterAbstract.size() != blocks.size()) {
            LOGGER.warn(
                    "{} author blocks but {} block positions, ignoring the positions",
                    blocks.size(),
                    afterAbstract.size());
            afterAbstract = null;
        }

        boolean listBeforeAbstract = false;
        if (afterAbstract != null) {
            for (int k = 0; k < blocks.size(); k++) {
                if (!afterAbstract.get(k) && hasNamedPerson(blocks.get(k))) {
                    listBeforeAbstract = true;
                    break;
                }
            }
        }

        for (int k = 0; k < blocks.size(); k++) {
            List<Person> block = blocks.get(k);
            if (CollectionUtils.isEmpty(block)) {
                continue;
            }
            if (!hasNamedPerson(result)) {
                result.addAll(block);
                continue;
            }
            if (listBeforeAbstract && afterAbstract.get(k)) {
                LOGGER.debug("Author block {} dropped: after the abstract", k);
                continue;
            }
            int named = 0;
            int known = 0;
            for (Person person : block) {
                if (StringUtils.isBlank(person.getLastName())) {
                    continue;
                }
                named++;
                for (Person accepted : result) {
                    if (Person.isSamePerson(accepted, person)) {
                        known++;
                        break;
                    }
                }
            }
            if (named > 0 && known * 2 >= named) {
                LOGGER.debug("Author block {} dropped: {} of {} persons already listed", k, known, named);
                continue;
            }
            result.addAll(block);
        }
        return result;
    }

    private static boolean hasNamedPerson(List<Person> persons) {
        for (Person person : persons) {
            if (StringUtils.isNotBlank(person.getLastName())) {
                return true;
            }
        }
        return false;
    }
}
