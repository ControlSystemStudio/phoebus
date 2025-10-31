/*******************************************************************************
 * Copyright (c) 2019-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import static org.epics.pva.PVASettings.logger;

import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.epics.pva.common.AccessRightsChange;
import org.epics.pva.common.PVAAuth;
import org.epics.pva.common.PVAHeader;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;

/** A served PV with data
 *
 *  <p>When updating the data, subscribed clients
 *  receive the changed data elements.
 *
 *  @author Kay Kasemir
 */
public class ServerPV implements AutoCloseable
{
    /** Value used when accessing an RPC PV as a data PV */
    private static final PVAStructure RPC_SERVICE_VALUE = new PVAStructure("", "", new PVAString("value", "This is an RPC server"));

    /** Value returned from RPC call to data PV */
    private static final PVAStructure NO_SERVICE_VALUE = new PVAStructure("", "", new PVAString("value", "This is no RPC server"));

    /** Service implementation when accessing a data PV as an RPC service */
    private static final RPCService DEFAULT_RPC_SERVICE = request -> NO_SERVICE_VALUE;

    /** {@link WriteEventHandler} for read-only PVs */
    static final WriteEventHandler READONLY_WRITE_HANDLER = (tcp, pv, changes, data) ->
    {
        throw new Exception("PV " + pv.getName() + " is read-only");
    };

    /** Generator of PV server IDs
     *
     *  If both client and server start with 1,
     *  tests don't clearly show which one is SID and CID.
     *  Starting with higher SID makes it easier to distinguish.
     */
    private static final AtomicInteger IDs = new AtomicInteger(10);

    /** Server that holds this PV */
    private final PVAServer server;

    /** Name of this PV */
    private final String name;

    /** Server ID of this PV */
    private final int sid;

    /** Current value
     *
     *  <p>Updates need to SYNC on data
     */
    private final PVAStructure data;

    /** Handler for RPC invocations. May be DEFAULT_RPC_SERVICE */
    private final RPCService rpc;

    /** Handler for write access. May be READONLY_WRITE_HANDLER */
    private final WriteEventHandler write_handler;

    /** Is the PV writable? */
    private final AtomicBoolean writable;

    /** Map of TCP handlers and client IDs.
     *  PV has one server ID.
     *  Client ID is provided by client for each TCP connection.
     */
    private final ConcurrentHashMap<ServerTCPHandler, Integer> cid_by_client = new ConcurrentHashMap<>();

    /** All the 'monitor' subscriptions to this PV */
    private final KeySetView<MonitorSubscription, Boolean> subscriptions = ConcurrentHashMap.newKeySet();

    /** Create a PV for serving data
     *  @param name PV name
     *  @param data Initial value
     *  @param write_handler Event handler for write access, or READONLY_WRITE_HANDLER
     */
    ServerPV(final PVAServer server, final String name, final PVAStructure data, final WriteEventHandler write_handler)
    {
        this.server = server;
        this.name = name;
        this.sid = IDs.incrementAndGet();
        this.data = data.cloneData();
        rpc = DEFAULT_RPC_SERVICE;
        this.write_handler = write_handler;
        writable = new AtomicBoolean(write_handler != READONLY_WRITE_HANDLER);
    }

    /** Create PV for handling RPC calls
     *  @param name PV name
     *  @param rpc {@link RPCService}
     */
    ServerPV(final PVAServer server, final String name, final RPCService rpc)
    {
        this.server = server;
        this.name = name;
        this.sid = IDs.incrementAndGet();
        this.data = RPC_SERVICE_VALUE;
        this.rpc = rpc;
        write_handler = READONLY_WRITE_HANDLER;
        writable = new AtomicBoolean(false);
    }


    /** @return Channel name */
    public String getName()
    {
        return name;
    }

    /** @return Channel's service ID */
    public int getSID()
    {
        return sid;
    }

    /** Register a client of this PV
     *  @param tcp TCP connection to client
     *  @param cid Client's ID for this PV
     */
    void addClient(final ServerTCPHandler tcp, final int cid)
    {
        // Each client should create a PV just once.
        // If client creates PV several times, we only track the last CID
        // and issue a warning.
        final Integer other = cid_by_client.put(tcp, cid);
        if (other == null)
            logger.log(Level.FINE, "Client " + tcp + " requested " + this + " [CID " + cid + "]");
        else
            logger.log(Level.WARNING, "Client " + tcp + " requested " + this + " as CID " + cid + " but also " + other);
    }

    /** Called by CertificateStatusMonitor via ServerTCPHandler and PVAServer
     *  @param tcp {@link ServerTCPHandler} with new client authentication info
     *  @param client_auth {@link ClientAuthentication}
     */
    void updatePermissions(final ServerTCPHandler tcp, final ClientAuthentication client_auth)
    {
        // Is PV accessed via that TCP/TLS connection?
        final Integer cid = cid_by_client.get(tcp);
        if (cid != null)
        {
            // Does the client have write access?
            final boolean writable = isWritable(client_auth);
            tcp.submit((version, buffer) ->
            {
                logger.log(Level.FINE, () ->  "Send ACL " + this + " [CID " + cid + "]" + (writable ? " writable" : " read-only"));
                AccessRightsChange.encode(buffer, cid, writable);
            });
        }
    }

    /** Un-register a client of this PV
     *  @param tcp TCP connection to client
     *  @param cid Client's ID for this PV (-1 to remove any)
     */
    void removeClient(final ServerTCPHandler tcp, final int cid)
    {
        // Stop associating PV with that TCP connection
        final Integer original_cid = cid_by_client.remove(tcp);
        // Did we never deal with this PV via that TCP connection?
        if (cid == -1  &&  original_cid == null)
            return;
        else if (cid == -1)
            logger.log(Level.FINE, "Client " + tcp + " released " + this + " [CID was " + original_cid + "]");
        else if (original_cid != null  &&  original_cid.intValue() == cid)
            logger.log(Level.FINE, "Client " + tcp + " released " + this + " [CID " + cid + "]");
        else
            // Our memory of the cid differs from what the client now uses to release the PV?!?
            logger.log(Level.WARNING, "Client " + tcp + " released " + this + " as CID " + cid + " instead of " + original_cid);

        // Delete all subscriptions to this PV from that TCP connection
        // A perfect client would separately clear the subscription,
        // but this asserts they're all gone for sure
        unregisterSubscription(tcp, -1);
    }

    /** @param subscription Subscription that needs to receive value updates */
    void registerSubscription(final MonitorSubscription subscription)
    {
        logger.log(Level.FINER, () -> "Add " + subscription);
        subscriptions.add(subscription);
    }

    /** Forget monitor subscriptions
     *  @param tcp TCP connection for which to forget monitors
     *  @param req Specific monitor request or -1 to forget subscriptions for that connection
     */
    void unregisterSubscription(final ServerTCPHandler tcp, final int req)
    {
        subscriptions.removeIf(subscription ->
        {
            if (subscription.isFor(tcp, req))
            {
                logger.log(Level.FINER, () -> "Remove " + subscription);
                return true;
            }
            return false;
        });
        logger.log(Level.FINEST, () -> "There are " + subscriptions.size() + " remaining subscriptions");
    }

    /** @return Does the PV have client subscriptions? */
    public boolean isSubscribed()
    {
        return ! subscriptions.isEmpty();
    }

    /** Update the PV's data
     *
     *  <p>The new data is used to update the current
     *  value of the PV.
     *  Its type must match the initial value used when
     *  creating the PV on the server.
     *
     *  @param new_data New data to serve
     *  @throws Exception on error
     */
    public void update(final PVAStructure new_data) throws Exception
    {
        // Update data
        synchronized (data)
        {
            data.update(new_data);
        }
        // Update subscriptions
        for (MonitorSubscription subscription : subscriptions)
            subscription.update(new_data);
    }

    /** Get current value (thread-safe copy)
     *  @return PV's current data
     */
    PVAStructure getData()
    {
        synchronized (data)
        {
            return data.cloneData();
        }
    }

    /** @param client_auth Client authentication
     *  @return Is the PV writable by that client?
     */
    public boolean isWritable(final ClientAuthentication client_auth)
    {
        // For now, as long as PV supports write access,
        // any authenticated user (CA or X509) can write
        // TODO Check user in ServerAuthorization
        return writable.get()  &&  client_auth.getType() != PVAAuth.anonymous;
    }

    /** Update write access
     *
     *  To enable write access, PV must have been created with {@link WriteEventHandler}
     *
     *  @param writable Should the PV be writable?
     */
    public void setWritable(final boolean writable)
    {
        // Change in overall write support of this PV?
        if (write_handler != READONLY_WRITE_HANDLER  &&  this.writable.compareAndSet(!writable, writable))
        {
            // For each TCP/TLS connection, get authenticated user and compute access rights
            logger.log(Level.FINE, () ->  "Update ACL " + this + (writable ? " to writable" : " to read-only"));
            cid_by_client.forEach((tcp, cid) ->
            {
                boolean effective = isWritable(tcp.getClientAuthentication());
                tcp.submit((version, buffer) -> AccessRightsChange.encode(buffer, cid, effective));
            });
        }
    }

    /** Notification that a client wrote to the PV
     *  @param tcp {@link ServerTCPHandler} that received the 'write'
     *  @param changes Elements that the client tried to change
     *  @param written_data Data written by the client
     *  @throws Exception on error
     */
    void wrote(final ServerTCPHandler tcp, final BitSet changes, final PVAStructure written_data) throws Exception
    {
        write_handler.handleWrite(tcp, this, changes, written_data);
    }

    /** Invoke RPC service
     *  @param parameters RPC parameters
     *  @return RPC result
     *  @throws Exception on error, for example invalid parameters
     */
    PVAStructure call(final PVAStructure parameters) throws Exception
    {
        return rpc.call(parameters);
    }

    /** Close PV, i.e. remove from server */
    @Override
    public void close()
    {
        cid_by_client.forEach((tcp, cid) ->
            tcp.submit( (version, buffer) ->
            {   // Send CMD_DESTROY_CHANNEL for this PV to all clients
                logger.log(Level.FINE, () -> "Sending destroy channel command for SID " + sid + ", CID " + cid);
                PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_SERVER, PVAHeader.CMD_DESTROY_CHANNEL, 4+4);
                buffer.putInt(sid);
                buffer.putInt(cid);
            }));

        server.deletePV(this);
    }

    @Override
    public String toString()
    {
        return name + " [SID " + sid + "]";
    }
}
