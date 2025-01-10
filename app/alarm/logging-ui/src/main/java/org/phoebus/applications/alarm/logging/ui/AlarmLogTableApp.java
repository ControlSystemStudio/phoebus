package org.phoebus.applications.alarm.logging.ui;

import javafx.scene.image.Image;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.javafx.ImageCache;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AlarmLogTableApp implements AppResourceDescriptor {

    public static final Logger logger = Logger.getLogger(AlarmLogTableApp.class.getName());
    public static final String NAME = "Alarm Log Table";
    public static final String DISPLAYNAME = "Alarm Log Table";

    public static final String SUPPORTED_SCHEMA = "alarmLog";

    public static final Image icon = ImageCache.getImage(AlarmLogTableApp.class, "/icons/alarmtable.png");

    private HttpClient httpClient;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AppInstance create() {
        return new AlarmLogTable(this, null);
    }

    /**
     * Support the launching of alarmLogtable using resource {@literal alarmLog://?<search_string>}
     * e.g.
     * -resource alarmLog://?pv=SR*
     */
    @Override
    public AppInstance create(URI resource) {
        AlarmLogTable alarmLogTable = new AlarmLogTable(this, resource);
        return alarmLogTable;
    }

    @Override
    public boolean canOpenResource(String resource) {
        return URI.create(resource).getScheme().equals(SUPPORTED_SCHEMA);
    }

    @Override
    public void start() {

        try {
            String protocol = URI.create(Preferences.service_uri).getAuthority().toLowerCase().equals("https") ? "https" : "http";
            if ("https".equals(protocol)) {
                TrustManager PROMISCUOUS_TRUST_MANAGER = new X509ExtendedTrustManager() {
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }
                };

                SSLContext sslContext = SSLContext.getInstance("SSL"); // OR TLS
                sslContext.init(null, new TrustManager[]{PROMISCUOUS_TRUST_MANAGER}, new SecureRandom());
                httpClient = HttpClient.newBuilder().sslContext(sslContext).build();
            } else {
                httpClient = HttpClient.newBuilder().build();
            }

        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "Failed to properly create the elastic rest client to: " + Preferences.service_uri
                    , e);
        }
    }

    @Override
    public void stop() {

    }

    public HttpClient httpClient() {
        return httpClient;
    }
}
