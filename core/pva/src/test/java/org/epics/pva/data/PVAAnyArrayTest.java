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

import java.nio.ByteBuffer;
import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.*;

class PVAAnyArrayTest {

    @Test
    void decodeType() throws Exception {
        PVAAnyArray pvaAnyArray = new PVAAnyArray("name");
        ByteBuffer buffer = ByteBuffer.allocate(200);
        pvaAnyArray.encodeType(buffer, new BitSet());
        buffer.flip();
        PVATypeRegistry registry = new PVATypeRegistry();
        PVAData data = registry.decodeType("name", buffer);
        assertEquals(data.getClass(), pvaAnyArray.getClass());
    }

    @Test
    void cloneType() {
        PVAAnyArray pvaAnyArray = new PVAAnyArray("name");
        PVAData data = pvaAnyArray.cloneType("name2");
        assertEquals(data.getClass(), pvaAnyArray.getClass());

    }

    @Test
    void decode() throws Exception {
        PVAny[] anys = new PVAny[2];
        anys[0] = new PVAny("any0", new PVAString("string0", "stringValue0"));
        anys[1] = new PVAny("any1", new PVAInt("int1", 1));
        PVAAnyArray pvaAnyArray = new PVAAnyArray("variantArray", anys);


        ByteBuffer buffer = ByteBuffer.allocate(200);
        pvaAnyArray.encode(buffer);
        buffer.flip();

        PVAAnyArray newArray = new PVAAnyArray("variantArray");
        newArray.decode(new PVATypeRegistry(), buffer);

        assertEquals(newArray, pvaAnyArray);
    }

    @Test
    void format() {
        PVAny[] anys = new PVAny[2];
        anys[0] = new PVAny("any0", new PVAString("string0", "stringValue0"));
        anys[1] = new PVAny("any1", new PVAInt("int1", 1));
        PVAAnyArray pvaAnyArray = new PVAAnyArray("variantArray", anys);

        System.out.println(pvaAnyArray);
        assertFalse(pvaAnyArray.format().isEmpty());

    }

    @Test
    void update() throws Exception {
        PVAny[] anys = new PVAny[2];
        PVAStructure structure = new PVAStructure("structure0", "struct 0", new PVAString("element0", "element0value"));
        anys[0] = new PVAny("any s", structure);
        anys[1] = new PVAny("any string", new PVAString("element1", "valueElement1"));
        PVAAnyArray anyArray = new PVAAnyArray("anyArray", anys );

        PVAAnyArray cloneArray = anyArray.cloneData();
        cloneArray.get()[1].get().setValue(new PVAString("element1", "newElement2"));

        assertNotEquals(cloneArray, anyArray);
        anyArray.update(0, cloneArray, new BitSet());

        assertEquals(cloneArray, anyArray);
    }

    @Test
    void setValue() throws Exception {
        // Create a Structure Array for testing
        PVAny[] anys = new PVAny[2];
        PVAStructure structure = new PVAStructure("structure0", "struct 0", new PVAString("element0", "element0value"));
        anys[0] = new PVAny("any s", structure);
        anys[1] = new PVAny("any string", new PVAString("element1", "valueElement1"));
        PVAAnyArray anyArray = new PVAAnyArray("anyArray", anys );

        // Clone the structure
        PVAAnyArray cloneArray = anyArray.cloneData();
        // Modify to be different to the original array
        cloneArray.get()[1].get().setValue(new PVAString("element1", "newElement2"));

        assertNotEquals(cloneArray, anyArray);

        // Update the original to match the modified clone
        anyArray.setValue(cloneArray.cloneData());

        assertEquals(cloneArray, anyArray);

        // Modify the clone again
        cloneArray.get()[1].get().setValue(new PVAString("element1", "newElement3"));
        assertNotEquals(cloneArray, anyArray);

        // Update the original to match the modified clone via the PVAny[]
        cloneArray.setValue(anyArray.get());
        assertEquals(cloneArray, anyArray);

    }
    @Test
    void set() throws Exception {
        PVAny[] anys = new PVAny[2];
        PVAStructure structure = new PVAStructure("structure0", "struct 0", new PVAString("element0", "element0value"));
        anys[0] = new PVAny("any s", structure);
        anys[1] = new PVAny("any string", new PVAString("element1", "valueElement1"));
        PVAAnyArray anyArray = new PVAAnyArray("anyArray", anys );

        // Clone the structure
        PVAAnyArray cloneArray = anyArray.cloneData();
        // Modify to be different to the original array
        cloneArray.get()[1].get().setValue(new PVAString("element1", "newElement2"));

        assertNotEquals(cloneArray, anyArray);

        // Update the original to match the modified clone
        anyArray.set(cloneArray.cloneData().get());

        assertEquals(cloneArray, anyArray);
    }
}