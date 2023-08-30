package org.csstudio.apputil.formula.enums;

import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.vtype.Alarm;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VEnum;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;

import java.util.List;

/**
 * A Formula function to create an enum type
 * @author Kunal Shroff
 */
public class EnumOfFunction implements FormulaFunction {
    @Override
    public String getCategory()
    {
        return "enum";
    }

    @Override
    public String getName()
    {
        return "enumOf";
    }

    @Override
    public String getDescription()
    {
        return "Creates a VEnum based a value and a set of intervals.";
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("value", "intervals", "labels");
    }

    @Override
    public VType compute(VType... args) throws Exception
    {
        VNumber value = (VNumber) args[0];
        VNumberArray intervals = (VNumberArray) args[1];
        VStringArray labels = (VStringArray) args[2];
        int index = 0;
        while (index < intervals.getData().size() && value.getValue().doubleValue() >= intervals.getData().getDouble(index)) {
            index++;
        }
        return VEnum.of(value.getValue().intValue(),
                        EnumDisplay.of(labels.getData()),
                        Alarm.none(),
                        Time.now());
    }

}
