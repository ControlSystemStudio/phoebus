package org.phoebus.applications.alarm.logging.ui;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.javafx.ImageCache;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.image.Image;

public class AlarmLogTableApp implements AppResourceDescriptor {

    public static final Logger logger = Logger.getLogger(AlarmLogTableApp.class.getName());
    public static final String NAME = "Alarm Log Table";
    public static final String DISPLAYNAME = "Alarm Log Table";

    public static final String SUPPORTED_SCHEMA = "alarmLog";

    public static final Image icon = ImageCache.getImage(AlarmLogTableApp.class, "/icons/alarmtable.png");

    private PreferencesReader prefs;
    private WebResource alarmResource;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AppInstance create() {
        return new AlarmLogTable(this);
    }

    /**
     * Support the launching of alarmLogtable using resource alarmLog://?<search_string>
     * e.g.
     * -resource alarmLog://?pv=SR*
     */
    @Override
    public AppInstance create(URI resource) {
        AlarmLogTable alarmLogTable = new AlarmLogTable(this);
        //alarmLogTable.s
        return alarmLogTable;
    }

    @Override
    public boolean canOpenResource(String resource) {
        return URI.create(resource).getScheme().equals(SUPPORTED_SCHEMA);
    }

    @Override
    public void start() {
        prefs = new PreferencesReader(AlarmLogTableApp.class, "/alarm_logging_preferences.properties");
        String serviceUri = prefs.get("service_uri");
        String protocol = URI.create(serviceUri).getAuthority().toLowerCase().equals("https") ? "https" : "http";
        ClientConfig clientConfig = new DefaultClientConfig();
        try {
            logger.info("Creating a alarm logging rest client to : " + serviceUri);
            if (protocol.equalsIgnoreCase("https")) { //$NON-NLS-1$
                if (clientConfig == null) {
                    SSLContext sslContext = null;
                    try {
                        sslContext = SSLContext.getInstance("SSL"); //$NON-NLS-1$
                        //sslContext.init(null, this.trustManager, null);
                    } catch (NoSuchAlgorithmException e) {
                        logger.log(Level.SEVERE, "failed to create the alarm logging rest client : " + e.getMessage(), e);
                    }
                    clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
                            new HTTPSProperties(new HostnameVerifier() {

                                @Override
                                public boolean verify(String hostname, SSLSession session) {
                                    return true;
                                }
                            }, sslContext));
                }
            }
            Client client = Client.create(clientConfig);
            client.setFollowRedirects(true);
            alarmResource = client.resource(serviceUri.toString());

            // TODO add a preference to add logging
            if (prefs.getBoolean("rawFiltering")) {
                //client.addFilter(new RawLoggingFilter(Logger.getLogger(RawLoggingFilter.class.getName())));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "Failed to properly create the elastic rest client to: " + prefs.get("service_uri")
                    , e);
        }
    }

    @Override
    public void stop() {

    }

    public WebResource getClient() {
        return this.alarmResource;
    }
}
