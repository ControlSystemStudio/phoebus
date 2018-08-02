package org.phoebus.alarm.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.phoebus.alarm.logging.AlarmConfigLoggingService.logger;

public class PropertiesHelper {

    static Properties prop = new Properties();

    static {
        String filename = "alarm_config_logging_service.properties";
        InputStream input = null;
        input = PropertiesHelper.class.getClassLoader().getResourceAsStream(filename);
        if (input == null) {
            logger.warning("Sorry, unable to find " + filename);
        }
        // load a properties file from class path, inside static method
        try {
            prop.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Properties getProperties() {
        return prop;
    }
}
