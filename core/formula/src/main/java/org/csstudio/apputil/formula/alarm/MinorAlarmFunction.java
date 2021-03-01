package org.csstudio.apputil.formula.alarm;

import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Time;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.Arrays;
import java.util.List;

/**
 * Returns a minor alarm when the given condition evaluates as true.
 *
 */
public class MinorAlarmFunction implements FormulaFunction
{

    @Override
    public String getCategory()
    {
        return "alarm";
    }

    @Override
    public boolean isVarArgs()
    {
        return false;
    }

    @Override
    public String getName()
    {
        return "minorAlarm";
    }

    @Override
    public String getDescription()
    {
        return "Returns a string with minor severity when the given condition is true.";
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("Boolean", "String");
    }

    @Override
    public VType compute(VType... args) throws Exception {

        if(args[1] instanceof VString)
        {
            VType condition = args[0];
            VString message = (VString) args[1];
            if (VTypeHelper.toBoolean(condition))
            {
                return VString.of("True: " + message.getValue(), Alarm.of(AlarmSeverity.MINOR, AlarmStatus.CLIENT, message.getValue()), Time.now());
            } else
            {
                return VString.of("False: " + message.getValue(), Alarm.none(), Time.now());
            }
        } else
        {
            throw new Exception("Function " + getName() + " requires the following arguments " + Arrays.toString(args));
        }
    }
}
