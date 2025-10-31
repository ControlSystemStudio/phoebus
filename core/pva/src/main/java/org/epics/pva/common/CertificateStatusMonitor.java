/*******************************************************************************
 * Copyright (c) 2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.epics.pva.common;

import static org.epics.pva.PVASettings.logger;

import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.epics.pva.client.ClientChannelState;
import org.epics.pva.client.PVAChannel;
import org.epics.pva.client.PVAClient;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.nt.PVAEnum;

/** Monitors the 'CERT:STATUS:...' PV for a certificate
 *
 *  <p>A certificate might be valid based on its expiration date,
 *  but PVACMS can list a 'CERT:STATUS:...' PV in the certificate
 *  that tools which use the certificate are expected to monitor.
 *
 *  <p>Without a 'VALID' state confirmed by the 'CERT:STATUS:...' PV,
 *  the certificate should not be used for authentication.
 *
 *  <p>A server, for example, should consider the client 'anonymous'
 *  until the CERT:STATUS PV declares the certificate 'VALID',
 *  at which time the certificate will authenticate the principal user
 *  listed in the cert.
 *
 *  <p>On a 'REVOKED' update, the server should again ignore the authentication
 *  info from the certificate and consider the peer anonymous.
 *
 *  <p>Several client tools or IOCs might use the same certificate.
 *  This singleton performs the check only once per certificate
 *  and updates several listeners when the certificate validity changes.
 *
 *  @author Kay Kasemir
 */
public class CertificateStatusMonitor
{
    /** Singleton instance */
    private static CertificateStatusMonitor instance = null;

    /** Map from CERT:STATUS:... PV name to CertificateStatus of that PV */
    private final ConcurrentHashMap<String, CertificateStatus> certificate_states = new ConcurrentHashMap<>();

    /** PVA Client used for all CERT:STATUS:... PVs */
    private PVAClient client = null;

    /** Certificate status: Valid or not? */
    public class CertificateStatus
    {
        private final CopyOnWriteArrayList<CertificateStatusListener> listeners = new CopyOnWriteArrayList<>();
        private final String peer_name;
        private final PVAChannel pv;
        private String status = null;

        CertificateStatus(final String peer_name, final String status_pv_name)
        {
            this.peer_name = peer_name;
            pv = client.getChannel(status_pv_name, this::handleConnection);
        }

        /** @return CERT:STATUS:... PV name */
        public String getPVName()
        {
            return pv.getName();
        }

        /** @param listener Listener to add (with initial update) */
        void addListener(final CertificateStatusListener listener)
        {
            listeners.add(listener);
            // Send initial update
            logger.log(Level.FINER, "Initial " + getPVName() + " update");
            listener.handleCertificateStatusUpdate(this);
        }

        /** @param listener Listener to remove
         *  @return Was that the last listener, can CertificateStatus be removed?
         */
        boolean removeListener(final CertificateStatusListener listener)
        {
            if (! listeners.remove(listener))
                throw new IllegalStateException("Unknown CertificateStatusListener");
            return listeners.isEmpty();
        }

        /** @return Is the certificate currently valid? */
        public boolean isValid()
        {
            return "VALID".equals(status);
        }

        private void handleConnection(final PVAChannel channel, final ClientChannelState state)
        {
            if (state == ClientChannelState.CONNECTED)
                try
                {
                    channel.subscribe("", this::handleMonitor);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot subscribe to " + pv, ex);
                }
        }

        private void handleMonitor(final PVAChannel channel, final BitSet changes, final BitSet overruns, final PVAStructure data)
        {
            // TODO also check string ocsp_certified_until Mon Sep 22 19:37:25 2025 UTC?
            // TODO Can those be time_t secondsPastEpoch?
            // Decode overall status enum, VALID or not?
            final PVAEnum value = PVAEnum.fromStructure(data.get("value"));
            if (value != null)
            {
                status = value.enumString();
                logger.log(Level.FINER, () -> this.toString());

                // Notify listeners
                for (var listener : listeners)
                    listener.handleCertificateStatusUpdate(this);
            }
            else
                logger.log(Level.WARNING, pv + " failed to send status, got " + data);
        }

        /** Close the CERT:STATUS:... PV check */
        void close()
        {
            if (! listeners.isEmpty())
                throw new IllegalStateException(getPVName() + " is still in use");
            pv.close();
        }

        @Override
        public String toString()
        {
            return pv.getName() + " for '" + peer_name + "' is " + status;
        }
    }

    /** Constructor of the singleton instance */
    private CertificateStatusMonitor()
    {
        try
        {
            client = new PVAClient();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create PVAClient for CERT:STATUS:... monitor", ex);
        }
    }

    // Synchronization:
    //
    // - Creating/getting the singleton
    // - checkCertStatus: Creates or gets CertificateStatus for PV name, adds listener
    // - remove: Removes listener, closes CertificateStatus on removal of last listener
    //
    // Late CERT:STATUS.. monitor will call all listeners using a safe CopyOnWriteArray list

    /** @return Singleton instance */
    public static synchronized CertificateStatusMonitor instance()
    {
        if (instance == null)
            instance = new CertificateStatusMonitor();
        return instance;
    }

    /** @param status_pv_name CERT:STATUS:... PV name
     *  @param peer_name Name of the peer (principal of the certificate)
     *  @param listener Listener to invoke for certificate status updates
     *  @return {@link CertificateStatus} to which we're subscribed, need to unsubscribe when no longer needed
     */
    public synchronized CertificateStatus checkCertStatus(final String status_pv_name, final String peer_name, final CertificateStatusListener listener)
    {
        if (!status_pv_name.startsWith("CERT:STATUS:"))
            throw new IllegalArgumentException("Need CERT:STATUS:... PV");

        logger.log(Level.FINER, () -> "Checking " + status_pv_name + " for '" + peer_name + "'");

        CertificateStatus cert_stat = certificate_states.computeIfAbsent(status_pv_name,
                                                                         stat_pv_name -> new CertificateStatus(peer_name, status_pv_name));
        cert_stat.addListener(listener);

        return cert_stat;
    }

    public synchronized void remove(final CertificateStatus certificate_status, final CertificateStatusListener listener)
    {
        if (certificate_status.removeListener(listener))
        {
            logger.log(Level.FINER, () -> "Stopping check of " + certificate_status.getPVName());
            certificate_status.close();
            if (! certificate_states.remove(certificate_status.getPVName(), certificate_status))
                throw new IllegalStateException("Unknown certificate status " + certificate_status);
        }
    }
}
