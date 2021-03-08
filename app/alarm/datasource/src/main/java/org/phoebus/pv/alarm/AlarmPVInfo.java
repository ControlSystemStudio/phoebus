package org.phoebus.pv.alarm;

import java.util.List;
import java.util.Optional;

/**
 * A class which extracts the root and path info for a given alarm pv
 * @author Kunal Shroff
 */
public class AlarmPVInfo {

    public static final String activeField = "active";
    public static final String stateField = "state";
    public static final String enableField = "enabled";
    public static final String durationField = "duration";

    private static List<String> specialFields = List.of(activeField, stateField, enableField, durationField);

    private final String name;
    private final String root;
    private final Optional<String> path;
    private final Optional<String> field;
    private final String completePath;

    private AlarmPVInfo(String name)
    {
        this.name = name;

        // parse the fields from the alarm name
        Optional<String> containsFields = specialFields.stream().filter(s -> {
            return this.name.endsWith("." + s);
        }).findFirst();
        if(containsFields.isEmpty())
        {
            field = Optional.empty();
        }
        else
        {
            field = containsFields;
            name = name.replace("."+field.get(), "").trim();
        }

        int index = name.indexOf("/");
        if (index > 0)
        {
            this.root = name.substring(0, index);
            this.path = Optional.ofNullable(name.substring(index, name.length()));

        }
        else
        {
            this.root = name;
            this.path = Optional.ofNullable(null);
        }
        // parse and add the path as it appears in the alarm items
        this.completePath = "/" + name;

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

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return the complete path of the alarm element
     */
    public String getCompletePath()
    {
        return completePath;
    }

    /**
     * @return an {@link Optional} with the alarm pv field if present
     */
    public Optional<String> getField()
    {
        return field;
    }
}