/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import static org.epics.pva.PVASettings.logger;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.logging.Level;

import org.epics.pva.common.PVAHeader;
import org.epics.pva.common.RequestEncoder;
import org.epics.pva.data.PVABitSet;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVAStatus;
import org.epics.pva.data.PVAStructure;

@SuppressWarnings("nls")
class MonitorRequest implements AutoCloseable, RequestEncoder, ResponseHandler
{
    private final PVAChannel channel;

    private final String request;

    private final MonitorListener listener;

    private final int request_id;

    /** Next request to send, cycling from INIT to START.
     *  Cancel() then sets it to DESTROY.
     */
    private volatile byte state = PVAHeader.CMD_SUB_INIT;

    private volatile PVAStructure data;

    public MonitorRequest(final PVAChannel channel, final String request, final MonitorListener listener) throws Exception
    {
        this.channel = channel;
        this.request = request;
        this.listener = listener;
        this.request_id = channel.getClient().allocateRequestID();
        channel.getTCP().submit(this, this);
    }

    @Override
    public int getRequestID()
    {
        return request_id;
    }

    @Override
    public void encodeRequest(final byte version, final ByteBuffer buffer) throws Exception
    {
        if (state == PVAHeader.CMD_SUB_INIT)
        {
            logger.log(Level.FINE, () -> "Sending monitor INIT request #" + request_id + " for " + channel + " '" + request + "'");

            // Guess size based on empty field request (6)
            final int size_offset = buffer.position() + PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE;
            PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_NONE, PVAHeader.CMD_MONITOR, 4+4+1+6);
            final int payload_start = buffer.position();
            buffer.putInt(channel.sid);
            buffer.putInt(request_id);

            final boolean pipeline = false;
            if (pipeline)
                buffer.put((byte) (PVAHeader.CMD_SUB_PIPELINE | PVAHeader.CMD_SUB_INIT));
            else
                buffer.put(PVAHeader.CMD_SUB_INIT);

            // TODO Set pipeline flag, send nfree=10
            // record._options.pipeline=true

            final FieldRequest field_request = new FieldRequest(pipeline, request);
            logger.log(Level.FINE, () -> "Monitor INIT request " + field_request);
            field_request.encodeType(buffer);
            // TODO Encode pipeline value
            if (pipeline)
            {
                field_request.encode(buffer);
                // nfree = 10
                buffer.putInt(10);
            }
            buffer.putInt(size_offset, buffer.position() - payload_start);
        }
        else
        {
            if (state == PVAHeader.CMD_SUB_START)
                logger.log(Level.FINE, () -> "Sending monitor START request #" + request_id + " for " + channel);
            else if (state == PVAHeader.CMD_SUB_STOP)
                logger.log(Level.FINE, () -> "Sending monitor STOP request #" + request_id + " for " + channel);
            else if (state == PVAHeader.CMD_SUB_DESTROY)
                logger.log(Level.FINE, () -> "Sending monitor DESTROY request #" + request_id + " for " + channel);
            else
                throw new Exception("Cannot handle monitor state " + state);
            PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_NONE, PVAHeader.CMD_MONITOR, 4+4+1);
            buffer.putInt(channel.sid);
            buffer.putInt(request_id);
            buffer.put(state);
        }
    }

    @Override
    public void handleResponse(final ByteBuffer buffer) throws Exception
    {
        if (buffer.remaining() < 4+1+1)
            throw new Exception("Incomplete Monitor Response");
        final int request_id = buffer.getInt();
        final byte subcmd = buffer.get();

        // Response to specific command, or value update?
        if (subcmd != 0)
        {
            PVAStatus status = PVAStatus.decode(buffer);
            if (! status.isSuccess())
            {
                logger.log(Level.WARNING, channel + " Monitor Response for " + request + ": " + status);
                return;
            }

            if (subcmd == PVAHeader.CMD_SUB_INIT)
            {
                logger.log(Level.FINE,
                           () -> "Received monitor INIT reply #" + request_id +
                                 " for " + channel + ": " + status);

                // Decode type description from INIT response
                final PVAData type = channel.getTCP().getTypeRegistry().decodeType("", buffer);
                if (type instanceof PVAStructure)
                {
                    data = (PVAStructure)type;
                    logger.log(Level.FINER, () -> "Introspection Info: " + data.formatType());
                }
                else
                {
                    data = null;
                    throw new Exception("Expected PVAStructure, got " + type);
                }

                // Submit request again, this time to START getting data
                state = PVAHeader.CMD_SUB_START;
                channel.getTCP().submit(this, this);
            }
            else
                throw new Exception("Unexpected Monitor response to subcmd " + subcmd);
        }
        else
        {   // Value update
            if (channel.getState() != ClientChannelState.CONNECTED)
            {
                logger.log(Level.WARNING,
                           () -> "Received unexpected monitor #" + request_id +
                                 " update for " + channel);
                return;
            }
            logger.log(Level.FINE,
                       () -> "Received monitor #" + request_id +
                             " update for " + channel);

            // Decode data from monitor update
            // 1) Bitset that indicates which elements of struct have changed
            final BitSet changes = PVABitSet.decodeBitSet(buffer);

            // 2) Decode those elements
            data.decodeElements(changes, channel.getTCP().getTypeRegistry(), buffer);

            final BitSet overrun = PVABitSet.decodeBitSet(buffer);
            logger.log(Level.FINER, () -> "Overruns: " + overrun);

            // Notify listener of latest value
            listener.handleMonitor(channel, changes, overrun, data);

            // TODO With pipelining, once we receive nfree/2, request another nfree
        }
    }

    @Override
    public void close() throws Exception
    {
        // Submit request again, this time to stop getting data
        state = PVAHeader.CMD_SUB_DESTROY;
        final ClientTCPHandler tcp = channel.getTCP();
        tcp.submit(this, this);
        // Not expecting more replies
        tcp.removeResponseHandler(request_id);
    }

    @Override
    public String toString()
    {
        return "Monitor for " + channel + ", request ID " + request_id;
    }
}
