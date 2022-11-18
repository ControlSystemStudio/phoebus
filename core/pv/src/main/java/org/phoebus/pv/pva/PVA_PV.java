/*******************************************************************************
 * Copyright (c) 2019-2022 Oak Ridge National Laboratory.
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
import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.VString;
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

    /** @param name Full PV name with prefix and initializer
     *  @param base_name Base name
     *  @throws Exception on error
     */
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
                channel.subscribe(name_helper.getRequest(), this::handleMonitor);
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
        if (data == null)
        {
            // The PVA protocol allows the server to 'destroy' a monitor.
            // This higher-level client library aims to establish only
            // one subscription per PV, and when the server cancels it,
            // all we can do is indicate this similar to a disconnect,
            // since the client won't receive any more data,
            // with a log message that explains what happened.
            logger.log(Level.WARNING, "Server ends subscription for " + this);
            notifyListenersOfDisconnect();
        }
        else
            try
            {
                final VType value = PVAStructureHelper.getVType(data, name_helper);
                notifyListenersOfValue(value);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot decode " + channel + " = " + data, ex);
                notifyListenersOfValue(VString.of("Cannot decode " + data.formatType(), Alarm.noValue(), Time.now()));
            }
        }

    @Override
    public Future<VType> asyncRead() throws Exception
    {
        final Future<PVAStructure> data = channel.read(name_helper.getRequest());
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
        // Perform a disconnect check right now to alert caller
        // of clearly disconnected channel
        if (isDisconnected(read()))
            throw new IllegalStateException("Channel '" + getName() + "' is not connected");

        // The channel could still disconnect in the middle of the write,
        // or experience other errors which we'll receive in the
        // response checked below.

        // Perform a plain "put", not "put-callback"
        final Future<Void> response = channel.write(false, name_helper.getRequest(), new_value);

        // Compared to Channel Access, PVA currently offers no
        // information about writable vs. read-only channels.
        // A read-only channel will only inform us about the failed
        // write in the put response, for which we need to await
        // the return value from the PVA server.
        // Waiting for the response from the server, however,
        // can take a little time, enough to be noticeable in a GUI
        // that directly calls 'write' from the UI thread.
        // Still, there seems no alternative to waiting a little bit,
        // then potentially timing out.
        response.get(PVA_Preferences.epics_pva_write_reply_timeout_ms, TimeUnit.MILLISECONDS);
    }

    @Override
    public Future<?> asyncWrite(final Object new_value) throws Exception
    {
        // Perform a put with completion,
        // i.e., process target and block until processing completes,
        // akin to a Channel Access put-callback.
        // Return the Future that can be used to await completion
        return channel.write(true, name_helper.getRequest(), new_value);
    }

    @Override
    protected void close()
    {
        channel.close();
    }
}
