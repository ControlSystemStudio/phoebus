package org.csstudio.apputil.formula.array;

import org.csstudio.apputil.formula.Formula;
import org.epics.util.array.CollectionNumbers;
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
 * @author Kunal Shroff
 */
public class ArraySampleWithLTTBFunction extends BaseArrayFunction {

    @Override
    public String getName() {
        return "arraySampleWithLTTB";
    }

    @Override
    public String getDescription() {
        return "Downsample the array using LTTB";
    }

    @Override
    public List<String> getArguments() {
        return List.of("array", "buckets");
    }


    protected VType getArrayData(final VNumberArray array, final double buckets) {
        return VNumberArray.of(sampleWithLTTB(array.getData(), buckets), Alarm.none(), array.getTime(), Display.none());
    }

    // TODO at some point we might want to support non uniform x values
    private static final class Point {
        public final double x, y;
        public Point(double x, double y) { this.x = x; this.y = y; }
    }

    static ListNumber sampleWithLTTB(final ListNumber data, final double threshold) {
        final int n = data.size();
        if (threshold >= n || threshold <= 0) return data;
        if (threshold == 1) return CollectionNumbers.toListDouble(data.getDouble(0));
        if (threshold == 2) return CollectionNumbers.toListDouble(data.getDouble(0), data.getDouble(1));

        final double[] out = new double[(int) threshold];
        out[0] = data.getDouble(0); // keep first
        out[(int) (threshold-1)] = data.getDouble(n-1);

        int aIdx = 0; // index of last selected point
        double bucketSize = (double)(n - 2) / (threshold - 2);

        for (int i = 0; i < threshold - 2; i++) {
            // range of current bucket
            int cs = (int)Math.floor(i * bucketSize) + 1;
            int ce = (int)Math.floor((i + 1) * bucketSize) + 1;
            if (ce > n - 1) ce = n - 1;

            // range of next bucket
            int ns = (int)Math.floor((i + 1) * bucketSize) + 1;
            int ne = (int)Math.floor((i + 2) * bucketSize) + 1;
            if (ne > n - 1) ne = n - 1;

            // average of next bucket (x = index, y = value)
            double avgX;
            double avgY;
            if (ns == ne && ns < n) {
                avgX = ns;
                avgY = data.getDouble(ns);
            } else if (ns >= ne) {
                avgX = ns;
                avgY = data.getDouble(Math.min(ns, n - 2));
            } else {
                double sx = 0, sy = 0;
                for (int j = ns; j < ne; j++) { sx += j; sy += data.getDouble(j); }
                int cnt = ne - ns;
                avgX = sx / cnt;
                avgY = sy / cnt;
            }

            // find point in current bucket with max triangle area
            double ax = aIdx, ay = data.getDouble(aIdx);
            double bestArea = -1;
            int bestIdx = cs;
            for (int j = cs; j < ce; j++) {
                double bx = j, by = data.getDouble(j);
                double area = Math.abs(
                        (bx - ax) * (avgY - ay) - (avgX - ax) * (by - ay)
                );
                if (area >= bestArea) { // prefer last index in case of tie
                    bestArea = area;
                    bestIdx = j;
                }
            }

            out[i+1] = data.getDouble(bestIdx);
            aIdx = bestIdx;
        }

        return CollectionNumbers.toList(out);
    }

    @Override
    public VType compute(final VType... args) throws Exception {
        if (args.length != 2) {
            throw new Exception("Function " + getName() +
                    " requires 2 arguments but received " + Arrays.toString(args));
        }
        if (!VTypeHelper.isNumericArray(args[0])) {
            Formula.logger.log(Level.WARNING, "Function " + getName() +
                    " takes array but received " + Arrays.toString(args));
            return DEFAULT_NAN_DOUBLE_ARRAY;
        } else {
            double buckets = VTypeHelper.toDouble(args[1]);
            return getArrayData((VNumberArray) args[0], buckets);
        }
    }
}
