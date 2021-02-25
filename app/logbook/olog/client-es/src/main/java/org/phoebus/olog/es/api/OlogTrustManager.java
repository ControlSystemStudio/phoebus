/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.olog.es.api;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ideas stolen from https://github.com/escline/InstallCert.
 */
public class OlogTrustManager {

    private static final Logger LOGGER = Logger.getLogger(OlogTrustManager.class.getName());

    /**
     * This is the default password for the JVM certificate store
     */
    private static final String CACERTS_PASSWORD = "changeit";

    /**
     * Attempts to download certificates as presented by the remote service. If the remote service does not
     * resent any certificates (unlikely?), or if the presented certificates are trusted, this method does nothing.
     * If on the other hand the certificates are not trusted, they are imported into the JVM trust store. Note
     * however that the augmented trust store is in-memory only, a key/certificate file is NOT created.
     * @param ologHost Host name of the remote service, no protocol prefix, no trailing path.
     * @param ologPort The port number. A value of -1 will be interpreted as default HTTPS port 443.
     */
    public static void setupSSLTrust(String ologHost, int ologPort) {

        File cacertsDir = new File(System.getProperty("java.home") + File.separatorChar + "lib" + File.separatorChar + "security");
        LOGGER.log(Level.INFO, String.format("Loading default JVM certificates from %s", cacertsDir.getAbsolutePath()));

        // Certificates *may* be found in a file named jssecacerts. If not, assume cacerts.
        File cacertsFile = new File(cacertsDir, "jssecacerts");
        if(!cacertsFile.isFile()){
            cacertsFile = new File(cacertsDir, "cacerts");
        }
        if(!cacertsFile.isFile()){
            LOGGER.log(Level.INFO, "No certificate store found, skipping certificate installation.");
            return;
        }

        try(InputStream in = new FileInputStream(cacertsFile)){

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(in, CACERTS_PASSWORD.toCharArray());
            SSLContext context = SSLContext.getInstance("TLS");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            X509TrustManager defaultTrustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
            SavingTrustManager trustManager = new SavingTrustManager(defaultTrustManager);
            context.init(null, new TrustManager[] { trustManager}, null);
            SSLSocketFactory sslSocketFactory = context.getSocketFactory();

            try(SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(ologHost, ologPort == -1 ? 443 : ologPort)){
                socket.setSoTimeout(5000);
                LOGGER.log(Level.INFO, String.format("Attempting SSL handshake against %s", ologHost));
                socket.startHandshake();
                LOGGER.log(Level.INFO, "SSL Handshake succeeded, server certificate already trusted.");
                return;
            }
            catch(SSLException sslException){
                LOGGER.log(Level.INFO, "SSL Handshake failed, certificate is not trusted, will be imported.");
            }

            // The custom TrustManager holds a reference to the certificates as "downloaded" from
            // the remote service URL.
            X509Certificate[] chain = trustManager.certificateChain;
            if (chain == null) {
                LOGGER.log(Level.SEVERE, "Could not obtain server certificate chain.");
                return;
            }
            LOGGER.log(Level.INFO,String.format("Server sent %d certificate(s)", chain.length));
            for (int i = 0; i < chain.length; i++) {
                X509Certificate cert = chain[i];
                LOGGER.log(Level.INFO, String.format("%d Subject %s", i + 1, cert.getSubjectDN()));
                LOGGER.log(Level.INFO,String.format("%d Issuer %s", i + 1, cert.getIssuerDN()));
                String alias = ologHost + i;
                keyStore.setCertificateEntry(alias, cert);
            }

            // Now the key store should contain additional certificates, if successfully downloaded.
            // Initialize the trust factory and SSL context again with the augmented key store.
            trustManagerFactory.init(keyStore);
            defaultTrustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
            context.init(null, new TrustManager[] { defaultTrustManager }, null);
            SSLContext.setDefault(context);
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unexpected error occured during certificate import", e);
        }
    }

    /**
     * A {@link TrustManager} used to obtain the certificate chain from the remote service that is
     * using an untrusted certificate.
     */
    private static class SavingTrustManager implements X509TrustManager {

        private final X509TrustManager trustManager;
        private X509Certificate[] certificateChain;

        /**
         * @param trustManager The default JVM {@link TrustManager}.
         */
        SavingTrustManager(X509TrustManager trustManager) {
            this.trustManager = trustManager;
        }

        public X509Certificate[] getAcceptedIssuers() {
            return this.trustManager.getAcceptedIssuers();
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException{
            this.trustManager.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] certificateChain, String authType) throws CertificateException{
            this.certificateChain = certificateChain;
            trustManager.checkServerTrusted(certificateChain, authType);
        }
    }
}
