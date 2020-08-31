package org.phoebus.pv.alarm;

import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;

import java.util.logging.Logger;

/**
 * A datasource for the phoebus alarm server
 * @author Kunal Shroff
 */
public class AlarmPVFactory implements PVFactory
{
    final public static Logger logger = Logger.getLogger(AlarmPVFactory.class.getName());
    final public static String TYPE = "alarm";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public PV createPV(final String name, final String base_name) throws Exception
    {
        AlarmPV alarmPV = new AlarmPV(name, base_name);
        AlarmContext.registerPV(alarmPV);
        return alarmPV;
    }

    public static void releaseAlarmPV(AlarmPV alarmPV) {
        AlarmContext.releasePV(alarmPV);
    }

}
