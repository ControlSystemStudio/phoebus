/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.log.derby;

import java.io.Serializable;
import java.util.Arrays;

/** User-defined data type for Derby that holds a sample's value.
 *
 *  <p>For now only supports array of {@link Number}.
 *
 *  <p>SQL has no generic array support across database dialects.
 *  Derby can store 'Object[]' in a user defined type because array types are Serializable.
 *  Other databases might require a BLOB.
 *  The {@link SampleValue} class is meant to abstract the differences.
 *  For now it only works with Derby, but it's the spot where support for other
 *  databases could be added.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SampleValue implements Serializable
{
    /** Serialization ID */
    final private static long serialVersionUID = 1L;

    /** Values.
     *  Really Number[], but serializing as Object[] to allow String[] etc. in the future.
     */
    final private Object[] values;

    /** Create {@link SampleValue} from number
     *
     *  <p>Meant to be called from Derby FUNCTION.
     *  Java code should use constructor.
     *
     *  @param value {@link Double}
     *  @return {@link SampleValue}
     *  @see #SampleValue(Object...)
     */
    public static SampleValue create(final Double value)
    {
        return new SampleValue(new Number[] { value });
    }

    /** Initialize
     *  @param values One or more {@link Number}
     *  @throws IllegalArgumentException when values not accepted
     */
    public SampleValue(final Object[] values)
    {
        if (values instanceof Number[]  ||
            values instanceof String[])
            this.values = values;
        else
            throw new IllegalArgumentException("Cannot handle " + values[0].getClass().getName());
    }

    /** @return First array element */
    public Number getNumber()
    {
        if (values[0] instanceof Number)
            return (Number) values[0];
        return null;
    }

    /** @return Values */
    public Object[] getValues()
    {
        return values;
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        if (values.length == 1)
            return values[0].toString();
        return Arrays.toString(values);
    }
}
