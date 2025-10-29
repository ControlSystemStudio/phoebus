package org.phoebus.pv.tango;

import static org.phoebus.pv.PV.logger;

import java.util.logging.Level;

import org.phoebus.framework.preferences.PreferencesReader;

import fr.esrf.TangoApi.Database;

public class TangoPreferences {

    private static final String TANGO_HOST = "tango_host";
    private static final String DEFAULT_TANGO_HOST = "localhost:10000";
    private static Boolean tangoDBEnable = null;
    
    private static TangoPreferences instance = null;

    private TangoPreferences() {
        try {
            installPreferences();
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Preferences Error", e);
        }
    }

    public static TangoPreferences getInstance() {
        if (instance == null) {
            instance = new TangoPreferences();
        }
        return instance;
    }

    private void installPreferences() throws Exception {
        String currentTangoHost = System.getProperty(TANGO_HOST.toUpperCase());
        
        final PreferencesReader prefs = new PreferencesReader(TangoPVFactory.class, "/pv_tango_preferences.properties");
        if (prefs != null) {
            String tangohost = prefs.get(TANGO_HOST);
            if (tangohost != null && !tangohost.trim().isEmpty()) {
                if(currentTangoHost == null || currentTangoHost.equalsIgnoreCase(tangohost)) {
                    logger.log(Level.INFO, "set TANGO_HOST=" + tangohost);
                    System.setProperty(TANGO_HOST.toUpperCase(), tangohost) ;
                    currentTangoHost = tangohost;
                }
            }
        }
        
        if(currentTangoHost == null) {
            logger.log(Level.WARNING, "env " + TANGO_HOST.toUpperCase() + " not set => force to " + DEFAULT_TANGO_HOST);
            System.setProperty(TANGO_HOST.toUpperCase(), DEFAULT_TANGO_HOST) ;
            currentTangoHost = DEFAULT_TANGO_HOST;
        }
    }
    
    public boolean isTangoDbEnable() {
        if(tangoDBEnable == null) {
            //Test database
            try {
                Database db = new Database();
                db.ping();
                tangoDBEnable = true;
            }
            catch (Exception e) {
                tangoDBEnable = false;
                String currentTangoHost = System.getProperty(TANGO_HOST.toUpperCase());
                logger.log(Level.SEVERE, "Tango database not found on " + TANGO_HOST.toUpperCase() + "=" + currentTangoHost, e);
            }
        }
        return tangoDBEnable;
    }
}
