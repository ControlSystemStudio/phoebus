/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.util.indexname;

import static org.junit.Assert.assertEquals;

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
    public void dateBlock()
    {
        try
        {
            IndexNameHelper inh = new IndexNameHelper("", "w", 1);
            System.out.println(inh.getIndexName(Instant.now()));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
