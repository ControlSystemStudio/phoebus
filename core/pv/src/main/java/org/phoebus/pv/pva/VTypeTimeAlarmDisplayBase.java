/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.phoebus.vtype.Alarm;
import org.phoebus.vtype.Display;
import org.phoebus.vtype.Time;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueFactory;

/** Base {@link VType} that decodes {@link Time}, {@link Alarm} and {@link Display}
 *
 *  <p>Very similar to org.epics.pvmanager.pva.adapters.AlarmTimeDisplayExtractor
 */
@SuppressWarnings("nls")
class VTypeTimeAlarmDisplayBase extends VTypeTimeAlarmBase implements Display
{
    private final static Display noDisplay = ValueFactory.displayNone();

    /** Cache for formats */
    private static final Map<String, NumberFormat> formatterCache =
            new ConcurrentHashMap<String, NumberFormat>();

    /** @param printfFormat Format from NTScalar display.format
     *  @return Suitable NumberFormat
     */
    private static NumberFormat createNumberFormat(final String printfFormat)
    {
        if (printfFormat == null ||
            printfFormat.trim().isEmpty() ||
            printfFormat.equals("%s"))
            return noDisplay.getFormat();
        else
        {
            NumberFormat formatter = formatterCache.get(printfFormat);
            if (formatter != null)
                return formatter;
            formatter = new PrintfFormat(printfFormat);
            formatterCache.put(printfFormat, formatter);
            return formatter;
        }
    }

    static class PrintfFormat extends java.text.NumberFormat
    {
        private static final long serialVersionUID = 1L;
        private final String format;
        public PrintfFormat(final String printfFormat)
        {
            // probe format
            boolean allOK = true;
            try {
                String.format(printfFormat, 0.0);
            } catch (Throwable th) {
                allOK = false;
            }
            // accept it if all is OK
            this.format = allOK ? printfFormat : null;
        }

        private final String internalFormat(double number)
        {
            if (format != null)
                return String.format(format, number);
            else
                return String.valueOf(number);
        }

        @Override
        public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos)
        {
            toAppendTo.append(internalFormat(number));
            return toAppendTo;
        }

        @Override
        public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos)
        {
            toAppendTo.append(internalFormat(number));
            return toAppendTo;
        }

        @Override
        public Number parse(String source, ParsePosition parsePosition)
        {
            throw new UnsupportedOperationException("No parsing.");
        }
    };

    private String units;
    private NumberFormat format;
    private double display_low, display_high;
    private double control_low, control_high;
    private double alarm_low, warn_low, warn_high, alarm_high;

    public VTypeTimeAlarmDisplayBase(final PVStructure struct)
    {
        super(struct);

        // Decode display_t display
        PVStructure section = struct.getSubField(PVStructure.class, "display");
        if (section != null)
        {
            PVString str = section.getSubField(PVString.class, "units");
            units = str == null ? noDisplay.getUnits() : str.get();

            str = section.getSubField(PVString.class, "format");
            format = str == null
                ? noDisplay.getFormat()
                : createNumberFormat(str.get());

            display_low = PVStructureHelper.getDoubleValue(section, "limitLow", noDisplay.getLowerDisplayLimit());
            display_high = PVStructureHelper.getDoubleValue(section, "limitHigh", noDisplay.getUpperDisplayLimit());
        }
        else
        {
            units = noDisplay.getUnits();
            format = noDisplay.getFormat();
            display_low = noDisplay.getLowerDisplayLimit();
            display_high = noDisplay.getUpperDisplayLimit();
        }

        // Decode control_t control
        section = struct.getSubField(PVStructure.class, "control");
        if (section != null)
        {
            control_low = PVStructureHelper.getDoubleValue(section, "limitLow", noDisplay.getLowerCtrlLimit());
            control_high = PVStructureHelper.getDoubleValue(section, "limitHigh", noDisplay.getUpperCtrlLimit());
        }
        else
        {
            control_low = noDisplay.getLowerCtrlLimit();
            control_high = noDisplay.getUpperCtrlLimit();
        }

        // Decode valueAlarm_t valueAlarm
        section = struct.getSubField(PVStructure.class, "control");
        if (section != null)
        {
            alarm_low = PVStructureHelper.getDoubleValue(section, "lowAlarmLimit", noDisplay.getLowerAlarmLimit());
            warn_low = PVStructureHelper.getDoubleValue(section, "lowWarningLimit", noDisplay.getLowerWarningLimit());
            warn_high = PVStructureHelper.getDoubleValue(section, "highWarningLimit", noDisplay.getUpperWarningLimit());
            alarm_high = PVStructureHelper.getDoubleValue(section, "highAlarmLimit", noDisplay.getUpperAlarmLimit());
        }
        else
        {
            alarm_low = noDisplay.getLowerAlarmLimit();
            warn_low = noDisplay.getLowerWarningLimit();
            warn_high = noDisplay.getUpperWarningLimit();
            alarm_high = noDisplay.getUpperAlarmLimit();
        }
    }

    @Override
    public String getUnits()
    {
        return units;
    }

    @Override
    public NumberFormat getFormat()
    {
        return format;
    }

    @Override
    public Double getLowerDisplayLimit()
    {
        return display_low;
    }

    @Override
    public Double getUpperDisplayLimit()
    {
        return display_high;
    }

    @Override
    public Double getLowerCtrlLimit()
    {
        return control_low;
    }

    @Override
    public Double getUpperCtrlLimit()
    {
        return control_high;
    }

    @Override
    public Double getLowerAlarmLimit()
    {
        return alarm_low;
    }

    @Override
    public Double getLowerWarningLimit()
    {
        return warn_low;
    }

    @Override
    public Double getUpperWarningLimit()
    {
        return warn_high;
    }

    @Override
    public Double getUpperAlarmLimit()
    {
        return alarm_high;
    }
}
