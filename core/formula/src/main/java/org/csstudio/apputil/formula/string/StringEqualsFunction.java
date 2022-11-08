package org.csstudio.apputil.formula.string;


import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.util.array.ListNumber;
import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A formula functions for comparing strings
 * @author Kunal Shroff
 */
public class StringEqualsFunction implements FormulaFunction {

    @Override
    public String getCategory() {
        return "string";
    }

    @Override
    public String getName() {
        return "strEqual";
    }

    @Override
    public String getDescription() {
        return "Compare value of 2 strings";
    }

    @Override
    public boolean isVarArgs() {
        return false;
    }

    @Override
    public List<String> getArguments() {
        return List.of("String", "String");
    }

    @Override
    public VType compute(VType... args) throws Exception {
        if(isString(args[0]) && isString(args[1])) {
            String str1 = ((VString) args[0]).getValue();
            String str2 = ((VString) args[1]).getValue();
            return VBoolean.of(str1.equals(str2), Alarm.none(), Time.now());
        }
        return VBoolean.of(Boolean.FALSE, Alarm.none(), Time.now());
    }

    private boolean isString(VType value)
    {
        return value instanceof VString;
    }
}
