/*******************************************************************************
 * Copyright (c) 2019-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import static org.epics.pva.PVASettings.logger;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.logging.Level;

import org.epics.pva.common.CertificateStatusListener;
import org.epics.pva.common.CertificateStatusMonitor;
import org.epics.pva.common.CertificateStatusMonitor.CertificateStatus;
import org.epics.pva.common.CommandHandlers;
import org.epics.pva.common.PVAAuth;
import org.epics.pva.common.PVAHeader;
import org.epics.pva.common.RequestEncoder;
import org.epics.pva.common.SearchResponse;
import org.epics.pva.common.SecureSockets.TLSHandshakeInfo;
import org.epics.pva.common.TCPHandler;
import org.epics.pva.data.PVASize;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVATypeRegistry;

/** Handler for one TCP-connected client
 *  @author Kay Kasemir
 */
class ServerTCPHandler extends TCPHandler
{
    /** Handlers for various commands, re-used whenever a command is received */
    private static final CommandHandlers<ServerTCPHandler> handlers =
        new CommandHandlers<>(new ValidationHandler(),
                              new SearchCommandHandler(),
                              new EchoHandler(),
                              new CreateChannelHandler(),
                              new GetHandler(),
                              new PutHandler(),
                              new MonitorHandler(),
                              new DestroyChannelHandler(),
                              new GetTypeHandler(),
                              new DestroyRequestHandler(),
                              new RPCHandler(),
                              new CancelHandler());

    /** Server that holds all the PVs */
    private final PVAServer server;

    /** Info from TLS socket handshake or <code>null</code> */
    private final TLSHandshakeInfo tls_info;

    /** Types declared by client at other end of this TCP connection */
    private final PVATypeRegistry client_types = new PVATypeRegistry();

    /** Client authentication */
    private volatile ClientAuthentication client_auth = ClientAuthentication.Anonymous;

    /** {@link CertificateStatus} that we monitor for the TLS connection */
    private CertificateStatus certificate_status = null;

    /** Handler for updates from {@link CertificateStatusMonitor} */
    private final CertificateStatusListener certificate_status_listener;

    public ServerTCPHandler(final PVAServer server, final Socket client, final TLSHandshakeInfo tls_info) throws Exception
    {
        super(false);

        logger.log(Level.FINER, () -> "TCPHandler " + (tls_info != null ? "(TLS) " : "") + "for " + client.getRemoteSocketAddress() + " created ============================");

        // Server received the client socket from `accept`
        this.socket = Objects.requireNonNull(client);
        this.server = Objects.requireNonNull(server);
        this.tls_info = tls_info;

        server.register(this);

        certificate_status_listener = update->
        {
            final ClientAuthentication auth = getClientAuthentication();
            logger.log(Level.FINER, () -> "Certificate update for " + this + ": " + auth);

            // 1) Initial client_auth is Anonymous
            // When TLS connection starts,
            // 2a) CertificateStatusMonitor looks for CERT:STATUS:.., initial update has Anonymous from 1)
            // 2b) ValidationHandler will setClientAuthentication(x509 info from TLS)
            //     If somebody called getClientAuthentication(), they'd get Anon/invalid because no "Valid" update, yet
            // 3) "Valid" update from CertificateStatusMonitor tends to happen just after that
            //    --> Update all ServerPVs to send AccessRightsChange, in case there are already Server PVs
            server.updatePermissions(this, auth);

            // Channel created? CreateChannelHandler.sendChannelCreated sends initial AccessRightsChange
            // ServerPV.setWritable will send updated AccessRightsChange
        };
        if (tls_info != null  &&  !tls_info.status_pv_name.isEmpty())
            certificate_status = CertificateStatusMonitor.instance().checkCertStatus(tls_info, certificate_status_listener);

        startReceiver();
        startSender();

        // Initialize TCP connection by setting byte order..
        submit((version, buffer) ->
        {
            logger.log(Level.FINE, () -> "Set byte order " + buffer.order());
            // Payload size is used as byte order hint
            // 0xFFFFFFFF: Client needs to check each message for byte order
            // 0x00000000: Server sends each message in the same byte order,
            //             but there's still a valid byte order flag,
            //             so client is free to keep checking each message
            final int size_used_as_hint = 0x00000000;
            PVAHeader.encodeMessageHeader(buffer,
                    (byte) (PVAHeader.FLAG_CONTROL | PVAHeader.FLAG_SERVER),
                    PVAHeader.CTRL_SET_BYTE_ORDER, size_used_as_hint);
        });
        // .. and requesting connection validation
        submit((version, buffer) ->
        {
            logger.log(Level.FINE, () -> "Sending Validation Request");

            final int size_offset = buffer.position() + PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE;
            PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_SERVER, PVAHeader.CMD_CONNECTION_VALIDATION, 4+2+1);
            final int payload_start = buffer.position();

            // int serverReceiveBufferSize;
            buffer.putInt(receive_buffer.capacity());

            // short serverIntrospectionRegistryMaxSize;
            buffer.putShort(Short.MAX_VALUE);

            // If client identified itself on secure connection, server supports "x509"
            boolean support_x509 = this.tls_info != null;

            // string[] authNZ; listing most secure at end
            PVASize.encodeSize(support_x509 ? 3 : 2, buffer);
            PVAString.encodeString(PVAAuth.anonymous.name(), buffer);
            PVAString.encodeString(PVAAuth.ca.name(), buffer);
            if (support_x509)
                PVAString.encodeString(PVAAuth.x509.name(), buffer);

            buffer.putInt(size_offset, buffer.position() - payload_start);
        });
    }

    @Override
    protected boolean initializeSocket()
    {
        // Nothing to do, received client socket on construction
        return true;
    }

    @Override
    public InetSocketAddress getRemoteAddress()
    {
        return new InetSocketAddress(socket.getInetAddress(), socket.getPort());
    }

    PVAServer getServer()
    {
        return server;
    }

    TLSHandshakeInfo getTLSHandshakeInfo()
    {
        return tls_info;
    }

    PVATypeRegistry getClientTypes()
    {
        return client_types;
    }

    /** @param client_auth Client authentication  */
    void setClientAuthentication(final ClientAuthentication client_auth)
    {
        this.client_auth = client_auth;
    }

    /** @return How did the client authenticate? */
    ClientAuthentication getClientAuthentication()
    {
        // Do we have a certificate from the TLS connection, but CERT:STATUS:.. doesn't declare it valid?
        // --> Fall back to anonymous
        if (certificate_status != null  &&  !certificate_status.isValid())
            return new ClientAuthentication(PVAAuth.anonymous, "invalid/" + client_auth.getUser(), client_auth.getHost());

        return client_auth;
    }

    @Override
    protected void onReceiverExited(final boolean running)
    {
        if (certificate_status != null)
        {
            CertificateStatusMonitor.instance().remove(certificate_status, certificate_status_listener);
            certificate_status = null;
        }
        if (running)
            server.shutdownConnection(this);
    }

    @Override
    protected void handleControlMessage(final byte command, final ByteBuffer buffer) throws Exception
    {
        if (command == PVAHeader.CTRL_SET_BYTE_ORDER)
            logger.log(Level.WARNING, "Client sent SET_BYTE_ORDER command?");
        else
            super.handleControlMessage(command, buffer);
    }

    @Override
    protected void handleApplicationMessage(final byte command, final ByteBuffer buffer) throws Exception
    {
        if (!handlers.handleCommand(command, this, buffer))
            super.handleApplicationMessage(command, buffer);
    }

    /** Send a "channel found" reply to a client's search
     *  @param guid This server's GUID
     *  @param seq Client search request sequence number
     *  @param cid Client's channel ID or -1
     *  @param server_address TCP address where client can connect to server
     *  @param tls Should client use tls?
     */
    void submitSearchReply(final Guid guid, final int seq, final int cid, final InetSocketAddress server_address, final boolean tls)
    {
        final RequestEncoder encoder = (version, buffer) ->
        {
            logger.log(Level.FINER, () -> "Sending " + (tls ? "TLS" : "TCP") + " search reply");
            SearchResponse.encode(guid, seq, cid, server_address.getAddress(), server_address.getPort(), tls, buffer);
        };
        submit(encoder);
    }
}
