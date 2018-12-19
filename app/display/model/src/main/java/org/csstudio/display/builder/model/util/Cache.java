/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.util;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/** Generic cache
 *
 *  <p>When fetching an entry, it will return
 *  a previously submitted entry that's still valid.
 *  If there is none, or it has expired, a new entry is created.
 *
 *  <p>Entries are checked for expiration right when they're requested.
 *  In addition, a slower timer checks for expired entries
 *  which have not been requested for a while.
 *
 *  @author Kay Kasemir
 *
 *  @param <T> Value type for cache entries
 */
@SuppressWarnings("nls")
public class Cache<T>
{
    private final Duration timeout;

    @FunctionalInterface
    public interface CreateEntry<K, V>
    {
        public V create(K key) throws Exception;
    };

    private class Entry
    {
        private final T value;
        private final Instant expire;

        public Entry(final T value)
        {
            this.value = value;
            this.expire = Instant.now().plus(timeout);
        }

        public T getValue()
        {
            return value;
        }

        public boolean isExpired(final Instant now)
        {
            return expire.isBefore(now);
        }
    };
    private final ConcurrentHashMap<String, Future<Entry>> cache = new ConcurrentHashMap<>();

    private final AtomicReference<ScheduledFuture<?>> cleanup_timer = new AtomicReference<>();

    /** @param timeout How long entries remain valid */
    public Cache(final Duration timeout)
    {
        this.timeout = timeout;
    }

    /** Get existing entry or create new one
     *  @param key Key for entry
     *  @param creator Function to create entry, if there is none in the cache
     *  @return Entry, either the newly created one or a previously cached one
     *  @throws Exception on error
     */
    public T getCachedOrNew(final String key, final CreateEntry<String, T> creator) throws Exception
    {
        // In case two concurrent callers request the same key,
        // first one will submit the future,
        // second one will just 'get' the future submitted by first one.
        final Future<Entry> future_entry = cache.computeIfAbsent(key, k ->
        {
            final Callable<Entry> create_entry = () ->
            {
                try
                {
                    return new Entry(creator.create(key));
                }
                finally
                {   // No matter if entry was created successfully,
                    // or threw an exception.
                    // Either the value or the exception will be returned
                    // but the future,
                    // it will be in the cache, so arrange for expiration
                    schedule_cleanup();
                }
            };
            return ModelThreadPool.getExecutor().submit(create_entry);
        });
        // Both concurrent threads will await future
        final Entry entry = future_entry.get();

        // Check expiration
        if (! entry.isExpired(Instant.now()))
            return entry.getValue();
        // Re-create
        // Two threads might concurrently find an expired entry.
        // Both will remove it (second one is then a NOP),
        // and then create it, where only first one calls the supplier:
        cache.remove(key);
        return getCachedOrNew(key, creator);
    }

    private void cleanup()
    {
        final Instant now = Instant.now();
        cache.forEach((key, future) ->
        {
            if (future.isDone())
            {
                final Cache<T>.Entry entry;
                try
                {
                    entry = future.get();
                }
                catch (Exception ex)
                {
                    // There was an error creating the entry,
                    // for example a file could not be opened.
                    // This was reported to the caller of getCachedOrNew.
                    // Remove from cache, so next time we'll try again.
                    logger.log(Level.FINE, "Cache expires failed entry {0}", key);
                    cache.remove(key, future);
                    return;
                }
                if (entry.isExpired(now))
                {
                    logger.log(Level.FINE, "Cache expires {0}", key);
                    cache.remove(key, future);
                }
            }
        });
    }

    /** (Re-)schedule the cleanup of expired entries */
    private void schedule_cleanup()
    {
        // Use 1.5 times the timeout for cleanup
        final ScheduledFuture<?> next = ModelThreadPool.getTimer().schedule(this::cleanup, 3*timeout.getSeconds()/2, TimeUnit.SECONDS);
        final ScheduledFuture<?> prev = cleanup_timer.getAndSet(next);
        if (prev == null)
            logger.fine("Scheduling cache cleanup");
        else
        {
            logger.fine("Re-scheduling cache cleanup");
            prev.cancel(false);
        }
    }

    /** @return Keys for current cache entries (for unit test) */
    public Collection<String> getKeys()
    {
        return cache.keySet();
    }

    /** Clear cache entries (not waiting for them to expire) */
    public void clear()
    {
        cache.clear();
    }
}
