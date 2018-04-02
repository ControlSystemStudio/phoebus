/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.ndarray;


/** Type of N-dimensional array
 *
 *  <p>Available data types as enum, with helpers.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public enum NDType
{
    FLOAT64("float"),
    FLOAT32("float32"),
    INT64("int64"),
    INT32("int32"),
    INT16("int16"),
    INT8("int8"),
    BOOL("bool");

    final private String name;

    /** Initialize
     *  @param name
     */
    private NDType(final String name)
    {
        this.name = name;
    }

    /** @return Type name presented to users */
    @Override
    public String toString()
    {
        return name;
    }

    /** Determine type of a Java object
     *  @param object Object of type {@link Double}, ..., {@link Byte}
     *  @return Matching type
     *  @throws IllegalArgumentException for unhandled data type
     */
    public static NDType forJavaObject(final Object object)
    {
        if (object instanceof Double)
            return FLOAT64;
        else if (object instanceof Float)
            return FLOAT32;
        else if (object instanceof Long)
            return INT64;
        else if (object instanceof Integer)
            return INT64;
        else if (object instanceof Short)
            return INT16;
        else if (object instanceof Byte)
            return INT8;
        else if (object instanceof Boolean)
            return BOOL;
        else if (object == null)
            throw new NullPointerException("Cannot determine data type for null");
        throw new IllegalArgumentException("Unhandled data type " +object.getClass().getName());
    }

    /** @param type_a One type
     *  @param type_b Other type
     *  @return Type that can hold both input types
     */
    public static NDType determineSuperType(final NDType type_a, final NDType type_b)
    {
        if (type_a == FLOAT64  ||  type_b == FLOAT64)
            return FLOAT64;
        else if (type_a == FLOAT32  ||  type_b == FLOAT32)
            return FLOAT32;
        else if (type_a == INT64  ||  type_b == INT64)
            return INT64;
        else if (type_a == INT32  ||  type_b == INT32)
            return INT32;
        else if (type_a == INT16  ||  type_b == INT16)
            return INT16;
        return INT8;
    }
}
