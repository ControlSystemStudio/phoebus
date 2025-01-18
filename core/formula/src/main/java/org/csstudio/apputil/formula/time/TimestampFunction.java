package org.csstudio.apputil.formula.time;

import org.csstudio.apputil.formula.array.BaseArrayFunction;
import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.phoebus.util.time.TimestampFormats.MILLI_FORMAT;

/**
 * Extract the timestamps from PV values
 */
public class TimestampFunction implements FormulaFunction {

    final private static ZoneId zone = ZoneId.systemDefault();
    final private static VString emptyString = VString.of("", Alarm.none(), Time.now());

    @Override
    public String getCategory() {
        return "time";
    }

    @Override
    public String getName() {
        return "timestamp";
    }

    @Override
    public String getDescription() {
        return "Returns the timestamp of the last update";
    }

    @Override
    public List<String> getArguments() {
        return List.of("pv_name", "time format (optional)");
    }

    @Override
    public boolean isVarArgs() {
        return true;
    }

    private Map<String, DateTimeFormatter> formatters = new HashMap<>();

    @Override
    public VType compute(VType... args) {
        if (args.length == 1) {
            return VString.of(MILLI_FORMAT.format(VTypeHelper.getTimestamp(args[0])), Alarm.none(), Time.now());
        } else if (args.length == 2) {
            String format = VTypeHelper.toString(args[1]);
            if (!formatters.containsKey(format)) {
                formatters.put(format, DateTimeFormatter.ofPattern(VTypeHelper.toString(args[1])).withZone(zone));
            }
            return VString.of(formatters.get(format).format(VTypeHelper.getTimestamp(args[0])), Alarm.none(), Time.now());
        }
        return emptyString;

    }
}
