/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import java.util.BitSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.epics.pva.client.ClientChannelState;
import org.epics.pva.client.PVAChannel;
import org.epics.pva.data.PVAStructure;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;

/** PV Access {@link PV}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVA_PV extends PV
{
    private final PVAChannel channel;
    final PVNameHelper name_helper;

    public PVA_PV(final String name, final String base_name) throws Exception
    {
        super(name);

        // Analyze base_name, determine channel and request
        name_helper = PVNameHelper.forName(base_name);
        logger.log(Level.FINE, () -> "PVA '" + base_name + "' -> " + name_helper);
        channel = PVA_Context.getInstance().getClient().getChannel(name_helper.getChannel(), this::channelStateChanged);
    }

    private void channelStateChanged(final PVAChannel channel, final ClientChannelState state)
    {
        if (state == ClientChannelState.CONNECTED)
        {   // When connected, subscribe to updates
            try
            {
                channel.subscribe(name_helper.getReadRequest(), this::handleMonitor);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot subscribe to " + channel, ex);
            }
        }
        else if (! isDisconnected(super.read()))
        {
            // Was connected, so now disconnected
            notifyListenersOfDisconnect();
        }
    }

    private void handleMonitor(final PVAChannel channel,
                               final BitSet changes,
                               final BitSet overruns,
                               final PVAStructure data)
    {
        try
        {
            final VType value = PVAStructureHelper.getVType(data, name_helper);
            notifyListenersOfValue(value);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot decode " + channel + " = " + data, ex);
        }
    }

    @Override
    public Future<VType> asyncRead() throws Exception
    {
        final Future<PVAStructure> data = channel.read(name_helper.getReadRequest());
        // Wrap into Future that converts PVAStructure into VType
        return new Future<>()
        {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning)
            {
                return data.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled()
            {
                return data.isCancelled();
            }

            @Override
            public boolean isDone()
            {
                return data.isDone();
            }

            @Override
            public VType get() throws InterruptedException, ExecutionException
            {
                try
                {
                    return PVAStructureHelper.getVType(data.get(), name_helper);
                }
                catch (InterruptedException ex)
                {
                    throw ex;
                }
                catch (Exception ex)
                {
                    throw new ExecutionException(ex);
                }
            }

            @Override
            public VType get(final long timeout, final TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException
            {
                try
                {
                    return PVAStructureHelper.getVType(data.get(timeout, unit), name_helper);
                }
                catch (InterruptedException ex)
                {
                    throw ex;
                }
                catch (TimeoutException ex)
                {
                    throw ex;
                }
                catch (Exception ex)
                {
                    throw new ExecutionException(ex);
                }
            }
        };
    }

    @Override
    public void write(final Object new_value) throws Exception
    {
        channel.write(name_helper.getWriteRequest(), new_value);
    }

    @Override
    public Future<?> asyncWrite(final Object new_value) throws Exception
    {
        return channel.write(name_helper.getWriteRequest(), new_value);
    }

    @Override
    protected void close()
    {
        channel.close();
    }
}
