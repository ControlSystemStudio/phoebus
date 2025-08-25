package org.phoebus.pv.alarm;

import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VEnum;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.pv.PV;
import org.phoebus.util.time.TimestampFormats;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.phoebus.pv.alarm.AlarmPVInfo.*;

/**
 * A Connection to a node of leaf of the phoebus alarm tree
 * @author Kunal Shroff
 */
public class AlarmPV extends PV
{
    private final AlarmPVInfo info;

    public AlarmPV(String name, String baseName)
    {
        super(name);
        info = AlarmPVInfo.of(baseName);
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
            // Process alarm fields if they are present
            if (info.getField().isPresent())
            {
                notifyListenersOfValue(processField(item));
            }
            else {
                VString alarm = VString.of(item.toString(),
                        processState(item.getState()),
                        Time.now());
                notifyListenersOfValue(alarm);
            }
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
        if(this.info.getField().isPresent())
        {
            // Only the enable field is writeable.
            if(info.getField().get().equalsIgnoreCase(enableField))
            {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public void write(Object newValue) throws Exception {
        // write specific values to alarm pvs with fields
        if (this.getInfo().getField().isPresent())
        {
            writeToField(newValue);
        }
        // basic write
        else
        {
            if (newValue instanceof String)
            {
                switch (((String)newValue).toLowerCase())
                {
                    case "ack":
                    case "acknowledge":
                        AlarmContext.acknowledgePV(this, true);
                        break;
                    case "unack":
                    case "unacknowledge":
                        AlarmContext.acknowledgePV(this, false);
                        break;
                    case "enable":
                        AlarmContext.enablePV(this, true);
                        break;
                    case "disable":
                        AlarmContext.enablePV(this, false);
                        break;
                    default:
                        // Unknown value
                        throw new IllegalArgumentException("cannot write " + newValue + "  to " + this.info.getCompletePath());
                }
            }
            else if (newValue instanceof Number)
            {
                AlarmContext.acknowledgePV(this, ((Number)newValue).intValue() != 0);
            }
            else if (newValue instanceof Boolean)
            {
                AlarmContext.acknowledgePV(this, (Boolean)newValue);
            }
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
                case INVALID:
                    return Alarm.of(AlarmSeverity.INVALID, AlarmStatus.UNDEFINED, state.toString());
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

    private static EnumDisplay alarmLabels = EnumDisplay.of(List.of(SeverityLevel.values()).stream().map(SeverityLevel::toString).collect(Collectors.toList()));

    private VType processField(AlarmTreeItem item)
    {
        switch (info.getField().get())
        {
            case activeField:
                return VBoolean.of(item.getState().severity.isActive(), processState(item.getState()), Time.now());
            case stateField:
                return VEnum.of(alarmLabels.getChoices().indexOf(item.getState().getSeverity().toString()),
                        alarmLabels,
                        processState(item.getState()),
                        Time.now());
            case enableField:
                if (item instanceof AlarmClientLeaf)
                {
                    AlarmClientLeaf leaf = (AlarmClientLeaf) item;
                    return VBoolean.of(leaf.isEnabled(), processState(leaf.getState()), Time.now());
                }
            case durationField:
                if (item instanceof AlarmClientLeaf)
                {
                    AlarmClientLeaf leaf = (AlarmClientLeaf) item;
                    Instant time = leaf.getState().time;
                    return VString.of(TimestampFormats.MILLI_FORMAT.format(time), processState(leaf.getState()), Time.of(time));
                }
            default:
                return null;
        }
    }

    private void writeToField(Object newValue)
    {
        switch (info.getField().get())
        {
            // only enable field is writeable
            case enableField:
                if (newValue instanceof String)
                {
                    switch (((String)newValue).toLowerCase())
                    {
                        case "enable":
                            AlarmContext.enablePV(this, true);
                            break;
                        case "disable":
                            AlarmContext.enablePV(this, false);
                            break;
                        default:
                            // Unknown value
                            throw new IllegalArgumentException("cannot write " + newValue + "  to " + this.info.getCompletePath());
                    }
                }
                else if (newValue instanceof Number)
                {
                    AlarmContext.enablePV(this, ((Number)newValue).intValue() != 0);
                }
                else if (newValue instanceof Boolean)
                {
                    AlarmContext.enablePV(this, (Boolean)newValue);
                }
            default:
                // TODO throw write error
        }
    }
}
