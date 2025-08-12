/*******************************************************************************
 * Copyright (c) 2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import static org.epics.pva.PVASettings.logger;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;

/** Helper for CMD_ACL_CHANGE
 *  @author Kay Kasemir
 */
public class AccessRightsChange
{
    /** Size of payload */
    public static final int PAYLOAD_SIZE = Integer.BYTES + 1;

    /** Client channel ID */
    public int cid;

    /** Access rights bits */
    public byte access_rights;

    /** Access rights bit definitions */
    public static final byte READ_ONLY    = 0x00,
                             WRITE_ACCESS = 0x01;

    private AccessRightsChange(final int cid, final byte access_rights)
    {
        this.cid = cid;
        this.access_rights = access_rights;
    }

    /** @return Do the access rights include write access? */
    public boolean isWritable()
    {
        return (access_rights & WRITE_ACCESS) == WRITE_ACCESS;
    }

    /** Encode access rights change
     *  @param buffer Buffer into which to encode
     *  @param cid Client channel ID
     *  @param b Access rights
     */
    public static void encode(final ByteBuffer buffer, final int cid, final boolean writable)
    {
        PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_SERVER, PVAHeader.CMD_ACL_CHANGE, PAYLOAD_SIZE);
        buffer.putInt(cid);
        buffer.put(writable ? WRITE_ACCESS : READ_ONLY);
    }

    /** Decode access rights change
     *  @param from Peer address
     *  @param payload Payload size
     *  @param buffer Buffer positioned on payload
     *  @return Decoded access rights change or <code>null</code> if not a valid
     */
    public static AccessRightsChange decode(final InetSocketAddress from,
                                            final int payload, final ByteBuffer buffer)
    {
        if (payload < PAYLOAD_SIZE)
        {
            logger.log(Level.WARNING, "PVA client " + from + " sent only " + payload + " bytes for access rights change");
            return null;
        }
        final AccessRightsChange acl = new AccessRightsChange(buffer.getInt(), buffer.get());
        logger.log(Level.FINER, () -> "PVA client " + from + " sent " + acl);
        return acl;
    }

    @Override
    public String toString()
    {
        return String.format("CID %d access rights %s (0x%02X)",
                             cid,
                             isWritable() ? "writeable" : "read-only",
                             access_rights);
    }
}
