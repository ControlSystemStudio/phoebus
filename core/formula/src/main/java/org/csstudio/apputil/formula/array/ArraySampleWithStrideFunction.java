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
 * A formula function for extracting elements from the given array at regular intervals (stride),
 * starting from a specified offset.
 * <p>
 * The function allows for periodic sampling of an array, where the elements are selected
 * based on a defined stride and an optional offset. The offset allows specifying where
 * the slicing begins in the array.
 * </p>
 *
 * <b>Arguments:</b>
 * <ul>
 *     <li><b>array</b>: The input numeric array to sample from.</li>
 *     <li><b>stride</b>: The step size between selected elements. A value greater than 1 skips elements.</li>
 *     <li><b>offset</b> (optional): The starting index (offset) from which to begin the sampling. Defaults to 0 if not provided.</li>
 * </ul>
 *
 * <b>Example:</b>
 * <p>
 * If given an array of length 10, a stride of 2, and an offset of 1, the result will be a sublist
 * containing every second element starting from index 1 (i.e., [1, 3, 5, 7, 9]).
 * </p>
 *
 * @author Kunal Shroff
 */
public class ArraySampleWithStrideFunction extends BaseArrayFunction {

    @Override
    public String getName() {
        return "arraySampleWithStride";
    }

    @Override
    public String getDescription() {
        return "Extracts elements from the given array at regular intervals (stride), starting from a specified offset.";
    }

    @Override
    public List<String> getArguments() {
        return List.of("array", "stride", "offset");
    }


    /**
     * Extracts elements from a numeric array with a specified stride and offset.
     *
     * @param array     The input numeric array (VNumberArray) from which elements will be sampled.
     * @param xPosition The stride, representing the step size between selected elements.
     * @param offset    The optional starting index (offset) from which to begin the sampling.
     * @return A {@link VNumberArray} containing the sampled elements.
     * @throws IndexOutOfBoundsException If the computed index is out of the bounds of the array.
     */
    protected VType getArrayData(final VNumberArray array, final double xPosition, final double offset) {
        return VNumberArray.of(sampleWithStride(array.getData(), xPosition, offset), Alarm.none(), array.getTime(), Display.none());
    }

    private static ListDouble sampleWithStride(final ListNumber data, final double stride, final double offset) {

        return new ListDouble() {

            @Override
            public double getDouble(int index) {
                int computedIndex = (int) Math.round(offset + index * stride);
                if (computedIndex < 0 || computedIndex >= data.size()) {
                    throw new IndexOutOfBoundsException("Computed index " + computedIndex + " is out of range.");
                }
                return data.getDouble(computedIndex);
            }

            @Override
            public int size() {
                int size = (int) Math.ceil((data.size() - offset) / stride);
                return Math.max(size, 0);
            }
        };
    }

    /**
     * Computes the function's result by extracting the elements from the provided array
     * with the given stride and optional offset.
     *
     * @param args The arguments to the function:
     *             - args[0]: The input array to sample from.
     *             - args[1]: The stride value (step size).
     *             - args[2] (optional): The starting index (offset).
     * @return A {@link VType} containing the result of the array sampling.
     * @throws Exception If an invalid number of arguments or incorrect argument types are provided.
     */
    @Override
    public VType compute(final VType... args) throws Exception {
        if (args.length != 2 && args.length != 3) {
            throw new Exception("Function " + getName() +
                    " requires 2 or 3 arguments but received " + Arrays.toString(args));
        }
        if (!VTypeHelper.isNumericArray(args[0])) {
            Formula.logger.log(Level.WARNING, "Function " + getName() +
                    " takes array but received " + Arrays.toString(args));
            return DEFAULT_NAN_DOUBLE_ARRAY;

        } else {
            double stride = VTypeHelper.toDouble(args[1]);
            double offset = (args.length == 3) ? VTypeHelper.toDouble(args[2]) : 0; // Default offset to 0

            return getArrayData((VNumberArray) args[0], stride, offset);
        }
    }
}
