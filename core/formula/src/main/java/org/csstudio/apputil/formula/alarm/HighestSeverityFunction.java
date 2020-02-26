package org.csstudio.apputil.formula.alarm;

import java.util.Arrays;
import java.util.List;

import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VEnum;
import org.epics.vtype.VType;

/**
 * Retrieves the highest alarm from the values.
 *
 */
public class HighestSeverityFunction implements FormulaFunction {

    @Override
    public String getCategory() {
        return "alarm";
    }

    @Override
    public boolean isVarArgs() {
        return true;
    }

    @Override
    public String getName() {
        return "highestSeverity";
    }

    @Override
    public String getDescription() {
        return "Returns the highest severity";
    }

    @Override
    public List<String> getArguments() {
        return List.of("String...");
    }

    @Override
    public VType compute(VType... args) {
        Alarm alarm = Alarm.highestAlarmOf(Arrays.asList(args), true);

        return VEnum.of(alarm.getSeverity().ordinal(), EnumDisplay.of(AlarmSeverity.labels()), alarm, Time.now());
    }
}
