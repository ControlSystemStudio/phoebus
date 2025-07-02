/*******************************************************************************
 * Copyright (c) 2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import static org.epics.pva.PVASettings.logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.logging.Level;

import org.epics.pva.data.PVAAddress;

/** Helper for CMD_ORIGIN_TAG
 *
 *  <p>UDP search messages that are forwarded via
 *  the local 224.0.0.128 multicast are prefixed
 *  with CMD_ORIGIN_TAG.
 *
 *  <p>According to
 *  https://github.com/epics-docs/epics-docs/blob/master/pv-access/Protocol-Messages.md,
 *  the address listed in the origin tag should be
 *  "the address to which the receiving socket was bound. This may be 0.0.0.0".
 *
 *  <p>By default, we have EPICS_PVAS_INTF_ADDR_LIST = "0.0.0.0 [::] 224.0.0.128,1@127.0.0.1 [ff02::42:1],1@::1".
 *  The IPv4 UDP socket will be bound to 0.0.0.0, and the origin tags will show 0.0.0.0.
 *  Replacing "0.0.0.0" with the IP address of a network interface will cause it to show in the origin tags.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class OriginTag
{
    /** Size of CMD_ORIGIN_TAG payload (address) */
    public static final byte PAYLOAD_SIZE = 16;

    /** Size of CMD_ORIGIN_TAG message */
    public static final byte TOTAL_SIZE = PVAHeader.HEADER_SIZE + PAYLOAD_SIZE;

    /** "address to which the receiving socket was bound ..may be 0.0.0.0" */
    public InetAddress address;

    /** Check for optional CMD_ORIGIN_TAG before CMD_SEARCH
     *  @param from Peer address
     *  @param buffer UDP message buffer ready to decode CMD_SEARCH
     *  @return {@link OriginTag} or <code>null</code>
     */
    public static OriginTag testForOriginOfSearch(final InetSocketAddress from, final ByteBuffer buffer)
    {
        // Check for optional origin tag.
        // For a normal search packet, the buffer is positioned at PVAHeader.HEADER_SIZE (8).
        // For a forwarded packet, it's at CMD_ORIGIN_TAG message (8+16) + HEADER_SIZE (8), i.e., 32
        // For now assuming no or exactly one CMD_ORIGIN_TAG before CMD_SEARCH.
        // Not supporting multiple CMD_ORIGIN_TAGs nor anything else before CMD_SEARCH.

        // pos is just after the CMD_SEARCH message header
        final int pos = buffer.position();
        if (// Is there room for CMD_ORIGIN_TAG before this message header?
            pos == OriginTag.TOTAL_SIZE + PVAHeader.HEADER_SIZE  &&
            // Is command of previous message indeed CMD_ORIGIN_TAG?
            buffer.get(PVAHeader.HEADER_OFFSET_COMMAND) == PVAHeader.CMD_ORIGIN_TAG)
        {
            // Move back to origin message, decode it, restore buffer position
            buffer.position(PVAHeader.HEADER_SIZE);
            OriginTag origin = OriginTag.decode(from, OriginTag.PAYLOAD_SIZE, buffer);
            buffer.position(pos);
            return origin;
        }

        return null;
    }

    /** Decode origin tag
     *
     *  @param from Peer address
     *  @param payload Payload size
     *  @param buffer Buffer positioned on payload
     *  @return Decoded origin tag or <code>null</code> if not a valid
     */
    public static OriginTag decode(final InetSocketAddress from,
                                   final int payload, final ByteBuffer buffer)
    {
        if (payload < PAYLOAD_SIZE)
        {
            logger.log(Level.WARNING, "PVA client " + from + " sent only " + payload + " bytes for origin tag");
            return null;
        }
        final OriginTag origin = new OriginTag();

        // responseAddress, IPv6 address in case of IP based transport, UDP
        try
        {
            origin.address = PVAAddress.decode(buffer);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "PVA Client " + from + " sent origin tag with invalid address");
            return null;
        }
        logger.log(Level.FINER, () -> "PVA client " + from + " sent " + origin);
        return origin;
    }

    /** Encode origin tag
     *  @param udp UDP socket from which to pick the forwarder's address
     *  @param buffer Buffer into which to encode
     *  @return Encoded forwarder's address
     */
    public static InetAddress encode(final DatagramChannel udp, final ByteBuffer buffer)
    {
        PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_NONE, PVAHeader.CMD_ORIGIN_TAG, PAYLOAD_SIZE);
        InetAddress this_end;
        try
        {
            this_end = ((InetSocketAddress) udp.getLocalAddress()).getAddress();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Invalid address for CMD_ORIGIN_TAG", ex);
            this_end = new InetSocketAddress(0).getAddress();
        }
        PVAAddress.encode(this_end, buffer);
        return this_end;
    }

    @Override
    public String toString()
    {
        return "Origin tag with address " + address;
    }
}
