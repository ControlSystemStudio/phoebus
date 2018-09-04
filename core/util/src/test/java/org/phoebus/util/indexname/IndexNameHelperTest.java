/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.util.indexname;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for the IndexNameHelper class.
 * @author Evan Smith
 */
@SuppressWarnings("nls")
public class IndexNameHelperTest
{
    @BeforeClass
    public static void setup()
    {
        // Perform test in known timezone and Locale
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        Locale.setDefault(Locale.ROOT);
    }

    @Test
    public void baseIndexNameNull()
    {
        try
        {
            new IndexNameHelper(null, null, null);
        }
        catch (Exception ex)
        {
            assertEquals("Base Index Name is null.", ex.getMessage());
        }
    }

    @Test
    public void dateSpanUnitNull()
    {
        try
        {
            new IndexNameHelper("index", null, 10);
        }
        catch (Exception ex)
        {
            assertEquals("Date Span Unit is null.", ex.getMessage());
        }
    }

    @Test
    public void dateSpanUnitInvalid()
    {
        try
        {
            new IndexNameHelper("index", "Q", 5);
        }
        catch (Exception ex)
        {
            assertEquals("Date Span Unit is invalid.", ex.getMessage());
        }
    }

    @Test
    public void dateSpanValueNull()
    {
        try
        {
            new IndexNameHelper("index", "y", null);
        }
        catch (Exception ex)
        {
            assertEquals("Date Span Value is null.", ex.getMessage());
        }
    }

    @Test
    public void dateSpanValueZero() throws Exception
    {
        IndexNameHelper inh = new IndexNameHelper("test_index", "y", 0);
        String indexName = inh.getIndexName(Instant.now());

        assertEquals("test_index", indexName);
    }

    @Test
    public void dateSpanValueLessThanZero() throws Exception
    {
        IndexNameHelper inh = new IndexNameHelper("test_index", "y", -5);
        String indexName = inh.getIndexName(Instant.now());

        assertEquals("test_index", indexName);
    }

    @Test
    public void dateInCurrentYear() throws Exception
    {
        LocalDateTime expectedSpanStart = LocalDateTime.of(2018, 1, 1, 0, 0, 0);
        LocalDateTime expectedSpanEnd = LocalDateTime.of(2019, 1, 1, 0, 0, 0);
        LocalDateTime oldSpanTime = LocalDateTime.of(2018, 9, 15, 0, 0, 0);
        LocalDateTime newSpanTime = LocalDateTime.of(2018, 10, 15, 0, 0, 0);

        expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant();

        IndexNameHelper inh = new IndexNameHelper("test_index", "y", 1);

        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());

        assertEquals("test_index_2018-01-01", inh.getIndexName(oldSpanTime.atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanEnd());

        assertEquals("test_index_2018-01-01", inh.getIndexName(newSpanTime.atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanEnd());
    }

    @Test
    public void dateInNextYear() throws Exception
    {
        LocalDateTime expectedSpanStart = LocalDateTime.of(2018, 1, 1, 0, 0, 0);
        LocalDateTime expectedSpanEnd = LocalDateTime.of(2019, 1, 1, 0, 0, 0);
        LocalDateTime oldSpanTime = LocalDateTime.of(2018, 9, 13, 0, 0, 0);
        LocalDateTime newSpanTime = LocalDateTime.of(2019, 1, 1, 0, 0, 1);

        IndexNameHelper inh = new IndexNameHelper("test_index", "y", 1);

        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());

        inh.getIndexName(oldSpanTime.atZone(ZoneId.systemDefault()).toInstant());

        assertEquals("test_index_2018-01-01", inh.getIndexName(oldSpanTime.atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanEnd());

        expectedSpanStart = LocalDateTime.of(2019, 1, 1, 0, 0, 0);
        expectedSpanEnd = LocalDateTime.of(2020, 1, 1, 0, 0, 0);

        assertEquals("test_index_2019-01-01", inh.getIndexName(newSpanTime.atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanEnd());
    }

    @Test
    public void dateInCurrentMonth() throws Exception
    {
        LocalDateTime expectedSpanStart = LocalDateTime.of(2018, 9, 1, 0, 0, 0);
        LocalDateTime expectedSpanEnd = LocalDateTime.of(2018, 10, 1, 0, 0, 0);
        LocalDateTime oldSpanTime = LocalDateTime.of(2018, 9, 15, 0, 0, 0);
        LocalDateTime newSpanTime = LocalDateTime.of(2018, 9, 17, 0, 0, 0);

        IndexNameHelper inh = new IndexNameHelper("test_index", "m", 1);

        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());

        assertEquals("test_index_2018-09-01", inh.getIndexName(oldSpanTime.atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanEnd());

        assertEquals("test_index_2018-09-01", inh.getIndexName(newSpanTime.atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanEnd());
    }

    @Test
    public void dateInNextMonth() throws Exception
    {
        LocalDateTime expectedSpanStart = LocalDateTime.of(2018, 9, 1, 0, 0, 0);
        LocalDateTime expectedSpanEnd = LocalDateTime.of(2018, 10, 1, 0, 0, 0);
        LocalDateTime oldSpanTime = LocalDateTime.of(2018, 9, 13, 0, 0, 0);
        LocalDateTime newSpanTime = LocalDateTime.of(2018, 10, 1, 0, 0, 1);

        IndexNameHelper inh = new IndexNameHelper("test_index", "m", 1);

        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());

        inh.getIndexName(oldSpanTime.atZone(ZoneId.systemDefault()).toInstant());

        assertEquals("test_index_2018-09-01", inh.getIndexName(oldSpanTime.atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanEnd());

        expectedSpanStart = LocalDateTime.of(2018, 10, 1, 0, 0, 0);
        expectedSpanEnd = LocalDateTime.of(2018, 11, 1, 0, 0, 0);

        assertEquals("test_index_2018-10-01", inh.getIndexName(newSpanTime.atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanEnd());
    }

    @Test
    public void dateInCurrentWeek() throws Exception
    {
        LocalDateTime expectedSpanStart = LocalDateTime.of(2018, 9, 9, 0, 0, 0);
        LocalDateTime expectedSpanEnd = LocalDateTime.of(2018, 9, 16, 0, 0, 0);
        LocalDateTime oldSpanTime = LocalDateTime.of(2018, 9, 13, 0, 0, 0);
        LocalDateTime newSpanTime = LocalDateTime.of(2018, 9, 14, 0, 0, 0);

        IndexNameHelper inh = new IndexNameHelper("test_index", "w", 1);

        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());

        assertEquals("test_index_2018-09-09", inh.getIndexName(oldSpanTime.atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanEnd());

        assertEquals("test_index_2018-09-09", inh.getIndexName(newSpanTime.atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanEnd());
    }

    @Test
    public void dateInNextWeek() throws Exception
    {
        LocalDateTime expectedSpanStart = LocalDateTime.of(2018, 9, 9, 0, 0, 0);
        LocalDateTime expectedSpanEnd = LocalDateTime.of(2018, 9, 16, 0, 0, 0);
        LocalDateTime oldSpanTime = LocalDateTime.of(2018, 9, 13, 0, 0, 0);
        LocalDateTime newSpanTime = LocalDateTime.of(2018, 9, 16, 0, 0, 1);

        IndexNameHelper inh = new IndexNameHelper("test_index", "w", 1);

        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());

        inh.getIndexName(oldSpanTime.atZone(ZoneId.systemDefault()).toInstant());

        assertEquals("test_index_2018-09-09", inh.getIndexName(oldSpanTime.atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanEnd());

        expectedSpanStart = LocalDateTime.of(2018, 9, 16, 0, 0, 0);
        expectedSpanEnd = LocalDateTime.of(2018, 9, 23, 0, 0, 0);

        assertEquals("test_index_2018-09-16", inh.getIndexName(newSpanTime.atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanEnd());
    }

    @Test
    public void dateInCurrentDay() throws Exception
    {
        LocalDateTime expectedSpanStart = LocalDateTime.of(2018, 9, 9, 0, 0, 0);
        LocalDateTime expectedSpanEnd = LocalDateTime.of(2018, 9, 10, 0, 0, 0);
        LocalDateTime oldSpanTime = LocalDateTime.of(2018, 9, 9, 0, 1, 0);
        LocalDateTime newSpanTime = LocalDateTime.of(2018, 9, 9, 0, 2, 0);

        IndexNameHelper inh = new IndexNameHelper("test_index", "d", 1);

        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());

        assertEquals("test_index_2018-09-09", inh.getIndexName(oldSpanTime.atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanEnd());

        assertEquals("test_index_2018-09-09", inh.getIndexName(newSpanTime.atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanEnd());
    }

    @Test
    public void dateInNextDay() throws Exception
    {
        LocalDateTime expectedSpanStart = LocalDateTime.of(2018, 9, 13, 0, 0, 0);
        LocalDateTime expectedSpanEnd = LocalDateTime.of(2018, 9, 14, 0, 0, 0);
        LocalDateTime oldSpanTime = LocalDateTime.of(2018, 9, 13, 0, 0, 0);
        LocalDateTime newSpanTime = LocalDateTime.of(2018, 9, 14, 0, 0, 1);

        IndexNameHelper inh = new IndexNameHelper("test_index", "d", 1);

        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());

        inh.getIndexName(oldSpanTime.atZone(ZoneId.systemDefault()).toInstant());

        assertEquals("test_index_2018-09-13", inh.getIndexName(oldSpanTime.atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanEnd());

        expectedSpanStart = LocalDateTime.of(2018, 9, 14, 0, 0, 0);
        expectedSpanEnd = LocalDateTime.of(2018, 9, 15, 0, 0, 0);

        assertEquals("test_index_2018-09-14", inh.getIndexName(newSpanTime.atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(expectedSpanStart.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd.atZone(ZoneId.systemDefault()).toInstant(), inh.getCurrentDateSpanEnd());
    }
}
