/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.vtype;

import java.util.Collections;
import java.util.List;

/** String array
 *
 *  <p>Based on similar type in original vtypes removed in EPICS 7.0.2
 *
 *  @author Kay Kasemir
 */
public abstract class VStringArray extends Array implements AlarmProvider, TimeProvider
{
    /** Create immutable {@link VStringArray}
     *  @param data
     *  @param alarm Alarm
     *  @param time Timestamp
     *  @return {@link VStringArray}
     */
    public static VStringArray of(final List<String> data, final Alarm alarm, final Time time)
    {
        return new IVStringArray(Collections.unmodifiableList(data), alarm, time);
    }

    @Override
    public abstract List<String> getData();
}
