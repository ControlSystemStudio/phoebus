package org.phoebus.pv.tga;

import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;


public class TangoAttr_PVFactory implements PVFactory {

    /** PV type implemented by this factory */
    final public static String TYPE = "tga";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public PV createPV(String name, String base_name) throws Exception {
        return new TangoAttr_PV(name, base_name);
    }


}
