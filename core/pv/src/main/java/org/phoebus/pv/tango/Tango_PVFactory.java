package org.phoebus.pv.tango;

import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;

public class Tango_PVFactory implements PVFactory {

    /** PV type implemented by this factory */
    final public static String TYPE = "tango";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public PV createPV(String name, String base_name) throws Exception {
        return new Tango_PV(name, base_name);
    }


}
