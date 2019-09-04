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
import java.util.BitSet;

import org.junit.Test;

public class BitSetTest
{
    @Test
    public void testBitSet()
    {
        final ByteBuffer buffer = ByteBuffer.allocate(100);
        BitSet bits = new BitSet();

        PVABitSet.encodeBitSet(bits, buffer);
        buffer.flip();
        System.out.println(Hexdump.toHexdump(buffer));

        bits.set(7);
        buffer.clear();
        PVABitSet.encodeBitSet(bits, buffer);
        buffer.flip();
        System.out.println(Hexdump.toHexdump(buffer));

        bits.clear();
        bits.set(8);
        buffer.clear();
        PVABitSet.encodeBitSet(bits, buffer);
        buffer.flip();
        System.out.println(Hexdump.toHexdump(buffer));

        bits.clear();
        bits.set(0);
        bits.set(1);
        bits.set(2);
        bits.set(4);
        bits.set(8);
        buffer.clear();
        PVABitSet.encodeBitSet(bits, buffer);
        buffer.flip();
        System.out.println(Hexdump.toHexdump(buffer));

        bits.clear();
        bits.set(65);
        buffer.clear();
        PVABitSet.encodeBitSet(bits, buffer);
        buffer.flip();
        System.out.println(Hexdump.toHexdump(buffer));

        final BitSet copy = PVABitSet.decodeBitSet(buffer);
        assertThat(copy, equalTo(bits));
    }
}