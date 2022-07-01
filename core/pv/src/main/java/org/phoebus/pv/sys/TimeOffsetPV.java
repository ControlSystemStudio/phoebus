/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * Copyright (c) 2022 Brookhaven National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.sys;

import org.phoebus.pv.sim.SimulatedStringPV;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimestampFormats;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAmount;
import java.util.List;

/** System "time" PV
 *  @author Kunal Shroff, Kay Kasemir, based on similar code in diirt
 */
public class TimeOffsetPV extends SimulatedStringPV
{
    private DateTimeFormatter formatter;
    private TemporalAmount temporalAmount;
    private double updateRate;

    /**
     * Factory method for creating a time pv with user defined offset, format, and update rate.
     *
     * @param name pv name
     * @param parameters a list of optional parameters defining timeoffset, format, update rate
     * @return a new TimeOffsetPV
     */
    public static TimeOffsetPV forParameters(final String name, final List<String> parameters) throws Exception {
        if(parameters != null && !parameters.isEmpty()) {
            if(parameters.size() == 1) {
                return new TimeOffsetPV(name, parameters.get(0), TimestampFormats.SECONDS_FORMAT, 1.0);
            } else if (parameters.size() == 2) {
                return new TimeOffsetPV(name, parameters.get(0), TimestampFormats.SECONDS_FORMAT, Double.valueOf(parameters.get(1)));
            } else if (parameters.size() == 3) {
                DateTimeFormatter formatter;
                switch (parameters.get(1).toLowerCase()) {
                    case "full":
                        formatter = TimestampFormats.FULL_FORMAT;
                        break;
                    case "milli":
                        formatter = TimestampFormats.MILLI_FORMAT;
                        break;
                    case "seconds":
                        formatter = TimestampFormats.SECONDS_FORMAT;
                        break;
                    case "datetime":
                        formatter = TimestampFormats.DATETIME_FORMAT;
                        break;
                    case "date":
                        formatter = TimestampFormats.DATE_FORMAT;
                        break;
                    case "time":
                        formatter = TimestampFormats.TIME_FORMAT;
                        break;
                    default:
                        formatter = TimestampFormats.SECONDS_FORMAT;
                        break;
                }
                return new TimeOffsetPV(name, parameters.get(0), formatter, Double.valueOf(parameters.get(2)));
            }
        } else {
            return new TimeOffsetPV(name, "now", TimestampFormats.SECONDS_FORMAT, 1.0);
        }
        throw new Exception("sim://timeOffset needs no parameters or (offset, update_seconds) or (offset, format, update_seconds)");
    }

    public TimeOffsetPV(final String name, final String offset, final DateTimeFormatter formatter, final double updateRate) {
        super(name);
        this.temporalAmount = TimeParser.parseTemporalAmount(offset);
        this.formatter = formatter;
        this.updateRate = updateRate;
        start(updateRate);
    }

    @Override
    public String compute()
    {
        return formatter.format(Instant.now().minus(temporalAmount));
    }
}
