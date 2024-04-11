package org.phoebus.pv.archive.retrieve;

import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;

import java.time.Instant;
import java.util.logging.Logger;

import static org.phoebus.util.time.TimestampFormats.DATETIME_FORMAT;
import static org.phoebus.util.time.TimestampFormats.SECONDS_FORMAT;
import static org.phoebus.util.time.TimestampFormats.MILLI_FORMAT;
import static org.phoebus.util.time.TimestampFormats.FULL_FORMAT;

/**
 * A datasource for the retrieval of archived PV's
 * @author Kunal Shroff
 */
public class ArchivePVFactory implements PVFactory
{

    final public static Logger logger = Logger.getLogger(ArchivePVFactory.class.getName());
    final public static String TYPE = "archive";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public PV createPV(String name, String base_name) throws Exception {
        // Determine simulation function name and (optional) parameters
        final String pvName, parameters;
        int sep = base_name.indexOf('(');
        if (sep < 0)
        {
            pvName = base_name;
            parameters = "";
        }
        else
        {
            final int end = base_name.lastIndexOf(')');
            if (end < 0)
                throw new Exception("Missing closing bracket for parameters in '" + name + "'");
            pvName = base_name.substring(0, sep);
            parameters = base_name.substring(sep+1, end);
        }

        if(parameters.isEmpty()) {
            return new ArchivePV(pvName);
        } else {
            Instant time;
            switch (parameters.length()) {
                case 16 -> time = Instant.from(DATETIME_FORMAT.parse(parameters));
                case 19 -> time = Instant.from(SECONDS_FORMAT.parse(parameters));
                case 23 -> time = Instant.from(MILLI_FORMAT.parse(parameters));
                case 29 -> time = Instant.from(FULL_FORMAT.parse(parameters));
                default -> throw new Exception("Time value defined in a unknown formatt, '" + parameters + "'");
            }
            return new ArchivePV(pvName, time);
        }
    }
}
