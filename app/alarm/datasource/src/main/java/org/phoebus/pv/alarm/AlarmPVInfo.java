package org.phoebus.pv.alarm;

import java.util.Optional;

/**
 * A class which extracts the root and path info for a given alarm pv
 * @author Kunal Shroff
 */
public class AlarmPVInfo {

    private final String root;
    private final Optional<String> path;

    private AlarmPVInfo(String name)
    {
        int index = name.indexOf("/");
        if (index > 0)
        {
            root = name.substring(0, index);
            path = Optional.ofNullable(name.substring(index, name.length()));
        }
        else
        {
            root = name;
            path = Optional.ofNullable(null);
        }
    }

    /**
     * Creates an {@link AlarmPVInfo} form the given name
     * @param name pv name
     * @return an instance of the {@link AlarmPVInfo}
     */
    public static AlarmPVInfo of(String name)
    {
        return new AlarmPVInfo(name);
    }

    /**
     * @return the root for the give alarm pv
     */
    public String getRoot()
    {
        return root;
    }

    /**
     * @return an {@link Optional} with the alarm pv path if present
     */
    public Optional<String> getPath()
    {
        return path;
    }
}