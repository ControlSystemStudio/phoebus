/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import java.time.Instant;

import org.epics.vtype.Time;

/** Helper for VType {@link Time}
 *  @author Kay Kasemir
 */
public class TimeHelper
{
    /** Convert Instant into Time, considering 0 seconds as invalid
     *  @param instant
     *  @return Time
     */
    public static Time fromInstant(final Instant instant)
    {
        if (instant.getEpochSecond() <= 0)
            return Time.of(instant, 0, false);
        return Time.of(instant);
    }
}
