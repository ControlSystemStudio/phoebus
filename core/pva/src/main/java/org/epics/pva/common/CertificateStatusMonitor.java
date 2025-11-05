/*******************************************************************************
 * Copyright (c) 2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.epics.pva.common;

import static org.epics.pva.PVASettings.logger;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.SingleResp;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.epics.pva.client.ClientChannelState;
import org.epics.pva.client.PVAChannel;
import org.epics.pva.client.PVAClient;
import org.epics.pva.common.SecureSockets.TLSHandshakeInfo;
import org.epics.pva.data.Hexdump;
import org.epics.pva.data.PVAByteArray;
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
        private final X509Certificate certificate;
        private final String peer_name;
        private final PVAChannel pv;
        private String status = null;

        CertificateStatus(final X509Certificate certificate, final String status_pv_name)
        {
            this.certificate = certificate;
            this.peer_name = certificate.getSubjectX500Principal().getName();
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
            // Check overall status enum: VALID or UNKNOWN, PENDING, REVOKED, ...
            final PVAEnum value = PVAEnum.fromStructure(data.get("value"));
            if (value != null)
                status = value.enumString();
            else
                status = "UNKNOWN";
            logger.log(Level.FINE, () -> "Received " + channel.getName() + " = " + status);
            logger.log(Level.FINER, () -> data.toString());

            try
            {
                // Check OCSP Response bundled in the PVA structure
                final PVAByteArray raw = data.get("ocsp_response");
                if (raw == null)
                    throw new Exception("Missing 'ocsp_response' in " + data);

                // Is it a successful OCSP response ...
                final OCSPResp ocsp_response = new OCSPResp(raw.get());
                if (ocsp_response.getStatus() != OCSPResp.SUCCESSFUL)
                    throw new Exception("OCSP Response status " + ocsp_response.getStatus());
                // ...with "basic" info?
                if (! (ocsp_response.getResponseObject() instanceof BasicOCSPResp))
                    throw new Exception("Expected BasicOCSPResp, got " + ocsp_response.getResponseObject());
                final BasicOCSPResp basic = (BasicOCSPResp) ocsp_response.getResponseObject();
                logger.log(Level.FINER, () -> "OCSP responder " + basic.getResponderId().toASN1Primitive().getName());

                // Validate against certificates in our key chain
                boolean valid = false;
                for (X509Certificate x509 : SecureSockets.keychain_x509_certificates.values())
                    if (basic.isSignatureValid(new JcaContentVerifierProviderBuilder().build(x509)))
                    {
                        logger.log(Level.FINER, () -> "OCSP response verified by " + x509.getSubjectX500Principal());
                        valid = true;
                        break;
                    }
                if (! valid)
                    throw new Exception("Cannot validate OCSP response");

                // AuthorityKeyIdentifier, public key of "EPICS Root Certificate Authority"
                final JcaX509CertificateHolder bc_cert = new JcaX509CertificateHolder(certificate);
                final byte[] authority_key_id = AuthorityKeyIdentifier.fromExtensions(bc_cert.getExtensions()).getKeyIdentifierOctets();
                if (authority_key_id == null)
                    throw new Exception("Cannot get AuthorityKeyIdentifier from " + certificate);

                // OCSP can include one or more responses. Find one that confirms the certificate
                boolean ocsp_confirmation = false;
                for (SingleResp response : basic.getResponses())
                {
                    // Is response for the certificate we want to check?
                    // Same authority?
                    final CertificateID id = response.getCertID();
                    if (! Arrays.equals(authority_key_id, id.getIssuerKeyHash()))
                    {
                        logger.log(Level.FINER, () -> "OCSP authority\n" + Hexdump.toHexdump(id.getIssuerKeyHash()) +
                                                      "\ndiffers from\n"  + Hexdump.toHexdump(authority_key_id));
                        continue;
                    }

                    // Same serial number?
                    if (! id.getSerialNumber().equals(certificate.getSerialNumber()))
                    {
                        logger.log(Level.FINER, () -> "OCSP Serial: 0x" + id.getSerialNumber().toString(16) +
                                                      " differs from expected 0x" + certificate.getSerialNumber().toString(16));
                        continue;
                    }

                    // Is applicable time range from <= now <= until?   until may be null...
                    final Date now = new Date(), from = response.getThisUpdate(), until = response.getNextUpdate();
                    if (from.after(now)  ||  (until != null   &&   now.after(until)))
                    {
                        logger.log(Level.FINER, () -> "Applicable time range from " + response.getThisUpdate() +
                                                      " to " + response.getNextUpdate() + " does not include now, " + now);
                        continue;
                    }

                    // Seems to apply to the certificate we want to check.

                    // What is the status? OCSP only indicates null for valid, RevokedStatus with revocation date, or UnknownStatus
                    // Use that to potentially update the more detailed
                    final org.bouncycastle.cert.ocsp.CertificateStatus response_status = response.getCertStatus();
                    if (response_status == org.bouncycastle.cert.ocsp.CertificateStatus.GOOD)
                    {
                        logger.log(Level.FINER, "OCSP status is VALID");
                        status = "VALID";
                        ocsp_confirmation = true;
                        break;
                    }
                    else if (response_status instanceof RevokedStatus revoked)
                    {
                        logger.log(Level.FINER, "OCSP status is REVOKED as of " + revoked.getRevocationTime());
                        status = "REVOKED";
                        ocsp_confirmation = true;
                    }
                    else
                    {   // Allow PENDING etc. but correct VALID
                        logger.log(Level.FINER, "OCSP status is UNKNOWN");
                        if ("VALID".equals(status))
                            status = "UNKNOWN";
                    }
                }

                // Downgrade an unconfirmed VALID, but keep PENDING etc.
                if (! ocsp_confirmation  &&  "VALID".equals(status))
                    status = "UNKNOWN";
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot decode OCSP response for " + pv.getName(), ex);
                status = "ERROR";
            }

            logger.log(Level.FINE, () -> "Effective " + channel.getName() + " = " + status);


            // Notify listeners
            for (var listener : listeners)
                listener.handleCertificateStatusUpdate(this);
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

    /** @param tls_info {@link TLSHandshakeInfo}: certificate, CERT:STATUS:... PV name
     *  @param listener Listener to invoke for certificate status updates
     *  @return {@link CertificateStatus} to which we're subscribed, need to unsubscribe when no longer needed
     */
    public synchronized CertificateStatus checkCertStatus(final TLSHandshakeInfo tls_info,final CertificateStatusListener listener)
    {
        if (!tls_info.status_pv_name.startsWith("CERT:STATUS:"))
            throw new IllegalArgumentException("Need CERT:STATUS:... PV");

        logger.log(Level.FINER, () -> "Checking " + tls_info.status_pv_name + " for '" + tls_info.name + "'");

        CertificateStatus cert_stat = certificate_states.computeIfAbsent(tls_info.status_pv_name,
                                                                         stat_pv_name -> new CertificateStatus(tls_info.peer_cert, tls_info.status_pv_name));
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
