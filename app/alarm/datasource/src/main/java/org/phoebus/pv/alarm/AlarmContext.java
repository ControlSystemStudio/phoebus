package org.phoebus.pv.alarm;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientListener;
import org.phoebus.applications.alarm.model.AlarmTreeItem;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static org.phoebus.pv.alarm.AlarmPVFactory.logger;
/**
 * A context for creating alarm channels for multiple alarm trees.
 * The context creates and manages the use of alarm models for multiple configurations.
 * @author Kunal Shroff
 */
public class AlarmContext
{
    private static Map<String, AlarmClient> alarmModels = new HashMap<>();
    private static Map<String, AlarmPV> pvs = new HashMap<>();

    private static synchronized AlarmClient initializeAlarmClient(String config)
    {
        if (!alarmModels.containsKey(config))
        {
            logger.log(Level.CONFIG, "Creating a alarm client for config : + " + config + " in the alarm datasource");
            AlarmClient model = new AlarmClient(AlarmSystem.server, config);

            model.addListener(new AlarmClientListener() {


                @Override
                public void serverStateChanged(boolean alive) {

                }

                @Override
                public void serverModeChanged(boolean maintenance_mode) {

                }

                @Override
                public void serverDisableNotifyChanged(boolean disable_notify) {

                }

                @Override
                public void itemAdded(AlarmTreeItem<?> item) {

                }

                @Override
                public void itemRemoved(AlarmTreeItem<?> item) {

                }

                @Override
                public void itemUpdated(AlarmTreeItem<?> item) {
                    if(pvs.containsKey(item.getPathName()))
                    {
                        pvs.get(item.getPathName()).updateValue(item);
                    }
                }
            });

            model.start();
            alarmModels.put(config, model);
        }
        return alarmModels.get(config);
    }


    public static synchronized void registerPV(AlarmPV alarmPV)
    {
        pvs.put(alarmPV.getInfo().getCompletePath(), alarmPV);
        // Check if the alarm client associated with the root is created and running
        initializeAlarmClient(alarmPV.getInfo().getRoot());
    }
}
