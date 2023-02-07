package org.phoebus.service.saveandrestore.application;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class PropertiesHelper {

    static Properties prop = new Properties();

    /**
     * load the default properties from the properties file packaged with the jar
     */
    static {
        String filename = "application.properties";
        try (InputStream input = PropertiesHelper.class.getClassLoader().getResourceAsStream(filename);) {
            if (input != null) {
                // load a properties file from class path, inside static method
                prop.load(input);
            }else {
                Logger.getLogger(PropertiesHelper.class.getName()).warning("Sorry, unable to find " + filename);
            }
        } catch (IOException e) {
            Logger.getLogger(PropertiesHelper.class.getName()).warning("Failed to load properties from: " + filename);
        }
    }

    public static Properties getProperties() {
        return prop;
    }
}
