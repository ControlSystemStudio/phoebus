/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.scan.ui.datatable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.csstudio.scan.data.ScanSample;
import org.csstudio.scan.data.ScanSampleFormatter;

import javafx.beans.property.SimpleStringProperty;

/**
 * Data for a row of the Scan Data Table
 * @author Evan Smith
 */
public class DataRow
{
    /** Data values for each row's cell. */
    private final List<SimpleStringProperty> data = new ArrayList<>();
    
    /** Timestamps for each data value in the row. */
    private final List<SimpleStringProperty> data_timestamps = new ArrayList<>();
    
    public DataRow(final Instant timestamp, final ScanSample[] samples)
    {
        /* Add the row's timestamp. */
        data.add(new SimpleStringProperty(ScanSampleFormatter.format(timestamp)));
        data_timestamps.add(new SimpleStringProperty(ScanSampleFormatter.format(timestamp)));
        
        /* Add each cell's value and each value's specific timestamp to the row. */
        for (ScanSample sample : samples)
        {
            data.add(new SimpleStringProperty(ScanSampleFormatter.asString(sample)));
            
            final String str_timestamp = (null == sample) ? null : ScanSampleFormatter.format(sample.getTimestamp());
            
            data_timestamps.add(new SimpleStringProperty(str_timestamp));
        }
    }
    
    /** Retrieve a specific column's data value for this row. */
    public SimpleStringProperty getDataValue(final int col)
    {
        return data.get(col);
    }
    
    /** Retrieve a specific column's data value timestamp for this row. */
    public SimpleStringProperty getDataTimestamp(final int col)
    {
        return data_timestamps.get(col);
    }
    
    /** Get the size of the row (i.e. the number of columns). */
    public int size()
    {
        return data.size();
    }
}
