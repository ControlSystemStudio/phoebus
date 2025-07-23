package org.phoebus.pv.pvws;

import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;

public class PVWS_PVFactory implements PVFactory {

    /** PV type implemented by this factory */
    final public static String TYPE = "pvws";

    @Override
    public String getType()
    {
        return TYPE;
    }


    @Override
    public PV createPV(String name, String base_name) throws Exception {
        return null;
    }
}
