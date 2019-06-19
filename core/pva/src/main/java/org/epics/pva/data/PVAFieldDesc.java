/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

/** Type codes for PVA field descriptors
 *  @author Kay Kasemir
 */
public interface PVAFieldDesc
{
    public static final byte NULL_TYPE_CODE = (byte)0xFF;
    public static final byte ONLY_ID_TYPE_CODE = (byte)0xFE;
    public static final byte FULL_WITH_ID_TYPE_CODE = (byte)0xFD;
    public static final byte FULL_TAGGED_ID_TYPE_CODE = (byte)0xFC;

    // Largest defined FULL_TYPE_CODE:
    // 0b10011011 = 0x9B = complex 100, fixed-size array 11, bounded string 011
    public static final byte FULL_TYPE_CODE_MAX = (byte)0xDF;

    public enum Array
    {
        FIXED_SIZE((byte)0b00011000),
        BOUNDED_SIZE((byte)0b00010000),
        VARIABLE_SIZE((byte)0b00001000),
        SCALAR((byte)0b00000000);

        final byte mask;

        private Array(final byte mask)
        {
            this.mask = mask;
        }

        public boolean matches(final byte field_desc)
        {
            final byte type = (byte) (field_desc & 0b00011000);
            return type == mask;
        }

        public static Array forFieldDesc(final byte field_desc)
        {
            final byte type = (byte) (field_desc & 0b00011000);
            for (Array array : Array.values())
                if (array.mask == type)
                    return array;
            return SCALAR;
        }
    }
}
