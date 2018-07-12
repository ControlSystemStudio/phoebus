/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.util;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

/** JUnit test of {@link Cache}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CacheTest
{
    private Logger logger;

    @Before
    public void setup()
    {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tH:%1$tM:%1$tS %4$s: %5$s %n");
        logger = Logger.getLogger("");
        logger.setLevel(Level.FINE);
        for (Handler handler : logger.getHandlers())
            handler.setLevel(Level.FINE);
    }

    private String createEntry(final String key) throws Exception
    {
        final String value = "Entry for " + key;
        logger.fine(">> Creating " + value + " ...");
        if (key.equals("A"))
            TimeUnit.SECONDS.sleep(2);
        logger.fine("<< Returned " + value);
        return value;
    }

    @Test
    public void testCache() throws Exception
    {
        final Cache<String> cache = new Cache<>(Duration.ofSeconds(2));

        final CountDownLatch set_A = new CountDownLatch(1);
        final AtomicReference<String> A = new AtomicReference<>();
        final ExecutorService pool = Executors.newCachedThreadPool();
        pool.submit(() ->
        {
            final String key = "A";
            logger.fine("> Requesting " + key + " for 1st time ...");
            A.set(cache.getCachedOrNew(key, this::createEntry));
            set_A.countDown();
            logger.fine("< Got initial" + key);
            return null;
        });
        pool.submit(() ->
        {
            final String key = "B";
            logger.fine("> Requesting " + key + "...");
            cache.getCachedOrNew(key, this::createEntry);
            logger.fine("< Got " + key);
            return null;
        });
        pool.submit(() ->
        {
            final String key = "A";
            logger.fine("> Requesting " + key + " again (cached)...");
            cache.getCachedOrNew(key, this::createEntry);
            logger.fine("< Got cached " + key);
            return null;
        });

        String A2 = cache.getCachedOrNew("A", this::createEntry);
        assertThat(A2, equalTo("Entry for A"));
        // First submitted thread may have triggered creating the "A" entry,
        // but not set A, yet, so wait for that to avoid A.get() == null.
        set_A.await();
        assertThat(A2, sameInstance(A.get()));

        logger.fine("Allowing to expire");
        TimeUnit.SECONDS.sleep(3);
        A2 = cache.getCachedOrNew("A", this::createEntry);
        assertThat(A2, not(sameInstance(A.get())));

        logger.fine("Waiting for cache cleanup");
        TimeUnit.SECONDS.sleep(6);

        final Collection<String> keys = cache.getKeys();
        logger.fine("Remaining entries: " + keys);
        assertThat(keys.size(), equalTo(0));
    }


    @Test
    public void testEntryCreationError() throws Exception
    {
        final Cache<String> cache = new Cache<>(Duration.ofSeconds(2));

        try
        {
            cache.getCachedOrNew("Broken", k ->
            {
                throw new Exception("Dummy error creating " + k);
            });
            fail("Expected to get exception");
        }
        catch (ExecutionException ex)
        {
            assertThat(ex.getMessage(), containsString("Dummy error"));
        }

        // Entry _is_ cached
        Collection<String> keys = cache.getKeys();
        logger.fine("Remaining entries: " + keys);
        assertThat(keys.size(), equalTo(1));

        // Fetching it again will provide the same original exception
        try
        {
            cache.getCachedOrNew("Broken", k ->
            {
                throw new Exception("SHOULD NOT CREATE NEW ENTRY!");
            });
            fail("Expected to get exception");
        }
        catch (ExecutionException ex)
        {
            assertThat(ex.getMessage(), containsString("Dummy error"));
        }

        logger.fine("Waiting for cache cleanup");
        TimeUnit.SECONDS.sleep(6);

        // Entry should expire
        keys = cache.getKeys();
        logger.fine("Remaining entries: " + keys);
        assertThat(keys.size(), equalTo(0));
    }
}
