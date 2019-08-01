package org.phoebus.olog.api;
/**
 * Taken from http://java.sun.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html
 *
 */
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

class MyX509TrustManager implements X509TrustManager {

     /*
      * The default PKIX X509TrustManager9.  We'll delegate
      * decisions to it, and fall back to the logic in this class if the
      * default X509TrustManager doesn't trust it.
      */
     X509TrustManager pkixTrustManager;

     MyX509TrustManager(String trustStore, char[] password) throws Exception {    	 
         this(new File(trustStore), password);
     }

     MyX509TrustManager(File trustStore, char[] password) throws Exception {
         // create a "default" JSSE X509TrustManager.
         KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

         ks.load(new FileInputStream(trustStore), password);

         TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
         tmf.init(ks);

         TrustManager tms [] = tmf.getTrustManagers();

         /*
          * Iterate over the returned trustmanagers, look
          * for an instance of X509TrustManager.  If found,
          * use that as our "default" trust manager.
          */
         for (int i = 0; i < tms.length; i++) {
             if (tms[i] instanceof X509TrustManager) {
                 pkixTrustManager = (X509TrustManager) tms[i];
                 return;
             }
         }

         /*
          * Find some other way to initialize, or else we have to fail the
          * constructor.
          */
         throw new Exception("Couldn't initialize");
     }

     /*
      * Delegate to the default trust manager.
      */
     public void checkClientTrusted(X509Certificate[] chain, String authType)
                 throws CertificateException {
         try {
             pkixTrustManager.checkClientTrusted(chain, authType);
         } catch (CertificateException excep) {
             // do any special handling here, or rethrow exception.
         }
     }

     /*
      * Delegate to the default trust manager.
      */
     public void checkServerTrusted(X509Certificate[] chain, String authType)
                 throws CertificateException {
         try {
             pkixTrustManager.checkServerTrusted(chain, authType);
         } catch (CertificateException excep) {
             /*
              * Possibly pop up a dialog box asking whether to trust the
              * cert chain.
              */
         }
     }

     /*
      * Merely pass this through.
      */
     public X509Certificate[] getAcceptedIssuers() {
         return pkixTrustManager.getAcceptedIssuers();
     }
}
