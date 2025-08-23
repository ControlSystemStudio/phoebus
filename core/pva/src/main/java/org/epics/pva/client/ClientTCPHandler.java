/*******************************************************************************
 * Copyright (c) 2019-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import static org.epics.pva.PVASettings.logger;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import javax.net.ssl.SSLSocket;

import org.epics.pva.PVASettings;
import org.epics.pva.common.CommandHandlers;
import org.epics.pva.common.PVAHeader;
import org.epics.pva.common.RequestEncoder;
import org.epics.pva.common.SecureSockets;
import org.epics.pva.common.TCPHandler;
import org.epics.pva.data.PVATypeRegistry;
import org.epics.pva.server.Guid;

/** Handle TCP connection to PVA server
 *
 *  <p>Maintains state of all the channels that
 *  we read/write on one PVA server.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ClientTCPHandler extends TCPHandler
{
    private static final CommandHandlers<ClientTCPHandler> handlers =
        new CommandHandlers<>(new SearchResponseHandler(),
                              new ValidationHandler(),
                              new ValidatedHandler(),
                              new EchoHandler(),
                              new CreateChannelHandler(),
                              new AccessRightsChangeHandler(),
                              new DestroyChannelHandler(),
                              new GetHandler(),
                              new PutHandler(),
                              new MonitorHandler(),
                              new GetTypeHandler(),
                              new RPCHandler());

    /** Address of server to which this client will connect */
    private final InetSocketAddress server_address;

    /** Is this a TLS connection or plain TCP? */
    private final boolean tls;

    /** Client context */
    private final PVAClient client;

    /** Channels that use this connection */
    private final CopyOnWriteArrayList<PVAChannel> channels = new CopyOnWriteArrayList<>();

    /** Server's GUID */
    private volatile Guid guid;

    /** Description of data types used with this PVA server */
    private final PVATypeRegistry types = new PVATypeRegistry();

    /** Map of response handlers by request ID
     *
     *  <p>When response for request ID is received,
     *  handler is removed from map and invoked
     */
    private final ConcurrentHashMap<Integer, ResponseHandler> response_handlers = new ConcurrentHashMap<>();

    /** Timer used to check if connection is still alive */
    private static final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(run ->
    {
        final Thread thread = new Thread(run, "TCP Alive Timer");
        thread.setDaemon(true);
        return thread;
    });

    private volatile ScheduledFuture<?> alive_check;

    /** Time [ms] when this client received last message from server */
    private volatile long last_life_sign;

    /** Time [ms] when this client sent last message to server */
    private volatile long last_message_sent;

    /** Creates echo requests, tracks the counter for this TCP connection */
    private final EchoRequest echo_request = new EchoRequest();

    /** Indicates completion of the connection validation:
     *  Server sent connection validation request,
     *  we replied, server confirmed with CMD_VALIDATED.
     *
     *  Client must not send get/put/.. messages until
     *  this flag is set.
     */
    private final AtomicBoolean connection_validated = new AtomicBoolean(false);

    public ClientTCPHandler(final PVAClient client, final InetSocketAddress address, final Guid guid, final boolean tls) throws Exception
    {
        super(true);
        logger.log(Level.FINE, () -> "TCPHandler " + (tls ? "(TLS) " : "") + guid + " for " + address + " created ============================");
        this.server_address = address;
        this.tls = tls;
        this.client = client;
        this.guid = guid;

        // Start receiver, but not the send thread, yet.
        // To prevent sending messages before the server is ready,
        // it's started when server confirms the connection.
        startReceiver();
    }

    @Override
    protected boolean initializeSocket()
    {
        try
        {
            socket = SecureSockets.createClientSocket(server_address, tls);
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "PVA client cannot connect to " + server_address, ex);
            return false;
        }

        // For default EPICS_CA_CONN_TMO: 30 sec, send echo at ~15 sec:
        // Check every ~3 seconds
        last_life_sign = last_message_sent = System.currentTimeMillis();
        final long period = Math.max(1, PVASettings.EPICS_PVA_CONN_TMO * 1000L / 30 * 3);
        alive_check = timer.scheduleWithFixedDelay(this::checkResponsiveness, period, period, TimeUnit.MILLISECONDS);

        return true;
    }

    @Override
    public InetSocketAddress getRemoteAddress()
    {
        // socket may not be connected or null, return address to which we want to connect
        return new InetSocketAddress(server_address.getAddress(), server_address.getPort());
    }

    /** @return Client context */
    PVAClient getClient()
    {
        return client;
    }

    /** When using TLS, the socket has a peer (server, IOC) certificate
     *  @return Name from server's certificate, or <code>null</code>
     */
    String getServerX509Name()
    {
        try
        {
            if (tls)
                return SecureSockets.getPrincipalCN(((SSLSocket) socket).getSession().getPeerPrincipal());
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot get server principal", ex);
        }
        return null;
    }

    /** When using TLS, the socket may come with a local (client) certificate
     *  that TLS uses to authenticate to the server.
     *  @return Name from client's certificate, or <code>null</code> */
    String getClientX509Name()
    {
        return tls ? SecureSockets.getPrincipalCN(((SSLSocket) socket).getSession().getLocalPrincipal())
                   : null;
    }

    /** @param channel Channel that uses this TCP connection */
    void addChannel(final PVAChannel channel)
    {
        channels.add(channel);
    }

    /** @param channel Channel that no longer uses this TCP connection */
    void removeChannel(final PVAChannel channel)
    {
        channels.remove(channel);
    }

    /** @return Channels that use this connection */
    public Collection<PVAChannel> getChannels()
    {
        return channels;
    }

    /** @return Guid of server */
    public Guid getGuid()
    {
        return guid;
    }

    /** Update the Guid
     *
     *  <p>The Guid of a server is fixed,
     *  but this TCP handler may start out with Guid.EMPTY
     *  to issue searches to an IP address.
     *  Upon the first successful search reply,
     *  we set the Guid based on the search reply.
     *
     *  @param search_reply_guid Guid from search reply
     *  @return <code>true</code> if this updated the Guid from EMPTY
     */
    public boolean updateGuid(final Guid search_reply_guid)
    {
        if (guid.equals(Guid.EMPTY))
        {
            guid = search_reply_guid;
            return true;
        }
        return false;
    }

    /** @return {@link PVATypeRegistry} for this TCP connection */
    public PVATypeRegistry getTypeRegistry()
    {
        return types;
    }

    /** Submit item to be sent to server and register handler for the response
     *
     *  <p>Handler will be invoked when the server replies to the request.
     *  @param item {@link RequestEncoder}
     *  @param handler {@link ResponseHandler}
     */
    public void submit(final RequestEncoder item, final ResponseHandler handler)
    {
        final int request_id = handler.getRequestID();
        response_handlers.put(request_id, handler);
        if (! submit(item))
            removeResponseHandler(request_id);
    }

    @Override
    protected void send(ByteBuffer buffer) throws Exception
    {
        // Remember when we last sent a message to the server
        last_message_sent = System.currentTimeMillis();
        super.send(buffer);
    }

    ResponseHandler getResponseHandler(final int request_id)
    {
        return response_handlers.get(request_id);
    }

    /** Unregister response handler
     *  @param request_id Request ID
     *  @return {@link ResponseHandler} that will no longer be called,
     *          <code>null</code> if none registered
     */
    ResponseHandler removeResponseHandler(final int request_id)
    {
        return response_handlers.remove(request_id);
    }

    /** Check responsiveness of this TCP connection */
    private void checkResponsiveness()
    {
        final long now = System.currentTimeMillis();
        // How long has server been idle, not sending anything?
        final long idle = now - last_life_sign;
        if (idle > PVASettings.EPICS_PVA_CONN_TMO * 1000)
        {
            // If silent for full EPICS_CA_CONN_TMO, disconnect and start over
            logger.log(Level.FINE, () -> this + " idle for " + idle + "ms, closing");
            client.shutdownConnection(this);
            return;
        }

        boolean request_echo = false;
        if (idle >= PVASettings.EPICS_PVA_CONN_TMO * 1000 / 2)
        {
            if (channels.isEmpty())
            {   // Connection is idle because no channel uses it. Close!
                logger.log(Level.FINE, () -> this + " unused for " + idle + "ms, closing");
                client.shutdownConnection(this);
                return;
            }
            // With default EPICS_CA_CONN_TMO of 30 seconds,
            // Echo requested every 15 seconds.
            logger.log(Level.FINE, () -> this + " idle for " + idle + "ms, requesting echo");
            request_echo = true;
        }

        // How long have we been silent, which could cause the server to close connection?
        final long silent = now - last_message_sent;
        if (! request_echo  &&  silent >= PVASettings.EPICS_PVA_CONN_TMO * 1000 / 2)
        {
            // With default EPICS_CA_CONN_TMO of 30 seconds,
            // Echo sent every 15 seconds to inform server that this client is still alive.
            logger.log(Level.FINE, () -> "Client to " + this + " silent for " + silent + "ms, sending echo");
            request_echo = true;
        }

        if (request_echo)
        {
            // Skip echo if the send queue already has items to avoid
            // filling queue which isn't emptied anyway.
            if (isSendQueueIdle())
                submit(echo_request);
            else
                logger.log(Level.FINE, () -> "Skipping echo, send queue already has items to send");
        }
    }

    /** @return Most recently sent echo request */
    String getActiveEchoRequest()
    {
        return echo_request.getActiveRequest();
    }

    /** Called whenever e.g. value is received and server is thus alive */
    void markAlive()
    {
        last_life_sign = System.currentTimeMillis();
    }

    @Override
    protected void onReceiverExited(final boolean running)
    {
        if (running)
            client.shutdownConnection(this);
    }

    @Override
    protected void handleControlMessage(final byte command, final ByteBuffer buffer) throws Exception
    {
        // 0 byte magic
        // 1 byte version
        // 2 byte flags
        // 3 byte command
        // 4 int32 payload size
        // 8 .. payload
        if (command == PVAHeader.CTRL_SET_BYTE_ORDER)
        {
            // First message received from server, remember its version
            final byte version = buffer.get(1);
            server_version  = version;
            // By the time we decode this message,
            // receive buffer byte order has been set to the
            // order sent by the server.
            // Send thread is not running, yet, so safe to
            // configure it
            send_buffer.order(buffer.order());

            if (connection_validated.get())
                logger.log(Level.WARNING, () -> "Server Version " + server_version + " sets byte order to " + send_buffer.order() +
                           " after connection has already been validated");
            else
                logger.log(Level.FINE, () -> "Server Version " + server_version + " sets byte order to " + send_buffer.order());
            // Payload 'size' indicates if the server will send messages in that same order,
            // or might change order for each message.
            // We always adapt based on the flags of each received message,
            // so ignore.
            // send_buffer byte order is locked at this time, though.
            final int hint = buffer.getInt(4);
            if (hint == 0x00000000)
                logger.log(Level.FINE, () -> "Server hints that it will send all messages in byte order " + send_buffer.order());
            else if (hint == 0xFFFFFFFF)
                logger.log(Level.FINE, () -> "Server hints that client needs to check each received messages for changing byte order");
            else
                logger.log(Level.WARNING, () -> String.format("Server sent SET_BYTE_ORDER hint 0x%08X, expecting 0x00000000 or 0xFFFFFFFF", hint));
        }
        else
            super.handleControlMessage(command, buffer);
    }

    @Override
    protected void handleApplicationMessage(final byte command, final ByteBuffer buffer) throws Exception
    {
        if (! handlers.handleCommand(command, this, buffer))
            super.handleApplicationMessage(command, buffer);
    }

    void handleValidationRequest(final int server_receive_buffer_size,
                                 final short server_introspection_registry_max_size,
                                 final ClientAuthentication auth) throws Exception
    {
        // Don't send more than the server can handle
        server_buffer_size = Math.min(server_buffer_size, server_receive_buffer_size);

        // Now that server has contacted us and awaits a reply,
        // client needs to send validation response.
        // If server does not receive validation response within 5 seconds,
        // it will send a CMD_VALIDATED = 9 message with StatusOK and close the TCP connection.

        // Reply to Connection Validation request.
        logger.log(Level.FINE, () -> "Sending connection validation response, auth = " + auth);
        // Since send thread is not running, yet, send directly
        PVAHeader.encodeMessageHeader(send_buffer, PVAHeader.FLAG_NONE, PVAHeader.CMD_CONNECTION_VALIDATION, 4+2+2+1);
        final int start = send_buffer.position();

        // Inform server about our receive buffer size
        send_buffer.putInt(receive_buffer.capacity());

        // Unclear, just echo the server's size
        send_buffer.putShort(server_introspection_registry_max_size);

        // QoS = Connection priority
        final short quos = 0;
        send_buffer.putShort(quos);

        // Selected authNZ plug-in
        auth.encode(send_buffer);

        // Correct payload size (depends on auth)
        final int end = send_buffer.position();
        send_buffer.putInt(PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE, end - start);

        send_buffer.flip();
        send(send_buffer);
    }

    void markValid() throws Exception
    {
        if (connection_validated.compareAndSet(false, true))
            startSender();
    }

    /** Close network socket and threads
     *  @param wait Wait for threads to end?
     */
    @Override
    public void close(final boolean wait)
    {
        alive_check.cancel(false);

        super.close(wait);
    }

    @Override
    public String toString()
    {
        return super.toString() + " " + guid;
    }
}
