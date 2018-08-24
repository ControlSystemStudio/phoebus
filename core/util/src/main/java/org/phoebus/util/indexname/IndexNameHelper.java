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
public class IndexNameHelper
{
    private String dateSpanUnit;
    private Integer dateSpanField;
    private Integer dateSpanValue;
    private String baseIndexName;
    private String currentDateBlock;
    
    private Instant cutoff;
    private String indexName;
    private List<String> acceptedDateUnits = List.of("D", "W", "M", "Y");
    
    /**
     * 
     * @param baseIndexName : Index base name that the date will be appended to.
     * @param dateRangeUnit : The unit of the date span. Years (Y/y), Months (M/m), Weeks (W/w), and Days (D/d) are supported.
     * @param dateRangeValue : The integer number of date range units that each index will span.
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
                throw new Exception("Date range unit is invalid.");
            this.dateSpanUnit = dateSpanUnit.toUpperCase();
        }
        else
            throw new Exception("Date Span Unit is null.");
        
        if (null != dateSpanValue)
            this.dateSpanValue = dateSpanValue;
        else
            throw new Exception("Date Span Value is null.");
        
    }
    
    public String getIndexName(Instant time) throws Exception
    {
        if (null == time)
            throw new Exception("Instant is null");
        
        if (null == cutoff || time.compareTo(cutoff) > 0)
            updateIndexNameAndCutoff(time);
        
        return indexName;
    }
    
    private void updateIndexNameAndCutoff(Instant time)
    {

        Calendar calendar = getCalendarForNextDateBlock();
        currentDateBlock = calendar.getTime().toString();
        System.out.println("Next block start date: " + currentDateBlock);
        cutoff = getCutoff(calendar);
        
        indexName = baseIndexName + currentDateBlock;
    }
    
    private Calendar getCalendarForNextDateBlock()
    {
        Calendar calendar = new GregorianCalendar();
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);
        
        if (dateSpanUnit.equals("Y"))
        {
            // Roll the year back to the beginning (midnight of the first of the year).
            dateSpanField = Calendar.YEAR;
            calendar.set(Calendar.MONTH, Calendar.JANUARY);
            calendar.set(Calendar.WEEK_OF_MONTH, 1);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
        }
        if (dateSpanUnit.equals("M"))
        {
            // Roll the month back to the beginning (midnight of the first of the month).
            dateSpanField = Calendar.MONTH;
            calendar.set(Calendar.WEEK_OF_MONTH, 1);
            calendar.set(Calendar.DAY_OF_MONTH, 1);

        }
        if (dateSpanUnit.equals("W"))
        {
            // Roll the week back to the beginning (midnight Sunday).
            dateSpanField = Calendar.WEEK_OF_YEAR;    
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        }
        if (dateSpanUnit.equals("D"))
            dateSpanField = Calendar.DATE;
        
        // Roll the day back to midnight
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        calendar.add(dateSpanField, dateSpanValue);

        return calendar;
    }

    private Instant getCutoff(Calendar calendar)
    {
        calendar.add(Calendar.SECOND, -1);
        calendar.add(dateSpanField, dateSpanValue);
        System.out.println("Cutoff for next block: " + calendar.getTime().toString());
        Instant instant = calendar.toInstant();
        return instant;
    }
}
