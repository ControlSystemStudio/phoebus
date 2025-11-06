/*******************************************************************************
 * Copyright (c) 2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import static org.epics.pva.PVASettings.logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.epics.pva.client.PVAClient;
import org.epics.pva.common.SecureSockets.TLSHandshakeInfo;

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
    // Most of the work is done in the CertificateStatus.
    // This class holds the common PVAClient and handles
    // the synchronization of creating and removing cert status checks.

    /** Singleton instance */
    private static CertificateStatusMonitor instance = null;

    /** Map from CERT:STATUS:... PV name to CertificateStatus of that PV */
    private final ConcurrentHashMap<String, CertificateStatus> certificate_states = new ConcurrentHashMap<>();

    /** PVA Client used for all CERT:STATUS:... PVs */
    private PVAClient client = null;

    /** Constructor of the singleton instance */
    private CertificateStatusMonitor()
    {
        try
        {
            client = new PVAClient();
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Cannot create PVAClient for CERT:STATUS:... monitor", ex);
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

    /** @param tls_info {@link TLSHandshakeInfo}: certificate, CERT:STATUS:... PV name
     *  @param listener Listener to invoke for certificate status updates
     *  @return {@link CertificateStatus} to which we're subscribed, need to unsubscribe when no longer needed
     */
    public synchronized CertificateStatus checkCertStatus(final TLSHandshakeInfo tls_info,final CertificateStatusListener listener)
    {
        if (!tls_info.status_pv_name.startsWith("CERT:STATUS:"))
            throw new IllegalArgumentException("Need CERT:STATUS:... PV, got " + tls_info.status_pv_name);

        logger.log(Level.FINER, () -> "Checking " + tls_info.status_pv_name + " for '" + tls_info.name + "'");

        final CertificateStatus cert_stat = certificate_states.computeIfAbsent(tls_info.status_pv_name,
                stat_pv_name -> new CertificateStatus(client, tls_info.peer_cert, tls_info.status_pv_name));
        cert_stat.addListener(listener);

        return cert_stat;
    }

    /** Unsubscribe from certificate status updates
     *  @param certificate_status Certificate status from which to unsubscribe
     *  @param listener Listener to cancel
     */
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
