package org.phoebus.pv.alarm;

import org.phoebus.applications.alarm.client.AlarmClient;

import java.util.HashMap;
import java.util.Map;

/**
 * A context for creating alarm channels for multiple alarm trees.
 * The context creates and manages the use of alarm models for multiple configurations.
 * @author Kunal Shroff
 */
public class AlarmModelCache {

    private static AlarmModelCache instance;

    private Map<String, AlarmClient> alarmModels;

    private AlarmModelCache()
    {
        alarmModels = new HashMap<>();
    }

    public static synchronized AlarmModelCache getInstance()
    {
        if (instance == null)
        {
            instance = new AlarmModelCache();
        }
        return instance;
    }
}
