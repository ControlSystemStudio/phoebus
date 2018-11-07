/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.vtype;

import java.util.ArrayList;
import java.util.List;

import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ListInteger;

/** Immutable {@link VEnumArray} implementation
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
final class IVEnumArray extends VEnumArray
{
    private final EnumDisplay display;
    private final ListInteger indices;
    private final List<String> labels;
    private final Alarm alarm;
    private final Time time;

    public IVEnumArray(final EnumDisplay display, final ListInteger indices,
                       final Alarm alarm, final Time time)
    {
        this.display = display;
        this.indices = indices;
        this.alarm = alarm;
        this.time = time;
        labels = new ArrayList<>(indices.size());
        for (int i=0; i<indices.size(); ++i)
        {
            final int index = indices.getInt(i);
            if (index < 0  ||  index >= display.getChoices().size())
                throw new IndexOutOfBoundsException("VEnumArray element " + i +
                        " has index " + index +
                        " outside of permitted options " + display.getChoices());
            labels.add(display.getChoices().get(index));
        }
    }

    @Override
    public EnumDisplay getDisplay()
    {
        return display;
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
        return labels;
    }

    @Override
    public ListInteger getIndexes()
    {
        return indices;
    }

    @Override
    public ListInteger getSizes()
    {
        return ArrayInteger.of(labels.size());
    }
}
