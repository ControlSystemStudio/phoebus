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

import org.junit.Test;

public class SerializeTest
{
    @Test
    public void testSize()
    {
        final ByteBuffer buffer = ByteBuffer.allocate(100);
        // Check magic -1,
        // check 'ubyte' values from 0 to ~255 and a little beyond
        // to assert that encoding border is handled
        for (int size=-1; size<1026; ++size)
        {
            buffer.clear();
            PVASize.encodeSize(size, buffer);
            buffer.flip();
            final int readback = PVASize.decodeSize(buffer);
            assertThat(readback, equalTo(size));
        }


        // Check values up to MAX_VALUE
        for (int size=Integer.MAX_VALUE-100; size > 0; ++size)
        {
            buffer.clear();
            PVASize.encodeSize(size, buffer);
            buffer.flip();
            final int readback = PVASize.decodeSize(buffer);
            // System.out.println(Integer.toHexString(readback));
            assertThat(readback, equalTo(size));
        }
    }
}