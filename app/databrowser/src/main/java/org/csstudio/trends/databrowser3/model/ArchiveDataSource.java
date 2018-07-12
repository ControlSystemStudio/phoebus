/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import java.io.Serializable;

/** Archive data source
 *  @author Kay Kasemir
 */
public class ArchiveDataSource implements Serializable
{
    /** Default ID for {@link Serializable} */
    private static final long serialVersionUID = 1L;

    /** URL of the archive data server. */
    final private String url;

    /** Archive name. */
    final private String name;

    /** Initialize
     *  @param url Data server URL.
     *  @param name Archive name.
     */
    public ArchiveDataSource(final String url, final String name)
    {
        this.url = url;
        this.name = name;
    }

    /** @return URL of the archive data server. */
    public final String getUrl()
    {
        return url;
    }

    /** @return Archive name, derived from key. */
    public String getName()
    {
        return name;
    }

    /** Compare ArchiveDataSource by URL, ignoring the name
     *  {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof ArchiveDataSource))
            return false;
        if (obj == this)
            return true;
        final ArchiveDataSource other = (ArchiveDataSource) obj;
        return url.equals(other.url);
    }

    /** Hash on URL, ignoring the description
     *  {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return url.hashCode();
    }

    /** Debug string representation */
    @SuppressWarnings("nls")
    @Override
    public final String toString()
    {
        return "Archive '" + url + "' (" + getName() + "')";
    }
}
