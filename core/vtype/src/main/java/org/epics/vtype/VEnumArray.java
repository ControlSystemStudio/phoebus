/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.vtype;

import java.util.List;

import org.epics.util.array.ListInteger;

/** Enum array
 *
 *  <p>Based on similar type in original vtypes removed in EPICS 7.0.2
 *
 *  @author Kay Kasemir
 */
public abstract class VEnumArray extends Array implements AlarmProvider, TimeProvider
{
    /** Create immutable {@link VEnumArray}
     *  @param display Enum options
     *  @param indices Enum indices
     *  @param alarm Alarm
     *  @param time Timestamp
     *  @return {@link VEnumArray}
     */
    public static VEnumArray of(final EnumDisplay display, final ListInteger indices,
                                final Alarm alarm, final Time time)
    {
        return new IVEnumArray(display, indices, alarm, time);
    }

    /** @return the enum display information, i.e. choices */
    public abstract EnumDisplay getDisplay();

    /** @return Enum array elements as labels */
    @Override
    public abstract List<String> getData();

    /** @return Enum array elements as indices */
    public abstract ListInteger getIndexes();
}
