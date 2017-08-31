/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

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

    final private ConcurrentHashMap<K, ReferencedEntry<E>> map = new ConcurrentHashMap<>();

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
        final ReferencedEntry<E> ref_entry = map.computeIfAbsent(key, k -> new ReferencedEntry<E>(creator.get()));
        ref_entry.addRef();
        return ref_entry;
    }

    /** Release an item from the map
     *  @param key Key for item to release
     *  @return Remaining reference counts. 0 if item has been removed from map.
     */
    public int release(final K key)
    {
        // System.out.println("Release " + key + " in " + map);
        final ReferencedEntry<E> updated_entry = map.compute(key, (the_key, entry) ->
        {
            if (entry == null)
                throw new IllegalStateException("No reference found for " + key);
            if (entry.decRef() <= 0)
                return null;
            return entry;
        });

        return updated_entry == null ? 0 : updated_entry.getReferences();
    }

    /** @return Entries in map */
    public Collection<ReferencedEntry<E>> getEntries()
    {
        return Collections.unmodifiableCollection(map.values());
    }
}
