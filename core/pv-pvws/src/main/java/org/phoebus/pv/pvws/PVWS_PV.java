package org.phoebus.pv.pvws;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.phoebus.pv.PV;

import java.net.URISyntaxException;

public class PVWS_PV extends PV {
    /**
     * Initialize
     *
     * @param name PV name
     */
    protected PVWS_PV(String name, String base_name) throws Exception {
        super(name);
        PVWS_Context context = PVWS_Context.getInstance();
        context.clientSubscribe(base_name);
    }
}
