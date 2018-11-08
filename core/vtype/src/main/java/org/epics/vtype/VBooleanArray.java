/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.vtype;

import org.epics.util.array.ListBoolean;

/** Boolean array
 *
 *  <p>Based on similar type in original vtypes removed in EPICS 7.0.2
 *
 *  @author Kay Kasemir
 */
public abstract class VBooleanArray extends Array implements AlarmProvider, TimeProvider
{
    /** Create immutable {@link VBooleanArray}
     *  @param data The values
     *  @param alarm Alarm
     *  @param time Timestamp
     *  @return {@link VBooleanArray}
     */
    public static VBooleanArray of(final ListBoolean data,
                                   final Alarm alarm, final Time time)
    {
        return new IVBooleanArray(data, alarm, time);
    }

    /** @return Enum array elements as labels */
    @Override
    public abstract ListBoolean getData();
}
