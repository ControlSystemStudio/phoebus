/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.vtype;

import java.util.List;

import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ListInteger;

/** Immutable {@link VStringArray} implementation
 *
 *  @author Kay Kasemir
 */
final class IVStringArray extends VStringArray
{
    private final List<String> data;
    private final Alarm alarm;
    private final Time time;

    public IVStringArray(final List<String> data, final Alarm alarm, final Time time)
    {
        this.data = data;
        this.alarm = alarm;
        this.time = time;
    }

    @Override
    public Alarm getAlarm()
    {
        return alarm;
    }

    @Override
    public Time getTime()
    {
        return time;
    }

    @Override
    public List<String> getData()
    {
        return data;
    }

    @Override
    public ListInteger getSizes()
    {
        return ArrayInteger.of(data.size());
    }
}
