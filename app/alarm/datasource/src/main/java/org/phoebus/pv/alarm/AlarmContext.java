package org.phoebus.pv.alarm;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientListener;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Path;
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
            model.addListener(new AlarmClientDatasourceListener(config));
            model.start();
            alarmModels.put(config, model);
        }
    }

    private static synchronized void initializeAlarmPV(AlarmPV alarmPV)
    {
        if (alarmModels.containsKey(alarmPV.getInfo().getRoot()))
        {
            if(pvs.containsKey(alarmPV.getInfo().getCompletePath()))
            {
                AlarmClientNode root = alarmModels.get(alarmPV.getInfo().getRoot()).getRoot();
                AlarmTreeItem<?> node = root;
                // find the child
                if (alarmPV.getInfo().getPath().isPresent())
                {
                    Iterator<Path> it = Path.of(encodedURLPath(alarmPV.getInfo().getPath().get())).iterator();
                    while (it.hasNext() && node != null)
                    {
                        node = node.getChild(decodedURLPath(it.next().toString()));
                    }
                }
                pvs.get(alarmPV.getInfo().getCompletePath()).updateValue(node);
            }
            else
            {
                // TODO error initializing an alarm pv which does not exist as yet

            }

        }
    }

    public static synchronized void registerPV(AlarmPV alarmPV)
    {
        pvs.put(alarmPV.getInfo().getCompletePath(), alarmPV);
        // Check if the alarm client associated with the root is created and running
        initializeAlarmClient(alarmPV.getInfo().getRoot());
        initializeAlarmPV(alarmPV);
    }

    public static synchronized void releasePV(AlarmPV alarmPV)
    {
        if(pvs.containsKey(alarmPV.getInfo().getCompletePath()))
        {
            pvs.remove(alarmPV.getInfo().getCompletePath());
        }
    }

    public static synchronized void acknowledgePV(AlarmPV alarmPV, boolean ack)
    {
        if (alarmModels.containsKey(alarmPV.getInfo().getRoot()))
        {
            if(pvs.containsKey(alarmPV.getInfo().getCompletePath()))
            {
                AlarmClientNode root = alarmModels.get(alarmPV.getInfo().getRoot()).getRoot();
                AlarmTreeItem<?> node = root;
                // find the child
                if (alarmPV.getInfo().getPath().isPresent())
                {
                    Iterator<Path> it = Path.of(encodedURLPath(alarmPV.getInfo().getPath().get())).iterator();
                    while (it.hasNext() && node != null)
                    {
                        node = node.getChild(decodedURLPath(it.next().toString()));
                    }
                    if (node != null)
                    {
                        alarmModels.get(alarmPV.getInfo().getRoot()).acknowledge(node, ack);
                    }
                }
            }
        }
    }

    public static synchronized void enablePV(AlarmPV alarmPV, boolean enable)
    {
        // TODO
    }

    private static class AlarmClientDatasourceListener implements AlarmClientListener
    {
        // The alarm configuration this listener is used to monitor
        private final String config;

        private AlarmClientDatasourceListener(String config) {
            this.config = config;
        }

        @Override
        public void serverStateChanged(boolean alive)
        {
            if(!alive)
            {
                // Disconnect AlarmPVs associated with this config only
                pvs.values().stream()
                        .filter(alarmPV -> {
                            return alarmPV.getInfo().getRoot().equalsIgnoreCase(config);
                        }).forEach(pv -> {
                            pv.disconnected();
                        });
            }
        }

        @Override
        public void serverModeChanged(boolean maintenance_mode)
        {

        }

        @Override
        public void serverDisableNotifyChanged(boolean disable_notify)
        {

        }

        @Override
        public void itemAdded(AlarmTreeItem<?> item)
        {
            if(pvs.containsKey(decodedKafaPath(item.getPathName())))
            {
                pvs.get(decodedKafaPath(item.getPathName())).updateValue(item);
            }
        }

        @Override
        public void itemRemoved(AlarmTreeItem<?> item)
        {
            if(pvs.containsKey(decodedKafaPath(item.getPathName())))
            {
                pvs.get(decodedKafaPath(item.getPathName())).disconnected();
            }
        }

        @Override
        public void itemUpdated(AlarmTreeItem<?> item)
        {
            if(pvs.containsKey(decodedKafaPath(item.getPathName())))
            {
                pvs.get(decodedKafaPath(item.getPathName())).updateValue(item);
            }
        }

        /**
         * Normalize the path encoded in the by the kafka messages.
         * @param path
         * @return normalized path of the
         */
        private static String normalizeItemPath(String path)
        {
            String normalizedPath = path;
            return normalizedPath;
        }
    }

    // A set of methods to handle the encoding and decoding of special chars and escaped chars in the config path
    // of alarm messages.

    private static final String delimiter ="://";

    private static final String encodedColon = URLEncoder.encode(":", Charset.defaultCharset());
    private static final String encodecDelimiter = URLEncoder.encode("://", Charset.defaultCharset());
    /**
     *
     * @param path
     * @return
     */
    static String encodedURLPath(String path)
    {
        return String.valueOf(path).replace("://", encodecDelimiter).replace(":", encodedColon);
    }

    /**
     *
     * @param path
     * @return
     */
    static String decodedURLPath(String path)
    {
        return String.valueOf(path).replace(encodecDelimiter, "://").replace(encodedColon, ":");
    }

    /**
     *
     * @param path
     * @return
     */
    static String encodedKafkaPath(String path)
    {
        return path;
    }

    /**
     *
     * @param path
     * @return
     */
    static String decodedKafaPath(String path)
    {
        return path.replace("\\/","/");
    }
}
