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
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.epics.pva.common.PVAHeader;
import org.epics.pva.common.RequestEncoder;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVAStatus;
import org.epics.pva.data.PVAStructure;

/** Send a 'RPC' request to server and handle response
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class RPCRequest extends CompletableFuture<PVAStructure> implements RequestEncoder, ResponseHandler
{
    private final PVAChannel channel;

    private final PVAStructure request;

    private final int request_id;

    /** INIT or invoke? */
    private volatile boolean init = true;

    private volatile PVAStructure response;

    /** Request to perform a remove procedure call (RPC)
     *  @param channel {@link PVAChannel}
     *  @param request Request, structure with parameter data to send
     */
    public RPCRequest(final PVAChannel channel, final PVAStructure request)
    {
        this.channel = channel;
        this.request = request;
        this.request_id = channel.getClient().allocateRequestID();
        try
        {
            channel.getTCP().submit(this, this);
        }
        catch (Exception ex)
        {
            completeExceptionally(ex);
        }
    }

    @Override
    public int getRequestID()
    {
        return request_id;
    }

    @Override
    public void encodeRequest(final byte version, final ByteBuffer buffer) throws Exception
    {
        if (init)
        {
            logger.log(Level.FINE, () -> "Sending RPC INIT request #" + request_id + " for " + channel + ":\n" + request);
            // Example:
            // Init channel 1, request 1, define type 1 as a structure (no type name, no content)
            // 01 00 00 00  01 00 00 00  08 FD 01 00  80 00 00     .... .... .... ...
            final int size_offset = buffer.position() + PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE;
            PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_NONE, PVAHeader.CMD_RPC, 4+4+1+2);
            final int payload_start = buffer.position();
            buffer.putInt(channel.getSID());
            buffer.putInt(request_id);
            buffer.put(PVAHeader.CMD_SUB_INIT);

            final PVAStructure generic_struct = new PVAStructure("", "");
            generic_struct.setTypeID((short)1);
            generic_struct.encodeType(buffer, new BitSet());

            // Update size
            buffer.putInt(size_offset, buffer.position() - payload_start);

            init = false;
        }
        else
        {
            logger.log(Level.FINE, () -> "Invoke RPC request #" + request_id + " for " + channel);
            // Example:
            // Invoke channel 1, request 1, define type 2 as
            // structure (no type name, 2 fields) 'a' string, 'b' string.
            // Data: "3.14", "2.71"
            // 01 00 00 00  01 00 00 00  00 FD 02 00  80 00 02 01  .... .... .... ....
            // 61 60 01 62  60 04 33 2E  31 34 04 32  2E 37 31     a`.b `.3. 14.2 .71
            final int size_offset = buffer.position() + PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE;
            PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_NONE, PVAHeader.CMD_RPC, 4+4+1);
            final int payload_start = buffer.position();
            buffer.putInt(channel.getSID());
            buffer.putInt(request_id);
            buffer.put(PVAHeader.CMD_SUB_DESTROY);

            request.encodeType(buffer, new BitSet());
            request.encode(buffer);

            // Update size
            buffer.putInt(size_offset, buffer.position() - payload_start);
        }
    }

    @Override
    public void handleResponse(final ByteBuffer buffer) throws Exception
    {
        if (buffer.remaining() < 4+1+1)
            fail(new Exception("Incomplete RPC Response"));
        final int request_id = buffer.getInt();
        final byte subcmd = buffer.get();
        PVAStatus status = PVAStatus.decode(buffer);
        if (! status.isSuccess())
            throw new Exception(channel + " RPC Response for " + request + ": " + status);

        if (subcmd == PVAHeader.CMD_SUB_INIT)
        {
            logger.log(Level.FINE,
                       () -> "Received RPC INIT reply #" + request_id +
                             " for " + channel + ": " + status);

            // Submit request again, this time to invoke RPC
            channel.getTCP().submit(this, this);
        }
        else
        {
            logger.log(Level.FINE,
                       () -> "Received RPC reply #" + request_id +
                             " for " + channel + ": " + status);

            // Decode type description
            final PVAData type = channel.getTCP().getTypeRegistry().decodeType("", buffer);
            if (type instanceof PVAStructure)
            {
                response = (PVAStructure)type;
                logger.log(Level.FINER, () -> "Response type: " + response.formatType());
            }
            else
            {
                response = null;
                fail(new Exception("Expected PVAStructure, got " + type));
            }

            // Decode data
            response.decode(channel.getTCP().getTypeRegistry(), buffer);

            // Indicate completion now that we have response
            complete(response);
        }
    }

    private void fail(final Exception ex) throws Exception
    {
        completeExceptionally(ex);
        throw ex;
    }
}
