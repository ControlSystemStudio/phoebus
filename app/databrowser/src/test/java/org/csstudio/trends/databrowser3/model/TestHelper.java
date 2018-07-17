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

import org.phoebus.util.array.ArrayDouble;
import org.phoebus.vtype.AlarmSeverity;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueFactory;

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
        return ValueFactory.newVDouble(Double.valueOf(i), ValueFactory.newTime(Instant.ofEpochMilli(i)));
    }

    /**@param ts timestamp
     * @param vals array
     * @return Sample that has waveform and time based on input parameter
     */
    public static VType makeWaveform(final int ts, final double array[])
    {
        return ValueFactory.newVDoubleArray(new ArrayDouble(array),
                ValueFactory.alarmNone(),
                ValueFactory.timeNow(),
                ValueFactory.displayNone());
    }

    /** @param i Pseudo-timestamp
     *  @return Sample that has error text with time based on input parameter
     */
    public static VType makeError(final int i, final String error)
    {
        return ValueFactory.newVDouble(Double.NaN,
                ValueFactory.newAlarm(AlarmSeverity.UNDEFINED, error),
                ValueFactory.newTime(Instant.ofEpochMilli(i)),
                ValueFactory.displayNone());
    }
}


