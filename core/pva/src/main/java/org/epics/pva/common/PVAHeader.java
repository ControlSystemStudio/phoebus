/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** PVA Message Header
 *
 *  <pre>
 *  byte PVA_MAGIC
 *  byte PVA_PROTOCOL_REVISION
 *  byte FLAG_*
 *  byte CMD_*
 *  int payload_size
 *  </pre>
 */
@SuppressWarnings("nls")
public class PVAHeader
{
    /** PVA protocol magic */
    public static final byte PVA_MAGIC = (byte)0xCA;

    /** PVA protocol revision (implemented by this library) */
    public static final byte PVA_PROTOCOL_REVISION = 2;

    /** Oldest PVA protocol revision handled by this library */
    public static final byte REQUIRED_PVA_PROTOCOL_REVISION = 1;

    /** Application message, single, by client, little endian */
    public static final byte FLAG_NONE       = 0;

    /** Control message? Else: Application */
    public static final byte FLAG_CONTROL    = 1;

    /** Segmented message? Else: Single */
    public static final byte FLAG_SEGMENT_MASK  = 3 << 4;
    public static final byte FLAG_FIRST      = 1 << 4;
    public static final byte FLAG_LAST       = 2 << 4;
    public static final byte FLAG_MIDDLE     = 3 << 4;

    /** Server message? Else: Client */
    public static final byte FLAG_SERVER     = 1 << 6;

    /** Big endian encoding? Else: Little endian */
    public static final byte FLAG_BIG_ENDIAN = (byte) (1 << 7);

    /** Application command: Beacon */
    public static final byte CMD_BEACON = 0x00;

    /** Application command: Connection validation */
    public static final byte CMD_CONNECTION_VALIDATION = 0x01;

    /** Application command: Echo */
    public static final byte CMD_ECHO = 0x02;

    /** Application command: Search */
    public static final byte CMD_SEARCH = 0x03;

    /** Application command: Reply to search */
    public static final byte CMD_SEARCH_RESPONSE = 0x04;

    /** Application command: Create Channel */
    public static final byte CMD_CREATE_CHANNEL = 0x07;

    /** Application command: Destroy Channel */
    public static final byte CMD_DESTROY_CHANNEL = 0x08;

    /** Application command: Connection was validated */
    public static final byte CMD_CONNECTION_VALIDATED = 0x09;

    /** Application command: Get data */
    public static final byte CMD_GET = 0x0A;

    /** Application command: Get data */
    public static final byte CMD_PUT = 0x0B;

    /** Application command: Get data */
    public static final byte CMD_MONITOR = 0x0D;

    /** Application command: Destroy Request */
    public static final byte CMD_DESTROY_REQUEST = 0x0F;

    /** Application command: Get type info (aka "FIELD" request) */
    public static final byte CMD_GET_TYPE = 0x11;

    /** Application command: Message */
    public static final byte CMD_MESSAGE = 0x12;

    /** Application command: Remote Procedure Call */
    public static final byte CMD_RPC = 0x14;

    /** Application command: Cancel request */
    public static final byte CMD_CANCEL_REQUEST = 0x15;

    /** Application command: Origin tag */
    public static final byte CMD_ORIGIN_TAG = 0x16;


    /** Sub command to initialize GET/PUT/MONITOR/RPC (get data description) */
    public static final byte CMD_SUB_INIT = 0x08;

    /** MONITOR PIPELINE flag */
    public static final byte CMD_SUB_PIPELINE = (byte) 0x80;

    /** Sub command to (re)start getting monitor values */
    public static final byte CMD_SUB_START = 0x44;

    /** Sub command to stop/pause a monitor*/
    public static final byte CMD_SUB_STOP = 0x04;

    /** Sub command delete a request GET/PUT/MONITOR/RPC */
    public static final byte CMD_SUB_DESTROY = 0x10;

    /** Sub command of PUT to first GET the current value */
    public static final byte CMD_SUB_GET = 0x40;


    /** Control message command to set byte order */
    public static final byte CTRL_SET_BYTE_ORDER = 2;

    /** Size of common PVA message header */
    public static final int HEADER_SIZE = 8;

    /** Offset from start of common PVA message header to int payload_size */
    public static final int HEADER_OFFSET_PAYLOAD_SIZE = 4;


    /** Encode common PVA message header
     *  @param buffer Buffer into which to encode
     *  @param flags  Combination of FLAG_
     *  @param command Command
     *  @param payload_size Size of payload that follows
     */
    public static void encodeMessageHeader(final ByteBuffer buffer, byte flags, final byte command, final int payload_size)
    {
        if (buffer.order() == ByteOrder.BIG_ENDIAN)
            flags |= FLAG_BIG_ENDIAN;
        else
            flags &= ~FLAG_BIG_ENDIAN;
        buffer.clear();
        buffer.put(PVA_MAGIC);
        buffer.put(PVA_PROTOCOL_REVISION);
        buffer.put(flags);
        buffer.put(command);
        buffer.putInt(payload_size);
    }

    /** Check message header for correct protocol identifier and version
     *  @param buffer Buffer as start of protocol header
     *  @param expect_server Expect a server message? Else client message
     *  @return Expected total message size (header + payload)
     *  @throws Exception on protocol violation
     */
    public static int checkMessageAndGetSize(final ByteBuffer buffer, final boolean expect_server) throws Exception
    {
        if (buffer.position() < PVAHeader.HEADER_SIZE)
            return PVAHeader.HEADER_SIZE;

        final byte magic = buffer.get(0);
        if (magic != PVAHeader.PVA_MAGIC)
            throw new Exception(String.format("Message lacks magic 0x%02X, got 0x%02X", PVAHeader.PVA_MAGIC, magic));

        final byte version = buffer.get(1);
        if (version < PVAHeader.REQUIRED_PVA_PROTOCOL_REVISION)
            throw new Exception("Cannot handle protocol version " + version +
                                ", expect version " +
                                PVAHeader.REQUIRED_PVA_PROTOCOL_REVISION +
                                " or higher");

        final byte flags = buffer.get(2);
        final boolean is_server = (flags & PVAHeader.FLAG_SERVER) != 0;
        if (is_server != expect_server)
                throw new Exception(expect_server ? "Expected server message" : "Expected client message");

        if ((flags & PVAHeader.FLAG_BIG_ENDIAN) == 0)
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        else
            buffer.order(ByteOrder.BIG_ENDIAN);

        // Control messages use the 'payload' field itself for data
        if ((flags & PVAHeader.FLAG_CONTROL) != 0)
            return PVAHeader.HEADER_SIZE;

        // Application messages are followed by this number of data bytes
        final int payload = buffer.getInt(PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE);

        // Total message size: Header followed by data
        return PVAHeader.HEADER_SIZE + payload;
    }
}
