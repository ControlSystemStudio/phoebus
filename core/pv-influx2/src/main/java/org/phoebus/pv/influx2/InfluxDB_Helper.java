/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.pv.influx2;

import com.influxdb.query.FluxRecord;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.pv.TimeHelper;

/**
 * Utility class for converting InfluxDB {@link FluxRecord} values to EPICS {@link VType} values.
 * <p>
 * Supports conversion of numeric values to {@link VDouble} and string values to {@link VString}.
 * Unhandled types will cause an exception to be thrown.
 *
 */
public class InfluxDB_Helper {
    /** Logger for the helper class */
    private static final Logger LOGGER = Logger.getLogger(InfluxDB_Helper.class.getName());

    /**
     * Converts a single {@link FluxRecord} from InfluxDB into a corresponding {@link VType}.
     *
     * @param record the InfluxDB record to convert
     * @return a {@link VType} representing the value in the record (e.g., {@link VDouble} or {@link VString})
     * @throws Exception if the record is null or contains an unsupported value type
     */
    public static VType convertRecordToVType(FluxRecord record) throws Exception {
        if (record == null) {
            throw new Exception("FluxRecord is null");
        }

        Object value = record.getValue();

        // Get the timestamp, default to now if missing
        Instant instant = record.getTime() != null ? record.getTime() : Instant.now();
        Time time = TimeHelper.fromInstant(instant);
        Alarm alarm = Alarm.none();
        Display display = Display.none();

        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            return VDouble.of(d, alarm, time, display);
        } else if (value instanceof String) {
            return VString.of((String) value, alarm, time);
        } else {
            assert value != null;
            LOGGER.log(Level.WARNING, "Unsupported type: " + value.getClass().getName());
            throw new Exception("Cannot handle type " + value.getClass().getName());
        }
    }
}
