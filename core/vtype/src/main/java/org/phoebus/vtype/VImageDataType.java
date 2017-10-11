/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;
/**
 * 
 * Data type description for {@link VImage} data.
 * 
 * based on the the VImageDataType from org.epics.pvdata.pv
 * @author mrk
 *
 */
public enum VImageDataType {
    /**
     * Value has type <i>boolean</i>.
     */
    pvBoolean,
    /**
     * Value has type <i>byte</i>.
     */
    pvByte,
    /**
     * Value has type <i>short</i>.
     */
    pvShort,
    /**
     * Value has type <i>int</i>.
     */
    pvInt,
    /**
     * Value has type <i>long</i>.
     */
    pvLong,
    /**
     * Value has type <i>ubyte</i>.
     */
    pvUByte,
    /**
     * Value has type <i>ushort</i>.
     */
    pvUShort,
    /**
     * Value has type <i>uint</i>.
     */
    pvUInt,
    /**
     * Value has type <i>ulong</i>.
     */
    pvULong,
    /**
     * value has type <i>float</i>.
     */
    pvFloat,
    /**
     * Value has type <i>double</i>.
     */
    pvDouble,
    /**
     * Value has type <i>string</i>.
     */
    pvString;

    /**
     * Is this an integer (signed or unsigned). true if byte, short, int, long, ubyte, ushort, uint, or ulong.
     * @return true if it is an integer type
     */
    public boolean isInteger() {
        if( (ordinal() >= VImageDataType.pvByte.ordinal()) && (ordinal() <= VImageDataType.pvULong.ordinal()) ) {
            return true;
        }
        return false;
    }

    /**
     * Is this an unsigned integer. true if ubyte, ushort, uint, or ulong.
     * 
     * @return true if it is an unsigned integer type
     */
    public boolean isUInteger() {
        if( (ordinal() >= VImageDataType.pvUByte.ordinal()) && (ordinal() <= VImageDataType.pvULong.ordinal()) ) {
            return true;
        }
        return false;
    }

    /**
     * Is this a Java numeric type?
     * 
     * @return true if the type is a Java numeric type.
     * The numeric types are byte, short, int, long, float, and double.
     */
    public boolean isNumeric() {
        if( (ordinal() >= VImageDataType.pvByte.ordinal()) && (ordinal() <= VImageDataType.pvDouble.ordinal()) ) {
            return true;
        }
        return false;
    }

    /**
     * Is this a Java primitive type?
     * 
     * @return true if the type is a Java primitive type.
     * The numeric types and boolean are primitive types.
     */
    public boolean isPrimitive() {
        if(isNumeric()) return true;
        if(ordinal() == VImageDataType.pvBoolean.ordinal()) return true;
        return false;
    }

    /**
     * Get the VImageDataType for a string defining the type.
     * 
     * @param type a character string defining the type
     * @return the VImageDataType or null if an illegal type
     */
    public static VImageDataType getVImageDataType(String type) {
        if(type.equals("boolean")) return VImageDataType.pvBoolean;
        if(type.equals("byte")) return VImageDataType.pvByte;
        if(type.equals("short")) return VImageDataType.pvShort;
        if(type.equals("int")) return VImageDataType.pvInt;
        if(type.equals("long")) return VImageDataType.pvLong;
        if(type.equals("ubyte")) return VImageDataType.pvUByte;
        if(type.equals("ushort")) return VImageDataType.pvUShort;
        if(type.equals("uint")) return VImageDataType.pvUInt;
        if(type.equals("ulong")) return VImageDataType.pvULong;
        if(type.equals("float")) return VImageDataType.pvFloat;
        if(type.equals("double")) return VImageDataType.pvDouble;
        if(type.equals("string")) return VImageDataType.pvString;
        return null;
    }
    public String toString() {
        switch(this) {
        case pvBoolean: return "boolean";
        case pvByte: return "byte";
        case pvShort: return "short";
        case pvInt:   return "int";
        case pvLong:  return "long";
        case pvUByte: return "ubyte";
        case pvUShort: return "ushort";
        case pvUInt:   return "uint";
        case pvULong:  return "ulong";
        case pvFloat: return "float";
        case pvDouble: return "double";
        case pvString: return "string";
        }
        throw new IllegalArgumentException("Unknown VImageDataType");
    }
}