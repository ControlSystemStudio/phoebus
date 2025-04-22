package org.csstudio.apputil.formula.array;

import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ListNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.Display;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.List;

/**
 * Computes the cumulative sum of the array.
 */
public class ArrayCumSumFunction extends BaseArrayFunction {

    @Override
    public String getName() {
        return "arrayCumSum";
    }

    @Override
    public String getDescription() {
        return "Result[x] = Sum(array[0] to array[x]).";
    }

    @Override
    public List<String> getArguments() {
        return List.of("array");
    }

    /**
     * Computes the cumulative sum of the array.
     * @param args Arguments, count will match <code>getArgumentCount()</code>
     * @return A {@link VNumberArray} with the cumulative sum of the array. If the
     * specified arguments are not of the supported types, {@link BaseArrayFunction#DEFAULT_NAN_DOUBLE}
     */
    @Override
    public VType compute(VType... args) {
        if (VTypeHelper.isNumericArray(args[0])) {
            VNumberArray array = (VNumberArray) args[0];
            ListNumber data = array.getData();
            double[] result = new double[data.size()];
            double sum = 0;
            for (int i = 0; i < data.size(); i++) {
                sum += data.getDouble(i);
                result[i] = sum;
            }
            return VNumberArray.of(ArrayDouble.of(result), Alarm.none(), Time.now(), Display.none());
        } else {
            return DEFAULT_NAN_DOUBLE_ARRAY;
        }
    }
}
