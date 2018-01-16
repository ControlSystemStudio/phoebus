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

    /** Key of the archive under the url. */
    final private int key;

    /** Archive name. */
    final private String name;

    /** Initialize
     *  @param url Data server URL.
     *  @param key Archive key.
     *  @param name Archive name.
     */
    public ArchiveDataSource(final String url, final int key, final String name)
    {
        this.url = url;
        this.key = key;
        this.name = name;
    }

    /** @return URL of the archive data server. */
    public final String getUrl()
    {
        return url;
    }

    /** @return Key of the archive under the url.
     *  @deprecated Remove the Key. Only used by Channel Archiver, include in the URL
     */
    @Deprecated
    public final int getKey()
    {
        return key;
    }

    /** @return Archive name, derived from key. */
    public String getName()
    {
        return name;
    }

    /** Compare ArchiveDataSource by URL and key, ignoring the name
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
        return key == other.key && url.equals(other.url);
    }

    /** Hash on URL and key, ignoring the description
     *  {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = prime + key;
        result = prime * result + url.hashCode();
        return result;
    }

    /** Debug string representation */
    @SuppressWarnings("nls")
    @Override
    public final String toString()
    {
        return "Archive '" + url + "' (" + key + ", '" + getName() + "')";
    }
}
