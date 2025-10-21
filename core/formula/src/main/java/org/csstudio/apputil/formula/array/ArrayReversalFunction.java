package org.csstudio.apputil.formula.array;

import org.csstudio.apputil.formula.Formula;
import org.epics.util.array.ListDouble;
import org.epics.util.array.ListNumber;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * A formula function for reversing the given array.
 * <p>
 * The function allows for reversing the order of elements in an array, such that the first element
 * becomes the last, the second element becomes the second to last, and so on.
 * </p>
 *
 * <b>Arguments:</b>
 * <ul>
 *     <li><b>array</b>: The input numeric array to reverse.</li>
 * </ul>
 *
 * <b>Example:</b>
 * <p>
 * If given an array [1, 2, 3, 4, 5], the result will be [5, 4, 3, 2, 1].
 * </p>
 *
 * @author Kunal Shroff
 */
public class ArrayReversalFunction extends BaseArrayFunction {

    @Override
    public String getName() {
        return "arrayReverse";
    }

    @Override
    public String getDescription() {
        return "Reverses the given numeric array so that the first element becomes the last and vice versa.";
    }

    @Override
    public List<String> getArguments() {
        return List.of("array");
    }

    /**
     * Reverses the elements of a numeric array.
     *
     * @param array The input numeric array (VNumberArray) to reverse.
     * @return A {@link VNumberArray} containing the reversed elements.
     */
    protected VType getArrayData(final VNumberArray array) {
        return VNumberArray.of(reverse(array.getData()), Alarm.none(), array.getTime(), Display.none());
    }

    private static ListDouble reverse(final ListNumber data) {
        return new ListDouble() {
            @Override
            public double getDouble(int index) {
                return data.getDouble(data.size() - 1 - index);
            }

            @Override
            public int size() {
                return data.size();
            }
        };
    }

    /**
     * Computes the function's result by reversing the provided array.
     *
     * @param args The arguments to the function:
     *             - args[0]: The input array to reverse.
     * @return A {@link VType} containing the reversed array.
     * @throws Exception If an invalid number of arguments or incorrect argument types are provided.
     */
    @Override
    public VType compute(final VType... args) throws Exception {
        if (args.length != 1) {
            throw new Exception("Function " + getName() +
                    " requires 1 argument but received " + Arrays.toString(args));
        }
        if (!VTypeHelper.isNumericArray(args[0])) {
            Formula.logger.log(Level.WARNING, "Function " + getName() +
                    " takes array but received " + Arrays.toString(args));
            return DEFAULT_NAN_DOUBLE_ARRAY;
        } else {
            return getArrayData((VNumberArray) args[0]);
        }
    }
}
