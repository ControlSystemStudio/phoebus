package org.phoebus.applications.alarm.logging.ui;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.sniff.Sniffer;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.image.Image;

public class AlarmLogTableApp implements AppResourceDescriptor {

    public static final Logger logger = Logger.getLogger(AlarmLogTableApp.class.getName());
    public static final String NAME = "Alarm Log Table";
    public static final String DISPLAYNAME = "Alarm Log Table";

    public static final String SUPPORTED_SCHEMA = "alarmLog";
    
    public static final Image icon = ImageCache.getImage(AlarmLogTableApp.class, "/icons/alarmtable.png");

    private RestHighLevelClient client;
    private Sniffer sniffer;
    private PreferencesReader prefs;

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
        try {
            client = new RestHighLevelClient(
                    RestClient.builder(new HttpHost(prefs.get("es_host"), Integer.valueOf(prefs.get("es_port")))));
            if (prefs.get("es_sniff").equals("true")) {
                sniffer = Sniffer.builder(client.getLowLevelClient()).build();
                logger.log(Level.INFO, "ES Sniff feature is enabled");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to properly create the elastic rest client to: " + prefs.get("es_host")
                    + ":" + prefs.get("es_port"), e);
        }

    }

    @Override
    public void stop() {
        if (client != null) {
            try {
                if (sniffer != null) {
                    sniffer.close();
                }
                client.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to properly close the elastic rest client", e);
            }
        }
        
    }

    public RestHighLevelClient getClient() {
        return client;
    }

}
