/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

import org.junit.Test;

@SuppressWarnings("nls")
public class EncodeTypeTest
{
    @Test
    public void testEncodeType() throws Exception
    {
        // Define some data structure
        final PVAStructure time = new PVAStructure("time", "time_t",
                new PVALong("seconds"),
                new PVAInt("nano"));
        final PVAStructure attr = new PVAStructure("", "epics:nt/NTAttribute:1.0",
                new PVAString("name"),
                new PVAny("value"),
                new PVAString("descriptor"),
                new PVAInt("sourceType"),
                new PVAString("source"));
        final PVAStructureArray attribs = new PVAStructureArray("attribute", attr);
        final PVAStructure struct = new PVAStructure("Example", "example_t",
                new PVADouble("value"),
                time,
                attribs);
        struct.setTypeID((short)1);
        time.setTypeID((short)2);
        attribs.setTypeID((short)8);
        attr.setTypeID((short)9);

        // Encode its type
        System.out.println(struct.formatType());
        final ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        struct.encodeType(buffer, new BitSet());
        buffer.flip();
        System.out.println(Hexdump.toHexdump(buffer));

        // Parse type back
        final PVATypeRegistry types = new PVATypeRegistry();
        final PVAData decoded = types.decodeType("Example", buffer);
        System.out.println(decoded.formatType());

        assertThat(struct.formatType(), equalTo(decoded.formatType()));

    }
}