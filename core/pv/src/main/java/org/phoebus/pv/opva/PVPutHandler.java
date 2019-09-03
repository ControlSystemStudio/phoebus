/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.opva;

import static org.phoebus.pv.PV.logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.epics.pvaccess.client.ChannelPut;
import org.epics.pvaccess.client.ChannelPutRequester;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Structure;
import org.phoebus.pv.PV;

/** A {@link ChannelPutRequester} for writing a value to a {@link PVA_PV},
 *  indicating completion via a {@link Future}
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class PVPutHandler extends PVRequester implements ChannelPutRequester, Future<Object>
{
    final private PV pv;
    final private Object new_value;

    final private CountDownLatch updates = new CountDownLatch(1);
    private volatile Exception error = null;

    /** @param pv PV to write
     *  @param new_value Value to write
     */
    public PVPutHandler(final PV pv, final Object new_value)
    {
        this.pv = pv;
        this.new_value = new_value;
    }

    // ChannelPutRequester
    @Override
    public void channelPutConnect(final Status status, final ChannelPut channelPut, final Structure structure)
    {
        if (! status.isSuccess())
        {
            error = new Exception("Failed to connect 'put' for " + pv.getName() + ": " + status);
            updates.countDown();
            return;
        }

        try
        {
            final PVStructure write_structure = PVDataFactory.getPVDataCreate().createPVStructure(structure);
            final BitSet bit_set = new BitSet(write_structure.getNumberFields());

            // Locate the 'value' field
            PVField field = write_structure.getSubField("value");
            if (field == null)
                throw new Exception("Cannot locate 'value' to write in " + structure);

            // Enumerated? Write to value.index
            if (field instanceof PVStructure  &&  "enum_t".equals(field.getField().getID()))
                field = ((PVStructure)field).getSubField("index");

            // Indicate what's changed & change it
            bit_set.set(field.getFieldOffset());
            PVStructureHelper.setField(field, new_value);

            // Perform write
            channelPut.put(write_structure, bit_set);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Failed to write " + pv.getName() + " = " + new_value, ex);
            error = new Exception("Failed to write " + pv.getName() + " = " + new_value, ex);
            updates.countDown();
        }
    }

    // ChannelPutRequester
    @Override
    public void putDone(final Status status, final ChannelPut channelPut)
    {
        if (status.isSuccess())
            logger.log(Level.FINE, "Write {0} = {1} completed",
                    new Object[] { pv.getName(), new_value });
        else
        {
            error = new Exception("Write " + pv.getName() + " = " + new_value + " failed, " + status.toString());
            logger.log(Level.WARNING, "", error);
        }
        updates.countDown();
        channelPut.destroy();
    }

    // ChannelPutRequester
    @Override
    public void getDone(final Status status, final ChannelPut channelPut, final PVStructure pvStructure, final BitSet bitSet)
    {
        // Only used for createChannelPutGet
        logger.log(Level.WARNING, "Unexpected call to ChannelPutRequester.getDone(), channel {0}", channelPut.getChannel().getChannelName());
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
    public Object get() throws InterruptedException, ExecutionException
    {
        updates.await();
        if (error != null)
            throw new ExecutionException(error);
        return null;
    }

    // Future
    @Override
    public Object get(final long timeout, final TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException
    {
        if (! updates.await(timeout, unit))
            throw new TimeoutException(pv.getName() + " write timeout");
        if (error != null)
            throw new ExecutionException(error);
        return null;
    }
}
