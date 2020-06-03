package org.phoebus.alarm.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

import static org.phoebus.alarm.logging.AlarmConfigLoggingService.logger;

public class PropertiesHelper {

    static Properties prop = new Properties();

    static {
        String filename = "alarm_config_logger.properties";

        try (InputStream input = PropertiesHelper.class.getClassLoader().getResourceAsStream(filename);) {
            if (input != null) {
                // load a properties file from class path, inside static method
                prop.load(input);
            } else {
                logger.warning("Unable to configuration find " + filename);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING,"Unable to load configuration " + filename, e);
        }
    }

    public static Properties getProperties() {
        return prop;
    }
}
