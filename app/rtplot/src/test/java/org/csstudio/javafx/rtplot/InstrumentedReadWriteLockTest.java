/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;

import org.csstudio.javafx.rtplot.data.InstrumentedReadWriteLock;
import org.junit.Test;

/** JUnit test of the {@link InstrumentedReadWriteLock}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class InstrumentedReadWriteLockTest
{
    final private ReadWriteLock lock = new InstrumentedReadWriteLock();
    private volatile boolean writer_has_lock = false;

    @Test
    public void testReadWriteLock() throws Exception
    {
        lock.readLock().lock();

        final Thread writer = new Thread(() ->
        {
            lock.writeLock().lock();
            System.out.println("Locked for writing");
            writer_has_lock = true;
            synchronized (this)
            {
                try
                {
                    this.wait();
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        }, "Writer");

        // Allow writer to start
        writer.start();
        TimeUnit.SECONDS.sleep(2);

        // Writer should be pending because we have the lock
        String info = lock.toString();
        System.out.println(info);
        assertThat(writer_has_lock, equalTo(false));
        assertThat(info, containsString("no owner"));
        assertThat(info, containsString("pending writers (" + writer.toString()));

        // We can get another read lock
        assertThat(lock.readLock().tryLock(), equalTo(true));

        // Release our read locks
        lock.readLock().unlock();
        lock.readLock().unlock();

        // Now writer should be able to get the lock
        TimeUnit.SECONDS.sleep(2);

        info = lock.toString();
        System.out.println(info);
        assertThat(writer_has_lock, equalTo(true));

        // Can no longer obtain the read lock
        assertThat(lock.readLock().tryLock(), equalTo(false));
        assertThat(info, containsString("owned by " + writer.toString()));
    }
}
