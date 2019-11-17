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
import org.epics.pva.data.PVABitSet;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVAStatus;
import org.epics.pva.data.PVAStructure;

/** Send a 'GET' request to server and handle response
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class GetRequest extends CompletableFuture<PVAStructure> implements RequestEncoder, ResponseHandler
{
    private final PVAChannel channel;

    private final String request;

    private final int request_id;

    /** INIT or GET? */
    private volatile boolean init = true;

    private volatile PVAStructure data;

    /** Request to read channel's value
     *  @param channel {@link PVAChannel}
     *  @param request Request, "" for all fields, or "field_a, field_b.subfield"
     */
    public GetRequest(final PVAChannel channel, final String request)
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
            logger.log(Level.FINE, () -> "Sending get INIT request #" + request_id + " for " + channel + " '" + request + "'");

            // Guess, assumes empty FieldRequest (6)
            final int size_offset = buffer.position() + PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE;
            PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_NONE, PVAHeader.CMD_GET, 4+4+1+6);
            final int payload_start = buffer.position();
            buffer.putInt(channel.getSID());
            buffer.putInt(request_id);
            buffer.put(PVAHeader.CMD_SUB_INIT);

            final FieldRequest field_request = new FieldRequest(request);
            field_request.encodeType(buffer);
            field_request.encode(buffer);
            buffer.putInt(size_offset, buffer.position() - payload_start);

            init = false;
        }
        else
        {
            logger.log(Level.FINE, () -> "Sending get GET request #" + request_id + " for " + channel);
            PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_NONE, PVAHeader.CMD_GET, 4+4+1);
            buffer.putInt(channel.getSID());
            buffer.putInt(request_id);
            buffer.put(PVAHeader.CMD_SUB_DESTROY);
        }
    }

    @Override
    public void handleResponse(final ByteBuffer buffer) throws Exception
    {
        if (buffer.remaining() < 4+1+1)
            fail(new Exception("Incomplete Get Response"));
        final int request_id = buffer.getInt();
        final byte subcmd = buffer.get();
        PVAStatus status = PVAStatus.decode(buffer);
        if (! status.isSuccess())
            throw new Exception(channel + " Get Response for " + request + ": " + status);

        if (subcmd == PVAHeader.CMD_SUB_INIT)
        {
            logger.log(Level.FINE,
                       () -> "Received get INIT reply #" + request_id +
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
                fail(new Exception("Expected PVAStructure, got " + type));
            }

            // Submit request again, this time to GET data
            channel.getTCP().submit(this, this);
        }
        else
        {
            logger.log(Level.FINE,
                       () -> "Received get GET reply #" + request_id +
                             " for " + channel + ": " + status);

            // Decode data from GET reply
            // 1) Bitset that indicates which elements of struct will follow
            final BitSet changes = PVABitSet.decodeBitSet(buffer);
            logger.log(Level.FINER, () -> "Updated: " + changes);

            // 2) Decode those elements
            data.decodeElements(changes, channel.getTCP().getTypeRegistry(), buffer);

            // Indicate completion now that we have data
            complete(data);
        }
    }

    private void fail(final Exception ex) throws Exception
    {
        completeExceptionally(ex);
        throw ex;
    }
}
