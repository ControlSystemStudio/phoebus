package org.phoebus.core.vtypes;

import org.epics.vtype.Alarm;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VEnum;

/**
 * This type is for Enum VType with labels and description
 */
public class AdvancedEnum extends VEnum implements DescriptionProvider {
    private String description;
    private VEnum source; 
    
    private AdvancedEnum(final VEnum source, final String description) {
        this.description = description;
        this.source = source;
    }

    @Override
    public Alarm getAlarm() {
        return source != null ? source.getAlarm() : null;
    }

    @Override
    public Time getTime() {
        return source != null ? source.getTime() : null;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getValue() {
        return source != null ? source.getValue() : null;
    }

    @Override
    public int getIndex() {
        return source != null ? source.getIndex() : 0;
    }

    @Override
    public EnumDisplay getDisplay() {
        return source != null ? source.getDisplay() : null;
    }
    

    /**
     * Create a new VEnum.
     * 
     * @param index the index in the label array
     * @param metaData the metadata
     * @param alarm the alarm
     * @param time the time
     * @param description
     * @return the new value
     */
    public static VEnum of(int index, EnumDisplay metaData, Alarm alarm, Time time, String description) {
        VEnum of = VEnum.of(index, metaData, alarm,  time) ;
        return new AdvancedEnum(of, description);
    }


}
