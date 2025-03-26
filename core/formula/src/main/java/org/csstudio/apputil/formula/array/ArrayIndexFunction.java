package org.csstudio.apputil.formula.array;

import org.epics.util.array.ListNumber;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.epics.vtype.VInt;
import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.Display;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.List;

/**
 * Get index of the specified element in the array.
 */
public class ArrayIndexFunction extends BaseArrayFunction {

    @Override
    public String getName() {
        return "arrayIndex";
    }

    @Override
    public String getDescription() {
        return "Returns the index of the specified element in the array, -1 if not found.";
    }

    @Override
    public List<String> getArguments() {
        return List.of("<String | Number> array", "element");
    }

    /**
     * Get index of the specified element in the array.
     * @param args Arguments, count will match <code>getArgumentCount()</code>
     * @return A {@link VInt} with the index of the element in the array, -1 if not found. If the arguments are not of
     * the supported types, {@link BaseArrayFunction#DEFAULT_NAN_DOUBLE} or {@link BaseArrayFunction#DEFAULT_EMPTY_STRING}
     */
    @Override
    public VType compute(VType... args) {
        boolean isStringArray = args[0] instanceof VStringArray;
        if (isStringArray && args[1] instanceof VString) {
            VStringArray stringArray = (VStringArray) args[0];
            VString element = (VString) args[1];
            List<String> data = stringArray.getData();

            int idx = data.indexOf(element.getValue());
            return VInt.of(idx, Alarm.none(), Time.now(), Display.none());
        }
        else if (VTypeHelper.isNumericArray(args[0]) && args[1] instanceof VNumber) {
            VNumberArray numberArray = (VNumberArray) args[0];
            ListNumber data = numberArray.getData();
            VNumber element = (VNumber) args[1];
            double elementValue = element.getValue().doubleValue();

            for (int i = 0; i < data.size(); i++) {
                if (data.getDouble(i) == elementValue) {
                    return VInt.of(i, Alarm.none(), Time.now(), Display.none());
                }
            }
            // return -1 if not found
            return VInt.of(-1, Alarm.none(), Time.now(), Display.none());
        }
        else {
            return isStringArray ? DEFAULT_EMPTY_STRING : DEFAULT_NAN_DOUBLE;
        }
    }
}
