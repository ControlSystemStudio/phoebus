package org.phoebus.pv.archive.replay;

import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;
import org.phoebus.pv.archive.retrieve.ArchivePVFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.pv.archive.ArchiveReaderUtil.parseSupportedTimeFormat;

/**
 * A datasource for the replaying archived PV's
 * @author Kunal Shroff
 */
public class ReplayPVFactory implements PVFactory
{
    final public static Logger logger = Logger.getLogger(ReplayPVFactory.class.getName());
    final public static String TYPE = "replay";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public PV createPV(String name, String base_name) throws Exception
    {
        // Determine simulation function name and (optional) parameters
        final String pvName;
        int sep = base_name.indexOf('(');
        if (sep < 0)
        {
            pvName = base_name;
            return new ReplayPV(name, pvName, Instant.now().minusSeconds(300), Instant.now());
        }
        else
        {
            final int end = base_name.lastIndexOf(')');
            if (end < 0)
                throw new Exception("Missing closing bracket for parameters in '" + name + "'");
            pvName = base_name.substring(0, sep);
            final List<String> parameters = Arrays
                    .stream(base_name.substring(sep+1, end).split(","))
                    .map(String::strip)
                    .collect(Collectors.toList());

            Instant startTime;
            Instant endTime;
            if (parameters.size() == 2) {
                // start and end
                startTime = parseSupportedTimeFormat(parameters.get(0));
                endTime = parseSupportedTimeFormat(parameters.get(1));
                return new ReplayPV(name, pvName, startTime, endTime);

            } else if (parameters.size() == 3 ) {
                // start, end, and rate
                startTime = parseSupportedTimeFormat(parameters.get(0));
                endTime = parseSupportedTimeFormat(parameters.get(1));
                double rate = Double.parseDouble(parameters.get(2));

                return new ReplayPV(name, pvName, startTime, endTime, rate);

            } else {
                throw new Exception("Incorrect number of parameters defined," + "'" + name + "'" +
                        " the replay datasource supports start, end, and optionally a rate parameter.");
            }

        }
    }
}
