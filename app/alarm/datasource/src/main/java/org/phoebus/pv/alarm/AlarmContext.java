package org.phoebus.pv.alarm;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientListener;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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

    private static synchronized void initializeAlarmClient(String config)
    {
        if (!alarmModels.containsKey(config))
        {
            logger.log(Level.CONFIG, "Creating a alarm client for config : + " + config + " in the alarm datasource");
            AlarmClient model = new AlarmClient(AlarmSystem.server, config);
            model.addListener(new AlarmClientListener() {


                @Override
                public void serverStateChanged(boolean alive) {
                    System.out.println("serverStateChanged : " + alive);

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
    }

    private static synchronized void intializeAlarmPV(AlarmPV alarmPV)
    {
        if (alarmModels.containsKey(alarmPV.getInfo().getRoot()))
        {
            AlarmClientNode root = alarmModels.get(alarmPV.getInfo().getRoot()).getRoot();
            AlarmTreeItem<?> node = root;
            // find the child
            if (alarmPV.getInfo().getPath().isPresent())
            {
                Iterator<Path> it = Path.of(alarmPV.getInfo().getPath().get()).iterator();
                while (it.hasNext())
                {
                    node = node.getChild(it.next().toString());
                }
            }
            pvs.get(alarmPV.getInfo().getCompletePath()).updateValue(node);
        }
    }

    public static synchronized void registerPV(AlarmPV alarmPV)
    {
        pvs.put(alarmPV.getInfo().getCompletePath(), alarmPV);
        // Check if the alarm client associated with the root is created and running
        initializeAlarmClient(alarmPV.getInfo().getRoot());
        intializeAlarmPV(alarmPV);
    }

    public static synchronized void releasePV(AlarmPV alarmPV) {
        if(pvs.containsKey(alarmPV.getInfo().getCompletePath()))
        {
            pvs.remove(alarmPV.getInfo().getCompletePath());
        }
    }
}
