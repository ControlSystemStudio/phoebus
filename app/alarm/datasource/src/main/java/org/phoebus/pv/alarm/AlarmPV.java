package org.phoebus.pv.alarm;

import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.VBoolean;
import org.phoebus.pv.PV;

/**
 * A Connection to a node of leaf of the phoebus alarm tree
 * @author Kunal Shroff
 */
public class AlarmPV extends PV
{
    public AlarmPV(String name, String base_name)
    {
        super(name);
    }


}
