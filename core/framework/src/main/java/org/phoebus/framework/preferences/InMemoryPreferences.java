/*******************************************************************************
 * Copyright (c) 2024 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.preferences;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** Java Preferences that are held in memory, not persisted
 *  @author Kay Kasemir
 */
class InMemoryPreferences extends AbstractPreferences
{
    /** Preferences for both "user" and "system" */
    private static final InMemoryPreferences prefs = new InMemoryPreferences(null, "");
    
    /** Settings for this node in the preferences hierarchy */
    private Map<String, String> cache = new HashMap<>();

    // Javadoc for all ..Spi calls includes
    // "This method is invoked with the lock on this node held."
    // so no need for ConcurrentHashMap or our own locking
    
    /** @return User preferences */
    public static Preferences getUserRoot()
    {
        return prefs;
    }

    /** @return System preferences */
    public static Preferences getSystemRoot()
    {
        return prefs;
    }
    
    InMemoryPreferences(final InMemoryPreferences parent, final String name)
    {
        super(parent, name);
    }
    
    /** @inheritDoc */
    @Override
    protected void putSpi(String key, String value)
    {
        cache.put(key, value);
    }

    /** @inheritDoc */
    @Override
    protected String getSpi(String key)
    {
        return cache.get(key);
    }

    /** @inheritDoc */
    @Override
    protected void removeSpi(String key)
    {
        cache.remove(key);        
    }

    /** @inheritDoc */
    @Override
    protected void removeNodeSpi() throws BackingStoreException
    {
        // Nothing to remove in the file system
        cache.clear();
    }

    /** @inheritDoc */
    @Override
    protected String[] keysSpi() throws BackingStoreException
    {
        return cache.keySet().toArray(new String[cache.size()]);        
    }

    /** @inheritDoc */
    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException
    {
        // This method need not return the names of any nodes already cached
        // by the AbstractPreferences
        return new String[0];
    }

    /** @inheritDoc */
    @Override
    protected AbstractPreferences childSpi(String name)
    {
        // AbstractPreferences guaranteed that the named node has not been returned
        // by a previous invocation of this method or {@link #getChild(String)},
        // so only called once and can then create the one and only new child
        return new InMemoryPreferences(this, name);
    }

    /** @inheritDoc */
    @Override
    protected void syncSpi() throws BackingStoreException
    {
        // Nothing to sync
    }

    /** @inheritDoc */
    @Override
    protected void flushSpi() throws BackingStoreException
    {
        // Nothing to sync
    }
}
