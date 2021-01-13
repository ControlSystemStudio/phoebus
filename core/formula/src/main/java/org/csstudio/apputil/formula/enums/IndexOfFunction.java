package org.csstudio.apputil.formula.enums;

import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VEnum;
import org.epics.vtype.VInt;
import org.epics.vtype.VType;

import java.util.Arrays;
import java.util.List;

/**
 * A Formula function to retrieve the index of an enum.
 * This function is equivalent to a caget -n *pv_name*
 * @author Kunal Shroff
 */
public class IndexOfFunction implements FormulaFunction {
    @Override
    public String getCategory()
    {
        return "enum";
    }

    @Override
    public String getName()
    {
        return "indexOf";
    }

    @Override
    public String getDescription()
    {
        return "Return the index of the enum value.";
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("Enum");
    }

    @Override
    public VType compute(VType... args) throws Exception
    {
        if(args[0] instanceof VEnum)
        {
            final VEnum value = (VEnum)args[0];
            return VInt.of(value.getIndex(),
                    value.getAlarm(),
                    Time.now(),
                    Display.none());
        } else
        {
            throw new Exception("Function " + getName() + " requires an enum argument " + Arrays.toString(args));
        }

    }

}
