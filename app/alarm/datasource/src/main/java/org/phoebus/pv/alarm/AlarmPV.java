package org.phoebus.pv.alarm;

import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Time;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VNumber;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.pv.PV;

/**
 * A Connection to a node of leaf of the phoebus alarm tree
 * @author Kunal Shroff
 */
public class AlarmPV extends PV
{
    private final AlarmPVInfo info;

    public AlarmPV(String name, String base_name)
    {
        super(name);
        info = AlarmPVInfo.of(base_name);
    }

    public AlarmPVInfo getInfo()
    {
        return info;
    }

    public void updateValue(AlarmTreeItem item)
    {
        if(item == null)
        {
            notifyListenersOfDisconnect();
        }
        else {
            // Process alarm
            VString alarm = VString.of(item.toString(),
                    processState(item.getState()),
                    Time.now());
            notifyListenersOfValue(alarm);
        }
    }

    public void disconnected() {
        notifyListenersOfDisconnect();
    }

    @Override
    protected void close()
    {
        super.close();
        AlarmPVFactory.releaseAlarmPV(this);
    }

    @Override
    public boolean isReadonly() {
        // TODO enable write based on auth
        return false;
    }

    @Override
    public void write(Object new_value) throws Exception {
        if (new_value instanceof String)
        {
            switch (((String)new_value).toLowerCase())
            {
                case "ack":
                case "acknowledge":
                    AlarmContext.acknowledgePV(this, true);
                    break;
                case "unack":
                case "unacknowledge":
                    AlarmContext.acknowledgePV(this, false);
                    break;
                default:
                    // Unknown value
                    throw new IllegalArgumentException("cannot write " + new_value + "  to " + this.info.getCompletePath());
            }
        }
        else if (new_value instanceof Number)
        {
            AlarmContext.acknowledgePV(this, ((Number)new_value).intValue() != 0);
        }
        else if (new_value instanceof Boolean)
        {
            AlarmContext.acknowledgePV(this, (Boolean)new_value);
        }
    }

    private static Alarm processState(BasicState state)
    {
        if (state.getSeverity().isActive())
        {
            switch (state.getSeverity())
            {
                case MINOR:
                    return Alarm.of(AlarmSeverity.MINOR, AlarmStatus.UNDEFINED, state.toString());
                case MAJOR:
                    return Alarm.of(AlarmSeverity.MAJOR, AlarmStatus.UNDEFINED, state.toString());
                case UNDEFINED:
                    return Alarm.of(AlarmSeverity.UNDEFINED, AlarmStatus.UNDEFINED, state.toString());
                default:
                    return Alarm.of(AlarmSeverity.NONE, AlarmStatus.UNDEFINED, state.toString());
            }
        }
        else
        {
            return Alarm.of(AlarmSeverity.NONE, AlarmStatus.UNDEFINED, state.toString());
        }
    }

}
