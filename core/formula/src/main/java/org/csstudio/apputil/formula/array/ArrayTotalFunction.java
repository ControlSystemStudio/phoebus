package org.csstudio.apputil.formula.array;

import org.epics.util.array.ListNumber;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.List;

/**
 * Sums all elements in an array and returns a single number.
 */
public class ArrayTotalFunction extends BaseArrayFunction {

    @Override
    public String getName() {
        return "arrayTotal";
    }

    @Override
    public String getDescription() {
        return "Returns the sum of all elements in the array.";
    }

    @Override
    public List<String> getArguments() {
        return List.of("array");
    }

    @Override
    public VType compute(VType... args) {
        if (VTypeHelper.isNumericArray(args[0])) {
            VNumberArray array = (VNumberArray) args[0];
            ListNumber data = array.getData();
            double sum = 0.0;
            for (int i = 0; i < data.size(); i++) {
                sum += data.getDouble(i);
            }
            return VNumber.of(sum, Alarm.none(), Time.now(), Display.none());
        }
        else {
            return DEFAULT_NAN_DOUBLE;
        }
    }
}
