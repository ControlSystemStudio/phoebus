/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import static org.epics.pva.PVASettings.logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;

/** Client channel
 *
 *  <p>Obtained via {@link PVAClient#getChannel(String)}.
 *  Allows reading and writing a channel's data.
 *
 *  <p>Channel 'connects' automatically.
 *  When obtaining the channel from the {@link PVAClient},
 *  a listener can be provided to receive connection state updates.
 *  In addition, {@link PVAChannel#connect()} can be used to await
 *  connection, or {@link PVAChannel#isConnected()} can be used to check
 *  the current state.
 *
 *  <p>Once connected, a channel can {@link #read(String)} or {@link #write(String, Object)} data,
 *  {@link #subscribe(String, MonitorListener)} to value updates, or {@link #invoke(PVAStructure)} RPC calls.
 *
 *  <p>When no longer in use, the channel should be {@link #close()}d.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAChannel implements AutoCloseable
{
    /** Provider for the 'next' client channel ID
     *
     *  <p>Starts at 1, i.e. the first channel will use CID 2.
     *  Since the server tends to start at 1, this makes it
     *  more obvious in tests which ID is the SID
     *  and which is the CID.
     */
    private static final AtomicInteger CID_Provider = new AtomicInteger(1);

    private final PVAClient client;
    private final String name;
    private final ClientChannelListener listener;
    private final int cid = CID_Provider.incrementAndGet();
    private volatile int sid = -1;

    /** State
     *
     *  {@#awaitConnection()} SYNCs on state, so notifyAll() when changing
     */
    private final AtomicReference<ClientChannelState> state = new AtomicReference<>(ClientChannelState.INIT);

    /** Completes with <code>true</code> when state == CONNECTED */
    private CompletableFuture<Boolean> connected = new CompletableFuture<>();

    /** TCP Handler, set by PVAClient */
    final AtomicReference<ClientTCPHandler> tcp = new AtomicReference<>();

    private final CopyOnWriteArrayList<MonitorRequest> subscriptions = new CopyOnWriteArrayList<>();

    PVAChannel(final PVAClient client, final String name, final ClientChannelListener listener)
    {
        this.client = client;
        this.name = name;
        this.listener = listener;
    }

    PVAClient getClient()
    {
        return client;
    }

    ClientTCPHandler getTCP() throws Exception
    {
        final ClientTCPHandler copy = tcp.get();
        if (copy == null)
            throw new Exception("Channel '" + name + "' is not connected");
        return copy;
    }

    /** @return Client channel ID */
    int getCID()
    {
        return cid;
    }

    /** @return Server channel ID */
    int getSID()
    {
        return sid;
    }


    /** @return Channel name */
    public String getName()
    {
        return name;
    }

    /** @return {@link ClientChannelState} */
    public ClientChannelState getState()
    {
        return state.get();
    }

    /** @return <code>true</code> if channel is connected */
    public boolean isConnected()
    {
        return getState() == ClientChannelState.CONNECTED;
    }

    /** Wait for channel to connect
     *  @return {@link Future} to await connection.
     *          <code>true</code> on success,
     *          {@link TimeoutException} on timeout.
     */
    public CompletableFuture<Boolean> connect()
    {
        return connected;
    }

    /** Set channel state, notify listeners on change
     *  @param new_state
     *  @return old state
     */
    ClientChannelState setState(final ClientChannelState new_state)
    {
        final ClientChannelState old = state.getAndSet(new_state);
        if (old != new_state)
        {
            if (new_state == ClientChannelState.CONNECTED)
                connected.complete(true);
            else if (old == ClientChannelState.CONNECTED)
                connected = new CompletableFuture<>();

            synchronized (state)
            {
                state.notifyAll();
            }
            listener.channelStateChanged(this, new_state);
        }
        return old;
    }

    /** Register channel on server
     *
     *  <p>Asks server to create channel
     *  on this TCP connection.
     *
     *  @param tcp {@link ClientTCPHandler}
     */
    void registerWithServer(final ClientTCPHandler tcp)
    {
        final ClientTCPHandler old = this.tcp.getAndSet(tcp);
        if (old != null)
            logger.log(Level.WARNING, this + " was already on " + old + ", now added to " + tcp);
        tcp.addChannel(this);

        // Enqueue request to create channel.
        // TCPHandler will perform it when connected,
        // or right away if it is already connected.
        tcp.submit(new CreateChannelRequest(this));
    }

    /** Called when CreateChannelRequest succeeds
     *  @param sid Server ID for this channel
     */
    void completeConnection(final int sid)
    {
        if (state.compareAndSet(ClientChannelState.FOUND, ClientChannelState.CONNECTED))
        {
            this.sid = sid;
            logger.log(Level.FINE, () -> "Received create channel reply " + this + ", SID " + sid);
            connected.complete(true);
            synchronized (state)
            {
                state.notifyAll();
            }
            listener.channelStateChanged(this, ClientChannelState.CONNECTED);
        }
        // Else: Channel was destroyed or closed, ignore the late connection
    }

    /** Connection lost, detach from {@link ClientTCPHandler}
     *  @return <code>true</code> if the channel was searched or connected,
     *          <code>false</code> if it was closing or closed
     */
    boolean resetConnection()
    {
        clearSubscriptions();
        final ClientTCPHandler old = tcp.getAndSet(null);
        if (old != null)
            old.removeChannel(this);
        final ClientChannelState old_state = setState(ClientChannelState.INIT);
        return ClientChannelState.isActive(old_state);
    }

    /** Read (get) channel's type info from server
     *
     *  <p>Returned {@link PVAStructure} only describes the type,
     *  the values are not set.
     *
     *  @param subfield Sub field to get, "" for complete data type
     *  @return {@link Future} for fetching the result
     */
    public Future<PVAStructure> info(final String subfield)
    {
        return new GetTypeRequest(this, subfield);
    }

    /** Read (get) channel's value from server
     *  @param request Request, "" for all fields, or "field_a, field_b.subfield"
     *  @return {@link Future} for fetching the result
     */
    public Future<PVAStructure> read(final String request)
    {
        return new GetRequest(this, request);
    }

    /** Write (put) an element of the channel's value on server
     *
     *  <p>The request needs to address one field of the channel,
     *  and the value to write must be accepted by that field.
     *
     *  <p>For example, when "field(value)" addresses a double field,
     *  {@link PVADouble#setValue(Object)} will be called, so <code>new_value</code>
     *  may be a {@link Number}.
     *
     *  <p>When "field(value)" addresses a text field,
     *  {@link PVAString#setValue(Object)} will be called,
     *  which accepts any object by converting it to a string.
     *
     *  <p>When writing an enumerated field, its <code>int index</code>
     *  will be written, requiring a {@link Number} that's then
     *  used as an integer.
     *
     *  @param request Request for element to write, e.g. "field(value)"
     *  @param new_value New value: Number, String
     *  @throws Exception on error
     *  @return {@link Future} for awaiting completion
     */
    public Future<Void> write(final String request, final Object new_value) throws Exception
    {
        return new PutRequest(this, request, new_value);
    }

    /** Start a subscription
     *
     *  @param request Request, "" for all fields, or "field_a, field_b.subfield"
     *  @param listener Will be invoked with channel and latest value
     *  @return {@link AutoCloseable}, used to close the subscription
     *  @throws Exception on error
     */
    public AutoCloseable subscribe(final String request, final MonitorListener listener) throws Exception
    {
        return subscribe(request, 0, listener);
    }

    /** Start a pipelined subscription
     *
     *  <p>Asks the server to send a certain number of 'pipelined' updates.
     *  Client automatically requests more updates as soon as half the pipelined updates
     *  are received. In case the client gets overloaded and cannot do this,
     *  the server will thus pause after sending the pipelined updates.
     *
     *  @param request Request, "" for all fields, or "field_a, field_b.subfield"
     *  @param pipeline Number of updates to pipeline
     *  @param listener Will be invoked with channel and latest value
     *  @return {@link AutoCloseable}, used to close the subscription
     *  @throws Exception on error
     */
    public AutoCloseable subscribe(final String request, final int pipeline, final MonitorListener listener) throws Exception
    {
        // MonitorRequest submits itself to TCPHandler
        // and registers as response handler,
        // so we can later retrieve it via its requestID
        final MonitorRequest subscription = new MonitorRequest(this, request, pipeline, listener);
        subscriptions.add(subscription);
        return subscription;
    }

    /** Invoke remote procedure call (RPC)
     *  @param request Request, i.e. parameters sent to the RPC call
     *  @return {@link Future} for fetching the result returned by the RPC call
     */
    public Future<PVAStructure> invoke(final PVAStructure request)
    {
        return new RPCRequest(this, request);
    }

    /** Called when server confirms channel has been destroyed
     *  @param sid Server ID for channel
     */
    void channelDestroyed(final int sid)
    {
        if (sid != this.sid)
        {
            logger.log(Level.WARNING, "Server destroyed " + this + " with unexpected SID " + sid);
            return;
        }

        // If client is CLOSING this channel, then move to CLOSED and be done.
        if (state.compareAndSet(ClientChannelState.CLOSING, ClientChannelState.CLOSED))
        {
            synchronized (state)
            {
                state.notifyAll();
            }
            logger.log(Level.FINE, () -> "Server confirmed destroying channel " + this);
            listener.channelStateChanged(this, ClientChannelState.CLOSED);
            clearSubscriptions();
            client.forgetChannel(this);
        }
        else
        {
            // Server closed the channel.
            logger.log(Level.FINE, () -> "Server destroyed channel " + this);
            resetConnection();
            // Keep in client, revert to SEARCHING soon
            client.search.register(this, false);
        }
    }

    /** Remove all MonitorRequests for this channel from TCP handler,
     *  since we are not expecting any more updates.
     */
    private void clearSubscriptions()
    {
        final ClientTCPHandler copy = tcp.get();
        if (copy != null)
            for (MonitorRequest subscription : subscriptions)
                copy.removeResponseHandler(subscription.getRequestID());
        subscriptions.clear();
    }

    /** Close the channel */
    @Override
    public void close()
    {
        // In case channel is still being searched, stop
        client.search.unregister(getCID());

        // Indicate that channel is closing
        final ClientChannelState old_state = setState(ClientChannelState.CLOSING);
        if (old_state == ClientChannelState.SEARCHING)
        {   // Not connected, so forget the channel
            client.forgetChannel(this);
        }
        else
        {   // Try to destroy channel on server.
            final ClientTCPHandler safe = tcp.get();
            if (safe != null)
            {   // If we can still reach the server, it should confirm deletion,
                // and channelDestroyed() is invoked to forget the channel.
                // Depending on situation, however, we may no longer reach the server,
                // in which case the channel will not be forgotten
                // until the PVAClient is closed...
                safe.submit(new DestroyChannelRequest(this));
            }
            else
                client.forgetChannel(this);
        }
    }

    @Override
    public String toString()
    {
        return "'" + name + "' [CID " + cid + ", SID " + sid + " " + state.get() + "]";
    }
}
