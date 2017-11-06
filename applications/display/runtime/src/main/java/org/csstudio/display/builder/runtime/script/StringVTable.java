/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.script;

import java.util.List;

import org.csstudio.display.builder.model.util.VTypeUtil;
import org.phoebus.vtype.VTable;

/** VTable for Strings
 *  @author Kay Kasemir
 */
/* Only visible to package */
class StringVTable implements VTable
{
    private final List<String> headers;
    private final List<List<String>> columns;

    /** @param headers Table headers
     *  @param columns List of columns, i.e. columns.get(N) is the Nth column
     */
    public StringVTable(final List<String> headers, final List<List<String>> columns)
    {
        this.headers = headers;
        this.columns = columns;
    }

    @Override
    public int getColumnCount()
    {
        return headers.size();
    }

    @Override
    public Class<?> getColumnType(final int column)
    {
        return String.class;
    }

    @Override
    public String getColumnName(final int column)
    {
        return headers.get(column);
    }

    @Override
    public Object getColumnData(final int column)
    {
        return columns.get(column);
    }

    @Override
    public int getRowCount()
    {
        return columns.isEmpty() ? 0 : columns.get(0).size();
    }

    @Override
    public String toString()
    {
        return VTypeUtil.getValueString(this, false);
   }
}