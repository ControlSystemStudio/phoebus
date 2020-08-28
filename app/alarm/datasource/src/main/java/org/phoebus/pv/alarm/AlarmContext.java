package org.phoebus.pv.alarm;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static org.phoebus.pv.alarm.AlarmPVFactory.logger;
/**
 * A context for creating alarm channels for multiple alarm trees.
 * The context creates and manages the use of alarm models for multiple configurations.
 * @author Kunal Shroff
 */
public class AlarmContext {

    private static Map<String, AlarmClient> alarmModels = new HashMap<>();

    static synchronized AlarmClient getAlarmClient(String config)
    {
        if (!alarmModels.containsKey(config))
        {
            logger.log(Level.CONFIG, "Creating a alarm client for config : + " + config + " in the alarm datasource");
            AlarmClient model = new AlarmClient(AlarmSystem.server, config);
            model.start();
            alarmModels.put(config, model);
        }
        return alarmModels.get(config);
    }

    
}
