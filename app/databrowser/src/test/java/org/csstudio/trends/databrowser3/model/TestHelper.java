/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import java.time.Instant;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.epics.util.array.ArrayDouble;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VType;

/** Unit-test helper for creating samples
 *  @author Kay Kasemir
 *  @author Takashi Nakamoto added makeWaveform() method
 */
@SuppressWarnings("nls")
public class TestHelper
{
    public static void setup()
    {
        // Logging
        final Level level = Level.FINE;
        Logger logger = Logger.getLogger("");
        logger.setLevel(level);
        for (Handler handler : logger.getHandlers())
            handler.setLevel(level);
    }

    /** @param i Numeric value as well as pseudo-timestamp
     *  @return Sample that has value and time based on input parameter
     */
    public static VType makeValue(final int i)
    {
        return VDouble.of(Double.valueOf(i), Alarm.none(), Time.of(Instant.ofEpochMilli(i)), Display.none());
    }

    /**@param ts timestamp
     * @param vals array
     * @return Sample that has waveform and time based on input parameter
     */
    public static VType makeWaveform(final int ts, final double array[])
    {
        return VDoubleArray.of(ArrayDouble.of(array),
                               Alarm.none(),
                               Time.now(),
                               Display.none());
    }

    /** @param i Pseudo-timestamp
     *  @return Sample that has error text with time based on input parameter
     */
    public static VType makeError(final int i, final String error)
    {
        return VDouble.of(Double.NaN,
                          Alarm.of(AlarmSeverity.UNDEFINED, AlarmStatus.CLIENT, error),
                          Time.of(Instant.ofEpochMilli(i)),
                          Display.none());
    }
}


