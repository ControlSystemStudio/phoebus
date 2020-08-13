/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import static org.phoebus.pv.PV.logger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;

/** Map that keeps reference count for its objects
 *
 *  <p>Thread-safe.
 *
 *  @param <K> Key data type
 *  @param <E> Entry data type
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RefCountMap<K, E>
{
    /** Wrapper for an entry with reference count */
    public static class ReferencedEntry<E>
    {
        private final E entry;
        private final AtomicInteger references = new AtomicInteger(0);

        private ReferencedEntry(final E entry)
        {
            this.entry = Objects.requireNonNull(entry);
        }

        /** @return Item */
        public E getEntry()
        {
            return entry;
        }

        /** @return Reference count for the item */
        public int getReferences()
        {
            return references.get();
        }

        private int addRef()
        {
            return references.incrementAndGet();
        }

        private int decRef()
        {
            return references.decrementAndGet();
        }

        @Override
        public String toString()
        {
            return entry + " (" + references + " references)";
        }
    }

    // Started with ConcurrentHashMap and computeIfAbsent() in createOrGet().
    // But the computeIfAbsent() mapping function must not itself update the map,
    // which can happen with a formula PV which references and thus creates
    // other PVs.
    // With plain synchronization, adding a formula takes the synchronization lock,
    // and while it is held, recursive additions from the same thread are possible.
    //
    // SYNC on access.
    final private Map<K, ReferencedEntry<E>> map = new HashMap<>();

    /** Get or create item
     *
     *  <p>If item already exists, add reference.
     *  Otherwise create new item with initial reference count of 1.
     *
     *  @param key Item key
     *  @param entry The item to add
     *  @param creator Function that will be called atomically for new items
     *  @return reference count
     */
    public ReferencedEntry<E> createOrGet(final K key, final Supplier<E> creator)
    {
        try
        {
            ReferencedEntry<E> ref_entry;
            synchronized (map)
            {
                ref_entry = map.get(key);
                if (ref_entry == null)
                {
                    ref_entry = new ReferencedEntry<>(creator.get());
                    map.put(key, ref_entry);
                }
            }
            ref_entry.addRef();
            return ref_entry;
        }
        catch (Throwable ex)
        {
            // Show PV name to help debug errors
            throw new RuntimeException("Error for PV " + key, ex);
        }
    }

    /** Release an item from the map
     *  @param key Key for item to release
     *  @return Remaining reference counts. 0 if item has been removed from map.
     */
    public int release(final K key)
    {
        // System.out.println("Release " + key + " in " + map);
        synchronized (map)
        {
            final ReferencedEntry<E> entry = map.get(key);
            if (entry == null)
            {
                logger.log(Level.WARNING, "No reference found for " + key, new Exception("Call stack"));
                return 0;
            }

            final int refs = entry.decRef();
            if (refs <= 0)
            {   // No more references
                map.remove(key);
                return 0;
            }
            return refs;
        }
    }

    /** @return Entries in map */
    public Collection<ReferencedEntry<E>> getEntries()
    {
        synchronized (map)
        {
            return Collections.unmodifiableCollection(map.values());
        }
    }
}
