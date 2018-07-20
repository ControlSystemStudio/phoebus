package org.phoebus.applications.alarm.logging.ui;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.image.Image;

public class AlarmLogTableApp implements AppDescriptor {

    public static final Logger logger = Logger.getLogger(AlarmLogTableApp.class.getName());
    public static final String NAME = "alarmLogTable";
    public static final String DISPLAYNAME = "Alarm Log Table";

    static final Image icon = ImageCache.getImage(AlarmLogTableApp.class, "/icons/alarmtable.png");

    private RestHighLevelClient client;
    private PreferencesReader prefs;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AppInstance create() {
        return new AlarmLogTable(this);
    }

    @Override
    public void start() {
        AppDescriptor.super.start();
        prefs = new PreferencesReader(AlarmLogTableApp.class, "/alarm_logging_preferences.properties");
        try {
            client = new RestHighLevelClient(
                    RestClient.builder(new HttpHost(prefs.get("es_host"), Integer.valueOf(prefs.get("es_port")))));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void stop() {
        AppDescriptor.super.stop();
        if (client != null) {
            try {
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
