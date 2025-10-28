package org.phoebus.pv.tango;

import java.util.HashMap;
import java.util.Map;

import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;
import org.phoebus.pv.PVPool;

public class TangoPVFactory implements PVFactory {

    /** PV type implemented by this factory */
    final public static String TYPE = "tango";

    /** Map of local PVs */
    private static final Map<String, TangoPV> tango_pvs = new HashMap<>();

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public PV createPV(String name, String base_name) throws Exception {

        String actual_name = getCoreName(name);
        // Actual name: tango://the_pv
        // Add tango://prefix if not exist
        if(!name.startsWith(TangoPVFactory.TYPE + "://")) {
            actual_name = PVPool.TypedName.format(TangoPVFactory.TYPE, name);
        }
        TangoPV tangopv = tango_pvs.get(actual_name);
        
        //Check TANGO HOST
        
        if (tangopv == null) {
            synchronized (tango_pvs) {
                tangopv = new TangoPV(actual_name, base_name);
                tango_pvs.put(actual_name, tangopv);
            }
        }

        return tangopv;
    }
    
    @Override
    public String getCoreName(String name) {
        String actual_name = name;
        // Actual name: tango://the_pv
        // Add tango://prefix if not exist
        if(!name.startsWith(TangoPVFactory.TYPE + "://")) {
            actual_name = PVPool.TypedName.format(TangoPVFactory.TYPE, name);
        }
        return actual_name;
    }

    protected static void releasePV(TangoPV tangopv) {
        if (tango_pvs.containsKey(tangopv.getName())) {
            synchronized (tango_pvs) {
                tango_pvs.remove(tangopv.getName());
            }
        }
    }
}
