package org.grobid.core.engines.entities;

import org.grobid.core.data.ChemicalEntity;

/**
 * Chemical name-to-structure processing based on external Open Source libraries.
 *
 */
public class NameToStructureResolver {

    public static ChemicalEntity process(String name) {
        ChemicalEntity result = new ChemicalEntity(name);
        //

        return result;
    }

    public static void depict(ChemicalEntity structure, String path) {

    }

}
