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

import org.junit.Test;

/**
 * Unit test for the IndexNameHelper class.
 * @author Evan Smith
 */
public class IndexNameHelperTest
{
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
    public void passedInstantNull()
    {
        try
        {
            IndexNameHelper inh = new IndexNameHelper("index", "w", 1);
            inh.getIndexName(null);
        }
        catch (Exception ex)
        {
            assertEquals("Passed instant is null.", ex.getMessage());
        }
    }
    
    @Test 
    public void dateInCurrentYear() throws Exception
    {
        Instant expectedSpanStart = Instant.parse("2018-01-01T00:00:00Z");
        Instant expectedSpanEnd = Instant.parse("2019-01-01T00:00:00Z");   
        Instant oldSpanTime = Instant.parse("2018-09-15T00:00:00Z");
        Instant newSpanTime = Instant.parse("2018-10-15T00:00:00Z");

        IndexNameHelper inh = new IndexNameHelper("test_index", "y", 1);
        
        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());
        
        assertEquals("test_index_2018-01-01", inh.getIndexName(oldSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());
        
        assertEquals("test_index_2018-01-01", inh.getIndexName(newSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());
    }
    
    @Test
    public void dateInNextYear() throws Exception
    {
        Instant expectedSpanStart = Instant.parse("2018-01-01T00:00:00Z");
        Instant expectedSpanEnd = Instant.parse("2019-01-01T00:00:00Z");   
        Instant oldSpanTime = Instant.parse("2018-09-13T00:00:00Z");
        Instant newSpanTime = Instant.parse("2019-09-13T00:00:00Z");

        IndexNameHelper inh = new IndexNameHelper("test_index", "y", 1);

        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());
        
        inh.getIndexName(oldSpanTime);
        
        assertEquals("test_index_2018-01-01", inh.getIndexName(oldSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());

        expectedSpanStart = Instant.parse("2019-01-01T00:00:00Z");
        expectedSpanEnd = Instant.parse("2020-01-01T00:00:00Z");
        
        assertEquals("test_index_2019-01-01", inh.getIndexName(newSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());
    }
    
    @Test
    public void dateInCurrentMonth() throws Exception
    {        
        Instant expectedSpanStart = Instant.parse("2018-09-01T00:00:00Z");
        Instant expectedSpanEnd = Instant.parse("2018-10-01T00:00:00Z");   
        Instant oldSpanTime = Instant.parse("2018-09-15T00:00:00Z");
        Instant newSpanTime = Instant.parse("2018-09-17T00:00:00Z");

        IndexNameHelper inh = new IndexNameHelper("test_index", "m", 1);
        
        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());
        
        assertEquals("test_index_2018-09-01", inh.getIndexName(oldSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());
        
        assertEquals("test_index_2018-09-01", inh.getIndexName(newSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());
    }
    
    @Test
    public void dateInNextMonth() throws Exception
    {
        Instant expectedSpanStart = Instant.parse("2018-09-01T00:00:00Z");
        Instant expectedSpanEnd = Instant.parse("2018-10-01T00:00:00Z");   
        Instant oldSpanTime = Instant.parse("2018-09-13T00:00:00Z");
        Instant newSpanTime = Instant.parse("2018-10-13T00:00:00Z");

        IndexNameHelper inh = new IndexNameHelper("test_index", "m", 1);

        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());
        
        inh.getIndexName(oldSpanTime);
        
        assertEquals("test_index_2018-09-01", inh.getIndexName(oldSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());

        expectedSpanStart = Instant.parse("2018-10-01T00:00:00Z");
        expectedSpanEnd = Instant.parse("2018-11-01T00:00:00Z");
        
        assertEquals("test_index_2018-10-01", inh.getIndexName(newSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());
    }
    
    @Test
    public void dateInCurrentWeek() throws Exception
    {        
        Instant expectedSpanStart = Instant.parse("2018-09-09T00:00:00Z");
        Instant expectedSpanEnd = Instant.parse("2018-09-16T00:00:00Z");   
        Instant oldSpanTime = Instant.parse("2018-09-13T00:00:00Z");
        Instant newSpanTime = Instant.parse("2018-09-14T00:00:00Z");

        IndexNameHelper inh = new IndexNameHelper("test_index", "w", 1);
        
        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());
        
        assertEquals("test_index_2018-09-09", inh.getIndexName(oldSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());
        
        assertEquals("test_index_2018-09-09", inh.getIndexName(newSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());
    }
    
    @Test
    public void dateInNextWeek() throws Exception
    {
        Instant expectedSpanStart = Instant.parse("2018-09-09T00:00:00Z");
        Instant expectedSpanEnd = Instant.parse("2018-09-16T00:00:00Z");   
        Instant oldSpanTime = Instant.parse("2018-09-13T00:00:00Z");
        Instant newSpanTime = Instant.parse("2018-09-18T00:00:00Z");

        IndexNameHelper inh = new IndexNameHelper("test_index", "w", 1);

        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());
        
        inh.getIndexName(oldSpanTime);
        
        assertEquals("test_index_2018-09-09", inh.getIndexName(oldSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());

        expectedSpanStart = Instant.parse("2018-09-16T00:00:00Z");
        expectedSpanEnd = Instant.parse("2018-09-23T00:00:00Z");
        
        assertEquals("test_index_2018-09-16", inh.getIndexName(newSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());
    }
    
    @Test
    public void dateInCurrentDay() throws Exception
    {        
        Instant expectedSpanStart = Instant.parse("2018-09-09T00:00:00Z");
        Instant expectedSpanEnd = Instant.parse("2018-09-10T00:00:00Z");   
        Instant oldSpanTime = Instant.parse("2018-09-09T00:01:00Z");
        Instant newSpanTime = Instant.parse("2018-09-09T00:02:00Z");

        IndexNameHelper inh = new IndexNameHelper("test_index", "d", 1);
        
        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());
        
        assertEquals("test_index_2018-09-09", inh.getIndexName(oldSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());
        
        assertEquals("test_index_2018-09-09", inh.getIndexName(newSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());
    }
    
    @Test
    public void dateInNextDay() throws Exception
    {
        Instant expectedSpanStart = Instant.parse("2018-09-13T00:00:00Z");
        Instant expectedSpanEnd = Instant.parse("2018-09-14T00:00:00Z");   
        Instant oldSpanTime = Instant.parse("2018-09-13T00:00:00Z");
        Instant newSpanTime = Instant.parse("2018-09-14T00:00:01Z");

        IndexNameHelper inh = new IndexNameHelper("test_index", "d", 1);
        
        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());
        
        inh.getIndexName(oldSpanTime);
        
        assertEquals("test_index_2018-09-13", inh.getIndexName(oldSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());
        
        expectedSpanStart = Instant.parse("2018-09-14T00:00:00Z");
        expectedSpanEnd = Instant.parse("2018-09-15T00:00:00Z");
        
        assertEquals("test_index_2018-09-14", inh.getIndexName(newSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());
    }
}
