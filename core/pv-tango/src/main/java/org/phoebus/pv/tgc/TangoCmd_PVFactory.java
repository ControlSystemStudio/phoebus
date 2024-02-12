package org.phoebus.pv.tgc;

import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;

public class TangoCmd_PVFactory implements PVFactory {

    /** PV type implemented by this factory */
    final public static String TYPE = "tgc";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public PV createPV(String name, String base_name) throws Exception {
        return new TangoCmd_PV(name, base_name);
    }

}
