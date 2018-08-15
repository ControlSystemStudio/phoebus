package org.phoebus.alarm.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.phoebus.alarm.logging.AlarmLoggingService.logger;

public class PropertiesHelper {

    static Properties prop = new Properties();

    /**
     * load the default properties from the properties file packaged with the jar
     */
    static {
        String filename = "alarm_logging_service.properties";
        try (InputStream input = PropertiesHelper.class.getClassLoader().getResourceAsStream(filename);) {
            if (input == null) {
                logger.warning("Sorry, unable to find " + filename);
            }
            // load a properties file from class path, inside static method
            prop.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Properties getProperties() {
        return prop;
    }
}
