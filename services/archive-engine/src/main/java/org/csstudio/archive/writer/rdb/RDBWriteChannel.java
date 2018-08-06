/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.writer.rdb;

import org.csstudio.archive.writer.WriteChannel;

/** Channel information for channel in RDB
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RDBWriteChannel implements WriteChannel
{
    final private String name;
    final private int id;
    private Object meta = null;

    /** Initialize
     *  @param name Channel name
     *  @param id Channel ID in RDB
     */
    public RDBWriteChannel(final String name, final int id)
    {
        this.name = name;
        this.id = id;
    }

    /** {@inheritDoc} */
    @Override
    public String getName()
    {
        return name;
    }

    /** @return RDB ID of channel */
    public int getId()
    {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "RDBWriteChannel '" + name + "' (" + id + ")";
    }

    /** @return Meta data or <code>null</code> */
    public Object getMetadata()
    {
        return meta;
    }

    /** @param meta Current meta data of channel */
    public void setMetaData(final Object meta)
    {
        this.meta = meta;
    }
}
