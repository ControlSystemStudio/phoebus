/*******************************************************************************
 * Copyright (c) 2014-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.opva;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.epics.pvaccess.client.ChannelGet;
import org.epics.pvaccess.client.ChannelGetRequester;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Structure;
import org.epics.vtype.VType;

/** A {@link ChannelGetRequester} for reading a value from a {@link PVA_PV},
 *  indicating completion via a {@link Future}
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class PVGetHandler extends PVRequester implements ChannelGetRequester, Future<VType>
{
    final private PVA_PV pv;

    final private CountDownLatch updates = new CountDownLatch(1);
    private volatile VType value = null;
    private volatile Exception error = null;

    /** @param pv PV to notify of value update */
    public PVGetHandler(final PVA_PV pv)
    {
        this.pv = pv;
    }

    // ChannelGetRequester
    @Override
    public void channelGetConnect(final Status status, final ChannelGet channelGet,
            final Structure structure)
    {
        if (! status.isSuccess())
        {
            error = new Exception("Failed to connect 'get' for " + pv.getName() + ": " + status);
            updates.countDown();
        }
        else
        {
            channelGet.get();
        }
    }

    // ChannelGetRequester
    @Override
    public void getDone(final Status status, final ChannelGet channelGet, final PVStructure data, final BitSet bitSet)
    {
        if (! status.isSuccess())
            error = new Exception("Get failed for " + pv.getName() + ": " + status);
        else
        {
            try
            {
                value = pv.handleValueUpdate(data);
            }
            catch (Exception ex)
            {
                error = ex;
            }
        }
        updates.countDown();
    }

    // Future
    @Override
    public boolean cancel(final boolean mayInterruptIfRunning)
    {
        return false;
    }

    // Future
    @Override
    public boolean isCancelled()
    {
        return false;
    }

    // Future
    @Override
    public boolean isDone()
    {
        return updates.getCount() == 0;
    }

    // Future
    @Override
    public VType get() throws InterruptedException, ExecutionException
    {
        updates.await();
        if (error != null)
            throw new ExecutionException(error);
        return value;
    }

    // Future
    @Override
    public VType get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException
    {
        if (! updates.await(timeout, unit))
            throw new TimeoutException(pv.getName() + " write timeout");
        if (error != null)
            throw new ExecutionException(error);
        return value;
    }
}
