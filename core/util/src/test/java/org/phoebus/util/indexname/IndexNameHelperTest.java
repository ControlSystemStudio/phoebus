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
    public void dateRangeUnitNull()
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
    public void dateRangeValueNull()
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
    public void dateInCurrentMonth() throws Exception
    {        
        Instant testTime = Instant.parse("2018-09-15T00:00:00Z");
        Instant expectedSpanStart = Instant.parse("2018-09-01T00:00:00Z");
        Instant expectedSpanEnd = Instant.parse("2018-10-01T00:00:00Z");
        
        IndexNameHelper inh = new IndexNameHelper("test_index", "m", 1);
        
        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());
        
        assertEquals("test_index_2018-09-01", inh.getIndexName(testTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());
    }
    
    @Test
    public void dateInNextMonth() throws Exception
    {
        IndexNameHelper inh = new IndexNameHelper("test_index", "m", 1);
        
        assertNull(inh.getCurrentDateSpanStart());
        assertNull(inh.getCurrentDateSpanEnd());
        
        Instant oldSpanTime = Instant.parse("2018-09-15T00:00:00Z");
        inh.getIndexName(oldSpanTime);
        
        Instant expectedSpanStart = Instant.parse("2018-09-01T00:00:00Z");
        Instant expectedSpanEnd = Instant.parse("2018-10-01T00:00:00Z");   
        
        Instant newSpanTime = Instant.parse("2018-10-15T00:00:00Z");
     
        expectedSpanStart = Instant.parse("2018-10-01T00:00:00Z");
        expectedSpanEnd = Instant.parse("2018-11-01T00:00:00Z");
        
        assertEquals("test_index_2018-10-01", inh.getIndexName(newSpanTime));
        assertEquals(expectedSpanStart, inh.getCurrentDateSpanStart());
        assertEquals(expectedSpanEnd, inh.getCurrentDateSpanEnd());
    }
}
