package org.phoebus.pv.alarm;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientListener;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.framework.jobs.JobManager;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static Map<String, List<AlarmPV>> pvs = new HashMap<>();

    private static synchronized void initializeAlarmClient(String config)
    {
        if (!alarmModels.containsKey(config))
        {
            logger.log(Level.CONFIG, "Creating a alarm client for config : + " + config + " in the alarm datasource");
            AlarmClient model = new AlarmClient(AlarmSystem.server, config, AlarmSystem.kafka_properties);
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
                pvs.get(alarmPV.getInfo().getCompletePath())
                        .get(pvs.get(alarmPV.getInfo().getCompletePath()).indexOf(alarmPV))
                        .updateValue(node);
            }
            else
            {
                // TODO error initializing an alarm pv which does not exist as yet

            }

        }
    }

    public static synchronized void registerPV(AlarmPV alarmPV)
    {
        if(!pvs.containsKey(alarmPV.getInfo().getCompletePath()))
        {
            pvs.put(alarmPV.getInfo().getCompletePath(), new ArrayList<AlarmPV>());
        }
        pvs.get(alarmPV.getInfo().getCompletePath()).add(alarmPV);
        // Check if the alarm client associated with the root is created and running
        initializeAlarmClient(alarmPV.getInfo().getRoot());
        initializeAlarmPV(alarmPV);
    }

    public static synchronized void releasePV(AlarmPV alarmPV)
    {
        if(pvs.containsKey(alarmPV.getInfo().getCompletePath()))
        {
            pvs.get(alarmPV.getInfo().getCompletePath()).remove(alarmPV);
            if (pvs.get(alarmPV.getInfo().getCompletePath()).isEmpty())
            {
                pvs.remove(alarmPV.getInfo().getCompletePath());
            }
        }

        shutdownClientIfPossbile(alarmPV);
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
                        AlarmTreeItem<?> finalNode = node;
                        JobManager.schedule("getText()", monitor ->
                        {
                            final List<AlarmClientLeaf> pvs = new ArrayList<>();
                            findAffectedPVs(finalNode, pvs);
                            for (AlarmClientLeaf pv : pvs)
                            {
                                final AlarmClientLeaf copy = pv.createDetachedCopy();
                                if (copy.setEnabled(enable))
                                    alarmModels.get(alarmPV.getInfo().getRoot()).sendItemConfigurationUpdate(pv.getPathName(), copy);
                            }
                        });
                    }
                }
            }
        }
    }

    /** @param item Node where to start recursing for PVs that would be affected
     *  @param pvs Array to update with PVs that would be affected
     */
    private static void findAffectedPVs(final AlarmTreeItem<?> item, final List<AlarmClientLeaf> pvs)
    {
        if (item instanceof AlarmClientLeaf)
        {
            final AlarmClientLeaf pv = (AlarmClientLeaf) item;
            if (!pvs.contains(pv))
                pvs.add(pv);
        }
        else
        {
            for (AlarmTreeItem<?> sub : item.getChildren())
                findAffectedPVs(sub, pvs);
        }
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
                            // Since all the pvs share the same root, only checking ths first one.
                            // TODO a more elegant solution required.
                            return alarmPV.get(0).getInfo().getRoot().equalsIgnoreCase(config);
                        }).forEach(pv -> {
                            pv.forEach(AlarmPV::disconnected);
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
                pvs.get(decodedKafaPath(item.getPathName())).forEach(pv -> {pv.updateValue(item);});
            }
        }

        @Override
        public void itemRemoved(AlarmTreeItem<?> item)
        {
            if(pvs.containsKey(decodedKafaPath(item.getPathName())))
            {
                pvs.get(decodedKafaPath(item.getPathName())).forEach(AlarmPV::disconnected);
            }
        }

        @Override
        public void itemUpdated(AlarmTreeItem<?> item)
        {
            if(pvs.containsKey(decodedKafaPath(item.getPathName())))
            {
                pvs.get(decodedKafaPath(item.getPathName())).forEach(pv -> {pv.updateValue(item);});
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

    /**
     * Shuts down the alarm client, but only if no other alarm PVs in the same
     * alarm configuration are found in the list of registered PVs.
     * @param alarmPV Alarm PV
     */
    private static void shutdownClientIfPossbile(AlarmPV alarmPV){
        String config = alarmPV.getInfo().getRoot();
        if(!remainingPVsInConfig(config)){
            AlarmClient alarmClient = alarmModels.get(config);
            if(alarmClient != null){
                alarmClient.shutdown();
                alarmModels.remove(config);
            }
        }
    }

    /**
     * Checks if the list of registered PVs contains any items for the specified
     * configuration.
     * @param config An alarm configuration name, i.e. root of the {@link AlarmPV} path.
     * @return <code>true</code> if additional PVs are found, otherwise <code>false</code>.
     */
    private static boolean remainingPVsInConfig(String config){
        Set<String> pvNames = pvs.keySet();
        for(String pvName : pvNames){
            if(pvName.startsWith("/" + config)){
                return true;
            }
        }
        return false;
    }
}
