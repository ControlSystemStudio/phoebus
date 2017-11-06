/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/** Demo of handling unsigned numbers in Java
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class UnsignedsTest
{
    @Test
    public void testUnsignedByte()
    {
        // signed byte overflows from +127 to -128
        byte b = 127;
        ++b;
        // convert to int gives +128
        int i = Byte.toUnsignedInt(b);
        System.out.println("Byte " + b + " -> " + i);
        assertThat(i, equalTo(128));

        b = -1;
        i = Byte.toUnsignedInt(b);
        System.out.println("Byte " + b + " -> " + i);
        assertThat(i, equalTo(255));

        long l = Byte.toUnsignedLong(b);
        System.out.println("Byte " + b + " -> " + l);
        assertThat(l, equalTo(255L));
    }

    @Test
    public void testUnsignedShort()
    {
        // signed short overflows to -32768
        short s = 0x7FFF;
        ++s;
        // convert to int gives +32768
        int i = Short.toUnsignedInt(s);
        System.out.println("Short " + s + " -> " + i);
        assertThat(i, equalTo(32768));

        s = -1;
        i = Short.toUnsignedInt(s);
        System.out.println("Short " + s + " -> " + i);
        assertThat(i, equalTo(65535));
    }

    @Test
    public void testUnsignedInt()
    {
        // signed int overflows to -2147483648
        int i = 0x7FFFFFFF;
        ++i;
        // convert to int gives +2147483648
        long l = Integer.toUnsignedLong(i);
        System.out.println("Int " + i + " -> " + l);
        assertThat(l, equalTo(2147483648L));

        i = -1;
        l = Integer.toUnsignedLong(i);
        System.out.println("Int " + i + " -> " + l);
        assertThat(l, equalTo(4294967295L));
    }
}
