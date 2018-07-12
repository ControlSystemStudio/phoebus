/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import org.phoebus.core.types.ProcessVariable;


/** Archive search result, information about one channel
 *  @author Kay Kasemir
 */
public class ChannelInfo extends ProcessVariable
{
    /** Default ID for {@link Serializable} */
    private static final long serialVersionUID = 1L;

    final private ArchiveDataSource archive;

    /** Initialize
     *  @param archive IArchiveDataSource for channel
     *  @param name    Channel name
     */
    public ChannelInfo(final String name, final ArchiveDataSource archive)
    {
        super(name);
        this.archive = archive;
    }

    /** @return ArchiveDataSource */
    public ArchiveDataSource getArchiveDataSource()
    {
        return archive;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof ChannelInfo))
            return false;
        final ChannelInfo other = (ChannelInfo) obj;
        return other.getName().equals(getName())                 &&
               other.getArchiveDataSource().equals(archive);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = getName().hashCode();
        result = prime * result + archive.hashCode();
        return result;
    }

    /** @return String representation for debugging */
    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return getName() + " [" + archive.getName() + "]";
    }
}
