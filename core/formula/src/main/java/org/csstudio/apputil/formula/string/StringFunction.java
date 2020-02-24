package org.csstudio.apputil.formula.string;


import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.util.array.ListNumber;
import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
/**
 * A list of formula functions for string manipulations
 * @author Kunal Shroff
 */
public class StringFunction implements FormulaFunction {

    @Override
    public String getCategory() {
        return "string";
    }

    @Override
    public String getName() {
        return "concat";
    }

    @Override
    public String getDescription() {
        return "Concatenate a list of strings of a string array";
    }

    @Override
    public boolean isVarArgs() {
        return true;
    }

    @Override
    public List<String> getArguments() {
        return List.of("String...");
    }

    @Override
    public VType compute(VType... args) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        Arrays.asList(args).forEach(arg -> {
            if (isStringArray(arg))
            {
                stringBuilder.append(getStringArray(arg).stream().collect(Collectors.joining()));
            } else if (isString(arg))
            {
                stringBuilder.append(((VString)arg).getValue());
            }
        });
        return VString.of(stringBuilder.toString(), Alarm.none(), Time.now());
    }

    /**
     * Returns true is the value is a StringArray or can be converted to a StringArray
     * @param value 
     * @return boolean true if value can be used as a string array
     */
    private boolean isStringArray(VType value)
    {
        return value instanceof VStringArray 
            || value instanceof VNumberArray;
    }

    private List<String> getStringArray(VType value) {
        if(value instanceof VStringArray)
        {
            return ((VStringArray) value).getData();
        }
        else if (value instanceof VNumberArray)
        {
            List<String> stringData = new ArrayList<String>();
            ListNumber data = ((VNumberArray) value).getData();
            for (int i = 0; i < data.size(); i++) {
                stringData.add(String.valueOf(data.getDouble(i)));
            }
            return stringData;
        }
        return Collections.emptyList();
    }

    private boolean isString(VType value)
    {
        return value instanceof VString;
    }
}
