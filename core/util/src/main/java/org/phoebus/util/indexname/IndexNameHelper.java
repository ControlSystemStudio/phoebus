/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.util.indexname;

import java.time.Instant;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Helper class for calculating the index name for time based indices.
 * @author Evan Smith
 */
@SuppressWarnings("nls")
public class IndexNameHelper
{
    private String baseIndexName;
    private String currentDateSpan;
    private String dateSpanUnit;
    private Integer dateSpanValue;

    private Instant spanStart;
    private Instant spanEnd;

    private static final List<String> acceptedDateUnits = List.of("D", "W", "M", "Y");

    private static final String DELIMITER = "T";
    private static final String YEAR = "Y",
                                MONTH = "M",
                                WEEK = "W",
                                DAY = "D";

    /**
     *
     * @param baseIndexName : Index base name that the date will be appended to.
     * @param dateSpanUnit : The unit of the date span. Years (Y/y), Months (M/m), Weeks (W/w), and Days (D/d) are supported.
     * @param dateSpanValue : The integer number of date range units that each index will span.
     * @throws Exception : If any parameters are invalid or null.
     */
    public IndexNameHelper(final String baseIndexName, final String dateSpanUnit, final Integer dateSpanValue) throws Exception
    {
        if (null != baseIndexName)
            this.baseIndexName = baseIndexName;
        else
            throw new Exception("Base Index Name is null.");

        if (null != dateSpanUnit)
        {
            if (! acceptedDateUnits.contains(dateSpanUnit.toUpperCase()))
                throw new Exception("Date Span Unit is invalid.");
            this.dateSpanUnit = dateSpanUnit.toUpperCase();
        }
        else
            throw new Exception("Date Span Unit is null.");

        if (null != dateSpanValue)
            this.dateSpanValue = dateSpanValue;
        else
            throw new Exception("Date Span Value is null.");
    }

    /**
     * Return a time based index name for the given time. If the dateSpanValue is 0 then returns the base index name.
     * @param time
     * @return index_name
     */
    public String getIndexName(Instant time)
    {
        if (dateSpanValue < 1)
            return baseIndexName;

        if (null != time && (null == spanEnd || time.isAfter(spanEnd)))
        {
            setDateSpanStartAndEnd(time);
            currentDateSpan = parseCurrentDateSpan();
        }

        return baseIndexName + "_" + currentDateSpan;
    }

    private void setDateSpanStartAndEnd(Instant time)
    {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(time.toEpochMilli());
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);

        Integer dateSpanField = -1;

        if (dateSpanUnit.equals(YEAR))
        {
            // Roll the year back to the beginning (midnight of the first of the year).
            dateSpanField = Calendar.YEAR;
            calendar.set(Calendar.MONTH, Calendar.JANUARY);
            calendar.set(Calendar.WEEK_OF_MONTH, 1);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
        }
        if (dateSpanUnit.equals(MONTH))
        {
            // Roll the month back to the beginning (midnight of the first of the month).
            dateSpanField = Calendar.MONTH;
            calendar.set(Calendar.WEEK_OF_MONTH, 1);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
        }
        if (dateSpanUnit.equals(WEEK))
        {
            // Roll the week back to the beginning (midnight Sunday).
            dateSpanField = Calendar.WEEK_OF_YEAR;
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        }
        if (dateSpanUnit.equals(DAY))
            dateSpanField = Calendar.DATE;

        // Roll the day back to midnight
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        spanStart = calendar.toInstant();

        calendar.add(dateSpanField, dateSpanValue);

        spanEnd = calendar.toInstant();
    }

    private String parseCurrentDateSpan()
    {
        String fullDate = spanStart.toString();

        return fullDate.split(DELIMITER)[0];
    }

    public Instant getCurrentDateSpanStart()
    {
        return spanStart;
    }

    public Instant getCurrentDateSpanEnd()
    {
        return spanEnd;
    }
}
