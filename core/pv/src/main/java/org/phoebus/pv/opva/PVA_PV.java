/*******************************************************************************
 * Copyright (c) 2014-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.opva;

import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.Channel.ConnectionState;
import org.epics.pvaccess.client.ChannelProvider;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvdata.copy.CreateRequest;
import org.epics.pvdata.monitor.Monitor;
import org.epics.pvdata.monitor.MonitorElement;
import org.epics.pvdata.monitor.MonitorRequester;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Structure;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.VEnum;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;

/** pvAccess {@link PV}
 *
 *  <p>Based on ideas from msekoranja org.epics.pvmanager.pva.PVAChannelHandler and PVATypeAdapter
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class PVA_PV extends PV implements ChannelRequester, MonitorRequester
{
    final private static short priority = ChannelProvider.PRIORITY_DEFAULT;

    /** Request factory */
    final private static CreateRequest request_creater = CreateRequest.create();

    /** Request used for reading */
    final private PVStructure read_request;

    /** Request used for writing */
    final private PVStructure write_request;

    /** Offset into received data that contains values selected by read_request */
    final private int value_offset;

    /** PVAccess channel, also holds the 'base_name' */
    final private Channel channel;

    /** If channel is enumerated, this holds its most recent labels */
    private volatile List<String> enum_labels = null;

    private Monitor value_monitor = null;

    /** Initialize
     *  @param name Full name, may include "pva://"
     *  @param base_name Base name without optional prefix
     *  @param provider {@link ChannelProvider}
     *  @throws Exception on error
     */
    PVA_PV(final String name, final String base_name) throws Exception
    {
        super(name);

        // Analyze base_name, determine channel and request
        final PVNameHelper request_helper = PVNameHelper.forName(base_name);

        logger.log(Level.FINE, "PV {0}: Channel \"{1}\", request \"{2}\"",
                   new Object[] { name, request_helper.getChannel(), request_helper.getReadRequest() });

        read_request = request_creater.createRequest(request_helper.getReadRequest());
        write_request = request_creater.createRequest(request_helper.getWriteRequest());
        value_offset = getValueOffset(read_request);

        channel = PVA_Context.getInstance().getProvider()
                             .createChannel(request_helper.getChannel(), this, priority);
    }

    /** Get offset to requested value.
     *
     *  <p>If the request is empty ("field()"),
     *  then this is 0 to use the start of the received PVStructures.
     *
     *  <p>If the request is "field(some_struct.some_subfield)",
     *  this will return the value offset to "some_subfield"
     *
     *  @param read_request Read request
     *  @return Value offset to use when decoding received data
     *  @throws Exception on error
     */
    private int getValueOffset(final PVStructure read_request) throws Exception
    {
        // read_request = structure
        //                   structure field   <-- Marks this as a request for fields
        //                        structure some_struct
        //                            structure some_subfield
        // Start at "field", then locate the deepest subfield
        PVStructure element = read_request.getSubField(PVStructure.class, "field");
        while (element != null)
        {
            final String[] fields = element.getStructure().getFieldNames();
            if (fields.length == 1)
            {   // Descend further into structure
                element = element.getSubField(PVStructure.class, fields[0]);
            }
            else if (fields.length == 0)
            {   // Found the requested field
                // Return offset-1 because read_request contains
                // another "structure field" level that
                // will be absent in the received data
                return element.getFieldOffset() - 1;
            }
            else
                throw new Exception("Can only handle request to single element, not " + read_request);
        }

        return 0;
    }

    // ChannelRequester
    @Override
    public String getRequesterName()
    {
        return getClass().getName();
    }

    // ChannelRequester
    @Override
    public void message(final String message, final MessageType type)
    {
        switch (type)
        {
        case fatalError:
            logger.log(Level.SEVERE, message);
            break;
        case error:
        case warning:
            logger.log(Level.WARNING, message);
            break;
        default:
            logger.log(Level.INFO, message);
        }
    }

    // ChannelRequester
    @Override
    public void channelCreated(final Status status, final Channel channel)
    {
        if (status.isSuccess())
        {
            logger.log(Level.FINER, "Channel {0} created", channel.getChannelName());
        }
        else
            logger.log(Level.WARNING, "Channel {0} status {1}",
                                   new Object[] { getName(), status.getMessage() });
    }

    // ChannelRequester
    @Override
    public void channelStateChange(final Channel channel, final ConnectionState state)
    {
        logger.log(Level.FINE, "Channel {0} {1}",
                new Object[] { channel.getChannelName(), state });
        switch (state)
        {
        case CONNECTED:
            subscribe();
            break;
        case DISCONNECTED:
            notifyListenersOfDisconnect();
        default:
            // Ignore
        }
    }

    private void subscribe()
    {
        synchronized (this)
        {   // Avoid double-subscription
            if (this.value_monitor != null)
                return;
            value_monitor = channel.createMonitor(this, read_request);
        }
    }

    // MonitorRequester
    @Override
    public void monitorConnect(final Status status, final Monitor monitor,
            final Structure structure)
    {
        if (status.isSuccess())
            monitor.start();
    }

    /** @param update_struct {@link PVStructure} received from a get or monitor
     *  @return {@link VType} extracted from the received data
     *  @throws Exception on error
     */
    VType handleValueUpdate(final PVStructure update_struct) throws Exception
    {
        final VType value = PVStructureHelper.getVType(update_struct, value_offset);
        if (value instanceof VEnum)
        {   // Remember most recent labels, but note that
            // not all updates will include the complete labels?!
            final EnumDisplay display = ((VEnum)value).getDisplay();
            if (display != null)
            {
                final List<String> labels = display.getChoices();
                if (! labels.isEmpty())
                    enum_labels = labels;
            }
        }
        notifyListenersOfValue(value);
        return value;
    }

    // MonitorRequester
    @Override
    public void monitorEvent(final Monitor monitor)
    {
        MonitorElement update;
        while ((update = monitor.poll()) != null)
        {
            try
            {
                handleValueUpdate(update.getPVStructure());
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot handle update for " + getName(), ex);
            }
            monitor.release(update);
        }
    }

    // MonitorRequester
    @Override
    public void unlisten(final Monitor monitor)
    {
        // Ignore
    }

    /** {@inheritDoc} */
    @Override
    public Future<VType> asyncRead() throws Exception
    {
        final PVGetHandler result = new PVGetHandler(this);
        channel.createChannelGet(result, read_request);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void write(final Object new_value) throws Exception
    {   // Submit write, but don't await/check its completion
        asyncWrite(new_value);
    }

    /** {@inheritDoc} */
    @Override
    public Future<?> asyncWrite(Object new_value) throws Exception
    {
        if (enum_labels != null  &&  new_value instanceof String)
        {   // Convert string-for-enum into index of corresponding label
            final VType current = read();
            if (current instanceof VEnum)
            {
                final int index = enum_labels.indexOf(new_value);
                if (index >= 0)
                    new_value = index;
                else
                    if (new_value instanceof String)
                    {   // Try parsing number from string
                        try
                        {
                            new_value = Integer.valueOf((String) new_value);
                        }
                        catch (NumberFormatException ex)
                        {
                            throw new Exception("Cannot obtain index for enum PV " + getName() + " from value " + new_value);
                        }
                    }
                    else
                        throw new Exception("Cannot obtain label's index for enum PV " + getName() + " from value " + new_value);
            }
        }

        final PVPutHandler requester = new PVPutHandler(this, new_value);
        channel.createChannelPut(requester, write_request);
        return requester;
    }

    /** {@inheritDoc} */
    @Override
    protected void close()
    {
        channel.destroy();
    }
}