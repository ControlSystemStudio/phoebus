package org.csstudio.apputil.formula.areadetector;


import org.csstudio.apputil.formula.VTypeHelper;
import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.util.array.ArrayUInteger;
import org.epics.util.array.ArrayULong;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VEnum;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
/**
 *
 * @author Kunal Shroff
 */
public class ADDataTypeMappingFunction implements FormulaFunction {

    @Override
    public String getName() {
        return "adData";
    }

    @Override
    public String getDescription() {
        return "Map the area detector data to the specified type,"
                + " i.e. [Int8, UInt8, Int16, UInt16, Int32, UInt32, Float32, Float64]";
    }

    @Override
    public int getArgumentCount() {
        return 2;
    }

    @Override
    public VType compute(VType... args) throws Exception {
        // TODO check if any of the arguments in null

        VNumberArray data = (VNumberArray) args[0];
        String dataType = VTypeHelper.getString(args[1]);
        switch (dataType) {
        case "UInt8":
            int[] newUInt8Data = new int[data.getData().size()];
            for (int i = 0; i < data.getData().size(); i++) {
                if (data.getData().getInt(i) < 0) {
                    newUInt8Data[i] = data.getData().getInt(i) + 256;
                } else {
                    newUInt8Data[i] = data.getData().getInt(i);
                }
            }
            return VNumberArray.of(ArrayUInteger.of(newUInt8Data), Alarm.none(), Time.now(), Display.displayOf(args[0]));
        case "UInt16":
            int[] newUInt16Data = new int[data.getData().size()];
            for (int i = 0; i < data.getData().size(); i++) {
                if (data.getData().getInt(i) < 0) {
                    newUInt16Data[i] = data.getData().getInt(i) + ((int) Math.pow(2, 16));
                } else {
                    newUInt16Data[i] = data.getData().getInt(i);
                }
            }
            return VNumberArray.of(ArrayUInteger.of(newUInt16Data), Alarm.none(), Time.now(), Display.displayOf(args[0]));
        case "UInt32":
            long[] newUInt32Data = new long[data.getData().size()];
            for (int i = 0; i < data.getData().size(); i++) {
                if (data.getData().getInt(i) < 0) {
                    newUInt32Data[i] = data.getData().getInt(i) + ((long) Math.pow(2, 32));
                } else {
                    newUInt32Data[i] = data.getData().getInt(i);
                }
            }
            return VNumberArray.of(ArrayULong.of(newUInt32Data), Alarm.none(), Time.now(), Display.displayOf(args[0]));
        default:
            break;
        }
        return data;
    }

}
