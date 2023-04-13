/*
 *
 * Copyright (C) 2023 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.epics.pva.data;

import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.*;

class PVAStructureArrayTest {

    @Test
    void update() throws Exception {
        PVAStructure[] structures = new PVAStructure[2];
        structures[0] = new PVAStructure("structure0", "struct 0", new PVAString("element0", "element0value"));
        PVAStructure second = structures[0].cloneType("structure1");
        second.get("element0").setValue(new PVAString("element0", "newElement1"));
        structures[1] = second;
        PVAStructureArray structureArray = new PVAStructureArray("structureArray", structures[0].cloneType("stype"),structures );

        PVAStructureArray cloneArray = structureArray.cloneData();
        cloneArray.get()[0].get("element0").setValue(new PVAString("element0", "newElement2"));

        assertNotEquals(cloneArray, structureArray);

        structureArray.update(0, cloneArray, new BitSet());

        assertEquals(cloneArray, structureArray);
    }

    @Test
    void setValue() throws Exception {
        // Create a Structure Array for testing
        PVAStructure[] structures = new PVAStructure[2];
        structures[0] = new PVAStructure("structure0", "struct 0", new PVAString("element0", "element0value"));
        PVAStructure second = structures[0].cloneType("structure1");
        second.get("element0").setValue(new PVAString("element0", "newElement1"));
        structures[1] = second;
        PVAStructureArray structureArray = new PVAStructureArray("structureArray", structures[0].cloneType("stype"),structures );

        // Clone the structure
        PVAStructureArray cloneArray = structureArray.cloneData();
        // Modify to be different to the original array
        cloneArray.get()[0].get("element0").setValue(new PVAString("element0", "newElement2"));

        assertNotEquals(cloneArray, structureArray);

        // Update the original to match the modified clone
        structureArray.setValue(cloneArray.cloneData());

        assertEquals(cloneArray, structureArray);

        // Modify the clone again
        cloneArray.get()[0].get("element0").setValue(new PVAString("element0", "newElement3"));
        assertNotEquals(cloneArray, structureArray);

        // Update the original to match the modified clone via the PVAStructure[]
        structureArray.setValue(cloneArray.get());
        assertEquals(cloneArray, structureArray);

        // Check errors are thrown when incompatible types are passed in
        PVAStructureArray diffTypeArray = new PVAStructureArray("name",
                new PVAStructure("different", "diff"),
                new PVAStructure("different", "diff"));
        assertThrows(ElementTypeException.class,() -> structureArray.setValue(diffTypeArray));
        assertThrows(ElementTypeException.class, () -> structureArray.setValue(diffTypeArray.get()));

    }
    @Test
    void set() throws Exception {
        // Create a Structure Array for testing
        PVAStructure[] structures = new PVAStructure[2];
        structures[0] = new PVAStructure("structure0", "struct 0", new PVAString("element0", "element0value"));
        PVAStructure second = structures[0].cloneType("structure1");
        second.get("element0").setValue(new PVAString("element0", "newElement1"));
        structures[1] = second;
        PVAStructureArray structureArray = new PVAStructureArray("structureArray", structures[0].cloneType("stype"),structures );

        // Clone the structure
        PVAStructureArray cloneArray = structureArray.cloneData();
        // Modify to be different to the original array
        cloneArray.get()[0].get("element0").setValue(new PVAString("element0", "newElement2"));

        assertNotEquals(cloneArray, structureArray);

        // Update the original to match the modified clone
        structureArray.set(cloneArray.cloneData().get());

        assertEquals(cloneArray, structureArray);

        // Check errors are thrown when incompatible types are passed in
        PVAStructureArray diffTypeArray = new PVAStructureArray("name",
                new PVAStructure("different", "diff"),
                new PVAStructure("different", "diff"));
        assertThrows(ElementTypeException.class, () -> structureArray.set(diffTypeArray.get()));
    }
}